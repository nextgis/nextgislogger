package com.nextgis.gsm_logger;

import android.content.Context;
import android.telephony.*;
import android.telephony.gsm.GsmCellLocation;

import java.util.List;


public class GSMEngine {

    Context mContext;

    private TelephonyManager mTelephonyManager;
    private GSMPhoneStateListener mSignalListener;
    private int signalStrength = -1;


    public GSMEngine(Context context) {
        mContext = context;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mSignalListener = new GSMPhoneStateListener();
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public void onResume() {
        mTelephonyManager.listen(mSignalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public void onPause() {
        mTelephonyManager.listen(mSignalListener, PhoneStateListener.LISTEN_NONE);
    }

//    private void onCellLocationChanged(CellLocation location) {
//
//        GsmCellLocation gsmCellLocation;
//        try {
//            gsmCellLocation = (GsmCellLocation) location;
//        } catch (ClassCastException e) {
//            gsmCellLocation = null;
//        }
//
//        if (gsmCellLocation != null) {
////            gsmCellLocation.getCid(), gsmCellLocation.getLac();
//        }
//    }

    private class GSMPhoneStateListener extends PhoneStateListener {

        /* Get the Signal strength from the provider, each time there is an update */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signal) {
            super.onSignalStrengthsChanged(signal);
            setSignalStrength(signal.isGsm() ? signal.getGsmSignalStrength() : -1);
        }

//        @Override
//        public void onCellLocationChanged(CellLocation location) {
//            super.onCellLocationChanged(location);
//            GSMEngine.this.onCellLocationChanged(location);
//        }
    }

    public String getGSMInfo() {

        int cellID = -1;
        int lac = -1;
        StringBuilder sbNeighborsInfo = new StringBuilder();

        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM &&
                // check, we are connected to the network...
                mTelephonyManager.getNetworkOperatorName() != null) {

            GsmCellLocation gsmCellLocation;
            try {
                gsmCellLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
            } catch (ClassCastException e) {
                gsmCellLocation = null;
            }

            if (gsmCellLocation != null) {

                cellID = gsmCellLocation.getCid();
                lac = gsmCellLocation.getLac();

                List<NeighboringCellInfo> neighbors = mTelephonyManager.getNeighboringCellInfo();

                for (NeighboringCellInfo neighbor : neighbors) {

                    int nbNetworkType = neighbor.getNetworkType();

                    if (nbNetworkType ==  TelephonyManager.NETWORK_TYPE_GPRS ||
                            nbNetworkType == TelephonyManager.NETWORK_TYPE_EDGE) {

                        sbNeighborsInfo.append(MainActivity.CSV_SEPARATOR)
                                .append("[")
                                .append(neighbor.getCid()).append(",")
                                .append(neighbor.getLac()).append(",")
                                .append(neighbor.getRssi()).append(",")
                                .append("]");
                    }
                }
            }
        }

        StringBuilder sbCellInfo = new StringBuilder();

        sbCellInfo.append(System.currentTimeMillis()).append(MainActivity.CSV_SEPARATOR)
                .append(cellID).append(MainActivity.CSV_SEPARATOR)
                .append(lac).append(MainActivity.CSV_SEPARATOR)
                .append(signalStrength);

        if (sbNeighborsInfo.length() > 0)
            sbCellInfo.append(sbNeighborsInfo);

        return sbCellInfo.toString();
    }
}
