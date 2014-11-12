package com.nextgis.gsm_logger;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	public static final int minPeriodSec = 1;
	public static final int maxPeriodSec = 3600;
	
	private final String NO_FILE = "No file selected";

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		getPreferenceManager().setSharedPreferencesName(MainActivity.PREFERENCE_NAME);
		addPreferencesFromResource(R.xml.preferences);
		final Activity parent = this;

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD
				|| ((SensorManager) this.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null)
			findPreference(MainActivity.PREF_SENSOR_MODE).setEnabled(false);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
			findPreference(MainActivity.PREF_USE_API17).setEnabled(false);

		IntEditTextPreference periodPreference = (IntEditTextPreference) findPreference(MainActivity.PREF_PERIOD_SEC);
		periodPreference.setSummary(getString(R.string.settings_period_sum) + periodPreference.getPersistedString("1"));
		periodPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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

		Preference catPathPreference = findPreference(MainActivity.PREF_CAT_PATH);
		
		catPathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				SimpleFileDialog FileOpenDialog = new SimpleFileDialog(parent, new SimpleFileDialog.SimpleFileDialogListener() {
					@Override
					public void onChosenDir(String chosenDir) {
						// The code in this function will be executed when the dialog OK button is pushed
						String summary = NO_FILE;

						if (!new File(chosenDir).isFile())
							Toast.makeText(parent, "No such file", Toast.LENGTH_SHORT).show();
						else
							summary = chosenDir;
						
						preference.getEditor().putString(preference.getKey(), chosenDir).commit();
						
						preference.setSummary(summary);
					}
				});
				
				FileOpenDialog.chooseFile_or_Dir();
				
				return true;
			}
		});
		
		String selectedFile = catPathPreference.getSharedPreferences().getString(catPathPreference.getKey(), "");
		
		if (!new File(selectedFile).isFile())
			selectedFile = NO_FILE;
	}
}
