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
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;
import de.delusions.measure.R;
import de.delusions.measure.ment.Measurement;

public class MeasureDisplay extends TableRow {
    private final TextView labelText;
    private final TextView valueText;
    private final TextView unitText;

    public MeasureDisplay(Context context, AttributeSet attr) {
        super(context, attr);

        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_component_display, this);

        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);

        this.labelText = (TextView) findViewById(R.id.label);
        this.valueText = (TextView) findViewById(R.id.value);
        this.unitText = (TextView) findViewById(R.id.unit);

        // set attributes
        final TypedArray a = context.obtainStyledAttributes(attr, R.styleable.MeasureInput);
        final String labelStr = a.getString(R.styleable.MeasureInput_label);
        this.labelText.setText(labelStr);
    }

    public void display(Measurement m) {
        if (m != null) {
            this.valueText.setText(m.prettyPrint(getContext()));
            this.unitText.setText(m.getUnit().retrieveUnitName(getContext()));
        }
    }

    public void display(Measurement m, String subZeroLabel) {
        if (m != null) {
            this.valueText.setText(m.prettyPrint(getContext()));
            this.unitText.setText(m.getUnit().retrieveUnitName(getContext()));
            if (m.getValue() < 0) {
                this.labelText.setText(subZeroLabel);
                // TODO remove stupid hack
                String valueStr = m.prettyPrint(getContext());
                valueStr = valueStr.replace("-", "");
                this.valueText.setText(valueStr);
            } else {
                this.valueText.setText(m.prettyPrint(getContext()));
            }
        }
    }

    public void display(float value) {
        this.valueText.setText(String.format("%.2f", value));
        this.unitText.setVisibility(View.GONE);
    }

    public void display(float value, int color) {
        this.display(value);
        this.valueText.setTextColor(color);
    }

    public void display(String value, int color) {
        this.valueText.setText(value);
        this.valueText.setTextColor(color);
    }

    public void display(Date date) {
        if (date == null) {
            this.valueText.setText(R.string.stat_eta_never);
        } else {
            this.valueText.setText(DateUtils.formatDateTime(getContext(), date.getTime(), DateUtils.FORMAT_ABBREV_ALL));
        }
    }
}
