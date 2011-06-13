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

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import de.delusions.measure.database.SqliteHelper;

public class MeasurementFactory {

    public static Measurement retrieveCurrentWeight(Context ctx) {
        final SqliteHelper db = new SqliteHelper(ctx);
        db.open();
        final Cursor c = db.fetchLast(MeasureType.WEIGHT);
        final Measurement result = createMeasurement(c, MeasureType.WEIGHT);
        c.close();
        db.close();
        return result;
    }

    public static Measurement createMeasurement(Cursor cursor, MeasureType field) {
        final Date timestamp = SqliteHelper.getTimestamp(cursor);
        final Float value = cursor.getFloat(cursor.getColumnIndex(SqliteHelper.KEY_MEASURE_VALUE));
        return new Measurement(value, field, true, timestamp);
    }

}
