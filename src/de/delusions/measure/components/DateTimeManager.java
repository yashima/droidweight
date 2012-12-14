package de.delusions.measure.components;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;
import de.delusions.measure.database.MeasureCursorAdapter;
import de.delusions.measure.ment.Measurement;

public class DateTimeManager {


    public static void addShowDatePickerButtonOnClickListener(final Measurement measure,final Activity activity,final OnDateSetListener listener) {
        final Button showDatePickerButton = (Button) activity.findViewById(R.id.entryDate);
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                executeShowDatePickerButtonOnClick(measure,activity,listener);
            }
        });
    }

    public static void addShowTimePickerButtonOnClickListener(final Measurement measure,final Activity activity,final OnTimeSetListener listener) {
        final Button showDatePickerButton = (Button) activity.findViewById(R.id.entryTime);
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                executeShowTimePickerButtonOnClick(measure,activity,listener);
            }
        });
    }

    private static void executeShowDatePickerButtonOnClick(final Measurement measure,final Activity activity,final OnDateSetListener listener) {
        Log.i(MeasureActivity.TAG, "ComponentInitializer:executeShowDatePickerButtonOnClick");
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(measure.getTimestamp());
        final int year = timestamp.get(Calendar.YEAR);
        final int month = timestamp.get(Calendar.MONTH);
        final int dayOfMonth = timestamp.get(Calendar.DAY_OF_MONTH);
        final DatePickerDialog datePickerDialog = new DatePickerDialog(activity, listener, year, month, dayOfMonth);
        datePickerDialog.show();
    }


    private static void executeShowTimePickerButtonOnClick(final Measurement measure,final Activity activity,final OnTimeSetListener listener) {
        Log.i(MeasureActivity.TAG, "ComponentInitializer:executeShowTimePickerButtonOnClick");
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(measure.getTimestamp());
        final int hour = timestamp.get(Calendar.HOUR_OF_DAY);
        final int minute = timestamp.get(Calendar.MINUTE);
        final TimePickerDialog datePickerDialog = new TimePickerDialog(activity, listener, hour, minute, true);
        datePickerDialog.show();
    }

    public static void populateDateButton(Measurement measure,Activity activity) {
        final TextView showDatePickerButton = (TextView) activity.findViewById(R.id.entryDate);
        showDatePickerButton.setText(MeasureCursorAdapter.DATE_FORMAT.format(measure.getTimestamp()));
    }

    public static void populateTimeButton(Measurement measure,Activity activity) {
        final TextView timePickerButton = (TextView) activity.findViewById(R.id.entryTime);
        timePickerButton.setText(MeasureCursorAdapter.TIME_FORMAT.format(measure.getTimestamp()));
    }
}
