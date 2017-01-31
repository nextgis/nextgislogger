/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2015, 2017 NextGIS
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
package com.nextgis.logger.ui.activity;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.nextgis.logger.R;

public class AboutActivity extends ProgressBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mHasFAB = false;
		setContentView(R.layout.activity_about);

		String verName, verCode;
		verName = verCode = "?";

		try {
			String pkg = getPackageName();
			verName = getPackageManager().getPackageInfo(pkg, 0).versionName;
			verCode = getPackageManager().getPackageInfo(pkg, 0).versionCode + "";
		} catch (NameNotFoundException ignored) {
		}

		((TextView) findViewById(R.id.tv_about_app_ver)).setText("v. " + verName + " (rev. " + verCode + ")");
		((TextView) findViewById(R.id.tv_about_gpl)).setMovementMethod(LinkMovementMethod.getInstance());
		findViewById(R.id.iv_about_logo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nextgis = new Intent("android.intent.action.VIEW", Uri.parse(getString(R.string.about_nextgis_url)));
                startActivity(nextgis);
            }
        });
	}
}
