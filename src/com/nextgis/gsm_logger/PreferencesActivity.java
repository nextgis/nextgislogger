package com.nextgis.gsm_logger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	public static final int minPeriodSec = 1;
	public static final int maxPeriodSec = 3600;

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		getPreferenceManager().setSharedPreferencesName(MainActivity.PREFERENCE_NAME);
		addPreferencesFromResource(R.xml.preferences);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD
				|| ((SensorManager) this.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null)
			findPreference(MainActivity.PREF_SENSOR_MODE).setEnabled(false);
		
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
			findPreference(MainActivity.PREF_USE_API17).setEnabled(false);

		IntEditTextPreference period = (IntEditTextPreference) findPreference(MainActivity.PREF_PERIOD_SEC);
		period.setSummary(getString(R.string.settings_period_sum) + period.getPersistedString("1"));
		findPreference(MainActivity.PREF_PERIOD_SEC).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int period;

				try {
					period = Integer.parseInt((String) newValue);
					boolean max = period > maxPeriodSec;
					boolean min = period < minPeriodSec;

					if (max)
						period = maxPeriodSec;

					if (min)
						period = minPeriodSec;

					((IntEditTextPreference) preference).persistString(Integer.toString(period));
					preference.setSummary(getString(R.string.settings_period_sum) + period);

					if (min || max)
						throw new IllegalArgumentException();
				} catch (Exception e) {
					Toast.makeText(getBaseContext(), R.string.settings_period_toast, Toast.LENGTH_LONG).show();
				}

				return false;
			}
		});
	}
}
