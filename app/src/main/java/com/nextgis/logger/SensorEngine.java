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
package com.nextgis.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SensorEngine implements SensorEventListener {
	private float x, y, z, gyroX, gyroY, gyroZ, magnetic, azimuth, pitch, roll;
	private long lastUpdateAccel, lastUpdateGyro, lastUpdateMag, lastUpdateOrient;
	private int sensorAccelerationType;
    private String mAccelerometerName, mMagneticName, mOrientName, mGyroName;

	private boolean linearAcceleration;

	private final int updateFrequency = 100; // in ms

    private Context mContext;
	private SharedPreferences prefs;

	private SensorManager sm;
	private Sensor sAccelerometer, sGyroscope, sOrientation, sMagnetic;
	private GPSEngine gpsEngine;

    private List<SensorInfoListener> mSensorListeners;

    interface SensorInfoListener {
        public void onSensorInfoChanged();
    }

	public SensorEngine(Context ctx) {
        mContext = ctx;
        mSensorListeners = new ArrayList<>();
		sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        gpsEngine = new GPSEngine(ctx);
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
//		onResume();
	}

    public void addSensorListener(SensorInfoListener listener) {
        mSensorListeners.add(listener);
    }

    private void notifySensorListeners() {
        for (SensorInfoListener listener : mSensorListeners)
            listener.onSensorInfoChanged();
    }

	protected void onPause() {
		sm.unregisterListener(this);
        gpsEngine.onPause();
	}

	@SuppressWarnings("deprecation")
	protected void onResume() {
		boolean noSensor = false;
		ArrayList<String> noSensors = new ArrayList<>();

		if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER)) {
			linearAcceleration = prefs.getBoolean(C.PREF_SENSOR_MODE, false);

			if (linearAcceleration)
				sensorAccelerationType = Sensor.TYPE_LINEAR_ACCELERATION;
			else
				sensorAccelerationType = Sensor.TYPE_ACCELEROMETER;

			sAccelerometer = sm.getDefaultSensor(sensorAccelerationType);

			if (sAccelerometer != null) {
                mAccelerometerName = sAccelerometer.getName();
                sm.registerListener(this, sAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(linearAcceleration ?
                        mContext.getString(R.string.sensor_linear).toLowerCase() :
                        mContext.getString(R.string.sensor_accelerometer).toLowerCase());
				noSensor = true;
			}
		}

		if (isSensorEnabled(Sensor.TYPE_GYROSCOPE)) {
			sGyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

			if (sGyroscope != null) {
                mGyroName = sGyroscope.getName();
                sm.registerListener(this, sGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mContext.getString(R.string.sensor_gyroscope).toLowerCase());
				noSensor = true;
                prefs.edit().putBoolean(C.PREF_SENSOR_GYRO, false).apply();
			}
		}

		if (isSensorEnabled(Sensor.TYPE_ORIENTATION)) {
			sOrientation = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);

			if (sOrientation != null) {
                mOrientName = sOrientation.getName();
                sm.registerListener(this, sOrientation, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mContext.getString(R.string.sensor_orientation).toLowerCase());
				noSensor = true;
                prefs.edit().putBoolean(C.PREF_SENSOR_ORIENT, false).apply();
			}
		}

		if (isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD)) {
			sMagnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

			if (sMagnetic != null) {
                mMagneticName = sMagnetic.getName();
                sm.registerListener(this, sMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
				noSensors.add(mContext.getString(R.string.sensor_magnetic).toLowerCase());
				noSensor = true;
                prefs.edit().putBoolean(C.PREF_SENSOR_MAG, false).apply();
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

        gpsEngine.onResume();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		long curTime = System.currentTimeMillis();
		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if ((curTime - lastUpdateAccel) > updateFrequency) {
				lastUpdateAccel = curTime;
				x = event.values[0];
				y = event.values[1];
				z = event.values[2];
                notifySensorListeners();
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if ((curTime - lastUpdateOrient) > updateFrequency) {
				lastUpdateOrient = curTime;
				azimuth = event.values[0];
				pitch = event.values[1];
				roll = event.values[2];
                notifySensorListeners();
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if ((curTime - lastUpdateMag) > updateFrequency) {
				lastUpdateMag = curTime;
				magnetic = event.values[0];
                notifySensorListeners();
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if ((curTime - lastUpdateGyro) > updateFrequency) {
				lastUpdateGyro = curTime;
				gyroX = event.values[0];
				gyroY = event.values[1];
				gyroZ = event.values[2];
                notifySensorListeners();
			}
		}
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}
	
	public float getGyroX() {
		return gyroX;
	}
	
	public float getGyroY() {
		return gyroY;
	}
	
	public float getGyroZ() {
		return gyroZ;
	}
	
	public float getMagnetic() {
		return magnetic;
	}
	
	public float getAzimuth() {
		return azimuth;
	}
	
	public float getPitch() {
		return pitch;
	}
	
	public float getRoll() {
		return roll;
	}

    public String getAccelerometerName() {
        return mAccelerometerName;
    }

    public String getMagneticName() {
        return mMagneticName;
    }

    public String getOrientName() {
        return mOrientName;
    }

    public String getGyroName() {
        return mGyroName;
    }

	@SuppressWarnings("deprecation")
	public boolean isSensorEnabled(int sensorType) {
		switch (sensorType) {
		case Sensor.TYPE_ACCELEROMETER:
			return prefs.getBoolean(C.PREF_SENSOR_STATE, false);
		case Sensor.TYPE_GYROSCOPE:
			return prefs.getBoolean(C.PREF_SENSOR_GYRO, false);
		case Sensor.TYPE_MAGNETIC_FIELD:
			return prefs.getBoolean(C.PREF_SENSOR_MAG, false);
		case Sensor.TYPE_ORIENTATION:
			return prefs.getBoolean(C.PREF_SENSOR_ORIENT, false);
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	public boolean isAnySensorEnabled() {
		return isSensorEnabled(Sensor.TYPE_ACCELEROMETER) || isSensorEnabled(Sensor.TYPE_GYROSCOPE)
				|| isSensorEnabled(Sensor.TYPE_ORIENTATION) || isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD) || gpsEngine.isGpsEnabled();
	}

	public String getSensorType() {
		return linearAcceleration ? "Linear" : "Raw";
	}

    public GPSEngine getGpsEngine() {
        return gpsEngine;
    }

	public static String getItem(SensorEngine sensorEngine, String ID, String markName, String userName, long timeStamp) {
        GPSEngine gpsEngine = sensorEngine.getGpsEngine();

		StringBuilder sb = new StringBuilder();

		sb.append(ID).append(C.CSV_SEPARATOR);
		sb.append(markName).append(C.CSV_SEPARATOR);
		sb.append(userName).append(C.CSV_SEPARATOR);
		sb.append(timeStamp).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getSensorType()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getX()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getY()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getZ()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getAzimuth()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getPitch()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getRoll()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getMagnetic()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getGyroX()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getGyroY()).append(C.CSV_SEPARATOR);
		sb.append(sensorEngine.getGyroZ()).append(C.CSV_SEPARATOR);
		sb.append(gpsEngine.getLatitude()).append(C.CSV_SEPARATOR);
		sb.append(gpsEngine.getLongitude()).append(C.CSV_SEPARATOR);
		sb.append(gpsEngine.getAltitude()).append(C.CSV_SEPARATOR);
		sb.append(gpsEngine.getAccuracy()).append(C.CSV_SEPARATOR);
		sb.append(gpsEngine.getSpeed()).append(C.CSV_SEPARATOR);
		sb.append(gpsEngine.getBearing());

		return sb.toString();
	}
}
