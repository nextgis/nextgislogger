/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS
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

package com.nextgis.logger.engines;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class BaseEngine {
    protected int mLocks;
    protected Context mContext;
    protected ArrayList<InfoItem> mItems;
    protected List<EngineListener> mListeners;

    public interface EngineListener {
        void onInfoChanged(String sourceEngine);
    }

    public abstract boolean isEngineEnabled();
    protected abstract void loadHeader();

    public BaseEngine(Context context) {
        mLocks = 0;
        mContext = context;
        mItems = new ArrayList<>();
        mListeners = new ArrayList<>();
    }

    public void addListener(EngineListener listener) {
        mListeners.add(listener);
    }

    public boolean removeListener(EngineListener listener) {
        return mListeners.remove(listener);
    }

    public int getListenersCount() {
        return mListeners.size();
    }

    protected void notifyListeners(String source) {
        for (EngineListener listener : mListeners)
            listener.onInfoChanged(source);
    }

    /**
     * Must be called before start getting data
     * @return  True if has any active receivers
     */
    public boolean onResume() {
        return ++mLocks > 0;
    }

    /**
     * Must be called in the end of getting data
     * @return  True if does not have any active receivers
     */
    public boolean onPause() {
        return --mLocks <= 0;
    }

    public static long saveMark(long session, int id, String name, long timestamp, GeoPoint point) {
        NGWVectorLayer markLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_MARK);
        if (markLayer != null) {
            Feature mark = new Feature(Constants.NOT_FOUND, markLayer.getFields());
            mark.setFieldValue(LoggerApplication.FIELD_SESSION, session);
            mark.setFieldValue(LoggerApplication.FIELD_MARK_ID, id);
            mark.setFieldValue(LoggerApplication.FIELD_NAME, name);
            mark.setFieldValue(LoggerApplication.FIELD_TIMESTAMP, timestamp * 1d);
            mark.setFieldValue(LoggerApplication.FIELD_DATETIME, timestamp);
            mark.setGeometry(point);
            return markLayer.createFeature(mark);
        }

        return Constants.NOT_FOUND;
    }

    public abstract void saveData(long markId);

    public abstract void saveData(ArrayList<InfoItem> items, long markId);

//    TODO
//    public void saveToCSV throws FileNotFoundException {
//        String logPath = MainActivity.dataDirPath + File.separator;
//        String logHeader = LoggerConstants.CSV_HEADER_PREAMBLE + getHeader();
//
//        if (this instanceof CellEngine) {
//            if (onDemand)
//                logPath += LoggerConstants.CSV_MARK_CELL;
//            else
//                logPath += LoggerConstants.CSV_LOG_CELL;
//        } else if (this instanceof SensorEngine) {
//            if (onDemand)
//                logPath += LoggerConstants.CSV_MARK_SENSOR;
//            else
//                logPath += LoggerConstants.CSV_LOG_SENSOR;
//        } else if (this instanceof ArduinoEngine) {
//            if (onDemand)
//                logPath += LoggerConstants.CSV_MARK_EXTERNAL;
//            else
//                logPath += LoggerConstants.CSV_LOG_EXTERNAL;
//        } else
//            throw new RuntimeException(getClass().getSimpleName() + " is not supported.");
//
//        FileUtil.append(logPath, logHeader, data);
//    }

    public String getHeader() {
        String result = "";

        for (InfoItem item : mItems)
            for (String name : item.getShortNames())
                result += LoggerConstants.CSV_SEPARATOR + name;

        return result;
    }

    public ArrayList<InfoItem> getData() {
        return mItems;
    }

    public List<String> getDataAsStringList(String preamble) {
        String result = preamble;

        for (InfoItem item : mItems)
            for (InfoColumn column : item.getColumns())
                result += LoggerConstants.CSV_SEPARATOR + column.getValue();

        return Collections.singletonList(result);
    }

    public static String getPreamble(String ID, String markName, String userName, long timeStamp) {
        return ID + LoggerConstants.CSV_SEPARATOR + markName + LoggerConstants.CSV_SEPARATOR + userName + LoggerConstants.CSV_SEPARATOR
                + timeStamp + LoggerConstants.CSV_SEPARATOR + DateFormat.getDateTimeInstance().format(new Date(timeStamp));
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mContext);
    }
}
