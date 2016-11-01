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

package com.nextgis.logger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FeatureChanges;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.GET_ACCOUNTS;

public class NGWLoginActivity extends ProgressBarActivity implements NGWLoginFragment.OnAddAccountListener {
    public static final String FOR_NEW_ACCOUNT = "for_new_account";
    public static final String ACCOUNT_URL_TEXT = "account_url_text";
    public static final String ACCOUNT_LOGIN_TEXT = "account_login_text";
    public static final String CHANGE_ACCOUNT_URL = "change_account_url";
    public static final String CHANGE_ACCOUNT_LOGIN = "change_account_login";

    private static final String[] TABLES = new String[]{LoggerApplication.TABLE_SESSION, LoggerApplication.TABLE_MARK,
            LoggerApplication.TABLE_CELL, LoggerApplication.TABLE_SENSOR, LoggerApplication.TABLE_EXTERNAL};

    protected boolean mForNewAccount = true;
    protected boolean mChangeAccountUrl = mForNewAccount;
    protected boolean mChangeAccountLogin = mForNewAccount;

    protected String mUrlText;
    protected String mLoginText;
    protected String mLoggerName;
    private Connection mConnection;
    private ProgressDialog mProgress;
    private Long mGroupId;
    private int mCounter;
    private volatile Pair<Integer, Integer> mVer;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_ngw_login);

        mFAB.hide();
        mLoggerName = getString(R.string.app_name);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mForNewAccount = extras.getBoolean(FOR_NEW_ACCOUNT, true);
            if (!mForNewAccount) {
                mUrlText = extras.getString(ACCOUNT_URL_TEXT);
                mLoginText = extras.getString(ACCOUNT_LOGIN_TEXT);
                mChangeAccountUrl = extras.getBoolean(CHANGE_ACCOUNT_URL, true);
                mChangeAccountLogin = extras.getBoolean(CHANGE_ACCOUNT_LOGIN, true);
                setTitle(R.string.ngw_edit);
            }
        }

        createView();
    }

    protected void createView() {
        FragmentManager fm = getFragmentManager();
        NGWLoginFragment ngwLoginFragment = (NGWLoginFragment) fm.findFragmentByTag("NGWLogin");

        if (ngwLoginFragment == null) {
            ngwLoginFragment = new NGWLoginFragment();
            ngwLoginFragment.setForNewAccount(mForNewAccount);
            ngwLoginFragment.setUrlText(mUrlText == null ? "" : mUrlText);
            ngwLoginFragment.setLoginText(mLoginText);
            ngwLoginFragment.setChangeAccountUrl(mChangeAccountUrl);
            ngwLoginFragment.setChangeAccountLogin(mChangeAccountLogin);
            ngwLoginFragment.setOnAddAccountListener(this);
        }

        if (!ngwLoginFragment.isAdded()) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.login_frame, ngwLoginFragment, "NGWLogin");
            ft.commit();
        }
    }

    @Override
    public void onAddAccount(Account account, String token, boolean accountAdded) {
        getConnection();
        if (mConnection == null) {
            Toast.makeText(this, R.string.error_login, Toast.LENGTH_SHORT).show();
            return;
        }

        final FindGroup task = new FindGroup();
        mProgress = new ProgressDialog(this);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                task.cancel(true);
            }
        });
        mProgress.setMessage(getString(R.string.sync_progress));
        mProgress.show();

        task.execute();
    }

    private void checkLayers() {
        HashMap<String, INGWResource> keys = new HashMap<>();
        keys.put(LoggerApplication.TABLE_SESSION, null);
        keys.put(LoggerApplication.TABLE_MARK, null);
        keys.put(LoggerApplication.TABLE_CELL, null);
        keys.put(LoggerApplication.TABLE_SENSOR, null);
        keys.put(LoggerApplication.TABLE_EXTERNAL, null);
        getResourceByName(mConnection, keys);

        if (isFilled(keys)) {
            assignLayers(keys);
        } else {
            createLayers(keys);
        }
    }

    private boolean isFilled(Map<?, ?> map) {
        boolean filled = true;
        for (Map.Entry<?, ?> entry : map.entrySet())
            if (entry.getValue() == null) {
                filled = false;
                break;
            }

        return filled;
    }

    private void createGroup() {
        NGWCreateNewResourceTask createGroup = new NGWCreateNewResourceTask(this, mConnection, 0);
        createGroup.setName(mLoggerName);
        createGroup.execute();
    }

    private void createLayers(HashMap<String, INGWResource> keys) {
        for (String table : keys.keySet()) {
            if (keys.get(table) == null) {
                NGWVectorLayer layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(table);
                if (layer != null) {
                    NGWCreateNewResourceTask createLayer = new NGWCreateNewResourceTask(this, mConnection, mGroupId);
                    createLayer.setLayer(layer);
                    createLayer.execute();
                    mCounter++;
                }
            }
        }
    }

    private void assignLayers(HashMap<String, INGWResource> keys) {
        IGISApplication app = (IGISApplication) getApplicationContext();
        String authority = app.getAuthority();
        String account = mConnection.getName();

        if (mVer == null) {
            try {
                AccountUtil.AccountData accountData = AccountUtil.getAccountData(this, account);
                mVer = NGWUtil.getNgwVersion(accountData.url, accountData.login, accountData.password);
            } catch (Exception ignored) {}
        }

        long id;
        for (String table : TABLES) {
            NGWVectorLayer layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(table);
            if (layer != null && mVer != null) {
                id = ((Resource) keys.get(table)).getRemoteId();
                layer.mNgwVersionMajor = mVer.first;
                layer.mNgwVersionMinor = mVer.second;
                layer.mNGWLayerType = Connection.NGWResourceTypeVectorLayer;
                layer.setSyncType(table.equals(LoggerApplication.TABLE_MARK) ? Constants.SYNC_DATA : Constants.SYNC_ATTRIBUTES);
                layer.setAccountName(account);
                layer.setRemoteId(id);
                layer.save();
                FeatureChanges.initialize(layer.getChangeTableName());
                layer.sync(authority, mVer, new SyncResult());
            }
        }

        mProgress.dismiss();
        finish();
    }

    public void getConnection() {
        AccountManager accountManager = AccountManager.get(this);
        IGISApplication app = (IGISApplication) getApplicationContext();
        if (LoggerApplication.checkAccountStatus(getApplicationContext(), accountManager, GET_ACCOUNTS))
            return;

        try {
            for (Account account : accountManager.getAccountsByType(Constants.NGW_ACCOUNT_TYPE)) {
                if (account.name.equals(mLoggerName)) {
                    String url = app.getAccountUrl(account);
                    String password = app.getAccountPassword(account);
                    String login = app.getAccountLogin(account);
                    mConnection = new Connection(account.name, login, password, url.toLowerCase());
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getResourceByName(INGWResource resource, Map<String, INGWResource> keys) {
        boolean br = true;
        for (Map.Entry<String, INGWResource> entry : keys.entrySet())
            if (entry.getValue() == null) {
                br = false;
                break;
            }

        if (br)
            return;

        if (resource instanceof Connection) {
            Connection connection = (Connection) resource;
            if (!connection.isConnected())
                connection.connect();
            connection.loadChildren();
        } else if (resource instanceof ResourceGroup) {
            ResourceGroup resourceGroup = (ResourceGroup) resource;
            resourceGroup.loadChildren();
        }

        for (int i = 0; i < resource.getChildrenCount(); ++i) {
            INGWResource childResource = resource.getChild(i);

            if (keys.containsKey(childResource.getName()))
                keys.put(childResource.getName(), childResource);

            if (childResource instanceof ResourceGroup)
                getResourceByName(childResource, keys);
        }
    }

    public class FindGroup extends AsyncTask<Object, Object, String> {
        @Override
        protected String doInBackground(Object... voids) {
            HashMap<String, INGWResource> keys = new HashMap<>();
            keys.put(mLoggerName, null);
            getResourceByName(mConnection, keys);
            INGWResource group = keys.get(mLoggerName);
            if (group == null) {
                createGroup();
            } else if (group instanceof ResourceGroup) {
                mGroupId = ((ResourceGroup) group).getRemoteId();
                checkLayers();
            } else
                return "";

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null)
                Toast.makeText(NGWLoginActivity.this, R.string.error_incompatible, Toast.LENGTH_SHORT).show();
        }
    }

    public class NGWCreateNewResourceTask extends AsyncTask<Void, Void, String> {
        private Connection mConnection;
        private VectorLayer mLayer;
        private Context mContext;
        private long mParentId;
        private String mName;

        NGWCreateNewResourceTask(Context context, Connection connection, long parentId) {
            mContext = context;
            mParentId = parentId;
            mConnection = connection;
        }

        NGWCreateNewResourceTask setLayer(VectorLayer layer) {
            mLayer = layer;
            return this;
        }

        public NGWCreateNewResourceTask setName(String name) {
            mName = name;
            return this;
        }

        @Override
        protected String doInBackground(Void... voids) {
            if (mConnection.isConnected() || mConnection.connect()) {
                if (mVer == null) {
                    try {
                        AccountUtil.AccountData accountData = AccountUtil.getAccountData(mContext, mConnection.getName());
                        if (null == accountData.url)
                            return "404";

                        mVer = NGWUtil.getNgwVersion(accountData.url, accountData.login, accountData.password);
                    } catch (IllegalStateException e) {
                        return "401";
                    } catch (JSONException | IOException | NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                String result;
                if (mLayer != null)
                    result = NGWUtil.createNewLayer(mConnection, mLayer, mParentId);
                else
                    result = NGWUtil.createNewGroup(mContext, mConnection, mParentId, mName);

                if (!MapUtil.isParsable(result)) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        Long id = obj.getLong(Constants.JSON_ID_KEY);

                        if (mName != null && id != Constants.NOT_FOUND) {
                            mGroupId = id;
                            checkLayers();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                return result;
            }

            return "0";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            String message;
            if (!MapUtil.isParsable(result)) {
                try {
                    JSONObject obj = new JSONObject(result);
                    Long id = obj.getLong(Constants.JSON_ID_KEY);

                    if (mLayer != null) {
                        boolean isMarks = mLayer.getName().equals(LoggerApplication.TABLE_MARK);
                        int syncType = isMarks ? Constants.SYNC_DATA : Constants.SYNC_ATTRIBUTES;
                        ((NGWVectorLayer) mLayer).setSyncType(syncType);
                        mLayer.save();
                        mLayer.toNGW(id, mConnection.getName(), syncType, mVer);
                        mCounter--;

                        if (mCounter == 0) {
                            mProgress.dismiss();
                            finish();
                        }
                    }

                    result = null;
                } catch (JSONException e) {
                    result = "500";
                    e.printStackTrace();
                }
            }

            if (result != null) {
                switch (result) {
                    case "-999":
                        message = getString(R.string.btn_ok);
                        break;
                    default:
                        message = NetworkUtil.getError(mContext, result);
                        break;
                }

                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                mProgress.dismiss();
            }
        }
    }
}