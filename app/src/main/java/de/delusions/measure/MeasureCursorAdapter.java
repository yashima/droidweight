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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.delusions.measure.R;
import de.delusions.measure.activities.bmi.StatisticsFactory;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class MeasureCursorAdapter extends CursorAdapter {

    public static final String HOUR_MINUTE = "HH:mm";
    public static final String DAY_MONTH_YEAR = "dd/MM/yyyy";

    // only ok because this app is not multithreaded!
    public static final SimpleDateFormat TIME_AND_DATE_FORMAT = new SimpleDateFormat(String.format("%s, %s", HOUR_MINUTE, DAY_MONTH_YEAR));

    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(HOUR_MINUTE);
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DAY_MONTH_YEAR);

    private final Measurement height;
    private final Context ctx;
    private final MeasureType displayField;
    private final String labelText;

    public MeasureCursorAdapter(final Context context, final Cursor c, final MeasureType displayField) {
        super(context, c, false);
        this.height = UserPreferences.getHeight(context);
        this.ctx = context;
        this.displayField = displayField;
        this.labelText = this.ctx.getResources().getString(displayField.getLabelId()) + ":";
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final Measurement measurement = this.displayField.createMeasurement(cursor);
        final String date = cursor.getString(cursor.getColumnIndex(SqliteHelper.KEY_DATE));
        final float percent = calculatePercentChange(cursor, measurement);
        displayMeasurement(view, measurement);
        displayDate(view, date);
        displayBMI(view, measurement);
        displayPercentChange(view, percent);
        displayComment(context, view, measurement);

    }

    private float calculatePercentChange(final Cursor cursor, final Measurement measurement) {
        final float percent;
        if (!cursor.isLast()) {
            cursor.moveToNext();
            final Measurement previous = this.displayField.createMeasurement(cursor);
            cursor.moveToPrevious();
            percent = measurement.getPercentDifference(previous);
        } else {
            percent = 0;
        }
        return percent;
    }

    private void displayPercentChange(final View view, final float percent) {
        // final TextView t = (TextView) view.findViewById(R.id.percentchange);
        // t.setText((percent > 0 ? "+" : "") + percent + "%");
        final ImageView i = (ImageView) view.findViewById(R.id.change_arrow);
        final Resources resources = view.getContext().getResources();
        if (percent > 0) {
            i.setImageDrawable(resources.getDrawable(android.R.drawable.arrow_up_float));
        } else if (percent < 0) {
            i.setImageDrawable(resources.getDrawable(android.R.drawable.arrow_down_float));
        } else {
            i.setImageDrawable(resources.getDrawable(android.R.drawable.radiobutton_off_background));
        }
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View view = LayoutInflater.from(context).inflate(R.layout.layout_weight_row, parent, false);
        return view;
    }

    /**
     * Formats and displays the date
     * 
     * @param view
     * @param circumference
     */
    private void displayDate(final View view, final String strDate) {
        final long timestamp = Long.parseLong(strDate);
        final String formattedDate = TIME_AND_DATE_FORMAT.format(new Date(timestamp));
        final TextView t2 = (TextView) view.findViewById(R.id.date);
        t2.setText(formattedDate);
    }

    /**
     * Converts, formats and displays the weight measure
     * 
     * @param view
     * @param circumference
     */
    private void displayMeasurement(final View view, final Measurement measurement) {
        final TextView t = (TextView) view.findViewById(R.id.measure);
        t.setText(measurement.prettyPrintWithUnit(this.ctx));
        final TextView t2 = (TextView) view.findViewById(R.id.label_measure);
        t2.setText(this.labelText);
    }

    /**
     * Calculates, formats and displays the bmi
     * 
     * @param view
     * @param circumference
     */
    private void displayBMI(final View view, final Measurement measurement) {
        if (measurement.getField() == MeasureType.WEIGHT) {
            final TextView t3 = (TextView) view.findViewById(R.id.bmi);
            t3.setText(NumberFormat.getInstance(Locale.ENGLISH).format(StatisticsFactory.calculateBmi(measurement, this.height)));
        } else {
            view.findViewById(R.id.label_bmi).setVisibility(View.INVISIBLE);
        }
    }

    private void displayComment(final Context context, final View view, final Measurement measurement) {
        if (UserPreferences.isCommentEnabled(context) && measurement.getComment() != null && !measurement.getComment().equals("")) {
            final TextView commentView = (TextView) view.findViewById(R.id.comment);
            commentView.setText(measurement.getComment());
        } else {
            final View commentRowView = view.findViewById(R.id.comment_row);
            commentRowView.setVisibility(View.GONE);
        }
    }

}
