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
import android.support.v7.widget.GridLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nextgis.logger.PreferencesActivity;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.InfoColumn;
import com.nextgis.logger.engines.InfoItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class InfoSensorsFragment extends InfoFragment implements View.OnClickListener {
    private ScrollView mSvData;
    private LinearLayout mLayoutError, mLayoutData;

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.info_sensors_fragment, container, false);

        mListener = new BaseEngine.EngineListener() {
            @Override
            public void onInfoChanged(String source) {
                if (isAdded())
                    fillSensorsTextViews(source);
            }
        };

        mLayoutError = (LinearLayout) rootView.findViewById(R.id.ll_error);
        mLayoutData = (LinearLayout) rootView.findViewById(R.id.ll_data);
        mSvData = (ScrollView) rootView.findViewById(R.id.sv_data);
        rootView.findViewById(R.id.btn_settings).setOnClickListener(this);

        return rootView;
    }

    private void setInterface() {
        if (isConnected() && mEngine.isEngineEnabled()) {
            mLayoutError.setVisibility(View.GONE);
            mSvData.setVisibility(View.VISIBLE);
        } else {
            mLayoutError.setVisibility(View.VISIBLE);
            mSvData.setVisibility(View.GONE);
        }
    }

    private void createTextViews() {
        mLayoutData.removeAllViews();

        ArrayList<InfoItem> infoItemArray = getData();
        for (InfoItem item : infoItemArray) {
            LinearLayout itemLayout = (LinearLayout) View.inflate(getActivity(), R.layout.info_item, null);

            if (!TextUtils.isEmpty(item.getTitle())) {
                ((TextView) itemLayout.findViewById(R.id.tv_title)).setText(item.getTitle());
                itemLayout.findViewById(R.id.tv_title).setVisibility(View.VISIBLE);
            }

            if (!TextUtils.isEmpty(item.getDescription())) {
                ((TextView) itemLayout.findViewById(R.id.tv_description)).setText(item.getDescription());
                itemLayout.findViewById(R.id.tv_description).setVisibility(View.VISIBLE);
            }

            GridLayout parent = (GridLayout) itemLayout.findViewById(R.id.gl_values);
            GridLayout.LayoutParams lp;
            int total = item.size();

            for (int i = 0; i < total; i++) {
                TextView data = (TextView) View.inflate(getActivity(), R.layout.item_value, null);
                int row = (int) Math.floor(i / 3f);
                int tail = total - row * 3 - 3;
                int size = 1;
                if (tail < 0)
                    size = Math.abs(tail) * 3;

                lp = new GridLayout.LayoutParams(GridLayout.spec(row, GridLayout.FILL, 1f), GridLayout.spec(i % 3 * 2, size, GridLayout.FILL, 1f));
                data.setLayoutParams(lp);
                InfoColumn column = item.getColumns().get(i);
                setValue(data, column);
                parent.addView(data);
            }

            mLayoutData.addView(itemLayout);
        }
    }

    private void fillSensorsTextViews(String source) {
        ArrayList<InfoItem> infoItemArray = getData();
        for (int i = 0; i < mLayoutData.getChildCount(); i++) {
            InfoItem item = infoItemArray.get(i);

            if (!item.getTitle().equals(source))
                continue;

            LinearLayout layout = (LinearLayout) mLayoutData.getChildAt(i);
            GridLayout values = (GridLayout) layout.findViewById(R.id.gl_values);

            for (int j = 0; j < item.size(); j++) {
                TextView data = (TextView) values.getChildAt(j);
                InfoColumn column = item.getColumns().get(j);
                setValue(data, column);
            }
        }
    }

    private void setValue(TextView textView, InfoColumn column) {
        String fullName = column.getFullName();
        boolean isTimeOrSpeed = !TextUtils.isEmpty(fullName) &&
                (fullName.equals(getString(R.string.info_speed)) || fullName.equals(getString(R.string.info_time)));
        String value = "";

        if (!TextUtils.isEmpty(column.getFullName()))
            value += column.getFullName() + ":\r\n";

        if (isTimeOrSpeed)
            value += format(column.getValue(), column.getUnit());
        else
            value += column.getValueWithUnit();

        textView.setText(value);
    }

    private String format(Object value, String unit) {
        String formatted = "" + value;

        if (value instanceof Float)
            formatted = String.format(Locale.getDefault(), "%.2f %s", (float) value * 3600 / 1000, unit);
        else if (value instanceof Long) {
            DateFormat simpleDateFormat = DateFormat.getTimeInstance();
            formatted = String.format("%s", simpleDateFormat.format(new Date((long) value)));
        }

        return formatted;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        createTextViews();
        setInterface();
    }

    @Override
    public void onClick(View v) {
        Intent preferencesActivity = new Intent(getActivity(), PreferencesActivity.class);
        startActivity(preferencesActivity);
    }

    @Override
    public void setEngine(BaseEngine engine) {
        super.setEngine(engine);
        onResume();
    }
}
