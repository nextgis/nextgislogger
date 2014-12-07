package com.nextgis.gsm_logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

public class MarkActivity extends Activity {
	private static int marksCount = 0;
	
	MenuItem searchBox;
	ListView lvCategories;
	
	CustomArrayAdapter substringMarkNameAdapter;

	private GSMEngine gsmEngine;
	private SensorEngine sensorEngine;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mark_activity);
		
		gsmEngine = new GSMEngine(this);

		if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER))
			sensorEngine = new SensorEngine(this);

		lvCategories = (ListView) findViewById(R.id.lv_categories);
		final Activity base = this;
		
		lvCategories.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String info = getString(R.string.mark_saved);
				
				try {
					File csvFile = new File(C.csvMarkFilePath);
					boolean isFileExist = csvFile.exists();
					PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

					if (!isFileExist)
						pw.println(C.csvMarkHeader);

					String markName = substringMarkNameAdapter.getItem(position);
					String ID = substringMarkNameAdapter.getSelectedMarkNameID(markName);

					if (markName.length() == 0) {	// FIXME looks like doesn't need anymore
						markName = C.markDefaultName;
					}

					ArrayList<GSMEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();
					String userName = getSharedPreferences(C.PREFERENCE_NAME, MODE_PRIVATE).getString(C.PREF_USER_NAME, "User 1");

					for (GSMEngine.GSMInfo gsmInfo : gsmInfoArray) {
						StringBuilder sb = new StringBuilder();

						String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
								+ gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();

						sb.append(ID).append(C.CSV_SEPARATOR);
						sb.append(markName).append(C.CSV_SEPARATOR);
						sb.append(userName).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getTimeStamp()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.networkGen()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.networkType()).append(C.CSV_SEPARATOR);
						sb.append(active).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getMcc()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getMnc()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getLac()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getCid()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getPsc()).append(C.CSV_SEPARATOR);
						sb.append(gsmInfo.getRssi());

						pw.println(sb.toString());
					}

					pw.close();
					info += " (" + ++marksCount + ")";

					Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(csvFile));
			    	sendBroadcast(intent);	// update media for MTP

					// checking accelerometer data state
					if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER)) {
						csvFile = new File(C.csvMarkFilePathSensor);
						isFileExist = csvFile.exists();
						pw = new PrintWriter(new FileOutputStream(csvFile, true));

						if (!isFileExist)
							pw.println(C.csvHeaderSensor);

						StringBuilder sb = new StringBuilder();

						sb.append(ID).append(C.CSV_SEPARATOR);
						sb.append(markName).append(C.CSV_SEPARATOR);
						sb.append(userName).append(C.CSV_SEPARATOR);
						sb.append(gsmInfoArray.get(0).getTimeStamp()).append(C.CSV_SEPARATOR);
						sb.append(sensorEngine.getSensorType()).append(C.CSV_SEPARATOR);
						sb.append(sensorEngine.getX()).append(C.CSV_SEPARATOR);
						sb.append(sensorEngine.getY()).append(C.CSV_SEPARATOR);
						sb.append(sensorEngine.getZ());

						pw.println(sb.toString());
						pw.close();

						intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(csvFile));
				    	sendBroadcast(intent);	// update media for MTP
					}
				} catch (FileNotFoundException e) {
//					setInterfaceState(R.string.fs_error_msg, true);
					info = getString(R.string.fs_error_msg);
				}
				
				Toast.makeText(base, info, Toast.LENGTH_SHORT).show();
			}
		});
		
		List<MarkName> markNames = new ArrayList<MarkName>();
		
		if (getSharedPreferences(C.PREFERENCE_NAME, MODE_PRIVATE).getBoolean(C.PREF_USE_CATS, false)) {
			String internalPath = getFilesDir().getAbsolutePath();
			File cats = new File(internalPath + "/" + C.CAT_FILE);

			if (cats.isFile()) {
				BufferedReader in;
				String[] split;

				try {
					in = new BufferedReader(new FileReader(cats));
					String line = in.readLine();	// skip header "ID,NAME"

					while ((line = in.readLine()) != null) {
						split = line.split(",");	// FIXME dup preferences
						markNames.add(new MarkName(split[0], split[1]));
					}

					in.close();

					if (markNames.size() == 0)
						throw new ArrayIndexOutOfBoundsException();
				} catch (IOException e) {
					Toast.makeText(this, R.string.fs_error_msg, Toast.LENGTH_SHORT).show();
				} catch (ArrayIndexOutOfBoundsException e) {
					Toast.makeText(this, R.string.cat_file_error, Toast.LENGTH_SHORT).show();
				}
			}
		}
		
		substringMarkNameAdapter = new CustomArrayAdapter(this, markNames);
		lvCategories.setAdapter(substringMarkNameAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		gsmEngine.onResume();

		if (isSensorEnabled(Sensor.TYPE_ACCELEROMETER))
			if (sensorEngine != null)
				sensorEngine.onResume(this);
			else
				sensorEngine = new SensorEngine(this);
		else
			sensorEngine = null;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		gsmEngine.onPause();

		if (sensorEngine != null)
			sensorEngine.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mark, menu);

		searchBox = menu.findItem(R.id.search);
		SearchView search = (SearchView) searchBox.getActionView();
		search.setQueryHint(getString(R.string.mark_editor_hint));
		search.setIconifiedByDefault(true);	// set icon inside edit text view
		search.setIconified(false);	// expand search view in action bar
		
		search.setOnCloseListener(new OnCloseListener() {
			@Override
			public boolean onClose() {
				return true;	// prevent collapse search view
			}
		});
		
		search.requestFocus();
		search.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				substringMarkNameAdapter.getFilter().filter(newText);
				return true;
			}
		});

		return true;
	}

	private boolean isSensorEnabled(int sensorType) {
		switch (sensorType) {
		case Sensor.TYPE_ACCELEROMETER:
			return getSharedPreferences(C.PREFERENCE_NAME, MODE_PRIVATE).getBoolean(C.PREF_SENSOR_STATE, true);
		}
		
		return false;
	}
	
	public class MarkName {
		private String ID = "";
		private String CAT = "Mark";

		public MarkName(String ID, String CAT) {
			this.ID = ID;
			this.CAT = CAT;
		}

		public String getID() {
			return ID;
		}

		public String getCAT() {
			return CAT;
		}
	}

	public class CustomArrayAdapter extends ArrayAdapter<String> implements Filterable {
		private List<MarkName> objects;
		private ArrayList<MarkName> matches = new ArrayList<MarkName>();;
		private final CustomFilter substringFilter = new CustomFilter();
		private MarkName temp;

		public CustomArrayAdapter(final Context ctx, final List<MarkName> objects) {
			super(ctx, android.R.layout.simple_list_item_1, android.R.id.text1);
			this.objects = objects;
			substringFilter.filter("");
		}

		@Override
		public Filter getFilter() {
			return substringFilter;
		}

		@Override
		public int getCount() {
			return matches.size();
		}

		@Override
		public String getItem(int position) {
			return matches.get(position).getCAT();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return super.getView(position, convertView, parent);
		}
		
		public String getSelectedMarkNameID(String prefix) {
			for (MarkName item : objects) {
				String CAT = substringFilter.getUpperString(item.getCAT());

				if (CAT.equals(substringFilter.getUpperString(prefix)))
					return item.getID();
			}

			return "";
		}

		private class CustomFilter extends Filter {
			@Override
			protected FilterResults performFiltering(final CharSequence prefix) {
				final FilterResults results = new FilterResults();

				if (prefix != null) {
					ArrayList<MarkName> resultList = new ArrayList<MarkName>();

					for (MarkName item : objects) {
						String CAT = getUpperString(item.getCAT());
						String substr = getUpperString(prefix.toString());

						if (CAT.contains(substr))
							resultList.add(item);
					}

					results.count = resultList.size();
					results.values = resultList;
					
					if (resultList.size() != 1 && !prefix.equals("")) {
						temp = new MarkName("", prefix.toString());
						results.count++;
//						resultList.add(temp);
					}
					else
						temp = null;
				}

				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(final CharSequence constraint, final FilterResults results) {
				matches.clear();
				
				if (results != null && results.count > 0) {
					if (temp != null)
						matches.add(temp);
					
					matches.addAll((ArrayList<MarkName>) results.values);
					notifyDataSetChanged();
				} else
					notifyDataSetInvalidated();
			}

			public String getUpperString(String str) {
				return str.toUpperCase(Locale.getDefault());
			}
		}
	}

}
