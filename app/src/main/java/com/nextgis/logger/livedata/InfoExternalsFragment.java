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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.PreferencesActivity;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.BaseEngine;

public class InfoExternalsFragment extends Fragment implements View.OnClickListener, ArduinoEngine.ConnectionListener {
    private enum EXTERNAL_STATUS {DISABLED, BT_DISABLED, NOT_FOUND, CONNECTING, CONNECTED}

    private ArduinoEngine mArduinoEngine;
    private BaseEngine.EngineListener mArduinoListener;
    private TextView mTvInfo;
    private LinearLayout mLlError, mLlData;
    private ScrollView mSvData;
    private Button mBtnSettings;
    private ProgressBar mPbConnecting;
    private Handler mHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.info_external_fragment, container, false);
        mLlError = (LinearLayout) rootView.findViewById(R.id.ll_error);
        mSvData = (ScrollView) rootView.findViewById(R.id.sv_data);
        mLlData = (LinearLayout) rootView.findViewById(R.id.ll_data);
        mTvInfo = (TextView) rootView.findViewById(R.id.tv_info);
        mBtnSettings = (Button) rootView.findViewById(R.id.btn_settings);
        mPbConnecting = (ProgressBar) rootView.findViewById(R.id.pb_connecting);
        mBtnSettings.setOnClickListener(this);

        mHandler = new Handler();
        mArduinoEngine = LoggerApplication.getApplication().getArduinoEngine();
        mArduinoEngine.addConnectionListener(this);

        return rootView;
    }

    private void createTextViews() {
        for (int i = 0; i < mArduinoEngine.getSensorsCount(); i++) {
            View item = View.inflate(getActivity(), R.layout.info_external_row, null);
            ((TextView) item.findViewById(R.id.tv_title)).setText(mArduinoEngine.getData().get(i).getTitle());
            mLlData.addView(item);
        }
    }

    private void fillTextViews() {
        if (mLlData.getChildCount() > 0)
            for (int i = 0; i < mArduinoEngine.getSensorsCount(); i++)
                ((TextView) mLlData.getChildAt(i).findViewById(R.id.tv_data)).setText(mArduinoEngine.getData().get(i).getColumns().get(0).getValueWithUnit());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mArduinoEngine.isEngineEnabled()) {
            if (mArduinoEngine.isBTEnabled())
                connect();
            else
                setInterface(EXTERNAL_STATUS.BT_DISABLED);
        } else
            setInterface(EXTERNAL_STATUS.DISABLED);
    }

    @Override
    public void onPause() {
        super.onPause();

        mArduinoEngine.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mArduinoListener != null)
            mArduinoEngine.removeListener(mArduinoListener);

        mArduinoEngine.removeConnectionListener(this);
        mArduinoEngine.onPause();
    }

    private void connect() {
        setInterface(EXTERNAL_STATUS.CONNECTING);

        if (mArduinoListener == null)
            initializeArduino();

        if (mArduinoEngine.isDeviceAvailable() && mArduinoEngine.isConnected())
            setInterface(EXTERNAL_STATUS.CONNECTED);
        else
            mArduinoEngine.onResume();
    }

    private void initializeArduino() {
        mArduinoListener = new BaseEngine.EngineListener() {
            @Override
            public void onInfoChanged(String source) {
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
    }

    @Override
    public void onTimeoutOrFailure() {
        mArduinoEngine.onPause();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdded())
                    setInterface(EXTERNAL_STATUS.NOT_FOUND);
            }
        });
    }

    @Override
    public void onConnected() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdded())
                    setInterface(EXTERNAL_STATUS.CONNECTED);
            }
        });
    }

    @Override
    public void onConnectionLost() {
        mArduinoEngine.onPause();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdded())
                    setInterface(EXTERNAL_STATUS.NOT_FOUND);
            }
        });
    }

    private void setInterface(EXTERNAL_STATUS status) {
        switch (status) {
            case DISABLED:
                mTvInfo.setText(getString(R.string.external_disabled));
                mBtnSettings.setText(R.string.external_goto_settings);
                showButton();
                break;
            case BT_DISABLED:
                mTvInfo.setText(getString(R.string.external_bt_disabled));
                mBtnSettings.setText(R.string.external_goto_settings);
                showButton();
                break;
            case NOT_FOUND:
                mTvInfo.setText(String.format(getString(R.string.external_not_found), mArduinoEngine.getDeviceName()));
                mBtnSettings.setText(R.string.external_retry);
                showButton();
                break;
            case CONNECTING:
                mTvInfo.setText(String.format(getString(R.string.external_connecting), mArduinoEngine.getDeviceName()));
                mPbConnecting.setVisibility(View.VISIBLE);
                mBtnSettings.setVisibility(View.GONE);
                mLlError.setVisibility(View.VISIBLE);
                mSvData.setVisibility(View.GONE);
                break;
            case CONNECTED:
                mLlError.setVisibility(View.GONE);
                mSvData.setVisibility(View.VISIBLE);
                createTextViews();
                fillTextViews();
                break;
        }
    }

    private void showButton() {
        mPbConnecting.setVisibility(View.GONE);
        mBtnSettings.setVisibility(View.VISIBLE);
        mLlError.setVisibility(View.VISIBLE);
        mSvData.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_settings:
                Intent preferencesActivity = new Intent();

                if (!mArduinoEngine.isEngineEnabled())
                    preferencesActivity.setClass(getActivity(), PreferencesActivity.class);
                else if (!mArduinoEngine.isBTEnabled())
                    preferencesActivity.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                else {
                    connect();
                    break;
                }

                startActivity(preferencesActivity);
                break;
        }
    }
}
