package de.delusions.measure;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class MeasureCreateActivity extends Activity {
    public static final String EDIT_TYPE = "type";

    private SqliteHelper mDbHelper;

    private MeasureType field;
    private EditText input;


    private int spinnerPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MeasureActivity.TAG,"onCreate MeasureCreateActivity");
        setContentView(R.layout.activity_create);

        this.mDbHelper = new SqliteHelper(this);
        this.mDbHelper.open();
        this.input = (EditText) findViewById(R.id.input);

        retrieveExtras(savedInstanceState);
        setUnitLabel();
        setTitle();
        createConfirmButton();
        manageFieldSpinner();
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EDIT_TYPE, this.field);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                    e.createToast(MeasureCreateActivity.this, "confirmButton");
                }
            }
        });
    }

    private void retrieveExtras(Bundle savedInstanceState) {
        this.field = savedInstanceState == null ? null : (MeasureType) savedInstanceState.getSerializable(EDIT_TYPE);
        if (this.field == null) {
            final Bundle extras = getIntent().getExtras();
            this.field = extras != null ? (MeasureType) extras.getSerializable(EDIT_TYPE) : null;
            Log.d(MeasureActivity.TAG, "retrieveExtras " + this.field);
        }
    }

    private void setTitle() {
        setTitle(getResources().getString(R.string.activity_createmeasure));
    }

    public void setUnitLabel() {
        final TextView unit = (TextView) findViewById(R.id.unit);
        unit.setText(this.field.getUnit().retrieveUnitName(this));
    }

    private void saveMeasurement() throws MeasurementException {
        final String strValue = this.input.getText().toString();
        final Measurement measurement = new Measurement(null, strValue, this.field, UserPreferences.isMetric(this), new Date());
        measurement.setField(this.field);
        measurement.setTimestamp(new Date());
        this.mDbHelper.createMeasure(measurement);
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

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                MeasureCreateActivity.this.field = types.get(pos);
                setUnitLabel();
                UserPreferences.setDisplayField(MeasureCreateActivity.this, MeasureCreateActivity.this.field);
            }


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
            found = found || field.equals(MeasureCreateActivity.this.field);
            if (!found) {
                this.spinnerPosition++;
            }
        }
        return spinnerLabels;
    }
}
