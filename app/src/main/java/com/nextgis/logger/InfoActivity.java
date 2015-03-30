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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.nextgis.logger.UI.ProgressBarActivity;

public class InfoActivity extends ProgressBarActivity {
    private static final String TITLE_CELL    = "Network";
    private static final String TITLE_SENSORS = "Sensors";

    ItemPagerAdapter itemAdapter;
    ViewPager vpScreens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_activity);
        itemAdapter = new ItemPagerAdapter(getSupportFragmentManager());
        vpScreens = (ViewPager) findViewById(R.id.vp_tabs);
        vpScreens.setAdapter(itemAdapter);
//        vpScreens.setCurrentItem(); // TODO last visited tab

        mFAB.setOnClickListener(this);
        mFAB.setImageResource(R.drawable.ic_undo_white_24dp);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                finish();
                break;
            default:
                super.onClick(v);
                break;
        }
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
            }

            return new Fragment();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return TITLE_CELL;
                case 1:
                    return TITLE_SENSORS;
            }

            return "";
        }
    }
}
