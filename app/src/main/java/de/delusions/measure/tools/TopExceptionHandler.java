/**
 * 
 */
package de.delusions.measure.tools;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/**
 * 
 * @author sonja.pieper@workreloaded.com, 2013
 */
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {

    public static final String STACKTRACE_FILE = "stack.trace";
    private static final String TAG = TopExceptionHandler.class.getSimpleName();
    private Thread.UncaughtExceptionHandler defaultUEH;

    private Activity app = null;

    public TopExceptionHandler(final Activity app) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.app = app;
    }

    public void uncaughtException(final Thread t, final Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString() + "\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (final StackTraceElement element : arr) {
            report += "    " + element.toString() + "\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report += "--------- Cause ---------\n\n";
        final Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (final StackTraceElement element : arr) {
                report += "    " + element.toString() + "\n";
            }
        }
        report += "-------------------------------\n\n";

        try {
            final FileOutputStream trace = this.app.openFileOutput(STACKTRACE_FILE, Context.MODE_PRIVATE);
            trace.write(report.getBytes());
            trace.close();
        } catch (final IOException ioe) {
            Log.e(TAG, "failed writing stacktrace ", e);
        } finally {
            Log.d(TAG, "done writing stactrace");
            this.defaultUEH.uncaughtException(t, e);
        }
    }

}
