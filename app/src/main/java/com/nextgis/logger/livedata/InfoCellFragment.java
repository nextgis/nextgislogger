/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015-2017 NextGIS
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

package com.nextgis.logger.livedata;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.nextgis.logger.R;
import com.nextgis.logger.engines.BaseEngine;
import com.nextgis.logger.engines.CellEngine;
import com.nextgis.logger.engines.InfoItem;
import com.nextgis.logger.util.LoggerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoCellFragment extends InfoFragment {
    private static final String CELL_ID         = "id";
    private static final String CELL_ACTIVE     = "active";
    private static final String CELL_GEN        = "generation";
    private static final String CELL_TYPE       = "type";
    private static final String CELL_MCC        = "mcc";
    private static final String CELL_MNC        = "mnc";
    private static final String CELL_LAC        = "lac";
    private static final String CELL_CID        = "cid";
    private static final String CELL_PSC        = "psc";
    private static final String CELL_POWER      = "power";

    private ListView mLvNeighbours;
    private TextView mTvNeighbours, mTvActive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_info_cell, container, false);

        mLvNeighbours = (ListView) rootView.findViewById(R.id.lv_neighbours);
        mTvNeighbours = (TextView) rootView.findViewById(R.id.tv_network_neighbours);
        mTvActive = (TextView) rootView.findViewById(R.id.tv_network_active);

        mListener = new BaseEngine.EngineListener() {
            @Override
            public void onInfoChanged(String source) {
                if (isAdded())
                    fillTextViews();
            }
        };

        return rootView;
    }

    private void fillTextViews() {
        ArrayList<InfoItem> infoItemGSMArray = getData();
        ArrayList<Map<String, Object>> neighboursData = new ArrayList<>();
        Map<String, Object> itemData;
        final String na = getString(R.string.info_na);

        CellEngine.sortByActive(infoItemGSMArray);

        int id = 1;
        String gen, type, mnc, mcc, lac, cid, psc, power;
        for (InfoItem gsmItem : infoItemGSMArray) {
            String data = (String) gsmItem.getColumn(LoggerConstants.HEADER_GEN).getValue();
            gen = data.equals(LoggerConstants.UNKNOWN) ? na : data;
            data = (String) gsmItem.getColumn(LoggerConstants.HEADER_TYPE).getValue();
            type = data.equals(LoggerConstants.UNKNOWN) ? na : data;
            mcc = replaceNegativeValue(gsmItem, LoggerConstants.HEADER_MCC, na);
            mnc = replaceNegativeValue(gsmItem, LoggerConstants.HEADER_MNC, na);
            lac = replaceNegativeValue(gsmItem, LoggerConstants.HEADER_LAC, na);
            cid = replaceNegativeValue(gsmItem, LoggerConstants.HEADER_CID, na);
            psc = replaceNegativeValue(gsmItem, LoggerConstants.HEADER_PSC, na);
            power = gsmItem.getColumn(LoggerConstants.HEADER_POWER).getValueWithUnit();

            itemData = new HashMap<>();

            boolean isActive = gsmItem.getColumn(LoggerConstants.HEADER_ACTIVE).getValue().equals("1");
            if (!isActive)
                itemData.put(CELL_ID, String.format("%s)", id++));

            itemData.put(CELL_ACTIVE, isActive);
            itemData.put(CELL_GEN, gen);
            itemData.put(CELL_TYPE, type);
            itemData.put(CELL_MCC, mcc);
            itemData.put(CELL_MNC, mnc);
            itemData.put(CELL_LAC, lac);
            itemData.put(CELL_CID, cid);
            itemData.put(CELL_PSC, psc);
            itemData.put(CELL_POWER, power);
            neighboursData.add(itemData);
        }

        String[] from = { CELL_ID, CELL_GEN, CELL_TYPE, CELL_MCC, CELL_MNC, CELL_LAC, CELL_CID, CELL_PSC, CELL_POWER };
        int[] to = {R.id.tv_id, R.id.tv_network_gen, R.id.tv_network_type, R.id.tv_network_mcc, R.id.tv_network_mnc,
                R.id.tv_network_lac, R.id.tv_network_cid, R.id.tv_network_psc, R.id.tv_network_power};
        CellsAdapter saNeighbours = new CellsAdapter(getActivity(), neighboursData, R.layout.info_cell_neighbour_row, from, to);

        mLvNeighbours.setAdapter(saNeighbours);
        mTvNeighbours.setText(id - 1 + "");
        mTvActive.setText(infoItemGSMArray.size() - id + 1 + "");
    }

    private String replaceNegativeValue(InfoItem item, String column, String placeholder) {
        String data = (String) item.getColumn(column).getValue();
        return data.equals("-1") ? placeholder : data;
    }

    @Override
    public void onResume() {
        super.onResume();
        fillTextViews();
    }

    private class CellsAdapter extends SimpleAdapter {
        LayoutInflater mInflater;

        private TextView tvGen, tvType, tvOperator, tvMNC, tvMCC, tvLAC, tvCID, tvPSC, tvPower;

        /**
         * Constructor
         *
         * @param context  The context where the View associated with this SimpleAdapter is running
         * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
         *                 Maps contain the data for each row, and should include all the entries specified in
         *                 "from"
         * @param resource Resource identifier of a view layout that defines the views for this list
         *                 item. The layout file should include at least those named views defined in "to"
         * @param from     A list of column names that will be added to the Map associated with each
         *                 item.
         * @param to       The views that should display column in the "from" parameter. These should all be
         *                 TextViews. The first N views in this list are given the values of the first N columns
         */
        CellsAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result;
            Map<String, ?> item = (Map<String, ?>) getItem(position);
            boolean isActive = (Boolean) item.get(CELL_ACTIVE);

            if (!isActive)
                result = super.getView(position, null, parent);
            else {
                boolean roaming = false;
                String operator = LoggerConstants.UNKNOWN;

                if (isConnected()) {
                    CellEngine cellEngine = (CellEngine) mEngine;
                    roaming = cellEngine.isRoaming();
                    boolean sameOperator = item.get(CELL_MNC).equals(cellEngine.getNetworkMNC() + "");
                    if (sameOperator)
                        operator = cellEngine.getNetworkOperator();
                }

                View v = mInflater.inflate(R.layout.info_cell_active_row, parent, false);
                TextView tvRoaming = (TextView) v.findViewById(R.id.tv_network_roaming);
                if (roaming)
                    tvRoaming.setVisibility(View.VISIBLE);

                tvGen = (TextView) v.findViewById(R.id.tv_network_gen);
                tvType = (TextView) v.findViewById(R.id.tv_network_type);
                tvOperator = (TextView) v.findViewById(R.id.tv_network_operator);
                tvMCC = (TextView) v.findViewById(R.id.tv_network_mcc);
                tvMNC = (TextView) v.findViewById(R.id.tv_network_mnc);
                tvLAC = (TextView) v.findViewById(R.id.tv_network_lac);
                tvCID = (TextView) v.findViewById(R.id.tv_network_cid);
                tvPSC = (TextView) v.findViewById(R.id.tv_network_psc);
                tvPower = (TextView) v.findViewById(R.id.tv_network_power);


                tvOperator.setText(operator);
                tvGen.setText((String) item.get(CELL_GEN));
                tvType.setText((String) item.get(CELL_TYPE));
                tvMCC.setText((String) item.get(CELL_MCC));
                tvMNC.setText((String) item.get(CELL_MNC));
                tvLAC.setText((String) item.get(CELL_LAC));
                tvCID.setText((String) item.get(CELL_CID));
                tvPSC.setText((String) item.get(CELL_PSC));
                tvPower.setText((String) item.get(CELL_POWER));

                switch (item.get(CELL_GEN).toString()) {
                    case LoggerConstants.GEN_2G:
                        v.findViewById(R.id.ll_psc).setVisibility(View.GONE);
                        ((TextView) v.findViewById(R.id.tv_network_lac_title)).setText(R.string.info_lac);
                        ((TextView) v.findViewById(R.id.tv_network_cid_title)).setText(R.string.info_cid);
                        break;
                    case LoggerConstants.GEN_4G:
                        v.findViewById(R.id.ll_psc).setVisibility(View.VISIBLE);
                        ((TextView) v.findViewById(R.id.tv_network_lac_title)).setText(R.string.info_tac);
                        ((TextView) v.findViewById(R.id.tv_network_cid_title)).setText(R.string.info_pci);
                        ((TextView) v.findViewById(R.id.tv_network_psc_title)).setText(R.string.info_ci);
                        break;
                    default:
                        v.findViewById(R.id.ll_psc).setVisibility(View.VISIBLE);
                        ((TextView) v.findViewById(R.id.tv_network_lac_title)).setText(R.string.info_lac);
                        ((TextView) v.findViewById(R.id.tv_network_cid_title)).setText(R.string.info_cid);
                        ((TextView) v.findViewById(R.id.tv_network_psc_title)).setText(R.string.info_psc);
                        break;
                }

                result = v;
            }

            TextView psc = (TextView) result.findViewById(R.id.tv_network_psc);
            TextView psc_title = (TextView) result.findViewById(R.id.tv_network_psc_title);

            switch (item.get(CELL_GEN).toString()) {
                case LoggerConstants.GEN_2G:
                    if (psc != null && psc_title != null) {
                        psc.setVisibility(View.INVISIBLE);
                        psc_title.setVisibility(View.INVISIBLE);
                        ((TextView) result.findViewById(R.id.tv_network_lac_title)).setText(R.string.info_lac);
                        ((TextView) result.findViewById(R.id.tv_network_cid_title)).setText(R.string.info_cid);
                    }
                    break;
                case LoggerConstants.GEN_4G:
                    if (psc != null && psc_title != null) {
                        psc.setVisibility(View.VISIBLE);
                        psc_title.setText(R.string.info_ci);
                        psc_title.setVisibility(View.VISIBLE);
                        ((TextView) result.findViewById(R.id.tv_network_lac_title)).setText(R.string.info_tac);
                        ((TextView) result.findViewById(R.id.tv_network_cid_title)).setText(R.string.info_pci);
                    }
                    break;
                default:
                    if (psc != null && psc_title != null) {
                        psc.setVisibility(View.VISIBLE);
                        psc_title.setText(R.string.info_psc);
                        psc_title.setVisibility(View.VISIBLE);
                        ((TextView) result.findViewById(R.id.tv_network_lac_title)).setText(R.string.info_lac);
                        ((TextView) result.findViewById(R.id.tv_network_cid_title)).setText(R.string.info_cid);
                    }
                    break;
            }

            return result;
        }
    }
}
