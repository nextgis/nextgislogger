package com.nextgis.logger;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class GPSEngine implements LocationListener {

	@Override
	public void onLocationChanged(Location location) {
		location.getLatitude();
		location.getLongitude();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

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
