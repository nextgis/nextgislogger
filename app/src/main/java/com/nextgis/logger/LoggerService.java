/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Nikita Kirin
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
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
 * *****************************************************************************
 */
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
import android.net.Uri;
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
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;

public class LoggerService extends Service implements ArduinoEngine.ConnectionListener {
    private static final int ID_MEASURING = 1;

    private ArduinoEngine mArduinoEngine;
    private SensorEngine mSensorEngine;
    private CellEngine mGsmEngine;
    private Thread mThread = null;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPreferences;

    private volatile boolean mIsRunning = false;
    private int mRecordsCount;
    private int mInterval = 1;
    private int mBinders;
    private String mSessionId;
    private Uri mUri;

    private LocalBinder mLocalBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public CellEngine getCellEngine() {
            return mGsmEngine;
        }

        public SensorEngine getSensorEngine() {
            return mSensorEngine;
        }

        public ArduinoEngine getArduinoEngine() {
            return mArduinoEngine;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mGsmEngine = new CellEngine(this);
        mGsmEngine.onResume();

        mSensorEngine = new SensorEngine(this);
        mSensorEngine.onResume();

        mArduinoEngine = new ArduinoEngine(this);
        if (mArduinoEngine.isEngineEnabled()) {
            mArduinoEngine.addConnectionListener(this);
            mArduinoEngine.onResume();
        }

        mUri = Uri.parse("content://" + ((IGISApplication) getApplication()).getAuthority());
        mUri = mUri.buildUpon().appendPath(LoggerApplication.TABLE_MARK).build();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() != null ? intent.getAction() : "" : "";
        switch (action) {
            case LoggerConstants.ACTION_STOP:
                mPreferences.edit().putBoolean(LoggerConstants.PREF_MEASURING, false).apply();
                stopForeground(true);
                mIsRunning = false;

                if (mThread != null)
                    mThread.interrupt();

                if (--mBinders == 0) {
                    stopSelf();
                    return START_NOT_STICKY;
                }

                return START_REDELIVER_INTENT;
            case LoggerConstants.ACTION_START:
                if (!mIsRunning) {
                    mInterval = mPreferences.getInt(LoggerConstants.PREF_PERIOD_SEC, mInterval);
                    mSessionId = mPreferences.getString(LoggerConstants.PREF_SESSION_ID, null);
                    if (mSessionId == null) {
                        Intent intentStatus = new Intent(LoggerConstants.ACTION_INFO);
                        intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_ERROR);
                        sendBroadcast(intentStatus);
                        stopSelf();
                        return START_NOT_STICKY;
                    } else {
                        mIsRunning = true;
                        mRecordsCount = getRecordsCount();
                        sendNotification();
                        startMeasuring();
                        return START_REDELIVER_INTENT;
                    }
                }
            case LoggerConstants.ACTION_DESTROY:
                stopSelf();
                return START_NOT_STICKY;
            default:
                return START_REDELIVER_INTENT;
        }
    }

    private int getRecordsCount() {
        int result = 0;
        SQLiteDatabase db = ((MapContentProviderHelper) MapBase.getInstance()).getDatabase(true);
        Cursor count = db.rawQuery("SELECT COUNT(*) FROM " + LoggerApplication.TABLE_MARK + " WHERE " + LoggerApplication.FIELD_MARK_ID + " = -1 AND " +
                                           LoggerApplication.FIELD_SESSION + " = ?;", new String[]{mSessionId});

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
        mBinders++;
        return mLocalBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (--mBinders == 0 && !mIsRunning)
            stopSelf();

        return super.onUnbind(intent);
    }

    private void sendNotification() {
        Intent intentNotification = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intentNotification, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(pIntent).setSmallIcon(R.drawable.ic_status_notification)
               .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher)).setTicker(getString(R.string.service_notif_title))
               .setWhen(System.currentTimeMillis()).setAutoCancel(false).setContentTitle(getString(R.string.service_notif_title))
               .setContentText(getString(R.string.service_notif_text));

        Notification notification = builder.build();
        mNotificationManager.notify(ID_MEASURING, notification);
        startForeground(ID_MEASURING, notification);
    }

    private void startMeasuring() {
        mPreferences.edit().putBoolean(LoggerConstants.PREF_MEASURING, true).apply();
        mThread = new Thread(new Runnable() {
            public void run() {
                Intent intentStatus = new Intent(LoggerConstants.ACTION_INFO);
                intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_STARTED)
                            .putExtra(LoggerConstants.PREF_TIME_START, System.currentTimeMillis());
                sendBroadcast(intentStatus);

                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSMLoggerWakeLock");
                wakeLock.acquire();

                while (isRunning()) {
                    try {
                        GeoPoint point = GPSEngine.getFix(mSensorEngine.getData());
                        String markId = BaseEngine.saveMark(mUri, mSessionId, -1, LoggerConstants.LOG_UID, System.currentTimeMillis(), point);

                        if (!isRunning()) {
                            BaseEngine.deleteMark(mUri, markId);
                            break;
                        }

                        mGsmEngine.saveData(markId);

                        if (mSensorEngine.isEngineEnabled())
                            mSensorEngine.saveData(markId);

                        if (mArduinoEngine.isEngineEnabled())
                            mArduinoEngine.saveData(markId);

                        if (!isRunning())
                            break;

                        intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_RUNNING)
                                    .putExtra(LoggerConstants.PREF_RECORDS_COUNT, ++mRecordsCount);
                        sendBroadcast(intentStatus);
                        Thread.sleep(mInterval * 1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                mIsRunning = false;
                intentStatus.putExtra(LoggerConstants.SERVICE_STATUS, LoggerConstants.STATUS_FINISHED)
                            .putExtra(LoggerConstants.PREF_TIME_FINISH, System.currentTimeMillis());
                sendBroadcast(intentStatus);

                wakeLock.release();
            }

            private boolean isRunning() {
                return !mThread.isInterrupted() && mIsRunning;
            }
        });

        mThread.start();
    }

    @Override
    public void onTimeoutOrFailure() {
        mArduinoEngine.onResume();
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnectionLost() {
        mArduinoEngine.onResume();
    }
}
