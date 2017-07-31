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
package de.delusions.measure.activities.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import de.delusions.measure.R;
import de.delusions.measure.ment.Unit;

public class FloatEditTextPreference extends EditTextPreference {

    private Unit unit;

    public FloatEditTextPreference(final Context context) {
        super(context);
    }

    public FloatEditTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MeasureInput);
        final String unitStr = a.getString(R.styleable.MeasureInput_unit);
        this.unit = Unit.valueOf(unitStr.toUpperCase());
    }

    @Override
    protected String getPersistedString(final String defaultReturnValue) {
        final Float storedValue = getPersistedFloat(-1);
        Float returnValue;
        if (UserPreferences.isMetric(getContext())) {
            returnValue = storedValue;
        } else {
            returnValue = this.unit.convertToImperial(storedValue);
        }
        return String.valueOf(returnValue);
    }

    @Override
    protected boolean persistString(final String value) {
        try {
            final Float floatValue = Float.parseFloat(value);
            final Float storeValue;
            if (UserPreferences.isMetric(getContext())) {
                storeValue = floatValue;
            } else {
                storeValue = this.unit.convertToMetric(floatValue);
            }
            return persistFloat(storeValue);
        } catch (final NumberFormatException e) {
            Log.e("FloatEditTextPreference", "could not be parsed as float: " + value);
            return false;
        }
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);
        getEditText().setText(getPersistedString(""));
    }

}
