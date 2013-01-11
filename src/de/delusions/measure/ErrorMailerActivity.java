/**
 * 
 */
package de.delusions.measure;

import java.io.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import de.delusions.measure.tools.TopExceptionHandler;

/**
 * 
 * @author sonja.pieper@workreloaded.com, 2013
 */
public class ErrorMailerActivity extends Activity {

    private static final String TAG = ErrorMailerActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_startup);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        if (stacktraceExists()) {
            final Intent sendIntent = createSendStacktraceIntent();
            final Intent chooser = Intent.createChooser(sendIntent, getText(R.string.crash_report));
            startActivityForResult(chooser, 1);
            deleteStacktrace();
        } else {
            startTabs();
        }

    }

    protected void startTabs() {
        startActivity(new Intent(this, MeasureTabs.class));
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startTabs();
    }

    protected boolean stacktraceExists() {
        return new File(getFilesDir(), TopExceptionHandler.STACKTRACE_FILE).exists();
    }

    protected void deleteStacktrace() {
        deleteFile(TopExceptionHandler.STACKTRACE_FILE);
    }

    protected Intent createSendStacktraceIntent() {

        final String subject = "Droidweight Error report";
        final String trace = readStacktrace();
        final String body = "Mail this to droidweight@googlecode.com: " + "\n\n" + trace + "\n\n";

        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "droidweight@googlecode.com" });
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("message/rfc822");

        return sendIntent;
    }

    protected String readStacktrace() {
        String trace = "";
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(TopExceptionHandler.STACKTRACE_FILE)));
            String line = null;

            while ((line = reader.readLine()) != null) {
                trace += line + "\n";
            }
        } catch (final FileNotFoundException fnfe) {
            // no use logging a crash when reading the stacktrace
        } catch (final IOException ioe) {
            // no use logging a crash when reading the stacktrace
        }
        return trace;
    }

}
