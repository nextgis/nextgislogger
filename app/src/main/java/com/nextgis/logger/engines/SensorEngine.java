/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov
 ******************************************************************************
 * Copyright © 2014-2015 NextGIS
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
 *****************************************************************************/
package com.nextgis.logger.engines;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.widget.Toast;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.R;
import com.nextgis.logger.util.Constants;

import java.util.ArrayList;

public class SensorEngine extends BaseEngine implements SensorEventListener, BaseEngine.EngineListener {
    private float[] mAccelerometer, mGyroscope, mMagnetic, mOrientation;
    private long mLastUpdateAccelerometer, mLastUpdateGyro, mLastUpdateMag, mLastUpdateOrient;
    private String mAccelerometerName, mMagneticName, mOrientationName, mGyroscopeName;
    private boolean mIsLinearAcceleration;
    private int mAccelerometerType;

    private final SensorManager mSensorManager;
    private GPSEngine mGpsEngine;
    private static AudioEngine mAudioEngine;
    private Handler mAudioHandler;
    private SharedPreferences mPreferences;

    public SensorEngine(Context context) {
		super(context);
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mGpsEngine = new GPSEngine(context);
        mAudioEngine = LoggerApplication.getApplication().getAudioEngine();
        mAudioHandler = new Handler();
		mPreferences = getPreferences();

		mAccelerometer = new float[3];
        mAccelerometer[0] = mAccelerometer[1] = mAccelerometer[2] = Float.NaN;
        mGyroscope = mMagnetic = mOrientation = mAccelerometer.clone();
	}

	public void onPause() {
		mSensorManager.unregisterListener(this);
        mGpsEngine.onPause();
        mAudioEngine.onPause();
        mAudioEngine.removeListener(this);
	}

	@SuppressWarnings("deprecation")
	public void onResume() {
		boolean noSensor = false;
		ArrayList<String> noSensors = new ArrayList<>();
        Sensor sensor;

		if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER)) {
			mIsLinearAcceleration = mPreferences.getBoolean(Constants.PREF_SENSOR_MODE, false);
            mAccelerometerType = mIsLinearAcceleration ? Sensor.TYPE_LINEAR_ACCELERATION : Sensor.TYPE_ACCELEROMETER;
            sensor = mSensorManager.getDefaultSensor(mAccelerometerType);

			if (sensor != null) {
                mAccelerometerName = sensor.getName();
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mIsLinearAcceleration ?
                        mContext.getString(R.string.sensor_linear).toLowerCase() :
                        mContext.getString(R.string.sensor_accelerometer).toLowerCase());
				noSensor = true;
			}
		}

		if (isSensorEnabled(Sensor.TYPE_GYROSCOPE)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

			if (sensor != null) {
                mGyroscopeName = sensor.getName();
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mContext.getString(R.string.sensor_gyroscope).toLowerCase());
				noSensor = true;
                mPreferences.edit().putBoolean(Constants.PREF_SENSOR_GYRO, false).apply();
			}
		}

		if (isSensorEnabled(Sensor.TYPE_ORIENTATION)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

			if (sensor != null) {
                mOrientationName = sensor.getName();
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mContext.getString(R.string.sensor_orientation).toLowerCase());
				noSensor = true;
                mPreferences.edit().putBoolean(Constants.PREF_SENSOR_ORIENT, false).apply();
			}
		}

		if (isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

			if (sensor != null) {
                mMagneticName = sensor.getName();
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mContext.getString(R.string.sensor_magnetic).toLowerCase());
				noSensor = true;
                mPreferences.edit().putBoolean(Constants.PREF_SENSOR_MAG, false).apply();
			}
		}

		if (noSensor) {
			StringBuilder info = new StringBuilder();
			info.append(mContext.getString(R.string.sensor_error));

			for (int i = 0; i < noSensors.size(); i++) {
				if (i > 0)
					info.append(", ");

				info.append(noSensors.get(i));
			}

			Toast.makeText(mContext, info.toString(), Toast.LENGTH_LONG).show();
		}

        mGpsEngine.onResume();
        mAudioEngine.onResume();
        mAudioEngine.addListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		long curTime = System.currentTimeMillis();

		if (event.sensor.getType() == mAccelerometerType) {
			if ((curTime - mLastUpdateAccelerometer) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateAccelerometer = curTime;
                mAccelerometer = event.values.clone();
                notifyListeners();
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if ((curTime - mLastUpdateOrient) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateOrient = curTime;
                mOrientation = event.values.clone();
                notifyListeners();
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if ((curTime - mLastUpdateMag) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateMag = curTime;
                mMagnetic = event.values.clone();
                notifyListeners();
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if ((curTime - mLastUpdateGyro) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateGyro = curTime;
				mGyroscope = event.values.clone();
                notifyListeners();
			}
		}
	}

	public float getAccelerometerX() {
		return mAccelerometer[0];
	}

	public float getAccelerometerY() {
		return mAccelerometer[1];
	}

	public float getAccelerometerZ() {
		return mAccelerometer[2];
	}
	
	public float getGyroscopeX() {
		return mGyroscope[0];
	}
	
	public float getGyroscopeY() {
		return mGyroscope[1];
	}
	
	public float getGyroscopeZ() {
		return mGyroscope[2];
	}
	
	public float getMagneticX() {
		return mMagnetic[0];
	}

	public float getMagneticY() {
		return mMagnetic[1];
	}

	public float getMagneticZ() {
		return mMagnetic[2];
	}

	public float getAzimuth() {
		return mOrientation[0];
	}
	
	public float getPitch() {
		return mOrientation[1];
	}
	
	public float getRoll() {
		return mOrientation[2];
	}

    public String getAccelerometerName() {
        return mAccelerometerName;
    }

    public String getMagneticName() {
        return mMagneticName;
    }

    public String getOrientName() {
        return mOrientationName;
    }

    public String getGyroName() {
        return mGyroscopeName;
    }

	@SuppressWarnings("deprecation")
	public boolean isSensorEnabled(int sensorType) {
		switch (sensorType) {
		case Sensor.TYPE_ACCELEROMETER:
			return mPreferences.getBoolean(Constants.PREF_SENSOR_STATE, false);
		case Sensor.TYPE_GYROSCOPE:
			return mPreferences.getBoolean(Constants.PREF_SENSOR_GYRO, false);
		case Sensor.TYPE_MAGNETIC_FIELD:
			return mPreferences.getBoolean(Constants.PREF_SENSOR_MAG, false);
		case Sensor.TYPE_ORIENTATION:
			return mPreferences.getBoolean(Constants.PREF_SENSOR_ORIENT, false);
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	public boolean isAnySensorEnabled() {
		return isSensorEnabled(Sensor.TYPE_ACCELEROMETER) || isSensorEnabled(Sensor.TYPE_GYROSCOPE)
				|| isSensorEnabled(Sensor.TYPE_ORIENTATION) || isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD)
                || mGpsEngine.isGpsEnabled() || mAudioEngine.isAudioEnabled();
	}

	public String getSensorType() {
		return mIsLinearAcceleration ? "Linear" : "Raw";
	}

    public GPSEngine getGpsEngine() {
        return mGpsEngine;
    }

    public AudioEngine getAudioEngine() {
        return mAudioEngine;
    }

	public static String getItem(SensorEngine sensorEngine, String ID, String markName, String userName, long timeStamp) {
        GPSEngine gpsEngine = sensorEngine.getGpsEngine();

		StringBuilder sb = new StringBuilder();

		sb.append(ID).append(Constants.CSV_SEPARATOR);
		sb.append(markName).append(Constants.CSV_SEPARATOR);
		sb.append(userName).append(Constants.CSV_SEPARATOR);
		sb.append(timeStamp).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getSensorType()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getAccelerometerX()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getAccelerometerY()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getAccelerometerZ()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getAzimuth()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getPitch()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getRoll()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getMagneticX()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getMagneticY()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getMagneticZ()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getGyroscopeX()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getGyroscopeY()).append(Constants.CSV_SEPARATOR);
		sb.append(sensorEngine.getGyroscopeZ()).append(Constants.CSV_SEPARATOR);
		sb.append(gpsEngine.getLatitude()).append(Constants.CSV_SEPARATOR);
		sb.append(gpsEngine.getLongitude()).append(Constants.CSV_SEPARATOR);
		sb.append(gpsEngine.getAltitude()).append(Constants.CSV_SEPARATOR);
		sb.append(gpsEngine.getAccuracy()).append(Constants.CSV_SEPARATOR);
		sb.append(gpsEngine.getSpeed()).append(Constants.CSV_SEPARATOR);
		sb.append(gpsEngine.getBearing());

        sb.length();
		return sb.toString();
	}

    @Override
    public void onInfoChanged() {
        mAudioHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        });
    }
}
