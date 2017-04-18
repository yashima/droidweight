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

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class SqliteExport extends AsyncTask<Boolean, Void, Integer> {

    private static final String TAG = SqliteExport.class.getSimpleName();

    private final static String EXPORT_DIR = "droidweight";
    private final static String EXPORT_FILE_NAME = "data.csv";
    private final static String EXPORT_FILE_HEADER = "value|type|date|metric|id|comment";
    private final static String DATE_STRING = "yyyy-MM-dd hh:mm:ss";
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_STRING);

    private final static int VALUE_POS = 0;
    private final static int TYPE_POS = 1;
    private final static int TIMESTAMP_POS = 2;
    private final static int METRIC_POS = 3;
    private final static int ID_POS = 4;
    private final static int COMMENT_POS = 5;

    private final ProgressDialog dialog;
    private final SqliteHelper db;
    private final boolean metric;
    boolean export;
    private final Activity a;
    private final File exportFile;
    private final String modeStr;

    public SqliteExport(final Activity a, final boolean export) throws MeasurementException {
        this.a = a;
        this.export = export;
        this.db = new SqliteHelper(a);
        this.dialog = new ProgressDialog(a);
        this.metric = UserPreferences.isMetric(a);
        this.exportFile = openExportFile(export);
        this.modeStr = this.a.getString(export ? R.string.exporter_export : R.string.exporter_import);
    }

    @Override
    protected Integer doInBackground(final Boolean... export) {
        Log.d(TAG, "doInBackground started");
        try {
            if (this.export) {
                return exportMeasurements();
            } else {
                return importMeasurements();
            }
        } catch (final MeasurementException e) {
            return -1;
        }

    }

    @Override
    protected void onPreExecute() {
        this.dialog.setMessage(String.format(this.a.getString(R.string.exporter_dialog_message), this.modeStr));
        this.dialog.show();
    }

    @Override
    protected void onPostExecute(final Integer success) {
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }
        if (this.a instanceof MeasureActivity) {
            ((MeasureActivity) this.a).refreshListView();
        }
        if (success > 0) {
            Toast.makeText(this.a, String.format(this.a.getString(R.string.exporter_toast_success), this.modeStr, success), Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(this.a, String.format(this.a.getString(R.string.exporter_toast_fail), this.modeStr), Toast.LENGTH_SHORT).show();
        }
    }

    public Integer exportMeasurements() throws MeasurementException {
        Log.d(TAG, "exportMeasurements started");
        int numberOfMeasures = 0;
        FileWriter writer = null;
        try {
            writer = new FileWriter(this.exportFile);
            writer.append(EXPORT_FILE_HEADER);
            writer.append("\n");
            Log.d(MeasureActivity.TAG, "exportMeasurements: header written");
            final Cursor cursor = this.db.fetchAll();
            Log.d(MeasureActivity.TAG, "exportMeasurements: " + cursor.getCount());
            if (cursor.getCount() > 0) {
                while (!cursor.isLast()) {
                    cursor.moveToNext();
                    final Measurement measurement = Measurement.create(cursor);
                    writer.append(createLine(measurement, this.metric));
                    numberOfMeasures++;
                }
            }
        } catch (final IOException e) {
            Log.e(MeasureActivity.TAG, "Oops something bad happened", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (final IOException e) { /* ignore */
            }
            this.db.close();
        }
        return numberOfMeasures;
    }

    public int importMeasurements() throws MeasurementException {
        Log.d(TAG, "importMeasurements started");
        LineNumberReader reader = null;
        int result = 0;
        try {
            reader = new LineNumberReader(new FileReader(this.exportFile));
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("value")) { // header
                    Log.d(TAG, "processing line " + line);
                    final Measurement measurement = readLine(line);
                    if (measurement != null && !this.db.exists(measurement)) {
                        this.db.createMeasure(measurement);
                        result++;
                    }
                }
                line = reader.readLine();
            }
        } catch (final FileNotFoundException e) {
            throw new MeasurementException(MeasurementException.ErrorId.EXPORT_FILEMISSING);
        } catch (final IOException e) {
            throw new MeasurementException(MeasurementException.ErrorId.EXPORT_READFILE);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (final IOException e) { /* ignore */
            }
            this.db.close();
        }
        return result;
    }

    public static String createLine(final Measurement measurement, final boolean metric) {
        final int numberOfParts = 6;
        final String[] parts = new String[numberOfParts];
        parts[VALUE_POS] = Float.toString(measurement.getValue(metric));
        parts[TYPE_POS] = measurement.getField().name();
        parts[TIMESTAMP_POS] = DATE_FORMAT.format(measurement.getTimestamp());
        parts[METRIC_POS] = Boolean.toString(metric);
        parts[ID_POS] = measurement.getId() != null ? Long.toString(measurement.getId()) : "";
        parts[COMMENT_POS] = measurement.getComment();

        final StringBuffer line = new StringBuffer();
        for (final String part : parts) {
            line.append(part);
            line.append("|");
        }
        line.append("\n");
        return line.toString();
    }

    public static Measurement readLine(final String line) throws MeasurementException {
        if (!line.equals(EXPORT_FILE_HEADER)) {
            Log.d(MeasureActivity.TAG, "parsing " + line);
            final String[] parts = line.split("\\|");
            final String value = retrieveStringFromLine(parts, VALUE_POS);
            final MeasureType type = MeasureType.valueOf(retrieveStringFromLine(parts, TYPE_POS));
            final boolean metric = Boolean.parseBoolean(retrieveStringFromLine(parts, METRIC_POS));
            final Date date;
            try {
                date = DATE_FORMAT.parse(retrieveStringFromLine(parts, TIMESTAMP_POS));
            } catch (final ParseException e) {
                throw new MeasurementException(MeasurementException.ErrorId.PARSEERROR_DATE, DATE_STRING);
            }
            final String idString = retrieveStringFromLine(parts, ID_POS);
            final Long id = idString != null ? Long.parseLong(idString) : -1l;
            final String comment = retrieveStringFromLine(parts, COMMENT_POS);

            final Measurement measurement = new Measurement();
            measurement.setId(id);
            measurement.setField(type);
            measurement.setTimestamp(date);
            measurement.parseAndSetValue(value, metric);
            measurement.setComment(comment);
            return measurement;
        } else {
            Log.d(MeasureActivity.TAG, "ignoring header");
            return null;
        }
    }

    private static String retrieveStringFromLine(final String[] parts, final int pos) {
        final String result;
        if (parts.length > pos) {
            final String part = parts[pos];
            if (part != null && !part.equals("") && !part.equals("null") && !part.equals("NULL")) {
                result = part;
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    private File openExportFile(final boolean create) throws MeasurementException {
        final File sdPath = getSDPath();
        final File exportFile = new File(sdPath, EXPORT_FILE_NAME);
        if (create) {
            try {
                exportFile.createNewFile();
            } catch (final IOException e) {
                Log.e(MeasureActivity.TAG, "openExportFile failed", e);
                throw new MeasurementException(MeasurementException.ErrorId.EXPORT_FILECREATION, e.getMessage());
            }
        }
        return exportFile;
    }

    private File getSDPath() throws MeasurementException {
        final File sdDir = new File(Environment.getExternalStorageDirectory(), "");
        final File dwDir = new File(sdDir, EXPORT_DIR);
        if (!dwDir.exists()) {
            dwDir.mkdirs();
        }
        return dwDir;
    }
}
