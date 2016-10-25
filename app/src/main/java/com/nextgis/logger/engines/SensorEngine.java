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

import android.content.ContentValues;
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

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.R;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import java.io.IOException;
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
        mUri = mUri.buildUpon().appendPath(LoggerApplication.TABLE_SENSOR).build();

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
    public void saveData(String markId) {
        saveData(getData(), markId);
    }

    @Override
    public void saveData(ArrayList<InfoItem> items, String markId) {
        NGWVectorLayer sensorLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SENSOR);
        if (sensorLayer != null) {
            ContentValues cv = new ContentValues();
            cv.put(LoggerApplication.FIELD_MARK, markId);

            for (InfoItem item : items) {
                if (item.getColumn(LoggerConstants.HEADER_ACC_X) != null) {
                    cv.put(LoggerConstants.HEADER_ACC_X, item.getColumn(LoggerConstants.HEADER_ACC_X).getValue() + "");
                    cv.put(LoggerConstants.HEADER_ACC_Y, item.getColumn(LoggerConstants.HEADER_ACC_Y).getValue() + "");
                    cv.put(LoggerConstants.HEADER_ACC_Z, item.getColumn(LoggerConstants.HEADER_ACC_Z).getValue() + "");
                }

                if (item.getColumn(LoggerConstants.HEADER_LINEAR_X) != null) {
                    cv.put(LoggerConstants.HEADER_LINEAR_X, item.getColumn(LoggerConstants.HEADER_LINEAR_X).getValue() + "");
                    cv.put(LoggerConstants.HEADER_LINEAR_Y, item.getColumn(LoggerConstants.HEADER_LINEAR_Y).getValue() + "");
                    cv.put(LoggerConstants.HEADER_LINEAR_Z, item.getColumn(LoggerConstants.HEADER_LINEAR_Z).getValue() + "");
                }

                if (item.getColumn(LoggerConstants.HEADER_AZIMUTH) != null) {
                    cv.put(LoggerConstants.HEADER_AZIMUTH, item.getColumn(LoggerConstants.HEADER_AZIMUTH).getValue() + "");
                    cv.put(LoggerConstants.HEADER_PITCH, item.getColumn(LoggerConstants.HEADER_PITCH).getValue() + "");
                    cv.put(LoggerConstants.HEADER_ROLL, item.getColumn(LoggerConstants.HEADER_ROLL).getValue() + "");
                }

                if (item.getColumn(LoggerConstants.HEADER_MAGNETIC_X) != null) {
                    cv.put(LoggerConstants.HEADER_MAGNETIC_X, item.getColumn(LoggerConstants.HEADER_MAGNETIC_X).getValue() + "");
                    cv.put(LoggerConstants.HEADER_MAGNETIC_Y, item.getColumn(LoggerConstants.HEADER_MAGNETIC_Y).getValue() + "");
                    cv.put(LoggerConstants.HEADER_MAGNETIC_Z, item.getColumn(LoggerConstants.HEADER_MAGNETIC_Z).getValue() + "");
                }

                if (item.getColumn(LoggerConstants.HEADER_GYRO_X) != null) {
                    cv.put(LoggerConstants.HEADER_GYRO_X, item.getColumn(LoggerConstants.HEADER_GYRO_X).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GYRO_Y, item.getColumn(LoggerConstants.HEADER_GYRO_Y).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GYRO_Z, item.getColumn(LoggerConstants.HEADER_GYRO_Z).getValue() + "");
                }

                if (item.getColumn(LoggerConstants.HEADER_GPS_LAT) != null) {
                    cv.put(LoggerConstants.HEADER_GPS_LAT, item.getColumn(LoggerConstants.HEADER_GPS_LAT).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_LON, item.getColumn(LoggerConstants.HEADER_GPS_LON).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_ALT, item.getColumn(LoggerConstants.HEADER_GPS_ALT).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_ACC, item.getColumn(LoggerConstants.HEADER_GPS_ACC).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_SP, item.getColumn(LoggerConstants.HEADER_GPS_SP).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_BE, item.getColumn(LoggerConstants.HEADER_GPS_BE).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_SAT, item.getColumn(LoggerConstants.HEADER_GPS_SAT).getValue() + "");
                    cv.put(LoggerConstants.HEADER_GPS_TIME, item.getColumn(LoggerConstants.HEADER_GPS_TIME).getValue() + "");
                }

                if (item.getColumn(LoggerConstants.HEADER_AUDIO) != null)
                    cv.put(LoggerConstants.HEADER_AUDIO, item.getColumn(LoggerConstants.HEADER_AUDIO).getValue() + "");
            }

            try {
                cv.put(Constants.FIELD_GEOM, new GeoPoint(0, 0).toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }

            sensorLayer.insert(mUri, cv);
        }
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
                mAccelerometer.addColumn(LoggerConstants.HEADER_ACC_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_ms2), "%.2f");
                mAccelerometer.addColumn(LoggerConstants.HEADER_ACC_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_ms2), "%.2f");
                mAccelerometer.addColumn(LoggerConstants.HEADER_ACC_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_ms2), "%.2f");
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
                mLinear.addColumn(LoggerConstants.HEADER_LINEAR_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_ms2), "%.2f");
                mLinear.addColumn(LoggerConstants.HEADER_LINEAR_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_ms2), "%.2f");
                mLinear.addColumn(LoggerConstants.HEADER_LINEAR_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_ms2), "%.2f");
                mItems.add(mLinear);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_linear).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(LoggerConstants.PREF_SENSOR_MODE, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_GYROSCOPE)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (sensor != null) {
                mGyroscope = new InfoItem(mContext.getString(R.string.sensor_gyroscope), sensor.getName());
                mGyroscope.addColumn(LoggerConstants.HEADER_GYRO_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_rads), "%.2f");
                mGyroscope.addColumn(LoggerConstants.HEADER_GYRO_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_rads), "%.2f");
                mGyroscope.addColumn(LoggerConstants.HEADER_GYRO_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_rads), "%.2f");
                mItems.add(mGyroscope);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_gyroscope).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(LoggerConstants.PREF_SENSOR_GYRO, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_ORIENTATION)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

            if (sensor != null) {
                mOrientation = new InfoItem(mContext.getString(R.string.sensor_orientation), sensor.getName());
                mOrientation.addColumn(LoggerConstants.HEADER_AZIMUTH, mContext.getString(R.string.info_azimuth), mContext.getString(R.string.info_degree), "%.2f");
                mOrientation.addColumn(LoggerConstants.HEADER_PITCH, mContext.getString(R.string.info_pitch), mContext.getString(R.string.info_degree), "%.2f");
                mOrientation.addColumn(LoggerConstants.HEADER_ROLL, mContext.getString(R.string.info_roll), mContext.getString(R.string.info_degree), "%.2f");
                mItems.add(mOrientation);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_orientation).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(LoggerConstants.PREF_SENSOR_ORIENT, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            if (sensor != null) {
                mMagnetic = new InfoItem(mContext.getString(R.string.sensor_magnetic), sensor.getName());
                mMagnetic.addColumn(LoggerConstants.HEADER_MAGNETIC_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_tesla), "%.2f");
                mMagnetic.addColumn(LoggerConstants.HEADER_MAGNETIC_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_tesla), "%.2f");
                mMagnetic.addColumn(LoggerConstants.HEADER_MAGNETIC_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_tesla), "%.2f");
                mItems.add(mMagnetic);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_magnetic).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(LoggerConstants.PREF_SENSOR_MAG, false).apply();
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
			if ((curTime - mLastUpdateAccelerometer) > LoggerConstants.UPDATE_FREQUENCY) {
				mLastUpdateAccelerometer = curTime;
                mAccelerometer.setValue(LoggerConstants.HEADER_ACC_X, event.values[0]);
                mAccelerometer.setValue(LoggerConstants.HEADER_ACC_Y, event.values[1]);
                mAccelerometer.setValue(LoggerConstants.HEADER_ACC_Z, event.values[2]);
                notifyListeners(mAccelerometer.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			if ((curTime - mLastUpdateLinear) > LoggerConstants.UPDATE_FREQUENCY) {
				mLastUpdateLinear = curTime;
                mLinear.setValue(LoggerConstants.HEADER_LINEAR_X, event.values[0]);
                mLinear.setValue(LoggerConstants.HEADER_LINEAR_Y, event.values[1]);
                mLinear.setValue(LoggerConstants.HEADER_LINEAR_Z, event.values[2]);
                notifyListeners(mLinear.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if ((curTime - mLastUpdateOrient) > LoggerConstants.UPDATE_FREQUENCY) {
				mLastUpdateOrient = curTime;
                mOrientation.setValue(LoggerConstants.HEADER_AZIMUTH, event.values[0]);
                mOrientation.setValue(LoggerConstants.HEADER_PITCH, event.values[1]);
                mOrientation.setValue(LoggerConstants.HEADER_ROLL, event.values[2]);
                notifyListeners(mOrientation.getTitle());
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if ((curTime - mLastUpdateMag) > LoggerConstants.UPDATE_FREQUENCY) {
				mLastUpdateMag = curTime;
                mMagnetic.setValue(LoggerConstants.HEADER_MAGNETIC_X, event.values[0]);
                mMagnetic.setValue(LoggerConstants.HEADER_MAGNETIC_Y, event.values[1]);
                mMagnetic.setValue(LoggerConstants.HEADER_MAGNETIC_Z, event.values[2]);
                notifyListeners(mMagnetic.getTitle());
			}
		}
		
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if ((curTime - mLastUpdateGyro) > LoggerConstants.UPDATE_FREQUENCY) {
				mLastUpdateGyro = curTime;
                mGyroscope.setValue(LoggerConstants.HEADER_GYRO_X, event.values[0]);
                mGyroscope.setValue(LoggerConstants.HEADER_GYRO_Y, event.values[1]);
                mGyroscope.setValue(LoggerConstants.HEADER_GYRO_Z, event.values[2]);
                notifyListeners(mGyroscope.getTitle());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public boolean isSensorEnabled(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return mPreferences.getBoolean(LoggerConstants.PREF_SENSOR_STATE, false);
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return mPreferences.getBoolean(LoggerConstants.PREF_SENSOR_MODE, false);
            case Sensor.TYPE_GYROSCOPE:
                return mPreferences.getBoolean(LoggerConstants.PREF_SENSOR_GYRO, false);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return mPreferences.getBoolean(LoggerConstants.PREF_SENSOR_MAG, false);
            case Sensor.TYPE_ORIENTATION:
                return mPreferences.getBoolean(LoggerConstants.PREF_SENSOR_ORIENT, false);
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
