/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
 * Copyright © 2014-2015 NextGIS
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
 *****************************************************************************/
package com.nextgis.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MarkActivity extends ProgressBarActivity implements View.OnClickListener {
	private static final int DELAY = 4000;

	private static final int MARK_SAVE = 0;
	private static final int MARK_UNDO = 1;

	private static final String BUNDLE_CELL     = "data_network";
	private static final String BUNDLE_SENSOR   = "data_sensors";

    private static int marksCount = 0;
    private boolean mIsHot, mIsVolumeControlEnabled;

    private MenuItem searchBox;
    private ListView lvCategories;

	private CustomArrayAdapter substringMarkNameAdapter;

	private CellEngine gsmEngine;
	private SensorEngine sensorEngine;
//	private WiFiEngine wifiEngine;

	private SharedPreferences prefs;
    static MarksHandler marksHandler;
    private static int mSavedMarkPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mIsVolumeControlEnabled = prefs.getBoolean(C.PREF_USE_VOL, true) && prefs.getBoolean(C.PREF_USE_CATS, false);

        if(prefs.getBoolean(C.PREF_KEEP_SCREEN, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.mark_activity);

        mFAB.setOnClickListener(this);

		gsmEngine = new CellEngine(this);
		sensorEngine = new SensorEngine(this);
//		wifiEngine = new WiFiEngine(this);

        mSavedMarkPosition = prefs.getInt(C.PREF_MARK_POS, Integer.MIN_VALUE);
        marksCount = prefs.getInt(C.PREF_MARKS_COUNT, 0);
        marksHandler = new MarksHandler(this);

        lvCategories = (ListView) findViewById(R.id.lv_categories);
        lvCategories.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MarkName match = substringMarkNameAdapter.getMatchedMarkItem(position);
                saveMark(match);
			}
		});

        lvCategories.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (view.getLastVisiblePosition() == view.getAdapter().getItemId(view.getAdapter().getCount() - 1))
                        return;

                    int firstPosition = view.getFirstVisiblePosition();

                    // http://stackoverflow.com/a/15339238/2088273
                    View firstChild = view.getChildAt(0);    // first visible child
                    Rect r = new Rect(0, 0, firstChild.getWidth(), firstChild.getHeight());     // set this initially, as required by the docs
                    double height = firstChild.getHeight () * 1.0;

                    view.getChildVisibleRect(firstChild, r, null);

                    if (Math.abs(r.height()) < height / 2.0)
                        firstPosition++;

                    view.setSelection(firstPosition);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

		List<MarkName> markNames = new ArrayList<>();

		if (prefs.getBoolean(C.PREF_USE_CATS, false)) {
			String internalPath = getFilesDir().getAbsolutePath();
			File cats = new File(internalPath + "/" + C.categoriesFile);

			if (cats.isFile()) {
				BufferedReader in;
				String[] split;

				try {
					in = new BufferedReader(new FileReader(cats));
					String line;
                    in.readLine();  // skip header "ID,NAME"

					while ((line = in.readLine()) != null) {
						split = line.split(",");	// FIXME dup preferences
						markNames.add(new MarkName(Integer.parseInt(split[0]), split[1]));
					}

					in.close();

					if (markNames.size() == 0)
						throw new ArrayIndexOutOfBoundsException();
				} catch (IOException e) {
					Toast.makeText(this, R.string.fs_error_msg, Toast.LENGTH_SHORT).show();
				} catch (ArrayIndexOutOfBoundsException e) {
					Toast.makeText(this, R.string.cat_file_error, Toast.LENGTH_SHORT).show();
				} catch (NumberFormatException e) {
					Toast.makeText(this, R.string.cat_id_not_int, Toast.LENGTH_SHORT).show();
				}
			}
		}

        Collections.sort(markNames, new Comparator<MarkName>() {
            @Override
            public int compare(MarkName lhs, MarkName rhs) {
                return lhs.getID() - rhs.getID();
            }
        });

		substringMarkNameAdapter = new CustomArrayAdapter(this, markNames);
		lvCategories.setAdapter(substringMarkNameAdapter);
        lvCategories.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

//        if (mFAB != null)
//            mFAB.attachToListView(lvCategories);
	}

    protected void setFABIcon(boolean restore) {
        if (mFAB.isVisible())
            mFAB.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotation));

        mFAB.setImageResource(restore ? R.drawable.ic_insert_chart_white_24dp : R.drawable.ic_undo_white_24dp);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                if (mIsHot) {
                    marksCount--;
                    marksHandler.sendEmptyMessage(MARK_UNDO);
                    Toast.makeText(this, R.string.mark_undo, Toast.LENGTH_SHORT).show();
                    break;
                }
            default:
                super.onClick(v);
                break;
        }
    }

	@Override
	protected void onResume() {
		super.onResume();

		gsmEngine.onResume();
		sensorEngine.onResume();
//		wifiEngine.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();

		gsmEngine.onPause();
		sensorEngine.onPause();
//		wifiEngine.onPause();

		prefs.edit().putInt(C.PREF_MARKS_COUNT, marksCount).apply();
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mark, menu);

		searchBox = menu.findItem(R.id.search);
		SearchView search = (SearchView) searchBox.getActionView();
		search.setQueryHint(getString(R.string.mark_editor_hint));
//		search.setIconifiedByDefault(true);	// set icon inside edit text view
//		search.setIconified(false);	// expand search view in action bar

//		search.setOnCloseListener(new OnCloseListener() {
//			@Override
//			public boolean onClose() {
//				return true;	// prevent collapse search view
//			}
//		});

//		search.requestFocus();
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

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        int position;

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mIsVolumeControlEnabled) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        position = mSavedMarkPosition - 1;
                        break;
                    } else
                        return true;
                }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mIsVolumeControlEnabled) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        position = mSavedMarkPosition + 1;
                        break;
                    } else
                        return true;
                }
            default:
                return super.dispatchKeyEvent(event);
        }

        if (substringMarkNameAdapter.hasItem(position)) {
            MarkName mark = substringMarkNameAdapter.getMarkItem(position);
            position = substringMarkNameAdapter.getMatchedMarkPosition(mark);

            lvCategories.setItemChecked(position, true);
            lvCategories.setSelection(position);

            saveMark(mark);
        } else
            Toast.makeText(this, R.string.mark_no_items, Toast.LENGTH_SHORT).show();

        return true;
    }

    private void saveMark(MarkName mark) {
        if (mIsHot)
            return;

        FileUtil.checkOrCreateDirectory(MainActivity.dataDirPath);

        final Bundle data = new Bundle();

        data.putInt(C.PREF_MARK_POS, substringMarkNameAdapter.getMarkPosition(mark));
        String markName = mark.getCAT();
        String ID = mark.getID() + "";

        if (markName.length() == 0) {	// FIXME looks like doesn't need anymore
            markName = C.markDefaultName;
        }

        String info = String.format(getString(R.string.mark_saved), markName);

        ArrayList<CellEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();
        ArrayList<String> items = new ArrayList<>();
        String userName = prefs.getString(C.PREF_USER_NAME, C.DEFAULT_USERNAME);

        for (CellEngine.GSMInfo gsmInfo : gsmInfoArray) {
            String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
                    + gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();

            items.add(CellEngine.getItem(gsmInfo, active, ID, markName, userName));
        }

        data.putStringArrayList(BUNDLE_CELL, items);
        info += " (" + ++marksCount + ")";

        // checking sensors state
        if (sensorEngine.isAnySensorEnabled()) {
            data.putString(BUNDLE_SENSOR, SensorEngine.getItem(sensorEngine, ID, markName, userName, gsmInfoArray.get(0).getTimeStamp()));
        }

        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibe.vibrate(100);
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        mIsHot = true;

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(searchBox.getActionView().getWindowToken(), 0);
            lvCategories.requestFocus();
//                    searchBox.collapseActionView();
        }

        setFABIcon(false);
//        mFAB.show();

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = MARK_SAVE;
                msg.setData(data);
                marksHandler.sendMessage(msg);
            }
        }, DELAY);
    }

    protected void setIsHot(boolean isHot) {
        mIsHot = isHot;
    }

    public class MarkName {
		private int ID = -1;
		private String CAT = "Mark";

		public MarkName(int ID, String CAT) {
			this.ID = ID;
			this.CAT = CAT;
		}

		public int getID() {
			return ID;
		}

		public String getCAT() {
			return CAT;
		}
	}

	public class CustomArrayAdapter extends ArrayAdapter<String> implements Filterable {
		private List<MarkName> objects;
		private ArrayList<MarkName> matches = new ArrayList<>();
		private final CustomFilter substringFilter = new CustomFilter();
		private MarkName temp;

		public CustomArrayAdapter(final Context ctx, final List<MarkName> objects) {
			super(ctx, android.R.layout.simple_list_item_activated_1, android.R.id.text1);
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

        public int getMarkPosition(MarkName item) {
            return objects.indexOf(item);
        }

        public int getMatchedMarkPosition(MarkName item) {
            return matches.indexOf(item);
        }

		public MarkName getMatchedMarkItem(int position) {
            if (position >= 0 && position < matches.size())
			    return matches.get(position);
            else
                return new MarkName(-1, "null");
		}

		public MarkName getMarkItem(int position) {
            if (hasItem(position))
			    return objects.get(position);
            else
                return new MarkName(-1, "null");
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return super.getView(position, convertView, parent);
		}

        public boolean hasItem(int position) {
            return position >= 0 && position < objects.size();
        }

		private class CustomFilter extends Filter {
			@Override
			protected FilterResults performFiltering(final CharSequence prefix) {
				final FilterResults results = new FilterResults();

				if (prefix != null) {
					ArrayList<MarkName> resultList = new ArrayList<>();

					for (MarkName item : objects) {
						String CAT = getUpperString(item.getCAT());
						String substr = getUpperString(prefix.toString());

						if (CAT.contains(substr))
							resultList.add(item);
					}

					results.count = resultList.size();
					results.values = resultList;

					if (resultList.size() != 1 && !prefix.equals("")) {
						temp = new MarkName(-1, prefix.toString());
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

    private static class MarksHandler extends Handler {
        private long mUndoTimeStamp = 0;
        private MarkActivity mParent;

        public MarksHandler(MarkActivity parent) {
            mParent = parent;
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MARK_SAVE:
                    if (Math.abs(System.currentTimeMillis() - mUndoTimeStamp) >= DELAY) {
                        try {
                            for (String item : msg.getData().getStringArrayList(BUNDLE_CELL))
                                FileUtil.saveItemToLog(C.LOG_TYPE_NETWORK, true, item);

                            String sensorItem = msg.getData().getString(BUNDLE_SENSOR);

                            if (!TextUtils.isEmpty(sensorItem))
                                FileUtil.saveItemToLog(C.LOG_TYPE_SENSORS, true, sensorItem);

                            mSavedMarkPosition = msg.getData().getInt(C.PREF_MARK_POS);
                            PreferenceManager.getDefaultSharedPreferences(mParent).edit().putInt(C.PREF_MARK_POS, mSavedMarkPosition).apply();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else
                        break;
                case MARK_UNDO:
                    mParent.setFABIcon(true);
                    mParent.setIsHot(false);
                    mUndoTimeStamp = System.currentTimeMillis();
                    break;
            }
        }
    }
}
