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
package de.delusions.measure;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import de.delusions.measure.activities.bmi.BmiTable;
import de.delusions.measure.activities.chart.WeightChartActivity;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteExport;
import de.delusions.measure.database.SqliteManagement;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.MeasurementException;

public class MeasureTabs extends TabActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabhost);

        MeasureType.initializeTypeMap(this);
        final Resources res = getResources();
        final TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        Intent intent;

        intent = new Intent().setClass(this, MeasureActivity.class);
        spec = tabHost.newTabSpec("kg").setIndicator(res.getString(R.string.tab_weight), res.getDrawable(R.drawable.ic_tab_kg)).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, BmiTable.class);
        spec = tabHost.newTabSpec("bmi").setIndicator(res.getString(R.string.tab_stats), res.getDrawable(R.drawable.ic_tab_bmi)).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, WeightChartActivity.class);
        spec = tabHost.newTabSpec("graph").setIndicator(res.getString(R.string.tab_graph), res.getDrawable(R.drawable.ic_tab_graph))
                .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.basic_menu, menu);
        return true;
    }

    public static boolean basicMenu(Activity a, MenuItem item) {
        final Intent i;
        switch (item.getItemId()) {
        case R.id.basic_menu_settings:
            i = new Intent(a, UserPreferences.class);
            a.startActivityForResult(i, 0);
            return true;
        case R.id.basic_menu_create_entry:
            if (UserPreferences.isFastInput(a)) {
                i = new Intent(a, MeasureFastEdit.class);
            } else {
                i = createMeasureIntent(a, UserPreferences.getDisplayField(a));
            }
            a.startActivityForResult(i, 0);
            return true;
        case R.id.basic_menu_homepage:
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.workreloaded.com/software/droid-weight"));
            a.startActivity(i);
            return true;
        case R.id.basic_menu_showtype:
            final AlertDialog alert = getChoice(a);
            alert.show();
            return true;
        case R.id.basic_menu_export:
            try {
                new SqliteExport(a, true).execute();
            } catch (final MeasurementException e) {
                e.createToast(a, "export");
            }
            return true;
        case R.id.basic_menu_import:
            try {
                new SqliteExport(a, false).execute();
            } catch (final MeasurementException e) {
                e.createToast(a, "import");
            }
            return true;
        case R.id.basic_menu_deleteall:
            SqliteManagement.clearDatabase(a).show();
        }
        return false;
    }

    public static Intent createMeasureIntent(Context ctx, MeasureType type) {
        final Intent i;
        i = new Intent(ctx, MeasureCreateActivity.class);
        i.putExtra(MeasureEdit.EDIT_TYPE, type);
        return i;
    }

    private static AlertDialog getChoice(final Activity a) {
        final List<MeasureType> tracked = MeasureType.getEnabledTypes(a);
        final List<String> list = new ArrayList<String>();
        for (final MeasureType field : tracked) {
            list.add(a.getResources().getString(field.getLabelId()));
        }
        final String[] items = list.toArray(new String[0]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.menu_showtype);
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {
                UserPreferences.setDisplayField(a, tracked.get(item));
                dialog.dismiss();
            }
        });
        final AlertDialog alert = builder.create();
        return alert;
    }

}
