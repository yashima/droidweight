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
package de.delusions.measure.activities.prefs;

import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Unit;

public enum PrefItem {
    HEIGHT("user.height", Unit.CM, Float.class, null),

    GOAL("user.goal", Unit.KG, Float.class, null),

    METRIC("user.units", null, String.class, null),

    NOTIFICATION("user.notification", null, String.class, null),

    NOTIFICATION_ENABLED("user.notification.enabled", null, Boolean.class, null),

    WEIGHT_TRACKING("user.weight", null, Boolean.class, MeasureType.WEIGHT),

    HEIGHT_TRACKING("user.heighttracking", null, Boolean.class, null),

    WAIST_TRACKING("user.waisttracking", null, Boolean.class, MeasureType.WAIST),

    FAT_TRACKING("user.fat", null, Boolean.class, MeasureType.BODYFAT),

    FAST_INPUT("user.fastinput", null, Boolean.class, null),

    FREQUENCY("user.notification.frequency", null, Integer.class, null),

    DISPLAY_MEASURE("user.display", null, String.class, null);

    String key;
    Unit unit;
    Class<?> prefClass;
    MeasureType trackingType;

    private PrefItem(String key, Unit unit, Class<?> prefClass, MeasureType trackingType) {
        this.key = key;
        this.unit = unit;
        this.prefClass = prefClass;
        this.trackingType = trackingType;
    }

    public String getKey() {
        return this.key;
    }

    public Unit getUnit() {
        return this.unit;
    }

    public Class<?> getPrefClass() {
        return this.prefClass;
    }

    public MeasureType getTrackingType() {
        return this.trackingType;
    }
}