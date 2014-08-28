package com.nextgis.gsm_logger;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

    public static final int minPeriodSec = 1;
    public static final int maxPeriodSec = 3600;
    public static final String PREF_PERIOD_SEC = "periodSec";

    public static int loggerPeriodSec = minPeriodSec;

    private GSMEngine gsmEngine;

    private Button serviceOnOffButton;
    private ProgressBar serviceProgressBar;
    private Button markButton;
    private EditText markTextEditor;
    private Button setPeriodButton;
    private EditText periodEditor;
    private TextView errorMessage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        errorMessage = (TextView) findViewById(R.id.tv_error_message);
        boolean isMediaMounted = true;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            isMediaMounted = false;

        } else {
            File dataDir = new File(dataDirPath);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        }

        gsmEngine = new GSMEngine(this);

        boolean isServiceRunning = isLoggerServiceRunning();

        serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
        serviceOnOffButton.setText(getString(isServiceRunning
                ? R.string.btn_service_stop : R.string.btn_service_start));

        serviceProgressBar = (ProgressBar) findViewById(R.id.service_progress_bar);
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

        markButton = (Button) findViewById(R.id.btn_mark);
        markButton.setText(getString(R.string.btn_save_mark));

        markTextEditor = (EditText) findViewById(R.id.mark_text_editor);
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
                                : gsmInfoArray.get(0).getMcc() + "-" +
                                gsmInfoArray.get(0).getMnc() + "-" +
                                gsmInfoArray.get(0).getLac() + "-" +
                                gsmInfoArray.get(0).getCid();

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
                    setState(R.string.fs_error_msg, true);
                }
            }
        });

        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        final Editor prefEd = pref.edit();

        loggerPeriodSec = pref.getInt(PREF_PERIOD_SEC, minPeriodSec);

        setPeriodButton = (Button) findViewById(R.id.btn_set_period);

        periodEditor = (EditText) findViewById(R.id.period_editor);
        periodEditor.setText(loggerPeriodSec + "");

        setPeriodButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String sPeriod = periodEditor.getText().toString();

                if (sPeriod.length() > 0) {
                    int sec = Integer.parseInt(sPeriod);

                    if (minPeriodSec <= sec && sec <= maxPeriodSec) {
                        loggerPeriodSec = sec;
                    } else if (sec < minPeriodSec) {
                        loggerPeriodSec = minPeriodSec;
                    } else if (sec > maxPeriodSec) {
                        loggerPeriodSec = maxPeriodSec;
                    }

                } else {
                    loggerPeriodSec = minPeriodSec;
                }

                prefEd.putInt(PREF_PERIOD_SEC, loggerPeriodSec);
                prefEd.commit();

                periodEditor.setText(loggerPeriodSec + "");
            }
        });

        setState(R.string.ext_media_unmounted_msg, !isMediaMounted);
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
            setState(R.string.fs_error_msg, true);
        }
    }

    private void setState(int resId, boolean isError) {

        if (isError) {
            serviceOnOffButton.setText(getString(R.string.btn_service_start));

            serviceOnOffButton.setEnabled(false);
            markButton.setEnabled(false);
            setPeriodButton.setEnabled(false);
            periodEditor.setEnabled(false);

            serviceProgressBar.setVisibility(View.INVISIBLE);
            markTextEditor.setVisibility(View.GONE);

            errorMessage.setText(resId);
            errorMessage.setVisibility(View.VISIBLE);

        } else {
            serviceOnOffButton.setEnabled(true);
            markButton.setEnabled(true);
            setPeriodButton.setEnabled(true);
            periodEditor.setEnabled(true);

            markTextEditor.setVisibility(View.VISIBLE);
            errorMessage.setVisibility(View.GONE);
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
