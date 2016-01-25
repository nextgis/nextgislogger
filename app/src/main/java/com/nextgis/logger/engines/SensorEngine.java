/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov
 ******************************************************************************
 * Copyright © 2014-2016 NextGIS
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.nextgis.logger.R;
import com.nextgis.logger.util.Constants;

import java.util.ArrayList;

public class SensorEngine extends BaseEngine implements SensorEventListener {
    private static final String BUNDLE_SOURCE = "source";

    private long mLastUpdateAccelerometer, mLastUpdateLinear, mLastUpdateGyro, mLastUpdateMag, mLastUpdateOrient;

    private static SensorManager mSensorManager;
    private static GPSEngine mGPSEngine;
    private static AudioEngine mAudioEngine;
    private EngineListener mListener;
    private Handler mHandler;
    private SharedPreferences mPreferences;

    private InfoItem mAccelerometer, mLinear, mGyroscope, mMagnetic, mOrientation;

    public SensorEngine(Context context) {
		super(context);
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAudioEngine = new AudioEngine(mContext);
        mGPSEngine = new GPSEngine(mContext);
		mPreferences = getPreferences();

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                notifyListeners(msg.getData().getString(BUNDLE_SOURCE));
                return true;
            }
        });
        mListener = new EngineListener() {
            @Override
            public void onInfoChanged(String source) {
                Bundle data = new Bundle();
                data.putString(BUNDLE_SOURCE, source);
                Message msg = new Message();
                msg.setData(data);
                mHandler.sendMessage(msg);
            }
        };
	}

    @Override
	public boolean onPause() {
        mGPSEngine.onPause();
        mAudioEngine.onPause();

        if (super.onPause()) {
            mSensorManager.unregisterListener(this);
            mGPSEngine.removeListener(mListener);
            mAudioEngine.removeListener(mListener);
            return true;
        }

        return false;
    }

    @Override
	@SuppressWarnings("deprecation")
	public boolean onResume() {
        mGPSEngine.onResume();
        mAudioEngine.onResume();

        mItems.clear();

        if (super.onResume()) {
            loadHeader();
            mGPSEngine.addListener(mListener);
            mAudioEngine.addListener(mListener);
            return true;
        }

        return false;
    }

	@Override
	protected void loadHeader() {
        mAccelerometer = mLinear = mGyroscope = mMagnetic = mOrientation = null;
        boolean noSensor = false;
        ArrayList<String> noSensors = new ArrayList<>();
        Sensor sensor;

        if (mGPSEngine.isEngineEnabled())
            mItems.add(mGPSEngine.getData().get(0));

        if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (sensor != null) {
                mAccelerometer = new InfoItem(mContext.getString(R.string.sensor_accelerometer), sensor.getName());
                mAccelerometer.addColumn(Constants.HEADER_ACC_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_ms2), "%.2f");
                mAccelerometer.addColumn(Constants.HEADER_ACC_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_ms2), "%.2f");
                mAccelerometer.addColumn(Constants.HEADER_ACC_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_ms2), "%.2f");
                mItems.add(mAccelerometer);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_accelerometer).toLowerCase());
                noSensor = true;
            }
        }

        if (isSensorEnabled(Sensor.TYPE_LINEAR_ACCELERATION)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

            if (sensor != null) {
                mLinear = new InfoItem(mContext.getString(R.string.sensor_linear), sensor.getName());
                mLinear.addColumn(Constants.HEADER_LINEAR_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_ms2), "%.2f");
                mLinear.addColumn(Constants.HEADER_LINEAR_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_ms2), "%.2f");
                mLinear.addColumn(Constants.HEADER_LINEAR_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_ms2), "%.2f");
                mItems.add(mLinear);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_linear).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(Constants.PREF_SENSOR_MODE, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_GYROSCOPE)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (sensor != null) {
                mGyroscope = new InfoItem(mContext.getString(R.string.sensor_gyroscope), sensor.getName());
                mGyroscope.addColumn(Constants.HEADER_GYRO_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_rads), "%.2f");
                mGyroscope.addColumn(Constants.HEADER_GYRO_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_rads), "%.2f");
                mGyroscope.addColumn(Constants.HEADER_GYRO_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_rads), "%.2f");
                mItems.add(mGyroscope);
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
                mOrientation = new InfoItem(mContext.getString(R.string.sensor_orientation), sensor.getName());
                mOrientation.addColumn(Constants.HEADER_AZIMUTH, mContext.getString(R.string.info_azimuth), mContext.getString(R.string.info_degree), "%.2f");
                mOrientation.addColumn(Constants.HEADER_PITCH, mContext.getString(R.string.info_pitch), mContext.getString(R.string.info_degree), "%.2f");
                mOrientation.addColumn(Constants.HEADER_ROLL, mContext.getString(R.string.info_roll), mContext.getString(R.string.info_degree), "%.2f");
                mItems.add(mOrientation);
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
                mMagnetic = new InfoItem(mContext.getString(R.string.sensor_magnetic), sensor.getName());
                mMagnetic.addColumn(Constants.HEADER_MAGNETIC_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_tesla), "%.2f");
                mMagnetic.addColumn(Constants.HEADER_MAGNETIC_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_tesla), "%.2f");
                mMagnetic.addColumn(Constants.HEADER_MAGNETIC_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_tesla), "%.2f");
                mItems.add(mMagnetic);
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

        if (mAudioEngine.isEngineEnabled())
            mItems.add(mAudioEngine.getData().get(0));
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		long curTime = System.currentTimeMillis();

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if ((curTime - mLastUpdateAccelerometer) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateAccelerometer = curTime;
                mAccelerometer.setValue(Constants.HEADER_ACC_X, event.values[0]);
                mAccelerometer.setValue(Constants.HEADER_ACC_Y, event.values[1]);
                mAccelerometer.setValue(Constants.HEADER_ACC_Z, event.values[2]);
                notifyListeners(mAccelerometer.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			if ((curTime - mLastUpdateLinear) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateLinear = curTime;
                mLinear.setValue(Constants.HEADER_LINEAR_X, event.values[0]);
                mLinear.setValue(Constants.HEADER_LINEAR_Y, event.values[1]);
                mLinear.setValue(Constants.HEADER_LINEAR_Z, event.values[2]);
                notifyListeners(mLinear.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if ((curTime - mLastUpdateOrient) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateOrient = curTime;
                mOrientation.setValue(Constants.HEADER_AZIMUTH, event.values[0]);
                mOrientation.setValue(Constants.HEADER_PITCH, event.values[1]);
                mOrientation.setValue(Constants.HEADER_ROLL, event.values[2]);
                notifyListeners(mOrientation.getTitle());
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if ((curTime - mLastUpdateMag) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateMag = curTime;
                mMagnetic.setValue(Constants.HEADER_MAGNETIC_X, event.values[0]);
                mMagnetic.setValue(Constants.HEADER_MAGNETIC_Y, event.values[1]);
                mMagnetic.setValue(Constants.HEADER_MAGNETIC_Z, event.values[2]);
                notifyListeners(mMagnetic.getTitle());
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if ((curTime - mLastUpdateGyro) > Constants.UPDATE_FREQUENCY) {
				mLastUpdateGyro = curTime;
                mGyroscope.setValue(Constants.HEADER_GYRO_X, event.values[0]);
                mGyroscope.setValue(Constants.HEADER_GYRO_Y, event.values[1]);
                mGyroscope.setValue(Constants.HEADER_GYRO_Z, event.values[2]);
                notifyListeners(mGyroscope.getTitle());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public boolean isSensorEnabled(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return mPreferences.getBoolean(Constants.PREF_SENSOR_STATE, false);
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return mPreferences.getBoolean(Constants.PREF_SENSOR_MODE, false);
            case Sensor.TYPE_GYROSCOPE:
                return mPreferences.getBoolean(Constants.PREF_SENSOR_GYRO, false);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return mPreferences.getBoolean(Constants.PREF_SENSOR_MAG, false);
            case Sensor.TYPE_ORIENTATION:
                return mPreferences.getBoolean(Constants.PREF_SENSOR_ORIENT, false);
		}

		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isEngineEnabled() {
		return isSensorEnabled(Sensor.TYPE_ACCELEROMETER) || isSensorEnabled(Sensor.TYPE_LINEAR_ACCELERATION) ||
                isSensorEnabled(Sensor.TYPE_GYROSCOPE) || isSensorEnabled(Sensor.TYPE_ORIENTATION) ||
                isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD) || mGPSEngine.isEngineEnabled() || mAudioEngine.isEngineEnabled();
	}

    public GPSEngine getGPSEngine() {
        return mGPSEngine;
    }

    public AudioEngine getAudioEngine() {
        return mAudioEngine;
    }
}
