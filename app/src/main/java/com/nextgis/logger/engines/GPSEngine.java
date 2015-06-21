/**
 * ***************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov
 * *****************************************************************************
 * Copyright © 2014-2015 NextGIS
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * ***************************************************************************
 */
package com.nextgis.logger.engines;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.nextgis.logger.util.Constants;

public class GPSEngine extends BaseEngine implements LocationListener {
    private final LocationManager mLocationManager;
    private Location mLastFix = null;

    public GPSEngine(Context context) {
        super(context);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void onResume() {
        if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) && isGpsEnabled()) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.MIN_GPS_TIME, Constants.MIN_GPS_DISTANCE, this);
            mLastFix = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    public void onPause() {
        mLocationManager.removeUpdates(this);
        mLastFix = null;
    }

    public double getLatitude() {
        if (mLastFix == null)
            return Constants.NaN;

        return mLastFix.getLatitude();
    }

    public double getLongitude() {
        if (mLastFix == null)
            return Constants.NaN;

        return mLastFix.getLongitude();
    }

    public double getAltitude() {
        if (mLastFix == null || !mLastFix.hasAltitude())
            return Constants.NaN;

        return mLastFix.getAltitude();
    }

    public float getAccuracy() {
        if (mLastFix == null || !mLastFix.hasAccuracy())
            return Constants.NaN;

        return mLastFix.getAccuracy();
    }

    public float getBearing() {
        if (mLastFix == null || !mLastFix.hasBearing())
            return Constants.NaN;

        return mLastFix.getBearing();
    }

    public float getSpeed() {
        if (mLastFix == null || !mLastFix.hasSpeed())
            return Constants.NaN;

        return mLastFix.getSpeed();
    }

    public float getSatellites() {
        if (mLastFix == null || mLastFix.getExtras() == null)
            return Constants.NaN;

        return mLastFix.getExtras().getInt("satellites", 0);
    }

    public long getTime() {
        if (mLastFix == null)
            return Constants.UNDEFINED;

        return mLastFix.getTime();
    }

    public boolean hasLocation() {
        return mLastFix != null;
    }

    public boolean isGpsEnabled() {
        return getPreferences().getBoolean(Constants.PREF_GPS, false);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastFix = location;
        notifyListeners();
    }

    @Override
    public void onProviderDisabled(String provider) {
        notifyListeners();
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

}
