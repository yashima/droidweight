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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.components.DateTimeManager;
import de.delusions.measure.database.SqliteHelper;
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
            populateComment();
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

    private void populateComment() {
        final EditText commentEdit = (EditText) findViewById(R.id.comment);
        commentEdit.setText(this.measure.getComment());
    }

    private void retrieveMeasureFromExtras(final Bundle savedInstanceState) {
        this.measure = new MeasureIntentHelper(this, getIntent(), savedInstanceState).retrieveMeasure();
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
        final EditText commentEdit = (EditText) findViewById(R.id.comment);
        this.measure.setComment(commentEdit.getText().toString());
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

    public static Intent createIntent(final Context context, final long rowId) {
        final Intent i = new Intent(context, MeasureEdit.class);
        i.putExtra(SqliteHelper.KEY_ROWID, rowId);
        i.putExtra(EDIT_TYPE, UserPreferences.getDisplayField(context));
        return i;
    }

}
