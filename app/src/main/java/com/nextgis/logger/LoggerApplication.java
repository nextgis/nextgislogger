/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015-2016 NextGIS
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

package com.nextgis.logger;

import android.app.Application;

import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.CellEngine;
import com.nextgis.logger.engines.SensorEngine;

public class LoggerApplication extends Application {
    private static ArduinoEngine mArduinoEngine;
    private static SensorEngine mSensorEngine;
    private static CellEngine mCellEngine;
    private static LoggerApplication mApplication;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = this;
        mArduinoEngine = new ArduinoEngine(this);
        mSensorEngine = new SensorEngine(this);
        mCellEngine = new CellEngine(this);
    }

    public static LoggerApplication getApplication() {
        return mApplication;
    }

    public ArduinoEngine getArduinoEngine() {
        return mArduinoEngine;
    }

    public SensorEngine getSensorEngine() {
        return mSensorEngine;
    }

    public CellEngine getCellEngine() {
        return mCellEngine;
    }
}
