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

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
import de.delusions.measure.notifications.AlarmController;

public class TimeDialogPreference extends DialogPreference {

    private TimePicker tp;

    public TimeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeDialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateDialogView() {
        this.tp = new TimePicker(getContext());
        this.tp.setIs24HourView(true);
        final int[] split = parseTime(getPersistedString(DEFAULT_TIME));
        this.tp.setCurrentHour(split[0]);
        this.tp.setCurrentMinute(split[1]);
        return this.tp;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            final String result = this.tp.getCurrentHour() + ":" + this.tp.getCurrentMinute();
            persistString(result);
            AlarmController.setRepeating(getContext());
        }
    }

    public static final String DEFAULT_TIME = "07:00";

    public static int[] parseTime(String timeString) {
        final String[] split = timeString.split(":");
        final int[] result = { Integer.parseInt(split[0]), Integer.parseInt(split[1]) };
        return result;
    }

}
