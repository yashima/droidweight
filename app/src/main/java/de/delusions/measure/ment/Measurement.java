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
package de.delusions.measure.ment;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;

public class Measurement implements Serializable {

    private static final String TAG = "Measurement";
    private static final long serialVersionUID = 1L;

    private Long id;
    private MeasureType field = null;
    private float value = 0;
    private Unit unit = null;
    private String comment;
    private Date timestamp = new Date();

    public Measurement() {
        this.id = -1l;
    }

    public void parseAndSetValue(final String strValue, final boolean metric) throws MeasurementException {
        if (strValue == null || strValue.equals("")) {
            throw new MeasurementException(MeasurementException.ErrorId.NOINPUT);
        }
        try {
            this.value = parseValue(strValue);
        } catch (final ParseException e) {
            throw new MeasurementException(MeasurementException.ErrorId.NONUMBER);
        } catch (final NumberFormatException e) {
            throw new MeasurementException(MeasurementException.ErrorId.PARSEERROR);
        }
        this.value = metric ? this.value : getUnit().convertToMetric(this.value);
        if (this.value < 0) {
            throw new MeasurementException(MeasurementException.ErrorId.SUBZERO);
        } else if (this.value > this.field.getMaxValue()) {
            throw new MeasurementException(MeasurementException.ErrorId.TOOLARGE);
        }
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public MeasureType getField() {
        return this.field;
    }

    public void setField(final MeasureType field) {
        this.field = field;
    }

    public float getValue() {
        return this.value;
    }

    public void setValue(final float value, final boolean metric) {
        this.value = metric ? value : getUnit().convertToMetric(value);
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public float getValue(final boolean metric) {
        return metric ? this.value : getUnit().convertToImperial(this.value);
    }

    public Unit getUnit() {
        return this.field == null ? this.unit : this.field.getUnit();
    }

    public void setUnit(final Unit unit) {
        this.unit = unit;
    }

    public void inc(final boolean metric) {
        this.inc(metric, 1);
    }

    public void dec(final boolean metric) {
        this.dec(metric, 1);
    }

    public void inc(final boolean metric, final float incBy) {
        this.value = this.value + (metric ? incBy : getUnit().convertToMetric(incBy));
    }

    public void dec(final boolean metric, final float decBy) {
        this.value = this.value - (metric ? decBy : getUnit().convertToMetric(decBy));
    }

    public void add(final Measurement measurement) {
        if (getUnit() == measurement.getUnit()) {
            this.value += measurement.value;
        }
    }

    public float getPercentDifference(final Measurement measurement) {
        return 100 - 100 * measurement.getValue() / this.value;
    }

    public String prettyPrint(final Context ctx) {
        return prettyPrint(UserPreferences.isMetric(ctx));
    }

    public String prettyPrint(final boolean metric) {
        return metric ? getUnit().formatMetric(this.value) : getUnit().formatImperial(this.value);
    }

    public String prettyPrintWithUnit(final Context ctx) {
        return prettyPrint(ctx) + " " + getUnit().retrieveUnitName(ctx);
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append("Measurement[");
        result.append(this.id).append(":");
        result.append(this.field != null ? this.field.name() : "");
        result.append("=").append(this.getValue()).append(",");
        result.append(getTimestamp() != null ? SimpleDateFormat.getDateInstance().format(getTimestamp()) : "");
        result.append(",");
        result.append(this.comment);
        result.append("]");
        return result.toString();
    }

    public void updateTime(final int hourOfDay, final int minute) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(getTimestamp());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        setTimestamp(calendar.getTime());
    }

    public void updateDate(final int year, final int monthOfYear, final int dayOfMonth) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(getTimestamp());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        setTimestamp(calendar.getTime());
    }

    public static Float parseValue(final String strValue) throws ParseException {
        return Float.parseFloat(strValue);
    }

    public static Measurement difference(final Measurement a, final Measurement b) {
        final Measurement measurement = new Measurement();
        measurement.setUnit(a.getUnit());
        measurement.setValue(a.getValue() - b.getValue(), true);
        return measurement;
    }

    public static Measurement sum(final Measurement a, final Measurement b) {
        final Measurement measurement = new Measurement();
        measurement.setUnit(a.getUnit());
        measurement.setValue(a.getValue() + b.getValue(), true);
        return measurement;
    }

    public static Measurement create(final Cursor cursor) throws MeasurementException {
        final Measurement measurement = new Measurement();
        if (isUsable(cursor)) {
            measurement.id = getLongValue(cursor, SqliteHelper.KEY_ROWID);
            measurement.value = getFloatValue(cursor, SqliteHelper.KEY_MEASURE_VALUE);
            measurement.timestamp = new Date(getLongValue(cursor, SqliteHelper.KEY_DATE));
            measurement.field = MeasureType.valueOf(getStringValue(cursor, SqliteHelper.KEY_NAME));
            measurement.comment = getStringValue(cursor, SqliteHelper.KEY_COMMENT);
        } else {
            Log.e(TAG, "failed to create measure from cursor");
            throw new MeasurementException(MeasurementException.ErrorId.NOINPUT);
        }
        return measurement;
    }

    public static Measurement create(final Measurement copyFrom) {
        final Measurement measurement = new Measurement();
        measurement.value = copyFrom.value;
        measurement.field = copyFrom.field;
        return measurement;
    }

    protected static boolean hasColumn(final Cursor cursor, final String columName) {
        return cursor.getColumnIndex(columName) > 0;
    }

    private static boolean isUsable(final Cursor cursor) {
        return cursor != null && cursor.getCount() > 0 && !cursor.isAfterLast() && !cursor.isBeforeFirst();
    }

    private static String getStringValue(final Cursor cursor, final String columnName) {
        if (hasColumn(cursor, columnName)) {
            return cursor.getString(cursor.getColumnIndex(columnName));
        } else {
            // Log.w(TAG, "Column not found " + columnName);
            return null;
        }
    }

    private static Long getLongValue(final Cursor cursor, final String columnName) {
        if (hasColumn(cursor, columnName)) {
            return cursor.getLong(cursor.getColumnIndex(columnName));
        } else {
            // Log.w(TAG, "Column not found " + columnName);
            return null;
        }
    }

    private static Float getFloatValue(final Cursor cursor, final String columnName) {
        if (hasColumn(cursor, columnName)) {
            return cursor.getFloat(cursor.getColumnIndex(columnName));
        } else {
            // Log.w(TAG, "Column not found " + columnName);
            return null;
        }
    }
}
