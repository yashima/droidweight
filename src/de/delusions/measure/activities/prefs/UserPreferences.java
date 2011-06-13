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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import de.delusions.measure.R;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.Unit;
import de.delusions.measure.notifications.AlarmController;

public class UserPreferences extends PreferenceActivity {

    public static final String PREFS_NAME = "MeasurePrefs";

    Preference metricPref;
    Preference goalPref;
    Preference heightPref;
    
    private static final String LOG_TAG = UserPreferences.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        addPreferencesFromResource(R.xml.preferences);

        // I was young and stupid:
        migratePreferencesToDefault(PREFS_NAME);

        this.metricPref = findPreference(PrefItem.METRIC.getKey());
        this.heightPref = findPreference(PrefItem.HEIGHT.getKey());
        this.goalPref = findPreference(PrefItem.GOAL.getKey());

        this.metricPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean metric = Boolean.parseBoolean(newValue.toString());
                changeSummaries(metric);
                return true;
            }
        });

        final Preference notificationEnabled = findPreference(PrefItem.NOTIFICATION_ENABLED.getKey());
        notificationEnabled.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(LOG_TAG, "notification enabled changed");
                final Boolean enabled = Boolean.parseBoolean(newValue.toString());
                if (enabled) {
                    AlarmController.setRepeating(getBaseContext());
                } else {
                    AlarmController.cancel(getBaseContext());
                }
                return true;
            }
        });
    }

    private void migratePreferencesToDefault(String filename) {
        try {
            final SharedPreferences customSharedPrefs = getSharedPreferences(filename, Activity.MODE_PRIVATE);
            final SharedPreferences defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            final Editor customEditor = customSharedPrefs.edit();
            final Editor defaultEditor = defaultSharedPrefs.edit();
            for (final PrefItem item : PrefItem.values()) {
                if (customSharedPrefs.contains(item.getKey())) {
                    if (item == PrefItem.METRIC) {
                        defaultEditor.putString(item.getKey(), customSharedPrefs.getBoolean(item.getKey(), true) + "");
                    } else if (item.getPrefClass() == String.class) {
                        defaultEditor.putString(item.getKey(), customSharedPrefs.getString(item.getKey(), ""));
                    } else if (item.getPrefClass() == Float.class) {
                        defaultEditor.putFloat(item.getKey(), customSharedPrefs.getFloat(item.getKey(), new Float(-1)));
                    } else if (item.getPrefClass() == Boolean.class) {
                        defaultEditor.putBoolean(item.getKey(), customSharedPrefs.getBoolean(item.getKey(), false));
                    }
                    Log.d(LOG_TAG, "migratePreferencesToDefault: moved " + item.getKey());
                    customEditor.remove(item.getKey());
                }
            }
            defaultEditor.commit();
            customEditor.commit();
        } catch (final Exception e) {
            Toast.makeText(this, "Failed to migrate Prefs, oops", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final boolean metric = isMetric(this);
        changeSummaries(metric);
    }

    private void changeSummaries(boolean toMetric) {
        this.goalPref.setSummary(toMetric ? R.string.pref_goal_summary : R.string.pref_goal_summary_imperial);
        this.heightPref.setSummary(toMetric ? R.string.pref_height_summary : R.string.pref_height_summary_imperial);
    }

    public static float getUnitPreference(Context ctx, String key) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final Float result = prefs.getFloat(key, new Float(0));
        Log.d(LOG_TAG, "getUnitPreference " + result);
        return result;
    }

    public static Measurement getGoal(Context ctx) {
        final float value = getUnitPreference(ctx, PrefItem.GOAL.getKey());
        return new Measurement(value, Unit.KG, true);
    }

    public static Measurement getHeight(Context ctx) {
        final float value = getUnitPreference(ctx, PrefItem.HEIGHT.getKey());
        return new Measurement(value, Unit.CM, true);
    }

    public static Boolean isMetric(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return Boolean.parseBoolean(prefs.getString(PrefItem.METRIC.getKey(), "true"));
    }

    public static Boolean isNotificationEnabled(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PrefItem.NOTIFICATION_ENABLED.getKey(), false);
    }

    // public static Boolean isTrackFat(Context ctx) {
    // final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    // return prefs.getBoolean(PrefItem.FAT_TRACKING.getKey(), false);
    // }
    //
    // public static Boolean isTrackWaist(Context ctx) {
    // final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    // return prefs.getBoolean(PrefItem.WAIST_TRACKING.getKey(), false);
    // }

    public static Boolean isFastInput(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PrefItem.FAST_INPUT.getKey(), false);
    }

    public static Boolean isEnabled(MeasureType type, Context ctx) {
        final PrefItem item = type.getPref();
        if (item == null) {
            Log.w(LOG_TAG, "isEnabled but item is null for " + type);
            return false;
        } else {
            Log.d(LOG_TAG, "isEnabled " + item);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            return prefs.getBoolean(item.getKey(), false);
        }
    }

    public static int getNotificationFrequency(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getInt(PrefItem.FREQUENCY.getKey(), 1);
    }

    public static Date getReminderStart(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final String timeStr = prefs.getString(PrefItem.NOTIFICATION.getKey(), TimeDialogPreference.DEFAULT_TIME);
        final int[] time = TimeDialogPreference.parseTime(timeStr);
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, time[0]);
        cal.set(Calendar.MINUTE, time[1]);
        cal.set(Calendar.SECOND, 0);
        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTime();
    }

    public static List<MeasureType> getTrackedTypes(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final List<MeasureType> result = new ArrayList<MeasureType>();
        for (final PrefItem item : PrefItem.values()) {
            if (item.getTrackingType() != null) {
                if (prefs.getBoolean(item.getKey(), false)) {
                    result.add(item.getTrackingType());
                }
            }
        }
        result.add(MeasureType.WEIGHT);
        return result;
    }

    public static Map<MeasureType, Boolean> getTracking(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final Map<MeasureType, Boolean> result = new HashMap<MeasureType, Boolean>();
        for (final PrefItem item : PrefItem.values()) {
            if (item.getTrackingType() != null) {
                result.put(item.getTrackingType(), prefs.getBoolean(item.getKey(), false));
            }
        }
        result.put(MeasureType.WEIGHT, true);
        return result;
    }

    /**
     * Changes the currently displayed tracking type.
     * 
     * @param ctx
     * @param type
     */
    public static void setDisplayField(Context ctx, MeasureType type) {
        Log.d(LOG_TAG, "setDisplayField " + type);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final Editor editor = prefs.edit();
        editor.putString(PrefItem.DISPLAY_MEASURE.getKey(), type.name());
        editor.commit();
    }

    /**
     * Returns the currently displayed tracking type.
     * 
     * @param ctx
     * @return
     */
    public static MeasureType getDisplayField(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return MeasureType.valueOf(prefs.getString(PrefItem.DISPLAY_MEASURE.getKey(), MeasureType.WEIGHT.name()));
    }

}
