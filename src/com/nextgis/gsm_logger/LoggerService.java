package com.nextgis.gsm_logger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;


public class LoggerService extends Service {

    private GSMEngine mGSMEngine;

    private String mCSVLogFileName = "gsm_time_log.csv";

    private Thread thread = null;
    private boolean isRunning = false;
    private boolean isFileSystemError = false;

    @Override
    public void onCreate() {
        super.onCreate();

        //android.os.Debug.waitForDebugger();

        mGSMEngine = new GSMEngine(this);
        mGSMEngine.onResume();

        mCSVLogFileName = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath() + File.separator + mCSVLogFileName;

        try {
            File csvFile = new File(mCSVLogFileName);

            if (!csvFile.exists()) {
                StringBuilder sbHeader = new StringBuilder();
                sbHeader.append("TimeStamp").append(MainActivity.CSV_SEPARATOR)
                        .append("CellID").append(MainActivity.CSV_SEPARATOR)
                        .append("LAC").append(MainActivity.CSV_SEPARATOR)
                        .append("[Neighbor_CellID,Neighbor_LAC,Neighbor_RSSI]...");

                PrintWriter pw = new PrintWriter(csvFile);
                pw.println(sbHeader.toString());
                pw.close();
            }

        } catch (FileNotFoundException e) {
            isFileSystemError = true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //android.os.Debug.waitForDebugger();

        if (!isRunning) {
            isRunning = true;
            PendingIntent pintent = intent.getParcelableExtra(MainActivity.PARAM_PINTENT);

            sendNotification();
            RunTask(pintent);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //android.os.Debug.waitForDebugger();

        mGSMEngine.onPause();

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
        PendingIntent pintent = PendingIntent.getActivity(this, 0, intentNotif, 0);

        notif.setLatestEventInfo(this,
                getString(R.string.service_notif_title),
                getString(R.string.service_notif_text),
                pintent);

        startForeground(1, notif);
    }

    private void RunTask(final PendingIntent pintent) {
        //android.os.Debug.waitForDebugger();

        thread = new Thread(new Runnable() {
            public void run() {

                while (!isFileSystemError) {

                    try {
                        PrintWriter pw = new PrintWriter(
                                new FileOutputStream(mCSVLogFileName, true));
                        pw.println(mGSMEngine.getGSMInfo());
                        pw.close();

                        Thread.sleep(1000);

                    } catch (FileNotFoundException e) {
                        isFileSystemError = true;
                        break;

                    } catch (InterruptedException e) {
                        break;
                    }

                    if (Thread.currentThread().isInterrupted())
                        break;
                }

                if (isFileSystemError) {
                    try {
                        pintent.send(MainActivity.STATUS_ERROR);
                    } catch (PendingIntent.CanceledException e) {
                        // TODO: PendingIntent.CanceledException
                        //e.printStackTrace();
                    }
                }

                stopSelf();
            }
        });

        thread.start();
    }
}
