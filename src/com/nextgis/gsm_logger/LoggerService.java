package com.nextgis.gsm_logger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class LoggerService extends Service {

    private boolean isRunning = false;
    private Thread thread = null;


    @Override
    public void onCreate() {
        super.onCreate();

        //android.os.Debug.waitForDebugger();

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

        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNotification() {

        //android.os.Debug.waitForDebugger();

        Notification notif = new Notification(
                R.drawable.ic_launcher,
                getString(R.string.service_notif_title),
                System.currentTimeMillis());

        Intent intentNotif = new Intent(this, MainActivity.class);
        PendingIntent pIntentNotif = PendingIntent.getActivity(this, 0, intentNotif, 0);

        notif.setLatestEventInfo(this,
                getString(R.string.service_notif_title),
                getString(R.string.service_notif_text),
                pIntentNotif);

        startForeground(1, notif);
    }


    private void RunTask() {
        //android.os.Debug.waitForDebugger();

        thread = new Thread(new Runnable() {
            public void run() {

                int i = 0;

                while (true) {
                    Log.d("logTimerService", "i = " + (++i));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }

                    if (Thread.currentThread().isInterrupted())
                        break;
                }

                stopSelf();
            }
        });

        thread.start();
    }
}
