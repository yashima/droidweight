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
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.MeasureCursorAdapter;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class MeasureEdit extends Activity {

    public static final String EDIT_TYPE = "type";

    private SqliteHelper mDbHelper;

    private Long mRowId;
    private MeasureType field;
    private EditText input;
    private boolean createMode;
    private int spinnerPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        this.mDbHelper = new SqliteHelper(this);
        this.mDbHelper.open();
        this.input = (EditText) findViewById(R.id.input);

        retrieveExtras(savedInstanceState);
        setUnitLabel();
        setTitle();
        populateInput();
        createConfirmButton();
        manageFieldSpinner();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SqliteHelper.KEY_ROWID, this.mRowId);
        outState.putSerializable(EDIT_TYPE, this.field);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateInput();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mDbHelper.close();
    }

    private void createConfirmButton() {
        final Button confirmButton = (Button) findViewById(R.id.ok);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                try {
                    saveMeasurement();
                    finish();
                } catch (final MeasurementException e) {
                    e.createToast(MeasureEdit.this, "confirmButton");
                }
            }
        });
    }

    private void retrieveExtras(Bundle savedInstanceState) {
        this.mRowId = savedInstanceState == null ? null : (Long) savedInstanceState.getSerializable(SqliteHelper.KEY_ROWID);
        if (this.mRowId == null) {
            final Bundle extras = getIntent().getExtras();
            this.mRowId = extras != null ? extras.getLong(SqliteHelper.KEY_ROWID) : null;
            Log.d(MeasureActivity.TAG, "retrieveExtras " + this.mRowId);
        }
        this.field = savedInstanceState == null ? null : (MeasureType) savedInstanceState.getSerializable(EDIT_TYPE);
        if (this.field == null) {
            final Bundle extras = getIntent().getExtras();
            this.field = extras != null ? (MeasureType) extras.getSerializable(EDIT_TYPE) : null;
            Log.d(MeasureActivity.TAG, "retrieveExtras " + this.field);
        }
        this.createMode = this.mRowId == -1;
    }

    private void setTitle() {
        if (this.createMode) {
            setTitle(getResources().getString(R.string.activity_createmeasure));
        } else {
            final String label = this.field == null ? null : this.field.getLabel(this);
            setTitle(getResources().getString(R.string.activity_editmeasure) + " " + label);
        }
    }

    private void populateInput() {
        final TextView date = (TextView) findViewById(R.id.entryDate);
        if (!this.createMode) {
            final Cursor cursor = this.mDbHelper.fetchById(this.mRowId);
            if (cursor.getCount() != 0) {
                final Measurement measurement = this.field.createMeasurement(cursor);
                this.input.setText(measurement.prettyPrint(this));
                date.setText(MeasureCursorAdapter.DATEFORMAT.format(measurement.getTimestamp()));
            }
            cursor.close();
        } else {
            date.setVisibility(View.GONE);
        }
    }

    public void setUnitLabel() {
        final TextView unit = (TextView) findViewById(R.id.unit);
        unit.setText(this.field.getUnit().retrieveUnitName(this));
    }

    private void saveMeasurement() throws MeasurementException {
        final String strValue = this.input.getText().toString();
        final Measurement measurement = new Measurement(null, strValue, this.field, UserPreferences.isMetric(this), new Date());
        measurement.setField(this.field);
        if (this.createMode) {
            final long id = this.mDbHelper.createMeasure(measurement);
            if (id > 0) { // TODO may not be needed
                this.mRowId = id;
            }
        } else {
            this.mDbHelper.updateMeasure(this.mRowId, measurement);
        }
    }

    public void manageFieldSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.chooseType);
        if (this.createMode) {
            spinnerFill(spinner);
        } else {
            spinner.setVisibility(View.GONE);
        }
    }

    protected void spinnerFill(final Spinner spinner) {
        final List<MeasureType> types = MeasureType.getEnabledTypes(this);
        final List<String> spinnerLabels = createLabels(types);
        Log.d(MeasureActivity.TAG, "spinnerFill in edit " + spinnerLabels);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(this.spinnerPosition);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                MeasureEdit.this.field = types.get(pos);
                setUnitLabel();
                UserPreferences.setDisplayField(MeasureEdit.this, MeasureEdit.this.field);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
            }
        });
    }

    protected int getSpinnerPosition() {
        return this.spinnerPosition;
    }

    protected List<String> createLabels(final List<MeasureType> types) {
        final List<String> spinnerLabels = new ArrayList<String>();
        this.spinnerPosition = 0;
        boolean found = false;
        for (final MeasureType field : types) {
            spinnerLabels.add(getResources().getString(field.getLabelId()));
            found = found || field.equals(MeasureEdit.this.field);
            if (!found) {
                this.spinnerPosition++;
            }
        }
        return spinnerLabels;
    }
}
