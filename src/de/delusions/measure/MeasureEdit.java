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

import java.util.Calendar;

import android.app.*;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.components.DateTimeManager;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class MeasureEdit extends Activity implements OnDateSetListener, OnTimeSetListener {

    public static final String EDIT_TYPE = "type";

    private Measurement measure;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MeasureActivity.TAG, "onCreate MeasureEdit");
        setContentView(R.layout.activity_edit);
        retrieveMeasureFromExtras(savedInstanceState);
        finishOnMissingMeasure();
        populateUI();
        addConfirmButtonOnClickListener();
        DateTimeManager.addShowDatePickerButtonOnClickListener(this.measure, this, this);
        DateTimeManager.addShowTimePickerButtonOnClickListener(this.measure, this, this);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SqliteHelper.KEY_ROWID, this.measure.getId());
        outState.putSerializable(EDIT_TYPE, this.measure.getField());
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        retrieveMeasureFromExtras(savedInstanceState);
        populateUI();
    }

    private void addShowDatePickerButtonOnClickListener() {
        final Button showDatePickerButton = (Button) findViewById(R.id.entryDate);
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                executeShowDatePickerButtonOnClick();
            }
        });
    }

    private void addShowTimePickerButtonOnClickListener() {
        final Button showDatePickerButton = (Button) findViewById(R.id.entryTime);
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                executeShowTimePickerButtonOnClick();
            }
        });
    }

    private void addConfirmButtonOnClickListener() {
        final Button confirmButton = (Button) findViewById(R.id.ok);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                executeConfirmButtonOnClick();
            }
        });
    }

    private void executeShowDatePickerButtonOnClick() {
        Log.i(MeasureActivity.TAG, "MeasureEdit:executeShowDatePickerButtonOnClick");
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(this.measure.getTimestamp());
        final int year = timestamp.get(Calendar.YEAR);
        final int month = timestamp.get(Calendar.MONTH);
        final int dayOfMonth = timestamp.get(Calendar.DAY_OF_MONTH);
        final DatePickerDialog datePickerDialog = new DatePickerDialog(this, this, year, month, dayOfMonth);
        datePickerDialog.show();
    }

    private void executeShowTimePickerButtonOnClick() {
        Log.i(MeasureActivity.TAG, "MeasureEdit:executeShowTimePickerButtonOnClick");
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(this.measure.getTimestamp());
        final int hour = timestamp.get(Calendar.HOUR_OF_DAY);
        final int minute = timestamp.get(Calendar.MINUTE);
        final TimePickerDialog datePickerDialog = new TimePickerDialog(this, this, hour, minute, true);
        datePickerDialog.show();
    }

    private void executeConfirmButtonOnClick() {
        setResult(RESULT_OK);
        try {
            updateMeasureValueFromInput();
            saveMeasurement(this.measure);
            finish();
        } catch (final MeasurementException e) {
            e.createToast(this, "confirmButton");
        }
    }

    private void populateUI() {
        if (this.measure != null) {
            populateValueEdit();
            populateUnitLabel();
            populateTitle();
            DateTimeManager.populateDateButton(this.measure, this);
            DateTimeManager.populateTimeButton(this.measure, this);
        }
    }

    private void populateTitle() {
        final String label = this.measure.getField() == null ? null : this.measure.getField().getLabel(this);
        setTitle(getResources().getString(R.string.activity_editmeasure) + " " + label);
    }

    private void populateUnitLabel() {
        final TextView unit = (TextView) findViewById(R.id.unit);
        unit.setText(this.measure.getField().getUnit().retrieveUnitName(this));
    }

    private void populateValueEdit() {
        final EditText valueEdit = retrieveMeasureValueEditView();
        valueEdit.setText(this.measure.prettyPrint(this));
    }

    private void retrieveMeasureFromExtras(final Bundle savedInstanceState) {
        final Long mRowId = retrieveRowIdFromExtras(savedInstanceState);
        final MeasureType field = retrieveMeasureFieldFromExtras(savedInstanceState);
        this.measure = retrieveMeasureFromDatabase(mRowId, field);
    }

    private Long retrieveRowIdFromExtras(final Bundle savedInstanceState) {
        Long mRowId = savedInstanceState == null ? null : (Long) savedInstanceState.getSerializable(SqliteHelper.KEY_ROWID);
        if (mRowId == null) {
            final Bundle extras = getIntent().getExtras();
            mRowId = extras != null ? extras.getLong(SqliteHelper.KEY_ROWID) : null;
            Log.d(MeasureActivity.TAG, "MeasureEdit:retrieveRowIdFromExtras:" + mRowId);
        }
        return mRowId;
    }

    private MeasureType retrieveMeasureFieldFromExtras(final Bundle savedInstanceState) {
        MeasureType field = savedInstanceState == null ? null : (MeasureType) savedInstanceState.getSerializable(EDIT_TYPE);
        if (field == null) {
            final Bundle extras = getIntent().getExtras();
            field = extras != null ? (MeasureType) extras.getSerializable(EDIT_TYPE) : null;
            Log.d(MeasureActivity.TAG, "MeasureEdit:retrieveMeasureFieldFromExtras:" + field);
        }
        return field;
    }

    private Measurement retrieveMeasureFromDatabase(final Long mRowId, final MeasureType field) {
        final Measurement result;
        final SqliteHelper mDbHelper = new SqliteHelper(this);
        final Cursor cursor = mDbHelper.fetchById(mRowId);
        if (cursor.getCount() != 0 && field != null) {
            result = field.createMeasurement(cursor);
        } else {
            Log.w(MeasureActivity.TAG, "MeasureEdit:retrieveMeasureFromDatabase:measure not found");
            result = null;
        }

        cursor.close();
        mDbHelper.close();
        return result;
    }

    private void finishOnMissingMeasure() {
        if (this.measure == null) {
            Log.w(MeasureActivity.TAG, "MeasureEdit:MeasureEdit:finishOnMissingMeasure: No measure found for editing");
            setResult(MeasureActivity.RESULT_FAILURE);
            finish();
        } else {
            Log.d(MeasureActivity.TAG, "MeasureEdit:finishOnMissingMeasure: Measure found. Continuing.");
        }
    }

    private void saveMeasurement(final Measurement toSave) {
        Log.d(MeasureActivity.TAG, "saveMeasurement " + toSave);
        final SqliteHelper mDbHelper = new SqliteHelper(this);
        mDbHelper.updateMeasure(toSave.getId(), toSave);
        mDbHelper.close();
    }

    private void updateMeasureValueFromInput() throws MeasurementException {
        Log.d(MeasureActivity.TAG, "updateMeasureValueFromInput");
        final EditText valueEdit = retrieveMeasureValueEditView();
        final String strValue = valueEdit.getText().toString();
        this.measure.parseAndSetValue(strValue, UserPreferences.isMetric(this));
    }

    private EditText retrieveMeasureValueEditView() {
        return (EditText) findViewById(R.id.input);
    }

    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        this.measure.updateDate(year, monthOfYear, dayOfMonth);
        DateTimeManager.populateDateButton(this.measure, this);
    }

    public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
        this.measure.updateTime(hourOfDay, minute);
        DateTimeManager.populateTimeButton(this.measure, this);
    }

}
