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
import java.util.*;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.util.Log;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.PrefItem;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;

public class MeasureType implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final MeasureType WEIGHT = new MeasureType("WEIGHT", -1, R.string.label_weight, Unit.KG, Float.valueOf(999), Float.valueOf(0.1f),
            Float.valueOf(1), Color.CYAN, PrefItem.WEIGHT_TRACKING);

    public static final MeasureType BODYFAT = new MeasureType("BODYFAT", -1, R.string.label_bodyfat, Unit.PERCENT, Float.valueOf(100),
            Float.valueOf(0.1f), Float.valueOf(1), Color.MAGENTA, PrefItem.FAT_TRACKING);

    public static final MeasureType WAIST = new MeasureType("WAIST", -1, R.string.label_waist, Unit.CM, Float.valueOf(300), Float.valueOf(1),
            Float.valueOf(5), Color.GREEN, PrefItem.WAIST_TRACKING);

    public static final MeasureType HEIGHT = new MeasureType("HEIGHT", -1, R.string.label_height, Unit.CM, Float.valueOf(300), Float.valueOf(1),
            Float.valueOf(5), Color.BLACK, PrefItem.HEIGHT_TRACKING);

    private static final Map<String, MeasureType> VALUES = new HashMap<String, MeasureType>();
    static {
        VALUES.put(WEIGHT.name(), WEIGHT);
        VALUES.put(BODYFAT.name(), BODYFAT);
        VALUES.put(WAIST.name(), WAIST);
        VALUES.put(HEIGHT.name(), HEIGHT);
        Log.d(MeasureActivity.TAG, "initing hashmap with static values " + VALUES);
    }

    private final long id;
    private final int labelId;
    private final int androidId;
    private final Unit unit;
    private Float maxValue;
    private Float smallStep;
    private Float bigStep;
    private final String name;
    private int licenseKey; // TODO calculate
    private boolean enabled; // TODO switch over from props
    private final int color;
    private PrefItem pref;

    private MeasureType(final Cursor cursor) {
        this.id = cursor.getLong(cursor.getColumnIndex(SqliteHelper.KEY_ROWID));
        this.labelId = -1;
        this.androidId = -1;
        this.unit = Unit.valueOf(cursor.getString(cursor.getColumnIndex(SqliteHelper.KEY_UNIT)));
        this.maxValue = cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_MAXVALUE));
        this.smallStep = cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_SMALLSTEP));
        this.bigStep = cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_BIGSTEP));
        this.name = cursor.getString(cursor.getColumnIndex(SqliteHelper.KEY_NAME));
        this.enabled = cursor.getInt(cursor.getColumnIndex(SqliteHelper.KEY_ENABLED)) == 1;
        this.licenseKey = cursor.getInt(cursor.getColumnIndex(SqliteHelper.KEY_LICENSE));
        this.color = cursor.getInt(cursor.getColumnIndex(SqliteHelper.KEY_COLOR));
    }

    private MeasureType(final String name, final int androidId, final int labelId, final Unit unit, final Float maxValue, final Float smallStep,
            final Float bigStep, final int color, final PrefItem item) {
        this.id = -1;
        this.androidId = androidId;
        this.unit = unit;
        this.labelId = labelId;
        this.maxValue = maxValue;
        this.smallStep = smallStep;
        this.bigStep = bigStep;
        this.name = name;
        this.color = color;
        this.pref = item;
    }

    @Override
    public boolean equals(final Object o) {
        final boolean result;
        if (o == null) {
            result = false;
        } else if (!(o instanceof MeasureType)) {
            result = false;
        } else {
            final MeasureType other = (MeasureType) o;
            result = this.name.equals(other.name);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * for the default fields to make some stuff configurable
     * 
     * @param cursor
     */
    public void refresh(final MeasureType mt) {
        this.maxValue = mt.maxValue;
        this.smallStep = mt.smallStep;
        this.bigStep = mt.bigStep;
    }

    public long getId() {
        return this.id;
    }

    public PrefItem getPref() {
        return this.pref;
    }

    public int getLicenseKey() {
        return this.licenseKey;
    }

    @Deprecated
    public int getAndroidId() {
        return this.androidId;
    }

    public int getColor() {
        return this.color;
    }

    public Float getSmallStep() {
        return this.smallStep;
    }

    public Float getBigStep() {
        return this.bigStep;
    }

    public Float getMaxValue() {
        return this.maxValue;
    }

    public Unit getUnit() {
        return this.unit;
    }

    public String getLabel(final Activity activity) {
        return this.labelId < 0 ? this.name : activity.getResources().getString(this.labelId);
    }

    public int getLabelId() {
        return this.labelId;
    }

    public float getFloat(final Cursor cursor) {
        return cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_MEASURE_VALUE));
    }

    public Measurement createMeasurement(final Cursor cursor) {
        final Measurement measurement = new Measurement();
        if (cursor.getCount() > 0 && !cursor.isAfterLast()) {
            final Date timestamp = SqliteHelper.getTimestamp(cursor);
            final Float value = cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_MEASURE_VALUE));
            final Long id = cursor.getLong(cursor.getColumnIndex(SqliteHelper.KEY_ROWID));
            measurement.setId(id);
            measurement.setField(this);
            measurement.setUnit(getUnit());
            measurement.setValue(value, true);
            measurement.setTimestamp(timestamp);
            return measurement;
        } else {
            Log.d(MeasureActivity.TAG, "MeasureType:createMeasurement: Cursor Empty, no Measurement created");
            return new Measurement();
        }
    }

    public Measurement zero(final Context ctx) {
        final Measurement measurement = new Measurement();
        measurement.setField(this);
        measurement.setUnit(getUnit());
        measurement.setValue(0f, UserPreferences.isMetric(ctx));
        return measurement;
    }

    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(this.name).append("[").append(this.smallStep).append(",").append(this.bigStep).append(",").append(this.unit).append(",")
                .append(this.enabled).append(",").append(this.androidId).append(",").append(this.labelId).append("]");
        return buffer.toString();
    }

    public static List<MeasureType> getTypes() {
        final List<MeasureType> types = new ArrayList<MeasureType>();
        types.addAll(VALUES.values());
        return types;
    }

    public static List<MeasureType> getEnabledTypes(final Context ctx) {
        final List<MeasureType> types = new ArrayList<MeasureType>();
        types.add(WEIGHT);
        if (UserPreferences.isEnabled(BODYFAT, ctx)) {
            types.add(BODYFAT);
        }
        if (UserPreferences.isEnabled(WAIST, ctx)) {
            types.add(WAIST);
        }
        return types;
    }

    public static MeasureType valueOf(final String name) {
        return VALUES.get(name);
    }

    public static void initializeTypeMap(final Context ctx) {
        refreshFromDatabase(ctx);
    }

    private static void refreshFromDatabase(final Context ctx) {
        final SqliteHelper db = new SqliteHelper(ctx);
        Cursor cursor = null;
        try {
            cursor = db.fetchTypes();
            if (cursor.getCount() > 0) {
                while (!cursor.isLast()) {
                    cursor.moveToNext();
                    final MeasureType type = new MeasureType(cursor);
                    if (VALUES.containsKey(type.name())) {
                        VALUES.get(type.name()).refresh(type);
                        Log.d(MeasureActivity.TAG, "initializeTypeMap refreshed: " + VALUES.get(type.name()));
                    } else {
                        VALUES.put(type.name(), type);
                        Log.d(MeasureActivity.TAG, "initializeTypeMap from db: " + type);
                    }
                }
            }
        } catch (final SQLiteException e) {
            Log.e(MeasureActivity.TAG, "Could not retrieve stuff from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        db.close();
    }

    public static void initializeDatabase(final SqliteHelper db) {
        Log.d(MeasureActivity.TAG, "initializeDatabase MeasureType");
        db.createType(WEIGHT);
        db.createType(BODYFAT);
        db.createType(WAIST);
        db.createType(HEIGHT);
    }
}