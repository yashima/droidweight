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

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

/**
 * Simple measure database access helper class. Defines the basic CRUD operations for the measure app.
 */
public class SqliteHelper {

    public static final String KEY_MEASURE_VALUE = "weight";
    public static final String KEY_DATE = "measure_date";
    public static final String KEY_NAME = "name";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_UNIT = "unit";
    public static final String KEY_MAXVALUE = "maxValue";
    public static final String KEY_SMALLSTEP = "smallStep";
    public static final String KEY_BIGSTEP = "bigStep";
    public static final String KEY_LICENSE = "key";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_COLOR = "color";
    public static final String KEY_COMMENT = "comment";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private boolean open = false;

    /**
     * Database creation sql statement
     */
    private static final String WEIGHT_CREATE = "create table weightTable (_id integer primary key autoincrement, " + "weight real not null, "
            + "measure_date datetime default current_timestamp,comment text, name text default 'WEIGHT');";

    private static final String TRACKING_CREATE = "create table trackingTable(_id integer primary key autoincrement, enabled integer default 0,name text unique, unit text, maxValue real default 999, smallStep real default 1, bigStep real default 5, key integer, color integer);";

    private static final String DATABASE_NAME = "data";
    private static final String WEIGHT_TABLE = "weightTable";
    private static final String TRACKING_TABLE = "trackingTable";
    private static final int DATABASE_VERSION = 11;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(WEIGHT_CREATE);
            db.execSQL(TRACKING_CREATE);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (db.isReadOnly() || db.isDbLockedByOtherThreads()) {
                Log.w(MeasureActivity.TAG, "no database upgrade possible!");
            }
            Log.w(MeasureActivity.TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which may mess up old data");
            if (oldVersion < 8) {
                db.execSQL("DROP TABLE IF EXISTS sizeTable");
                db.execSQL("alter table weightTable add column name text default " + MeasureType.WEIGHT.name());
            }
            if (oldVersion < 10) {
                db.execSQL(TRACKING_CREATE);
                initTypes(db);
            }
            if (oldVersion < 11) {
                db.execSQL("alter table weightTable add column comment text");
            }
        }

    }

    public static void initTypes(final SQLiteDatabase db) {
        SqliteHelper.createType(db, MeasureType.WEIGHT);
        SqliteHelper.createType(db, MeasureType.HEIGHT);
        SqliteHelper.createType(db, MeasureType.BODYFAT);
        SqliteHelper.createType(db, MeasureType.WAIST);
    }

    /**
     * Constructor - takes the context to allow the database to be opened/created
     * 
     * @param ctx
     *            the Context within which to work
     */
    public SqliteHelper(final Context ctx) {
        this.mCtx = ctx;
        open();
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new instance of the database. If it cannot be
     * created, throw an exception to signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an initialization call)
     * @throws SQLException
     *             if the database could be neither opened or created
     */
    private SqliteHelper open() throws SQLException {
        Log.d(MeasureActivity.TAG, "open database");
        try {
            if (this.mDbHelper == null) {
                this.mDbHelper = new DatabaseHelper(this.mCtx);
            }
            this.mDb = this.mDbHelper.getWritableDatabase();
            if (this.mDb.isReadOnly() || this.mDb.isDbLockedByOtherThreads() || this.mDb.isDbLockedByCurrentThread()) {
                this.mDb.close();
                Log.w(MeasureActivity.TAG, "could not open database: locked or readonly");
            } else {
                this.open = true;
            }
        } catch (final Exception e) {
            Log.e(MeasureActivity.TAG, "open failed", e);
        }
        return this;
    }

    public void close() {
        Log.d(MeasureActivity.TAG, "close");
        try {
            if (this.open) {
                this.mDb.close();
            }
            this.mDbHelper.close();
            this.open = false;
        } catch (final Exception e) {
            Log.e(MeasureActivity.TAG, "close failed", e);
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    /**
     * Create a new note using the title and body provided. If the note is successfully created return the new rowId for
     * that note, otherwise return a -1 to indicate failure.
     * 
     * @param title
     *            the title of the note
     * @param body
     *            the body of the note
     * 
     * @return rowId or -1 if failed
     */
    public long createMeasure(final Measurement measurement) {
        Log.d(MeasureActivity.TAG, "createMeasure " + measurement);
        final ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_MEASURE_VALUE, measurement.getValue());
        initialValues.put(KEY_NAME, measurement.getField().name());
        initialValues.put(KEY_COMMENT, measurement.getComment());
        initialValues.put(KEY_DATE, measurement.getTimestamp() != null ? measurement.getTimestamp().getTime() : System.currentTimeMillis());
        return this.mDb.insert(WEIGHT_TABLE, null, initialValues);
    }

    static long createType(final SQLiteDatabase db, final MeasureType type) {
        Log.d(MeasureActivity.TAG, "createType " + type);
        final ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, type.name());
        initialValues.put(KEY_MAXVALUE, type.getMaxValue());
        initialValues.put(KEY_SMALLSTEP, type.getSmallStep());
        initialValues.put(KEY_BIGSTEP, type.getBigStep());
        initialValues.put(KEY_UNIT, type.getUnit().name());
        // initialValues.put(KEY_ENABLED, type.isEnabled());
        initialValues.put(KEY_LICENSE, type.getLicenseKey());
        initialValues.put(KEY_COLOR, type.getColor());
        return db.insert(TRACKING_TABLE, null, initialValues);
    }

    public long createType(final MeasureType type) {
        return createType(this.mDb, type);
    }

    public boolean exists(final Measurement measurement) {
        final boolean result;
        if (measurement.getId() != null) {
            final Cursor cursor = fetchById(measurement.getId());
            result = cursor != null && cursor.getCount() > 0;
        } else {
            result = false;
        }
        return result;
    }

    public boolean exists(final MeasureType type) {
        final boolean result;
        if (type.name() != null) {
            final Cursor cursor = fetchByName(type.name());
            result = cursor != null && cursor.getCount() > 0;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Update the measure using the details provided. The measure to be updated is specified using the rowId, and it is
     * altered to use the title and body values passed in
     * 
     * @param rowId
     *            id of measure to update
     * @param weight
     *            value to set measure weight to
     * @param date
     *            value to set measure date to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateMeasure(final long rowId, final Measurement measurement) {
        Log.d(MeasureActivity.TAG, "SqliteHelper: updateMeasure " + rowId);
        final ContentValues args = new ContentValues();
        args.put(KEY_MEASURE_VALUE, measurement.getValue());
        args.put(KEY_DATE, measurement.getTimestamp().getTime());
        args.put(KEY_COMMENT, measurement.getComment());
        return this.mDb.update(WEIGHT_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateType(final long rowId, final MeasureType type) {
        final ContentValues args = new ContentValues();
        args.put(KEY_NAME, type.name());
        args.put(KEY_MAXVALUE, type.getMaxValue());
        args.put(KEY_SMALLSTEP, type.getSmallStep());
        args.put(KEY_BIGSTEP, type.getBigStep());
        args.put(KEY_UNIT, type.getUnit().name());
        // args.put(KEY_ENABLED, type.isEnabled());
        args.put(KEY_COLOR, type.getColor());
        return this.mDb.update(TRACKING_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId
     *            id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteNote(final long rowId) {
        return this.mDb.delete(WEIGHT_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public int deleteAll() {
        return this.mDb.delete(WEIGHT_TABLE, null, null);
    }

    public boolean isEmptyWeight() {
        return fetchFirst(MeasureType.WEIGHT).getCount() == 0;
    }

    /**
     * Return a Cursor over the list of all measures in the database
     * 
     * @param fieldName
     *            TODO
     * 
     * @return Cursor over all measures
     */
    public Cursor fetchAll(final MeasureType fieldName) {
        Log.d(MeasureActivity.TAG, "fetchAllMeasures " + fieldName);
        final String[] selectionArgs = { fieldName.name() };
        return this.mDb.query(WEIGHT_TABLE, null, "name=?", selectionArgs, null, null, "measure_date DESC");
    }

    public Cursor fetchByName(final String name) {
        Log.d(MeasureActivity.TAG, "fetchByName " + name);
        final String[] selectionArgs = { name };
        return this.mDb.query(TRACKING_TABLE, null, "name=?", selectionArgs, null, null, null);
    }

    /**
     * Return a Cursor over the list of all measures in the database
     * 
     * @param fieldName
     *            TODO
     * 
     * @return Cursor over all measures
     */
    public Cursor fetchAll() {
        Log.d(MeasureActivity.TAG, "fetchAllMeasures");
        return this.mDb.query(WEIGHT_TABLE, null, null, null, null, null, null);
    }

    public Cursor fetchTypes() {
        Log.d(MeasureActivity.TAG, "fetchTypes");
        return this.mDb.query(TRACKING_TABLE, null, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the measure that matches the given rowId
     * 
     * @param rowId
     *            id of note to retrieve
     * @return Cursor positioned to matching measure, if found
     * @throws SQLException
     *             if measure could not be found/retrieved
     */
    public Cursor fetchById(final long rowId) throws SQLException {
        final Cursor mCursor = this.mDb.query(true, WEIGHT_TABLE, null, KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    public Cursor fetchByDate(final Date dateAfter, final MeasureType field) {
        final String[] columns = { KEY_ROWID, KEY_MEASURE_VALUE, KEY_DATE, KEY_COMMENT };
        final String selection = "measure_date > ? and name=?";
        final String[] selectionArgs = { dateAfter.getTime() + "", field.name() };
        return this.mDb.query(WEIGHT_TABLE, columns, selection, selectionArgs, null, null, "measure_date ASC");
    }

    /**
     * Fetches the last measure by date
     * 
     * @param field
     *            TODO
     * 
     * @return
     * @throws SQLException
     */
    public Cursor fetchLast(final MeasureType field) throws SQLException {
        Log.d(MeasureActivity.TAG, "fetchLastMeasure");
        final String[] selectionArgs = { field.name() };
        final Cursor cursor = this.mDb.query(WEIGHT_TABLE, null, "name=?", selectionArgs, null, null, "measure_date DESC", "1");
        cursor.moveToLast();
        return cursor;
    }

    /**
     * Fetches the first measure by date
     * 
     * @param field
     *            TODO
     * 
     * @return
     */
    public Cursor fetchFirst(final MeasureType field) {
        Log.d(MeasureActivity.TAG, "fetchFirstWeight");
        final String[] selectionArgs = { field.name() };
        final Cursor cursor = this.mDb.query(WEIGHT_TABLE, null, "name=?", selectionArgs, null, null, "measure_date ASC", "1");
        cursor.moveToLast();
        Log.d(MeasureActivity.TAG, "fetchFirstWeight count=" + cursor.getCount());
        return cursor;
    }

    public static Date getTimestamp(final Cursor cursor) {
        if (cursor.getCount() > 0 && !cursor.isNull(cursor.getColumnIndex(SqliteHelper.KEY_DATE))) {
            return new Date(cursor.getLong(cursor.getColumnIndex(SqliteHelper.KEY_DATE)));
        } else {
            return null;
        }
    }

}
