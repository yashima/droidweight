/**
 * 
 */
package de.delusions.measure;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

/**
 * 
 * @author sonja.pieper@workreloaded.com, 2013
 */
public class MeasureIntentHelper {

    private static final String TAG = "MeasureIntentHelper";
    public static final String EDIT_TYPE = "type";

    private Context context;
    private Intent intent;
    private final Bundle savedInstanceState;

    public MeasureIntentHelper(final Context context, final Intent intent, final Bundle savedInstanceState) {
        this.context = context;
        this.intent = intent;
        this.savedInstanceState = savedInstanceState;
    }

    public Measurement retrieveMeasure() {
        final Long mRowId = retrieveRowIdFromExtras();
        final MeasureType field = retrieveMeasureFieldFromExtras();
        Log.d(TAG, "mRowId " + mRowId);

        return retrieveMeasureFromDatabase(mRowId, field);
    }

    private Long retrieveRowIdFromExtras() {
        Long mRowId = this.savedInstanceState == null ? null : (Long) this.savedInstanceState.getSerializable(SqliteHelper.KEY_ROWID);
        if (mRowId == null) {
            final Bundle extras = this.intent.getExtras();
            mRowId = extras != null ? extras.getLong(SqliteHelper.KEY_ROWID) : null;
            Log.d(MeasureActivity.TAG, "MeasureEdit:retrieveRowIdFromExtras:" + mRowId);
        }
        return mRowId;
    }

    private MeasureType retrieveMeasureFieldFromExtras() {
        MeasureType field = this.savedInstanceState == null ? null : (MeasureType) this.savedInstanceState.getSerializable(EDIT_TYPE);
        if (field == null) {
            final Bundle extras = this.intent.getExtras();
            field = extras != null ? (MeasureType) extras.getSerializable(EDIT_TYPE) : null;
            Log.d(MeasureActivity.TAG, "MeasureEdit:retrieveMeasureFieldFromExtras:" + field);
        }
        return field;
    }

    private Measurement retrieveMeasureFromDatabase(final Long mRowId, final MeasureType field) {
        final Measurement result;
        final SqliteHelper mDbHelper = new SqliteHelper(this.context);
        final Cursor cursor = mDbHelper.fetchById(mRowId);
        if (cursor.getCount() != 0 && field != null) {
            result = field.createMeasurement(cursor);
            result.setId(mRowId);
        } else {
            Log.w(MeasureActivity.TAG, "MeasureEdit:retrieveMeasureFromDatabase:measure not found");
            result = null;
        }

        cursor.close();
        mDbHelper.close();
        Log.d(TAG, "retrievedMeasure " + result);
        return result;
    }
}
