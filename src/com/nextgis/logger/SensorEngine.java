package com.nextgis.logger;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SensorEngine implements SensorEventListener {
	private float x, y, z, gyroX, gyroY, gyroZ, magnetic, azimuth, pitch, roll;
	private long lastUpdateAccel, lastUpdateGyro, lastUpdateMag, lastUpdateOrient;
	private int sensorAccelerationType;

	private boolean linearAcceleration;

	private final int updateFrequency = 100; // in ms

	private SharedPreferences prefs;

	private SensorManager sm;
	private Sensor sAccelerometer, sGyroscope, sOrientation, sMagnetic;

	public SensorEngine(Context ctx) {
		sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		onResume(ctx);
	}

	protected void onPause() {
		sm.unregisterListener(this);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	protected void onResume(Context ctx) {
		boolean noSensor = false;
		ArrayList<String> noSensors = new ArrayList<String>();

		if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER)) {
			linearAcceleration = prefs.getBoolean(C.PREF_SENSOR_MODE, false);

			if (linearAcceleration)
				sensorAccelerationType = Sensor.TYPE_LINEAR_ACCELERATION;
			else
				sensorAccelerationType = Sensor.TYPE_ACCELEROMETER;

			sAccelerometer = sm.getDefaultSensor(sensorAccelerationType);

			if (sAccelerometer != null)
				sm.registerListener(this, sAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			else {
				noSensors.add(linearAcceleration ? ctx.getString(R.string.sensor_linear) : ctx.getString(R.string.sensor_accelerometer));
				noSensor = true;
			}
		}

		if (isSensorEnabled(Sensor.TYPE_GYROSCOPE)) {
			sGyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

			if (sGyroscope != null)
				sm.registerListener(this, sGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
			else {
				noSensors.add(ctx.getString(R.string.sensor_gyroscope));
				noSensor = true;
			}
		}

		if (isSensorEnabled(Sensor.TYPE_ORIENTATION)) {
			sOrientation = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);

			if (sOrientation != null)
				sm.registerListener(this, sOrientation, SensorManager.SENSOR_DELAY_NORMAL);
			else {
				noSensors.add(ctx.getString(R.string.sensor_orientation));
				noSensor = true;
			}
		}

		if (isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD)) {
			sMagnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

			if (sMagnetic != null)
				sm.registerListener(this, sMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
			else {
				noSensors.add(ctx.getString(R.string.sensor_magnetic));
				noSensor = true;
			}
		}

		if (noSensor) {
			StringBuilder info = new StringBuilder();
			info.append(ctx.getString(R.string.sensor_error));

			for (int i = 0; i < noSensors.size(); i++) {
				if (i > 0)
					info.append(", ");

				info.append(noSensors.get(i));
			}

			Toast.makeText(ctx, info.toString(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		long curTime = System.currentTimeMillis();
		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if ((curTime - lastUpdateAccel) > updateFrequency) {
				lastUpdateAccel = curTime;
				x = event.values[0];
				y = event.values[1];
				z = event.values[2];
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if ((curTime - lastUpdateOrient) > updateFrequency) {
				lastUpdateOrient = curTime;
				azimuth = event.values[0];
				pitch = event.values[1];
				roll = event.values[2];
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if ((curTime - lastUpdateMag) > updateFrequency) {
				lastUpdateMag = curTime;
				magnetic = event.values[0];
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if ((curTime - lastUpdateGyro) > updateFrequency) {
				lastUpdateGyro = curTime;
				gyroX = event.values[0];
				gyroY = event.values[1];
				gyroZ = event.values[2];
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

	private boolean isSensorEnabled(int sensorType) {
		switch (sensorType) {
		case Sensor.TYPE_ACCELEROMETER:
			return prefs.getBoolean(C.PREF_SENSOR_STATE, true);
		case Sensor.TYPE_GYROSCOPE:
			return prefs.getBoolean(C.PREF_SENSOR_GYRO, true);
		case Sensor.TYPE_MAGNETIC_FIELD:
			return prefs.getBoolean(C.PREF_SENSOR_MAG, true);
		case Sensor.TYPE_ORIENTATION:
			return prefs.getBoolean(C.PREF_SENSOR_ORIENT, true);
		}

		return false;
	}

	public boolean isAnySensorEnabled() {
		return isSensorEnabled(Sensor.TYPE_ACCELEROMETER) || isSensorEnabled(Sensor.TYPE_GYROSCOPE)
				|| isSensorEnabled(Sensor.TYPE_ORIENTATION) || isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD);
	}

	public String getSensorType() {
		return linearAcceleration ? "Linear" : "Raw";
	}

	public static String getItem(SensorEngine sensorEngine, String ID, String markName, String userName, long timeStamp) {
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
		sb.append(sensorEngine.getGyroZ());

		return sb.toString();
	}
}
