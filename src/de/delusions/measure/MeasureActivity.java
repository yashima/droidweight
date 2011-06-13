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

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import de.delusions.measure.activities.prefs.PrefItem;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.components.InputRecorder;
import de.delusions.measure.database.MeasureCursorAdapter;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class MeasureActivity extends ListActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "MeasureActivity";
    private static final int ACTIVITY_EDIT = 1;
    private InputRecorder recorder;
    private SqliteHelper sqliteHelper;
    private Button set;
    private MeasureType field;
    private Cursor valuesCursor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight);

        if (this.sqliteHelper != null) {
            Log.d(TAG, "sqliteHelper exists in onCreate() ain't that fishy");
        }
        this.sqliteHelper = new SqliteHelper(this);
        this.sqliteHelper.open();

        this.field = UserPreferences.getDisplayField(this);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        this.recorder = (InputRecorder) findViewById(R.id.input);
        this.set = (Button) findViewById(R.id.set);
        setButtonText();

        this.set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createEntry();
            }
        });
        refreshListView();
        registerForContextMenu(getListView());
    }

    private void setButtonText() {
        final String buttonFormat = getResources().getString(R.string.button_set_current);
        final String label = getResources().getString(this.field.getLabelId());
        final String unit = this.field.getUnit().retrieveUnitName(this);
        this.set.setText(String.format(buttonFormat, label, unit));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.context_menu_delete:
            this.sqliteHelper.deleteNote(info.id);
            refreshListView();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        editItem(id);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return MeasureTabs.basicMenu(this, item) || super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        refreshListView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        refreshListView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        this.sqliteHelper.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void createEntry() {
        Log.d(TAG, "creating new entry for " + this.recorder.getCurrent());
        this.sqliteHelper.createMeasure(this.recorder.getCurrent());
        refreshListView();
    }

    private void editItem(long rowId) {
        final Intent i = new Intent(this, MeasureEdit.class);
        i.putExtra(SqliteHelper.KEY_ROWID, rowId);
        i.putExtra(MeasureEdit.EDIT_TYPE, this.field);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    public boolean refreshListView() {
        final boolean result;
        if (this.valuesCursor != null && !this.valuesCursor.isClosed()) {
            this.valuesCursor.requery();
        } else {
            this.valuesCursor = this.sqliteHelper.fetchAll(this.field);
            startManagingCursor(this.valuesCursor);
            final MeasureCursorAdapter measures = new MeasureCursorAdapter(this, this.valuesCursor, this.field);
            setListAdapter(measures);
        }
        refreshInputRecorder(this.valuesCursor);
        result = true;
        return result;
    }

    private void refreshInputRecorder(final Cursor cursor) {
        final Measurement lastMeasure;
        if (cursor.getCount() != 0) {
            final Cursor lastCursor = this.sqliteHelper.fetchLast(this.field);
            lastMeasure = this.field.createMeasurement(lastCursor);
            lastCursor.close();
        } else {
            lastMeasure = new Measurement(0, this.field, true, null);
        }
        Log.d(TAG, "refreshInputRecorder " + lastMeasure);
        this.recorder.setCurrent(lastMeasure);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefItem.DISPLAY_MEASURE.getKey())) {
            Log.d(TAG, "onSharedPreferenceChanged " + key);
            this.field = UserPreferences.getDisplayField(this);
            setButtonText();
            this.valuesCursor.close();
            refreshListView();
        }
    }

}