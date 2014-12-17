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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SimpleLogsChooser extends DialogFragment implements android.content.DialogInterface.OnClickListener {
	SimpleLogsChooserListener slcListener;
	ArrayList<String> logsDirectories;
	final ArrayList<Integer> selectedLogs = new ArrayList<Integer>();
	
	interface SimpleLogsChooserListener {
		public void onChosenLogs(ArrayList<String> logsFiles);
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		logsDirectories = getLogs();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setMultiChoiceItems(logsDirectories.toArray(new CharSequence[logsDirectories.size()]), null, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) {
					selectedLogs.add(which);
				} else if (selectedLogs.contains(which)) {
					selectedLogs.remove(Integer.valueOf(which));
				}
			}
		}).setPositiveButton(R.string.btn_ok, this).setNegativeButton(R.string.btn_cancel, this).setTitle(getArguments().getString("title"));
		
		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
	    case Dialog.BUTTON_POSITIVE:
	    	ArrayList<String> result = new ArrayList<String>();
	    	
	    	for (int file : selectedLogs) {
	    		String fileName = logsDirectories.get(file);
	    				
	    		if (fileName.contains("*"))
	    			fileName = fileName.substring(0, fileName.indexOf(" *"));
	    			
				result.add(fileName);
	    	}
	    	
	    	slcListener.onChosenLogs(result);
	      break;
	    default:
	      
	      break;
		}
	}

	private ArrayList<String> getLogs() {
		ArrayList<String> logs = new ArrayList<String>();

		try {
			File baseDir = new File(C.dataBasePath);

			if (!baseDir.exists() || !baseDir.isDirectory()) {
				return logs;
			}

			for (File file : baseDir.listFiles())
				if (file.isDirectory() && !file.isHidden())
					if (file.getName().equals(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(C.PREF_SESSION_NAME, "")))
						logs.add(file.getName() + " *" + getString(R.string.scl_current_session) + "*");
					else
						logs.add(file.getName());
		} catch (Exception e) {
		}
		
		Collections.sort(logs, Collections.reverseOrder());	// descending sort

		return logs;
	}

	public void setOnChosenLogs(SimpleLogsChooserListener slc) {
		slcListener = slc;
	}
}
