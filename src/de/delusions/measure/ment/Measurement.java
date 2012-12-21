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
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;

public class Measurement implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private MeasureType field;
    private float value;
    private Unit unit;

    private Date timestamp;

    public Measurement() {
        this.id = null;
        this.unit = null;
        this.value = 0;
        this.field = null;
        this.timestamp = new Date();
    }

    /**
     * Only called by things that really were numbers in the first place. Do not try to parse value from string anywhere
     * else, just use the constructor with the string!
     * 
     * @param value
     * @param unit
     */
    public Measurement(final float value, final Unit unit) {
        this(value, unit, true);
    }

    public Measurement(final float value, final Unit unit, final boolean metric) {
        this.value = metric ? value : unit.convertToMetric(value);
        this.unit = unit;
        this.timestamp = null;
        this.id = null;
    }

    /**
     * This is the only method that processes user input therefore a few checks are done if the input is valid.
     * 
     * @param id
     *            TODO
     * @param strValue
     * @param metric
     * @param timestamp
     *            TODO
     * @param unit
     * 
     * @throws MeasurementException
     */
    public Measurement(final Long id, final String strValue, final MeasureType mType, final boolean metric, final Date timestamp)
            throws MeasurementException {
        this.id = id;
        this.field = mType;
        this.unit = mType.getUnit();
        this.timestamp = timestamp;
        parseAndSetValue(strValue, metric);
    }

    public Measurement(final Long id, final float value, final MeasureType mType, final boolean metric, final Date timestamp) {
        this.id = id;
        this.field = mType;
        this.unit = mType.getUnit();
        this.timestamp = timestamp;
        this.value = value;
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
        this.value = metric ? this.value : this.unit.convertToMetric(this.value);
        if (this.value < 0) {
            throw new MeasurementException(MeasurementException.ErrorId.SUBZERO);
        } else if (this.value > this.field.getMaxValue()) {
            throw new MeasurementException(MeasurementException.ErrorId.TOOLARGE);
        }
    }

    public Measurement(final Cursor cursor) throws MeasurementException {
        if (cursor != null && cursor.getCount() > 0) {
            final long dateLong = cursor.getLong(cursor.getColumnIndex(SqliteHelper.KEY_DATE));
            this.timestamp = new Date(dateLong);
            this.value = cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_MEASURE_VALUE));
            this.field = MeasureType.valueOf(cursor.getString(cursor.getColumnIndex(SqliteHelper.KEY_NAME)));
            this.unit = this.field.getUnit();
            this.id = cursor.getLong(cursor.getColumnIndex(SqliteHelper.KEY_ROWID));
        } else {
            throw new MeasurementException(MeasurementException.ErrorId.NOINPUT);
        }
    }

    /**
     * Called by MeasureField when reading from a Cursor (aka database). Number is not parsed from String as number is
     * stored in database!
     * 
     * @param value
     * @param field
     * @param metric
     * @param timestamp
     */
    public Measurement(final float value, final MeasureType field, final boolean metric, final Date timestamp) {
        this.field = field;
        this.unit = field.getUnit();
        this.value = metric ? value : this.unit.convertToMetric(value);
        this.timestamp = timestamp;
        this.id = null;
    }

    public Long getId() {
        return this.id;
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
        this.value = metric ? value : this.unit.convertToMetric(value);
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public float getValue(final boolean metric) {
        return metric ? this.value : this.unit.convertToImperial(this.value);
    }

    public Unit getUnit() {
        return this.unit;
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
        this.value = this.value + (metric ? incBy : this.unit.convertToMetric(incBy));
    }

    public void dec(final boolean metric, final float decBy) {
        this.value = this.value - (metric ? decBy : this.unit.convertToMetric(decBy));
    }

    public void add(final Measurement measurement) {
        if (this.unit == measurement.unit) {
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
        return metric ? this.unit.formatMetric(this.value) : this.unit.formatImperial(this.value);
    }

    public String prettyPrintWithUnit(final Context ctx) {
        return prettyPrint(ctx) + " " + this.unit.retrieveUnitName(ctx);
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append("Measurement[");
        result.append(this.id).append(":");
        result.append(this.field != null ? this.field.name() : "");
        result.append("=").append(this.getValue()).append(",");
        result.append(getTimestamp() != null ? SimpleDateFormat.getDateInstance().format(getTimestamp()) : "");
        result.append("]");
        return result.toString();
    }

    public static Float parseValue(final String strValue) throws ParseException {
        final Float result = Float.parseFloat(strValue);
        // final Number number = NumberFormat.getInstance().parse(strValue);
        // return number.floatValue();
        return result;
    }

    public static Measurement difference(final Measurement a, final Measurement b) {
        return new Measurement(a.getValue() - b.getValue(), a.getUnit());
    }

    public static Measurement sum(final Measurement a, final Measurement b) {
        return new Measurement(a.getValue() + b.getValue(), a.getUnit());
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

}
