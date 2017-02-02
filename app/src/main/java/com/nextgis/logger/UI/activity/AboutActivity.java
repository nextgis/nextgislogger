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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.logger.BuildConfig;
import com.nextgis.logger.R;
import com.nextgis.logger.ui.view.SlidingTabLayout;
import com.nextgis.logger.util.UiUtil;

public class AboutActivity extends ProgressBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHasFAB = false;
		setContentView(R.layout.activity_about);

		PagerAdapter adapter = new TabsAdapter(getSupportFragmentManager());
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
		viewPager.setAdapter(adapter);

		SlidingTabLayout strips = (SlidingTabLayout) findViewById(R.id.strips);
		strips.setDistributeEvenly(true);
		strips.setViewPager(viewPager);

		TextView txtCopyrightText = (TextView) findViewById(R.id.copyright);
		txtCopyrightText.setText(Html.fromHtml(getString(R.string.copyright)));
		txtCopyrightText.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private class TabsAdapter extends FragmentPagerAdapter {
		TabsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return new ContactsFragment();
				case 1:
					return new AboutFragment();
			}

			return new Fragment();
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return getString(R.string.app_support);
				case 1:
					return getString(R.string.app_about);
			}

			return getString(R.string.app_about);
		}
	}

	public static class AboutFragment extends Fragment {
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
			Context context = getContext();
			View v = View.inflate(context, R.layout.fragment_about, null);

			TextView txtVersion = (TextView) v.findViewById(R.id.app_version);
			txtVersion.setText("v. " + BuildConfig.VERSION_NAME + " (rev. " + BuildConfig.VERSION_CODE + ")");

			v.findViewById(R.id.credits_into).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog builder = new AlertDialog.Builder(v.getContext())
							.setTitle(R.string.credits_intro)
							.setMessage(R.string.credits)
							.setPositiveButton(android.R.string.ok, null)
							.show();

					TextView message = (TextView) builder.findViewById(android.R.id.message);
					if (message != null) {
						message.setMovementMethod(LinkMovementMethod.getInstance());
						message.setLinksClickable(true);
					}
				}
			});

			return v;
		}
	}

	public static class ContactsFragment extends Fragment {
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
			final Context context = getContext();
			final View v = View.inflate(context, R.layout.fragment_contacts, null);

			TextView telegram = (TextView) v.findViewById(R.id.telegram);
			UiUtil.highlightText(telegram);
			telegram.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						Intent telegram = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=nextgis_support"));
						startActivity(telegram);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(context, R.string.not_installed, Toast.LENGTH_SHORT).show();
					}
				}
			});

			return v;
		}
	}
}
