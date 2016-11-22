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

package com.nextgis.logger.UI;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.nextgis.logger.LoggerService;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.CellEngine;
import com.nextgis.logger.engines.SensorEngine;

public class BindActivity extends ProgressBarActivity implements ArduinoEngine.ConnectionListener, ServiceConnection {
    protected CellEngine mCellEngine;
    protected SensorEngine mSensorEngine;
    protected ArduinoEngine mArduinoEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startLoggerService(this, null);
        Intent connection = new Intent(this, LoggerService.class);
        bindService(connection, this, 0);
    }

    @Override
    protected void onDestroy() {
        onServiceDisconnected(null);
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        LoggerService.LocalBinder binder = (LoggerService.LocalBinder) iBinder;
        mCellEngine = binder.getCellEngine();
        mSensorEngine = binder.getSensorEngine();
        mArduinoEngine = binder.getArduinoEngine();
        mArduinoEngine.addConnectionListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mArduinoEngine.removeConnectionListener(this);
        mCellEngine = null;
        mSensorEngine = null;
        mArduinoEngine = null;
    }

    @Override
    public void onTimeoutOrFailure() {

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnectionLost() {

    }

    public CellEngine getCellEngine() {
        return mCellEngine;
    }

    public SensorEngine getSensorEngine() {
        return mSensorEngine;
    }

    public ArduinoEngine getArduinoEngine() {
        return mArduinoEngine;
    }
}
