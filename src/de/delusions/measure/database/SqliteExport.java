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

    private final static String EXPORT_DIR = "droidweight";
    private final static String EXPORT_FILE_NAME = "data.csv";
    private final static String EXPORT_FILE_HEADER = "value|type|date|metric|id|comment";
    private final static String DATE_STRING = "yyyy-MM-dd hh:mm:ss";
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_STRING);

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
                    writer.append(createLine(measurement));
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
        LineNumberReader reader = null;
        int result = 0;
        try {
            reader = new LineNumberReader(new FileReader(this.exportFile));
            String line = reader.readLine();
            while (line != null) {
                final Measurement measurement = readLine(line);
                if (measurement != null && !this.db.exists(measurement)) {
                    this.db.createMeasure(measurement);
                    result++;
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

    private String createLine(final Measurement measurement) {
        final StringBuffer line = new StringBuffer();
        line.append(measurement.getValue(this.metric)).append("|");
        line.append(measurement.getField().name()).append("|");
        line.append(DATE_FORMAT.format(measurement.getTimestamp())).append("|");
        line.append(this.metric).append("|");
        line.append(measurement.getId()).append("|");
        line.append(measurement.getComment());
        line.append("\n");
        return line.toString();
    }

    private Measurement readLine(final String line) throws MeasurementException {
        if (!line.equals(EXPORT_FILE_HEADER)) {
            Log.d(MeasureActivity.TAG, "parsing " + line);
            final String[] parts = line.split("\\|");
            final String value = parts[0];
            final MeasureType type = MeasureType.valueOf(parts[1]);
            final boolean metric = Boolean.parseBoolean(parts[3]);
            final Date date;
            try {
                date = DATE_FORMAT.parse(parts[2]);
            } catch (final ParseException e) {
                throw new MeasurementException(MeasurementException.ErrorId.PARSEERROR_DATE, DATE_STRING);
            }
            final Long id = parts.length > 4 ? Long.parseLong(parts[4]) : null;

            final String comment = parts.length > 5 ? parts[5] : null;

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
