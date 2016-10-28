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

package com.nextgis.logger.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.nextgis.logger.PreferencesActivity;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.AudioEngine;
import com.nextgis.logger.engines.BaseEngine;

public class AudioCalibratePreference extends DialogPreference implements View.OnClickListener, BaseEngine.EngineListener {
    private AudioEngine mAudioEngine;
    private TextView mTvDelta, mTvTotal;
    private int mValue;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AudioCalibratePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AudioCalibratePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AudioCalibratePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AudioCalibratePreference(Context context) {
        super(context);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue)
            mValue = getPersistedInt(0);
        else
            mValue = (Integer) defaultValue;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (mAudioEngine != null)
            mAudioEngine.removeListener(this);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        onSetInitialValue(true, 0);

        mTvTotal = (TextView) view.findViewById(R.id.tv_message);
        mTvDelta = (TextView) view.findViewById(R.id.tv_value);
        mTvDelta.setText(Integer.toString(mValue));
        FloatingActionButton mFAB = (FloatingActionButton) view.findViewById(R.id.fab_dec);
        mFAB.setOnClickListener(this);
        mFAB.setColorRipple(UiUtil.darkerColor(mFAB.getColorNormal(), 0.5f));
        mFAB.setColorPressed(UiUtil.darkerColor(mFAB.getColorNormal(), 0.3f));
        mFAB = (FloatingActionButton) view.findViewById(R.id.fab_inc);
        mFAB.setOnClickListener(this);
        mFAB.setColorRipple(UiUtil.darkerColor(mFAB.getColorNormal(), 0.5f));
        mFAB.setColorPressed(UiUtil.darkerColor(mFAB.getColorNormal(), 0.3f));
    }

    @Override
    protected View onCreateDialogView() {
        mAudioEngine = ((PreferencesActivity) getContext()).getAudioEngine();
        if (mAudioEngine != null) {
            mAudioEngine.addListener(this);
            mAudioEngine.setDelta(0);
        }

        return super.onCreateDialogView();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE ) {
            persistInt(mValue);
            callChangeListener(mValue);
            if (mAudioEngine != null)
                mAudioEngine.setDelta(mValue);
            setSummary();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_dec:
                mValue--;
                break;
            case R.id.fab_inc:
                mValue++;
                break;
        }

        mTvDelta.setText(Integer.toString(mValue));
    }

    @Override
    public void onInfoChanged(String sourceEngine) {
        ((Activity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvTotal.setText(mAudioEngine.getDb() + mValue + " " + getContext().getString(R.string.info_db));
            }
        });
    }

    public void setSummary() {
        setSummary(getPersistedInt(0) + " " + getContext().getString(R.string.info_db));
    }
}
