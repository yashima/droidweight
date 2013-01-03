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
package de.delusions.measure.activities.bmi;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.delusions.measure.R;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;
import de.delusions.measure.ment.MeasurementException;

public class BmiCalc extends Activity {

    private EditText weightField;
    private EditText heightField;

    private Measurement weight;
    private Measurement height;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        setTitle(R.string.menu_calc);

        setContentView(R.layout.activity_calc);

        this.weightField = (EditText) findViewById(R.id.userWeight);
        this.heightField = (EditText) findViewById(R.id.userHeight);

        final Button goButton = (Button) findViewById(R.id.go);
        goButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View view) {
                BmiCalc.this.weight = tryGetValue(BmiCalc.this.weightField, MeasureType.WEIGHT);
                BmiCalc.this.height = tryGetValue(BmiCalc.this.heightField, MeasureType.HEIGHT);
                tryCalculateBmi();
            }

        });

    }

    private boolean isMetric() {
        return UserPreferences.isMetric(this);
    }

    private void tryCalculateBmi() {
        if (this.height != null && this.weight != null) {
            final float bmiValue = StatisticsFactory.calculateBmi(this.weight, this.height);
            final TextView result = (TextView) findViewById(R.id.Result);
            result.setText(String.format("bmi = %.2f", bmiValue));
            final BMI bmi = BMI.getBmi(bmiValue, this);
            if (bmi != null) {
                result.setTextColor(bmi.getColor(this));
            }
        } else {
            // TODO make this done with exception handling / test if it is already fixed
            Toast.makeText(this, getResources().getString(R.string.error_noinput), Toast.LENGTH_LONG).show();
        }
    }

    private Measurement tryGetValue(final EditText text, final MeasureType type) {
        Measurement result;
        final String input = text.getText().toString();
        try {
            result = new Measurement();
            result.setField(type);
            result.setUnit(type.getUnit());
            result.parseAndSetValue(input, isMetric());
        } catch (final MeasurementException e) {
            e.createToast(this, "tryGetValue");
            result = null;
        }
        return result;
    }
}
