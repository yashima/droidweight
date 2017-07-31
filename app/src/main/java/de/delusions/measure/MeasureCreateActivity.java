/*
   Copyright 2012 Sonja Pieper

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
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.components.DateTimeManager;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class MeasureCreateActivity extends Activity implements OnDateSetListener, OnTimeSetListener {
    public static final String EDIT_TYPE = "type";

    private Measurement measure;

    private int spinnerPosition = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MeasureActivity.TAG, "onCreate MeasureCreateActivity");
        setContentView(R.layout.activity_create);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        this.measure = new Measurement();
        this.measure.setField(UserPreferences.getDisplayField(this));
        retrieveExtras(savedInstanceState);

        createConfirmButton();
        manageFieldSpinner();
        populateUI();
        DateTimeManager.addShowDatePickerButtonOnClickListener(this.measure, this, this);
        DateTimeManager.addShowTimePickerButtonOnClickListener(this.measure, this, this);
        findViewById(R.id.input).requestFocus();
    }

    private void populateUI() {
        setUnitLabel();
        setTitle();
        DateTimeManager.populateDateButton(this.measure, this);
        DateTimeManager.populateTimeButton(this.measure, this);
    }

    private void createConfirmButton() {
        final Button confirmButton = (Button) findViewById(R.id.ok);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                executeConfirmButtonOnClick();
            }
        });
    }

    private void executeConfirmButtonOnClick() {
        setResult(RESULT_OK);
        try {
            updateMeasureValueFromInput();
            saveMeasurement(MeasureCreateActivity.this.measure);
            finish();
        } catch (final MeasurementException e) {
            e.createToast(MeasureCreateActivity.this, "confirmButton");
        }
    }

    private void retrieveExtras(final Bundle savedInstanceState) {
        MeasureType field = savedInstanceState == null ? null : (MeasureType) savedInstanceState.getSerializable(EDIT_TYPE);
        if (field == null) {
            final Bundle extras = getIntent().getExtras();
            field = extras != null ? (MeasureType) extras.getSerializable(EDIT_TYPE) : null;
            Log.d(MeasureActivity.TAG, "retrieveExtras " + field);
        } else {
            this.measure.setField(field);
        }
    }

    private void setTitle() {
        setTitle(getResources().getString(R.string.activity_createmeasure));
    }

    private void setUnitLabel() {
        final TextView unit = (TextView) findViewById(R.id.unit);
        unit.setText(this.measure.getField().getUnit().retrieveUnitName(this));
    }

    public void manageFieldSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.chooseType);
        spinnerFill(spinner);
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

            public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {
                MeasureCreateActivity.this.measure.setField(types.get(pos));
                setUnitLabel();
                UserPreferences.setDisplayField(MeasureCreateActivity.this, types.get(pos));
            }

            public void onNothingSelected(final AdapterView<?> parent) {
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
            found = found || field.equals(this.measure.getField());
            if (!found) {
                this.spinnerPosition++;
            }
        }
        return spinnerLabels;
    }

    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        this.measure.updateDate(year, monthOfYear, dayOfMonth);
        DateTimeManager.populateDateButton(this.measure, this);
    }

    public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
        this.measure.updateTime(hourOfDay, minute);
        DateTimeManager.populateTimeButton(this.measure, this);
    }

    private EditText retrieveMeasureValueEditView() {
        return (EditText) findViewById(R.id.input);
    }

    private void saveMeasurement(final Measurement toSave) {
        Log.d(MeasureActivity.TAG, "saveMeasurement " + toSave);
        final SqliteHelper mDbHelper = new SqliteHelper(this);
        mDbHelper.createMeasure(this.measure);
        mDbHelper.close();
    }

    private void updateMeasureValueFromInput() throws MeasurementException {
        Log.d(MeasureActivity.TAG, "updateMeasureValueFromInput");
        final EditText valueEdit = retrieveMeasureValueEditView();
        final String strValue = valueEdit.getText().toString();
        this.measure.parseAndSetValue(strValue, UserPreferences.isMetric(this));
        final EditText commentEdit = (EditText) findViewById(R.id.comment);
        this.measure.setComment(commentEdit.getText().toString());
    }
}
