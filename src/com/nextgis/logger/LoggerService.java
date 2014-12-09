package com.nextgis.logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.nextgis.logger.R;

public class LoggerService extends Service {

	private static long timeStart = 0;
	private static int recordsCount = 0;
	private static int interval = 1;

	private boolean isRunning = false;
	private boolean isSensor = true;

	private String userName = "User1";
	
	private CellEngine gsmEngine;
	private SensorEngine sensorEngine;
	private Thread thread = null;
	private LocalBinder localBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public LoggerService getService() {
			return LoggerService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		//android.os.Debug.waitForDebugger();

		if (timeStart == 0) {
			timeStart = System.currentTimeMillis();
		}

		gsmEngine = new CellEngine(this);
		gsmEngine.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		isSensor = prefs.getBoolean(C.PREF_SENSOR_STATE, true);
		userName = prefs.getString(C.PREF_USER_NAME, "userName");
		interval = prefs.getInt(C.PREF_PERIOD_SEC, interval);

		if (isSensor)
			sensorEngine = new SensorEngine(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//android.os.Debug.waitForDebugger();

		if (!isRunning) {
			isRunning = true;
			sendNotification();
			RunTask();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		//android.os.Debug.waitForDebugger();

		gsmEngine.onPause();

		if (sensorEngine != null)
			sensorEngine.onPause();

		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return localBinder;
	}

	private void sendNotification() {

		//android.os.Debug.waitForDebugger();

		Notification notif = new Notification(R.drawable.antenna, getString(R.string.service_notif_title), System.currentTimeMillis());

		Intent intentNotif = new Intent(this, MainActivity.class);
		PendingIntent pintent = PendingIntent.getActivity(this, 0, intentNotif, 0);

		notif.setLatestEventInfo(this, getString(R.string.service_notif_title), getString(R.string.service_notif_text), pintent);

		startForeground(1, notif);
	}

	private void sendErrorNotification() {

		//android.os.Debug.waitForDebugger();

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Notification notif = new Notification(R.drawable.antenna, getString(R.string.service_notif_title), System.currentTimeMillis());

		PendingIntent pIntentNotif = PendingIntent.getActivity(this, 0, new Intent(), 0);

		notif.setLatestEventInfo(this, getString(R.string.service_notif_title), getString(R.string.fs_error_msg), pIntentNotif);

		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(2, notif);
	}

	public long getTimeStart() {
		return timeStart;
	}

	public int getRecordsCount() {
		return recordsCount;
	}

	private void RunTask() {
		//android.os.Debug.waitForDebugger();

		thread = new Thread(new Runnable() {
			public void run() {

				boolean isFileSystemError = false;
				Intent intentStatus = new Intent(C.BROADCAST_ACTION);

				intentStatus.putExtra(C.PARAM_SERVICE_STATUS, C.STATUS_STARTED).putExtra(C.PARAM_TIME, timeStart);
				sendBroadcast(intentStatus);

				PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
				PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSMLoggerWakeLock");
				wakeLock.acquire();

				while (true) {
					MainActivity.checkOrCreateDirectory(MainActivity.dataDirPath);

					try {
						File csvFile = new File(MainActivity.csvLogFilePath);
						boolean isFileExist = csvFile.exists();
						PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

						if (!isFileExist) {
							pw.println(C.csvMarkHeader);
						}

						ArrayList<CellEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();

						for (CellEngine.GSMInfo gsmInfo : gsmInfoArray) {
							String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
									+ gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();
							
							pw.println(CellEngine.getItem(gsmInfo, active, "", C.logDefaultName, userName));
						}

						pw.close();
						
						Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(csvFile));
				    	sendBroadcast(intent);

						if (isSensor) {
							csvFile = new File(MainActivity.csvLogFilePathSensor);
							isFileExist = csvFile.exists();
							pw = new PrintWriter(new FileOutputStream(csvFile, true));

							if (!isFileExist)
								pw.println(C.csvHeaderSensor);

							pw.println(SensorEngine.getItem(sensorEngine, "", C.logDefaultName, userName, gsmInfoArray.get(0).getTimeStamp()));
							pw.close();

							intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(csvFile));
					    	sendBroadcast(intent);
						}

						intentStatus.putExtra(C.PARAM_SERVICE_STATUS, C.STATUS_RUNNING).putExtra(C.PARAM_RECORDS_COUNT,
								++recordsCount);

						sendBroadcast(intentStatus);

						Thread.sleep(interval * 1000);
					} catch (FileNotFoundException e) {
						isFileSystemError = true;
						break;

					} catch (InterruptedException e) {
						break;
					}

					if (Thread.currentThread().isInterrupted())
						break;
				}

				wakeLock.release();

				if (isFileSystemError) {
					sendErrorNotification();
					intentStatus.putExtra(C.PARAM_SERVICE_STATUS, C.STATUS_ERROR);

				} else {
					intentStatus.putExtra(C.PARAM_SERVICE_STATUS, C.STATUS_FINISHED).putExtra(C.PARAM_TIME,
							System.currentTimeMillis());

				}
				sendBroadcast(intentStatus);

				stopSelf();
			}
		});

		thread.start();
	}
}
