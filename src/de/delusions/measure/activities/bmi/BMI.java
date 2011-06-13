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

import android.content.Context;
import de.delusions.measure.R;

public enum BMI {

    OBESE(R.id.range_obese, R.array.obese, R.color.obese),

    OVERWEIGHT(R.id.range_overweight, R.array.overweight, R.color.overweight),

    NORMAL(R.id.range_normalweight, R.array.normalweight, R.color.normalweight),

    UNDERWEIGHT(R.id.range_underweight, R.array.underweight, R.color.underweight);

    private final int id;
    private final int color;
    private final int array;

    private BMI(int id, int array, int color) {
        this.id = id;
        this.array = array;
        this.color = color;
    }

    public boolean isInRange(float bmiValue, Context ctx) {
        final int[] range = ctx.getResources().getIntArray(this.array);
        return range[0] < bmiValue && bmiValue <= range[1];
    }

    public int getFloor(Context ctx) {
        final int[] range = ctx.getResources().getIntArray(this.array);
        return range[0];
    }

    public int getCeiling(Context ctx) {
        final int[] range = ctx.getResources().getIntArray(this.array);
        return range[1];
    }

    public int getId() {
        return this.id;
    }

    public int getColor(Context ctx) {
        return ctx.getResources().getColor(this.color);
    }

    public int getRangeId() {
        return this.array;
    }

    public static BMI getBmi(float bmiValue, Context ctx) {
        for (final BMI bmi : BMI.values()) {
            if (bmi.isInRange(bmiValue, ctx)) {
                return bmi;
            }
        }
        return null;
    }
}
