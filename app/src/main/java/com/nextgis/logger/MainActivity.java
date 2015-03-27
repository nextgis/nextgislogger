/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Nikita Kirin
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2015 NextGIS
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends ProgressBarActivity implements OnClickListener {
	public static String dataDirPath = C.dataBasePath;
	public static String csvLogFilePath = dataDirPath + File.separator + C.csvLogFile;
	public static String csvLogFilePathSensor = dataDirPath + File.separator + C.csvLogFileSensor;
	public static String csvMarkFilePath = dataDirPath + File.separator + C.csvMarkFile;
	public static String csvMarkFilePathSensor = dataDirPath + File.separator + C.csvMarkFileSensor;

	private static long timeStarted = 0;
	private static int recordsCount = 0;

	private static enum INTERFACE_STATE {
		SESSION_NONE, SESSION_STARTED, ERROR, OK
	}

	private BroadcastReceiver broadcastReceiver;

	private Button serviceOnOffButton;

	private Button markButton;
	private Button sessionButton;

	private TextView loggerStartedTime;
	private TextView loggerFinishedTime;
	private TextView recordsCollectedCount;
	private TextView sessionName;
	private TextView marksCollectedCount;

	private ServiceConnection servConn = null;

	NetworkTypeChangeListener networkTypeListener;

	private SharedPreferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(false);

        setContentView(R.layout.main_activity);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        FileUtil.deleteFiles(new File(C.tempPath).listFiles()); // clear cache directory with shared zips
        ((TextView)findViewById(R.id.tv_sessions)).setText(getString(R.string.title_activity_sessions).toUpperCase());

        ((TextView)findViewById(R.id.tv_sessions)).setTextColor(mThemeColor);
        ((TextView)findViewById(R.id.tv_modes)).setTextColor(mThemeColor);

		boolean isServiceRunning = isLoggerServiceRunning(this);

		serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
		serviceOnOffButton.setText(getString(isServiceRunning ? R.string.btn_service_stop : R.string.btn_service_start));
		serviceOnOffButton.setOnClickListener(this);

		markButton = (Button) findViewById(R.id.btn_mark);
		markButton.setText(getString(R.string.btn_save_mark));
		markButton.setOnClickListener(this);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String session = prefs.getString(C.PREF_SESSION_NAME, "");

		setDataDirPath(session);
		setInterfaceState(0, session.equals("") ? INTERFACE_STATE.SESSION_NONE : INTERFACE_STATE.SESSION_STARTED);

        findViewById(R.id.btn_sessions).setOnClickListener(this);

		sessionName = (TextView) findViewById(R.id.tv_current_session_name);
		sessionName.setText(session);

		sessionButton = (Button) findViewById(R.id.btn_session);
		sessionButton.setText(getString(session.equals("") ? R.string.btn_session_open : R.string.btn_session_close));
		sessionButton.setEnabled(!isServiceRunning);
		sessionButton.setOnClickListener(this);

		networkTypeListener = new NetworkTypeChangeListener((TextView) findViewById(R.id.tv_network_type_str));

		loggerStartedTime = (TextView) findViewById(R.id.tv_logger_started_time);
		loggerFinishedTime = (TextView) findViewById(R.id.tv_logger_finished_time);
		recordsCollectedCount = (TextView) findViewById(R.id.tv_records_collected_count);
		marksCollectedCount = (TextView) findViewById(R.id.tv_marks_collected_count);

		recordsCount = prefs.getInt(C.PREF_RECORDS_COUNT, 0);

		if (!isServiceRunning) {
			if (timeStarted > 0) {
				loggerStartedTime.setText(millisToDate(timeStarted, "dd.MM.yyyy hh:mm:ss"));
			}

//			loggerFinishedTime.setText(getText(R.string.service_stopped));

			if (recordsCount > 0) {
				recordsCollectedCount.setText(recordsCount + "");
			}

		} else {
			servConn = new ServiceConnection() {
				public void onServiceConnected(ComponentName name, IBinder binder) {
					LoggerService loggerService = ((LoggerService.LocalBinder) binder).getService();
					loggerStartedTime.setText(millisToDate(loggerService.getTimeStart(), "dd.MM.yyyy hh:mm:ss"));
					recordsCollectedCount.setText(recordsCount + loggerService.getRecordsCount() + "");
				}

				public void onServiceDisconnected(ComponentName name) {
				}
			};
			Intent intentConn = new Intent(this, LoggerService.class);
			bindService(intentConn, servConn, 0);

			loggerFinishedTime.setText(getText(R.string.service_running));
		}

		broadcastReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				int serviceStatus = intent.getIntExtra(C.PARAM_SERVICE_STATUS, 0);
				long time = intent.getLongExtra(C.PARAM_TIME, 0);

				switch (serviceStatus) {
				case C.STATUS_STARTED:
					timeStarted = time;
					loggerStartedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					loggerFinishedTime.setText(getText(R.string.service_running));
					break;

				case C.STATUS_RUNNING:
					recordsCollectedCount.setText(recordsCount + intent.getIntExtra(C.PARAM_RECORDS_COUNT, 0) + "");
					loggerFinishedTime.setText(getText(R.string.service_running));
					break;

				case C.STATUS_ERROR:
					setInterfaceState(R.string.fs_error_msg, INTERFACE_STATE.ERROR);
				case C.STATUS_FINISHED:
                    updateFileForMTP(csvLogFilePath);
                    updateFileForMTP(csvLogFilePathSensor);
					recordsCount += intent.getIntExtra(C.PARAM_RECORDS_COUNT, 0);
					loggerFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					prefs.edit().putInt(C.PREF_RECORDS_COUNT, recordsCount).apply();
					break;
				}
			}
		};

		IntentFilter intentFilter = new IntentFilter(C.BROADCAST_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		int marksCount = prefs.getInt(C.PREF_MARKS_COUNT, 0);

		if (marksCount > 0) {
            marksCollectedCount.setText(marksCount + "");
            updateFileForMTP(csvMarkFilePath);
            updateFileForMTP(csvMarkFilePathSensor);
        }

		//		int networkType = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType();
		((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(networkTypeListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
	}

	@Override
	protected void onPause() {
		super.onPause();

		((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(networkTypeListener, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	protected void onDestroy() {
		if (servConn != null) {
			unbindService(servConn);
		}

		unregisterReceiver(broadcastReceiver);

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_settings).setEnabled(!isLoggerServiceRunning(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
			startActivity(preferencesActivity);
			break;
		case R.id.action_about:
			Intent aboutActivity = new Intent(this, AboutActivity.class);
			startActivity(aboutActivity);
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_session:
			final SharedPreferences pref = prefs;

			if (pref.getString(C.PREF_SESSION_NAME, "").equals("")) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(getString(R.string.session_name));

				final EditText input = new EditText(this);
				String defaultName = millisToDate(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd--HH-mm-ss");
                String userName = pref.getString(C.PREF_USER_NAME, C.DEFAULT_USERNAME);
				defaultName += userName.equals("") ? "" : "--" + userName;
				input.setText(defaultName); // default session name
				input.setSelection(input.getText().length()); // move cursor at the end
				alert.setView(input);

				alert.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();

						if (isCorrectName(value)) { // open session
							pref.edit().putString(C.PREF_SESSION_NAME, value).apply();
							setInterfaceState(0, INTERFACE_STATE.SESSION_STARTED);
							setDataDirPath(value);
							sessionButton.setText(R.string.btn_session_close);
							sessionName.setText(value);

							File deviceInfoFile = new File(dataDirPath + File.separator + C.deviceInfoFile);

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

				alert.setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

				AlertDialog dialog = alert.create();
//				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); // show keyboard
				dialog.show();
			} else { // close session
				prefs.edit().putString(C.PREF_SESSION_NAME, "").putInt(C.PREF_MARKS_COUNT, 0).putInt(C.PREF_RECORDS_COUNT, 0).apply();
				recordsCount = 0;
				setInterfaceState(0, INTERFACE_STATE.SESSION_NONE);
				setDataDirPath("");
				sessionButton.setText(R.string.btn_session_open);
				sessionName.setText("");

                loggerStartedTime.setText("");
                loggerFinishedTime.setText("");
				marksCollectedCount.setText("");
				recordsCollectedCount.setText("");
			}
			break;
		case R.id.btn_service_onoff:
			// Service can be stopped, but still visible in the system as working,
			// therefore, we need to use isLoggerServiceRunning()
			if (isLoggerServiceRunning(this)) {
				stopService(new Intent(getApplicationContext(), LoggerService.class));
				serviceOnOffButton.setText(getString(R.string.btn_service_start));
                setActionBarProgress(false);
				sessionButton.setEnabled(true);
			} else {
				Intent intent = new Intent(getApplicationContext(), LoggerService.class);
				startService(intent);

				serviceOnOffButton.setText(getString(R.string.btn_service_stop));
                setActionBarProgress(true);
				sessionButton.setEnabled(false);
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
		directory = directory.equals("") ? C.dataBasePath : C.dataBasePath + File.separator + directory;
		dataDirPath = directory;
		csvLogFilePath = directory + File.separator + C.csvLogFile;
		csvLogFilePathSensor = directory + File.separator + C.csvLogFileSensor;
		csvMarkFilePath = directory + File.separator + C.csvMarkFile;
		csvMarkFilePathSensor = directory + File.separator + C.csvMarkFileSensor;

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
			serviceOnOffButton.setText(getString(R.string.btn_service_start));
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
		case SESSION_NONE:
			serviceOnOffButton.setEnabled(false);
			markButton.setEnabled(false);
            setActionBarProgress(false);
            findViewById(R.id.rl_modes).setVisibility(View.GONE);
			break;
		case SESSION_STARTED:
		default:
			serviceOnOffButton.setEnabled(true);
			markButton.setEnabled(true);
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

	private class NetworkTypeChangeListener extends PhoneStateListener {

		TextView tv;

		public NetworkTypeChangeListener(TextView tv) {
			this.tv = tv;
		}

		@Override
		public void onDataConnectionStateChanged(int state, int networkType) {
			super.onDataConnectionStateChanged(state, networkType);

			tv.setText(CellEngine.getNetworkGen(networkType) + " / " + CellEngine.getNetworkType(networkType));
		}
	}
}
