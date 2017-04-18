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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.delusions.measure.MeasureTabs;
import de.delusions.measure.R;

public class NotifyService extends IntentService {

    private final static String LOG_TAG = NotifyService.class.getName();

    public NotifyService() {
        super(LOG_TAG);
    }

    /** Create a notification. */
    @Override
    public void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "NofifyService invoked.");
        // Create the notification
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final CharSequence notificationText = getText(R.string.notification_title);

        // Set the icon, scrolling text and timestamp
        final Notification notification = new Notification(R.drawable.icon_notification, notificationText, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // The PendingIntent to launch our activity if the user selects this
        // notification
        final Intent notifyIntent = new Intent(this, MeasureTabs.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 0, notifyIntent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.notification_ticker), notificationText, pi);

        // Send the notification.
        // Use a layout id because it is a unique number.
        // We can use it later to cancel.
        nm.notify(R.layout.activity_edit, notification);
    }
}
