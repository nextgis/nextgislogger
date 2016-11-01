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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;

public abstract class BaseEngine {
    private int mLocks;
    Context mContext;
    ArrayList<InfoItem> mItems;
    private List<EngineListener> mListeners;
    Uri mUri;

    public interface EngineListener {
        void onInfoChanged(String sourceEngine);
    }

    public abstract boolean isEngineEnabled();
    protected abstract void loadEngine();

    public BaseEngine(Context context) {
        mLocks = 0;
        mContext = context;
        mItems = new ArrayList<>();
        mListeners = new ArrayList<>();
        mUri = Uri.parse("content://" + ((IGISApplication) context.getApplicationContext()).getAuthority());
    }

    public void addListener(EngineListener listener) {
        mListeners.add(listener);
    }

    public boolean removeListener(EngineListener listener) {
        return mListeners.remove(listener);
    }

    int getListenersCount() {
        return mListeners.size();
    }

    void notifyListeners(String source) {
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

    public static String saveMark(Uri uri, String session, int id, String name, long timestamp, GeoPoint point) {
        NGWVectorLayer markLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_MARK);
        if (markLayer != null) {
            String uniqueId = UUID.randomUUID().toString();
            ContentValues cv = new ContentValues();
            cv.put(LoggerApplication.FIELD_UNIQUE_ID, uniqueId);
            cv.put(LoggerApplication.FIELD_SESSION, session);
            cv.put(LoggerApplication.FIELD_MARK_ID, id);
            cv.put(LoggerApplication.FIELD_NAME, name);
            cv.put(LoggerApplication.FIELD_TIMESTAMP, timestamp * 1d);
            cv.put(LoggerApplication.FIELD_DATETIME, timestamp);
            try {
                cv.put(FIELD_GEOM, point.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }

            markLayer.insert(uri, cv);
            return uniqueId;
        }

        return null;
    }

    public static void deleteMark(Uri uri, String uuid) {
        NGWVectorLayer markLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_MARK);
        if (markLayer != null) {
            markLayer.delete(uri, LoggerApplication.FIELD_UNIQUE_ID + " = ?" , new String[]{uuid});
        }
    }

    public abstract void saveData(String markId);

    public abstract void saveData(ArrayList<InfoItem> items, String markId);

    public ArrayList<InfoItem> getData() {
        return mItems;
    }

    public static String getPreamble(String ID, String markName, String userName, long timeStamp) {
        return ID + LoggerConstants.CSV_SEPARATOR + markName + LoggerConstants.CSV_SEPARATOR + userName + LoggerConstants.CSV_SEPARATOR
                + timeStamp + LoggerConstants.CSV_SEPARATOR + DateFormat.getDateTimeInstance().format(new Date(timeStamp));
    }

    SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mContext);
    }
}
