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
import android.database.Cursor;
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
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import java.io.IOException;
import java.util.ArrayList;

import static com.nextgis.logger.util.LoggerConstants.*;

public class SensorEngine extends BaseEngine implements SensorEventListener {
    private static final String BUNDLE_SOURCE = "source";

    private long mLastUpdateAccelerometer, mLastUpdateLinear, mLastUpdateGyro, mLastUpdateMag, mLastUpdateOrient;

    private SensorManager mSensorManager;
    private GPSEngine mGPSEngine;
    private AudioEngine mAudioEngine;
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
                if (item.getColumn(HEADER_ACC_X) != null) {
                    cv.put(HEADER_ACC_X, item.getColumn(HEADER_ACC_X).getValue() + "");
                    cv.put(HEADER_ACC_Y, item.getColumn(HEADER_ACC_Y).getValue() + "");
                    cv.put(HEADER_ACC_Z, item.getColumn(HEADER_ACC_Z).getValue() + "");
                }

                if (item.getColumn(HEADER_LINEAR_X) != null) {
                    cv.put(HEADER_LINEAR_X, item.getColumn(HEADER_LINEAR_X).getValue() + "");
                    cv.put(HEADER_LINEAR_Y, item.getColumn(HEADER_LINEAR_Y).getValue() + "");
                    cv.put(HEADER_LINEAR_Z, item.getColumn(HEADER_LINEAR_Z).getValue() + "");
                }

                if (item.getColumn(HEADER_AZIMUTH) != null) {
                    cv.put(HEADER_AZIMUTH, item.getColumn(HEADER_AZIMUTH).getValue() + "");
                    cv.put(HEADER_PITCH, item.getColumn(HEADER_PITCH).getValue() + "");
                    cv.put(HEADER_ROLL, item.getColumn(HEADER_ROLL).getValue() + "");
                }

                if (item.getColumn(HEADER_MAGNETIC_X) != null) {
                    cv.put(HEADER_MAGNETIC_X, item.getColumn(HEADER_MAGNETIC_X).getValue() + "");
                    cv.put(HEADER_MAGNETIC_Y, item.getColumn(HEADER_MAGNETIC_Y).getValue() + "");
                    cv.put(HEADER_MAGNETIC_Z, item.getColumn(HEADER_MAGNETIC_Z).getValue() + "");
                }

                if (item.getColumn(HEADER_GYRO_X) != null) {
                    cv.put(HEADER_GYRO_X, item.getColumn(HEADER_GYRO_X).getValue() + "");
                    cv.put(HEADER_GYRO_Y, item.getColumn(HEADER_GYRO_Y).getValue() + "");
                    cv.put(HEADER_GYRO_Z, item.getColumn(HEADER_GYRO_Z).getValue() + "");
                }

                if (item.getColumn(HEADER_GPS_LAT) != null) {
                    cv.put(HEADER_GPS_LAT, item.getColumn(HEADER_GPS_LAT).getValue() + "");
                    cv.put(HEADER_GPS_LON, item.getColumn(HEADER_GPS_LON).getValue() + "");
                    cv.put(HEADER_GPS_ALT, item.getColumn(HEADER_GPS_ALT).getValue() + "");
                    cv.put(HEADER_GPS_ACC, item.getColumn(HEADER_GPS_ACC).getValue() + "");
                    cv.put(HEADER_GPS_SP, item.getColumn(HEADER_GPS_SP).getValue() + "");
                    cv.put(HEADER_GPS_BE, item.getColumn(HEADER_GPS_BE).getValue() + "");
                    cv.put(HEADER_GPS_SAT, item.getColumn(HEADER_GPS_SAT).getValue() + "");
                    cv.put(HEADER_GPS_TIME, item.getColumn(HEADER_GPS_TIME).getValue() + "");
                }

                if (item.getColumn(HEADER_AUDIO) != null)
                    cv.put(HEADER_AUDIO, item.getColumn(HEADER_AUDIO).getValue() + "");
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
            loadEngine();
            mGPSEngine.addListener(mListener);
            mAudioEngine.addListener(mListener);
            return true;
        }

        return false;
    }

	@SuppressWarnings("deprecation")
    @Override
	protected void loadEngine() {
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
                mAccelerometer.addColumn(HEADER_ACC_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_ms2), "%.2f");
                mAccelerometer.addColumn(HEADER_ACC_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_ms2), "%.2f");
                mAccelerometer.addColumn(HEADER_ACC_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_ms2), "%.2f");
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
                mLinear.addColumn(HEADER_LINEAR_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_ms2), "%.2f");
                mLinear.addColumn(HEADER_LINEAR_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_ms2), "%.2f");
                mLinear.addColumn(HEADER_LINEAR_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_ms2), "%.2f");
                mItems.add(mLinear);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_linear).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(PREF_SENSOR_MODE, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_GYROSCOPE)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (sensor != null) {
                mGyroscope = new InfoItem(mContext.getString(R.string.sensor_gyroscope), sensor.getName());
                mGyroscope.addColumn(HEADER_GYRO_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_rads), "%.2f");
                mGyroscope.addColumn(HEADER_GYRO_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_rads), "%.2f");
                mGyroscope.addColumn(HEADER_GYRO_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_rads), "%.2f");
                mItems.add(mGyroscope);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_gyroscope).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(PREF_SENSOR_GYRO, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_ORIENTATION)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

            if (sensor != null) {
                mOrientation = new InfoItem(mContext.getString(R.string.sensor_orientation), sensor.getName());
                mOrientation.addColumn(HEADER_AZIMUTH, mContext.getString(R.string.info_azimuth), mContext.getString(R.string.info_degree), "%.2f");
                mOrientation.addColumn(HEADER_PITCH, mContext.getString(R.string.info_pitch), mContext.getString(R.string.info_degree), "%.2f");
                mOrientation.addColumn(HEADER_ROLL, mContext.getString(R.string.info_roll), mContext.getString(R.string.info_degree), "%.2f");
                mItems.add(mOrientation);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_orientation).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(PREF_SENSOR_ORIENT, false).apply();
            }
        }

        if (isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD)) {
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            if (sensor != null) {
                mMagnetic = new InfoItem(mContext.getString(R.string.sensor_magnetic), sensor.getName());
                mMagnetic.addColumn(HEADER_MAGNETIC_X, mContext.getString(R.string.info_x), mContext.getString(R.string.info_tesla), "%.2f");
                mMagnetic.addColumn(HEADER_MAGNETIC_Y, mContext.getString(R.string.info_y), mContext.getString(R.string.info_tesla), "%.2f");
                mMagnetic.addColumn(HEADER_MAGNETIC_Z, mContext.getString(R.string.info_z), mContext.getString(R.string.info_tesla), "%.2f");
                mItems.add(mMagnetic);
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                noSensors.add(mContext.getString(R.string.sensor_magnetic).toLowerCase());
                noSensor = true;
                mPreferences.edit().putBoolean(PREF_SENSOR_MAG, false).apply();
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
			if ((curTime - mLastUpdateAccelerometer) > UPDATE_FREQUENCY) {
				mLastUpdateAccelerometer = curTime;
                mAccelerometer.setValue(HEADER_ACC_X, event.values[0]);
                mAccelerometer.setValue(HEADER_ACC_Y, event.values[1]);
                mAccelerometer.setValue(HEADER_ACC_Z, event.values[2]);
                notifyListeners(mAccelerometer.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			if ((curTime - mLastUpdateLinear) > UPDATE_FREQUENCY) {
				mLastUpdateLinear = curTime;
                mLinear.setValue(HEADER_LINEAR_X, event.values[0]);
                mLinear.setValue(HEADER_LINEAR_Y, event.values[1]);
                mLinear.setValue(HEADER_LINEAR_Z, event.values[2]);
                notifyListeners(mLinear.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			if ((curTime - mLastUpdateOrient) > UPDATE_FREQUENCY) {
				mLastUpdateOrient = curTime;
                mOrientation.setValue(HEADER_AZIMUTH, event.values[0]);
                mOrientation.setValue(HEADER_PITCH, event.values[1]);
                mOrientation.setValue(HEADER_ROLL, event.values[2]);
                notifyListeners(mOrientation.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if ((curTime - mLastUpdateMag) > UPDATE_FREQUENCY) {
				mLastUpdateMag = curTime;
                mMagnetic.setValue(HEADER_MAGNETIC_X, event.values[0]);
                mMagnetic.setValue(HEADER_MAGNETIC_Y, event.values[1]);
                mMagnetic.setValue(HEADER_MAGNETIC_Z, event.values[2]);
                notifyListeners(mMagnetic.getTitle());
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if ((curTime - mLastUpdateGyro) > UPDATE_FREQUENCY) {
				mLastUpdateGyro = curTime;
                mGyroscope.setValue(HEADER_GYRO_X, event.values[0]);
                mGyroscope.setValue(HEADER_GYRO_Y, event.values[1]);
                mGyroscope.setValue(HEADER_GYRO_Z, event.values[2]);
                notifyListeners(mGyroscope.getTitle());
			}
		}
	}

	@SuppressWarnings("deprecation")
    private boolean isSensorEnabled(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return mPreferences.getBoolean(PREF_SENSOR_STATE, false);
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return mPreferences.getBoolean(PREF_SENSOR_MODE, false);
            case Sensor.TYPE_GYROSCOPE:
                return mPreferences.getBoolean(PREF_SENSOR_GYRO, false);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return mPreferences.getBoolean(PREF_SENSOR_MAG, false);
            case Sensor.TYPE_ORIENTATION:
                return mPreferences.getBoolean(PREF_SENSOR_ORIENT, false);
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

    public static String getHeader() {
        return HEADER_ACC_X + CSV_SEPARATOR + HEADER_ACC_Y + CSV_SEPARATOR + HEADER_ACC_Z + CSV_SEPARATOR +
                HEADER_LINEAR_X + CSV_SEPARATOR + HEADER_LINEAR_Y + CSV_SEPARATOR + HEADER_LINEAR_Z + CSV_SEPARATOR +
                HEADER_AZIMUTH + CSV_SEPARATOR + HEADER_PITCH + CSV_SEPARATOR + HEADER_ROLL + CSV_SEPARATOR +
                HEADER_GYRO_X + CSV_SEPARATOR + HEADER_GYRO_Y + CSV_SEPARATOR + HEADER_GYRO_Z + CSV_SEPARATOR +
                HEADER_MAGNETIC_X + CSV_SEPARATOR + HEADER_MAGNETIC_Y + CSV_SEPARATOR + HEADER_MAGNETIC_Z + CSV_SEPARATOR +
                AudioEngine.getHeader() + CSV_SEPARATOR + GPSEngine.getHeader();
    }

    public static String getDataFromCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(HEADER_ACC_X)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_ACC_Y)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_ACC_Z)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_LINEAR_X)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_LINEAR_Y)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_LINEAR_Z)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_AZIMUTH)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_PITCH)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_ROLL)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_GYRO_X)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_GYRO_Y)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_GYRO_Z)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_MAGNETIC_X)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_MAGNETIC_Y)) + CSV_SEPARATOR +
                cursor.getString(cursor.getColumnIndex(HEADER_MAGNETIC_Z)) + CSV_SEPARATOR +
                AudioEngine.getDataFromCursor(cursor) + CSV_SEPARATOR + GPSEngine.getDataFromCursor(cursor);
    }
}
