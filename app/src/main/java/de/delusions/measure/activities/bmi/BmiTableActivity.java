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
package de.delusions.measure.activities.bmi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.MeasureTabs;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.components.MeasureDisplay;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class BmiTableActivity extends Activity {

    private static final int UPPER_BOUND = 100;
    private static final int LOWER_BOUND = 0;
    private static final String TAG = BmiTableActivity.class.getSimpleName();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate BmiTableActivity");
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        setContentView(R.layout.activity_bmi);

        populateDataLabels();

        final Button calc = (Button) findViewById(R.id.calculator);
        calc.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                startCalculator();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        populateDataLabels();
    }

    private void populateDataLabels() {
        final SqliteHelper mDbHelper = new SqliteHelper(this);

        if (!mDbHelper.isEmptyWeight()) {
            final StatisticsFactory stats = new StatisticsFactory(mDbHelper, this);

            for (final BMI bmi : BMI.values()) {
                setBMIText(bmi);
            }
            setText(R.id.stat_starting, stats.getStartingWeight());
            setText(R.id.stat_current, stats.getLastWeight());
            setText(R.id.stat_goal, stats.getGoal());
            setText(R.id.stat_loss, stats.calculateLoss(), getResources().getString(R.string.stat_gain));
            setText(R.id.stat_togo, stats.calculateTogo());
            setTextBmi(R.id.stat_bmi, stats.calculateCurrentBmi());

            if (UserPreferences.isEnabled(MeasureType.WAIST, this)) {
                setText(R.id.stat_wthr, stats.calculateWtHR());
            } else {
                findViewById(R.id.stat_wthr).setVisibility(View.GONE);
            }
        }
        mDbHelper.close();
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        return MeasureTabs.basicMenu(this, item) || super.onMenuItemSelected(featureId, item);
    }

    private void startCalculator() {
        startActivity(new Intent(this, BmiCalcActivity.class));
    }

    private void setText(final int textId, final Measurement measurement, final String alternateLabel) {
        final MeasureDisplay text = (MeasureDisplay) findViewById(textId);
        text.display(measurement, alternateLabel);
    }

    private void setText(final int textId, final Measurement measurement) {
        Log.d(MeasureActivity.TAG, "BmiTableActivity.setText: " + measurement);
        final MeasureDisplay text = (MeasureDisplay) findViewById(textId);
        text.display(measurement);
    }

    private void setText(final int textId, final float value) {
        Log.d(MeasureActivity.TAG, "BmiTableActivity.setText: " + value);
        final MeasureDisplay text = (MeasureDisplay) findViewById(textId);
        text.display(value);
    }

    private void setTextBmi(final int textId, final float value) {
        final BMI bmi = BMI.getBmi(value, this);
        final MeasureDisplay text = (MeasureDisplay) findViewById(textId);
        if (bmi != null) {
            text.display(value, bmi.getColor(this));
        } else {
            text.display(value);
        }
    }

    private void setBMIText(final BMI bmi) {
        final MeasureDisplay text = (MeasureDisplay) findViewById(bmi.getId());
        final int[] range = getResources().getIntArray(bmi.getRangeId());
        final StringBuffer result = new StringBuffer();
        if (range[0] <= LOWER_BOUND) {
            result.append("< ").append(range[1]);
        } else if (range[1] >= UPPER_BOUND) {
            result.append("> ").append(range[0]);
        } else {
            result.append(range[0]).append(" - ").append(range[1]);
        }
        text.display(result.toString(), bmi.getColor(this));
    }
}
