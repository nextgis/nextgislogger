/**
 * ***************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
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
 * ***************************************************************************
 */

package com.nextgis.logger.util;

public interface LoggerConstants {
    String AUTHORITY = "com.nextgis.logger.provider";

    String CSV_SEPARATOR = ";";
    String CELL = "cell";
    String SENSOR = "sensor";
    String EXTERNAL = "external";
    String DATA = "data";
    String LOG = "log";
    String MARK = "mark";
    String CATEGORIES = "categories.csv";
    String DEVICE_INFO = "device_info.txt";
    String TEMP_PATH = ".temp";
    String CSV_EXT = ".csv";
    String ZIP_EXT = ".zip";

    String CSV_HEADER_PREAMBLE = "ID" + CSV_SEPARATOR + "Name" + CSV_SEPARATOR + "User" + CSV_SEPARATOR + "TimeStamp" + CSV_SEPARATOR + "DateTime";

    String HEADER_GEN = "NetworkGen";
    String HEADER_TYPE = "NetworkType";
    String HEADER_ACTIVE = "Active";
    String HEADER_MCC = "MCC";
    String HEADER_MNC = "MNC";
    String HEADER_LAC = "LAC";
    String HEADER_TAC = "TAC";
    String HEADER_CID = "CID";
    String HEADER_PCI = "PCI";
    String HEADER_PSC = "PSC";
    String HEADER_CI = "CI";
    String HEADER_POWER = "Power";
    String HEADER_RSSI = "RSSI";
    String HEADER_RSCP = "RSCP";

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

    String PREF_APP_VERSION = "app_version";
    String PREF_PERIOD_SEC = "period_sec";
    String PREF_SENSOR_STATE = "sensor_state";
    String PREF_SENSOR_MODE = "sensor_mode";
    String PREF_SENSOR_GYRO = "sensor_gyroscope_state";
    String PREF_SENSOR_MAG = "sensor_magnetic_state";
    String PREF_SENSOR_ORIENT = "sensor_orientation_state";
    String PREF_GPS = "gps";
    String PREF_MIC = "sensor_mic";
    String PREF_MIC_DELTA = "sensor_mic_delta";
    String PREF_USE_API17 = "use_api17";
    String PREF_CAT_PATH = "cat_path";
    String PREF_USE_VOL = "use_volume_buttons";
    String PREF_USER_NAME = "user_name";
    String PREF_SESSION_ID = "session_name";
    String PREF_MARKS_COUNT = "marks_count";
    String PREF_MARK_POS = "mark_position";
    String PREF_RECORDS_COUNT = "records_count";
    String PREF_KEEP_SCREEN = "keep_screen";
    String PREF_EXTERNAL = "external_data";
    String PREF_EXTERNAL_DEVICE = "external_device";
    String PREF_EXTERNAL_HEADER = "external_header";
    String PREF_TIME_START = "time_start";
    String PREF_TIME_FINISH = "time_finish";
    String PREF_MEASURING = "service_measuring";
    String PREF_ACCOUNT_EDIT = "account_edit";
    String PREF_ACCOUNT_DELETE = "account_delete";
    String PREF_AUTO_SYNC = "sync_auto";
    String PREF_AUTO_SYNC_PERIOD = "sync_period";
    String PREF_SYNC_PERIOD_SEC_LONG = "sync_period_sec_long";

    String DEFAULT_USERNAME = "User1";
    String LOG_UID = "ServiceLog";

    String SERVICE_STATUS = "service_status";
    String ACTION_INFO = "com.nextgis.logger.SERVICE_INFO";
    String ACTION_START = "com.nextgis.logger.SERVICE_START";
    String ACTION_STOP = "com.nextgis.logger.SERVICE_STOP";
    String ACTION_DESTROY = "com.nextgis.logger.SERVICE_DESTROY";

    String GEN_2G = "2G";
    String GEN_3G = "3G";
    String GEN_4G = "4G";
    String UNKNOWN = "unknown";
    String NO_DATA = "null";

    int STATUS_STARTED = 100;
    int STATUS_RUNNING = 101;
    int STATUS_FINISHED = 102;
    int STATUS_ERROR = 103;

    int UNDEFINED = -1;
    int UPDATE_FREQUENCY = 250; // in ms
    long  MIN_GPS_TIME = 0;
    float MIN_GPS_DISTANCE = 0f;
}
