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
package de.delusions.measure.ment;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;

public class MeasurementException extends Exception {

    public enum ErrorId {
        SUBZERO(R.string.error_subzero),
        TOOLARGE(R.string.error_toolarge),
        NOINPUT(R.string.error_noinput),
        NONUMBER(R.string.error_nonumber),
        PARSEERROR(R.string.error_parseerror),
        PARSEERROR_DATE(R.string.error_parse_date),
        EXPORT_FILECREATION(R.string.error_exportfilecreate),
        EXPORT_NOSDCARD(R.string.error_nosdcard),
        EXPORT_FILEEXISTS(R.string.error_fileexists),
        EXPORT_FILEMISSING(R.string.error_filemissing),
        EXPORT_READFILE(R.string.error_readfile),
        UNKNOWN(R.string.error_unknown);

        int messageId;

        private ErrorId(int messageId) {
            this.messageId = messageId;
        }

        public int getMesssageId() {
            return this.messageId;
        }
    }

    private final ErrorId id;

    public MeasurementException(ErrorId id) {
        super();
        this.id = id;
    }

    public MeasurementException(ErrorId id, String string) {
        super(string);
        this.id = id;
    }

    private static final long serialVersionUID = 1L;

    public ErrorId getId() {
        return this.id;
    }

    public void createToast(Context ctx, String logMsg) {
        if (Log.isLoggable(MeasureActivity.TAG, Log.DEBUG)) {
            Log.e(MeasureActivity.TAG, logMsg + "|" + this.id, this);
        } else {
            Log.e(MeasureActivity.TAG, logMsg + "|" + this.id + "|" + getMessage());
        }
        Toast.makeText(ctx, String.format(ctx.getResources().getString(this.id.getMesssageId()), getMessage()), Toast.LENGTH_LONG).show();
    }

}
