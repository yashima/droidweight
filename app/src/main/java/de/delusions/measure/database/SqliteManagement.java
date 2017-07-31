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
package de.delusions.measure.database;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;

public class SqliteManagement {

    public static AlertDialog clearDatabase(final Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setMessage(R.string.dialog_message_deleteall);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(final DialogInterface dialog, final int id) {
                final SqliteHelper db = new SqliteHelper(a);
                db.deleteAll();
                db.close();
                if (a instanceof MeasureActivity) {
                    ((MeasureActivity) a).refreshListView();
                }
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            public void onClick(final DialogInterface dialog, final int id) {
                dialog.cancel();
            }
        });
        return builder.create();
    }
}
