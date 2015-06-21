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

package com.nextgis.logger.engines;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseEngine {
    protected Context mContext;
    protected List<EngineListener> mListeners;

    public interface EngineListener {
        void onInfoChanged();
    }

    public BaseEngine(Context context) {
        mContext = context;
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

    protected void notifyListeners() {
        for (EngineListener listener : mListeners)
            listener.onInfoChanged();
    }

    public abstract void onPause();
    public abstract void onResume();

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mContext);
    }
}
