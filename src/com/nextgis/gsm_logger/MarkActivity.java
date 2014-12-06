package com.nextgis.gsm_logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

public class MarkActivity extends Activity {
	MenuItem searchBox;
	ListView lvCategories;
	
	CustomArrayAdapter substringMarkNameAdapter;

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mark_activity);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			getActionBar().setHomeButtonEnabled(true);

		lvCategories = (ListView) findViewById(R.id.lv_categories);
		
		lvCategories.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				
			}
		});
		
		List<MarkName> markNames = new ArrayList<MarkName>();
		
		if (getSharedPreferences(MainActivity.PREFERENCE_NAME, MODE_PRIVATE).getBoolean(MainActivity.PREF_USE_CATS, false)) {
			String internalPath = getFilesDir().getAbsolutePath();
			File cats = new File(internalPath + "/" + MainActivity.CAT_FILE);

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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mark, menu);

		searchBox = menu.findItem(R.id.search);
		SearchView search = (SearchView) searchBox.getActionView();
		search.setQueryHint(getString(R.string.mark_editor_hint));
		search.setIconifiedByDefault(true);	// set icon inside edit text view
		search.setIconified(false);	// expand search view in action bar

		int closeButtonId = getResources().getIdentifier("android:id/search_close_btn", null, null);
		search.findViewById(closeButtonId).setVisibility(View.GONE);	// prevent collapse search view
		
		search.requestFocus();
		search.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				substringMarkNameAdapter.getFilter().filter(newText);
				return false;
			}
		});

		return true;
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
