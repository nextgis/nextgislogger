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

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class IntEditTextPreference extends EditTextPreference {
	public IntEditTextPreference(Context context) {
		super(context);
	}

	public IntEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		return String.valueOf(getPersistedInt(-1));
	}

	@Override
	protected boolean persistString(String value) {
		return persistInt(Integer.valueOf(value));
	}
}
