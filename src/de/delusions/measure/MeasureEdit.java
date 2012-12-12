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

import java.text.ParseException;
import java.util.Date;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.MeasureCursorAdapter;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;
import de.delusions.measure.ment.MeasurementException.ErrorId;

public class MeasureEdit extends Activity {

    public static final String EDIT_TYPE = "type";

    private SqliteHelper mDbHelper;

    private Long mRowId;
    private MeasureType field;
    private EditText input;
    private Button date;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MeasureActivity.TAG,"onCreate MeasureEdit");
        setContentView(R.layout.activity_edit);

        this.mDbHelper = new SqliteHelper(this);
        this.mDbHelper.open();
        this.input = (EditText) findViewById(R.id.input);

        this.date = (Button) findViewById(R.id.entryDate);
        this.date.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                // try {
                // DatePickerDialog datePickerDialog = new DatePickerDialog(MeasureEdit.this, callBack, year, monthOfYear, dayOfMonth)
                // } catch (final MeasurementException e) {
                //     e.createToast(MeasureEdit.this, "confirmButton");
                //  }
            }
        });

        retrieveExtras(savedInstanceState);
        setUnitLabel();
        setTitle();
        populateInput();
        createConfirmButton();

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

    }

    private void setTitle() {

        final String label = this.field == null ? null : this.field.getLabel(this);
        setTitle(getResources().getString(R.string.activity_editmeasure) + " " + label);

    }

    private void populateInput() {

        final Cursor cursor = this.mDbHelper.fetchById(this.mRowId);
        if (cursor.getCount() != 0) {
            final Measurement measurement = this.field.createMeasurement(cursor);
            this.input.setText(measurement.prettyPrint(this));
            this.date.setText(MeasureCursorAdapter.DATEFORMAT.format(measurement.getTimestamp()));
        }
        cursor.close();

    }

    public void setUnitLabel() {
        final TextView unit = (TextView) findViewById(R.id.unit);
        unit.setText(this.field.getUnit().retrieveUnitName(this));
    }

    private void saveMeasurement() throws MeasurementException {
        final String strValue = this.input.getText().toString();
        final Measurement measurement = new Measurement(null, strValue, this.field, UserPreferences.isMetric(this), new Date());
        measurement.setField(this.field);
        measurement.setTimestamp(parseDateFromInput(this.date));

        this.mDbHelper.updateMeasure(this.mRowId, measurement);

    }



    private static Date parseDateFromInput(TextView dateView) throws MeasurementException {
        try {
            final String input = dateView.getText().toString();
            return MeasureCursorAdapter.DATEFORMAT.parse(input);
        } catch (final ParseException e){
            Log.e(MeasureActivity.TAG,"Could not parse date input");
            throw new MeasurementException(ErrorId.PARSEERROR_DATE);
        }
    }


}
