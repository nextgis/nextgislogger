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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class LoggerService extends Service {

	private static long timeStart = 0;
	private static int recordsCount = 0;
	private static int interval = 1;

	private boolean isRunning = false;

	private CellEngine gsmEngine;
	private SensorEngine sensorEngine;
	private Thread thread = null;
	private LocalBinder localBinder = new LocalBinder();
    private NotificationManager notificationManager;
    private String userName;

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
        userName = prefs.getString(C.PREF_USER_NAME, C.DEFAULT_USERNAME);
		interval = prefs.getInt(C.PREF_PERIOD_SEC, interval);

		sensorEngine = new SensorEngine(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//android.os.Debug.waitForDebugger();

		if (!isRunning) {
			isRunning = true;
			recordsCount = 0;
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
		Intent intentNotif = new Intent(this, MainActivity.class);
		PendingIntent pintent = PendingIntent.getActivity(this, 0, intentNotif, 0);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pintent)
                .setSmallIcon(R.drawable.ic_status_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setTicker(getString(R.string.service_notif_title))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.service_notif_title))
                .setContentText(getString(R.string.service_notif_text));

        Notification notif = builder.getNotification();
        notificationManager.notify(1, notif);
		startForeground(1, notif);
	}

	private void sendErrorNotification() {
		//android.os.Debug.waitForDebugger();
		PendingIntent pIntentNotif = PendingIntent.getActivity(this, 0, new Intent(), 0);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pIntentNotif)
                .setSmallIcon(R.drawable.ic_status_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setTicker(getString(R.string.service_notif_title))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.service_notif_title))
                .setContentText(getString(R.string.fs_error_msg));

        Notification notif = builder.getNotification();
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
					FileUtil.checkOrCreateDirectory(MainActivity.dataDirPath);

					try {
						ArrayList<CellEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();

						for (CellEngine.GSMInfo gsmInfo : gsmInfoArray) {
							String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
									+ gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();

                            FileUtil.saveItemToLog(C.LOG_TYPE_NETWORK, false, CellEngine.getItem(gsmInfo, active, "", C.logDefaultName, userName));
						}

						if (sensorEngine.isAnySensorEnabled()) {
                            FileUtil.saveItemToLog(C.LOG_TYPE_SENSORS, false,
                                    SensorEngine.getItem(sensorEngine, "", C.logDefaultName, userName, gsmInfoArray.get(0).getTimeStamp()));
						}

						intentStatus.putExtra(C.PARAM_SERVICE_STATUS, C.STATUS_RUNNING).putExtra(C.PARAM_RECORDS_COUNT, ++recordsCount);
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
