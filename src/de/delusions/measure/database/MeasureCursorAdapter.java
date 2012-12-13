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
package de.delusions.measure.database;

import de.delusions.measure.R;
import de.delusions.measure.activities.bmi.StatisticsFactory;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class MeasureCursorAdapter extends CursorAdapter {

    public static final String HOUR_MINUTE = "HH:mm";
    public static final String DAY_MONTH_YEAR = "dd/MM/yyyy";

    //only ok because this app is not multithreaded!
    public static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat(
            String.format("%s, %s", HOUR_MINUTE, DAY_MONTH_YEAR));

    private final Measurement height;
    private final Context ctx;
    private final MeasureType displayField;
    private final String labelText;

    public MeasureCursorAdapter(Context context, Cursor c,
            MeasureType displayField) {
        super(context, c);
        this.height = UserPreferences.getHeight(context);
        this.ctx = context;
        this.displayField = displayField;
        this.labelText = this.ctx.getResources().getString(
                displayField.getLabelId())
                + ":";
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final Measurement measurement = this.displayField
                .createMeasurement(cursor);
        final String date = cursor.getString(cursor
                .getColumnIndex(SqliteHelper.KEY_DATE));
        final float percent = calculatePercentChange(cursor, measurement);
        displayMeasurement(view, measurement);
        displayDate(view, date);
        displayBMI(view, measurement);
        displayPercentChange(view, percent);

    }

    private float calculatePercentChange(Cursor cursor,
            final Measurement measurement) {
        final float percent;
        if (!cursor.isLast()) {
            cursor.moveToNext();
            final Measurement previous = this.displayField
                    .createMeasurement(cursor);
            cursor.moveToPrevious();
            percent = measurement.getPercentDifference(previous);
        } else {
            percent = 0;
        }
        return percent;
    }

    private void displayPercentChange(View view, float percent) {
        // final TextView t = (TextView) view.findViewById(R.id.percentchange);
        // t.setText((percent > 0 ? "+" : "") + percent + "%");
        final ImageView i = (ImageView) view.findViewById(R.id.change_arrow);
        final Resources resources = view.getContext().getResources();
        if (percent > 0) {
            i.setImageDrawable(resources
                    .getDrawable(android.R.drawable.arrow_up_float));
        } else if (percent < 0) {
            i.setImageDrawable(resources
                    .getDrawable(android.R.drawable.arrow_down_float));
        } else {
            i.setImageDrawable(resources
                    .getDrawable(android.R.drawable.radiobutton_off_background));
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view = LayoutInflater.from(context).inflate(
                R.layout.layout_weight_row, parent, false);
        return view;
    }

    /**
     * Formats and displays the date
     * 
     * @param view
     * @param circumference
     */
    private void displayDate(View view, String strDate) {
        final long timestamp = Long.parseLong(strDate);
        final String formattedDate = DATEFORMAT.format(new Date(timestamp));
        final TextView t2 = (TextView) view.findViewById(R.id.date);
        t2.setText(formattedDate);
    }

    /**
     * Converts, formats and displays the weight measure
     * 
     * @param view
     * @param circumference
     */
    private void displayMeasurement(View view, Measurement measurement) {
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
    private void displayBMI(View view, Measurement measurement) {
        if (measurement.getField() == MeasureType.WEIGHT) {
            final TextView t3 = (TextView) view.findViewById(R.id.bmi);
            t3.setText(NumberFormat.getInstance(Locale.ENGLISH).format(
                    StatisticsFactory.calculateBmi(measurement, this.height)));
        } else {
            view.findViewById(R.id.label_bmi).setVisibility(View.INVISIBLE);
        }
    }

}
