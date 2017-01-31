/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2014-2017 NextGIS
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
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.net.Uri;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.R;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.GPSEngine;
import com.nextgis.logger.engines.InfoItem;
import com.nextgis.logger.util.FileUtil;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.MarkName;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MarkActivity extends BindActivity implements View.OnClickListener {
	private static final int DELAY = 4000;

	private static final int MARK_SAVE = 0;
	private static final int MARK_UNDO = 1;

	private static final String BUNDLE_CELL     = "data_network";
	private static final String BUNDLE_SENSOR   = "data_sensors";
	private static final String BUNDLE_EXTERNAL = "data_external";
	private static final String BUNDLE_SESSION  = "session";
	private static final String BUNDLE_TIME     = "time";
	private static final String BUNDLE_ID       = "mark_id";
	private static final String BUNDLE_NAME     = "mark_name";

    private SearchView mSearchView;
    private MenuItem mSearchBox, mBtRetry;
    private ListView mLvCategories;
    private MarkArrayAdapter mMarksAdapter;
    private MarksHandler mMarksHandler;

    private boolean mIsHot, mIsVolumeControlEnabled, mIsActive, mLastConnection;
    private int mSavedMarkPosition;
    private int mMarksCount = 0;
    private Uri mUri;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mIsVolumeControlEnabled = mPreferences.getBoolean(LoggerConstants.PREF_USE_VOL, true);

        if(mPreferences.getBoolean(LoggerConstants.PREF_KEEP_SCREEN, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_mark);

        mFAB.setOnClickListener(this);

        mSavedMarkPosition = mPreferences.getInt(LoggerConstants.PREF_MARK_POS, Integer.MIN_VALUE);
        mMarksCount = mPreferences.getInt(LoggerConstants.PREF_MARKS_COUNT, 0);
        mMarksHandler = new MarksHandler(this);

        mLvCategories = (ListView) findViewById(R.id.lv_categories);
        mLvCategories.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MarkName match = mMarksAdapter.getMatchedMarkItem(position);
                saveMark(match);
            }
        });
        mLvCategories.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                editMark(mMarksAdapter.getMarkItem(position));
                return false;
            }
        });

        mLvCategories.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (view.getLastVisiblePosition() == view.getAdapter().getItemId(view.getAdapter().getCount() - 1))
                        return;

                    int firstPosition = view.getFirstVisiblePosition();

                    // http://stackoverflow.com/a/15339238/2088273
                    View firstChild = view.getChildAt(0);    // first visible child
                    Rect r = new Rect(0, 0, firstChild.getWidth(), firstChild.getHeight());     // set this initially, as required by the docs
                    double height = firstChild.getHeight() * 1.0;

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

        // load marks from presets to ListView
		List<MarkName> markNames = new ArrayList<>();
        File cats = FileUtil.getCategoriesFile(this);
        FileUtil.loadMarksFromPreset(this, cats, markNames);
		mMarksAdapter = new MarkArrayAdapter(this, markNames);
		mLvCategories.setAdapter(mMarksAdapter);
        mLvCategories.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        mUri = Uri.parse("content://" + ((IGISApplication) getApplicationContext()).getAuthority());
        mUri = mUri.buildUpon().appendPath(LoggerApplication.TABLE_MARK).build();
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
                    mMarksCount--;
                    mMarksHandler.sendEmptyMessage(MARK_UNDO);
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
        mIsActive = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
        mIsActive = false;
		mPreferences.edit().putInt(LoggerConstants.PREF_MARKS_COUNT, mMarksCount).apply();
	}

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.mark, menu);
        mBtRetry = menu.findItem(R.id.bt_lost);
		mSearchBox = menu.findItem(R.id.search);

        mSearchView = (SearchView) mSearchBox.getActionView();
        mSearchView.setQueryHint(getString(R.string.mark_editor_hint));

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setItemsVisibility(menu, mSearchBox, false);
                mSearchView.requestFocus();
            }
        });

		mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setItemsVisibility(menu, mSearchBox, true);
                return false;    // true = prevent collapse mSearchView
            }
        });

		mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mMarksAdapter.getFilter().filter(newText);
                return true;
            }
        });

		return true;
	}

    // http://stackoverflow.com/q/30577252/2088273
    private void setItemsVisibility(final Menu menu, final MenuItem exception, final boolean visible) {
        int flags = visible ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (item != exception)
                item.setShowAsAction(flags);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isArduinoAvailable = mArduinoEngine != null;
        boolean isEngineActive = isArduinoAvailable && mArduinoEngine.isEngineEnabled();
        boolean isEngineOk = isArduinoAvailable && mArduinoEngine.isConnected() && mArduinoEngine.isDeviceAvailable();
        mBtRetry.setVisible(isEngineActive && !isEngineOk);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_lost:
                if (mArduinoEngine != null && mArduinoEngine.isDeviceAvailable() && mArduinoEngine.isConnected())
                    mBtRetry.setVisible(false);
                return true;
            case R.id.new_mark:
                editMark(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void editMark(final MarkName mark) {
        final boolean edit = mark != null;
        AlertDialog.Builder markDialog = new AlertDialog.Builder(this);
        markDialog.setTitle(getString(edit ? R.string.mark_edit : R.string.mark_new));
        View layout = View.inflate(this, R.layout.dialog_edit_mark, null);

        final EditText etID = (EditText) layout.findViewById(R.id.et_mark_id);
        int id = mMarksAdapter.getMarkItem(mMarksAdapter.getTotalCount() - 1).getId() + 1;
        id = edit ? mark.getId() : id;
        etID.setText(id + "");

        final EditText etName = (EditText) layout.findViewById(R.id.et_mark_name);
        etName.setText(edit ? mark.getCat() : "");
        etName.requestFocus();

        final CheckBox cbSave = (CheckBox) layout.findViewById(R.id.ctv_save);
        cbSave.setVisibility(edit ? View.GONE : View.VISIBLE);
        cbSave.setChecked(true);
        markDialog.setView(layout);

        markDialog.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Context context = MarkActivity.this;
                String sId = etID.getText().toString();
                String name = etName.getText().toString();
                int info = edit ? -1 : R.string.mark_tap_to_edit;
                MarkName newMark;

                try {
                    int id = Integer.parseInt(sId);
                    if (id < 0)
                        throw new NumberFormatException();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(name.trim()))
                        throw new NullPointerException();

                    newMark = new MarkName(id, name.trim());
                    saveMark(newMark);

                    if (cbSave.isChecked()) {
                        FileUtil.addMarkToPreset(context, newMark, mark);

                        if (edit)
                            mMarksAdapter.replaceMark(mark, newMark);
                        else
                            mMarksAdapter.addMark(newMark);

                        mMarksAdapter.getFilter().filter(mSearchView.getQuery());
                    }
                } catch (NumberFormatException e) {
                    info = R.string.cat_id_not_int;
                } catch (NullPointerException e) {
                    info = R.string.cat_name_empty;
                }

                if (info != -1)
                    Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
            }
        });

        markDialog.setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        });

        AlertDialog dialog = markDialog.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
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

        if (mMarksAdapter.hasItem(position)) {
            MarkName mark = mMarksAdapter.getMarkItem(position);
            position = mMarksAdapter.getMatchedMarkPosition(mark);

            mLvCategories.setItemChecked(position, true);
            mLvCategories.setSelection(position);

            saveMark(mark);
        } else
            Toast.makeText(this, R.string.mark_no_items, Toast.LENGTH_SHORT).show();

        return true;
    }

    private void saveMark(MarkName mark) {
        if (mIsHot)
            return;

        final Bundle data = new Bundle();
        data.putInt(LoggerConstants.PREF_MARK_POS, mMarksAdapter.getMarkPosition(mark));
        data.putString(BUNDLE_SESSION, mSessionId);
        data.putInt(BUNDLE_ID, mark.getId());
        data.putString(BUNDLE_NAME, mark.getCat());
        data.putLong(BUNDLE_TIME, System.currentTimeMillis());
        ArrayList<InfoItem> infoArray = mCellEngine != null ? mCellEngine.getData() : new ArrayList<InfoItem>();
        data.putParcelableArrayList(BUNDLE_CELL, infoArray);

        // checking sensors state
        if (mSensorEngine != null && mSensorEngine.isEngineEnabled())
            data.putParcelableArrayList(BUNDLE_SENSOR, new ArrayList<>(mSensorEngine.getData()));

        // checking external state
        if (mArduinoEngine != null && mArduinoEngine.isEngineEnabled())
            data.putParcelableArrayList(BUNDLE_EXTERNAL, new ArrayList<>(mArduinoEngine.getData()));

        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibe.vibrate(100);
        String info = String.format(getString(R.string.mark_saved), mark.getCat());
        info += " (" + ++mMarksCount + ")";
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        mIsHot = true;

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(mSearchBox.getActionView().getWindowToken(), 0);
            mLvCategories.requestFocus();
        }

        setFABIcon(false);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = MARK_SAVE;
                msg.setData(data);
                mMarksHandler.sendMessage(msg);
            }
        }, DELAY);
    }

    protected void setIsHot(boolean isHot) {
        mIsHot = isHot;
    }

    @Override
    public void onTimeoutOrFailure() {
        showExternalStatus(false);
    }

    @Override
    public void onConnected() {
        showExternalStatus(true);
    }

    @Override
    public void onConnectionLost() {
        showExternalStatus(false);
    }

    private void showExternalStatus(final boolean isConnected) {
        String deviceName = mArduinoEngine != null ? mArduinoEngine.getDeviceName() : getString(R.string.external_device_name);
        final String info = isConnected ? String.format(getString(R.string.external_connected), deviceName) :
                String.format(getString(R.string.external_not_found), deviceName);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsActive && mLastConnection != isConnected)
                    Toast.makeText(MarkActivity.this, info, Toast.LENGTH_SHORT).show();

                if (mBtRetry != null && mArduinoEngine != null && mArduinoEngine.isEngineEnabled())
                    mBtRetry.setVisible(!isConnected);

                mLastConnection = isConnected;
            }
        });
    }

	public class MarkArrayAdapter extends ArrayAdapter<String> implements Filterable {
		private List<MarkName> mMarks;
		private List<MarkName> mMatchedMarks = new ArrayList<>();
		private final SubstringFilter mFilter = new SubstringFilter();

		MarkArrayAdapter(final Context context, final List<MarkName> objects) {
			super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1);
			mMarks = objects;
            sortById();
			mFilter.filter("");
		}

        private void sortById() {
            Collections.sort(mMarks, new Comparator<MarkName>() {
                @Override
                public int compare(MarkName lhs, MarkName rhs) {
                    return lhs.getId() - rhs.getId();
                }
            });
        }

		@NonNull
        @Override
		public Filter getFilter() {
			return mFilter;
		}

		@Override
		public int getCount() {
			return mMatchedMarks.size();
		}

		int getTotalCount() {
			return mMarks.size();
		}

		@Override
		public String getItem(int position) {
			return mMatchedMarks.get(position).getCat();
		}

        int getMarkPosition(MarkName item) {
            return mMarks.indexOf(item);
        }

        int getMatchedMarkPosition(MarkName item) {
            return mMatchedMarks.indexOf(item);
        }

		MarkName getMatchedMarkItem(int position) {
            if (position >= 0 && position < mMatchedMarks.size())
			    return mMatchedMarks.get(position);
            else
                return new MarkName(-1, "null");
		}

		MarkName getMarkItem(int position) {
            if (hasItem(position))
			    return mMarks.get(position);
            else
                return new MarkName(-1, "null");
		}

		@NonNull
        @Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			return super.getView(position, convertView, parent);
		}

        boolean hasItem(int position) {
            return position >= 0 && position < mMarks.size();
        }

        void addMark(MarkName mark) {
            mMarks.add(mark);
            sortById();
            notifyDataSetChanged();
        }

        void replaceMark(MarkName mark, MarkName newMark) {
            int position = mMarks.indexOf(mark);
            mMarks.remove(position);
            mMarks.add(position, newMark);
            sortById();
            notifyDataSetChanged();
        }

        private class SubstringFilter extends Filter {
			@Override
			protected FilterResults performFiltering(final CharSequence prefix) {
				final FilterResults results = new FilterResults();

				if (prefix != null) {
					ArrayList<MarkName> resultList = new ArrayList<>();

					for (MarkName item : mMarks) {
						String CAT = getUpperString(item.getCat());
						String substr = getUpperString(prefix.toString());

						if (CAT.contains(substr))
							resultList.add(item);
					}

					results.count = resultList.size();
					results.values = resultList;
				}

				return results;
			}

            @SuppressWarnings("unchecked")
			@Override
			protected void publishResults(final CharSequence constraint, final FilterResults results) {
				mMatchedMarks.clear();

				if (results != null && results.count > 0) {
					mMatchedMarks.addAll((ArrayList<MarkName>) results.values);
					notifyDataSetChanged();
				} else
					notifyDataSetInvalidated();
			}

			String getUpperString(String str) {
				return str.toUpperCase(Locale.getDefault());
			}
		}
	}

    private class MarksHandler extends Handler {
        private long mUndoTimeStamp = 0;
        private MarkActivity mParent;

        MarksHandler(MarkActivity parent) {
            mParent = parent;
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MARK_SAVE:
                    if (Math.abs(System.currentTimeMillis() - mUndoTimeStamp) >= DELAY) {
                        Bundle bundle = msg.getData();
                        ArrayList<InfoItem> items = bundle.getParcelableArrayList(BUNDLE_SENSOR);
                        GeoPoint point = GPSEngine.getFix(items);
                        String session = bundle.getString(BUNDLE_SESSION);
                        int markId = bundle.getInt(BUNDLE_ID);
                        String name = bundle.getString(BUNDLE_NAME);
                        long time = bundle.getLong(BUNDLE_TIME);
                        String newMarkId = BaseEngine.saveMark(mUri, session, markId, name, time, point);

                        items = bundle.getParcelableArrayList(BUNDLE_CELL);
                        if (mCellEngine != null)
                            mCellEngine.saveData(items, newMarkId);

                        items = bundle.getParcelableArrayList(BUNDLE_SENSOR);
                        if (items != null && mSensorEngine != null)
                            mSensorEngine.saveData(items, newMarkId);

                        items = bundle.getParcelableArrayList(BUNDLE_EXTERNAL);
                        if (items != null && mArduinoEngine != null)
                            mArduinoEngine.saveData(items, newMarkId);

                        mSavedMarkPosition = bundle.getInt(LoggerConstants.PREF_MARK_POS);
                        PreferenceManager.getDefaultSharedPreferences(mParent).edit().putInt(LoggerConstants.PREF_MARK_POS, mSavedMarkPosition).apply();
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
