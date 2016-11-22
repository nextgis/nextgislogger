/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2016 NextGIS
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

package com.nextgis.logger.livedata;

import android.support.v4.app.Fragment;

import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.InfoItem;

import java.util.ArrayList;

public class InfoFragment extends Fragment {
    protected BaseEngine mEngine;
    protected BaseEngine.EngineListener mListener;

    public void setEngine(BaseEngine engine) {
        if (mEngine != null)
            mEngine.removeListener(mListener);

        mEngine = engine;

        if (mEngine != null)
            mEngine.addListener(mListener);

        if (isAdded())
            onResume();
    }

    public boolean isConnected() {
        return mEngine != null;
    }

    public ArrayList<InfoItem> getData() {
        return isConnected() ? mEngine.getData() : new ArrayList<InfoItem>();
    }
}
