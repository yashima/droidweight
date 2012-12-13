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

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.MeasureCursorAdapter;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class MeasureEdit extends Activity implements OnDateSetListener,OnTimeSetListener {

    public static final String EDIT_TYPE = "type";



    private Measurement measure;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MeasureActivity.TAG,"onCreate MeasureEdit");
        setContentView(R.layout.activity_edit);
        retrieveMeasureFromExtras(savedInstanceState);
        finishOnMissingMeasure();
        populateUI();
        addConfirmButtonOnClickListener();
        addShowDatePickerButtonOnClickListener();
        addShowTimePickerButtonOnClickListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SqliteHelper.KEY_ROWID, this.measure.getId());
        outState.putSerializable(EDIT_TYPE, this.measure.getField());
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        retrieveMeasureFromExtras(savedInstanceState);
        populateUI();
    }

    private void addShowDatePickerButtonOnClickListener() {
        final Button showDatePickerButton = (Button) findViewById(R.id.entryDate);
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                executeShowDatePickerButtonOnClick();
            }
        });
    }

    private void addShowTimePickerButtonOnClickListener() {
        final Button showDatePickerButton = (Button) findViewById(R.id.entryTime);
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                executeShowTimePickerButtonOnClick();
            }
        });
    }

    private void executeShowDatePickerButtonOnClick() {
        Log.i(MeasureActivity.TAG,"MeasureEdit:executeShowDatePickerButtonOnClick");
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(this.measure.getTimestamp());
        final int year = timestamp.get(Calendar.YEAR);
        final int month = timestamp.get(Calendar.MONTH);
        final int dayOfMonth = timestamp.get(Calendar.DAY_OF_MONTH);
        final DatePickerDialog datePickerDialog = new DatePickerDialog(this, this, year, month, dayOfMonth);
        datePickerDialog.show();
    }

    private void executeShowTimePickerButtonOnClick() {
        Log.i(MeasureActivity.TAG,"MeasureEdit:executeShowTimePickerButtonOnClick");
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(this.measure.getTimestamp());
        final int hour = timestamp.get(Calendar.HOUR_OF_DAY);
        final int minute = timestamp.get(Calendar.MINUTE);
        final TimePickerDialog datePickerDialog = new TimePickerDialog(this, this, hour, minute,true);
        datePickerDialog.show();
    }

    private void addConfirmButtonOnClickListener() {
        final Button confirmButton = (Button) findViewById(R.id.ok);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                executeConfirmButtonOnClick();
            }
        });
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

    private void populateUI(){
        if(this.measure!=null){
            populateValueEdit();
            populateDateButton();
            populateTimeButton();
            populateUnitLabel();
            populateTitle();
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

    private void populateDateButton() {
        final TextView showDatePickerButton = (TextView) findViewById(R.id.entryDate);
        showDatePickerButton.setText(MeasureCursorAdapter.DATE_FORMAT.format(this.measure.getTimestamp()));
    }

    private void populateTimeButton() {
        final TextView timePickerButton = (TextView) findViewById(R.id.entryTime);
        timePickerButton.setText(MeasureCursorAdapter.TIME_FORMAT.format(this.measure.getTimestamp()));
    }

    private void retrieveMeasureFromExtras(Bundle savedInstanceState) {
        final Long mRowId = retrieveRowIdFromExtras(savedInstanceState);
        final MeasureType field = retrieveMeasureFieldFromExtras(savedInstanceState);
        this.measure = retrieveMeasureFromDatabase(mRowId, field);
    }



    private Long retrieveRowIdFromExtras(Bundle savedInstanceState) {
        Long mRowId = savedInstanceState == null ? null : (Long) savedInstanceState.getSerializable(SqliteHelper.KEY_ROWID);
        if (mRowId == null) {
            final Bundle extras = getIntent().getExtras();
            mRowId = extras != null ? extras.getLong(SqliteHelper.KEY_ROWID) : null;
            Log.d(MeasureActivity.TAG, "MeasureEdit:retrieveRowIdFromExtras:" + mRowId);
        }
        return mRowId;
    }



    private MeasureType retrieveMeasureFieldFromExtras(Bundle savedInstanceState) {
        MeasureType field = savedInstanceState == null ? null : (MeasureType) savedInstanceState.getSerializable(EDIT_TYPE);
        if (field == null) {
            final Bundle extras = getIntent().getExtras();
            field = extras != null ? (MeasureType) extras.getSerializable(EDIT_TYPE) : null;
            Log.d(MeasureActivity.TAG, "MeasureEdit:retrieveMeasureFieldFromExtras:" + field);
        }
        return field;
    }



    private Measurement retrieveMeasureFromDatabase(Long mRowId, MeasureType field) {
        final Measurement result;
        final SqliteHelper mDbHelper = new SqliteHelper(this);
        mDbHelper.open();
        final Cursor cursor = mDbHelper.fetchById(mRowId);
        if (cursor.getCount() != 0 && field!=null) {
            result = field.createMeasurement(cursor);
        } else {
            Log.w(MeasureActivity.TAG,"MeasureEdit:retrieveMeasureFromDatabase:measure not found");
            result = null;
        }

        cursor.close();
        mDbHelper.close();
        return result;
    }

    private void finishOnMissingMeasure() {
        if(this.measure==null){
            Log.w(MeasureActivity.TAG,"MeasureEdit:MeasureEdit:finishOnMissingMeasure: No measure found for editing");
            setResult(MeasureActivity.RESULT_FAILURE);
            finish();
        } else {
            Log.d(MeasureActivity.TAG,"MeasureEdit:finishOnMissingMeasure: Measure found. Continuing.");
        }
    }


    private void saveMeasurement(Measurement toSave)  {
        Log.d(MeasureActivity.TAG,"saveMeasurement "+toSave);
        final SqliteHelper mDbHelper = new SqliteHelper(this);
        mDbHelper.open();
        mDbHelper.updateMeasure(toSave.getId(),toSave);
        mDbHelper.close();
    }

    private void updateMeasureValueFromInput() throws MeasurementException {
        Log.d(MeasureActivity.TAG,"updateMeasureValueFromInput");
        final EditText valueEdit = retrieveMeasureValueEditView();
        final String strValue = valueEdit.getText().toString();
        this.measure.parseAndSetValue(strValue, UserPreferences.isMetric(this));
    }


    private EditText retrieveMeasureValueEditView() {
        return (EditText) findViewById(R.id.input);
    }


    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.measure.getTimestamp());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        this.measure.setTimestamp(calendar.getTime());
        populateDateButton();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.measure.getTimestamp());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        this.measure.setTimestamp(calendar.getTime());
        populateTimeButton();
    }


}
