/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
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
 *****************************************************************************/
package com.nextgis.logger;

import android.os.Environment;

import java.io.File;

public class C {
	public static final String CSV_SEPARATOR = ";";
	public static final String csvLogFile = "cell_time_log.csv";
	public static final String csvLogFileSensor = "sensor_time_log.csv";
	public static final String csvMarkFile = "cell_time_marks.csv";
	public static final String csvMarkFileSensor = "sensor_time_marks.csv";
	public static final String categoriesFile = "categories.csv";
	public static final String deviceInfoFile = "device_info.txt";
	public static final String dataBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "nextgis_logger";
	public static final String tempPath = dataBasePath + File.separator + ".temp";

	public static final String csvMarkHeader = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR
			+ "NetworkGen" + CSV_SEPARATOR + "NetworkType" + CSV_SEPARATOR + "Active" + CSV_SEPARATOR + "MCC" + CSV_SEPARATOR + "MNC" + CSV_SEPARATOR + "LAC"
			+ CSV_SEPARATOR + "CID" + CSV_SEPARATOR + "PSC" + CSV_SEPARATOR + "Power";

	public static final String csvHeaderSensor = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR + "Type"
			+ CSV_SEPARATOR + "Accel_X" + CSV_SEPARATOR + "Accel_Y" + CSV_SEPARATOR + "Accel_Z" + CSV_SEPARATOR + "Azimuth" + CSV_SEPARATOR + "Pitch" + CSV_SEPARATOR + "Roll"
			+ CSV_SEPARATOR + "Magnetic" + CSV_SEPARATOR + "Gyro_X" + CSV_SEPARATOR + "Gyro_Y" + CSV_SEPARATOR + "Gyro_Z" + CSV_SEPARATOR + "GPS_Lat" +
            CSV_SEPARATOR + "GPS_Lon" + CSV_SEPARATOR + "GPS_Alt" + CSV_SEPARATOR + "GPS_Accuracy" + CSV_SEPARATOR + "GPS_Speed" + CSV_SEPARATOR +
            "GPS_Bearing";
//			+ CSV_SEPARATOR + "Magnetic" + CSV_SEPARATOR + "Gyro_X" + CSV_SEPARATOR + "Gyro_Y" + CSV_SEPARATOR + "Gyro_Z";

	public static final String logDefaultName = "ServiceLog";
	public static final String markDefaultName = "Mark";

	public static final String PREF_PERIOD_SEC = "period_sec";
	public static final String PREF_SENSOR_STATE = "sensor_state";
	public static final String PREF_SENSOR_MODE = "sensor_mode";
	public static final String PREF_SENSOR_GYRO = "sensor_gyroscope_state";
	public static final String PREF_SENSOR_MAG = "sensor_magnetic_state";
	public static final String PREF_SENSOR_ORIENT = "sensor_orientation_state";
	public static final String PREF_GPS = "gps";
	public static final String PREF_USE_API17 = "use_api17";
	public static final String PREF_USE_CATS = "use_cats";
	public static final String PREF_CAT_PATH = "cat_path";
	public static final String PREF_USE_VOL = "use_volume_buttons";
	public static final String PREF_USER_NAME = "user_name";
	public static final String PREF_SESSION_NAME = "session_name";
	public static final String PREF_MARKS_COUNT = "marks_count";
    public static final String PREF_MARK_POS = "mark_position";
	public static final String PREF_RECORDS_COUNT = "records_count";
    public static final String PREF_KEEP_SCREEN = "keep_screen";
    public static final String DEFAULT_USERNAME = "User1";

	public static final String BROADCAST_ACTION = "com.nextgis.gsm_logger.MainActivity";

	public static final String PARAM_SERVICE_STATUS = "serviceStatus";
	public static final String PARAM_TIME = "time";
	public static final String PARAM_RECORDS_COUNT = "recordsCount";

	public static final int STATUS_STARTED = 100;
	public static final int STATUS_RUNNING = 101;
	public static final int STATUS_FINISHED = 102;
	public static final int STATUS_ERROR = 103;

    public static final int UNDEFINED = -1;

    public static final short LOG_TYPE_NETWORK = 0;
    public static final short LOG_TYPE_SENSORS = 1;
}
