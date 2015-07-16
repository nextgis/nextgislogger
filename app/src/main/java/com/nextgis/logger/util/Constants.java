/**
 * ***************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2015 NextGIS
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * ***************************************************************************
 */
package com.nextgis.logger.util;

import android.os.Environment;

import java.io.File;

public interface Constants {
    String CSV_SEPARATOR = ";";
    String CSV_LOG_CELL = "cell_time_log.csv";
    String CSV_LOG_SENSOR = "sensor_time_log.csv";
    String CSV_LOG_EXTERNAL = "external_time_log.csv";
    String CSV_MARK_CELL = "cell_time_marks.csv";
    String CSV_MARK_SENSOR = "sensor_time_marks.csv";
    String CSV_MARK_EXTERNAL = "external_time_marks.csv";
    String CATEGORIES = "categories.csv";
    String DEVICE_INFO = "device_info.txt";
    String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "nextgis_logger";
    String TEMP_PATH = DATA_PATH + File.separator + ".temp";

    String CSV_HEADER_BASE = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR + "DateTime";

    String CSV_HEADER_CELL = CSV_HEADER_BASE + CSV_SEPARATOR + "NetworkGen" + CSV_SEPARATOR + "NetworkType" + CSV_SEPARATOR + "Active" + CSV_SEPARATOR
            + "MCC" + CSV_SEPARATOR + "MNC" + CSV_SEPARATOR + "LAC" + CSV_SEPARATOR + "CID" + CSV_SEPARATOR + "PSC" + CSV_SEPARATOR + "Power";

    String CSV_HEADER_SENSOR = CSV_HEADER_BASE + CSV_SEPARATOR + "Type" + CSV_SEPARATOR + "Accel_X" + CSV_SEPARATOR + "Accel_Y" + CSV_SEPARATOR + "Accel_Z"
            + CSV_SEPARATOR + "Azimuth" + CSV_SEPARATOR + "Pitch" + CSV_SEPARATOR + "Roll" + CSV_SEPARATOR + "Magnetic_X" + CSV_SEPARATOR + "Magnetic_Y"
            + CSV_SEPARATOR + "Magnetic_Z" + CSV_SEPARATOR + "Gyro_X" + CSV_SEPARATOR + "Gyro_Y" + CSV_SEPARATOR + "Gyro_Z" + CSV_SEPARATOR + "GPS_Lat"
            + CSV_SEPARATOR + "GPS_Lon" + CSV_SEPARATOR + "GPS_Alt" + CSV_SEPARATOR + "GPS_Accuracy" + CSV_SEPARATOR + "GPS_Speed" + CSV_SEPARATOR
            + "GPS_Bearing" + CSV_SEPARATOR + "Audio";

    String PREF_PERIOD_SEC = "period_sec";
    String PREF_SENSOR_STATE = "sensor_state";
    String PREF_SENSOR_MODE = "sensor_mode";
    String PREF_SENSOR_GYRO = "sensor_gyroscope_state";
    String PREF_SENSOR_MAG = "sensor_magnetic_state";
    String PREF_SENSOR_ORIENT = "sensor_orientation_state";
    String PREF_GPS = "gps";
    String PREF_MIC = "sensor_mic";
    String PREF_USE_API17 = "use_api17";
    String PREF_CAT_PATH = "cat_path";
    String PREF_USE_VOL = "use_volume_buttons";
    String PREF_USER_NAME = "user_name";
    String PREF_SESSION_NAME = "session_name";
    String PREF_MARKS_COUNT = "marks_count";
    String PREF_MARK_POS = "mark_position";
    String PREF_RECORDS_COUNT = "records_count";
    String PREF_KEEP_SCREEN = "keep_screen";
    String PREF_EXTERNAL = "external_data";
    String PREF_EXTERNAL_DEVICE = "external_device";
    String PREF_EXTERNAL_HEADER = "external_header";
    String DEFAULT_USERNAME = "User1";
    String LOG_UID = "ServiceLog";

    String BROADCAST_ACTION = "com.nextgis.gsm_logger.MainActivity";

    String PARAM_SERVICE_STATUS = "serviceStatus";
    String PARAM_TIME = "time";
    String PARAM_RECORDS_COUNT = "recordsCount";

    int STATUS_STARTED = 100;
    int STATUS_RUNNING = 101;
    int STATUS_FINISHED = 102;
    int STATUS_ERROR = 103;

    float NaN = Float.NaN;
    int UNDEFINED = -1;
    int UPDATE_FREQUENCY = 100; // in ms
    long  MIN_GPS_TIME = 0;
    float MIN_GPS_DISTANCE = 0f;

    short LOG_TYPE_NETWORK = 0;
    short LOG_TYPE_SENSORS = 1;
    short LOG_TYPE_EXTERNAL = 2;
}
