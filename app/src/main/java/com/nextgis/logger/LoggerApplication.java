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

package com.nextgis.logger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.LoggerVectorLayer;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplib.util.SettingsConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.GET_ACCOUNTS;
import static com.nextgis.logger.util.LoggerConstants.PREF_AUTO_SYNC;
import static com.nextgis.logger.util.LoggerConstants.PREF_AUTO_SYNC_PERIOD;
import static com.nextgis.maplib.util.Constants.CONFIG;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_VECTOR;
import static com.nextgis.maplib.util.Constants.MAP_EXT;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;

public class LoggerApplication extends Application implements IGISApplication {
    private static final String MAP_NAME = "default";
    private static final String PERMISSION_MANAGE_ACCOUNTS = "android.permission.MANAGE_ACCOUNTS";
    private static final String PERMISSION_AUTHENTICATE_ACCOUNTS = "android.permission.AUTHENTICATE_ACCOUNTS";

    private static byte[] GEO_POINT_NULL;

    public static final String TABLE_SESSION = "session";
    public static final String TABLE_MARK = "mark";
    public static final String TABLE_CELL = "data_cell";
    public static final String TABLE_SENSOR = "data_sensor";
    public static final String TABLE_EXTERNAL = "data_external";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_USER = "user";
    public static final String FIELD_DEVICE_INFO = "device_info";
    public static final String FIELD_UNIQUE_ID = "uuid";
    public static final String FIELD_SESSION = "session";
    public static final String FIELD_MARK_ID = "mark_id";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_DATETIME = "datetime";
    public static final String FIELD_MARK = "mark";
    public static final String FIELD_DATA = "data";

    private static LoggerApplication mApplication;

    protected MapDrawable mMap;
    protected GpsEventSource mGpsEventSource;
    protected SharedPreferences mSharedPreferences;
    protected AccountManager mAccountManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = this;

        mGpsEventSource = new GpsEventSource(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAccountManager = AccountManager.get(this);

        getMap();
        checkLayers();
        updateFromPrevious();

        if (mSharedPreferences.getBoolean(PREF_AUTO_SYNC, true)) {
            long period = mSharedPreferences.getLong(PREF_AUTO_SYNC_PERIOD, Constants.DEFAULT_SYNC_PERIOD);
            if (period == -1)
                period = Constants.DEFAULT_SYNC_PERIOD;

            Bundle params = new Bundle();
            params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

            SyncAdapter.setSyncPeriod(this, params, period);
        }
    }

    public static byte[] getNullGeometry() {
        if (GEO_POINT_NULL == null) {
            try {
                GEO_POINT_NULL = new GeoPoint(0, 0).toBlob();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return GEO_POINT_NULL;
    }

    public static LoggerApplication getApplication() {
        return mApplication;
    }

    @Override
    public MapBase getMap() {
        if (null != mMap) {
            return mMap;
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        File defaultPath = getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(getFilesDir(), KEY_PREF_MAP);
        }

        String mapPath = mSharedPreferences.getString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        File mapFullPath = new File(mapPath, MAP_NAME + MAP_EXT);
        final Bitmap bkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.nextgis);

        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactory() {
            @Override
            public ILayer createLayer(Context context, File path) {
                File config_file = new File(path, CONFIG);
                try {
                    String sData = FileUtil.readFromFile(config_file);
                    JSONObject rootObject = new JSONObject(sData);
                    int nType = rootObject.getInt(JSON_TYPE_KEY);

                    switch (nType) {
                        case LAYERTYPE_NGW_VECTOR:
                            return new LoggerVectorLayer(context, path);
                        default:
                            return super.createLayer(context, path);
                    }
                } catch (IOException | JSONException e) {
                    Log.d(TAG, e.getLocalizedMessage());
                }

                return super.createLayer(context, path);
            }

            @Override
            public void createNewRemoteTMSLayer(Context context, LayerGroup groupLayer) {

            }

            @Override
            public void createNewNGWLayer(Context context, LayerGroup groupLayer) {

            }

            @Override
            public void createNewLocalTMSLayer(Context context, LayerGroup groupLayer, Uri uri) {

            }

            @Override
            public void createNewVectorLayer(Context context, LayerGroup groupLayer, Uri uri) {

            }

            @Override
            public void createNewVectorLayerWithForm(Context context, LayerGroup groupLayer, Uri uri) {

            }
        });
        mMap.setName(MAP_NAME);
        mMap.load();

        return mMap;
    }

    @Override
    public String getAuthority() {
        return LoggerConstants.AUTHORITY;
    }

    @Override
    public Account getAccount(String accountName) {
        if (checkAccountStatus(this, mAccountManager, GET_ACCOUNTS))
            return null;

        try {
            for (Account account : mAccountManager.getAccountsByType(Constants.NGW_ACCOUNT_TYPE)) {
                if (account == null)
                    continue;

                if (account.name.equals(accountName))
                    return account;
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public AccountManagerFuture<Boolean> removeAccount(Account account) {
        AccountManagerFuture<Boolean> bool = new AccountManagerFuture<Boolean>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Boolean getResult() throws OperationCanceledException, IOException, AuthenticatorException {
                return null;
            }

            @Override
            public Boolean getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
                return null;
            }
        };

        if (checkAccountStatus(this, mAccountManager, PERMISSION_MANAGE_ACCOUNTS))
            return bool;

        try {
            return mAccountManager.removeAccount(account, null, new Handler());
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return bool;
    }

    @Override
    public String getAccountUrl(Account account) {
        return getAccountUserData(account, "url").toLowerCase();
    }

    @Override
    public String getAccountLogin(Account account) {
        return getAccountUserData(account, "login");
    }

    @Override
    public String getAccountPassword(Account account) {
        if (checkAccountStatus(this, mAccountManager, PERMISSION_AUTHENTICATE_ACCOUNTS))
            return "";

        try {
            return mAccountManager.getPassword(account);
        } catch (SecurityException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public GpsEventSource getGpsEventSource() {
        return mGpsEventSource;
    }

    @Override
    public void showSettings(String setting) {

    }

    protected void checkLayers() {
        ArrayList<Field> fields = new ArrayList<>();
        LoggerVectorLayer layer = (LoggerVectorLayer) mMap.getLayerByName(TABLE_SESSION);
        if (layer == null) {
            fields.clear();
            fields.add(new Field(GeoConstants.FTString, FIELD_UNIQUE_ID, getString(R.string.unique_id)));
            fields.add(new Field(GeoConstants.FTString, FIELD_NAME, getString(R.string.mark_name)));
            fields.add(new Field(GeoConstants.FTString, FIELD_USER, getString(R.string.user_name)));
            fields.add(new Field(GeoConstants.FTString, FIELD_DEVICE_INFO, getString(R.string.device_info)));
            layer = createEmptyVectorLayer(TABLE_SESSION, fields);
            layer.setAccountName("");
            mMap.addLayer(layer);
            mMap.save();
        }

        layer = (LoggerVectorLayer) mMap.getLayerByName(TABLE_MARK);
        if (layer == null) {
            fields.clear();
            fields.add(new Field(GeoConstants.FTString, FIELD_UNIQUE_ID, getString(R.string.unique_id)));
            fields.add(new Field(GeoConstants.FTString, FIELD_SESSION, getString(R.string.title_activity_sessions)));
            fields.add(new Field(GeoConstants.FTInteger, FIELD_MARK_ID, getString(R.string.mark_id)));
            fields.add(new Field(GeoConstants.FTString, FIELD_NAME, getString(R.string.mark_name)));
            fields.add(new Field(GeoConstants.FTReal, FIELD_TIMESTAMP, getString(R.string.timestamp)));
            fields.add(new Field(GeoConstants.FTDateTime, FIELD_DATETIME, getString(R.string.field_type_datetime)));
            layer = createEmptyVectorLayer(TABLE_MARK, fields);
            layer.setAccountName("");
            mMap.addLayer(layer);
            mMap.save();
        }

        layer = (LoggerVectorLayer) mMap.getLayerByName(TABLE_CELL);
        if (layer == null) {
            fields.clear();
            fields.add(new Field(GeoConstants.FTString, FIELD_MARK, getString(R.string.btn_save_mark)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GEN, getString(R.string.info_title_network)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_TYPE, getString(R.string.network_type)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_ACTIVE, getString(R.string.info_active)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_MCC, "MCC"));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_MNC, "MNC"));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_LAC, "LAC/TAC"));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_CID, "CID/PCI"));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_PSC, "PSC/CI"));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_POWER, "RSSI/RSRP"));
            layer = createEmptyVectorLayer(TABLE_CELL, fields);
            layer.setAccountName("");
            mMap.addLayer(layer);
            mMap.save();
        }

        layer = (LoggerVectorLayer) mMap.getLayerByName(TABLE_SENSOR);
        if (layer == null) {
            fields.clear();
            fields.add(new Field(GeoConstants.FTString, FIELD_MARK, getString(R.string.btn_save_mark)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_ACC_X, getString(R.string.info_x)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_ACC_Y, getString(R.string.info_y)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_ACC_Z, getString(R.string.info_z)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_LINEAR_X, getString(R.string.info_x)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_LINEAR_Y, getString(R.string.info_y)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_LINEAR_Z, getString(R.string.info_z)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_AZIMUTH, getString(R.string.info_azimuth)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_PITCH, getString(R.string.info_pitch)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_ROLL, getString(R.string.info_roll)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_MAGNETIC_X, getString(R.string.info_x)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_MAGNETIC_Y, getString(R.string.info_y)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_MAGNETIC_Z, getString(R.string.info_z)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GYRO_X, getString(R.string.info_x)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GYRO_Y, getString(R.string.info_y)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GYRO_Z, getString(R.string.info_z)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_LAT, getString(R.string.info_lat)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_LON, getString(R.string.info_lon)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_ALT, getString(R.string.info_ele)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_ACC, getString(R.string.info_acc)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_SP, getString(R.string.info_speed)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_BE, getString(R.string.info_bearing)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_SAT, getString(R.string.info_sat)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_GPS_TIME, getString(R.string.info_time)));
            fields.add(new Field(GeoConstants.FTString, LoggerConstants.HEADER_AUDIO, getString(R.string.mic)));
            layer = createEmptyVectorLayer(TABLE_SENSOR, fields);
            layer.setAccountName("");
            mMap.addLayer(layer);
            mMap.save();
        }

        layer = (LoggerVectorLayer) mMap.getLayerByName(TABLE_EXTERNAL);
        if (layer == null) {
            fields.clear();
            fields.add(new Field(GeoConstants.FTString, FIELD_MARK, getString(R.string.btn_save_mark)));
            fields.add(new Field(GeoConstants.FTString, FIELD_DATA, getString(R.string.info_title_external)));
            layer = createEmptyVectorLayer(TABLE_EXTERNAL, fields);
            layer.setAccountName("");
            mMap.addLayer(layer);
            mMap.save();
        }
    }

    public LoggerVectorLayer createEmptyVectorLayer(String layerName, List<Field> fields) {
        LoggerVectorLayer layer = new LoggerVectorLayer(this, mMap.createLayerStorage(layerName));
        layer.setName(layerName);
        layer.create(GeoConstants.GTPoint, fields);
        return layer;
    }

    @Override
    public boolean addAccount(String name, String url, String login, String password, String token) {
        if (checkAccountStatus(this, mAccountManager, PERMISSION_AUTHENTICATE_ACCOUNTS))
            return false;

        final Account account = new Account(name, Constants.NGW_ACCOUNT_TYPE);
        Bundle userData = new Bundle();
        userData.putString("url", url.trim());
        userData.putString("login", login);

        try {
            boolean accountAdded = mAccountManager.addAccountExplicitly(account, password, userData);
            if (accountAdded)
                mAccountManager.setAuthToken(account, account.type, token);

            return accountAdded;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setPassword(String name, String value) {
        if (checkAccountStatus(this, mAccountManager, PERMISSION_AUTHENTICATE_ACCOUNTS))
            return;

        Account account = getAccount(name);
        if (null != account)
            mAccountManager.setPassword(account, value);
    }

    @Override
    public void setUserData(String name, String key, String value) {
        if (checkAccountStatus(this, mAccountManager, PERMISSION_AUTHENTICATE_ACCOUNTS))
            return;

        Account account = getAccount(name);
        if (null != account)
            mAccountManager.setUserData(account, key, value);
    }

    @Override
    public String getAccountUserData(Account account, String key) {
        if (checkAccountStatus(this, mAccountManager, PERMISSION_AUTHENTICATE_ACCOUNTS))
            return "";

        String result = mAccountManager.getUserData(account, key);
        return result == null ? "" : result;
    }

    public static boolean checkAccountStatus(Context context, AccountManager accountManager, String permission) {
        return !PermissionUtil.hasPermission(context, permission) || !isAccountManagerValid(accountManager);
    }

    public static boolean isAccountManagerValid(AccountManager accountManager) {
        return null != accountManager;
    }

    private void updateFromPrevious() {
        try {
            int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int savedVersionCode = mSharedPreferences.getInt(LoggerConstants.PREF_APP_VERSION, 0);

            switch (savedVersionCode) {
                case 0:
                    mSharedPreferences.edit().putString(LoggerConstants.PREF_SESSION_ID, null).putInt(LoggerConstants.PREF_MARKS_COUNT, 0)
                                      .putInt(LoggerConstants.PREF_RECORDS_COUNT, 0).putInt(LoggerConstants.PREF_MARK_POS, Integer.MIN_VALUE).apply();
                default:
                    break;
            }

            if (savedVersionCode < currentVersionCode)
                mSharedPreferences.edit().putInt(LoggerConstants.PREF_APP_VERSION, currentVersionCode).apply();
        } catch (PackageManager.NameNotFoundException ignored) { }
    }
}
