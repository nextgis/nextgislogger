/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov
 * *****************************************************************************
 * Copyright © 2014-2016 NextGIS
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.nextgis.logger.R;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.GeoConstants;

import java.util.ArrayList;

public class GPSEngine extends BaseEngine implements LocationListener {
    private final LocationManager mLocationManager;
    private Location mLastFix = null;
    private InfoItem mGPSItem;

    public GPSEngine(Context context) {
        super(context);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        loadHeader();
    }

    @Override
    public boolean onResume() {
        if (super.onResume() && mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) && isEngineEnabled()) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                return false;
            }

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LoggerConstants.MIN_GPS_TIME, LoggerConstants.MIN_GPS_DISTANCE, this);
            mLastFix = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPause() {
        if (super.onPause()) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                return false;
            }

            mLocationManager.removeUpdates(this);
            mLastFix = null;
            return true;
        }

        return false;
    }

    @Override
    public void saveData(String markId) {

    }

    @Override
    public void saveData(ArrayList<InfoItem> items, String markId) {

    }

    public int getSatellites() {
        return mLastFix.getExtras().getInt("satellites", 0);
    }

    public long getTime() {
        if (mLastFix == null)
            return LoggerConstants.UNDEFINED;

        return mLastFix.getTime();
    }

    public static GeoPoint getFix(ArrayList<InfoItem> items) {
        InfoItem gpsItem = null;
        for (InfoItem item : items) {
            if (item.getColumn(LoggerConstants.HEADER_GPS_LAT) != null) {
                gpsItem = item;
                break;
            }
        }

        double x = 0, y = 0;
        if (gpsItem != null) {
            Object value = gpsItem.getColumn(LoggerConstants.HEADER_GPS_LAT).getValue();
            if (value instanceof Double)
                x = (double) value;

            value = gpsItem.getColumn(LoggerConstants.HEADER_GPS_LON).getValue();
            if (value instanceof Double)
                y = (double) value;
        }

        GeoPoint point = new GeoPoint(x, y);
        point.setCRS(GeoConstants.CRS_WGS84);
        point.project(GeoConstants.CRS_WEB_MERCATOR);
        return point;
    }

    @Override
    public boolean isEngineEnabled() {
        return getPreferences().getBoolean(LoggerConstants.PREF_GPS, true);
    }

    @Override
    protected void loadHeader() {
        mGPSItem = new InfoItem(mContext.getString(R.string.gps));
        mGPSItem.addColumn(LoggerConstants.HEADER_GPS_LAT, mContext.getString(R.string.info_lat), mContext.getString(R.string.info_degree), "%.6f")
                .addColumn(LoggerConstants.HEADER_GPS_LON, mContext.getString(R.string.info_lon), mContext.getString(R.string.info_degree), "%.6f")
                .addColumn(LoggerConstants.HEADER_GPS_ALT, mContext.getString(R.string.info_ele), mContext.getString(R.string.info_meter), "%.2f")
                .addColumn(LoggerConstants.HEADER_GPS_ACC, mContext.getString(R.string.info_acc), mContext.getString(R.string.info_meter), "%.2f")
                .addColumn(LoggerConstants.HEADER_GPS_SP, mContext.getString(R.string.info_speed), mContext.getString(R.string.info_kmh))
                .addColumn(LoggerConstants.HEADER_GPS_BE, mContext.getString(R.string.info_bearing), mContext.getString(R.string.info_degree))
                .addColumn(LoggerConstants.HEADER_GPS_SAT, mContext.getString(R.string.info_sat), null)
                .addColumn(LoggerConstants.HEADER_GPS_TIME, mContext.getString(R.string.info_time), null);

        mItems.add(mGPSItem);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastFix = location;
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_LAT, location.getLatitude());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_LON, location.getLongitude());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_ALT, location.getAltitude());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_ACC, location.getAccuracy());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_SP, location.getSpeed());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_BE, location.getBearing());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_SAT, getSatellites());
        mGPSItem.setValue(LoggerConstants.HEADER_GPS_TIME, getTime());
        notifyListeners(mGPSItem.getTitle());
    }

    @Override
    public void onProviderDisabled(String provider) {
        notifyListeners(mGPSItem.getTitle());
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

}
