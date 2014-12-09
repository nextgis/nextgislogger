package com.nextgis.logger;

import java.io.File;

import android.app.Activity;
import android.os.Environment;

public class C extends Activity {
	public static final String CSV_SEPARATOR = ";";
	public static final String csvLogFile = "cell_time_log.csv";
	public static final String csvLogFileSensor = "sensor_time_log.csv";
	public static final String csvMarkFile = "cell_time_marks.csv";
	public static final String csvMarkFileSensor = "sensor_time_marks.csv";
	public static final String categoriesFile = "categories.csv";
	public static final String dataBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "nextgis_logger";	
	
	public static final String csvMarkHeader = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR
			+ "NetworkGen" + CSV_SEPARATOR + "NetworkType" + CSV_SEPARATOR + "Active" + CSV_SEPARATOR + "MCC" + CSV_SEPARATOR + "MNC" + CSV_SEPARATOR + "LAC"
			+ CSV_SEPARATOR + "CID" + CSV_SEPARATOR + "PSC" + CSV_SEPARATOR + "Power";

	public static final String csvHeaderSensor = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR + "Type"
			+ CSV_SEPARATOR + "X" + CSV_SEPARATOR + "Y" + CSV_SEPARATOR + "Z";

	public static final String logDefaultName = "ServiceLog";
	public static final String markDefaultName = "Mark";

	public static final String PREF_PERIOD_SEC = "period_sec";
	public static final String PREF_SENSOR_STATE = "sensor_state";
	public static final String PREF_SENSOR_MODE = "sensor_mode";
	public static final String PREF_USE_API17 = "use_api17";
	public static final String PREF_USE_CATS = "use_cats";
	public static final String PREF_CAT_PATH = "cat_path";
	public static final String PREF_USER_NAME = "user_name";
	public static final String PREF_SESSION_NAME = "session_name";
	public static final String PREF_MARKS_COUNT = "marks_count";
	public static final String PREF_RECORDS_COUNT = "records_count";
	
	public static final String BROADCAST_ACTION = "com.nextgis.gsm_logger.MainActivity";

	public static final String PARAM_SERVICE_STATUS = "serviceStatus";
	public static final String PARAM_TIME = "time";
	public static final String PARAM_RECORDS_COUNT = "recordsCount";

	public static final int STATUS_STARTED = 100;
	public static final int STATUS_RUNNING = 101;
	public static final int STATUS_FINISHED = 102;
	public static final int STATUS_ERROR = 103;
}
