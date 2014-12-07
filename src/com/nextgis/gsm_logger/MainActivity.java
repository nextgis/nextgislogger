package com.nextgis.gsm_logger;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
	private static long timeStarted = 0;
	private static int recordsCount = 0;

	private BroadcastReceiver broadcastReceiver;

	private Button serviceOnOffButton;
	private ProgressBar serviceProgressBar;

	private Button markButton;

	private TextView loggerStartedTime;
	private TextView loggerFinishedTime;
	private TextView recordsCollectedCount;
//	private TextView marksCollectedCount;

	private TextView errorMessage;

	private ServiceConnection servConn = null;
//	CustomArrayAdapter substringMarkNameAdapter;

	NetworkTypeChangeListener networkTypeListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);
		PreferenceManager.setDefaultValues(this, C.PREFERENCE_NAME, MODE_PRIVATE, R.xml.preferences, false);

		errorMessage = (TextView) findViewById(R.id.tv_error_message);
		boolean isMediaMounted = true;
		
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			isMediaMounted = false;

		} else {
			File dataDir = new File(C.dataDirPath);
			if (!dataDir.exists()) {
				dataDir.mkdirs();
			}
		}

		boolean isServiceRunning = isLoggerServiceRunning();

		serviceOnOffButton = (Button) findViewById(R.id.btn_service_onoff);
		serviceOnOffButton.setText(getString(isServiceRunning ? R.string.btn_service_stop : R.string.btn_service_start));

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
					Intent intent = new Intent(getApplicationContext(), LoggerService.class);
					startService(intent);

					serviceOnOffButton.setText(getString(R.string.btn_service_stop));
					serviceProgressBar.setVisibility(View.VISIBLE);
					;
				}
			}
		});

		markButton = (Button) findViewById(R.id.btn_mark);
		markButton.setText(getString(R.string.btn_save_mark));

		final SharedPreferences pref = getPreferences(MODE_PRIVATE);
		updateApplicationStructure(pref);
		
		markButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent markActivity = new Intent(getBaseContext(), MarkActivity.class);
				startActivity(markActivity);
			}
		});

		networkTypeListener = new NetworkTypeChangeListener((TextView) findViewById(R.id.tv_network_type_str));

		loggerStartedTime = (TextView) findViewById(R.id.tv_logger_started_time);
		loggerFinishedTime = (TextView) findViewById(R.id.tv_logger_finished_time);
		recordsCollectedCount = (TextView) findViewById(R.id.tv_records_collected_count);
//		marksCollectedCount = (TextView) findViewById(R.id.tv_marks_collected_count);

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
					recordsCollectedCount.setText(loggerService.getRecordsCount() + "");
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
					recordsCount = intent.getIntExtra(C.PARAM_RECORDS_COUNT, 0);
					recordsCollectedCount.setText(recordsCount + "");
					loggerFinishedTime.setText(getText(R.string.service_running));
					break;

				case C.STATUS_FINISHED:
					loggerFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					break;

				case C.STATUS_ERROR:
					loggerFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					setInterfaceState(R.string.fs_error_msg, true);
					break;
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter(C.BROADCAST_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);

		setInterfaceState(R.string.ext_media_unmounted_msg, !isMediaMounted);
	}

	@Override
	protected void onResume() {
		super.onResume();

//		if (marksCount > 0) {
//			marksCollectedCount.setText(marksCount + "");
//		}

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

	private void setInterfaceState(int resId, boolean isError) {
		if (isError) {
			serviceOnOffButton.setText(getString(R.string.btn_service_start));

			serviceOnOffButton.setEnabled(false);
			markButton.setEnabled(false);
			serviceProgressBar.setVisibility(View.INVISIBLE);

			errorMessage.setText(resId);
			errorMessage.setVisibility(View.VISIBLE);

		} else {
			serviceOnOffButton.setEnabled(true);
			markButton.setEnabled(true);
			errorMessage.setVisibility(View.GONE);
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

	private void updateApplicationStructure(SharedPreferences prefs)	// TODO remove when unnecessary
	{
//		String lastVersion = "version";
		
		//			if(prefs.getInt(lastVersion, 0) < getPackageManager().getPackageInfo(getPackageName(), 0).versionCode)
		if(getPreferences(MODE_PRIVATE).contains(C.PREF_CAT_PATH))
		{	// update from previous version or clean install
			// ==========Improvement==========
			String catPath = getPreferences(MODE_PRIVATE).getString(C.PREF_CAT_PATH, "");
			String info;
			
			File fromCats = new File(catPath);

			String internalPath = getFilesDir().getAbsolutePath();
			File toCats = new File(internalPath + "/" + C.CAT_FILE);

			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(toCats, false));
				BufferedReader in = new BufferedReader(new FileReader(fromCats));

				String[] split;
				String line;

				while ((line = in.readLine()) != null) {
					split = line.split(",");

					if (split.length != 2) {
						in.close();
						pw.close();
						throw new ArrayIndexOutOfBoundsException("Must be two columns splitted by ','!");
					} else
						pw.println(line);
				}

				in.close();
				pw.close();

				info = "Categories loaded from " + catPath;
			} catch (Exception e) {
				info = "Please reload categories file";
			}
			
			prefs.edit().remove(C.PREF_CAT_PATH).commit();
			Toast.makeText(this, info, Toast.LENGTH_LONG).show();
			// ==========End Improvement==========
			
			// save current version to preferences
//				prefs.edit().putInt(lastVersion, getPackageManager().getPackageInfo(getPackageName(), 0).versionCode).commit();
		}
	}
	
	private class NetworkTypeChangeListener extends PhoneStateListener {

		TextView tv;

		public NetworkTypeChangeListener(TextView tv) {
			this.tv = tv;
		}

		@Override
		public void onDataConnectionStateChanged(int state, int networkType) {
			super.onDataConnectionStateChanged(state, networkType);

			tv.setText(GSMEngine.getNetworkGen(networkType) + " / " + GSMEngine.getNetworkType(networkType));
		}
	}
}
