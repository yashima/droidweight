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
package de.delusions.measure.activities.chart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

public class MeasurePath extends Path {

    private static final String TAG = "MeasurePath";

    private final Context ctx;
    private MeasureType type;
    private Calendar startingDate;
    private int lowerValue;
    private int upperValue;
    private List<Measurement> measures;

    public MeasurePath(final Context ctx, final MeasureType type, final int days) {
        this.ctx = ctx;
        refreshData(type, days);
    }

    public void refreshData(final MeasureType type, final int days) {
        rewind();
        final boolean metric = UserPreferences.isMetric(this.ctx);
        this.type = type;
        this.startingDate = calculateStartingDate(days);
        this.measures = retrieveDataForDays(this.ctx);
        calculateBoundaries(this.measures, metric);
    }

    public void fillPath(final ChartCoordinates coords) {
        final boolean metric = UserPreferences.isMetric(this.ctx);
        Point lastPoint = null;
        for (final Measurement measure : this.measures) {
            final float x = calculateDaysOnXAxis(measure);
            final float y = measure.getValue(metric);
            final Point point = coords.calculatePoint(x, y, this.upperValue, this.lowerValue);
            Log.d(TAG, "fillPath " + x + " " + y + " -> " + point);

            if (lastPoint == null) {
                moveTo(point.x, point.y);
            } else if (lastPoint.x != point.x) {
                lineTo(point.x, point.y);
            } else {
                // ignore this value due to double dates
            }
            lastPoint = point;
        }
    }

    private float calculateDaysOnXAxis(final Measurement measure) {
        return (measure.getTimestamp().getTime() - this.startingDate.getTimeInMillis()) / (24 * 60 * 60 * 1000);
    }

    private void calculateBoundaries(final List<Measurement> measures, final boolean metric) {
        float min;
        final float goal = UserPreferences.getGoal(this.ctx).getValue(metric);
        if (this.type == MeasureType.WEIGHT && goal > 1) {
            min = goal - 1;
        } else {
            min = measures.size() > 0 ? measures.get(0).getValue() : 20;
        }
        float max = measures.size() > 0 ? measures.get(0).getValue() : min * 2;
        for (final Measurement measure : measures) {
            min = Math.min(measure.getValue(metric), min);
            max = Math.max(measure.getValue(metric), max);
        }
        this.lowerValue = (int) Math.floor(min / 10) * 10;
        this.upperValue = (int) Math.ceil(max / 10) * 10;
        Log.d(TAG, "lower= " + this.lowerValue);
        Log.d(TAG, "upper= " + this.upperValue);
        if (this.upperValue < this.lowerValue) {
            Log.e(TAG, "ceiling below floor on path!!!");
        }
    }

    private List<Measurement> retrieveDataForDays(final Context ctx) {
        final List<Measurement> result = new ArrayList<Measurement>();
        final SqliteHelper sqliteHelper = new SqliteHelper(ctx);
        final Cursor cursor = sqliteHelper.fetchByDate(this.startingDate.getTime(), this.type);
        Log.d(TAG, "retrieveDataForDays:count=" + cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final Measurement measurement = this.type.createMeasurement(cursor);
            Log.d(TAG, "retrieveWeightForDays:adding " + measurement);
            result.add(measurement);
            cursor.moveToNext();
        }
        cursor.close();
        sqliteHelper.close();
        return result;
    }

    private Calendar calculateStartingDate(final int days) {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Log.d(TAG, "calendar date set " + SimpleDateFormat.getDateTimeInstance().format(cal.getTime()));
        return cal;
    }

    public int getCeiling() {
        return this.upperValue;
    }

    public int getFloor() {
        return this.lowerValue;
    }

    public Calendar getStartingDate() {
        return this.startingDate;
    }

    public int labelVerticalMeasureValue(final int segment, final int segments) {
        final int perSegment = (getCeiling() - getFloor()) / segments;
        final int labelValue = segment * perSegment;
        final int displayed = getCeiling() - labelValue;
        return displayed;
    }

    public Date labelHorizontalDate(final int days, final int segment, final int segments) {
        final Calendar cal = (Calendar) getStartingDate().clone();
        final int daysPerSegment = days / segments;
        final int daysDifference = segment * daysPerSegment;
        cal.add(Calendar.DAY_OF_MONTH, daysDifference);
        return cal.getTime();
    }
}
