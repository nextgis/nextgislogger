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

package com.nextgis.logger;

import android.app.Application;

import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.AudioEngine;

public class LoggerApplication extends Application {
    private static ArduinoEngine mArduinoEngine;
    private static AudioEngine mAudioEngine;
    private static LoggerApplication mApplication;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = this;
        mArduinoEngine = new ArduinoEngine(this);
        mAudioEngine = new AudioEngine(this);
    }

    public static LoggerApplication getApplication() {
        return mApplication;
    }

    public ArduinoEngine getArduinoEngine() {
        return mArduinoEngine;
    }

    public AudioEngine getAudioEngine() {
        return mAudioEngine;
    }
}
