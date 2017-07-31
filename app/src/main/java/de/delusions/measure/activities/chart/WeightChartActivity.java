/*
   Copyright 2011 Sonja Pieper

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package de.delusions.measure.activities.chart;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.MeasureTabs;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.PrefItem;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class WeightChartActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int MONTH = 30;
    private static final int MONTH_3 = 90;
    private static final int MONTH_6 = 180;
    private static final int WEEK_2 = 14;
    private static final int YEAR_1 = 360;

    private WeightChartImage wcImage;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.i(MeasureActivity.TAG, "onCreate WeightChartActivity");
            getWindow().setFormat(PixelFormat.RGBA_8888);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
            setContentView(R.layout.activity_chart);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.registerOnSharedPreferenceChangeListener(this);

            final DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            final int orientation = getWindowManager().getDefaultDisplay().getOrientation();

            this.wcImage = new WeightChartImage(this, MONTH);
            this.wcImage.calculateImageDimensions(metrics, orientation);

            addButton(R.id.months_1, MONTH);
            addButton(R.id.months_3, MONTH_3);
            addButton(R.id.months_6, MONTH_6);
            addButton(R.id.week_2, WEEK_2);
            addButton(R.id.year_1, YEAR_1);
            addDisplayAllButton();
            addToggleOtherValuesButton();

            refreshAll();
        } catch (final IllegalStateException e) {
            Log.e(MeasureActivity.TAG, "WeightChartActivity:onCreate:fails with", e);
            setResult(RESULT_OK);
            finish();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(MeasureActivity.TAG, "onConfigurationChanged");
        refreshAll();
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals(PrefItem.DISPLAY_MEASURE.getKey())) {
            Log.d(MeasureActivity.TAG, "onSharedPreferenceChanged " + key);
            setDisplayField();
            refreshAll();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        return MeasureTabs.basicMenu(this, item) || super.onMenuItemSelected(featureId, item);
    }

    private void addToggleOtherValuesButton() {
        final ToggleButton showbutton = (ToggleButton) findViewById(R.id.showall);
        showbutton.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                WeightChartActivity.this.wcImage.setShowAll(showbutton.isChecked());
                refreshAll();
            }
        });
    }

    private void addDisplayAllButton() {
        final Button all = (Button) findViewById(R.id.all);
        all.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                final SqliteHelper db = new SqliteHelper(WeightChartActivity.this);
                final Cursor cursor = db.fetchFirst(WeightChartActivity.this.wcImage.getDisplayField());
                final Measurement first = MeasureType.WEIGHT.createMeasurement(cursor);
                WeightChartActivity.this.wcImage.setDays(new Long((System.currentTimeMillis() - first.getTimestamp().getTime())
                        / (1000 * 60 * 60 * 24)).intValue());
                cursor.close();
                db.close();
                refreshAll();
            }
        });
    }

    private void addButton(final int rId, final int daysToDisplay) {
        final Button button = (Button) findViewById(rId);
        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                WeightChartActivity.this.wcImage.setDays(daysToDisplay);
                refreshAll();
            }
        });
    }

    private void refreshAll() {
        Log.d(MeasureActivity.TAG, "WeightChartActivity:refreshDataAndGraph");
        setDisplayField();
        this.wcImage.refreshData();
        final ImageView image = (ImageView) findViewById(R.id.testy_img);
        image.setImageBitmap(this.wcImage.refreshGraph());
    }

    private void setDisplayField() {
        this.wcImage.setDisplayField(UserPreferences.getDisplayField(this));
        final TextView chartTitle = (TextView) findViewById(R.id.chart_title);
        chartTitle.setText(this.wcImage.getDisplayField().getLabel(this));
    }

}
