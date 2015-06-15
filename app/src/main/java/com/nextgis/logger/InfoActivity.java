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

package com.nextgis.logger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.WindowManager;

import com.nextgis.logger.UI.ProgressBarActivity;

public class InfoActivity extends ProgressBarActivity {
    private static final String PREF_LAST_VISITED = "last_tab";

    private SharedPreferences mPreferences;
    ItemPagerAdapter itemAdapter;
    ViewPager vpScreens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHasFAB = false;
        setContentView(R.layout.info_activity);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        itemAdapter = new ItemPagerAdapter(getSupportFragmentManager());
        vpScreens = (ViewPager) findViewById(R.id.vp_tabs);
        vpScreens.setAdapter(itemAdapter);
        vpScreens.setCurrentItem(mPreferences.getInt(PREF_LAST_VISITED, 0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mPreferences.getBoolean(C.PREF_KEEP_SCREEN, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mPreferences.edit().putInt(PREF_LAST_VISITED, vpScreens.getCurrentItem()).apply();
    }

    public class ItemPagerAdapter extends FragmentStatePagerAdapter {
        public ItemPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new InfoCellFragment();
                case 1:
                    return new InfoSensorsFragment();
                case 2:
                    return new InfoExternalsFragment();
                case 3:
                    return new IBeaconsInfoFragment();
            }

            return new Fragment();
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.info_title_network);
                case 1:
                    return getString(R.string.info_title_sensors);
                case 2:
                    return getString(R.string.info_title_external);
                case 3:
                    return getString(R.string.info_title_ibeacon);
            }

            return "";
        }
    }
}
