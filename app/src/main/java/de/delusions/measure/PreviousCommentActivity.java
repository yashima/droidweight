/**
 * 
 */
package de.delusions.measure;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.Measurement;

/**
 * 
 * @author sonja.pieper@workreloaded.com, 2013
 */
public class PreviousCommentActivity extends ListActivity {

    private static final String TAG = "PreviousCommentActivity";
    public static final String EDIT_TYPE = "type";

    private SqliteHelper db;
    private Cursor cursor;
    private int rowId;
    private Measurement measurement;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_comment);
        setTitle(R.string.menu_previous_comment);

        this.measurement = new MeasureIntentHelper(this, getIntent(), savedInstanceState).retrieveMeasure();
        Log.d(TAG, "measurement = " + this.measurement);

        this.db = new SqliteHelper(this);
        this.cursor = this.db.fetchCommentsOnly();
        Log.d(TAG, "cursor size : " + this.cursor.getCount());
        final CursorAdapter adapter = new CursorAdapter(this, this.cursor) {

            @Override
            public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.layout_comment_row, parent, false);
            }

            @Override
            public void bindView(final View view, final Context context, final Cursor cursor) {
                final TextView commentView = (TextView) view.findViewById(R.id.comment);
                final String comment = cursor.getString(cursor.getColumnIndex(SqliteHelper.KEY_COMMENT));
                commentView.setText(comment);
            }
        };
        setListAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.cursor.close();
        this.db.close();
    }

    @Override
    protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
        super.onListItemClick(l, v, position, id);
        final String comment = ((TextView) v.findViewById(R.id.comment)).getText().toString();
        final SqliteHelper dbHelper = new SqliteHelper(this);
        this.measurement.setComment(comment);
        dbHelper.updateMeasure(this.measurement.getId(), this.measurement);
        dbHelper.close();
        finish();
    }

    public static Intent createIntent(final Context context, final long rowId) {
        final Intent i = new Intent(context, PreviousCommentActivity.class);
        i.putExtra(SqliteHelper.KEY_ROWID, rowId);
        i.putExtra(EDIT_TYPE, UserPreferences.getDisplayField(context));
        return i;
    }
}
