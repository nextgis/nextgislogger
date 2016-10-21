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

import com.nextgis.logger.util.LoggerConstants;

import java.util.ArrayList;

public class InfoItem implements Parcelable {
    private static final String TITLE = "title";
    private static final String DESC = "desc";
    private static final String COLUMNS = "columns";

    private ArrayList<InfoColumn> mColumns;
    private String mTitle, mDescription;

    public InfoItem(String title) {
        mTitle = title;
        mColumns = new ArrayList<>();
    }

    public InfoItem(String title, String description) {
        this(title);
        mDescription = description;
    }

    private InfoItem(Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        mTitle = bundle.getString(TITLE);
        mDescription = bundle.getString(DESC);
        mColumns = bundle.getParcelableArrayList(COLUMNS);
    }

    public static final Creator<InfoItem> CREATOR = new Creator<InfoItem>() {
        @Override
        public InfoItem createFromParcel(Parcel in) {
            return new InfoItem(in);
        }

        @Override
        public InfoItem[] newArray(int size) {
            return new InfoItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putString(TITLE, mTitle);
        bundle.putString(DESC, mDescription);
        bundle.putParcelableArrayList(COLUMNS, mColumns);
        dest.writeBundle(bundle);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    /**
     * Add new column
     *
     * @param shortName      Column short name
     * @param fullName       Column full name
     * @param unit           Column unit
     */
    public InfoItem addColumn(String shortName, String fullName, String unit) {
        mColumns.add(new InfoColumn(shortName, fullName, unit, LoggerConstants.NO_DATA));
        return this;
    }

    /**
     * Add new column
     *
     * @param shortName     Column short name
     * @param fullName      Column full name
     * @param unit          Column unit
     * @param format        Format for values
     */
    public InfoItem addColumn(String shortName, String fullName, String unit, String format) {
        mColumns.add(new InfoColumn(shortName, fullName, unit, LoggerConstants.NO_DATA, format));
        return this;
    }

    public ArrayList<InfoColumn> getColumns() {
        return mColumns;
    }

    /**
     * Get specific column
     * @param key       Column short name
     */
    public InfoColumn getColumn(String key) {
        for (InfoColumn column : mColumns)
            if (column.getShortName().equals(key))
                return column;

        return null;
    }

    /**
     * Set value for specific column
     *
     * @param key       Column short name
     * @param value     Column value
     */
    public void setValue(String key, Object value) throws RuntimeException {
        for (InfoColumn column : mColumns)
            if (column.getShortName().equals(key)) {
                column.setValue(value);
                break;
            }
    }

    public int size() {
        return mColumns.size();
    }

    public ArrayList<String> getShortNames() {
        ArrayList<String> names = new ArrayList<>();
        for (InfoColumn column : mColumns)
            names.add(column.getShortName());

        return names;
    }
}
