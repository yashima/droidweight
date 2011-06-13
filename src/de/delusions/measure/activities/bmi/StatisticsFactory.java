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
package de.delusions.measure.activities.bmi;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.Unit;

public class StatisticsFactory {

    private final Measurement starting;
    private final Measurement last;
    private final Measurement goal;
    private final Measurement height;

    public StatisticsFactory(SqliteHelper db, Context ctx) {
        Cursor cursor = db.fetchFirst(MeasureType.WEIGHT);
        this.starting = MeasureType.WEIGHT.createMeasurement(cursor);
        cursor.close();
        cursor = db.fetchLast(MeasureType.WEIGHT);
        this.last = MeasureType.WEIGHT.createMeasurement(cursor);
        cursor.close();
        this.goal = UserPreferences.getGoal(ctx);
        this.height = UserPreferences.getHeight(ctx);
    }

    public StatisticsFactory(Measurement starting, Measurement last, Measurement goal, Measurement height) {
        this.starting = starting;
        this.last = last;
        this.goal = goal;
        this.height = height;
    }

    public Measurement getStartingWeight() {
        return this.starting;
    }

    public Measurement getGoal() {
        return this.goal;
    }

    public Measurement getLastWeight() {
        return this.last;
    }

    public float calculateCurrentBmi() {
        return calculateBmi(this.last, this.height);
    }

    public Measurement calculateLoss() {
        return Measurement.difference(this.starting, this.last);
    }

    public Measurement calculateTogo() {
        return Measurement.difference(this.last, this.goal);
    }

    public Measurement calculateAverageDailyLoss() {
        final Measurement loss = calculateLoss();
        final long time = this.last.getTimestamp().getTime() - this.starting.getTimestamp().getTime();
        final long days = time / (24 * 60 * 60 * 1000);
        final float dailyLoss = days > 0 ? loss.getValue() / days : loss.getValue();
        return new Measurement(dailyLoss, Unit.KG);
    }

    public Date calculateETA() {
        final Measurement averageLoss = calculateAverageDailyLoss();
        final Measurement togo = calculateTogo();
        final int days;
        if (averageLoss.getValue() > 0) {
            days = Math.round(togo.getValue() / averageLoss.getValue());
        } else {
            return null;
        }
        final Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.HOUR, 0);
        if (days > 0) {
            now.roll(Calendar.DAY_OF_MONTH, days);
        }
        return now.getTime();

    }

    /**
     * Calculates the BMI from kg and height in meter
     * 
     * @param weightInKg
     * @param heightInMeter
     * @return
     */
    public static float calculateBmi(Measurement weightInKg, Measurement heightInCm) {
        if (weightInKg.getUnit() == Unit.KG && heightInCm.getUnit() == Unit.CM) {
            final float heightInMeter = heightInCm.getValue() / 100;
            return weightInKg.getValue() / (heightInMeter * heightInMeter);
        } else {
            // should never happen is a bug!
            return -1;
        }
    }

    public static Measurement calculateBmiWeight(int bmiValue, Measurement heightInCm) {
        final float heightInMeter = heightInCm.getValue() / 100;
        final float result = heightInMeter * heightInMeter * bmiValue;
        final Measurement weight = new Measurement(result, Unit.KG);
        return weight;
    }
}
