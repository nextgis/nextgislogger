/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2017 NextGIS
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

package com.nextgis.logger.ui.fragment;

import android.Manifest;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.nextgis.logger.R;
import com.nextgis.logger.ui.activity.NGIDSettingsActivity;
import com.nextgis.logger.ui.activity.ProgressBarActivity;
import com.nextgis.logger.util.ApkDownloader;

public class NGIDSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey("updater")) {
            if (ProgressBarActivity.hasStoragePermissions(getActivity()))
                ApkDownloader.check(getActivity(), true);
            else {
                String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ProgressBarActivity.requestPermissions(getActivity(), R.string.permissions_title, R.string.permissions_storage,
                                                       ProgressBarActivity.PERMISSION_STORAGE, permissions);
            }

            return;
        }

        addPreferencesFromResource(R.xml.preferences_ngid);
        NGIDSettingsActivity activity = (NGIDSettingsActivity) getActivity();
        activity.fillAccountPreferences(getPreferenceScreen());
    }
}
