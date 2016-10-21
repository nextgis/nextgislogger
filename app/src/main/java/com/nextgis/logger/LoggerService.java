/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Nikita Kirin
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
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
import android.support.v4.app.NotificationCompat;

import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.CellEngine;
import com.nextgis.logger.engines.GPSEngine;
import com.nextgis.logger.engines.SensorEngine;
import com.nextgis.logger.util.FileUtil;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.GeoPoint;

public class LoggerService extends Service implements ArduinoEngine.ConnectionListener {

	private static long timeStart = 0;
	private static int recordsCount = 0;
	private static int interval = 1;

	private boolean isRunning = false;

	private static ArduinoEngine mArduinoEngine;
    private static SensorEngine mSensorEngine;
    private static CellEngine mGsmEngine;
	private Thread thread = null;
	private LocalBinder localBinder = new LocalBinder();
    private NotificationManager notificationManager;

    public class LocalBinder extends Binder {
		public LoggerService getService() {
			return LoggerService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		if (timeStart == 0) {
			timeStart = System.currentTimeMillis();
		}

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        interval = prefs.getInt(LoggerConstants.PREF_PERIOD_SEC, interval);

		mGsmEngine = LoggerApplication.getApplication().getCellEngine();
		mGsmEngine.onResume();

		mSensorEngine = LoggerApplication.getApplication().getSensorEngine();
        mSensorEngine.onResume();

        mArduinoEngine = LoggerApplication.getApplication().getArduinoEngine();
		if (mArduinoEngine.isEngineEnabled()) {
			mArduinoEngine.addConnectionListener(this);
			mArduinoEngine.onResume();
		}

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

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

		mGsmEngine.onPause();
		mSensorEngine.onPause();
        mArduinoEngine.removeConnectionListener(this);
        mArduinoEngine.onPause();

		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return localBinder;
	}

	private void sendNotification() {
		Intent intentNotification = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intentNotification, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(pIntent)
                .setSmallIcon(R.drawable.ic_status_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setTicker(getString(R.string.service_notif_title))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.service_notif_title))
                .setContentText(getString(R.string.service_notif_text));

        Notification notification = builder.build();
        notificationManager.notify(1, notification);
		startForeground(1, notification);
	}

	private void sendErrorNotification() {
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(pIntent)
                .setSmallIcon(R.drawable.ic_status_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setTicker(getString(R.string.service_notif_title))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.service_notif_title))
                .setContentText(getString(R.string.fs_error_msg));

        Notification notification = builder.build();
		notificationManager.notify(2, notification);
	}

	public long getTimeStart() {
		return timeStart;
	}

	public int getRecordsCount() {
		return recordsCount;
	}

	private void RunTask() {
		thread = new Thread(new Runnable() {
			public void run() {
				Intent intentStatus = new Intent(LoggerConstants.BROADCAST_ACTION);

				intentStatus.putExtra(LoggerConstants.PARAM_SERVICE_STATUS, LoggerConstants.STATUS_STARTED).putExtra(LoggerConstants.PARAM_TIME, timeStart);
				sendBroadcast(intentStatus);

				PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
				PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSMLoggerWakeLock");
				wakeLock.acquire();

				while (true) {
					FileUtil.checkOrCreateDirectory(MainActivity.dataDirPath);

					try {
						// TODO session id
						GeoPoint point = GPSEngine.getFix(mSensorEngine.getData());
						long markId = BaseEngine.saveMark(0, -1, LoggerConstants.LOG_UID, System.currentTimeMillis(), point);
						mGsmEngine.saveData(markId);

						if (mSensorEngine.isEngineEnabled())
							mSensorEngine.saveData(markId);

						if (mArduinoEngine.isEngineEnabled())
							mArduinoEngine.saveData(markId);

						intentStatus.putExtra(LoggerConstants.PARAM_SERVICE_STATUS, LoggerConstants.STATUS_RUNNING).putExtra(LoggerConstants.PARAM_RECORDS_COUNT, ++recordsCount);
						sendBroadcast(intentStatus);

						Thread.sleep(interval * 1000);
					} catch (InterruptedException e) {
						break;
					}

					if (Thread.currentThread().isInterrupted())
						break;
				}

				wakeLock.release();

				intentStatus.putExtra(LoggerConstants.PARAM_SERVICE_STATUS, LoggerConstants.STATUS_FINISHED)
						.putExtra(LoggerConstants.PARAM_TIME, System.currentTimeMillis());

				sendBroadcast(intentStatus);

				stopSelf();
			}
		});

		thread.start();
	}

    @Override
    public void onTimeoutOrFailure() {

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnectionLost() {

    }
}
