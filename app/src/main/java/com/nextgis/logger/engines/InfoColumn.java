/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2016 NextGIS
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

package com.nextgis.logger.engines;

import android.text.TextUtils;

import java.util.IllegalFormatException;

public class InfoColumn {
    private String mFullName, mShortName, mUnit, mFormat;
    private Object mValue;

    public InfoColumn(String shortName, String fullName, String unit) {
        mShortName = shortName;
        mFullName = fullName;
        mUnit = unit;
    }

    public InfoColumn(String shortName, String fullName, String unit, Object value) {
        this(shortName, fullName, unit);

        if (!isValueTypeValid(value))
            throw new RuntimeException("Value must be following classes: [String, Double, Float, Integer]!");

        mValue = value;
    }

    public InfoColumn(String shortName, String fullName, String unit, Object value, String format) {
        this(shortName, fullName, unit, value);
        mFormat = format;
    }

    private boolean isValueTypeValid(Object value) {
        return value instanceof String || value instanceof Double || value instanceof Float
                || value instanceof Integer || value instanceof Long || value instanceof Boolean;
    }

    public void setValue(Object value) throws RuntimeException {
        if (!isValueTypeValid(value))
            throw new RuntimeException("Value must be following classes: [String, Double, Float, Integer, Long, Boolean]!");

        mValue = value;
    }

    public String getShortName() {
        return mShortName;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getUnit() {
        return mUnit;
    }

    public Object getValue() {
        return mValue;
    }

    public String getValueWithUnit() {
        String unit = getUnit(), format = mFormat;
        unit = TextUtils.isEmpty(unit) ? "" : " " + unit;

        if (TextUtils.isEmpty(format))
            format = "%s";

        String result = getValue().toString();
        try {
            result = String.format(format, getValue());
        } catch (IllegalFormatException | NumberFormatException ignored) { }

        return result + unit;
    }
}
