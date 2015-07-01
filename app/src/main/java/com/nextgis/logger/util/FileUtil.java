/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015 NextGIS
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

package com.nextgis.logger.util;

import android.os.Environment;

import com.nextgis.logger.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public final class FileUtil {
    /**
     * Delete set of any files or directories.
     *
     * @param files File[] with all data to delete
     */
    public static void deleteFiles(File[] files) {
        if (files != null) { // there are something to delete
            for (File file : files)
                deleteDirectoryOrFile(file);
        }
    }

    /**
     * Delete single file or directory recursively (deleting anything inside it).
     *
     * @param dir The file / dir to delete
     * @return true if the file / dir was successfully deleted
     */
    public static boolean deleteDirectoryOrFile(File dir) {
        if (!dir.exists())
            return false;

        if (!dir.isDirectory())
            return dir.delete();
        else {
            String[] files = dir.list();

            for (String file : files) {
                File f = new File(dir, file);

                if (f.isDirectory())
                    deleteDirectoryOrFile(f);
                else
                    if (!f.delete())
                        return false;
            }
        }

        return dir.delete();
    }

    /**
     * Check directory existence or create it (with parents if missing).
     *
     * @param path Path to directory
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

    /**
     * Save item to log.
     *
     * @param logType   Log type (0 = Network, 1 = Sensors, 2 = External)
     * @param onDemand  Does entered by user or service
     * @param item      String item to save
     */
    public static void saveItemToLog(short logType, boolean onDemand, String item) throws FileNotFoundException, RuntimeException {
        String logPath, logHeader;

        switch (logType) {
            case Constants.LOG_TYPE_NETWORK:
                logHeader = Constants.CSV_HEADER_CELL;

                if (onDemand)
                    logPath = MainActivity.csvMarkFilePath;
                else
                    logPath = MainActivity.csvLogFilePath;
                break;
            case Constants.LOG_TYPE_SENSORS:
                logHeader = Constants.CSV_HEADER_SENSOR;

                if (onDemand)
                    logPath = MainActivity.csvMarkFilePathSensor;
                else
                    logPath = MainActivity.csvLogFilePathSensor;
                break;
            case Constants.LOG_TYPE_EXTERNAL:
                logHeader = Constants.CSV_HEADER_EXTERNAL;

                if (onDemand)
                    logPath = MainActivity.csvMarkFilePathExternal;
                else
                    logPath = MainActivity.csvLogFilePathExternal;
                break;
            default:
                throw new RuntimeException("Can not handle type: " + logType + ". Supported types are: 0 = Network and 1 = Sensors.");
        }

        File logFile = new File(logPath);
        boolean isFileExist = logFile.exists();
        PrintWriter pw = new PrintWriter(new FileOutputStream(logFile, true));

        if (!isFileExist)
            pw.println(logHeader);

        pw.println(item);
        pw.close();
    }
}
