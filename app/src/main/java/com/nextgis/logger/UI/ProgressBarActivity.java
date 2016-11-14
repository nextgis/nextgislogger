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

import android.Manifest;
import android.accounts.Account;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.nextgis.logger.AboutActivity;
import com.nextgis.logger.LoggerService;
import com.nextgis.logger.MainActivity;
import com.nextgis.logger.NGWLoginActivity;
import com.nextgis.logger.PreferencesActivity;
import com.nextgis.logger.R;
import com.nextgis.logger.livedata.InfoActivity;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.LoggerVectorLayer;
import com.nextgis.logger.util.UiUtil;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.NGWUtil;

import java.util.ArrayList;
import java.util.List;

public class ProgressBarActivity extends FragmentActivity implements View.OnClickListener {
    private static final int PERMISSION_MAIN = 1;
    public static final int PERMISSION_ACC = 2;

    protected SharedPreferences mPreferences;
    protected FloatingActionButton mFAB;
    protected boolean mHasFAB = true;
    protected String mSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSessionId = mPreferences.getString(LoggerConstants.PREF_SESSION_ID, null);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        setActionBarProgress(isLoggerServiceRunning());

        if (!hasPermissions()) {
            String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO};
            requestPermissions(this, R.string.permissions_title, R.string.permissions_main, PERMISSION_MAIN, permissions);
        }
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
                if (this instanceof MainActivity)
                    return true;

                finish();
                return true;
            case R.id.action_ngw:
                toCloud();
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

    private void toCloud() {
        if (!hasAccountPermissions(this)) {
            String[] permissions = new String[]{Manifest.permission.GET_ACCOUNTS};
            requestPermissions(this, R.string.permissions_title, R.string.permissions_acc, PERMISSION_ACC, permissions);
            return;
        }

        IGISApplication app = (IGISApplication) getApplication();
        Account account = app.getAccount(getString(R.string.app_name));

        if (account != null)
            new Sync(this).execute();
        else
            startNGWActivity(this);
    }

    private List<SyncResult> sync(Sync sync) {
        List<SyncResult> result = new ArrayList<>();
        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();
        String accountName;
        LoggerVectorLayer ngwVectorLayer;
        for (int i = 0; i < map.getLayerCount(); i++) {
            ILayer layer = map.getLayer(i);
            if (layer instanceof NGWVectorLayer) {
                ngwVectorLayer = (LoggerVectorLayer) layer;
                accountName = ngwVectorLayer.getAccountName();
                if (TextUtils.isEmpty(accountName)) {
                    startNGWActivity(this);
                    return result;
                }

                Pair<Integer, Integer> ver = NGWUtil.getNgwVersion(this, accountName);
                result.add(new SyncResult());
                ngwVectorLayer.sync(sync);
                ngwVectorLayer.sync(application.getAuthority(), ver, result.get(result.size() - 1));
            }
        }

        return result;
    }

    public class Sync extends AsyncTask<Void, String, List<SyncResult>> {
        private ProgressDialog mProgress;

        Sync(Activity activity) {
            mProgress = new ProgressDialog(activity);
            mProgress.setIndeterminate(true);
            mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(true);
                }
            });
            mProgress.setMessage(getString(R.string.sync_progress));
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress.show();
        }

        @Override
        protected List<SyncResult> doInBackground(Void... voids) {
            return sync(Sync.this);
        }

        public void publishProgress(int max, int progress, String message) {
            publishProgress(max + "", progress + "", message);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mProgress.setIndeterminate(false);
            mProgress.setMax(Integer.parseInt(values[0]));
            mProgress.setProgress(Integer.parseInt(values[1]));
            mProgress.setMessage(getString(R.string.start_fill_layer) + " " + values[2]);
        }

        @Override
        protected void onPostExecute(List<SyncResult> result) {
            super.onPostExecute(result);
            mProgress.dismiss();

            String info = getString(R.string.sync_finished);
            for (SyncResult syncResult : result) {
                if (syncResult.hasError()) {
                    if (syncResult.stats.numIoExceptions > 0)
                        info += getString(com.nextgis.maplib.R.string.sync_error_io);
                    if (syncResult.stats.numParseExceptions > 0) {
                        if (info.length() > 0)
                            info += "\r\n";
                        info += getString(com.nextgis.maplib.R.string.sync_error_parse);
                    }
                    if (syncResult.stats.numAuthExceptions > 0) {
                        if (info.length() > 0)
                            info += "\r\n";
                        info += getString(com.nextgis.maplib.R.string.error_auth);
                    }
                    if (syncResult.stats.numConflictDetectedExceptions > 0) {
                        if (info.length() > 0)
                            info += "\r\n";
                        info += getString(com.nextgis.maplib.R.string.sync_error_conflict);
                    }
                    break;
                }
            }
            Toast.makeText(ProgressBarActivity.this, info, Toast.LENGTH_LONG).show();
        }
    }

    public static void startNGWActivity(Activity activity) {
        Intent ngwActivity = new Intent(activity, NGWLoginActivity.class);
        IGISApplication app = (IGISApplication) activity.getApplication();

        Account account = app.getAccount(activity.getString(R.string.app_name));
        if (account != null) {
            ngwActivity.putExtra(NGWLoginActivity.FOR_NEW_ACCOUNT, false);
            ngwActivity.putExtra(NGWLoginActivity.ACCOUNT_URL_TEXT, app.getAccountUrl(account));
            ngwActivity.putExtra(NGWLoginActivity.ACCOUNT_LOGIN_TEXT, app.getAccountLogin(account));
        }

        activity.startActivity(ngwActivity);

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

    public boolean isLoggerServiceRunning() {
        boolean isServiceRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (LoggerService.class.getName().equals(service.service.getClassName())) {
                isServiceRunning = true;
                break;
            }

        return isServiceRunning && mPreferences.getBoolean(LoggerConstants.PREF_MEASURING, false);
    }

    public boolean isSessionClosed() {
        return TextUtils.isEmpty(mPreferences.getString(LoggerConstants.PREF_SESSION_ID, null));
    }

    protected void clearSession() {
        mPreferences.edit().remove(LoggerConstants.PREF_SESSION_ID).remove(LoggerConstants.PREF_MARKS_COUNT).remove(LoggerConstants.PREF_RECORDS_COUNT)
                    .remove(LoggerConstants.PREF_TIME_START).remove(LoggerConstants.PREF_TIME_FINISH).remove(LoggerConstants.PREF_MARK_POS).apply();
    }

    public static void startLoggerService(Context context, String action) {
        Intent startService = new Intent(context.getApplicationContext(), LoggerService.class);
        if (action != null)
            startService.setAction(action);
        context.startService(startService);
    }

    protected void stopLoggerService() {
        Intent stopService = new Intent(getApplicationContext(), LoggerService.class);
        stopService.setAction(LoggerConstants.ACTION_STOP);
        startService(stopService);
        setActionBarProgress(false);
    }

    protected boolean hasPermissions() {
        return UiUtil.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                UiUtil.isPermissionGranted(this, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                UiUtil.isPermissionGranted(this, Manifest.permission.READ_PHONE_STATE) &&
                UiUtil.isPermissionGranted(this, Manifest.permission.RECORD_AUDIO);
    }

    public static boolean hasAccountPermissions(Context context) {
        return UiUtil.isPermissionGranted(context, Manifest.permission.GET_ACCOUNTS);
    }

    public static void requestPermissions(final Activity activity, int title, int message, final int requestCode, final String... permissions) {
        boolean shouldShowDialog = false;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowDialog = true;
                break;
            }
        }

        if (shouldShowDialog) {
            AlertDialog builder = new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).create();
            builder.setCanceledOnTouchOutside(false);
            builder.show();
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    ActivityCompat.requestPermissions(activity, permissions, requestCode);
                }
            });
        } else
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_MAIN:
                break;
            case PERMISSION_ACC:
                startNGWActivity(this);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
