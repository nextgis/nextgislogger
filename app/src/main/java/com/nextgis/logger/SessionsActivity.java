/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015-2016 NextGIS
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

package com.nextgis.logger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.CellEngine;
import com.nextgis.logger.engines.SensorEngine;
import com.nextgis.logger.util.FileUtil;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.MapUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SessionsActivity extends ProgressBarActivity implements View.OnClickListener {
    private static final int SHARE = 1;
    private static int LAYOUT = android.R.layout.simple_list_item_multiple_choice;

    private static final String XML_VERSION = "<?xml version=\"1.0\"?>";
    private static final String GPX_VERSION = "1.1";
    private static final String GPX_TAG =
            "<gpx version=\"" + GPX_VERSION + "\" creator=\"%s\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";
    private static final String GPX_TAG_CLOSE = "</gpx>";
    private static final String GPX_TAG_TRACK = "<trk>";
    private static final String GPX_TAG_TRACK_CLOSE = "</trk>";
    private static final String GPX_TAG_TRACK_SEGMENT = "<trkseg>";
    private static final String GPX_TAG_TRACK_SEGMENT_CLOSE = "</trkseg>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT = "<trkpt lat=\"%s\" lon=\"%s\">";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_CLOSE = "</trkpt>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_TIME = "<time>%s</time>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_SAT = "<sat>%s</sat>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_ELE = "<ele>%s</ele>";
    private static final String GPX_TAG_TRACK_SEGMENT_POINT_SPEED = "<speed>%s</speed>";
    private static final String GPX_HEADER = XML_VERSION + "\r\n" + String
            .format(GPX_TAG, "NextGIS Logger v" + BuildConfig.VERSION_NAME) + "\r\n" + GPX_TAG_TRACK + "\r\n" + GPX_TAG_TRACK_SEGMENT;

    public static final int TYPE_MSTDT = 0; // marks and service together, data together
    public static final int TYPE_MSTDS = 1; // marks and service together, data separated
    public static final int TYPE_MSSDT = 2; // marks and service separated, data together
    public static final int TYPE_MSSDS = 3; // marks and service separated, data separated
    public static final int TYPE_GPX = 4; // gpx

    private ListView mLvSessions;
    private List<Feature> mSessions;
    private List<String> mSessionsName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sessions_activity);

        mSessionsName = new ArrayList<>();
        mSessions = new ArrayList<>();

        loadSessions();

        mLvSessions = (ListView) findViewById(R.id.lv_sessions);
        mLvSessions.setAdapter(new ArrayAdapter<>(this, LAYOUT, mSessionsName));

        if (mFAB != null)
            mFAB.attachToListView(mLvSessions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sessions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.getItemId() == R.id.action_share || item.getItemId() == R.id.action_delete) {
            ArrayList<Integer> result = new ArrayList<>();
            SparseBooleanArray sbaSelectedItems = mLvSessions.getCheckedItemPositions();

            for (int i = 0; i < sbaSelectedItems.size(); i++) {
                if (sbaSelectedItems.valueAt(i)) {
                    result.add(i);
                }
            }

            if (result.size() > 0) {
                String[] ids = getIdsFromPositions(result);

                switch (item.getItemId()) {
                    case R.id.action_share:
                        shareSessions(ids);
                        return true;
                    case R.id.action_delete:
                        deleteSessions(ids, true);
                        return true;
                }
            } else
                Toast.makeText(this, R.string.sessions_nothing_selected, Toast.LENGTH_SHORT).show();
        }

        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int i = 0; i < mLvSessions.getAdapter().getCount(); i++)
                    mLvSessions.setItemChecked(i, !item.isChecked());

                item.setChecked(!item.isChecked());
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadSessions() {
        mSessionsName.clear();
        mSessions.clear();
        NGWVectorLayer sessionLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SESSION);
        if (sessionLayer != null) {
            List<Long> ids = sessionLayer.query(null);
            for (Long id : ids) {
                Feature feature = sessionLayer.getFeature(id);
                if (feature == null)
                    continue;

                boolean isCurrentSession = mSessionId != null && mSessionId.equals(feature.getFieldValueAsString(LoggerApplication.FIELD_UNIQUE_ID));
                mSessions.add(feature);
                String name = feature.getFieldValueAsString(LoggerApplication.FIELD_NAME);
                mSessionsName.add(isCurrentSession ? name + " *" + getString(R.string.scl_current_session) + "*" : name);
            }

            Collections.sort(mSessionsName, Collections.reverseOrder());
        }
    }

    private void shareSessions(final String[] ids) {
        AlertDialog.Builder options = new AlertDialog.Builder(this);
        final int[] selected = new int[1];
        // TODO save last selected position
        options.setSingleChoiceItems(R.array.export, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position) {
                selected[0] = position;
            }
        }).setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new ExportTask(ids, selected[0]).execute();
            }
        }).show();
    }

    private ArrayList<Uri> prepareData(String[] ids, int type, ExportTask exportTask) throws SQLiteException, IOException {
        ArrayList<Uri> result = new ArrayList<>();
        File temp = new File(getExternalFilesDir(null), LoggerConstants.TEMP_PATH);
        boolean directoryExists = FileUtil.checkOrCreateDirectory(temp);
        if (!directoryExists)
            throw new IOException();

        String in = " IN (" + MapUtil.makePlaceholders(ids.length) + ")";
        SQLiteDatabase db = ((MapContentProviderHelper) MapBase.getInstance()).getDatabase(false);
        Cursor sessions = db.query(LoggerApplication.TABLE_SESSION,
                                   new String[]{LoggerApplication.FIELD_UNIQUE_ID, LoggerApplication.FIELD_NAME, LoggerApplication.FIELD_USER,
                                                LoggerApplication.FIELD_DEVICE_INFO}, LoggerApplication.FIELD_UNIQUE_ID + in, ids, null, null, null);

        if (sessions == null || exportTask.isUserCancelled())
            return null;

        if (sessions.moveToFirst()) {
            do {
                Cursor marks = db.query(LoggerApplication.TABLE_MARK,
                                        new String[]{LoggerApplication.FIELD_UNIQUE_ID, LoggerApplication.FIELD_MARK_ID, LoggerApplication.FIELD_NAME,
                                                     LoggerApplication.FIELD_TIMESTAMP}, LoggerApplication.FIELD_SESSION + " = ?",
                                        new String[]{sessions.getString(0)}, null, null, LoggerApplication.FIELD_TIMESTAMP);

                if (marks == null) // skip empty sessions
                    continue;

                File path = new File(temp, sessions.getString(1));
                directoryExists = FileUtil.checkOrCreateDirectory(path);
                if (!directoryExists)
                    throw new IOException();

                if (marks.moveToFirst()) {
                    do {
                        String preamble = BaseEngine.getPreamble(marks.getString(1), marks.getString(2), sessions.getString(2), marks.getLong(3));
                        preamble += LoggerConstants.CSV_SEPARATOR;
                        String prefix = LoggerConstants.LOG;
                        String markId = marks.getString(0);

                        switch (type) {
                            case TYPE_MSTDT:
                                writeDataTogether(LoggerConstants.DATA, db, path, preamble, markId);
                                break;
                            case TYPE_MSTDS:
                                writeDataSeparated("", db, path, preamble, markId);
                                break;
                            case TYPE_MSSDT:
                                if (marks.getInt(1) != -1)
                                    prefix = LoggerConstants.MARK;

                                writeDataTogether(prefix, db, path, preamble, markId);
                                break;
                            case TYPE_MSSDS:
                                if (marks.getInt(1) != -1)
                                    prefix = LoggerConstants.MARK;

                                writeDataSeparated(prefix + "_", db, path, preamble, markId);
                                break;
                            case TYPE_GPX:
                                writeGPX(LoggerConstants.GPX, db, path, markId);
                                break;
                            default:
                                throw new RuntimeException("Type" + type + " is not supported for export.");
                        }
                    } while (marks.moveToNext() && !exportTask.isUserCancelled());

                    if (type == TYPE_GPX) {
                        FileUtil.append(new File(path, LoggerConstants.GPX).getAbsolutePath(), GPX_HEADER, GPX_TAG_TRACK_SEGMENT_CLOSE + "\r\n" +
                                GPX_TAG_TRACK_CLOSE + "\r\n" + GPX_TAG_CLOSE);
                    }
                }

                marks.close();
                if (exportTask.isUserCancelled())
                    break;

                File deviceInfoFile = new File(path, LoggerConstants.DEVICE_INFO);
                FileUtil.append(deviceInfoFile.getAbsolutePath(), "\r\n\r\n", sessions.getString(3));

                if (putToZip(path))
                    result.add(Uri.fromFile(new File(temp, sessions.getString(1) + LoggerConstants.ZIP_EXT))); // add file's uri to share list
            } while (sessions.moveToNext() && !exportTask.isUserCancelled());
        }

        sessions.close();
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SHARE:
                deleteTemp();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void deleteTemp() {
        File temp = new File(getExternalFilesDir(null), LoggerConstants.TEMP_PATH);
        FileUtil.deleteDirectoryOrFile(temp);
    }

    private void writeDataSeparated(String prefix, SQLiteDatabase db, File path, String preamble, String markId) throws FileNotFoundException {
        Cursor data = db.query(LoggerApplication.TABLE_CELL, null, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null,
                               LoggerConstants.HEADER_ACTIVE);

        String header = LoggerConstants.CSV_HEADER_PREAMBLE + LoggerConstants.CSV_SEPARATOR;
        if (data != null) {
            if (data.moveToFirst()) {
                List<String> items = new ArrayList<>();
                do {
                    items.add(preamble + CellEngine.getDataFromCursor(data));
                } while (data.moveToNext());

                String filePath = new File(path, prefix + LoggerConstants.CELL + LoggerConstants.CSV_EXT).getAbsolutePath();
                FileUtil.append(filePath, header + CellEngine.getHeader(), items);
            }
            data.close();
        }

        data = db.query(LoggerApplication.TABLE_SENSOR, null, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null, null);
        if (data != null) {
            if (data.moveToFirst()) {
                String filePath = new File(path, prefix + LoggerConstants.SENSOR + LoggerConstants.CSV_EXT).getAbsolutePath();
                String item = preamble + SensorEngine.getDataFromCursor(data);
                FileUtil.append(filePath, header + SensorEngine.getHeader(), item);
            }
            data.close();
        }

        data = db.query(LoggerApplication.TABLE_EXTERNAL, null, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null, null);
        if (data != null) {
            if (data.moveToFirst()) {
                String filePath = new File(path, prefix + LoggerConstants.EXTERNAL + LoggerConstants.CSV_EXT).getAbsolutePath();
                String item = preamble + ArduinoEngine.getDataFromCursor(data);
                FileUtil.append(filePath, header + "data", item);
            }
            data.close();
        }
    }

    private void writeDataTogether(String prefix, SQLiteDatabase db, File path, String preamble, String markId) throws FileNotFoundException {
        String row = "";
        String header = "";
        Cursor data = db.query(LoggerApplication.TABLE_SENSOR, null, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null, null);
        if (data != null) {
            if (data.moveToFirst()) {
                row += LoggerConstants.CSV_SEPARATOR + SensorEngine.getDataFromCursor(data);
                header += LoggerConstants.CSV_SEPARATOR + SensorEngine.getHeader();
            }
            data.close();
        }

        data = db.query(LoggerApplication.TABLE_EXTERNAL, null, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null, null);
        if (data != null) {
            if (data.moveToFirst()) {
                row += LoggerConstants.CSV_SEPARATOR + ArduinoEngine.getDataFromCursor(data);
                header += LoggerConstants.CSV_SEPARATOR + "data";
            }
            data.close();
        }

        data = db.query(LoggerApplication.TABLE_CELL, null, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null,
                        LoggerConstants.HEADER_ACTIVE);
        if (data != null) {
            List<String> items = new ArrayList<>();
            if (data.moveToFirst()) {
                do {
                    items.add(preamble + CellEngine.getDataFromCursor(data) + row);
                } while (data.moveToNext());
            } else
                items.add(preamble + CellEngine.getEmptyRow() + row);
            data.close();

            String filePath = new File(path, prefix + LoggerConstants.CSV_EXT).getAbsolutePath();
            header = LoggerConstants.CSV_HEADER_PREAMBLE + LoggerConstants.CSV_SEPARATOR + CellEngine.getHeader() + header;
            FileUtil.append(filePath, header, items);
        }
    }

    private void writeGPX(String prefix, SQLiteDatabase db, File path, String markId) throws FileNotFoundException {
        String[] columns =
                new String[]{LoggerConstants.HEADER_GPS_LAT, LoggerConstants.HEADER_GPS_LON, LoggerConstants.HEADER_GPS_TIME, LoggerConstants.HEADER_GPS_ALT,
                             LoggerConstants.HEADER_GPS_SAT, LoggerConstants.HEADER_GPS_SP};
        Cursor data = db.query(LoggerApplication.TABLE_SENSOR, columns, LoggerApplication.FIELD_MARK + " = ?", new String[]{markId}, null, null, null);
        final StringBuilder sb = new StringBuilder();
        final Formatter f = new Formatter(sb);

        if (data != null) {
            if (data.moveToFirst()) {
                DecimalFormat df = new DecimalFormat("0", new DecimalFormatSymbols(Locale.ENGLISH));
                df.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

                String sLat = df.format(data.getDouble(0));
                String sLon = df.format(data.getDouble(1));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT, sLat, sLon);
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_TIME, getTimeStampAsString(data.getLong(2)));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_ELE, df.format(data.getDouble(3)));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_SAT, data.getInt(4));
                f.format(GPX_TAG_TRACK_SEGMENT_POINT_SPEED, data.getFloat(5));
                sb.append(GPX_TAG_TRACK_SEGMENT_POINT_CLOSE);

                File track = new File(path, prefix);
                FileUtil.append(track.getAbsolutePath(), GPX_HEADER, sb.toString());
            }

            data.close();
        }
    }

    private boolean putToZip(File files) throws IOException {
        byte[] buffer = new byte[1024];
        if (!files.exists() || !files.isDirectory())
            return false;

        FileOutputStream fos = new FileOutputStream(files.getAbsolutePath() + LoggerConstants.ZIP_EXT);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

        for (File file : files.listFiles()) { // for each log-file in directory
            FileInputStream fis = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(file.getName())); // put it in zip

            int length;
            while ((length = fis.read(buffer)) > 0)
                // write it to zip
                zos.write(buffer, 0, length);

            zos.closeEntry();
            fis.close();
        }

        zos.close();
        return files.listFiles().length > 0;
    }

    private boolean deleteSessions(String[] ids, boolean ask) {
        boolean result = false;
        String authority = ((LoggerApplication) getApplication()).getAuthority();
        Uri uri;

        try {
            if (hasCurrentSession(ids)) {
                if (ask) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final String[] finalIds = ids;
                    builder.setTitle(R.string.delete_sessions_title).setMessage(R.string.sessions_delete_current)
                           .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteSessions(finalIds, false);
                        }
                    }).show();
                    return false;
                } else {
                    stopLoggerService();
                    clearSession();
                }
            }

            NGWVectorLayer layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_MARK);
            String in = " IN (" + MapUtil.makePlaceholders(ids.length) + ")";
            List<String> markIds = new ArrayList<>();
            if (layer != null) {
                Cursor allMarks = layer.query(new String[]{LoggerApplication.FIELD_UNIQUE_ID}, LoggerApplication.FIELD_SESSION + in, ids, null, null);
                if (allMarks != null) {
                    if (allMarks.moveToFirst()) {
                        do {
                            markIds.add(allMarks.getString(0));
                        } while (allMarks.moveToNext());
                    }

                    allMarks.close();
                }

                uri = Uri.parse("content://" + authority + "/" + layer.getPath().getName() + "/");
                layer.delete(uri, LoggerApplication.FIELD_SESSION + in, ids);
                layer.rebuildCache(null);
            }

            layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SESSION);
            if (layer != null) {
                uri = Uri.parse("content://" + authority + "/" + layer.getPath().getName() + "/");
                layer.delete(uri, LoggerApplication.FIELD_UNIQUE_ID + in, ids);
                layer.rebuildCache(null);
            }

            ids = markIds.toArray(new String[markIds.size()]);
            in = " IN (" + MapUtil.makePlaceholders(ids.length) + ")";
            layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_CELL);
            if (layer != null) {
                uri = Uri.parse("content://" + authority + "/" + layer.getPath().getName() + "/");
                layer.delete(uri, LoggerApplication.FIELD_MARK + in, ids);
                layer.rebuildCache(null);
            }

            layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_SENSOR);
            if (layer != null) {
                uri = Uri.parse("content://" + authority + "/" + layer.getPath().getName() + "/");
                layer.delete(uri, LoggerApplication.FIELD_MARK + in, ids);
                layer.rebuildCache(null);
            }

            layer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_EXTERNAL);
            if (layer != null) {
                uri = Uri.parse("content://" + authority + "/" + layer.getPath().getName() + "/");
                layer.delete(uri, LoggerApplication.FIELD_MARK + in, ids);
                layer.rebuildCache(null);
            }

            result = true;

            // shrink database
            SQLiteDatabase db = ((MapContentProviderHelper) MapBase.getInstance()).getDatabase(false);
            db.execSQL("VACUUM");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, R.string.delete_sessions_done, Toast.LENGTH_SHORT).show();
        loadSessions();
        mLvSessions.setAdapter(new ArrayAdapter<>(this, LAYOUT, mSessionsName));

        return result;
    }

    private boolean hasCurrentSession(String[] ids) {
        for (String id : ids)
            if (id.equals(mSessionId))
                return true;

        return false;
    }

    private String[] getIdsFromPositions(List<Integer> positions) {
        String[] result = new String[positions.size()];
        for (int i = 0; i < positions.size(); i++)
            result[i] = mSessions.get(mSessions.size() - i - 1).getFieldValueAsString(LoggerApplication.FIELD_UNIQUE_ID);

        return result;
    }

    protected String getTimeStampAsString(long nTimeStamp) {
        final SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(new Date(nTimeStamp));
    }

    public class ExportTask extends AsyncTask<Void, Void, ArrayList<Uri>> {
        private AlertDialog.Builder mDialog;
        private ProgressDialog mProgress;
        private String[] mIds;
        private int mType;
        private boolean mIsCanceled = false;

        ExportTask(String[] ids, int type) {
            mIds = ids;
            mType = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mIds.length > 1) {
                mDialog = new AlertDialog.Builder(SessionsActivity.this);
                mDialog.setTitle(R.string.share_sessions_title).setMessage(R.string.sync_progress).show();
            }
        }

        @Override
        protected ArrayList<Uri> doInBackground(Void... params) {
            publishProgress();

            try {
                return prepareData(mIds, mType, this);
            } catch (SQLiteException | IOException ignored) { }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            mProgress = new ProgressDialog(SessionsActivity.this);
            mProgress.setTitle(R.string.export);
            mProgress.setMessage(getString(R.string.preparing));
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    mIsCanceled = true;
                }
            });
            mProgress.show();
        }

        boolean isUserCancelled() {
            return mIsCanceled;
        }

        @Override
        protected void onPostExecute(ArrayList<Uri> result) {
            super.onPostExecute(result);

            if (mProgress != null)
                mProgress.dismiss();

            if (mIsCanceled) {
                deleteTemp();
                return;
            }

            if (result == null) {
                Toast.makeText(SessionsActivity.this, R.string.fs_error_msg, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent shareIntent = new Intent();
            if (result.size() > 1) {
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, result);
            } else {
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, result.get(0));
            }

            shareIntent.setType("application/zip"); //set mime type
            startActivityForResult(Intent.createChooser(shareIntent, getString(R.string.share_sessions_title)), SHARE);
        }
    }
}
