/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Nikita Kirin
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2016 NextGIS
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

package com.nextgis.logger;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;
import com.nextgis.logger.util.FileUtil;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends ProgressBarActivity implements OnClickListener {
    private BroadcastReceiver mLoggerServiceReceiver;
    private Button mButtonService, mButtonSession;
    private TextView mTvSessionName, mTvStartedTime, mTvFinishedTime, mTvRecordsCount, mTvMarksCount;
    private Uri mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(false);

        setContentView(R.layout.main_activity);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        FileUtil.deleteFiles(new File(LoggerConstants.TEMP_PATH).listFiles()); // clear cache directory with shared zips

        ((TextView) findViewById(R.id.tv_sessions)).setText(getString(R.string.title_activity_sessions).toUpperCase());
        findViewById(R.id.btn_sessions).setOnClickListener(this);

        mTvSessionName = (TextView) findViewById(R.id.tv_current_session_name);
        mTvStartedTime = (TextView) findViewById(R.id.tv_logger_started_time);
        mTvFinishedTime = (TextView) findViewById(R.id.tv_logger_finished_time);
        mTvRecordsCount = (TextView) findViewById(R.id.tv_records_collected_count);
        mTvMarksCount = (TextView) findViewById(R.id.tv_marks_collected_count);

        mButtonService = (Button) findViewById(R.id.btn_service_onoff);
        mButtonService.setOnClickListener(this);
        Button buttonMark = (Button) findViewById(R.id.btn_mark);
        buttonMark.setOnClickListener(this);
        buttonMark.setText(getString(R.string.btn_save_mark));
        mButtonSession = (Button) findViewById(R.id.btn_session);
        mButtonSession.setOnClickListener(this);

        mLoggerServiceReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int serviceStatus = intent.getIntExtra(LoggerConstants.SERVICE_STATUS, 0);
                int count = intent.getIntExtra(LoggerConstants.PREF_RECORDS_COUNT, 0);
                long time;

                switch (serviceStatus) {
                    case LoggerConstants.STATUS_STARTED:
                        time = intent.getLongExtra(LoggerConstants.PREF_TIME_START, 0);
                        mTvStartedTime.setText(millisToDate(time));
                        mTvFinishedTime.setText(R.string.service_running);
                        mPreferences.edit().putLong(LoggerConstants.PREF_TIME_START, time).apply();
                        break;
                    case LoggerConstants.STATUS_RUNNING:
                        mTvRecordsCount.setText(count + "");
                        mTvFinishedTime.setText(R.string.service_running);
                        break;
                    case LoggerConstants.STATUS_FINISHED:
                        time = intent.getLongExtra(LoggerConstants.PREF_TIME_FINISH, 0);
                        mTvFinishedTime.setText(millisToDate(time));

                        if (mPreferences.getString(LoggerConstants.PREF_SESSION_ID, null) != null)
                            mPreferences.edit().putLong(LoggerConstants.PREF_TIME_FINISH, time).putInt(LoggerConstants.PREF_RECORDS_COUNT, count).apply();
                        break;
                    case LoggerConstants.STATUS_ERROR:
                        Toast.makeText(context, R.string.session_bad, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(LoggerConstants.ACTION_INFO);
        registerReceiver(mLoggerServiceReceiver, intentFilter);

        mUri = Uri.parse("content://" + ((IGISApplication) getApplicationContext()).getAuthority());
        mUri = mUri.buildUpon().appendPath(LoggerApplication.TABLE_SESSION).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateInterface();

        int marksCount = mPreferences.getInt(LoggerConstants.PREF_MARKS_COUNT, 0);
        if (marksCount > 0)
            mTvMarksCount.setText(marksCount + "");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mLoggerServiceReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_session:
                if (isSessionClosed())
                    showSessionNameDialog();
                else // close session
                    closeSession();
                break;
            case R.id.btn_service_onoff:
                // Service can be stopped, but still visible in the system as working,
                // therefore, we need to use isLoggerServiceRunning()
                if (isLoggerServiceRunning())
                    stopLoggerService();
                else
                    startLoggerService(LoggerConstants.ACTION_START);
                break;
            case R.id.btn_mark:
                Intent markActivity = new Intent(this, MarkActivity.class);
                startActivity(markActivity);
                break;
            case R.id.btn_sessions:
                Intent sessionsActivity = new Intent(this, SessionsActivity.class);
                startActivity(sessionsActivity);
                break;
            default:
                super.onClick(view);
                break;
        }
    }

    protected void startLoggerService(String action) {
        ProgressBarActivity.startLoggerService(this, action);
        mButtonService.setText(getString(R.string.btn_service_stop));
        setActionBarProgress(true);
        mButtonSession.setEnabled(false);
    }

    @Override
    protected void stopLoggerService() {
        super.stopLoggerService();
        mButtonService.setText(getString(R.string.btn_service_start));
        mButtonSession.setEnabled(true);
    }

    private void showSessionNameDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.session_name));

        final EditText input = new EditText(this);
        String defaultName = millisToDate(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd--HH-mm-ss");
        final String userName = mPreferences.getString(LoggerConstants.PREF_USER_NAME, LoggerConstants.DEFAULT_USERNAME);
        defaultName += userName.equals("") ? "" : "--" + userName;
        input.setText(defaultName); // default session name
        input.setSelection(input.getText().length()); // move cursor to the end

        alert.setView(input);
        alert.setNegativeButton(getString(R.string.btn_cancel), null);
        alert.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (isCorrectName(value)) { // open session
                    String id = startSession(value, userName, getDeviceInfo());
                    if (id == null) {
                        Toast.makeText(MainActivity.this, R.string.sessions_start_fail, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mSessionId = id;
                    mPreferences.edit().putString(LoggerConstants.PREF_SESSION_ID, id).apply();
                    updateInterface();
                }
            }
        });

        AlertDialog dialog = alert.create();
        dialog.show();
    }

    private String startSession(String name, String userName, String deviceInfo) {
        NGWVectorLayer sessionLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SESSION);
        if (sessionLayer != null) {
            String id = UUID.randomUUID().toString();
            ContentValues cv = new ContentValues();
            cv.put(LoggerApplication.FIELD_NAME, name);
            cv.put(LoggerApplication.FIELD_USER, userName);
            cv.put(LoggerApplication.FIELD_DEVICE_INFO, deviceInfo);
            cv.put(LoggerApplication.FIELD_UNIQUE_ID, id);
            cv.put(Constants.FIELD_GEOM, LoggerApplication.getNullGeometry());

            sessionLayer.insert(mUri, cv);
            return id;
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private String getDeviceInfo() {
        StringBuilder result = new StringBuilder();

        result.append("Manufacturer:\t").append(Build.MANUFACTURER).append("\r\n");
        result.append("Brand:\t").append(Build.BRAND).append("\r\n");
        result.append("Model:\t").append(Build.MODEL).append("\r\n");
        result.append("Product:\t").append(Build.PRODUCT).append("\r\n");
        result.append("Android:\t").append(Build.VERSION.RELEASE).append("\r\n");
        result.append("API:\t").append(Build.VERSION.SDK_INT).append("\r\n");

        result.append("Kernel version:\t").append(System.getProperty("os.version")).append("\r\n");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            result.append("Radio firmware:\t").append(Build.getRadioVersion()).append("\r\n");
        else
            result.append("Radio firmware:\t").append(Build.RADIO).append("\r\n");

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            result.append("Logger version name:\t").append(packageInfo.versionName).append("\r\n");
            result.append("Logger version code:\t").append(packageInfo.versionCode).append("\r\n");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    private boolean isCorrectName(String value) {
        final char[] ILLEGAL_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

        if (value == null || value.length() == 0) {
            Toast.makeText(this, R.string.session_null_name, Toast.LENGTH_SHORT).show();
            return false;
        }

        for (char ILLEGAL_CHARACTER : ILLEGAL_CHARACTERS)
            if (value.contains(String.valueOf(ILLEGAL_CHARACTER))) {
                Toast.makeText(this, getString(R.string.session_incorrect_name) + ILLEGAL_CHARACTER, Toast.LENGTH_SHORT).show();
                return false;
            }

        return true;
    }

    private void updateInterface() {
        mSessionId = mPreferences.getString(LoggerConstants.PREF_SESSION_ID, null);
        if (TextUtils.isEmpty(mSessionId)) {
            findViewById(R.id.rl_modes).setVisibility(View.GONE);
            mButtonSession.setText(R.string.btn_session_open);

            mTvSessionName.setText("");
            mTvStartedTime.setText("");
            mTvFinishedTime.setText("");
            mTvMarksCount.setText("");
            mTvRecordsCount.setText("");
        } else {
            findViewById(R.id.rl_modes).setVisibility(View.VISIBLE);
            mButtonSession.setText(R.string.btn_session_close);

            String sessionName;
            NGWVectorLayer sessionLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SESSION);
            if (sessionLayer != null) {
                String session = getSessionName();
                if (!TextUtils.isEmpty(session))
                    sessionName = session;
                else {
                    closeSession();
                    return;
                }
            } else {
                closeSession();
                return;
            }

            mTvSessionName.setText(sessionName);
            Long time = mPreferences.getLong(LoggerConstants.PREF_TIME_START, 0);
            mTvStartedTime.setText(time > 0 ? millisToDate(time) : getString(R.string.service_stopped));
            time = mPreferences.getLong(LoggerConstants.PREF_TIME_FINISH, 0);
            mTvFinishedTime.setText(time > 0 ? millisToDate(time) : getString(R.string.service_stopped));
            mTvMarksCount.setText(mPreferences.getInt(LoggerConstants.PREF_MARKS_COUNT, 0) + "");
            mTvRecordsCount.setText(mPreferences.getInt(LoggerConstants.PREF_RECORDS_COUNT, 0) + "");
        }

        boolean isServiceRunning = isLoggerServiceRunning();
        setActionBarProgress(isServiceRunning);

        mButtonService.setText(getString(isServiceRunning ? R.string.btn_service_stop : R.string.btn_service_start));
        mButtonSession.setEnabled(!isServiceRunning);

        if (isServiceRunning)
            mTvFinishedTime.setText(R.string.service_running);
    }

    private String getSessionName() {
        String result = null;
        SQLiteDatabase db = ((MapContentProviderHelper) MapBase.getInstance()).getDatabase(true);
        Cursor count = db.rawQuery("SELECT " + LoggerApplication.FIELD_NAME + " FROM " + LoggerApplication.TABLE_SESSION + " WHERE " +
                                           LoggerApplication.FIELD_UNIQUE_ID + " = ?;", new String[]{mSessionId});

        if (count != null) {
            if (count.moveToFirst())
                result = count.getString(0);

            count.close();
        }

        return result;
    }

    private void closeSession() {
        clearSession();
        updateInterface();
    }

    public static String millisToDate(long milliSeconds) {
        return millisToDate(milliSeconds, "dd.MM.yyyy HH:mm:ss");
    }

    /**
     * Return date in specified format.
     *
     * @param milliSeconds Date in milliseconds
     * @param dateFormat   Date format
     * @return String representing date in specified format
     */
    public static String millisToDate(long milliSeconds, String dateFormat) {
        // dateFormat example: "dd/MM/yyyy hh:mm:ss.SSS"
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.getDefault());

        // Create a calendar object that will convert
        // the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
