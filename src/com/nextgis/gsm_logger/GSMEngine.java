package com.nextgis.gsm_logger;

import android.content.Context;
import android.telephony.*;
import android.telephony.gsm.GsmCellLocation;

import java.util.ArrayList;
import java.util.List;


public class GSMEngine {

    Context mContext;

    public final static int SIGNAL_STRENGTH_NONE = 0;

    private TelephonyManager mTelephonyManager;
    private GSMPhoneStateListener mSignalListener;
    private int signalStrength = SIGNAL_STRENGTH_NONE;


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
            setSignalStrength(signal.isGsm()
                    ? signalStrengthAsuToDbm(
                            signal.getGsmSignalStrength(), TelephonyManager.NETWORK_TYPE_GPRS)
                    : SIGNAL_STRENGTH_NONE);
        }

//        @Override
//        public void onCellLocationChanged(CellLocation location) {
//            super.onCellLocationChanged(location);
//            GSMEngine.this.onCellLocationChanged(location);
//        }
    }

    public int signalStrengthAsuToDbm(int asu, int networkType) {

        switch (networkType) {

            // 2G -- GSM network
            case TelephonyManager.NETWORK_TYPE_GPRS : // API 1+
            case TelephonyManager.NETWORK_TYPE_EDGE : // API 1+
                if (0 <= asu && asu <= 31) {
                    return 2 * asu - 113;
                } else if (asu == 99) {
                    return 0;
                }
                break;

            // 3G -- UMTS network
//            case TelephonyManager.NETWORK_TYPE_UMTS : // API 1+
//            case TelephonyManager.NETWORK_TYPE_HSPA : // API 5+
//            case TelephonyManager.NETWORK_TYPE_HSDPA : // API 5+
//            case TelephonyManager.NETWORK_TYPE_HSUPA : // API 5+
//            case TelephonyManager.NETWORK_TYPE_HSPAP : // API 5+
//                if (-5 <= asu && asu <= 91) {
//                    return asu - 116;
//                } else if (asu == 255) {
//                    return 0;
//                }
//                break;

            // 4G -- LTE network
//            case TelephonyManager.NETWORK_TYPE_LTE : // API 11+
//                if (0 <= asu && asu <= 97) {
//                    return asu - 141;
//                }
//                break;
        }

        return 0;
    }

    public ArrayList<GSMInfo> getGSMInfoArray() {

        ArrayList<GSMInfo> gsmInfoArray = new ArrayList<GSMInfo>(10);

        long timeStamp = System.currentTimeMillis();
        int mcc = -1;
        int mnc = -1;

        boolean isPhoneTypeGSM = mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM;
        String networkOperator = mTelephonyManager.getNetworkOperator();

        // getNetworkOperator() will return an empty string if a SIM card is not inserted
        if (networkOperator != null && networkOperator.length() > 0 && isPhoneTypeGSM) {
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
        }

        // TODO: what is without SIM-card?
        if (isPhoneTypeGSM) {

            GsmCellLocation gsmCellLocation;
            try {
                gsmCellLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
            } catch (ClassCastException e) {
                gsmCellLocation = null;
            }

            if (gsmCellLocation != null) {

                gsmInfoArray.add(new GSMInfo(timeStamp, true, mcc, mnc,
                        gsmCellLocation.getCid(), gsmCellLocation.getLac(), signalStrength));
            }
        }

        if (gsmInfoArray.size() == 0) {
            gsmInfoArray.add(new GSMInfo(timeStamp));
        }

        // TODO: what is without SIM-card?
        List<NeighboringCellInfo> neighbors = mTelephonyManager.getNeighboringCellInfo();

        for (NeighboringCellInfo neighbor : neighbors) {

            int nbNetworkType = neighbor.getNetworkType();

            if (nbNetworkType == TelephonyManager.NETWORK_TYPE_GPRS ||
                    nbNetworkType == TelephonyManager.NETWORK_TYPE_EDGE) {

                gsmInfoArray.add(new GSMInfo(timeStamp, false, mcc, mnc,
                        neighbor.getCid(), neighbor.getLac(),
                        signalStrengthAsuToDbm(neighbor.getRssi(), nbNetworkType)));
            }
        }

        return gsmInfoArray;
    }


    public class GSMInfo {
        private long timeStamp;
        private boolean active;
        private int mcc;
        private int mnc;
        private int lac;
        private int cid;
        private int rssi;

        public GSMInfo(long timeStamp) {
            this.timeStamp = timeStamp;
            this.active = true;
            this.mcc = -1;
            this.mnc = -1;
            this.lac = -1;
            this.cid = -1;
            this.rssi = -1;
        }

        public GSMInfo(long timeStamp, boolean active,
                       int mcc, int mnc, int lac, int cid, int rssi) {
            this.timeStamp = timeStamp;
            this.active = active;
            this.mcc = mcc;
            this.mnc = mnc;
            this.lac = lac;
            this.cid = cid;
            this.rssi = rssi;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public boolean isActive() {
            return active;
        }

        public int getMcc() {
            return mcc;
        }

        public int getMnc() {
            return mnc;
        }

        public int getLac() {
            return lac;
        }

        public int getCid() {
            return cid;
        }

        public int getRssi() {
            return rssi;
        }
    }
}
