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
    private final Unit unit;
    private Date timestamp;

    /**
     * Only called by things that really were numbers in the first place. Do not try to parse value from string anywhere
     * else, just use the constructor with the string!
     * 
     * @param value
     * @param unit
     */
    public Measurement(float value, Unit unit) {
        this(value, unit, true);
    }

    public Measurement(float value, Unit unit, boolean metric) {
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
    public Measurement(Long id, String strValue, MeasureType mType, boolean metric, Date timestamp) throws MeasurementException {
        this.id = id;
        this.field = mType;
        this.unit = mType.getUnit();
        this.timestamp = timestamp;
        parseAndSetValue(strValue, metric);
    }

    public void parseAndSetValue(String strValue, boolean metric) throws MeasurementException {
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

    public Measurement(Cursor cursor) throws MeasurementException {
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
    public Measurement(float value, MeasureType field, boolean metric, Date timestamp) {
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

    public void setField(MeasureType field) {
        this.field = field;
    }

    public float getValue() {
        return this.value;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public float getValue(boolean metric) {
        return metric ? this.value : this.unit.convertToImperial(this.value);
    }

    public Unit getUnit() {
        return this.unit;
    }

    public void inc(boolean metric) {
        this.inc(metric, 1);
    }

    public void dec(boolean metric) {
        this.dec(metric, 1);
    }

    public void inc(boolean metric, float incBy) {
        this.value = this.value + (metric ? incBy : this.unit.convertToMetric(incBy));
    }

    public void dec(boolean metric, float decBy) {
        this.value = this.value - (metric ? decBy : this.unit.convertToMetric(decBy));
    }

    public void add(Measurement measurement) {
        if (this.unit == measurement.unit) {
            this.value += measurement.value;
        }
    }

    public int getPercentDifference(Measurement measurement) {
        return 100 - Math.round(100 * measurement.getValue() / this.value);
    }

    public String prettyPrint(Context ctx) {
        return prettyPrint(UserPreferences.isMetric(ctx));
    }

    public String prettyPrint(boolean metric) {
        return metric ? this.unit.formatMetric(this.value) : this.unit.formatImperial(this.value);
    }

    public String prettyPrintWithUnit(Context ctx) {
        return prettyPrint(ctx) + " " + this.unit.retrieveUnitName(ctx);
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append("Measurement[");
        result.append(this.field != null ? this.field.name() : "");
        result.append("=").append(this.getValue()).append(",");
        result.append((getTimestamp() != null ? SimpleDateFormat.getDateInstance().format(getTimestamp()) : ""));
        result.append("]");
        return result.toString();
    }

    public static Float parseValue(String strValue) throws ParseException {
        final Float result = Float.parseFloat(strValue);
        // final Number number = NumberFormat.getInstance().parse(strValue);
        // return number.floatValue();
        return result;
    }

    public static Measurement difference(Measurement a, Measurement b) {
        return new Measurement(a.getValue() - b.getValue(), a.getUnit());
    }

    public static Measurement sum(Measurement a, Measurement b) {
        return new Measurement(a.getValue() + b.getValue(), a.getUnit());
    }

}
