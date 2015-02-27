/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov
 ******************************************************************************
 * Copyright © 2014 NextGIS
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.nextgis.logger.UI.ProgressBarActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SessionsActivity extends ProgressBarActivity {
    private ListView lvSessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sessions_activity);

        lvSessions = (ListView) findViewById(R.id.lv_sessions);
        lvSessions.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, getSessions()));
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
            ArrayList<File> result = new ArrayList<>();
            SparseBooleanArray sbaSelectedItems = lvSessions.getCheckedItemPositions();

            for (int i = 0; i < sbaSelectedItems.size(); i++) {
                if (sbaSelectedItems.valueAt(i)) {
                    String fileName = lvSessions.getAdapter().getItem(sbaSelectedItems.keyAt(i)).toString();

                    if (fileName.contains("*"))
                        fileName = fileName.substring(0, fileName.indexOf(" *"));

                    result.add(new File(C.dataBasePath + File.separator + fileName));
                }
            }

            if (result.size() > 0)
                switch (item.getItemId()) {
                    case R.id.action_share:
                        ArrayList<Uri> logsZips = new ArrayList<>();

                        try {
                            byte[] buffer = new byte[1024];

                            MainActivity.checkOrCreateDirectory(C.tempPath);

                            for (File file : result) { // for each selected logs directory
                                String tempFileName = C.tempPath + File.separator + file.getName() + ".zip"; // set temp zip file path

                                File[] files = file.listFiles(); // get all files in current log directory

                                if (files.length == 0) // skip empty directories
                                    continue;

                                FileOutputStream fos = new FileOutputStream(tempFileName);
                                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

                                for (int j = 0; j < files.length; j++) { // for each log-file in directory
                                    FileInputStream fis = new FileInputStream(files[j]);
                                    zos.putNextEntry(new ZipEntry(files[j].getName())); // put it in zip

                                    int length;

                                    while ((length = fis.read(buffer)) > 0)
                                        // write it to zip
                                        zos.write(buffer, 0, length);

                                    zos.closeEntry();
                                    fis.close();
                                }

                                zos.close();
                                logsZips.add(Uri.fromFile(new File(tempFileName))); // add file's uri to share list
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, R.string.fs_error_msg, Toast.LENGTH_SHORT).show();
                        }

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE); // multiple sharing
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, logsZips); // set data
                        shareIntent.setType("application/zip"); //set mime type
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_sessions_title)));
                        return true;
                    case R.id.action_delete:
                        deleteFiles(result.toArray(new File[result.size()]));
                        Toast.makeText(this, R.string.delete_sessions_done, Toast.LENGTH_SHORT).show();
                        lvSessions.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, getSessions()));
                        return true;
                }
            else
                Toast.makeText(this, R.string.sessions_nothing_selected, Toast.LENGTH_SHORT).show();
        }

        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int i = 0; i < lvSessions.getAdapter().getCount(); i++)
                    lvSessions.setItemChecked(i, !item.isChecked());

                item.setChecked(!item.isChecked());
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private ArrayList<String> getSessions() {
        ArrayList<String> sessions = new ArrayList<>();

        try {
            File baseDir = new File(C.dataBasePath);

            if (!baseDir.exists() || !baseDir.isDirectory()) {
                return sessions;
            }

            for (File file : baseDir.listFiles())
                if (file.isDirectory() && !file.isHidden())
                    if (file.getName().equals(PreferenceManager.getDefaultSharedPreferences(this).getString(C.PREF_SESSION_NAME, "")))  // TODO should we block this session?
                        sessions.add(file.getName() + " *" + getString(R.string.scl_current_session) + "*");    // it's current opened session
                    else
                        sessions.add(file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(sessions, Collections.reverseOrder());    // descending sort

        return sessions;
    }

    /**
     * Delete set of any files or directories.
     *
     * @param files File[] with all data to delete
     * @return void
     */
    public static void deleteFiles(File[] files) {
        if (files != null) { // there are something to delete
            for (File file : files)
                deleteDirectoryOrFile(file);
        }
    }

    /**
     * Delete single file or directory recursively (deleting anything inside
     * it).
     *
     * @param dir The file / dir to delete
     * @return true if the file / dir was successfully deleted
     */
    public static boolean deleteDirectoryOrFile(File dir) {
        if (!dir.exists())
            return false;

        if (!dir.isDirectory())
            return dir.delete();
        else {
            String[] files = dir.list();

            for (String file : files) {
                File f = new File(dir, file);

                if (f.isDirectory())
                    deleteDirectoryOrFile(f);
                else
                    f.delete();
            }
        }

        return dir.delete();
    }
}
