/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov
 ******************************************************************************
 * Copyright © 2014-2015 NextGIS
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class GPSEngine implements LocationListener {
    private final long MIN_TIME = 0;
    private final float MIN_DISTANCE = 0f;

    private Context mContext;
    private final LocationManager mLocationManager;
    private Location mLastFix = null;

    private List<GPSInfoListener> mGPSListeners;

    interface GPSInfoListener {
        public void onGPSInfoChanged();
    }

    public GPSEngine(Context context) {
        mContext = context;
        mGPSListeners = new ArrayList<>();
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        onResume();
    }

    public void addGPSListener(GPSInfoListener listener) {
        mGPSListeners.add(listener);
    }

    private void notifyGPSListeners() {
        for (GPSInfoListener listener : mGPSListeners)
            listener.onGPSInfoChanged();
    }

    public void onResume() {
        if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) && isGpsEnabled()) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            mLastFix = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    public void onPause() {
        mLocationManager.removeUpdates(this);
        mLastFix = null;
    }

    public double getLatitude() {
        if (mLastFix == null)
            return C.UNDEFINED;

        return mLastFix.getLatitude();
    }

    public double getLongitude() {
        if (mLastFix == null)
            return C.UNDEFINED;

        return mLastFix.getLongitude();
    }

    public double getAltitude() {
        if (mLastFix == null || !mLastFix.hasAltitude())
            return C.UNDEFINED;

        return mLastFix.getAltitude();
    }

    public float getAccuracy() {
        if (mLastFix == null || !mLastFix.hasAccuracy())
            return C.UNDEFINED;

        return mLastFix.getAccuracy();
    }

    public float getBearing() {
        if (mLastFix == null || !mLastFix.hasBearing())
            return C.UNDEFINED;

        return mLastFix.getBearing();
    }

    public float getSpeed() {
        if (mLastFix == null || !mLastFix.hasSpeed())
            return C.UNDEFINED;

        return mLastFix.getSpeed();
    }

    public float getSatellites() {
        if (mLastFix == null || mLastFix.getExtras() == null)
            return C.UNDEFINED;

        return mLastFix.getExtras().getInt("satellites", 0);
    }

    public long getTime() {
        if (mLastFix == null)
            return C.UNDEFINED;

        return mLastFix.getTime();
    }

    public boolean hasLocation() {
        return mLastFix != null;
    }

    public boolean isGpsEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(C.PREF_GPS, false);
    }

	@Override
	public void onLocationChanged(Location location) {
		mLastFix = location;
        notifyGPSListeners();
	}

	@Override
	public void onProviderDisabled(String provider) {
        notifyGPSListeners();
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

}
