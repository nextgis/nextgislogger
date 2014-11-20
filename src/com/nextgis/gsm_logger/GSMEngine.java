package com.nextgis.gsm_logger;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
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

		/*
		 * Get the Signal strength from the provider, each time there is an
		 * update
		 */
		@Override
		public void onSignalStrengthsChanged(SignalStrength signal) {
			super.onSignalStrengthsChanged(signal);
			setSignalStrength(signal.isGsm() ? signalStrengthAsuToDbm(signal.getGsmSignalStrength(), TelephonyManager.NETWORK_TYPE_GPRS) : SIGNAL_STRENGTH_NONE);
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
		case TelephonyManager.NETWORK_TYPE_GPRS: // API 1+
		case TelephonyManager.NETWORK_TYPE_EDGE: // API 1+
			if (0 <= asu && asu <= 31) {
				return 2 * asu - 113;
			} else if (asu == 99) {
				return 0;
			}
			break;

		//	3G -- UMTS network
		case TelephonyManager.NETWORK_TYPE_UMTS: // API 1+
		case TelephonyManager.NETWORK_TYPE_HSPA: // API 5+
		case TelephonyManager.NETWORK_TYPE_HSDPA: // API 5+
		case TelephonyManager.NETWORK_TYPE_HSUPA: // API 5+
		case TelephonyManager.NETWORK_TYPE_HSPAP: // API 5+
			if (-5 <= asu && asu <= 91) {
				return asu - 116;
			} else if (asu == 255) {
				return 0;
			}
			break;

		// 4G -- LTE network
		//            case TelephonyManager.NETWORK_TYPE_LTE : // API 11+
		//                if (0 <= asu && asu <= 97) {
		//                    return asu - 141;
		//                }
		//                break;
		}

		return 0;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public ArrayList<GSMInfo> getGSMInfoArray() {
		int osVersion = android.os.Build.VERSION.SDK_INT;
		int api17 = android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
		int api18 = android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;

		ArrayList<GSMInfo> gsmInfoArray = new ArrayList<GSMInfo>();

		long timeStamp = System.currentTimeMillis();
		boolean useAPI17 = mContext.getSharedPreferences(MainActivity.PREFERENCE_NAME, MainActivity.MODE_PRIVATE)
				.getBoolean(MainActivity.PREF_USE_API17, false); // WCDMA uses API 18+, now min is 18

		// #2 using API 17 to get all cell towers around, including one which phone registered to
		if (osVersion >= api17 && useAPI17) {
			List<CellInfo> allCells = mTelephonyManager.getAllCellInfo();
			int nwType = mTelephonyManager.getNetworkType();

			if (allCells != null) // Samsung and Nexus return null
				for (CellInfo cell : allCells) {
					// 2G - GSM cell towers only
					if (cell.getClass() == CellInfoGsm.class) {
						CellInfoGsm gsm = (CellInfoGsm) cell;
						CellIdentityGsm gsmIdentity = gsm.getCellIdentity();

						gsmInfoArray.add(new GSMInfo(timeStamp, gsm.isRegistered(), nwType, gsmIdentity.getMcc(), gsmIdentity.getMnc(), gsmIdentity.getLac(),
								gsmIdentity.getCid(), -1, gsm.getCellSignalStrength().getDbm()));

						// 3G - WCDMA cell towers, its API 18+
					} else if (osVersion >= api18 && cell.getClass() == CellInfoWcdma.class) {
						CellInfoWcdma wcdma = (CellInfoWcdma) cell;
						CellIdentityWcdma wcdmaIdentity = wcdma.getCellIdentity();

						gsmInfoArray.add(new GSMInfo(timeStamp, wcdma.isRegistered(), nwType, wcdmaIdentity.getMcc(), wcdmaIdentity.getMnc(), wcdmaIdentity.getLac(),
								wcdmaIdentity.getCid(), wcdmaIdentity.getPsc(), wcdma.getCellSignalStrength().getDbm()));
					}
				}
		}

		if (gsmInfoArray.size() == 0) { // in case API 17/18 didn't return anything
			// #1 using default way to obtain cell towers info
			int mcc = -1;
			int mnc = -1;

			//			boolean isNetworkTypeGSM = isGSMNetwork(mTelephonyManager.getNetworkType());
			boolean isPhoneTypeGSM = mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM;
			String networkOperator = mTelephonyManager.getNetworkOperator();

			// getNetworkOperator() will return an empty string if a SIM card is not inserted
			// or if user's sim card is not registered at any operator's cell
			if (networkOperator != null && networkOperator.length() > 0 && isPhoneTypeGSM) {
				mcc = Integer.parseInt(networkOperator.substring(0, 3));
				mnc = Integer.parseInt(networkOperator.substring(3));
			}

			if (isPhoneTypeGSM) {
				GsmCellLocation gsmCellLocation;

				try {
					gsmCellLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
				} catch (ClassCastException e) {
					gsmCellLocation = null;
				}

				if (gsmCellLocation != null) {
					gsmInfoArray.add(new GSMInfo(timeStamp, true, mTelephonyManager.getNetworkType(), mcc, mnc, gsmCellLocation.getLac(), gsmCellLocation.getCid(), gsmCellLocation.getPsc(),
							signalStrength));
				}
			}

			List<NeighboringCellInfo> neighbors = mTelephonyManager.getNeighboringCellInfo();

			for (NeighboringCellInfo neighbor : neighbors) {
				int nbNetworkType = neighbor.getNetworkType();

				//				if (nbNetworkType == TelephonyManager.NETWORK_TYPE_GPRS || nbNetworkType == TelephonyManager.NETWORK_TYPE_EDGE) {
				gsmInfoArray.add(new GSMInfo(timeStamp, false, nbNetworkType, mcc, mnc, neighbor.getLac(), neighbor.getCid(), neighbor.getPsc(), signalStrengthAsuToDbm(
						neighbor.getRssi(), nbNetworkType)));
				//				}
			}
		}

		if (gsmInfoArray.size() == 0) { // add default record if there is no items in array /-1
			gsmInfoArray.add(new GSMInfo(timeStamp));
		}

		return gsmInfoArray;
	}

//	public static boolean isGSMNetwork(int network) {
//		return network == TelephonyManager.NETWORK_TYPE_EDGE || network == TelephonyManager.NETWORK_TYPE_GPRS;
//	}

	public static String getNetworkGen(int type) {
		String gen;
		
		switch (type) {
		case TelephonyManager.NETWORK_TYPE_EDGE:
		case TelephonyManager.NETWORK_TYPE_GPRS:
			gen = "2G";
			break;
		case TelephonyManager.NETWORK_TYPE_UMTS:
		case TelephonyManager.NETWORK_TYPE_HSPA:
		case TelephonyManager.NETWORK_TYPE_HSDPA:
		case TelephonyManager.NETWORK_TYPE_HSUPA:
		case TelephonyManager.NETWORK_TYPE_HSPAP:
			gen = "3G";
			break;
		case TelephonyManager.NETWORK_TYPE_LTE:
			gen = "4G";
			break;
		default:
			gen = "unknown";
			break;
		}
		
		return gen;
	}
	
	public static String getNetworkType(int type) {
		String network;

		switch (type) {
		//		case TelephonyManager.NETWORK_TYPE_CDMA:
		//			break;
		//		case TelephonyManager.NETWORK_TYPE_EVDO_0:
		//			break;
		//		case TelephonyManager.NETWORK_TYPE_IDEN:
		//			break;
		case TelephonyManager.NETWORK_TYPE_EDGE:
			network = "EDGE";
			break;
		case TelephonyManager.NETWORK_TYPE_GPRS:
			network = "GPRS";
			break;
		case TelephonyManager.NETWORK_TYPE_UMTS:
			network = "UMTS";
			break;
		case TelephonyManager.NETWORK_TYPE_HSPA:
			network = "HSPA";
			break;
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			network = "HSDPA";
			break;
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			network = "HSUPA";
			break;
		case TelephonyManager.NETWORK_TYPE_HSPAP:
			network = "HSPAP";
			break;
		case TelephonyManager.NETWORK_TYPE_LTE:
			network = "LTE";
			break;
		//		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
		//			break;
		default:
			network = "unknown";
			break;
		}

		return network;
	}

	public class GSMInfo {
		private long timeStamp;
		private boolean active;
		private int mcc;
		private int mnc;
		private int lac;
		private int cid;
		private int psc; // new - primary scrambling code for UMTS
		private int networkType; // new - primary scrambling code for UMTS
		private int rssi;

		public GSMInfo(long timeStamp) {
			this.timeStamp = timeStamp;
			this.active = true;
			this.networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
			this.mcc = -1;
			this.mnc = -1;
			this.lac = -1;
			this.cid = -1;
			this.psc = -1;
			this.rssi = -1;
		}

		public GSMInfo(long timeStamp, boolean active, int networkType, int mcc, int mnc, int lac, int cid, int psc, int rssi) {
			this.timeStamp = timeStamp;
			this.active = active;
			this.networkType = networkType;
			this.mcc = mcc;
			this.mnc = mnc;
			this.lac = lac;
			this.cid = cid;
			this.psc = psc;
			this.rssi = rssi;
		}

		public long getTimeStamp() {
			return timeStamp;
		}

		public boolean isActive() {
			return active;
		}

		public String networkType() {
			return getNetworkType(networkType);
		}
		
		public String networkGen() {
			return getNetworkGen(networkType);
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

		public int getPsc() {
			return psc;
		}

		public int getRssi() {
			return rssi;
		}
	}
}
