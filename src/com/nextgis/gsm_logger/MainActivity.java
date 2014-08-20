package com.nextgis.gsm_logger;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    private Intent mIntentService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        mIntentService = new Intent(this, LoggerService.class);
        boolean isServiceRunning = isLoggerServiceRunning();

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

        markButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (markTextEditor.isShown()) {
                    // TODO: save text
                    markTextEditor.setVisibility(View.GONE);
                    markButton.setText(getString(R.string.btn_make_mark));
                } else {
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
