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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class LabeledInput extends TableRow {

    private final TextView labelText;
    private final EditText input;
    private final TextView unitText;
    private final Button plus;
    private final Button minus;

    private final MeasureType mType;
    private Measurement current;
    private final Context ctx;

    public LabeledInput(Context context, AttributeSet attr) {
        super(context, attr);

        // setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_component_input, this);

        this.ctx = context;
        // fetch the views
        this.labelText = (TextView) findViewById(R.id.label);
        this.input = (EditText) findViewById(R.id.input);
        this.unitText = (TextView) findViewById(R.id.unit);
        this.plus = (Button) findViewById(R.id.plus);
        this.minus = (Button) findViewById(R.id.minus);

        // set attributes
        final TypedArray a = context.obtainStyledAttributes(attr, R.styleable.MeasureInput);
        this.mType = MeasureType.valueOf(a.getString(R.styleable.MeasureInput_type));

        this.unitText.setText(this.mType.getUnit().retrieveUnitName(context));
        this.labelText.setText(context.getString(this.mType.getLabelId()));

        // try {
        // this.current = getInput();
        // } catch (final MeasurementException e) {
        // // TODO
        // }

        final boolean metric = UserPreferences.isMetric(context);
        final boolean buttons = a.getBoolean(R.styleable.MeasureInput_buttons, false);
        if (buttons) {
            this.plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LabeledInput.this.current.inc(metric);
                    LabeledInput.this.rewriteText();
                }
            });

            this.minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LabeledInput.this.current.dec(metric);
                    LabeledInput.this.rewriteText();
                }
            });
        } else {
            this.plus.setVisibility(View.GONE);
            this.minus.setVisibility(View.GONE);
        }

    }

    public void setCurrent(Measurement measurement) {
        this.current = measurement;
        rewriteText();
    }

    protected void rewriteText() {
        if (this.input != null) {
            this.input.setText(this.current.prettyPrint(getContext()));
        }
    }

    public Measurement getCurrent() throws MeasurementException {
        final String strValue = this.input.getText().toString();
        Log.d(MeasureActivity.TAG, "input=" + strValue);
        this.current = new Measurement(null, strValue, this.mType, UserPreferences.isMetric(this.ctx), new Date());
        return this.current;
    }
}
