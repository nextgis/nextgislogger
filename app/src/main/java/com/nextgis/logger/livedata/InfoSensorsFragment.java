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

package com.nextgis.logger.livedata;

import android.hardware.Sensor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nextgis.logger.R;
import com.nextgis.logger.engines.AudioEngine;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.GPSEngine;
import com.nextgis.logger.engines.SensorEngine;

import java.text.DateFormat;
import java.util.Date;

public class InfoSensorsFragment extends Fragment {
    private SensorEngine mSensorEngine;
    private static GPSEngine mGPSEngine;
    private static AudioEngine mAudioEngine;
    private BaseEngine.EngineListener mSensorListener;
    private BaseEngine.EngineListener mGPSListener;

    private LinearLayout llGPS, llGPSInfo, llAccelerometer, llOrient, llGyro, llMagnetic, llAudio;
    private TextView tvAccelerometerTitle, tvOrientTitle, tvMagneticTitle, tvGyroTitle;
    private TextView tvGPSNoFix, tvGPSLat, tvGPSLon, tvGPSEle;
    private TextView tvGPSAcc, tvGPSSpeed, tvGPSSat, tvGPSTime;
    private TextView tvAccelerometerX, tvAccelerometerY, tvAccelerometerZ;
    private TextView tvOrientAzimuth, tvOrientPitch, tvOrientRoll;
    private TextView tvGyroX, tvGyroY, tvGyroZ;
    private TextView tvMagneticX, tvMagneticY, tvMagneticZ;
    private TextView tvAudio;

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.info_sensors_fragment, container, false);

        mSensorListener = new BaseEngine.EngineListener() {
            @Override
            public void onInfoChanged() {
                if (isAdded())
                    fillSensorsTextViews();
            }
        };
        mSensorEngine = new SensorEngine(getActivity());
        mSensorEngine.addListener(mSensorListener);

        mGPSListener = new BaseEngine.EngineListener() {
            @Override
            public void onInfoChanged() {
                if (isAdded())
                    fillGPSTextViews();
            }
        };
        mGPSEngine = mSensorEngine.getGPSEngine();
        mGPSEngine.addListener(mGPSListener);

        mAudioEngine = mSensorEngine.getAudioEngine();

        llGPS = (LinearLayout) rootView.findViewById(R.id.ll_gps);
        llGPSInfo = (LinearLayout) rootView.findViewById(R.id.ll_gps_info);
        tvGPSNoFix = (TextView) rootView.findViewById(R.id.tv_gps_no_fix);
        llAccelerometer = (LinearLayout) rootView.findViewById(R.id.ll_accelerometer);
        llOrient = (LinearLayout) rootView.findViewById(R.id.ll_orientation);
        llGyro = (LinearLayout) rootView.findViewById(R.id.ll_gyroscope);
        llMagnetic = (LinearLayout) rootView.findViewById(R.id.ll_magnetometer);
        llAudio = (LinearLayout) rootView.findViewById(R.id.ll_audio);

        tvAccelerometerTitle = (TextView) rootView.findViewById(R.id.tv_accelerometer_title);
        tvOrientTitle = (TextView) rootView.findViewById(R.id.tv_orient_title);
        tvGyroTitle = (TextView) rootView.findViewById(R.id.tv_gyroscope_title);
        tvMagneticTitle = (TextView) rootView.findViewById(R.id.tv_magnetometer_title);

        tvGPSLat = (TextView) rootView.findViewById(R.id.tv_gps_lat);
        tvGPSLon = (TextView) rootView.findViewById(R.id.tv_gps_lon);
        tvGPSEle = (TextView) rootView.findViewById(R.id.tv_gps_ele);
        tvGPSAcc = (TextView) rootView.findViewById(R.id.tv_gps_acc);
        tvGPSSpeed = (TextView) rootView.findViewById(R.id.tv_gps_speed);
        tvGPSSat = (TextView) rootView.findViewById(R.id.tv_gps_sat);
        tvGPSTime = (TextView) rootView.findViewById(R.id.tv_gps_time);
        tvAccelerometerX = (TextView) rootView.findViewById(R.id.tv_accelerometer_x);
        tvAccelerometerY = (TextView) rootView.findViewById(R.id.tv_accelerometer_y);
        tvAccelerometerZ = (TextView) rootView.findViewById(R.id.tv_accelerometer_z);
        tvOrientAzimuth = (TextView) rootView.findViewById(R.id.tv_orient_azimuth);
        tvOrientPitch = (TextView) rootView.findViewById(R.id.tv_orient_pitch);
        tvOrientRoll = (TextView) rootView.findViewById(R.id.tv_orient_roll);
        tvGyroX = (TextView) rootView.findViewById(R.id.tv_gyroscope_x);
        tvGyroY = (TextView) rootView.findViewById(R.id.tv_gyroscope_y);
        tvGyroZ = (TextView) rootView.findViewById(R.id.tv_gyroscope_z);
        tvMagneticX = (TextView) rootView.findViewById(R.id.tv_magnetic_x);
        tvMagneticY = (TextView) rootView.findViewById(R.id.tv_magnetic_y);
        tvMagneticZ = (TextView) rootView.findViewById(R.id.tv_magnetic_z);
        tvAudio = (TextView) rootView.findViewById(R.id.tv_audio);

        fillSensorsTextViews();
        fillGPSTextViews();

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSensorEngine.removeListener(mSensorListener);
        mGPSEngine.removeListener(mGPSListener);
    }

    @SuppressWarnings("deprecation")
    private void fillSensorsTextViews() {
        tvAccelerometerX.setText(format(Sensor.TYPE_ACCELEROMETER, mSensorEngine.getAccelerometerX()));
        tvAccelerometerY.setText(format(Sensor.TYPE_ACCELEROMETER, mSensorEngine.getAccelerometerY()));
        tvAccelerometerZ.setText(format(Sensor.TYPE_ACCELEROMETER, mSensorEngine.getAccelerometerZ()));
        tvOrientAzimuth.setText(format(Sensor.TYPE_ORIENTATION, mSensorEngine.getAzimuth()));
        tvOrientPitch.setText(format(Sensor.TYPE_ORIENTATION, mSensorEngine.getPitch()));
        tvOrientRoll.setText(format(Sensor.TYPE_ORIENTATION, mSensorEngine.getRoll()));
        tvGyroX.setText(format(Sensor.TYPE_GYROSCOPE, mSensorEngine.getGyroscopeX()));
        tvGyroY.setText(format(Sensor.TYPE_GYROSCOPE, mSensorEngine.getGyroscopeY()));
        tvGyroZ.setText(format(Sensor.TYPE_GYROSCOPE, mSensorEngine.getGyroscopeZ()));
        tvMagneticX.setText(format(Sensor.TYPE_MAGNETIC_FIELD, mSensorEngine.getMagneticX()));
        tvMagneticY.setText(format(Sensor.TYPE_MAGNETIC_FIELD, mSensorEngine.getMagneticY()));
        tvMagneticZ.setText(format(Sensor.TYPE_MAGNETIC_FIELD, mSensorEngine.getMagneticZ()));
        tvAudio.setText(format(-10, mAudioEngine.getDb())); // TODO enum
    }

    private void fillGPSTextViews() {
        if (!mGPSEngine.hasLocation()) {
            llGPSInfo.setVisibility(View.GONE);
            tvGPSNoFix.setVisibility(View.VISIBLE);
        } else {
            llGPSInfo.setVisibility(View.VISIBLE);
            tvGPSNoFix.setVisibility(View.GONE);
            tvGPSLat.setText(format(-4, mGPSEngine.getLatitude()));
            tvGPSLon.setText(format(-4, mGPSEngine.getLongitude()));
            tvGPSEle.setText(format(-2, mGPSEngine.getAltitude()));
            tvGPSAcc.setText(format(-2, mGPSEngine.getAccuracy()));
            tvGPSSpeed.setText(format(-3, mGPSEngine.getSpeed()));
            tvGPSSat.setText("" + mGPSEngine.getSatellites());
            tvGPSTime.setText(format(-5, mGPSEngine.getTime()));
        }
    }

    @SuppressWarnings("deprecation")
    private Spanned format(int type, double arg) {
        String formatted = "" + arg;

        switch (type) {
            case -2:
                formatted = String.format("%.2f %s", arg, getString(R.string.info_meter));
                break;
            case -3:
                formatted = String.format("%.2f %s", arg * 3600 / 1000, getString(R.string.info_kmh));
                break;
            case -4:
                formatted = String.format("%.6f", arg);
                break;
            case -5:
                DateFormat simpleDateFormat = DateFormat.getDateTimeInstance();
                formatted = String.format("%s", simpleDateFormat.format(new Date((long) arg)));
                break;
            case Sensor.TYPE_ACCELEROMETER:
                formatted = String.format("%.2f ", arg) + getString(R.string.info_ms2);
                break;
            case Sensor.TYPE_GYROSCOPE:
                formatted = String.format("%.2f %s", arg, getString(R.string.info_rads));
                break;
            case Sensor.TYPE_ORIENTATION:
                formatted = String.format("%.2f %s", arg, getString(R.string.info_degree));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                formatted = String.format("%.2f %s", arg, getString(R.string.info_tesla));
                break;
            case -10:
                formatted = String.format("%.0f %s", arg, getString(R.string.info_db));
                break;
        }

        return Html.fromHtml(formatted);
    }

    @Override
    public void onPause() {
        super.onPause();

        mSensorEngine.onPause();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        mSensorEngine.onResume();

        if (!mGPSEngine.isGpsEnabled())
            llGPS.setVisibility(View.GONE);
        else
            llGPS.setVisibility(View.VISIBLE);

        if (!mSensorEngine.isSensorEnabled(Sensor.TYPE_ACCELEROMETER))
            llAccelerometer.setVisibility(View.GONE);
        else
            llAccelerometer.setVisibility(View.VISIBLE);

        if (!mSensorEngine.isSensorEnabled(Sensor.TYPE_ORIENTATION))
            llOrient.setVisibility(View.GONE);
        else
            llOrient.setVisibility(View.VISIBLE);

        if (!mSensorEngine.isSensorEnabled(Sensor.TYPE_GYROSCOPE))
            llGyro.setVisibility(View.GONE);
        else
            llGyro.setVisibility(View.VISIBLE);

        if (!mSensorEngine.isSensorEnabled(Sensor.TYPE_MAGNETIC_FIELD))
            llMagnetic.setVisibility(View.GONE);
        else
            llMagnetic.setVisibility(View.VISIBLE);

        if (!mAudioEngine.isAudioEnabled())
            llAudio.setVisibility(View.GONE);
        else
            llAudio.setVisibility(View.VISIBLE);

        tvAccelerometerTitle.setText(mSensorEngine.getAccelerometerName());
        tvOrientTitle.setText(mSensorEngine.getOrientName());
        tvGyroTitle.setText(mSensorEngine.getGyroName());
        tvMagneticTitle.setText(mSensorEngine.getMagneticName());
    }
}
