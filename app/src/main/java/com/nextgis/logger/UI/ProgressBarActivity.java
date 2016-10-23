/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov, becomeglory@gmail.com
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

package com.nextgis.logger.UI;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.melnykov.fab.FloatingActionButton;
import com.nextgis.logger.AboutActivity;
import com.nextgis.logger.livedata.InfoActivity;
import com.nextgis.logger.LoggerService;
import com.nextgis.logger.PreferencesActivity;
import com.nextgis.logger.R;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.UiUtil;
import com.nextgis.maplib.util.Constants;

public class ProgressBarActivity extends FragmentActivity implements View.OnClickListener {
    protected SharedPreferences mPreferences;
    protected FloatingActionButton mFAB;
    protected boolean mHasFAB = true;
    protected long mSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSessionId = mPreferences.getLong(LoggerConstants.PREF_SESSION_ID, Constants.NOT_FOUND);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        setActionBarProgress(isLoggerServiceRunning(this));
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        if (!mHasFAB)
            return;

        View view = getWindow().getDecorView().findViewById(android.R.id.content);

        if (view instanceof FrameLayout) {
            FrameLayout base = (FrameLayout) view;

            if (base.findViewById(R.id.fab) == null) {
                FrameLayout layout = (FrameLayout) getLayoutInflater().inflate(R.layout.fab, base);
                mFAB = (FloatingActionButton) layout.findViewById(R.id.fab);
                layout.removeView(mFAB);
                base.addView(mFAB);

                mFAB.setColorRipple(UiUtil.darkerColor(mFAB.getColorNormal(), 0.5f));
                mFAB.setColorPressed(UiUtil.darkerColor(mFAB.getColorNormal(), 0.3f));
                mFAB.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                mFAB.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotation));
                Intent infoActivity = new Intent(this, InfoActivity.class);
                startActivity(infoActivity);
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settings = menu.findItem(R.id.action_settings);

        if (settings != null)
            settings.setEnabled(isSessionClosed());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
                startActivity(preferencesActivity);
                return true;
            case R.id.action_about:
                Intent aboutActivity = new Intent(this, AboutActivity.class);
                startActivity(aboutActivity);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // http://stackoverflow.com/a/24102651/2088273
    protected void setActionBarProgress(boolean state) {
        ActionBar actionBar = getActionBar();

        // retrieve the top view of our application
        final FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        ProgressBar progressBar = null;
        int osVersion = Build.VERSION.SDK_INT;

        for (int i = 0; i < decorView.getChildCount(); i++)
            if (decorView.getChildAt(i) instanceof ProgressBar) {
                progressBar = ((ProgressBar) decorView.getChildAt(i));
                break;
            }

        // create new ProgressBar and style it
        if (progressBar == null) {
            if (actionBar != null && osVersion < Build.VERSION_CODES.LOLLIPOP)
                actionBar.setBackgroundDrawable(null);

            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 24));
            progressBar.setProgress(100);
            decorView.addView(progressBar);

            // Here we try to position the ProgressBar to the correct position by looking
            // at the position where content area starts. But during creating time, sizes
            // of the components are not set yet, so we have to wait until the components
            // has been laid out
            // Also note that doing progressBar.setY(136) will not work, because of different
            // screen densities and different sizes of actionBar
            ViewTreeObserver observer = progressBar.getViewTreeObserver();
            final ProgressBar finalProgressBar = progressBar;
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View contentView = decorView.findViewById(android.R.id.content);
                    finalProgressBar.setY(contentView.getY() - 10);

                    ViewTreeObserver observer = finalProgressBar.getViewTreeObserver();
                    observer.removeGlobalOnLayoutListener(this);
                }
            });
        }

        if (state) {
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setIndeterminate(false);

            if (osVersion >= Build.VERSION_CODES.LOLLIPOP)
                progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public static boolean isLoggerServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LoggerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSessionClosed() {
        return mPreferences.getLong(LoggerConstants.PREF_SESSION_ID, Constants.NOT_FOUND) == Constants.NOT_FOUND;
    }

    protected void clearSession() {
        mPreferences.edit().remove(LoggerConstants.PREF_SESSION_ID)
                .remove(LoggerConstants.PREF_MARKS_COUNT)
                .remove(LoggerConstants.PREF_RECORDS_COUNT)
                .remove(LoggerConstants.PREF_TIME_START)
                .remove(LoggerConstants.PREF_TIME_FINISH)
                .remove(LoggerConstants.PREF_MARK_POS).apply();
    }

    protected void stopService() {
        Intent stopService = new Intent(getApplicationContext(), LoggerService.class);
        stopService.setAction(LoggerConstants.ACTION_STOP);
        startService(stopService);
        setActionBarProgress(false);
    }
}
