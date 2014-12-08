package com.nextgis.gsm_logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	public static final int minPeriodSec = 1;
	public static final int maxPeriodSec = 3600;

	private final String NO_FILE = "No file selected"; // FIXME hardcoded

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		getPreferenceManager().setSharedPreferencesName(C.PREFERENCE_NAME);
		addPreferencesFromResource(R.xml.preferences);
		final Activity parent = this;

		if (((SensorManager) this.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null)
			findPreference(C.PREF_SENSOR_MODE).setEnabled(false);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
			findPreference(C.PREF_USE_API17).setEnabled(false);

		EditTextPreference userName = (EditTextPreference) findPreference(C.PREF_USER_NAME);
		userName.setSummary(userName.getText());
		userName.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String) newValue);

				return true;
			}
		});

		IntEditTextPreference periodPreference = (IntEditTextPreference) findPreference(C.PREF_PERIOD_SEC);
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

		Preference catPathPreference = findPreference(C.PREF_CAT_PATH);

		catPathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				SimpleFileDialog FileOpenDialog = new SimpleFileDialog(parent, new SimpleFileDialog.SimpleFileDialogListener() {
					@Override
					public void onChosenDir(String chosenDir) {
						// The code in this function will be executed when the dialog OK button is pushed
						String info = NO_FILE;

						if (!new File(chosenDir).isFile())
							info = "No such file"; // FIXME hardcoded string
						else {
							File fromCats = new File(chosenDir);

							String internalPath = getFilesDir().getAbsolutePath();
							File toCats = new File(internalPath + "/" + C.categoriesFile);

							try {
								PrintWriter pw = new PrintWriter(new FileOutputStream(toCats, false));
								BufferedReader in = new BufferedReader(new FileReader(fromCats));

								String[] split;
								String line;

								while ((line = in.readLine()) != null) {
									split = line.split(",");

									if (split.length != 2) {
										in.close();
										pw.close();
										throw new ArrayIndexOutOfBoundsException("Must be two columns splitted by ','!");
									} else
										pw.println(line);
								}

								in.close();
								pw.close();

								info = "Loaded from " + chosenDir;
							} catch (IOException e) {
								info = getString(R.string.fs_error_msg);
							} catch (ArrayIndexOutOfBoundsException e) {
								info = getString(R.string.cat_file_structure_error);
							}
						}

						//						preference.getEditor().putString(preference.getKey(), chosenDir).commit();

						Toast.makeText(parent, info, Toast.LENGTH_SHORT).show();
					}
				});

				FileOpenDialog.chooseFile_or_Dir();

				return true;
			}
		});

		//		String selectedFile = catPathPreference.getSharedPreferences().getString(catPathPreference.getKey(), "");
		//		
		//		if (!new File(selectedFile).isFile())
		//			selectedFile = NO_FILE;
		//		
		//		catPathPreference.setSummary(selectedFile);
	}
}
