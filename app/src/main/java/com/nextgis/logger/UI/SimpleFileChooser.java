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
package com.nextgis.logger.UI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.nextgis.logger.R;

public class SimpleFileChooser extends DialogFragment implements android.content.DialogInterface.OnClickListener {
	private SimpleFileChooserListener simpleFileChooserListener;
    private String sdcardDirectory, currentPath;
    private ArrayList<String> dirs;
    ArrayAdapter<String> adapter;
    private ListView lvDirs;

	ArrayList<String> logsDirectories;
	final ArrayList<Integer> selectedLogs = new ArrayList<Integer>();
	
	public interface SimpleFileChooserListener {
		public void onFileChosen(File file);
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        currentPath = sdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

        try {
            sdcardDirectory = new File(sdcardDirectory).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dirs = getDirectories(sdcardDirectory);
        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, dirs);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setSingleChoiceItems(adapter, -1, this).setPositiveButton(R.string.btn_ok, this).setNegativeButton(R.string.btn_cancel, this).setTitle(getFileName());

        AlertDialog alert = builder.create();
        lvDirs = alert.getListView();

		return alert;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
        File current = new File(currentPath);

		switch (which) {
	    case Dialog.BUTTON_POSITIVE:
	    	simpleFileChooserListener.onFileChosen(current);
            break;
        case Dialog.BUTTON_NEGATIVE:
            break;
	    default:
                goTo(which);
            break;
		}
	}

    private void goTo(int which) {
        File current = new File(currentPath);

        try {
            current.getCanonicalPath();
        } catch (IOException e) {
            return;
        }

        if (current.isFile())
            currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));

        String selected = dirs.get(which).replace("/", "");

        if (selected.equals("..")) {
            currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
            updateListView();
        } else {
            currentPath += "/" +selected;
            current = new File(currentPath);

            if (!current.isFile())
                updateListView();
        }

        getDialog().setTitle(getFileName());
    }

    private void updateListView() {
        dirs.clear();
        dirs.addAll(getDirectories(currentPath));
        adapter.notifyDataSetChanged();

        if (!new File(currentPath).isFile())
            for (int i = 0; i < lvDirs.getCount(); i++)
                lvDirs.setItemChecked(i, false);
    }

    private String getFileName() {
        return currentPath.substring(currentPath.lastIndexOf("/"));
    }

	private ArrayList<String> getDirectories(String dir) {
		ArrayList<String> dirs = new ArrayList<String>();

		try {
			File currentDir = new File(dir);

            if (!dir.equals(sdcardDirectory))
                dirs.add("..");

			if (!currentDir.exists() || !currentDir.isDirectory())
				return dirs;

			for (File file : currentDir.listFiles())
				if (file.isDirectory())
					dirs.add(file.getName() + "/");
                else
                    dirs.add(file.getName());
		} catch (Exception e) {
		}

		Collections.sort(dirs);

		return dirs;
	}

	public void setOnFileChosen(SimpleFileChooserListener sfc) {
        simpleFileChooserListener = sfc;
	}
}
