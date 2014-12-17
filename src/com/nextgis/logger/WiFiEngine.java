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

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WiFiEngine {
	private BroadcastReceiver wifiReceiver;
	private Context ctx;
	List<ScanResult> wifis;
	
	public WiFiEngine(final Context ctx) {
		this.ctx = ctx;
		
		wifiReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				wifis = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getScanResults();
			}
		};
		
		onResume();
	}
	
	public void onResume() {
		IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		ctx.registerReceiver(wifiReceiver, intentFilter);
	}
	
	public void onPause() {
		ctx.unregisterReceiver(wifiReceiver);
	}
	
	public void scan() {
		((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE)).startScan();
	}
	
	public String getItem() {
		return wifis.get(0).SSID;	// loop
	}
}
