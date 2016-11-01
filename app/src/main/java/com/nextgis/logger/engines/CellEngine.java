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

package com.nextgis.logger.engines;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;

import com.nextgis.logger.LoggerApplication;
import com.nextgis.logger.R;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.UiUtil;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.nextgis.logger.util.LoggerConstants.CSV_SEPARATOR;

public class CellEngine extends BaseEngine {
	private final static String LTE_SIGNAL_STRENGTH = "getLteSignalStrength";

    private final static int LOW_BOUND = 0;
    private final static int MAX_MCC_MNC = 999;
    private final static int MAX_2G_LAC_CID_4G_TAC = 65535;
    private final static int MAX_3G_CID_4G_CI = 268435455;
    private final static int MAX_PSC = 511;
    private final static int MAX_PCI = 503;
	private final static int SIGNAL_STRENGTH_NONE = 0;

	private final TelephonyManager mTelephonyManager;
	private GSMPhoneStateListener mSignalListener;
	private int mSignalStrength = SIGNAL_STRENGTH_NONE;

	public CellEngine(Context context) {
		super(context);
		mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		mSignalListener = new GSMPhoneStateListener();
        mUri = mUri.buildUpon().appendPath(LoggerApplication.TABLE_CELL).build();
	}

    @Override
	public boolean onResume() {
        if (super.onResume()) {
			if (!UiUtil.isPermissionGranted(mContext, Manifest.permission.READ_PHONE_STATE))
				return false;

            int listen = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_CELL_LOCATION
                    | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SERVICE_STATE;
            mTelephonyManager.listen(mSignalListener, listen);
        }

        return false;
    }

    @Override
    public boolean isEngineEnabled() {
        return true;
    }

    @Override
	protected void loadEngine() {

	}

    @Override
	public boolean onPause() {
        if (super.onPause())
		    mTelephonyManager.listen(mSignalListener, PhoneStateListener.LISTEN_NONE);

        return false;
    }

	@Override
	public void saveData(String markId) {
		saveData(new ArrayList<>(getData()), markId);
	}

	@Override
	public void saveData(ArrayList<InfoItem> items, String markId) {
		NGWVectorLayer cellLayer = (NGWVectorLayer) MapBase.getInstance().getLayerByName(LoggerApplication.TABLE_CELL);
		if (cellLayer != null) {
			ContentValues cv = new ContentValues();
			for (InfoItem item : items) {
				cv.clear();
				cv.put(LoggerApplication.FIELD_MARK, markId);
				cv.put(LoggerConstants.HEADER_GEN, (String) item.getColumn(LoggerConstants.HEADER_GEN).getValue());
				cv.put(LoggerConstants.HEADER_TYPE, (String) item.getColumn(LoggerConstants.HEADER_TYPE).getValue());
				cv.put(LoggerConstants.HEADER_ACTIVE, (String) item.getColumn(LoggerConstants.HEADER_ACTIVE).getValue());
				cv.put(LoggerConstants.HEADER_MCC, (String) item.getColumn(LoggerConstants.HEADER_MCC).getValue());
				cv.put(LoggerConstants.HEADER_MNC, (String) item.getColumn(LoggerConstants.HEADER_MNC).getValue());
				cv.put(LoggerConstants.HEADER_LAC, (String) item.getColumn(LoggerConstants.HEADER_LAC).getValue());
				cv.put(LoggerConstants.HEADER_CID, (String) item.getColumn(LoggerConstants.HEADER_CID).getValue());
				cv.put(LoggerConstants.HEADER_PSC, (String) item.getColumn(LoggerConstants.HEADER_PSC).getValue());
				cv.put(LoggerConstants.HEADER_POWER, (String) item.getColumn(LoggerConstants.HEADER_POWER).getValue());
				try {
					cv.put(Constants.FIELD_GEOM, new GeoPoint(0, 0).toBlob());
				} catch (IOException e) {
					e.printStackTrace();
				}

				cellLayer.insert(mUri, cv);
			}
		}
	}

	private void setSignalStrength(int signalStrength) {
        mSignalStrength = signalStrength;
    }

	// http://stackoverflow.com/a/31696744/2088273
	private int getSignalStrengthLTE(SignalStrength signalStrength)
	{
		try {
			Method[] methods = android.telephony.SignalStrength.class.getMethods();

			for (Method mthd : methods)
				if (mthd.getName().equals(LTE_SIGNAL_STRENGTH))
					return (Integer) mthd.invoke(signalStrength);
		} catch (Exception ignored) { }

		return 0;
	}

	private int signalStrengthAsuToDbm(int asu, int networkType) {
		switch (networkType) {
			// 2G -- GSM network
			case TelephonyManager.NETWORK_TYPE_GPRS: // API 1+
			case TelephonyManager.NETWORK_TYPE_EDGE: // API 1+
				// getRssi() returns ASU for GSM
				if (0 <= asu && asu <= 31)
					return 2 * asu - 113;
                break;
			//	3G -- 3GSM network
			case TelephonyManager.NETWORK_TYPE_UMTS: // API 1+
			case TelephonyManager.NETWORK_TYPE_HSPA: // API 5+
			case TelephonyManager.NETWORK_TYPE_HSDPA: // API 5+
			case TelephonyManager.NETWORK_TYPE_HSUPA: // API 5+
			case TelephonyManager.NETWORK_TYPE_HSPAP: // API 5+
                // on some devices getRssi() returns RSCP for UMTS
                // on others ASU
                if (-5 <= asu && asu <= 91) {
                    return asu - 116;
                } else if (asu < -5) {
                    return asu;
                }
                break;
			//	4G -- LTE network
			case TelephonyManager.NETWORK_TYPE_LTE : // API 11+
				// getRssi() returns RSRP for LTE
				if (0 <= asu && asu <= 97)
					return asu - 141;
		}

		return 0;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private synchronized void updateItems() {
		if (mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_UNKNOWN)
			return;

        ArrayList<InfoItemGSM> temp = new ArrayList<>();

		int osVersion = android.os.Build.VERSION.SDK_INT;
		int api17 = android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
		int api18 = android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;

		boolean useAPI17 = getPreferences().getBoolean(LoggerConstants.PREF_USE_API17, false); // WCDMA uses API 18+, now min is 18
		boolean isRegistered = false;
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

						isRegistered |= gsm.isRegistered();
						temp.add(new InfoItemGSM(gsm.isRegistered(), nwType, gsmIdentity.getMcc(), gsmIdentity.getMnc(), gsmIdentity.getLac(),
								gsmIdentity.getCid(), LoggerConstants.UNDEFINED, gsm.getCellSignalStrength().getDbm()));

						// 3G - WCDMA cell towers, its API 18+
					} else if (osVersion >= api18 && cell.getClass() == CellInfoWcdma.class) {
						CellInfoWcdma wcdma = (CellInfoWcdma) cell;
						CellIdentityWcdma wcdmaIdentity = wcdma.getCellIdentity();

						isRegistered |= wcdma.isRegistered();
						temp.add(new InfoItemGSM(wcdma.isRegistered(), nwType, wcdmaIdentity.getMcc(), wcdmaIdentity.getMnc(),
								wcdmaIdentity.getLac(), wcdmaIdentity.getCid(), wcdmaIdentity.getPsc(), wcdma.getCellSignalStrength().getDbm()));

						// 4G - LTE cell towers
					} else if (cell.getClass() == CellInfoLte.class) {
						CellInfoLte lte = (CellInfoLte) cell;
						CellIdentityLte lteIdentity = lte.getCellIdentity();

						isRegistered |= lte.isRegistered();
						temp.add(new InfoItemGSM(lte.isRegistered(), nwType, lteIdentity.getMcc(), lteIdentity.getMnc(), lteIdentity.getTac(),
								lteIdentity.getPci(), lteIdentity.getCi(), lte.getCellSignalStrength().getDbm()));
					}
				}
		}

		if (temp.size() == 0) { // in case API 17/18 didn't return anything
			// #1 using default way to obtain cell towers info
			int mcc = LoggerConstants.UNDEFINED;
			int mnc = LoggerConstants.UNDEFINED;

			//			boolean isNetworkTypeGSM = isGSMNetwork(mTelephonyManager.getNetworkType());
			boolean isPhoneTypeGSM = mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM;
			String networkOperator = mTelephonyManager.getNetworkOperator();

			// getNetworkOperator() will return an empty string if a SIM card is not inserted
			// or if user's sim card is not registered at any operator's cell
			if (!TextUtils.isEmpty(networkOperator) && isPhoneTypeGSM) {
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
					isRegistered = true;
					temp.add(new InfoItemGSM(true, mTelephonyManager.getNetworkType(), mcc, mnc, gsmCellLocation.getLac(),
							gsmCellLocation.getCid(), gsmCellLocation.getPsc(), mSignalStrength));
				}
			}

			@SuppressWarnings("deprecation") List<NeighboringCellInfo> neighbors = mTelephonyManager.getNeighboringCellInfo();
			for (NeighboringCellInfo neighbor : neighbors) {
				int nbNetworkType = neighbor.getNetworkType();

				temp.add(new InfoItemGSM(false, nbNetworkType, LoggerConstants.UNDEFINED, LoggerConstants.UNDEFINED, neighbor.getLac(), neighbor.getCid(),
                        neighbor.getPsc(), signalStrengthAsuToDbm(neighbor.getRssi(), nbNetworkType)));
			}
		}

		if (temp.size() == 0 || !isRegistered) { // add default record if there is no items in array /-1
			temp.add(new InfoItemGSM());
		}

        mItems.clear();
        mItems.addAll(temp);
    }

    public String getNetworkOperator() {
        return mTelephonyManager.getNetworkOperatorName();
    }

    public int getNetworkMNC() {
        String operator = mTelephonyManager.getNetworkOperator();

        return TextUtils.isEmpty(operator) ? -1 : Integer.parseInt(operator.substring(3));
    }

	public boolean isRoaming() {
		return mTelephonyManager.isNetworkRoaming();
	}

    //	public static boolean isGSMNetwork(int network) {
	//		return network == TelephonyManager.NETWORK_TYPE_EDGE || network == TelephonyManager.NETWORK_TYPE_GPRS;
	//	}

	private static String getNetworkGen(int type) {
		String gen;

		switch (type) {
		case TelephonyManager.NETWORK_TYPE_EDGE:
		case TelephonyManager.NETWORK_TYPE_GPRS:
			gen = LoggerConstants.GEN_2G;
			break;
		case TelephonyManager.NETWORK_TYPE_UMTS:
		case TelephonyManager.NETWORK_TYPE_HSPA:
		case TelephonyManager.NETWORK_TYPE_HSDPA:
		case TelephonyManager.NETWORK_TYPE_HSUPA:
		case TelephonyManager.NETWORK_TYPE_HSPAP:
			gen = LoggerConstants.GEN_3G;
			break;
		case TelephonyManager.NETWORK_TYPE_LTE:
			gen = LoggerConstants.GEN_4G;
			break;
		default:
			gen = LoggerConstants.UNKNOWN;
			break;
		}

		return gen;
	}

	private static String getNetworkType(int type) {
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
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
		default:
			network = LoggerConstants.UNKNOWN;
			break;
		}

		return network;
	}

    /**
     * Sorts list by cells. Active cells arranged first.
     *
     * @param array    A List of GSMInfo to sort
     */
    public static void sortByActive(List<InfoItem> array) {
        Collections.sort(array, new Comparator<InfoItem>() {
            @Override
            public int compare(InfoItem lhs, InfoItem rhs) {
                boolean b1 = lhs.getColumn(LoggerConstants.HEADER_ACTIVE).getValue().equals("1");
                boolean b2 = rhs.getColumn(LoggerConstants.HEADER_ACTIVE).getValue().equals("1");

                if (b1 && !b2) {
                    return -1;
                }

                if (!b1 && b2) {
                    return +1;
                }

                return 0;
            }
        });
    }

	public static String getHeader() {
		return LoggerConstants.HEADER_GEN + CSV_SEPARATOR + LoggerConstants.HEADER_TYPE + CSV_SEPARATOR + LoggerConstants.HEADER_ACTIVE + CSV_SEPARATOR +
				LoggerConstants.HEADER_MCC + CSV_SEPARATOR + LoggerConstants.HEADER_MNC + CSV_SEPARATOR + LoggerConstants.HEADER_LAC + "/" +
				LoggerConstants.HEADER_TAC + CSV_SEPARATOR + LoggerConstants.HEADER_CID + "/" + LoggerConstants.HEADER_PCI + CSV_SEPARATOR +
				LoggerConstants.HEADER_PSC + "/" + LoggerConstants.HEADER_CI + CSV_SEPARATOR + LoggerConstants.HEADER_RSSI + "/" + LoggerConstants.HEADER_RSCP;
	}

	public static String getDataFromCursor(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_GEN)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_TYPE)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_ACTIVE)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_MCC)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_MNC)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_LAC)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_CID)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_PSC)) + CSV_SEPARATOR +
				cursor.getString(cursor.getColumnIndex(LoggerConstants.HEADER_POWER));
	}

    private class GSMPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signal) {
            super.onSignalStrengthsChanged(signal);

			int signalStrength = signal.getGsmSignalStrength();
			if (mTelephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE && 0 <= signalStrength && signalStrength <= 97)
				signalStrength = getSignalStrengthLTE(signal);

            setSignalStrength(signalStrengthAsuToDbm(signalStrength, mTelephonyManager.getNetworkType()));
            updateItems();
            notifyListeners();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);
            updateItems();
            notifyListeners();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            super.onDataConnectionStateChanged(state, networkType);
            updateItems();
            notifyListeners();
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            updateItems();
            notifyListeners();
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            super.onCellInfoChanged(cellInfo);
            updateItems();
            notifyListeners();
        }
    }

	private void notifyListeners() {
		notifyListeners("CELL");
	}

    private class InfoItemGSM extends InfoItem {
		private InfoItemGSM() {
			super("Cell Info");
            addColumn(LoggerConstants.HEADER_GEN, null, null);
            addColumn(LoggerConstants.HEADER_TYPE, null, null);
            setValue(LoggerConstants.HEADER_GEN, getNetworkGen(TelephonyManager.NETWORK_TYPE_UNKNOWN));
            setValue(LoggerConstants.HEADER_TYPE, getNetworkType(TelephonyManager.NETWORK_TYPE_UNKNOWN));

            addColumn(LoggerConstants.HEADER_ACTIVE, null, null, 1);
            addColumn(LoggerConstants.HEADER_MCC, null, null, LoggerConstants.UNDEFINED);
            addColumn(LoggerConstants.HEADER_MNC, null, null, LoggerConstants.UNDEFINED);
            addColumn(LoggerConstants.HEADER_LAC, null, null, LoggerConstants.UNDEFINED);
            addColumn(LoggerConstants.HEADER_CID, null, null, LoggerConstants.UNDEFINED);
            addColumn(LoggerConstants.HEADER_PSC, null, null, LoggerConstants.UNDEFINED);
            addColumn(LoggerConstants.HEADER_POWER, null, mContext.getString(R.string.info_dbm), SIGNAL_STRENGTH_NONE);
		}

		InfoItemGSM(boolean active, int networkType, int mcc, int mnc, int lac, int cid, int psc, int power) {
            this();
            setValue(LoggerConstants.HEADER_ACTIVE, active ? 1 : 0);
            setValue(LoggerConstants.HEADER_GEN, getNetworkGen(networkType));
            setValue(LoggerConstants.HEADER_TYPE, getNetworkType(networkType));

            boolean outOfBounds = mcc <= LOW_BOUND || mcc >= MAX_MCC_MNC;
            setValue(LoggerConstants.HEADER_MCC, outOfBounds ? LoggerConstants.UNDEFINED : mcc);

            outOfBounds = mnc <= LOW_BOUND || mnc >= MAX_MCC_MNC;
            setValue(LoggerConstants.HEADER_MNC, outOfBounds ? LoggerConstants.UNDEFINED : mnc);

            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    if (lac < LOW_BOUND || lac > MAX_2G_LAC_CID_4G_TAC)
                        lac = -1;

                    if (cid < LOW_BOUND || cid > MAX_2G_LAC_CID_4G_TAC)
                        cid = -1;
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
				default:
					if (lac < LOW_BOUND || lac > MAX_2G_LAC_CID_4G_TAC)
                        lac = -1;

					if (cid < LOW_BOUND || cid > MAX_3G_CID_4G_CI)
                        cid = -1;

					if (psc < LOW_BOUND || psc > MAX_PSC)
                        psc = -1;
                    break;
				case TelephonyManager.NETWORK_TYPE_LTE:
                    if (lac < LOW_BOUND || lac > MAX_2G_LAC_CID_4G_TAC)
                        lac = -1;

					if (cid < LOW_BOUND || cid > MAX_PCI)
						cid = -1;

					if (psc < LOW_BOUND || psc > MAX_3G_CID_4G_CI)
						psc = -1;
					break;
            }

            setValue(LoggerConstants.HEADER_LAC, lac);
            setValue(LoggerConstants.HEADER_CID, cid);
            setValue(LoggerConstants.HEADER_PSC, psc);
            setValue(LoggerConstants.HEADER_POWER, power);
		}

        private void setValue(String key, int value) {
            setValue(key, value + "");
        }

        private void addColumn(String shortName, String fullName, String unit, int data) {
            addColumn(shortName, fullName, unit);
            setValue(shortName, data);
        }
	}
}
