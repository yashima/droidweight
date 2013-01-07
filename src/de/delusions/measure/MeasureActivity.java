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

import android.app.Activity;
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
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class MeasureActivity extends ListActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "MeasureActivity";
    public static final int ACTIVITY_EDIT = 1;
    public static final int ACTIVITY_PREVIOUS_COMMENT = 2;
    public static final int RESULT_FAILURE = Activity.RESULT_FIRST_USER + 1;
    private InputRecorder recorder;

    private Button set;
    private MeasureType field;

    private SqliteHelper valuesDb;
    private Cursor valuesCursor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MeasureActivity.TAG, "onCreate MeasureActivity");
        setContentView(R.layout.activity_weight);

        this.field = UserPreferences.getDisplayField(this);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        this.recorder = (InputRecorder) findViewById(R.id.input);
        this.set = (Button) findViewById(R.id.set);
        setButtonText();

        this.set.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
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
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.context_menu_delete:
            deleteEntry(info.id);
            refreshListView();
            return true;
        case R.id.context_menu_previous_comment:
            addPreviousComment(info.id);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
        super.onListItemClick(l, v, position, id);
        editItem(id);
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        return MeasureTabs.basicMenu(this, item) || super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        if (this.valuesCursor != null && !this.valuesCursor.isClosed()) {
            this.valuesCursor.close();
            this.valuesDb.close();
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void createEntry() {
        Log.d(TAG, "creating new entry for " + this.recorder.getCurrent());
        final SqliteHelper db = new SqliteHelper(this);
        db.createMeasure(this.recorder.getCurrent());
        db.close();
        refreshListView();
    }

    private void deleteEntry(final long rowId) {
        final SqliteHelper db = new SqliteHelper(this);
        db.deleteNote(rowId);
        db.close();
    }

    private void editItem(final long rowId) {
        final Intent i = new Intent(this, MeasureEdit.class);
        i.putExtra(SqliteHelper.KEY_ROWID, rowId);
        i.putExtra(MeasureEdit.EDIT_TYPE, this.field);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    private void addPreviousComment(final long rowId) {
        final Intent i = new Intent(this, PreviousCommentActivity.class);
        i.putExtra(SqliteHelper.KEY_ROWID, rowId);
        i.putExtra(MeasureEdit.EDIT_TYPE, this.field);
        startActivityForResult(i, ACTIVITY_PREVIOUS_COMMENT);
    }

    public boolean refreshListView() {
        if (this.valuesCursor == null || this.valuesCursor.isClosed()) {
            this.valuesDb = new SqliteHelper(this);
            this.valuesCursor = this.valuesDb.fetchAll(this.field);
            final MeasureCursorAdapter measures = new MeasureCursorAdapter(this, this.valuesCursor, this.field);
            setListAdapter(measures);
        } else {
            this.valuesCursor.requery();
        }
        refreshInputRecorder(this.valuesCursor.getCount() != 0);
        return true;
    }

    private void refreshInputRecorder(final boolean entriesExist) {
        final Measurement lastMeasure;
        if (entriesExist) {
            final SqliteHelper db = new SqliteHelper(this);
            final Cursor lastCursor = db.fetchLast(this.field);
            lastMeasure = this.field.createMeasurement(lastCursor);
            lastCursor.close();
            db.close();
        } else {
            lastMeasure = new Measurement();
            lastMeasure.setField(this.field);
        }
        Log.d(TAG, "refreshInputRecorder " + lastMeasure);
        this.recorder.setCurrent(lastMeasure);
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals(PrefItem.DISPLAY_MEASURE.getKey())) {
            Log.d(TAG, "onSharedPreferenceChanged " + key);
            this.field = UserPreferences.getDisplayField(this);
            setButtonText();
            // this.valuesCursor.close();
            refreshListView();
        }
    }

}