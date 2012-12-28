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
package de.delusions.measure;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow.LayoutParams;
import de.delusions.measure.components.InputRecorder;
import de.delusions.measure.database.SqliteHelper;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class MeasureFastEdit extends Activity {

    private final List<InputRecorder> recorders = new ArrayList<InputRecorder>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MeasureActivity.TAG, "onCreate MeasureFastEdit");
        setContentView(R.layout.activity_fastinput);
        setTitle(getResources().getString(R.string.activity_createmeasure));
        Log.d(MeasureActivity.TAG, "onCreate MeasureFastEdit 2");

        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        final LinearLayout inputlayout = (LinearLayout) findViewById(R.id.inputlayout);
        for (final MeasureType type : MeasureType.getEnabledTypes(this)) {
            Log.d(MeasureActivity.TAG, "onCreate MeasureFastEdit adding " + type);
            final InputRecorder recorder = new InputRecorder(this, null);
            recorder.setLayoutParams(layoutParams);
            recorder.setCurrent(populateInput(type));
            this.recorders.add(recorder);
            inputlayout.addView(recorder);
        }

        createConfirmButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void createConfirmButton() {
        final Button confirmButton = (Button) findViewById(R.id.ok);
        confirmButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                final SqliteHelper db = new SqliteHelper(MeasureFastEdit.this);
                db.open();
                setResult(RESULT_OK);
                try {
                    saveMeasurements(db);
                    finish();
                } catch (final MeasurementException e) {
                    e.createToast(MeasureFastEdit.this, "confirmButton");
                } finally {
                    db.close();
                }
            }

        });
    }

    private void saveMeasurements(final SqliteHelper db) throws MeasurementException {
        for (final InputRecorder input : this.recorders) {
            final Measurement measurement = input.getCurrent();
            db.createMeasure(measurement);
        }
    }

    private Measurement populateInput(final MeasureType type) {
        final SqliteHelper db = new SqliteHelper(MeasureFastEdit.this);
        final Cursor cursor = db.fetchLast(type);
        Measurement measurement;
        if (cursor != null && cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                measurement = new Measurement(cursor);
            } catch (final MeasurementException e) {
                e.createToast(this, "failed to create from cursor in fastinput");
                measurement = type.zero(this);
            }
        } else {
            measurement = type.zero(this);
        }
        Log.d(MeasureActivity.TAG, "populate input with " + measurement);
        cursor.close();
        db.close();
        return measurement;
    }
}
