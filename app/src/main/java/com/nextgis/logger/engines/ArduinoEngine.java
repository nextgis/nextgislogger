/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015-2016 NextGIS
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

package com.nextgis.logger.engines;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// http://stackoverflow.com/q/10327506
public class ArduinoEngine extends BaseEngine {
    private final static byte DELIMITER = 10;
    private final static char GET_HEADER = 'h';
    private final static char GET_DATA = 'd';

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private byte[] mBuffer;
    private int mBufferPosition;

    private volatile boolean mIsWorking, mIsConnectionLost, mIsFirstConnect;
    private String mLine, mDeviceName, mDeviceMAC;
    private Thread mWorkerThread;
    private List<ConnectionListener> mConnectionListeners;
    private BroadcastReceiver mReceiver;

    @Override
    public boolean isEngineEnabled() {
        return getPreferences().getBoolean(LoggerConstants.PREF_EXTERNAL, false);
    }

    public interface ConnectionListener {
        void onTimeoutOrFailure();

        void onConnected();

        void onConnectionLost();
    }

    public ArduinoEngine(Context context) {
        super(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectionListeners = new ArrayList<>();
        String nameWithMAC = PreferenceManager.getDefaultSharedPreferences(context).getString(LoggerConstants.PREF_EXTERNAL_DEVICE, "");
        mDeviceMAC = splitDeviceMAC(nameWithMAC);
        mDeviceName = splitDeviceName(nameWithMAC);
        mUri = mUri.buildUpon().appendPath(LoggerApplication.TABLE_EXTERNAL).build();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) && device.getAddress().equals(mDeviceMAC)) {
                    mIsConnectionLost = true;
                    mLine = null;
                    clearData();

                    for (ConnectionListener listener : mConnectionListeners)
                        listener.onConnectionLost();

                    if (!mIsWorking)
                        closeConnection();
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mReceiver, filter);

        mLine = getPreferences().getString(LoggerConstants.PREF_EXTERNAL_HEADER, "");
        loadEngine();
        mLine = null;
        clearData();
    }

    public boolean removeListener(BaseEngine.EngineListener listener) {
        boolean result = super.removeListener(listener);

        if (!hasListeners())
            closeConnection();

        return result;
    }

    public void addConnectionListener(ConnectionListener listener) {
        mConnectionListeners.add(listener);
    }

    public boolean removeConnectionListener(ConnectionListener listener) {
        return mConnectionListeners.remove(listener);
    }

    private boolean hasListeners() {
        return mConnectionListeners.size() > 0 || getListenersCount() > 0;
    }

    @Override
    public boolean onPause() {
        if (hasListeners())
            return false;

        mContext.unregisterReceiver(mReceiver);
        closeConnection();
        return false;
    }

    @Override
    public void saveData(String markId) {
        saveData(getData(), markId);
    }

    @Override
    public void saveData(ArrayList<InfoItem> items, String markId) {
        NGWVectorLayer externalLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_EXTERNAL);
        if (externalLayer != null) {
            ContentValues cv = new ContentValues();
            cv.put(LoggerApplication.FIELD_MARK, markId);

            String data = "";
            for (InfoItem item : items)
                data += item.getColumns().get(0).getValue();

            cv.put(LoggerApplication.FIELD_DATA, data);
            try {
                cv.put(Constants.FIELD_GEOM, new GeoPoint(0, 0).toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }

            externalLayer.insert(mUri, cv);
        }
    }

    @Override
    public boolean onResume() {
        if (hasListeners() && (mWorkerThread != null && mWorkerThread.isAlive()))
            return false;

        mIsWorking = true;
        mBufferPosition = 0;
        mBuffer = new byte[1024];
        mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while (mIsWorking) {
                    try {
                        if (!isDeviceAvailable() || !isConnected())
                            openConnection();

                        if (mIsFirstConnect)
                            getExternalHeader();

                        mOutputStream.write(GET_DATA);
                        mLine = readln();
                        if (parseData())
                            notifyListeners("EXTERNAL");

                        SystemClock.sleep(LoggerConstants.UPDATE_FREQUENCY);
                    } catch (IOException e) {
                        mLine = null;
                        clearData();

                        for (ConnectionListener listener : mConnectionListeners)
                            listener.onConnectionLost();

                        if (!hasListeners())
                            mIsWorking = false;
                    } catch (NullPointerException ignored) { }
                }

                closeConnection();
            }
        });

        mWorkerThread.start();
        return false;
    }

    private void clearData() {
        for (InfoItem item : mItems)
            item.getColumns().get(0).setValue(LoggerConstants.NO_DATA);
    }

    private synchronized void getExternalHeader() throws IOException {
        mOutputStream.write(GET_HEADER);
        SystemClock.sleep(LoggerConstants.UPDATE_FREQUENCY);
        mLine = readln();
        loadEngine();
        getPreferences().edit().putString(LoggerConstants.PREF_EXTERNAL_HEADER, mLine).apply();
        mIsFirstConnect = false;

        for (ConnectionListener listener : mConnectionListeners)
            listener.onConnected();
    }

    @Override
    protected synchronized void loadEngine() {
        try {
            JSONObject json = new JSONObject(mLine);
            mItems.clear();

            Iterator<String> sensors = json.keys();
            while (sensors.hasNext()) {
                String key = sensors.next();
                JSONObject sensor = json.getJSONObject(key);
                InfoItem item = new InfoItem(sensor.getString("full"));
                item.addColumn(sensor.getString("short"), null, sensor.getString("unit"));
                mItems.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private synchronized String readln() throws IOException {
        String line = null;
        int bytesAvailable = mInputStream.available();

        if (bytesAvailable > 0) {
            byte[] packetBytes = new byte[bytesAvailable];
            int readLength = mInputStream.read(packetBytes);

            for (int i = 0; i < readLength; i++) {
                byte b = packetBytes[i];

                if (b == DELIMITER) {
                    byte[] encodedBytes = new byte[mBufferPosition];
                    System.arraycopy(mBuffer, 0, encodedBytes, 0, encodedBytes.length);
                    line = new String(encodedBytes, "UTF-8");
                    mBufferPosition = 0;
                } else {
                    mBuffer[mBufferPosition++] = b;
                }
            }
        }

        return line;
    }

    private synchronized boolean parseData() {
        String[] fields = mLine.split(";");
        boolean isCorrect = !TextUtils.isEmpty(mLine) && fields.length == mItems.size();

        if (isCorrect)
            for (int i = 0; i < fields.length; i++)
                mItems.get(i).getColumns().get(0).setValue(fields[i]);

        return isCorrect;
    }

    public int getSensorsCount() {
        return mItems.size();
    }

    private void openConnection() {
        try {
            findBT();
            openBT();
        } catch (IOException e) {
            for (ConnectionListener listener : mConnectionListeners)
                listener.onTimeoutOrFailure();

            mIsConnectionLost = true;
            return;
        }

        mIsFirstConnect = true;
        mIsConnectionLost = false;
    }

    private void findBT() throws IOException {
        if (!isBTEnabled()) {
            throw new IOException("Bluetooth adapter is not available");
        }

        Set<BluetoothDevice> pairedDevices = getPairedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getAddress().equals(mDeviceMAC)) {
                mDevice = device;
                return;
            }
        }

        throw new IOException("Can not find Arduino's bluetooth");
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (mBluetoothAdapter != null)
            return mBluetoothAdapter.getBondedDevices();
        else
            return Collections.emptySet();
    }

    public boolean isBTEnabled() {
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
    }

    public boolean isDeviceAvailable() {
        return (isBTEnabled() && mDevice != null);
    }

    private void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
        mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
        mSocket.connect();
        mOutputStream = mSocket.getOutputStream();
        mInputStream = mSocket.getInputStream();
    }

    public boolean isConnected() {
        return (mInputStream != null && mOutputStream != null && mSocket != null && !mIsConnectionLost);
    }

    private void closeConnection() {
        mIsWorking = false;

        try {
            if (mSocket != null)
                mSocket.close();

            if (mInputStream != null)
                mInputStream.close();

            if (mOutputStream != null)
                mOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mOutputStream = null;
        mInputStream = null;
        mSocket = null;
        mDevice = null;
        mLine = null;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String newName) {
        mDeviceName = newName;
    }

    public void setDeviceMAC(String newMAC) {
        mDeviceMAC = newMAC;
    }

    public String splitDeviceMAC(String nameWithMAC) {
        int i = nameWithMAC.indexOf("(");

        if (i > 0)
            return nameWithMAC.substring(i + 1, nameWithMAC.length() - 1);
        else
            return null;
    }

    public String splitDeviceName(String nameWithMAC) {
        int i = nameWithMAC.indexOf("(");

        if (i > 0)
            return nameWithMAC.substring(0, i - 1);
        else
            return null;
    }

    public static String getDataFromCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(LoggerApplication.FIELD_DATA));
    }
}
