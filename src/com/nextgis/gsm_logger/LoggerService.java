package com.nextgis.gsm_logger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;


public class LoggerService extends Service {

    private GSMEngine gsmEngine;
    private Thread thread = null;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        //android.os.Debug.waitForDebugger();

        gsmEngine = new GSMEngine(this);
        gsmEngine.onResume();
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

        gsmEngine.onPause();

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
                R.drawable.antenna,
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

                boolean isFileSystemError = false;

                while (true) {

                    try {
                        File csvFile = new File(MainActivity.csvLogFilePath);
                        boolean isFileExist = csvFile.exists();
                        PrintWriter pw = new PrintWriter(new FileOutputStream(csvFile, true));

                        if (!isFileExist) {
                            pw.println(MainActivity.csvLogHeader);
                        }

                        ArrayList<GSMEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();

                        for (GSMEngine.GSMInfo gsmInfo : gsmInfoArray) {
                            StringBuilder sb = new StringBuilder();

                            String active = gsmInfo.isActive() ? "1"
                                    : gsmInfoArray.get(0).getMcc() + "-" +
                                    gsmInfoArray.get(0).getMnc() + "-" +
                                    gsmInfoArray.get(0).getLac() + "-" +
                                    gsmInfoArray.get(0).getCid();

                            sb.append(gsmInfo.getTimeStamp()).append(MainActivity.CSV_SEPARATOR);
                            sb.append(active).append(MainActivity.CSV_SEPARATOR);
                            sb.append(gsmInfo.getMcc()).append(MainActivity.CSV_SEPARATOR);
                            sb.append(gsmInfo.getMnc()).append(MainActivity.CSV_SEPARATOR);
                            sb.append(gsmInfo.getLac()).append(MainActivity.CSV_SEPARATOR);
                            sb.append(gsmInfo.getCid()).append(MainActivity.CSV_SEPARATOR);
                            sb.append(gsmInfo.getRssi());

                            pw.println(sb.toString());
                        }

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
