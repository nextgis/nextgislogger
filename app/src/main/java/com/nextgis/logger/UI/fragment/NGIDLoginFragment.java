/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2017 NextGIS
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.logger.R;
import com.nextgis.logger.util.NGIDUtils;
import com.nextgis.logger.util.UiUtil;
import com.nextgis.maplib.util.NetworkUtil;

public class NGIDLoginFragment extends Fragment implements View.OnClickListener {
    protected EditText mLogin, mPassword;
    protected Button mSignInButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == getParentFragment())
            setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_ngid_login, container, false);
        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);
        mSignInButton = (Button) view.findViewById(R.id.signin);
        mSignInButton.setOnClickListener(this);
        TextView signUp = (TextView) view.findViewById(R.id.signup);
        UiUtil.highlightText(signUp);
        signUp.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.signin) {
            boolean loginPasswordFilled = checkEditText(mLogin) && checkEditText(mPassword);
            if (!loginPasswordFilled) {
                Toast.makeText(getActivity(), R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            mSignInButton.setEnabled(false);
            final Activity activity = getActivity();
            NGIDUtils.getToken(activity, mLogin.getText().toString(), mPassword.getText().toString(), new NGIDUtils.OnFinish() {
                @Override
                public void onFinish(String data) {
                    mSignInButton.setEnabled(true);

                    if (data == null)
                        activity.finish();
                    else
                        Toast.makeText(activity, NetworkUtil.getError(activity, data), Toast.LENGTH_SHORT).show();
                }
            });
        } else if (v.getId() == R.id.signup) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("http://my.nextgis.com"));
            startActivity(browser);
        }
    }

    private boolean checkEditText(EditText edit) {
        return edit.getText().length() > 0;
    }

}
