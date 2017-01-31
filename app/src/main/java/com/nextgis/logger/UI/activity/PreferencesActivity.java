/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2017 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 */
package com.nextgis.logger.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.keenfin.sfcdialog.SimpleFileChooser;
import com.nextgis.logger.LoggerService;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.AudioEngine;

import java.io.File;
import java.util.List;

public class PreferencesActivity extends PreferenceActivity implements ServiceConnection {
    private ArduinoEngine mArduinoEngine;
    private AudioEngine mAudioEngine;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equalsIgnoreCase(Intent.ACTION_GET_CONTENT)) {
                SimpleFileChooser sfcDialog = new SimpleFileChooser();

                sfcDialog.setOnChosenListener(new SimpleFileChooser.SimpleFileChooserListener() {
                    String info = getString(R.string.error_no_file);

                    @Override
                    public void onFileChosen(File file) {
                        Intent result = new Intent("com.example.RESULT_ACTION", Uri.parse("file://" + file.getPath()));
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onDirectoryChosen(File directory) {
                        finishWithError();
                    }

                    @Override
                    public void onCancel() {
                        finishWithError();
                    }

                    private void finishWithError() {
                        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                        Intent result = new Intent("com.example.RESULT_ACTION");
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }
                });

                if (getActionBar() != null)
                    getActionBar().hide();

                getWindow().setBackgroundDrawable(null);

                sfcDialog.show(getFragmentManager(), "SimpleFileChooserDialog");
                return;
            }
        }

        ProgressBarActivity.startLoggerService(this, null);
        Intent connection = new Intent(this, LoggerService.class);
        bindService(connection, this, 0);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            onServiceDisconnected(null);
            unbindService(this);
        } catch (IllegalArgumentException ignored) {}
    }

    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mArduinoEngine = ((LoggerService.LocalBinder) iBinder).getArduinoEngine();
        mAudioEngine = ((LoggerService.LocalBinder) iBinder).getSensorEngine().getAudioEngine();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mArduinoEngine = null;
        mAudioEngine = null;
    }

    public ArduinoEngine getArduinoEngine() {
        return mArduinoEngine;
    }

    public AudioEngine getAudioEngine() {
        return mAudioEngine;
    }
}
