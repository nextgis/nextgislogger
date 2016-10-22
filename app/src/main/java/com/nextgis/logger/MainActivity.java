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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.FileUtil;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends ProgressBarActivity implements OnClickListener {
	public static String dataDirPath = LoggerConstants.DATA_PATH;
	public static String csvLogFilePath = dataDirPath + File.separator + LoggerConstants.CSV_LOG_CELL;
	public static String csvLogFilePathSensor = dataDirPath + File.separator + LoggerConstants.CSV_LOG_SENSOR;
	public static String csvLogFilePathExternal = dataDirPath + File.separator + LoggerConstants.CSV_LOG_EXTERNAL;
	public static String csvMarkFilePath = dataDirPath + File.separator + LoggerConstants.CSV_MARK_CELL;
	public static String csvMarkFilePathSensor = dataDirPath + File.separator + LoggerConstants.CSV_MARK_SENSOR;
	public static String csvMarkFilePathExternal = dataDirPath + File.separator + LoggerConstants.CSV_MARK_EXTERNAL;

	private static long mTimeStarted = 0;
	private static int mRecordsCount = 0;

	private enum INTERFACE_STATE {
		SESSION_NONE, SESSION_STARTED, ERROR, OK
	}

	private BroadcastReceiver mLoggerServiceReceiver;
	private Button mButtonService, mButtonMark, mButtonSession;
	private TextView mTvSessionName, mTvStartedTime, mTvFinishedTime, mTvRecordsCount, mTvMarksCount;
	private ServiceConnection mServiceConnection = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(false);

        setContentView(R.layout.main_activity);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        FileUtil.deleteFiles(new File(LoggerConstants.TEMP_PATH).listFiles()); // clear cache directory with shared zips
        ((TextView)findViewById(R.id.tv_sessions)).setText(getString(R.string.title_activity_sessions).toUpperCase());

		boolean isServiceRunning = isLoggerServiceRunning(this);

		mButtonService = (Button) findViewById(R.id.btn_service_onoff);
		mButtonService.setText(getString(isServiceRunning ? R.string.btn_service_stop : R.string.btn_service_start));
		mButtonService.setOnClickListener(this);

		mButtonMark = (Button) findViewById(R.id.btn_mark);
		mButtonMark.setText(getString(R.string.btn_save_mark));
		mButtonMark.setOnClickListener(this);

		String sessionName = "";
		long id = mPreferences.getLong(LoggerConstants.PREF_SESSION_ID, Constants.NOT_FOUND);
		NGWVectorLayer sessionLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SESSION);
		if (sessionLayer != null && id != Constants.NOT_FOUND) {
			Feature session = sessionLayer.getFeature(id);
			sessionName = session.getFieldValueAsString(LoggerApplication.FIELD_NAME);
		}

		setDataDirPath(sessionName);
		setInterfaceState(0, id == Constants.NOT_FOUND ? INTERFACE_STATE.SESSION_NONE : INTERFACE_STATE.SESSION_STARTED);

        findViewById(R.id.btn_sessions).setOnClickListener(this);

		mTvSessionName = (TextView) findViewById(R.id.tv_current_session_name);
		mTvSessionName.setText(sessionName);

		mButtonSession = (Button) findViewById(R.id.btn_session);
		mButtonSession.setText(getString(sessionName.equals("") ? R.string.btn_session_open : R.string.btn_session_close));
		mButtonSession.setEnabled(!isServiceRunning);
		mButtonSession.setOnClickListener(this);

		mTvStartedTime = (TextView) findViewById(R.id.tv_logger_started_time);
		mTvFinishedTime = (TextView) findViewById(R.id.tv_logger_finished_time);
		mTvRecordsCount = (TextView) findViewById(R.id.tv_records_collected_count);
		mTvMarksCount = (TextView) findViewById(R.id.tv_marks_collected_count);

		mRecordsCount = mPreferences.getInt(LoggerConstants.PREF_RECORDS_COUNT, 0);

		if (!isServiceRunning) {
			if (mTimeStarted > 0)
				mTvStartedTime.setText(millisToDate(mTimeStarted, "dd.MM.yyyy hh:mm:ss"));

//			mTvFinishedTime.setText(getText(R.string.service_stopped));

			if (mRecordsCount > 0)
				mTvRecordsCount.setText(mRecordsCount + "");
		} else {
			mServiceConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName name, IBinder binder) {
					LoggerService loggerService = ((LoggerService.LocalBinder) binder).getService();
					mTvStartedTime.setText(millisToDate(loggerService.getTimeStart(), "dd.MM.yyyy hh:mm:ss"));
					mTvRecordsCount.setText(mRecordsCount + loggerService.getRecordsCount() + "");
				}

				public void onServiceDisconnected(ComponentName name) { }
			};
			Intent intentConn = new Intent(this, LoggerService.class);
			bindService(intentConn, mServiceConnection, 0);

			mTvFinishedTime.setText(getText(R.string.service_running));
		}

		mLoggerServiceReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				int serviceStatus = intent.getIntExtra(LoggerConstants.PARAM_SERVICE_STATUS, 0);
				long time = intent.getLongExtra(LoggerConstants.PARAM_TIME, 0);

				switch (serviceStatus) {
				case LoggerConstants.STATUS_STARTED:
					mTimeStarted = time;
					mTvStartedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					mTvFinishedTime.setText(getText(R.string.service_running));
					break;

				case LoggerConstants.STATUS_RUNNING:
					mTvRecordsCount.setText(mRecordsCount + intent.getIntExtra(LoggerConstants.PARAM_RECORDS_COUNT, 0) + "");
					mTvFinishedTime.setText(getText(R.string.service_running));
					break;

				case LoggerConstants.STATUS_ERROR:
					setInterfaceState(R.string.fs_error_msg, INTERFACE_STATE.ERROR);
				case LoggerConstants.STATUS_FINISHED:
                    updateFileForMTP(csvLogFilePath);
                    updateFileForMTP(csvLogFilePathSensor);
                    updateFileForMTP(csvLogFilePathExternal);
					mRecordsCount += intent.getIntExtra(LoggerConstants.PARAM_RECORDS_COUNT, 0);
					mTvFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					mPreferences.edit().putInt(LoggerConstants.PREF_RECORDS_COUNT, mRecordsCount).apply();
					break;
				}
			}
		};

		IntentFilter intentFilter = new IntentFilter(LoggerConstants.BROADCAST_ACTION);
		registerReceiver(mLoggerServiceReceiver, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		int marksCount = mPreferences.getInt(LoggerConstants.PREF_MARKS_COUNT, 0);

		if (marksCount > 0) {
            mTvMarksCount.setText(marksCount + "");
            updateFileForMTP(csvMarkFilePath);
            updateFileForMTP(csvMarkFilePathSensor);
            updateFileForMTP(csvMarkFilePathExternal);
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mServiceConnection != null) {
			unbindService(mServiceConnection);
		}

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
			if (isSessionClosed()) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(getString(R.string.session_name));

				final EditText input = new EditText(this);
				String defaultName = millisToDate(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd--HH-mm-ss");
                final String userName = mPreferences.getString(LoggerConstants.PREF_USER_NAME, LoggerConstants.DEFAULT_USERNAME);
				defaultName += userName.equals("") ? "" : "--" + userName;
				input.setText(defaultName); // default session name
				input.setSelection(input.getText().length()); // move cursor at the end
				alert.setView(input);

				alert.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();

						if (isCorrectName(value)) { // open session
							long id = startSession(value, userName);
							if (id == Constants.NOT_FOUND)
								return;

							mPreferences.edit().putLong(LoggerConstants.PREF_SESSION_ID, id).apply();
							setInterfaceState(0, INTERFACE_STATE.SESSION_STARTED);
							setDataDirPath(value);
							mButtonSession.setText(R.string.btn_session_close);
							mTvSessionName.setText(value);

							File deviceInfoFile = new File(dataDirPath + File.separator + LoggerConstants.DEVICE_INFO);

							PrintWriter pw;
							try {
								pw = new PrintWriter(new FileOutputStream(deviceInfoFile, true));
								pw.println(getDeviceInfo());
								pw.close();
                                updateFileForMTP(deviceInfoFile.getPath());
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
						}
					}
				});
				alert.setNegativeButton(getString(R.string.btn_cancel), null);

				AlertDialog dialog = alert.create();
//				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); // show keyboard
				dialog.show();
			} else { // close session
				mPreferences.edit().putLong(LoggerConstants.PREF_SESSION_ID, -1)
						.putInt(LoggerConstants.PREF_MARKS_COUNT, 0)
						.putInt(LoggerConstants.PREF_RECORDS_COUNT, 0)
                        .putInt(LoggerConstants.PREF_MARK_POS, Integer.MIN_VALUE).apply();
				mRecordsCount = 0;
				setInterfaceState(0, INTERFACE_STATE.SESSION_NONE);
				setDataDirPath("");
				mButtonSession.setText(R.string.btn_session_open);
				mTvSessionName.setText("");

                mTvStartedTime.setText("");
                mTvFinishedTime.setText("");
				mTvMarksCount.setText("");
				mTvRecordsCount.setText("");
			}
			break;
		case R.id.btn_service_onoff:
			// Service can be stopped, but still visible in the system as working,
			// therefore, we need to use isLoggerServiceRunning()
			if (isLoggerServiceRunning(this)) {
				stopService(new Intent(getApplicationContext(), LoggerService.class));
				mButtonService.setText(getString(R.string.btn_service_start));
                setActionBarProgress(false);
				mButtonSession.setEnabled(true);
			} else {
				Intent intent = new Intent(getApplicationContext(), LoggerService.class);
				startService(intent);

				mButtonService.setText(getString(R.string.btn_service_stop));
                setActionBarProgress(true);
				mButtonSession.setEnabled(false);
			}
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

	private long startSession(String name, String userName) {
		NGWVectorLayer sessionLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SESSION);
		if (sessionLayer != null) {
			Feature mark = new Feature(Constants.NOT_FOUND, sessionLayer.getFields());
			mark.setFieldValue(LoggerApplication.FIELD_NAME, name);
			mark.setFieldValue(LoggerApplication.FIELD_USER, userName);
			mark.setGeometry(new GeoPoint(0, 0));
			return sessionLayer.createFeature(mark);
		}

		return -1;
	}

    public void updateFileForMTP(String path) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(path));
        sendBroadcast(intent);	// update media for MTP
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
            result.append("Logger version name:\t").append(getPackageManager().getPackageInfo(getPackageName(), 0).versionName).append("\r\n");
            result.append("Logger version code:\t").append(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode).append("\r\n");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

		return result.toString();
	}

	private void setDataDirPath(String directory) {
		directory = directory.equals("") ? LoggerConstants.DATA_PATH : LoggerConstants.DATA_PATH + File.separator + directory;
		dataDirPath = directory;
		csvLogFilePath = directory + File.separator + LoggerConstants.CSV_LOG_CELL;
		csvLogFilePathSensor = directory + File.separator + LoggerConstants.CSV_LOG_SENSOR;
		csvLogFilePathExternal = directory + File.separator + LoggerConstants.CSV_LOG_EXTERNAL;
		csvMarkFilePath = directory + File.separator + LoggerConstants.CSV_MARK_CELL;
		csvMarkFilePathSensor = directory + File.separator + LoggerConstants.CSV_MARK_SENSOR;
		csvMarkFilePathExternal = directory + File.separator + LoggerConstants.CSV_MARK_EXTERNAL;

		if (!FileUtil.checkOrCreateDirectory(dataDirPath))
			setInterfaceState(R.string.ext_media_unmounted_msg, INTERFACE_STATE.ERROR);
	}

	private boolean isCorrectName(String value) {
		final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

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

	private void setInterfaceState(int resId, INTERFACE_STATE state) {
		switch (state) {
		case ERROR:
            setActionBarProgress(false);
            mButtonSession.setEnabled(true);
            mButtonService.setText(getString(R.string.btn_service_start));
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
            break;
		case SESSION_NONE:
			mButtonService.setEnabled(false);
			mButtonMark.setEnabled(false);
            setActionBarProgress(false);
            findViewById(R.id.rl_modes).setVisibility(View.GONE);
			break;
		case SESSION_STARTED:
		default:
			mButtonService.setEnabled(true);
			mButtonMark.setEnabled(true);
            findViewById(R.id.rl_modes).setVisibility(View.VISIBLE);
			break;
		}
	}

	/**
	 * Return date in specified format.
	 *
	 * @param milliSeconds
	 *            Date in milliseconds
	 * @param dateFormat
	 *            Date format
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
