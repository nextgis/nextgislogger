package com.nextgis.gsm_logger;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class LoggerService extends Service {

	private static long timeStart = 0;
	private static int recordsCount = 0;

	private boolean isRunning = false;
	private boolean isSensor = true;

	private GSMEngine gsmEngine;
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

		gsmEngine = new GSMEngine(this);
		gsmEngine.onResume();

		isSensor = getSharedPreferences(MainActivity.PREFERENCE_NAME, MODE_PRIVATE).getBoolean(MainActivity.PREF_SENSOR_STATE, true);

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
				Intent intentStatus = new Intent(MainActivity.BROADCAST_ACTION);

				intentStatus.putExtra(MainActivity.PARAM_SERVICE_STATUS, MainActivity.STATUS_STARTED).putExtra(MainActivity.PARAM_TIME, timeStart);
				sendBroadcast(intentStatus);

				PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
				PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSMLoggerWakeLock");
				wakeLock.acquire();

				while (true) {

					try {
						SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCE_NAME, MODE_PRIVATE);
						File csvFile = new File(MainActivity.csvLogFilePath);
						boolean isFileExist = csvFile.exists();
						PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

						if (!isFileExist) {
							pw.println(MainActivity.csvMarkHeader);
						}

						ArrayList<GSMEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();

						for (GSMEngine.GSMInfo gsmInfo : gsmInfoArray) {
							StringBuilder sb = new StringBuilder();

							String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
									+ gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();

							sb.append("").append(MainActivity.CSV_SEPARATOR);
							sb.append(MainActivity.logDefaultName).append(MainActivity.CSV_SEPARATOR);
							sb.append(prefs.getString(MainActivity.PREF_USER_NAME, "User 1")).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getTimeStamp()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.networkGen()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.networkType()).append(MainActivity.CSV_SEPARATOR);
							sb.append(active).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getMcc()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getMnc()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getLac()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getCid()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getPsc()).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfo.getRssi());

							pw.println(sb.toString());
						}

						pw.close();
						
						Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(csvFile));
				    	sendBroadcast(intent);

						if (isSensor) {
							csvFile = new File(MainActivity.csvLogFilePathSensor);
							isFileExist = csvFile.exists();
							pw = new PrintWriter(new FileOutputStream(csvFile, true));

							if (!isFileExist)
								pw.println(MainActivity.csvHeaderSensor);

							StringBuilder sb = new StringBuilder();

							sb.append("").append(MainActivity.CSV_SEPARATOR);
							sb.append(MainActivity.logDefaultName).append(MainActivity.CSV_SEPARATOR);
							sb.append(prefs.getString(MainActivity.PREF_USER_NAME, "User 1")).append(MainActivity.CSV_SEPARATOR);
							sb.append(gsmInfoArray.get(0).getTimeStamp()).append(MainActivity.CSV_SEPARATOR);
							sb.append(sensorEngine.getSensorType()).append(MainActivity.CSV_SEPARATOR);
							sb.append(sensorEngine.getX()).append(MainActivity.CSV_SEPARATOR);
							sb.append(sensorEngine.getY()).append(MainActivity.CSV_SEPARATOR);
							sb.append(sensorEngine.getZ());

							pw.println(sb.toString());
							pw.close();

							intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(csvFile));
					    	sendBroadcast(intent);
						}

						intentStatus.putExtra(MainActivity.PARAM_SERVICE_STATUS, MainActivity.STATUS_RUNNING).putExtra(MainActivity.PARAM_RECORDS_COUNT,
								++recordsCount);

						sendBroadcast(intentStatus);

						Thread.sleep(prefs.getInt(MainActivity.PREF_PERIOD_SEC, 1) * 1000);

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
					intentStatus.putExtra(MainActivity.PARAM_SERVICE_STATUS, MainActivity.STATUS_ERROR);

				} else {
					intentStatus.putExtra(MainActivity.PARAM_SERVICE_STATUS, MainActivity.STATUS_FINISHED).putExtra(MainActivity.PARAM_TIME,
							System.currentTimeMillis());

				}
				sendBroadcast(intentStatus);

				stopSelf();
			}
		});

		thread.start();
	}
}
