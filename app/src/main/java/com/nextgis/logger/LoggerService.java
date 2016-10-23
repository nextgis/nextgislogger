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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.CellEngine;
import com.nextgis.logger.engines.GPSEngine;
import com.nextgis.logger.engines.SensorEngine;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.util.Constants;

public class LoggerService extends Service implements ArduinoEngine.ConnectionListener {
    // TODO
    // 1. move engines to service
    // 2. run while app active
    // 3. separate process

    private static ArduinoEngine mArduinoEngine;
    private static SensorEngine mSensorEngine;
    private static CellEngine mGsmEngine;
	private Thread mThread = null;
    private NotificationManager mNotificationManager;

	private boolean mIsRunning = false;
	private int mRecordsCount;
	private int mInterval = 1;
	private long mSessionId;

	@Override
	public void onCreate() {
		super.onCreate();

        Log.d("LOGGER", "onCreate");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mInterval = prefs.getInt(LoggerConstants.PREF_PERIOD_SEC, mInterval);
        mSessionId = prefs.getLong(LoggerConstants.PREF_SESSION_ID, Constants.NOT_FOUND);

		mGsmEngine = LoggerApplication.getApplication().getCellEngine();
		mGsmEngine.onResume();

		mSensorEngine = LoggerApplication.getApplication().getSensorEngine();
        mSensorEngine.onResume();

        mArduinoEngine = LoggerApplication.getApplication().getArduinoEngine();
		if (mArduinoEngine.isEngineEnabled()) {
			mArduinoEngine.addConnectionListener(this);
			mArduinoEngine.onResume();
		}

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() != null ? intent.getAction() : "" : "";
        Log.d("LOGGER", "onStartCommand: " + action);
        switch (action) {
            case LoggerConstants.ACTION_STOP:
                Log.d("LOGGER", "mThread: " + mThread);
                if (mThread != null)
                    mThread.interrupt();

                stopSelf();
                return START_NOT_STICKY;
            case LoggerConstants.ACTION_START:
            default:
                if (!mIsRunning) {
                    mIsRunning = true;
                    mRecordsCount = getRecordsCount();
                    sendNotification();
                    startMeasuring();
                }
                return START_STICKY;
        }
	}

    private int getRecordsCount() {
        int result = 0;
        SQLiteDatabase db = ((MapContentProviderHelper) MapBase.getInstance()).getDatabase(false);
        Cursor count = db.rawQuery("SELECT COUNT(*) FROM " + LoggerApplication.TABLE_MARK + " WHERE " + LoggerApplication.FIELD_MARK_ID + " = -1 AND " +
                LoggerApplication.FIELD_SESSION + " = ?;", new String[]{mSessionId + ""});

        if (count != null) {
            if (count.moveToFirst())
                result = count.getInt(0);

            count.close();
        }

        return result;
    }

    @Override
	public void onDestroy() {
		super.onDestroy();

		mGsmEngine.onPause();
		mSensorEngine.onPause();
        mArduinoEngine.removeConnectionListener(this);
        mArduinoEngine.onPause();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
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
        mNotificationManager.notify(1, notification);
		startForeground(1, notification);
	}

	private void startMeasuring() {
		mThread = new Thread(new Runnable() {
			public void run() {
				Intent intentStatus = new Intent(LoggerConstants.ACTION_INFO);
				intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_STARTED)
                        .putExtra(LoggerConstants.PREF_TIME_START, System.currentTimeMillis());
				sendBroadcast(intentStatus);

				PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
				PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSMLoggerWakeLock");
				wakeLock.acquire();

				while (true) {
					try {
						GeoPoint point = GPSEngine.getFix(mSensorEngine.getData());
						long markId = BaseEngine.saveMark(mSessionId, -1, LoggerConstants.LOG_UID, System.currentTimeMillis(), point);
						mGsmEngine.saveData(markId);

						if (mSensorEngine.isEngineEnabled())
							mSensorEngine.saveData(markId);

						if (mArduinoEngine.isEngineEnabled())
							mArduinoEngine.saveData(markId);

						intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_RUNNING)
								.putExtra(LoggerConstants.PREF_RECORDS_COUNT, ++mRecordsCount);
						sendBroadcast(intentStatus);

                        Log.d("LOGGER", "sleep");
						Thread.sleep(mInterval * 1000);
					} catch (InterruptedException e) {
						break;
					}

					if (Thread.currentThread().isInterrupted())
						break;
				}

                Log.d("LOGGER", "end while");
                intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_FINISHED)
                        .putExtra(LoggerConstants.PREF_TIME_FINISH, System.currentTimeMillis());
                sendBroadcast(intentStatus);

				wakeLock.release();
			}
		});

		mThread.start();
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
