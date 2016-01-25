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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.nextgis.logger.R;
import com.nextgis.logger.util.Constants;

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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.MIN_GPS_TIME, Constants.MIN_GPS_DISTANCE, this);
            mLastFix = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPause() {
        if (super.onPause()) {
            mLocationManager.removeUpdates(this);
            mLastFix = null;
            return true;
        }

        return false;
    }

    public int getSatellites() {
        return mLastFix.getExtras().getInt("satellites", 0);
    }

    public long getTime() {
        if (mLastFix == null)
            return Constants.UNDEFINED;

        return mLastFix.getTime();
    }

    @Override
    public boolean isEngineEnabled() {
        return getPreferences().getBoolean(Constants.PREF_GPS, true);
    }

    @Override
    protected void loadHeader() {
        mGPSItem = new InfoItem(mContext.getString(R.string.gps));
        mGPSItem.addColumn(Constants.HEADER_GPS_LAT, mContext.getString(R.string.info_lat), mContext.getString(R.string.info_degree), "%.6f")
                .addColumn(Constants.HEADER_GPS_LON, mContext.getString(R.string.info_lon), mContext.getString(R.string.info_degree), "%.6f")
                .addColumn(Constants.HEADER_GPS_ALT, mContext.getString(R.string.info_ele), mContext.getString(R.string.info_meter), "%.2f")
                .addColumn(Constants.HEADER_GPS_ACC, mContext.getString(R.string.info_acc), mContext.getString(R.string.info_meter), "%.2f")
                .addColumn(Constants.HEADER_GPS_SP, mContext.getString(R.string.info_speed), mContext.getString(R.string.info_kmh))
                .addColumn(Constants.HEADER_GPS_BE, mContext.getString(R.string.info_bearing), mContext.getString(R.string.info_degree))
                .addColumn(Constants.HEADER_GPS_SAT, mContext.getString(R.string.info_sat), null)
                .addColumn(Constants.HEADER_GPS_TIME, mContext.getString(R.string.info_time), null);

        mItems.add(mGPSItem);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastFix = location;
        mGPSItem.setValue(Constants.HEADER_GPS_LAT, location.getLatitude());
        mGPSItem.setValue(Constants.HEADER_GPS_LON, location.getLongitude());
        mGPSItem.setValue(Constants.HEADER_GPS_ALT, location.getAltitude());
        mGPSItem.setValue(Constants.HEADER_GPS_ACC, location.getAccuracy());
        mGPSItem.setValue(Constants.HEADER_GPS_SP, location.getSpeed());
        mGPSItem.setValue(Constants.HEADER_GPS_BE, location.getBearing());
        mGPSItem.setValue(Constants.HEADER_GPS_SAT, getSatellites());
        mGPSItem.setValue(Constants.HEADER_GPS_TIME, getTime());
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
