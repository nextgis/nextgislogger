package com.nextgis.gsm_logger;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.*;


public class MainActivity extends Activity {

    private GSMEngine mGSMEngine;

    private String mCSVMarkFileName = "gsm_time_marks.csv";
    public static final String CSV_SEPARATOR = ";";

    public static final String PARAM_PINTENT = "pendingIntent";
    public final static int STATUS_ERROR = 100;
    public static final int CODE_ERROR = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isMediaMounted = true;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.ext_media_unmounted, Toast.LENGTH_LONG).show();
            isMediaMounted = false;
        }

        mGSMEngine = new GSMEngine(this);

        mCSVMarkFileName = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
                File.separator + mCSVMarkFileName;

        boolean isServiceRunning = isLoggerServiceRunning();

        setContentView(R.layout.main_activity);

        final Button serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
        serviceOnOffButton.setText(getString(isServiceRunning
                ? R.string.btn_service_stop : R.string.btn_service_start));
        serviceOnOffButton.setEnabled(isMediaMounted);

        final ProgressBar serviceProgressBar =
                (ProgressBar) findViewById(R.id.service_progress_bar);
        serviceProgressBar.setVisibility(isServiceRunning ? View.VISIBLE : View.INVISIBLE);

        serviceOnOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Service can be stopped, but still visible in the system as working,
                // therefore, we need to use isLoggerServiceRunning()
                if (isLoggerServiceRunning()) {
                    stopService(new Intent(getApplicationContext(), LoggerService.class));
                    serviceOnOffButton.setText(getString(R.string.btn_service_start));
                    serviceProgressBar.setVisibility(View.INVISIBLE);

                } else {
                    PendingIntent pintent = createPendingResult(CODE_ERROR, new Intent(), 0);
                    Intent intent = new Intent(getApplicationContext(), LoggerService.class)
                            .putExtra(PARAM_PINTENT, pintent);

                    startService(intent);
                    serviceOnOffButton.setText(getString(R.string.btn_service_stop));
                    serviceProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        final Button markButton = (Button) findViewById(R.id.btn_mark);
        markButton.setText(getString(R.string.btn_make_mark));
        markButton.setEnabled(isMediaMounted);

        final EditText markTextEditor = (EditText) findViewById(R.id.mark_text_editor);

        markButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (markTextEditor.isShown()) {

                    try {
                        File csvFile = new File(mCSVMarkFileName);
                        boolean isFileExist = csvFile.exists();
                        PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

                        if (!isFileExist) {
                            StringBuilder sbHeader = new StringBuilder();
                            sbHeader.append("MarkName").append(MainActivity.CSV_SEPARATOR)
                                    .append("TimeStamp").append(MainActivity.CSV_SEPARATOR)
                                    .append("CellID").append(MainActivity.CSV_SEPARATOR)
                                    .append("LAC").append(MainActivity.CSV_SEPARATOR)
                                    .append("[Neighbor_CellID,Neighbor_LAC,Neighbor_RSSI]...");

                            pw.println(sbHeader.toString());
                        }

                        pw.println(markTextEditor.getText().toString() +
                                CSV_SEPARATOR + mGSMEngine.getGSMInfo());
                        pw.close();

                    } catch (FileNotFoundException e) {
                        Toast.makeText(getApplicationContext(),
                                R.string.mark_fs_error, Toast.LENGTH_LONG).show();
                        markButton.setEnabled(false);
                    }

                    markTextEditor.setVisibility(View.GONE);
                    markButton.setText(getString(R.string.btn_make_mark));

                } else {
                    markTextEditor.setText("");
                    markTextEditor.setVisibility(View.VISIBLE);
                    markTextEditor.requestFocus();
                    markButton.setText(getString(R.string.btn_save_mark));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGSMEngine.onResume();
    }

    @Override
    protected void onPause() {
        mGSMEngine.onPause();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == STATUS_ERROR && requestCode == CODE_ERROR) {
            Toast.makeText(this, R.string.log_fs_error, Toast.LENGTH_LONG).show();

            Button serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
            serviceOnOffButton.setText(getString(R.string.btn_service_start));
            serviceOnOffButton.setEnabled(false);

            ProgressBar serviceProgressBar = (ProgressBar) findViewById(R.id.service_progress_bar);
            serviceProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    public boolean isLoggerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {

            if (LoggerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
