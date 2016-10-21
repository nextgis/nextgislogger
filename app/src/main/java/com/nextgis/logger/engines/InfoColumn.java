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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.IllegalFormatException;

public class InfoColumn implements Parcelable {
    private static final String FNAME = "full_name";
    private static final String SNAME = "short_name";
    private static final String UNIT = "unit";
    private static final String FORMAT = "format";
    private static final String VALUE = "value";

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
            throw new RuntimeException("Value must be following classes: [String, Double, Float, Integer, Long, Boolean]!");

        mValue = value;
    }

    public InfoColumn(String shortName, String fullName, String unit, Object value, String format) {
        this(shortName, fullName, unit, value);
        mFormat = format;
    }

    private InfoColumn(Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        mFullName = bundle.getString(FNAME);
        mShortName = bundle.getString(SNAME);
        mUnit = bundle.getString(UNIT);
        mFormat = bundle.getString(FORMAT);
        mValue = bundle.get(VALUE);
    }

    public static final Creator<InfoColumn> CREATOR = new Creator<InfoColumn>() {
        @Override
        public InfoColumn createFromParcel(Parcel in) {
            return new InfoColumn(in);
        }

        @Override
        public InfoColumn[] newArray(int size) {
            return new InfoColumn[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putString(FNAME, mFullName);
        bundle.putString(SNAME, mShortName);
        bundle.putString(UNIT, mUnit);
        bundle.putString(FORMAT, mFullName);

        if (mValue instanceof String)
            bundle.putString(VALUE, (String) mValue);
        else if (mValue instanceof Double)
            bundle.putDouble(VALUE, (Double) mValue);
        else if (mValue instanceof Float)
            bundle.putFloat(VALUE, (Float) mValue);
        else if (mValue instanceof Integer)
            bundle.putInt(VALUE, (Integer) mValue);
        else if (mValue instanceof Long)
            bundle.putLong(VALUE, (Long) mValue);
        else if (mValue instanceof Boolean)
            bundle.putBoolean(VALUE, (Boolean) mValue);

        dest.writeBundle(bundle);
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
