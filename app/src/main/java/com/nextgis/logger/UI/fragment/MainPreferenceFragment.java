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

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.nextgis.logger.R;
import com.nextgis.logger.UI.IntEditTextPreference;
import com.nextgis.logger.util.FileUtil;
import com.nextgis.logger.util.LoggerConstants;

public class MainPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final int MIN_PERIOD = 1;
    public static final int MAX_PERIOD = 3600;
    public static final int CHOOSE_FILE = 53;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_main);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
            findPreference(LoggerConstants.PREF_USE_API17).setEnabled(false);

        EditTextPreference userName = (EditTextPreference) findPreference(LoggerConstants.PREF_USER_NAME);
        userName.setSummary(userName.getText());
        userName.setOnPreferenceChangeListener(this);

        IntEditTextPreference periodPreference = (IntEditTextPreference) findPreference(LoggerConstants.PREF_PERIOD_SEC);
        periodPreference.setSummary(getString(R.string.settings_period_sum) + periodPreference.getPersistedString("1"));
        periodPreference.setOnPreferenceChangeListener(this);

        Preference catPathPreference = findPreference(LoggerConstants.PREF_CAT_PATH);
        catPathPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("file/*");
                startActivityForResult(intent, CHOOSE_FILE);

                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE)
            FileUtil.copyPreset(getActivity(), data);
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case LoggerConstants.PREF_USER_NAME:
                preference.setSummary((String) newValue);
                return true;
            case LoggerConstants.PREF_PERIOD_SEC:
                int period;

                try {
                    period = Integer.parseInt((String) newValue);
                    boolean max = period > MAX_PERIOD;
                    boolean min = period < MIN_PERIOD;

                    if (max)
                        period = MAX_PERIOD;

                    if (min)
                        period = MIN_PERIOD;

                    ((IntEditTextPreference) preference).persistString(Integer.toString(period));
                    preference.setSummary(getString(R.string.settings_period_sum) + period);

                    if (min || max)
                        throw new IllegalArgumentException();
                } catch (Exception e) {
                    Toast.makeText(preference.getContext(), R.string.settings_period_toast, Toast.LENGTH_LONG).show();
                }

                return false;
        }

        return false;
    }
}
