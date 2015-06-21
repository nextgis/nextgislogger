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

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.BaseEngine;

public class InfoExternalsFragment extends Fragment {
    private ArduinoEngine mArduinoEngine;
    BaseEngine.EngineListener mArduinoListener;
    private TextView mTvTemperature, mTvHumidity, mTvNoise, mTvCO, mTvC4H10, mTvCH4;
    private Handler mHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.info_external_fragment, container, false);

        mHandler = new Handler();
        mArduinoEngine = ((LoggerApplication) getActivity().getApplication()).getArduinoEngine();
        mArduinoListener = new BaseEngine.EngineListener() {
            @Override
            public void onInfoChanged() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded())
                            fillTextViews();
                    }
                });
            }
        };
        mArduinoEngine.addListener(mArduinoListener);

        mTvTemperature = (TextView) rootView.findViewById(R.id.tv_temperature);
        mTvHumidity = (TextView) rootView.findViewById(R.id.tv_humidity);
        mTvNoise = (TextView) rootView.findViewById(R.id.tv_noise);
        mTvCO = (TextView) rootView.findViewById(R.id.tv_CO);
        mTvC4H10 = (TextView) rootView.findViewById(R.id.tv_C4H10);
        mTvCH4 = (TextView) rootView.findViewById(R.id.tv_CH4);
        fillTextViews();

        return rootView;
    }

    private void fillTextViews() {
        mTvTemperature.setText(mArduinoEngine.getTemperature() + "C");
        mTvHumidity.setText(mArduinoEngine.getHumidity() + "%");
        mTvNoise.setText(mArduinoEngine.getNoise() + "");
        mTvCO.setText(mArduinoEngine.getCO() + "");
        mTvC4H10.setText(mArduinoEngine.getC4H10() +"");
        mTvCH4.setText(mArduinoEngine.getCH4() + "");
    }

    @Override
    public void onResume() {
        super.onResume();
        mArduinoEngine.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mArduinoEngine.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mArduinoEngine.removeListener(mArduinoListener);
    }
}
