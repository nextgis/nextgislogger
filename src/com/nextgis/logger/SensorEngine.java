package com.nextgis.logger;

import com.nextgis.logger.R;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SensorEngine implements SensorEventListener {
	private float x, y, z;
	private long lastUpdate = 0;
	private int sensorType;

	private boolean linearAcceleration;

	private final int updateFrequency = 100; // in ms

	SensorManager sm;
	Sensor sa;

	public SensorEngine(Context ctx) {
		sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		onResume(ctx);
	}

	protected void onPause() {
		sm.unregisterListener(this);
	}

	protected void onResume(Context ctx) {
		linearAcceleration = PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(C.PREF_SENSOR_MODE, false);
		
		if (linearAcceleration)
			sensorType = Sensor.TYPE_LINEAR_ACCELERATION;
		else
			sensorType = Sensor.TYPE_ACCELEROMETER;
		
		sa = sm.getDefaultSensor(sensorType);
		
		if (sa == null)
			Toast.makeText(ctx, R.string.sensor_error, Toast.LENGTH_LONG).show();
			
		sm.registerListener(this, sa, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor mySensor = event.sensor;

		if (mySensor.getType() == sensorType) {

			long curTime = System.currentTimeMillis();

			if ((curTime - lastUpdate) > updateFrequency) {
				lastUpdate = curTime;
				x = event.values[0];
				y = event.values[1];
				z = event.values[2];
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
		sb.append(sensorEngine.getZ());
		
		return sb.toString();
	}
}
