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

package com.nextgis.logger.UI.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;

import com.nextgis.logger.PreferencesActivity;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.util.AudioCalibratePreference;
import com.nextgis.logger.util.LoggerConstants;

public class SensorsPreferenceFragment extends PreferenceFragment {
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_sensors);

        final Activity parent = getActivity();
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
            findPreference(LoggerConstants.PREF_GPS).setEnabled(false);
            ((CheckBoxPreference) findPreference(LoggerConstants.PREF_GPS)).setChecked(false);
            findPreference(LoggerConstants.PREF_GPS).setSummary(R.string.settings_sensor_sum);
        }

        SensorManager sm = (SensorManager) parent.getSystemService(Context.SENSOR_SERVICE);
        if (sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null) {
            findPreference(LoggerConstants.PREF_SENSOR_MODE).setEnabled(false);
            findPreference(LoggerConstants.PREF_SENSOR_MODE).setSummary(R.string.settings_sensor_sum);
        }

        if (sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            findPreference(LoggerConstants.PREF_SENSOR_MAG).setEnabled(false);
            ((CheckBoxPreference) findPreference(LoggerConstants.PREF_SENSOR_MAG)).setChecked(false);
            findPreference(LoggerConstants.PREF_SENSOR_MAG).setSummary(R.string.settings_sensor_sum);
        }

        if (sm.getDefaultSensor(Sensor.TYPE_ORIENTATION) == null) {
            findPreference(LoggerConstants.PREF_SENSOR_ORIENT).setEnabled(false);
            ((CheckBoxPreference) findPreference(LoggerConstants.PREF_SENSOR_ORIENT)).setChecked(false);
            findPreference(LoggerConstants.PREF_SENSOR_ORIENT).setSummary(R.string.settings_sensor_sum);
        }

        if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
            findPreference(LoggerConstants.PREF_SENSOR_GYRO).setEnabled(false);
            ((CheckBoxPreference) findPreference(LoggerConstants.PREF_SENSOR_GYRO)).setChecked(false);
            findPreference(LoggerConstants.PREF_SENSOR_GYRO).setSummary(R.string.settings_sensor_sum);
        }

        AudioCalibratePreference audio = (AudioCalibratePreference) findPreference(LoggerConstants.PREF_MIC_DELTA);
        audio.setSummary();

        final Preference selectExternalDevice = findPreference(LoggerConstants.PREF_EXTERNAL_DEVICE);
        selectExternalDevice.setSummary(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LoggerConstants.PREF_EXTERNAL_DEVICE, null));
        selectExternalDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                final ArduinoEngine engine = ((PreferencesActivity) getActivity()).getArduinoEngine();
                if (engine == null)
                    return false;

                if (!engine.isBTEnabled()) {
                    dialog.setTitle(R.string.external_goto_settings);
                    dialog.setMessage(R.string.external_bt_disabled);
                } else {
                    dialog.setTitle(R.string.external_paired);
                    final ArrayAdapter<String> devicesNames = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item);

                    for (BluetoothDevice device : engine.getPairedDevices())
                        devicesNames.add(device.getName() + " (" + device.getAddress() + ")");

                    dialog.setAdapter(devicesNames,
                                      new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                              String name = devicesNames.getItem(which);
                                              PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                                               .putString(LoggerConstants.PREF_EXTERNAL_DEVICE, name).apply();
                                              engine.setDeviceMAC(engine.splitDeviceMAC(name));
                                              engine.setDeviceName(engine.splitDeviceName(name));
                                              selectExternalDevice.setSummary(name);
                                          }
                                      });
                }

                dialog.setNegativeButton(android.R.string.cancel, null);
                dialog.setPositiveButton(R.string.app_settings,
                                         new DialogInterface.OnClickListener() {
                                             @Override
                                             public void onClick(DialogInterface dialog, int which) {
                                                 Intent btSettings = new Intent();
                                                 btSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                                 startActivity(btSettings);
                                             }
                                         });

                dialog.show();
                return true;
            }
        });
    }
}
