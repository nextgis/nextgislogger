/**
 * ***************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2016 NextGIS
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

    String CSV_HEADER_PREAMBLE = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR + "DateTime";

    String CSV_HEADER_CELL = CSV_SEPARATOR + "NetworkGen" + CSV_SEPARATOR + "NetworkType" + CSV_SEPARATOR + "Active" + CSV_SEPARATOR
            + "MCC" + CSV_SEPARATOR + "MNC" + CSV_SEPARATOR + "LAC" + CSV_SEPARATOR + "CID" + CSV_SEPARATOR + "PSC" + CSV_SEPARATOR + "Power";

    String HEADER_GEN = "NetworkGen";
    String HEADER_TYPE = "NetworkType";
    String HEADER_ACTIVE = "Active";
    String HEADER_MCC = "MCC";
    String HEADER_MNC = "MNC";
    String HEADER_LAC = "LAC";
    String HEADER_CID = "CID";
    String HEADER_PSC = "PSC";
    String HEADER_POWER = "Power";

    String HEADER_ACC_X = "Accel_X";
    String HEADER_ACC_Y = "Accel_Y";
    String HEADER_ACC_Z = "Accel_Z";
    String HEADER_LINEAR_X = "Linear_X";
    String HEADER_LINEAR_Y = "Linear_Y";
    String HEADER_LINEAR_Z = "Linear_Z";
    String HEADER_AZIMUTH = "Azimuth";
    String HEADER_PITCH = "Pitch";
    String HEADER_ROLL = "Roll";
    String HEADER_MAGNETIC_X = "Magnetic_X";
    String HEADER_MAGNETIC_Y = "Magnetic_Y";
    String HEADER_MAGNETIC_Z = "Magnetic_Z";
    String HEADER_GYRO_X = "Gyro_X";
    String HEADER_GYRO_Y = "Gyro_Y";
    String HEADER_GYRO_Z = "Gyro_Z";

    String HEADER_GPS_LAT = "GPS_Lat";
    String HEADER_GPS_LON = "GPS_Lon";
    String HEADER_GPS_ALT = "GPS_Alt";
    String HEADER_GPS_ACC = "GPS_Accuracy";
    String HEADER_GPS_SP = "GPS_Speed";
    String HEADER_GPS_BE = "GPS_Bearing";
    String HEADER_GPS_SAT = "GPS_Satellites";
    String HEADER_GPS_TIME = "GPS_FixTime";
    String HEADER_AUDIO = "Audio";

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

    String GEN_2G = "2G";
    String GEN_3G = "3G";
    String GEN_4G = "4G";
    String UNKNOWN = "unknown";
    String NO_DATA = "NaN";

    String BROADCAST_ACTION = "com.nextgis.gsm_logger.MainActivity";

    String PARAM_SERVICE_STATUS = "serviceStatus";
    String PARAM_TIME = "time";
    String PARAM_RECORDS_COUNT = "recordsCount";

    int STATUS_STARTED = 100;
    int STATUS_RUNNING = 101;
    int STATUS_FINISHED = 102;
    int STATUS_ERROR = 103;

    int UNDEFINED = -1;
    int UPDATE_FREQUENCY = 250; // in ms
    long  MIN_GPS_TIME = 0;
    float MIN_GPS_DISTANCE = 0f;
}
