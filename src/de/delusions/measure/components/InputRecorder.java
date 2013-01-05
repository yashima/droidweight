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
package de.delusions.measure.components;

import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class InputRecorder extends RelativeLayout {

    private final ImageButton plus;
    private final ImageButton minus;
    private final ImageButton plusplus;
    private final ImageButton minusminus;
    private final TextView current;
    private Measurement currentMeasure;
    private final Context context;

    public InputRecorder(final Context context, final AttributeSet attr) {
        super(context, attr);

        this.context = context;
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_component_plusminus, this);

        this.plus = (ImageButton) findViewById(R.id.plus);
        this.minus = (ImageButton) findViewById(R.id.minus);
        this.plusplus = (ImageButton) findViewById(R.id.plusplus);
        this.minusminus = (ImageButton) findViewById(R.id.minusminus);
        this.current = (TextView) findViewById(R.id.currentWeight);

        this.current.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View v) {
                Log.d(MeasureActivity.TAG, "test clicky");
                editCurrent().show();
            }
        });

        this.plus.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                changeMeasure(true, true);
            }
        });

        this.plusplus.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                changeMeasure(true, false);
            }
        });

        this.minus.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                changeMeasure(false, true);
            }
        });

        this.minusminus.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                changeMeasure(false, false);
            }
        });
    }

    private void changeMeasure(final boolean inc, final boolean small) {
        final Float step = small ? this.currentMeasure.getField().getSmallStep() : this.currentMeasure.getField().getBigStep();
        if (inc) {
            this.currentMeasure.inc(isMetric(), step);
        } else {
            this.currentMeasure.dec(isMetric(), step);
        }
        rewriteText();
    }

    private boolean isMetric() {
        return UserPreferences.isMetric(this.context);
    }

    public void rewriteText() {
        if (this.currentMeasure.getValue() < 100) {
            this.current.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        } else {
            this.current.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        }
        this.current.setText(this.currentMeasure.prettyPrint(getContext()));
    }

    public void setCurrent(final Measurement measurement) {
        this.currentMeasure = measurement;
        final TextView labelView = (TextView) findViewById(R.id.label);
        final TextView unitView = (TextView) findViewById(R.id.unit);
        if (this.currentMeasure != null) {
            unitView.setText(this.currentMeasure.getUnit().retrieveUnitName(this.context));
            labelView.setText(this.currentMeasure.getField().getLabelId());
        }
        rewriteText();
    }

    public Measurement getCurrent() {
        this.currentMeasure.setTimestamp(new Date(System.currentTimeMillis()));
        return this.currentMeasure;
    }

    public AlertDialog editCurrent() {
        final LayoutInflater factory = LayoutInflater.from(this.context);
        final View textEntryView = factory.inflate(R.layout.dialog_edit_text, null);
        return new AlertDialog.Builder(this.context).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(this.currentMeasure.getField().getLabelId()).setView(textEntryView)
                .setPositiveButton(R.string.button_go, new DialogInterface.OnClickListener() {

                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        final EditText input = (EditText) textEntryView.findViewById(R.id.dialoginput);
                        final String strValue = input.getText().toString();
                        try {
                            InputRecorder.this.currentMeasure.parseAndSetValue(strValue, UserPreferences.isMetric(InputRecorder.this.context));
                            rewriteText();
                        } catch (final MeasurementException e) {
                            e.createToast(InputRecorder.this.context, "InputRecorder.editCurrent");
                        }
                    }
                }).create();
    }
}
