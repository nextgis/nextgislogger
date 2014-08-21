package com.nextgis.gsm_logger;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVWriteProc;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.*;

public class MainActivity extends Activity {

    private Intent mIntentService;

    private static final CSV mCSVMarkers = CSV
            // TODO: It is draft
            .separator(';')
            .quote('\'')
            .skipLines(1)
            .charset("UTF-8")
            .create();

    private String mCSVFileName = "time_marks.csv";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.ext_media_unmounted, Toast.LENGTH_LONG).show();
            finish();
        }

        mCSVFileName = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
                File.separator + mCSVFileName;

        try {
            File csvFile = new File(mCSVFileName);
            if (!csvFile.exists()) {
                mCSVMarkers.writeAndClose(new FileOutputStream(csvFile, true), new CSVWriteProc() {
                    public void process(CSVWriter out) {
                        // TODO: It is draft
                        out.writeNext("Header1", "Header2");
                    }
                });
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.file_system_error, Toast.LENGTH_LONG).show();
            finish();
        }

        mIntentService = new Intent(this, LoggerService.class);
        boolean isServiceRunning = isLoggerServiceRunning();

        setContentView(R.layout.main_activity);

        final Button serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
        serviceOnOffButton.setText(getString(isServiceRunning
                ? R.string.btn_service_stop : R.string.btn_service_start));

        final ProgressBar serviceProgressBar =
                (ProgressBar) findViewById(R.id.service_progress_bar);
        serviceProgressBar.setVisibility(isServiceRunning ? View.VISIBLE : View.INVISIBLE);

        serviceOnOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Service can be stopped, but still visible in the system as working,
                // therefore, we need to use isLoggerServiceRunning()
                if (isLoggerServiceRunning()) {
                    stopService(mIntentService);
                    serviceOnOffButton.setText(getString(R.string.btn_service_start));
                    serviceProgressBar.setVisibility(View.INVISIBLE);
                } else {
                    startService(mIntentService);
                    serviceOnOffButton.setText(getString(R.string.btn_service_stop));
                    serviceProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        final Button markButton = (Button) findViewById(R.id.btn_mark);
        markButton.setText(getString(R.string.btn_make_mark));

        final EditText markTextEditor = (EditText) findViewById(R.id.mark_text_editor);
        final Toast errorMsg = Toast.makeText(this, R.string.file_system_error, Toast.LENGTH_LONG);

        markButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (markTextEditor.isShown()) {

                    try {
                        mCSVMarkers.writeAndClose(new FileOutputStream(mCSVFileName, true),
                                new CSVWriteProc() {
                                    public void process(CSVWriter out) {
                                        // TODO: It is draft
                                        out.writeNext(markTextEditor.getText().toString(), "v22");
                                    }
                                });
                    } catch (FileNotFoundException e) {
                        errorMsg.show();
                        finish();
                    }

                    markTextEditor.setVisibility(View.GONE);
                    markButton.setText(getString(R.string.btn_make_mark));

                } else {
                    markTextEditor.setText("");
                    markTextEditor.setVisibility(View.VISIBLE);
                    markButton.setText(getString(R.string.btn_save_mark));
                }
            }
        });
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
