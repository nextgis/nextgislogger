/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2016-2017 NextGIS
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

package com.nextgis.logger.ui.fragment;

import android.accounts.Account;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.logger.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.util.NGWUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NGWLoginFragment extends Fragment implements LoaderManager.LoaderCallbacks<String>, View.OnClickListener {
    private static final String PASSWORD_HINT = "••••••••••";
    private static final String ENDING = ".nextgis.com";
    private static final String DEFAULT_ACCOUNT = "administrator";

    protected EditText mURL, mLogin, mPassword;
    protected Button mSignInButton;
    protected TextView mLoginTitle, mManual, mTip;

    protected String mUrlText = "";
    protected String mLoginText = "";
    protected boolean mNGW;

    protected boolean mForNewAccount = true;
    protected boolean mChangeAccountUrl = mForNewAccount;
    protected boolean mChangeAccountLogin = mForNewAccount;

    protected OnAddAccountListener mOnAddAccountListener;
    protected Loader<String> mLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setForNewAccount(boolean forNewAccount) {
        mForNewAccount = forNewAccount;
    }

    public void setChangeAccountUrl(boolean changeAccountUrl) {
        mChangeAccountUrl = changeAccountUrl;
    }

    public void setChangeAccountLogin(boolean changeAccountLogin) {
        mChangeAccountLogin = changeAccountLogin;
    }

    public void setUrlText(String urlText) {
        mUrlText = urlText;
    }

    public void setLoginText(String loginText) {
        mLoginText = loginText;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_ngw_login, container, false);
        mURL = (EditText) view.findViewById(R.id.url);
        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);
        mSignInButton = (Button) view.findViewById(R.id.signin);

        TextWatcher watcher = new LocalTextWatcher();
        mURL.addTextChangedListener(watcher);
        mLoginTitle = (TextView) view.findViewById(R.id.login_title);
        mTip = (TextView) view.findViewById(R.id.tip);

        mManual = (TextView) view.findViewById(R.id.manual);
        mManual.setOnClickListener(this);
        highlightText();

        mLogin.setText(DEFAULT_ACCOUNT);
        if (!mForNewAccount) {
            mURL.setEnabled(mChangeAccountUrl);
            mLogin.setText(mLoginText);
            mLogin.setEnabled(mChangeAccountLogin);
            mLoginTitle.setVisibility(View.GONE);
            mPassword.setHint(PASSWORD_HINT);
            mPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    mPassword.setHint(hasFocus ? null : PASSWORD_HINT);
                }
            });

            if (mUrlText.endsWith(ENDING)) {
                mURL.setText(mUrlText.replace(ENDING, ""));
            } else {
                mManual.performClick();
                mURL.setText(mUrlText);
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSignInButton.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        mSignInButton.setOnClickListener(null);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.signin) {
            boolean urlFilled = checkEditText(mURL);
            boolean loginPasswordFilled = checkEditText(mLogin) && checkEditText(mPassword);
            if (!urlFilled || !loginPasswordFilled) {
                Toast.makeText(getActivity(), R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.WEB_URL.matcher(mUrlText).matches()) {
                Toast.makeText(getActivity(), R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                return;
            }

            int id = R.id.auth_token_loader;
            if (null != mLoader && mLoader.isStarted()) {
                mLoader = getLoaderManager().restartLoader(id, null, this);
            } else {
                mLoader = getLoaderManager().initLoader(id, null, this);
            }

            mSignInButton.setEnabled(false);
        } else if (v.getId() == R.id.manual) {
            mNGW = !mNGW;

            if (mNGW) {
                mTip.setVisibility(View.GONE);
                mManual.setText(R.string.nextgis_com);
                mURL.setCompoundDrawables(null, null, null, null);
                mURL.setHint(R.string.ngw_url);
                mUrlText = mUrlText.replace(ENDING, "");
                mLoginTitle.setVisibility(View.GONE);
            } else {
                mTip.setVisibility(View.VISIBLE);
                mManual.setText(R.string.click_here);
                mLoginTitle.setVisibility(View.VISIBLE);
                Drawable addition = ContextCompat.getDrawable(getActivity(), R.drawable.nextgis_addition);
                mURL.setCompoundDrawablesWithIntrinsicBounds(null, null, addition, null);
                mURL.setHint(R.string.instance_name);
                if (!mUrlText.contains(ENDING))
                    mUrlText += ENDING;
            }

            highlightText();
        }
    }

    private void highlightText() {
        final CharSequence text = mManual.getText();
        final SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new URLSpan(""), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mManual.setText(spannableString, TextView.BufferType.SPANNABLE);
    }

    protected boolean checkEditText(EditText edit) {
        return edit.getText().length() > 0;
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        String login = null;
        String password = null;

        if (id == R.id.auth_token_loader) {
            login = mLogin.getText().toString();
            password = mPassword.getText().toString();
        }

        return new HTTPLoader(getActivity().getApplicationContext(), mUrlText, login, password);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String token) {
        mSignInButton.setEnabled(true);
        String accountName = getString(R.string.app_name);

        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.length() > 0)
                onTokenReceived(accountName, token);
            else
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();
        }
    }

    public void onTokenReceived(String accountName, String token) {
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        String login = mLogin.getText().toString();
        String password = mPassword.getText().toString();

        if (mForNewAccount) {
            boolean accountAdded = app.addAccount(accountName, mUrlText, login, password, token);

            if (null != mOnAddAccountListener) {
                Account account = app.getAccount(accountName);
                if (accountAdded)
                    account = app.getAccount(accountName);

                mOnAddAccountListener.onAddAccount(account, token, accountAdded);
            }
        } else {
            if (mChangeAccountUrl)
                app.setUserData(accountName, "url", mUrlText.toLowerCase());

            if (mChangeAccountLogin)
                app.setUserData(accountName, "login", login);

            app.setPassword(accountName, password);

            Account account = app.getAccount(accountName);
            if (account == null) {
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();
                return;
            }

            List<INGWLayer> layers = getLayersForAccount(app, account);
            for (INGWLayer layer : layers)
                layer.setAccountCacheData();

            if (null != mOnAddAccountListener)
                mOnAddAccountListener.onAddAccount(account, token, false);
        }
    }

    protected static List<INGWLayer> getLayersForAccount(final IGISApplication application, Account account) {
        List<INGWLayer> out = new ArrayList<>();
        MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, out);
        return out;
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    public class LocalTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            mUrlText = mURL.getText().toString().trim();

            if (!mNGW)
                mUrlText += ENDING;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    }

    public void setOnAddAccountListener(OnAddAccountListener onAddAccountListener) {
        mOnAddAccountListener = onAddAccountListener;
    }

    public interface OnAddAccountListener {
        void onAddAccount(Account account, String token, boolean accountAdded);
    }

    public static class HTTPLoader extends AsyncTaskLoader<String> {
        final String mUrl;
        final String mLogin;
        final String mPassword;
        String mAuthToken;

        HTTPLoader(Context context, String url, String login, String password) {
            super(context);
            mUrl = url;
            mLogin = login;
            mPassword = password;
        }

        @Override
        protected void onStartLoading() {
            if (mAuthToken == null || mAuthToken.length() == 0) {
                forceLoad();
            } else {
                deliverResult(mAuthToken);
            }
        }

        @Override
        public void deliverResult(String data) {
            mAuthToken = data;
            super.deliverResult(data);
        }

        @Override
        public String loadInBackground() {
            try {
                return signIn();
            } catch (IOException ignored) {}

            return "0";
        }

        String signIn() throws IOException {
            String url = mUrl.trim();
            if (!url.startsWith("http"))
                url = "http://" + url;

            try {
                return NGWUtil.getConnectionCookie(url, mLogin, mPassword);
            } catch (IllegalArgumentException | IllegalStateException e) {
                e.printStackTrace();
                return "0";
            }
        }
    }
}