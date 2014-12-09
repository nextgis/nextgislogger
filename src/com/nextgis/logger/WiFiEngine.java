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
