package com.nextgis.logger;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_activity);
		String verName, verCode;
		verName = verCode = "?";

		try {
			String pkg = getPackageName();
			verName = getPackageManager().getPackageInfo(pkg, 0).versionName;
			verCode = getPackageManager().getPackageInfo(pkg, 0).versionCode + "";
		} catch (NameNotFoundException e) {
		}

		((TextView) findViewById(R.id.tv_about_app_ver)).setText("v. " + verName + " (rev. " + verCode + ")");
		((TextView) findViewById(R.id.tv_about_gpl)).setMovementMethod(LinkMovementMethod.getInstance());
		((ImageView) findViewById(R.id.iv_about_logo)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent nextgis = new Intent("android.intent.action.VIEW", Uri.parse(getString(R.string.about_nextgis_url)));
				startActivity(nextgis);
			}
		});
	}
}
