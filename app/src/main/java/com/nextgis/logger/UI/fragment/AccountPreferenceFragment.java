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

package com.nextgis.logger.UI.fragment;

import android.Manifest;
import android.accounts.Account;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import com.nextgis.logger.R;
import com.nextgis.logger.UI.ProgressBarActivity;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;

import java.util.List;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;

public class AccountPreferenceFragment extends PreferenceFragment {
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_ngw);

        if (!ProgressBarActivity.hasAccountPermissions(getActivity())) {
            String[] permissions = new String[]{Manifest.permission.GET_ACCOUNTS};
            ProgressBarActivity
                    .requestPermissions(getActivity(), R.string.permissions_title, R.string.permissions_acc, ProgressBarActivity.PERMISSION_ACC, permissions);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    private void initPreferences() {
        final IGISApplication app = (IGISApplication) getActivity().getApplication();
        final Account account = app.getAccount(getString(R.string.app_name));
        final String authority = app.getAuthority();
        boolean hasAccount = account != null;

        // add/edit account preference
        Preference preference = findPreference(LoggerConstants.PREF_ACCOUNT_EDIT);
        preference.setTitle(hasAccount ? R.string.ngw_edit : R.string.ngw_add);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ProgressBarActivity.startNGWActivity(getActivity());
                return false;
            }
        });

        preference = findPreference(LoggerConstants.PREF_ACCOUNT_DELETE);
        preference.setEnabled(hasAccount);
        CheckBoxPreference enablePeriodicSync = (CheckBoxPreference) findPreference(LoggerConstants.PREF_AUTO_SYNC);
        enablePeriodicSync.setEnabled(hasAccount);
        if (!hasAccount)
            return;

        // delete account preference
        final boolean[] wasCurrentSyncActive = {false};
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            private ProgressDialog mProgressDialog;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (mProgressDialog == null)
                    mProgressDialog = new ProgressDialog(getActivity());

                if (!mProgressDialog.isShowing()) {
                    mProgressDialog.setTitle(R.string.ngw_delete);
                    mProgressDialog.setMessage(getString(R.string.message_loading));
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.show();
                }

                String action;
                if (wasCurrentSyncActive[0]) {
                    if (null == intent)
                        return;

                    action = intent.getAction();
                } else
                    action = SyncAdapter.SYNC_CANCELED;

                switch (action) {
                    case SyncAdapter.SYNC_START:
                        break;
                    case SyncAdapter.SYNC_FINISH:
                        break;
                    case SyncAdapter.SYNC_CANCELED:
                        wasCurrentSyncActive[0] = false;
                        app.removeAccount(account);
                        getActivity().unregisterReceiver(this);
                        mProgressDialog.dismiss();
                        onDeleteAccount();
                        break;
                    case SyncAdapter.SYNC_CHANGES:
                        break;
                }
            }
        };

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(R.string.ngw_delete).setMessage(R.string.confirmation).setNegativeButton(android.R.string.cancel, null)
                     .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             IntentFilter intentFilter = new IntentFilter();
                             intentFilter.addAction(SyncAdapter.SYNC_CANCELED);
                             getActivity().registerReceiver(broadcastReceiver, intentFilter);
                             wasCurrentSyncActive[0] = AccountUtil.isSyncActive(account, app.getAuthority());
                             ContentResolver.removePeriodicSync(account, app.getAuthority(), Bundle.EMPTY);
                             ContentResolver.setSyncAutomatically(account, app.getAuthority(), false);
                             ContentResolver.cancelSync(account, app.getAuthority());
                             broadcastReceiver.onReceive(getActivity(), null);
                         }
                     });

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                dialogBuilder.show();
                return true;
            }
        });

        // auto sync preference
        boolean isAccountSyncEnabled = ContentResolver.getSyncAutomatically(account, authority);
        enablePeriodicSync.setChecked(isAccountSyncEnabled);
        enablePeriodicSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isChecked = (boolean) newValue;
                ContentResolver.setSyncAutomatically(account, authority, isChecked);
                return true;
            }
        });

        // sync period preference
        String prefValue = "" + Constants.DEFAULT_SYNC_PERIOD;
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, app.getAuthority());
        if (null != syncs && !syncs.isEmpty()) {
            for (PeriodicSync sync : syncs) {
                Bundle bundle = sync.extras;
                long period = bundle.getLong(LoggerConstants.PREF_SYNC_PERIOD_SEC_LONG, Constants.NOT_FOUND);
                if (period > 0) {
                    prefValue = "" + period;
                    break;
                }
            }
        }

        ListPreference timeInterval = (ListPreference) findPreference(LoggerConstants.PREF_AUTO_SYNC_PERIOD);
        final CharSequence[] keys = getPeriodTitles(getActivity());
        final CharSequence[] values = getPeriodValues();
        timeInterval.setEntries(keys);
        timeInterval.setEntryValues(values);
        timeInterval.setValueIndex(4);
        timeInterval.setSummary(keys[4]);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(prefValue)) {
                timeInterval.setValueIndex(i);
                timeInterval.setSummary(keys[i]);
                break;
            }
        }

        timeInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                long interval = Long.parseLong((String) newValue);
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(newValue)) {
                        preference.setSummary(keys[i]);
                        break;
                    }
                }

                Bundle bundle = new Bundle();
                bundle.putLong(LoggerConstants.PREF_SYNC_PERIOD_SEC_LONG, interval);

                if (interval == NOT_FOUND)
                    ContentResolver.removePeriodicSync(account, app.getAuthority(), bundle);
                else
                    ContentResolver.addPeriodicSync(account, app.getAuthority(), bundle, interval);

                return true;
            }
        });
    }

    private void onDeleteAccount() {
        initPreferences();
    }

    public static CharSequence[] getPeriodTitles(Context context) {
        return new CharSequence[]{"5" + context.getString(R.string.unit_min), "10" + context.getString(R.string.unit_min),
                                  "15" + context.getString(R.string.unit_min), "30" + context.getString(R.string.unit_min),
                                  "1" + context.getString(R.string.unit_hour), "2" + context.getString(R.string.unit_hour)};
    }

    public static CharSequence[] getPeriodValues() {
        return new CharSequence[]{"300", "600", "900", "1800", "3600", "7200"};
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ProgressBarActivity.PERMISSION_ACC:
                initPreferences();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
