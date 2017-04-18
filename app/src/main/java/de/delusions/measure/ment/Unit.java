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

import java.text.NumberFormat;
import java.util.Locale;

import android.content.Context;
import de.delusions.measure.activities.prefs.UserPreferences;

public enum Unit {

    CM(Float.valueOf(0.39f), "cm", "in"),

    KG(Float.valueOf(2.2f), "kg", "lb"),

    PERCENT(Float.valueOf(1), "%", "%");

    private final float toImperialMultiplier;
    private final String metricUnit;
    private final String imperialUnit;

    Unit(final float toImperialMultiplier, final String metricUnit, final String imperialUnit) {
        this.toImperialMultiplier = toImperialMultiplier;
        this.metricUnit = metricUnit;
        this.imperialUnit = imperialUnit;
    }

    public String formatMetric(final Float metricNumber) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(metricNumber);
    }

    public String formatImperial(final Float metricNumber) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(convertToImperial(metricNumber));
    }

    public Float convertToImperial(final Float metricNumber) {
        return metricNumber * this.toImperialMultiplier;
    }

    public Float convertToMetric(final Float imperialNumber) {
        return imperialNumber / this.toImperialMultiplier;
    }

    public Float convertTo(final Float number, final boolean toMetric) {
        Float convertedValue;
        if (toMetric) {
            convertedValue = convertToMetric(number);
        } else {
            convertedValue = convertToImperial(number);
        }
        return convertedValue;
    }

    public String retrieveUnitName(final Context context) {
        return UserPreferences.isMetric(context) ? this.metricUnit : this.imperialUnit;
    }
}
