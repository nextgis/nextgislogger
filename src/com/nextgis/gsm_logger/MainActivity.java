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
import android.widget.TextView;

import java.io.*;
import java.util.ArrayList;


public class MainActivity extends Activity {

    public static final String CSV_SEPARATOR = ";";

    public static final String dataDirPath =
            Environment.getExternalStorageDirectory().getAbsolutePath() +
            File.separator + "gsm_logger";
    public static final String csvLogFilePath =
            dataDirPath + File.separator + "gsm_time_log.csv";
    public static final String csvMarkFilePath =
            dataDirPath + File.separator + "gsm_time_marks.csv";

    public static final String csvLogHeader =
            "TimeStamp" + MainActivity.CSV_SEPARATOR +
                    "Active" + MainActivity.CSV_SEPARATOR +
                    "MCC" + MainActivity.CSV_SEPARATOR +
                    "MNC" + MainActivity.CSV_SEPARATOR +
                    "LAC" + MainActivity.CSV_SEPARATOR +
                    "CID" + MainActivity.CSV_SEPARATOR +
                    "RSSI";

    public static final String csvMarkHeader =
            "MarkName" + MainActivity.CSV_SEPARATOR + csvLogHeader;

    public static final String PARAM_PINTENT = "pendingIntent";
    public static final int STATUS_ERROR = 100;
    public static final int CODE_ERROR = 1;

    private GSMEngine gsmEngine;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        final TextView errorMessage = (TextView) findViewById(R.id.tv_error_message);
        boolean isMediaMounted = true;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            errorMessage.setText(R.string.ext_media_unmounted_msg);
            errorMessage.setVisibility(View.VISIBLE);
            isMediaMounted = false;

        } else {
            File dataDir = new File(dataDirPath);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        }

        gsmEngine = new GSMEngine(this);

        boolean isServiceRunning = isLoggerServiceRunning();

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
        markButton.setText(getString(R.string.btn_save_mark));
        markButton.setEnabled(isMediaMounted);

        final EditText markTextEditor = (EditText) findViewById(R.id.mark_text_editor);
        markTextEditor.setEnabled(isMediaMounted);
        markTextEditor.requestFocus();

        markButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    File csvFile = new File(csvMarkFilePath);
                    boolean isFileExist = csvFile.exists();
                    PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

                    if (!isFileExist) {
                        pw.println(csvMarkHeader);
                    }

                    String markName = markTextEditor.getText().toString();

                    ArrayList<GSMEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();

                    for (GSMEngine.GSMInfo gsmInfo : gsmInfoArray) {
                        StringBuilder sb = new StringBuilder();

                        String active = gsmInfo.isActive() ? "1"
                                : gsmInfo.getMcc() + "-" + gsmInfo.getMnc() + "-" +
                                gsmInfo.getLac() + "-" + gsmInfo.getCid();

                        sb.append(markName).append(MainActivity.CSV_SEPARATOR);
                        sb.append(gsmInfo.getTimeStamp()).append(MainActivity.CSV_SEPARATOR);
                        sb.append(active).append(MainActivity.CSV_SEPARATOR);
                        sb.append(gsmInfo.getMcc()).append(MainActivity.CSV_SEPARATOR);
                        sb.append(gsmInfo.getMnc()).append(MainActivity.CSV_SEPARATOR);
                        sb.append(gsmInfo.getLac()).append(MainActivity.CSV_SEPARATOR);
                        sb.append(gsmInfo.getCid()).append(MainActivity.CSV_SEPARATOR);
                        sb.append(gsmInfo.getRssi());

                        pw.println(sb.toString());
                    }

                    pw.close();

                } catch (FileNotFoundException e) {
                    errorMessage.setText(R.string.fs_error_msg);
                    errorMessage.setVisibility(View.VISIBLE);
                    markButton.setEnabled(false);
                    markTextEditor.setEnabled(false);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gsmEngine.onResume();
    }

    @Override
    protected void onPause() {
        gsmEngine.onPause();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == STATUS_ERROR && requestCode == CODE_ERROR) {
            final TextView errorMessage = (TextView) findViewById(R.id.tv_error_message);
            errorMessage.setText(R.string.fs_error_msg);
            errorMessage.setVisibility(View.VISIBLE);

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
