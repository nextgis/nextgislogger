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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

	public static final String CSV_SEPARATOR = ";";

	public static final String dataDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "gsm_logger";
	public static final String csvLogFilePath = dataDirPath + File.separator + "gsm_time_log.csv";
	public static final String csvLogFilePathSensor = dataDirPath + File.separator + "sensor_time_log.csv";
	public static final String csvMarkFilePath = dataDirPath + File.separator + "gsm_time_marks.csv";
	public static final String csvMarkFilePathSensor = dataDirPath + File.separator + "sensor_time_marks.csv";

	public static final String csvMarkHeader = "ID" + MainActivity.CSV_SEPARATOR + "Name" + MainActivity.CSV_SEPARATOR + "User" + MainActivity.CSV_SEPARATOR
			+ "TimeStamp" + MainActivity.CSV_SEPARATOR + "NetworkGen" + MainActivity.CSV_SEPARATOR + "NetworkType" + MainActivity.CSV_SEPARATOR + "Active"
			+ MainActivity.CSV_SEPARATOR + "MCC" + MainActivity.CSV_SEPARATOR + "MNC" + MainActivity.CSV_SEPARATOR + "LAC" + MainActivity.CSV_SEPARATOR + "CID"
			+ MainActivity.CSV_SEPARATOR + "PSC" + MainActivity.CSV_SEPARATOR + "RSSI";

	public static final String csvHeaderSensor = "ID" + MainActivity.CSV_SEPARATOR + "Name" + MainActivity.CSV_SEPARATOR + "User" + MainActivity.CSV_SEPARATOR
			+ "TimeStamp" + MainActivity.CSV_SEPARATOR + "Type" + MainActivity.CSV_SEPARATOR + "X" + MainActivity.CSV_SEPARATOR + "Y"
			+ MainActivity.CSV_SEPARATOR + "Z";

	public static final String logDefaultName = "ServiceLog";
	public static final String markDefaultName = "Mark";

	public static final String PREFERENCE_NAME = "MainActivity";
	public static final String PREF_PERIOD_SEC = "periodSec";
	public static final String PREF_SENSOR_STATE = "sensor_state";
	public static final String PREF_SENSOR_MODE = "sensor_mode";
	public static final String PREF_USE_API17 = "use_api17";
	public static final String PREF_USE_CATS = "use_cats";
	public static final String PREF_CAT_PATH = "cat_path";
	public static final String PREF_USER_NAME = "user_name";

	public static final String BROADCAST_ACTION = "com.nextgis.gsm_logger.MainActivity";

	public static final String PARAM_SERVICE_STATUS = "serviceStatus";
	public static final String PARAM_TIME = "time";
	public static final String PARAM_RECORDS_COUNT = "recordsCount";

	public static final int STATUS_STARTED = 100;
	public static final int STATUS_RUNNING = 101;
	public static final int STATUS_FINISHED = 102;
	public static final int STATUS_ERROR = 103;

	//    private static int loggerPeriodSec = minPeriodSec;
	private static long timeStarted = 0;
	private static int recordsCount = 0;
	private static int marksCount = 0;

	private BroadcastReceiver broadcastReceiver;

	private Button serviceOnOffButton;
	private ProgressBar serviceProgressBar;

	private Button markButton;
	private AutoCompleteTextView markTextEditor;

	//    private Button setPeriodButton;
	//    private EditText periodEditor;

	private TextView loggerStartedTime;
	private TextView loggerFinishedTime;
	private TextView recordsCollectedCount;
	private TextView marksCollectedCount;

	private TextView errorMessage;

	private GSMEngine gsmEngine;
	private SensorEngine sensorEngine;
	private ServiceConnection servConn = null;
	CustomArrayAdapter substringMarkNameAdapter;

	NetworkTypeChangeListener networkTypeListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);
		PreferenceManager.setDefaultValues(this, PREFERENCE_NAME, MODE_PRIVATE, R.xml.preferences, false);

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

		markTextEditor = (AutoCompleteTextView) findViewById(R.id.mark_text_editor);
		markTextEditor.requestFocus();

		final SharedPreferences pref = getPreferences(MODE_PRIVATE);
		if (pref.getBoolean(PREF_SENSOR_STATE, true))
			sensorEngine = new SensorEngine(this);

		markButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				try {
					File csvFile = new File(csvMarkFilePath);
					boolean isFileExist = csvFile.exists();
					//					PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(csvFile, true), "UTF-8"));
					PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

					if (!isFileExist) {
						pw.println(csvMarkHeader);
					}

					String markName = markTextEditor.getText().toString();
					String ID = substringMarkNameAdapter == null ? "" : substringMarkNameAdapter.getSelectedMarkNameID(markTextEditor.getText().toString());
					markTextEditor.setText("");

					if (markName.length() == 0) {
						markName = markDefaultName;
					}

					ArrayList<GSMEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();

					for (GSMEngine.GSMInfo gsmInfo : gsmInfoArray) {
						StringBuilder sb = new StringBuilder();

						String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
								+ gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();

						sb.append(ID).append(MainActivity.CSV_SEPARATOR);
						sb.append(markName).append(MainActivity.CSV_SEPARATOR);
						sb.append(pref.getString(PREF_USER_NAME, "User 1")).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getTimeStamp()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.networkGen()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.networkType()).append(MainActivity.CSV_SEPARATOR);
						sb.append(active).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getMcc()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getMnc()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getLac()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getCid()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getPsc()).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfo.getRssi());

						pw.println(sb.toString());
					}

					pw.close();
					marksCollectedCount.setText(++marksCount + "");

					// checking accelerometer data state
					if (pref.getBoolean(PREF_SENSOR_STATE, true)) {
						csvFile = new File(csvMarkFilePathSensor);
						isFileExist = csvFile.exists();
						pw = new PrintWriter(new FileOutputStream(csvFile, true));

						if (!isFileExist)
							pw.println(csvHeaderSensor);

						StringBuilder sb = new StringBuilder();

						sb.append(ID).append(MainActivity.CSV_SEPARATOR);
						sb.append(markName).append(CSV_SEPARATOR);
						sb.append(pref.getString(PREF_USER_NAME, "User 1")).append(MainActivity.CSV_SEPARATOR);
						sb.append(gsmInfoArray.get(0).getTimeStamp()).append(CSV_SEPARATOR);
						sb.append(sensorEngine.getSensorType()).append(CSV_SEPARATOR);
						sb.append(sensorEngine.getX()).append(CSV_SEPARATOR);
						sb.append(sensorEngine.getY()).append(CSV_SEPARATOR);
						sb.append(sensorEngine.getZ());

						pw.println(sb.toString());
						pw.close();
					}

				} catch (FileNotFoundException e) {
					setInterfaceState(R.string.fs_error_msg, true);
				}
			}
		});

		//        final Editor prefEd = pref.edit();

		//        loggerPeriodSec = pref.getInt(PREF_PERIOD_SEC, minPeriodSec);

		//        setPeriodButton = (Button) findViewById(R.id.btn_set_period);

		//        periodEditor = (EditText) findViewById(R.id.period_editor);
		//        periodEditor.setText(loggerPeriodSec + "");

		//        setPeriodButton.setOnClickListener(new View.OnClickListener() {
		//            public void onClick(View v) {
		//
		//                String sPeriod = periodEditor.getText().toString();
		//
		//                if (sPeriod.length() > 0) {
		//                    int sec = Integer.parseInt(sPeriod);
		//
		//                    if (minPeriodSec <= sec && sec <= maxPeriodSec) {
		//                        loggerPeriodSec = sec;
		//                    } else if (sec < minPeriodSec) {
		//                        loggerPeriodSec = minPeriodSec;
		//                    } else if (sec > maxPeriodSec) {
		//                        loggerPeriodSec = maxPeriodSec;
		//                    }
		//
		//                } else {
		//                    loggerPeriodSec = minPeriodSec;
		//                }
		//
		//                prefEd.putInt(PREF_PERIOD_SEC, loggerPeriodSec);
		//                prefEd.commit();
		//
		//                periodEditor.setText(loggerPeriodSec + "");
		//            }
		//        });

		networkTypeListener = new NetworkTypeChangeListener((TextView) findViewById(R.id.tv_network_type_str));

		loggerStartedTime = (TextView) findViewById(R.id.tv_logger_started_time);
		loggerFinishedTime = (TextView) findViewById(R.id.tv_logger_finished_time);
		recordsCollectedCount = (TextView) findViewById(R.id.tv_records_collected_count);
		marksCollectedCount = (TextView) findViewById(R.id.tv_marks_collected_count);

		if (marksCount > 0) {
			marksCollectedCount.setText(marksCount + "");
		}

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
				int serviceStatus = intent.getIntExtra(PARAM_SERVICE_STATUS, 0);
				long time = intent.getLongExtra(PARAM_TIME, 0);

				switch (serviceStatus) {
				case STATUS_STARTED:
					timeStarted = time;
					loggerStartedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					loggerFinishedTime.setText(getText(R.string.service_running));
					break;

				case STATUS_RUNNING:
					recordsCount = intent.getIntExtra(PARAM_RECORDS_COUNT, 0);
					recordsCollectedCount.setText(recordsCount + "");
					loggerFinishedTime.setText(getText(R.string.service_running));
					break;

				case STATUS_FINISHED:
					loggerFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					break;

				case STATUS_ERROR:
					loggerFinishedTime.setText(millisToDate(time, "dd.MM.yyyy hh:mm:ss"));
					setInterfaceState(R.string.fs_error_msg, true);
					break;
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);

		setInterfaceState(R.string.ext_media_unmounted_msg, !isMediaMounted);
	}

	@Override
	protected void onResume() {
		super.onResume();
		gsmEngine.onResume();

		if (sensorEngine != null)
			sensorEngine.onResume(this);
		else if (getPreferences(MODE_PRIVATE).getBoolean(PREF_SENSOR_STATE, true))
			sensorEngine = new SensorEngine(this);

		//		int networkType = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType();

		//		if (!GSMEngine.isGSMNetwork(networkType)) {
		//			errorMessage.setText(R.string.network_error);
		//			errorMessage.setVisibility(View.VISIBLE);
		//		} else
		//			errorMessage.setVisibility(View.GONE);

		if (getPreferences(MODE_PRIVATE).getBoolean(PREF_USE_CATS, false)) { // reload file
			List<MarkName> markNames = new ArrayList<MainActivity.MarkName>();

			String catPath = getPreferences(MODE_PRIVATE).getString(PREF_CAT_PATH, "");
			File cats = new File(catPath);

			if (cats.isFile()) {
				BufferedReader in;
				String[] split;

				try {
					in = new BufferedReader(new FileReader(catPath));
					String line;

					while ((line = in.readLine()) != null) {
						split = line.split(",");
						markNames.add(new MarkName(split[0], split[1]));
					}
				} catch (FileNotFoundException e) {
					Toast.makeText(this, "No file found in " + catPath, Toast.LENGTH_LONG).show();
				} catch (IOException e) {
					Toast.makeText(this, R.string.fs_error_msg, Toast.LENGTH_SHORT).show();
				} catch (Exception other) {
					Toast.makeText(this, R.string.cat_file_error, Toast.LENGTH_SHORT).show();
				}

				substringMarkNameAdapter = new CustomArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, markNames);
			}
		} else
			substringMarkNameAdapter = null;

		markTextEditor.setAdapter(substringMarkNameAdapter);

		((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(networkTypeListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
	}

	@Override
	protected void onPause() {
		gsmEngine.onPause();

		if (sensorEngine != null)
			sensorEngine.onPause();

		((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(networkTypeListener, PhoneStateListener.LISTEN_NONE);

		super.onPause();
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

	//    public static int getLoggerPeriodSec() {
	//        return loggerPeriodSec;
	//    }

	private void setInterfaceState(int resId, boolean isError) {

		if (isError) {
			serviceOnOffButton.setText(getString(R.string.btn_service_start));

			serviceOnOffButton.setEnabled(false);
			markButton.setEnabled(false);
			//            setPeriodButton.setEnabled(false);
			//            periodEditor.setEnabled(false);

			serviceProgressBar.setVisibility(View.INVISIBLE);
			markTextEditor.setVisibility(View.GONE);

			errorMessage.setText(resId);
			errorMessage.setVisibility(View.VISIBLE);

		} else {
			serviceOnOffButton.setEnabled(true);
			markButton.setEnabled(true);
			//            setPeriodButton.setEnabled(true);
			//            periodEditor.setEnabled(true);

			markTextEditor.setVisibility(View.VISIBLE);
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

	public class CustomArrayAdapter extends ArrayAdapter<String> implements Filterable {
		private List<MarkName> objects;
		private ArrayList<MarkName> matches = new ArrayList<MarkName>();;
		private final CustomFilter substringFilter = new CustomFilter();

		public CustomArrayAdapter(final Context ctx, final int selectDialogItem, final List<MarkName> objects) {
			super(ctx, selectDialogItem);
			this.objects = objects;
		}

		@Override
		public Filter getFilter() {
			return substringFilter;
		}

		@Override
		public int getCount() {
			return matches.size();
		}

		@Override
		public String getItem(int position) {
			return matches.get(position).getCAT();
		}

		public String getSelectedMarkNameID(String prefix) {
			for (MarkName item : objects) {
				String CAT = substringFilter.getUpperString(item.getCAT());

				if (CAT.equals(substringFilter.getUpperString(prefix)))
					return item.getID();
			}

			return "";
		}

		private class CustomFilter extends Filter {
			@Override
			protected FilterResults performFiltering(final CharSequence prefix) {
				final FilterResults results = new FilterResults();

				if (prefix != null && prefix.length() > 0) {
					ArrayList<MarkName> resultList = new ArrayList<MarkName>();

					for (MarkName item : objects) {
						String CAT = getUpperString(item.getCAT());
						String substr = getUpperString(prefix.toString());

						if (CAT.contains(substr))
							resultList.add(item);
					}

					results.count = resultList.size();
					results.values = resultList;
				}

				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(final CharSequence constraint, final FilterResults results) {
				if (results != null && results.count > 0) {
					matches.clear();
					matches.addAll((ArrayList<MarkName>)results.values);
					
					notifyDataSetChanged();
				} else
					notifyDataSetInvalidated();
			}

			public String getUpperString(String str) {
				return str.toUpperCase(Locale.getDefault());
			}
		}
	}

	public class MarkName {
		private String ID = "";
		private String CAT = "Mark";

		public MarkName(String ID, String CAT) {
			this.ID = ID;
			this.CAT = CAT;
		}

		public String getID() {
			return ID;
		}

		public String getCAT() {
			return CAT;
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
