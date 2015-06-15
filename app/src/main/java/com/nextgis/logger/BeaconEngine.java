/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Nikita Kirin
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;

public class BeaconEngine extends Observable implements BeaconConsumer{

    Context mContext;

	private BeaconManager beaconManager;
    private Collection<Beacon> mBeacons;

	public BeaconEngine(Context context) {
        mContext = context;

		verifyBluetooth();

        mBeacons = new ArrayList<Beacon>();

		beaconManager = BeaconManager.getInstanceForApplication(mContext);
		beaconManager.getBeaconParsers().add((new BeaconParser()).setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

		beaconManager.bind(this);

        Log.i("BEACON TEST!!!", "test construct");
	}

	@Override
	public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                mBeacons = beacons;
                if (beacons.size() > 0) {
                    Log.i("BEACON TEST!!!", "The first beacon I see is about " + beacons.iterator().next().getDistance() + " meters away.");
                }
                setChanged();
                notifyObservers();
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("AllBeacons", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

	@Override
	public Context getApplicationContext() {
		return mContext;
	}

	@Override
	public void unbindService(ServiceConnection serviceConnection) {
        getApplicationContext().unbindService(serviceConnection);
	}

	@Override
	public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return getApplicationContext().bindService(intent, serviceConnection, i);
	}

	private void verifyBluetooth() {

		try {
			if (!BeaconManager.getInstanceForApplication(mContext).checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle("Bluetooth not enabled");
				builder.setMessage("Please enable bluetooth in settings and restart this application.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						System.exit(0);
					}
				});
				builder.show();
			}
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle("Bluetooth LE not available");
			builder.setMessage("Sorry, this device does not support Bluetooth LE.");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					System.exit(0);
				}

			});
			builder.show();

		}

	}


    public Collection<Beacon> getBeacons() {
        return mBeacons;
    }

}
