/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015-2016 NextGIS
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

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.WindowManager;

import com.nextgis.logger.R;
import com.nextgis.logger.UI.BindActivity;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.util.LoggerConstants;

public class InfoActivity extends BindActivity implements ViewPager.OnPageChangeListener {
    private static final String PREF_LAST_VISITED = "last_tab";

    private ItemPagerAdapter mItemAdapter;
    private ViewPager mVpScreens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHasFAB = false;
        setContentView(R.layout.info_activity);

        mItemAdapter = new ItemPagerAdapter(getSupportFragmentManager());
        mVpScreens = (ViewPager) findViewById(R.id.vp_tabs);
        mVpScreens.setAdapter(mItemAdapter);
        mVpScreens.addOnPageChangeListener(this);
        mVpScreens.setCurrentItem(mPreferences.getInt(PREF_LAST_VISITED, 0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_ngw).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mPreferences.getBoolean(LoggerConstants.PREF_KEEP_SCREEN, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPreferences.edit().putInt(PREF_LAST_VISITED, mVpScreens.getCurrentItem()).apply();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        super.onServiceConnected(componentName, iBinder);
        onPageSelected(mVpScreens.getCurrentItem());
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setEngine(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void setEngine(int position) {
        InfoFragment fragment = (InfoFragment) mItemAdapter.getFragment(position);
        BaseEngine engine = null;
        switch (position) {
            case 0:
                engine = mCellEngine;
                break;
            case 1:
                engine = mSensorEngine;
                break;
            case 2:
                if (fragment != null && mArduinoEngine != null) {
                    InfoExternalsFragment externalFragment = (InfoExternalsFragment) fragment;
                    mArduinoEngine.addConnectionListener(externalFragment);
                }

                engine = mArduinoEngine;
                break;
        }

        if (fragment != null)
            fragment.setEngine(engine);
    }

    public class ItemPagerAdapter extends FragmentStatePagerAdapter {
        private Fragment[] mFragments = new Fragment[3];

        ItemPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    mFragments[0] = new InfoCellFragment();
                    return mFragments[0];
                case 1:
                    mFragments[1] = new InfoSensorsFragment();
                    return mFragments[1];
                case 2:
                    mFragments[2] = new InfoExternalsFragment();
                    return mFragments[2];
            }

            return new Fragment();
        }

        @Override
        public int getCount() {
            return 3;
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
            }

            return "";
        }

        Fragment getFragment(int position) {
            return mFragments[position];
        }
    }
}
