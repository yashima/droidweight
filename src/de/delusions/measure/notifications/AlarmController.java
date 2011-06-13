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
package de.delusions.measure.notifications;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.delusions.measure.activities.prefs.UserPreferences;

public class AlarmController {

	private static final String LOG_TAG = AlarmController.class.getName();

	/** Cancels the existing alarm. */
	public static void cancel(Context ctx) {
		Log.d(LOG_TAG, "Cancelling notofication alarm");
		final PendingIntent pending = createPending(ctx);
		final AlarmManager alarm = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pending);
	}

	/** Sets a repeating alarm. */
	public static void setRepeating(Context ctx) {
		// If notifications are not enabled, just return
		if (!UserPreferences.isNotificationEnabled(ctx)) {
			return;
		}
		
		// If notifications are enabled, set the repeating alarm
		final Date reminderStart = UserPreferences.getReminderStart(ctx);
		final int frequency = UserPreferences.getNotificationFrequency(ctx);
		Log.d(LOG_TAG, "setRepeating @ " + reminderStart);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(reminderStart);
		final PendingIntent pending = createPending(ctx);
		final AlarmManager am = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC, cal.getTimeInMillis(),
				AlarmManager.INTERVAL_DAY * frequency, pending);
	}

	private static PendingIntent createPending(Context ctx) {
		final Intent intent = new Intent(ctx, AlarmReceiver.class);
		final PendingIntent pending = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pending;
	}
}
