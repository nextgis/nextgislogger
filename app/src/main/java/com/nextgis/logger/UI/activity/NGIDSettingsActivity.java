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

package com.nextgis.logger.ui.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.MenuItem;

import com.nextgis.logger.R;
import com.nextgis.logger.ui.fragment.NGIDSettingsFragment;
import com.nextgis.logger.util.NGIDUtils;

import java.util.List;

public class NGIDSettingsActivity extends PreferencesActivity {
    protected static final String ACCOUNT_ACTION = "com.nextgis.maplibui.ACCOUNT";
    SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        invalidateHeaders();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        boolean isLoggedIn = !TextUtils.isEmpty(mPreferences.getString(NGIDUtils.PREF_ACCESS_TOKEN, ""));
        Header header = new Header();
        if (isLoggedIn) {
            header.title = getString(R.string.ngid_my);
            header.fragment = NGIDSettingsFragment.class.getName();
        } else {
            header.title = getString(R.string.login);
            header.intent = new Intent(this, NGIDLoginActivity.class);
            //            header.fragment = NGIDLoginFragment.class.getName();
        }

        target.add(header);

        //add "New account" header
        header = new Header();
        header.title = getString(R.string.ngid_account_new);
        header.intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://my.nextgis.com"));
        target.add(header);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    public void fillAccountPreferences(PreferenceScreen screen) {
        String notDefined = getString(R.string.not_set);
        String value = mPreferences.getString(NGIDUtils.PREF_USERNAME, null);
        screen.findPreference(NGIDUtils.PREF_USERNAME).setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        value = mPreferences.getString(NGIDUtils.PREF_EMAIL, null);
        screen.findPreference(NGIDUtils.PREF_EMAIL).setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        value = mPreferences.getString(NGIDUtils.PREF_FIRST_NAME, null);
        screen.findPreference(NGIDUtils.PREF_FIRST_NAME).setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        value = mPreferences.getString(NGIDUtils.PREF_LAST_NAME, null);
        screen.findPreference(NGIDUtils.PREF_LAST_NAME).setSummary(TextUtils.isEmpty(value) ? notDefined : value);
        screen.findPreference("sign_out").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mPreferences.edit().remove(NGIDUtils.PREF_USERNAME).remove(NGIDUtils.PREF_EMAIL).remove(NGIDUtils.PREF_FIRST_NAME)
                            .remove(NGIDUtils.PREF_LAST_NAME).remove(NGIDUtils.PREF_ACCESS_TOKEN).remove(NGIDUtils.PREF_REFRESH_TOKEN).apply();

                if (onIsHidingHeaders())
                    finish();
                else
                    invalidateHeaders();

                return false;
            }
        });
    }
}
