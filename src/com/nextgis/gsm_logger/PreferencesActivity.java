package com.nextgis.gsm_logger;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		getPreferenceManager().setSharedPreferencesName(MainActivity.PREFERENCE_NAME);
		addPreferencesFromResource(R.xml.preferences);
	}
}
