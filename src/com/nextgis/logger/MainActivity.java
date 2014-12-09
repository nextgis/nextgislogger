package com.nextgis.logger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.nextgis.logger.R;

public class MainActivity extends Activity implements OnClickListener {
	public static String dataDirPath = C.dataBasePath;
	public static String csvLogFilePath = dataDirPath + File.separator + C.csvLogFile;
	public static String csvLogFilePathSensor = dataDirPath + File.separator + C.csvLogFileSensor;
	public static String csvMarkFilePath = dataDirPath + File.separator + C.csvMarkFile;
	public static String csvMarkFilePathSensor = dataDirPath + File.separator + C.csvMarkFileSensor;

	private static long timeStarted = 0;
	private static int recordsCount = 0;

	private static enum INTERFACE_STATE {
		SESSION_NONE, SESSION_STARTED, ERROR, OK
	};

	private BroadcastReceiver broadcastReceiver;

	private Button serviceOnOffButton;
	private ProgressBar serviceProgressBar;

	private Button markButton;
	private Button sessionButton;

	private TextView loggerStartedTime;
	private TextView loggerFinishedTime;
	private TextView recordsCollectedCount;
	private TextView sessionName;
	private TextView marksCollectedCount;

	private TextView errorMessage;

	private ServiceConnection servConn = null;

	NetworkTypeChangeListener networkTypeListener;
	
	private SharedPreferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		errorMessage = (TextView) findViewById(R.id.tv_error_message);

		boolean isServiceRunning = isLoggerServiceRunning();

		serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
		serviceOnOffButton.setText(getString(isServiceRunning ? R.string.btn_service_stop : R.string.btn_service_start));
		serviceOnOffButton.setOnClickListener(this);

		serviceProgressBar = (ProgressBar) findViewById(R.id.service_progress_bar);
		serviceProgressBar.setVisibility(isServiceRunning ? View.VISIBLE : View.INVISIBLE);

		markButton = (Button) findViewById(R.id.btn_mark);
		markButton.setText(getString(R.string.btn_save_mark));
		markButton.setOnClickListener(this);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String session = prefs.getString(C.PREF_SESSION_NAME, "");

		setDataDirPath(session);
		setInterfaceState(0, session.equals("") ? INTERFACE_STATE.SESSION_NONE : INTERFACE_STATE.SESSION_STARTED);

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

			loggerFinishedTime.setText(getText(R.string.service_stopped));

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
					recordsCount += intent.getIntExtra(C.PARAM_RECORDS_COUNT, 0);
					loggerFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					prefs.edit().putInt(C.PREF_RECORDS_COUNT, recordsCount).commit();
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
		
		if (marksCount > 0)
			marksCollectedCount.setText(marksCount + "");

		//		int networkType = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType();

		//		if (!GSMEngine.isGSMNetwork(networkType)) {
		//			errorMessage.setText(R.string.network_error);
		//			errorMessage.setVisibility(View.VISIBLE);
		//		} else
		//			errorMessage.setVisibility(View.GONE);

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
		menu.findItem(R.id.action_settings).setEnabled(!isLoggerServiceRunning());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
			startActivity(preferencesActivity);
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
				defaultName += pref.getString(C.PREF_USER_NAME, "User1").equals("") ? "" : "--" + pref.getString(C.PREF_USER_NAME, "User1");
				input.setText(defaultName); // default session name
				input.setSelection(input.getText().length()); // move cursor at the end
				alert.setView(input);

				alert.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();

						if (isCorrectName(value)) { // open session
							pref.edit().putString(C.PREF_SESSION_NAME, value).commit();
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
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
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
				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); // show keyboard
				dialog.show();
			} else { // close session
				prefs.edit().putString(C.PREF_SESSION_NAME, "").putInt(C.PREF_MARKS_COUNT, 0).putInt(C.PREF_RECORDS_COUNT, 0).commit();
				recordsCount = 0;
				setInterfaceState(0, INTERFACE_STATE.SESSION_NONE);
				setDataDirPath("");
				sessionButton.setText(R.string.btn_session_open);
				sessionName.setText("");
				
				marksCollectedCount.setText("");
				recordsCollectedCount.setText("");
			}
			break;
		case R.id.btn_service_onoff:
			// Service can be stopped, but still visible in the system as working,
			// therefore, we need to use isLoggerServiceRunning()
			if (isLoggerServiceRunning()) {
				stopService(new Intent(getApplicationContext(), LoggerService.class));
				serviceOnOffButton.setText(getString(R.string.btn_service_start));
				serviceProgressBar.setVisibility(View.INVISIBLE);
				sessionButton.setEnabled(true);
			} else {
				Intent intent = new Intent(getApplicationContext(), LoggerService.class);
				startService(intent);

				serviceOnOffButton.setText(getString(R.string.btn_service_stop));
				serviceProgressBar.setVisibility(View.VISIBLE);
				sessionButton.setEnabled(false);
			}
			break;
		case R.id.btn_mark:
			Intent markActivity = new Intent(getBaseContext(), MarkActivity.class);
			startActivity(markActivity);
			break;
		}
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
		
		return result.toString();
	}

	private void setDataDirPath(String directory) {
		directory = directory.equals("") ? C.dataBasePath : C.dataBasePath + File.separator + directory;
		dataDirPath = directory;
		csvLogFilePath = directory + File.separator + C.csvLogFile;
		csvLogFilePathSensor = directory + File.separator + C.csvLogFileSensor;
		csvMarkFilePath = directory + File.separator + C.csvMarkFile;
		csvMarkFilePathSensor = directory + File.separator + C.csvMarkFileSensor;

		if (!checkOrCreateDirectory(dataDirPath))
			setInterfaceState(R.string.ext_media_unmounted_msg, INTERFACE_STATE.ERROR);
	}

	private boolean isCorrectName(String value) {
		final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

		if (value == null || value.length() == 0) {
			Toast.makeText(this, R.string.session_null_name, Toast.LENGTH_SHORT).show();
			return false;
		}

		for (int i = 0; i < ILLEGAL_CHARACTERS.length; i++)
			if (value.contains(String.valueOf(ILLEGAL_CHARACTERS[i]))) {
				Toast.makeText(this, getString(R.string.session_incorrect_name) + ILLEGAL_CHARACTERS[i], Toast.LENGTH_SHORT).show();
				return false;
			}

		return true;
	}

	private void setInterfaceState(int resId, INTERFACE_STATE state) {
		switch (state) {
		case ERROR:
			serviceOnOffButton.setText(getString(R.string.btn_service_start));

			errorMessage.setText(resId);
			errorMessage.setVisibility(View.VISIBLE);
		case SESSION_NONE:
			serviceOnOffButton.setEnabled(false);
			markButton.setEnabled(false);
			serviceProgressBar.setVisibility(View.INVISIBLE);
			break;
		case SESSION_STARTED:
		default:
			serviceOnOffButton.setEnabled(true);
			markButton.setEnabled(true);
			errorMessage.setVisibility(View.GONE);
			break;
		}
	}

	public boolean isLoggerServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

			if (LoggerService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
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

	/**
	 * Check directory existence or create it (with parents if missing).
	 * 
	 * @param path
	 *            Path to directory
	 * @return boolean signing success or fail
	 */
	public static boolean checkOrCreateDirectory(String path) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return false;
		} else {
			File dataDir = new File(path);

			if (!dataDir.exists())
				return dataDir.mkdirs();
		}

		return true;
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
