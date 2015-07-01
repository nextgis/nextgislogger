/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015 NextGIS
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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextgis.logger.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// http://stackoverflow.com/q/10327506
public class ArduinoEngine extends BaseEngine {
    private final static byte DELIMITER = 10;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private byte[] mBuffer;
    private int mBufferPosition;

    private volatile boolean mIsWorking, mIsConnectionLost;
    private String mData, mDeviceName, mDeviceMAC;
    private Thread mWorkerThread;
    private List<ConnectionListener> mConnectionListeners;

    private volatile int mTemperature, mHumidity, mNoise;
    private volatile double mCO, mCH4, mC4H10;

    public boolean isLogEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Constants.PREF_EXTERNAL, false);
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
        String nameWithMAC = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_EXTERNAL_DEVICE, "");
        mDeviceMAC = splitDeviceMAC(nameWithMAC);
        mDeviceName = splitDeviceName(nameWithMAC);

        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) && device.getAddress().equals(mDeviceMAC)) {
                    mIsConnectionLost = true;
                    mData = null;

                    for (ConnectionListener listener : mConnectionListeners)
                        listener.onConnectionLost();

                    if (!mIsWorking)
                        closeConnection();
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mReceiver, filter);
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

    public void onPause() {
        if (hasListeners())
            return;

        closeConnection();
    }

    public void onResume() {
        if (hasListeners() && (mWorkerThread != null && mWorkerThread.isAlive()))
            return;

        mIsWorking = true;
        mBufferPosition = 0;
        mBuffer = new byte[1024];
        mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while (mIsWorking) {
                    try {
                        if (!isDeviceAvailable() || !isConnected())
                            openConnection();

                        int bytesAvailable = mInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            int readLength = mInputStream.read(packetBytes);

                            for (int i = 0; i < readLength; i++) {
                                byte b = packetBytes[i];

                                if (b == DELIMITER) {
                                    byte[] encodedBytes = new byte[mBufferPosition];
                                    System.arraycopy(mBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    mData = new String(encodedBytes, "US-ASCII");
                                    parseData();
                                    mBufferPosition = 0;
                                } else {
                                    mBuffer[mBufferPosition++] = b;
                                }
                            }

                            notifyListeners();
                        }
                    } catch (IOException e) {
                        mData = null;

                        for (ConnectionListener listener : mConnectionListeners)
                            listener.onConnectionLost();

                        if (!hasListeners())
                            mIsWorking = false;
                    } catch (NullPointerException ignored) {
                    }
                }

                closeConnection();
            }
        });

        mWorkerThread.start();
    }

    private void parseData() {
        String[] fields = mData.split(";");

        if (!TextUtils.isEmpty(mData) && fields.length == 6) {
            mTemperature = Integer.parseInt(fields[0]);
            mHumidity = Integer.parseInt(fields[1]);
            mNoise = Integer.parseInt((fields[2]));
            mCO = Double.parseDouble(fields[3]);
            mC4H10 = Double.parseDouble(fields[4]);
            mCH4 = Double.parseDouble(fields[5]);
        }
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

        mIsConnectionLost = false;
        for (ConnectionListener listener : mConnectionListeners)
            listener.onConnected();
    }

    public String getData() {
        return mData;
    }

    public int getTemperature() {
        return mTemperature;
    }

    public int getHumidity() {
        return mHumidity;
    }

    public int getNoise() {
        return mNoise;
    }

    public double getCO() {
        return mCO;
    }

    public double getC4H10() {
        return mC4H10;
    }

    public double getCH4() {
        return mCH4;
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
        mData = null;
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

    public String getItem(String ID, String markName, String userName, long timeStamp) {
        StringBuilder sb = new StringBuilder();

        sb.append(ID).append(Constants.CSV_SEPARATOR);
        sb.append(markName).append(Constants.CSV_SEPARATOR);
        sb.append(userName).append(Constants.CSV_SEPARATOR);
        sb.append(timeStamp).append(Constants.CSV_SEPARATOR);
        sb.append(getData());

        sb.length();
        return sb.toString();
    }
}
