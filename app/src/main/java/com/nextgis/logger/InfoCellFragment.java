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

package com.nextgis.logger;

import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InfoCellFragment extends Fragment implements CellEngine.CellInfoListener, View.OnClickListener {
    private static final String CELL_GEN        = "generation";
    private static final String CELL_TYPE       = "type";
    private static final String CELL_MCC        = "mcc";
    private static final String CELL_MNC        = "mnc";
    private static final String CELL_LAC        = "lac";
    private static final String CELL_CID        = "cid";
    private static final String CELL_PSC        = "psc";
    private static final String CELL_POWER      = "power";

    private CellEngine gsmEngine;

    private TextView tvGen, tvType, tvOperator, tvMNC, tvMCC, tvLAC, tvCID, tvPSC, tvPower, tvNeighbours;
    private ListView lvNeighbours;
    private LinearLayout llActiveCell;

    @Override
    public void onPause() {
        super.onPause();

        gsmEngine.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        gsmEngine.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.info_cell_fragment, container, false);

        llActiveCell = (LinearLayout) rootView.findViewById(R.id.ll_network_active_cell);

        tvGen = (TextView) rootView.findViewById(R.id.tv_network_gen);
        tvType = (TextView) rootView.findViewById(R.id.tv_network_type);
        tvOperator = (TextView) rootView.findViewById(R.id.tv_network_operator);
        tvMCC = (TextView) rootView.findViewById(R.id.tv_network_mcc);
        tvMNC = (TextView) rootView.findViewById(R.id.tv_network_mnc);
        tvLAC = (TextView) rootView.findViewById(R.id.tv_network_lac);
        tvCID = (TextView) rootView.findViewById(R.id.tv_network_cid);
        tvPSC = (TextView) rootView.findViewById(R.id.tv_network_psc);
        tvPower = (TextView) rootView.findViewById(R.id.tv_network_power);
        tvNeighbours = (TextView) rootView.findViewById(R.id.tv_network_neighbours);
        lvNeighbours = (ListView) rootView.findViewById(R.id.lv_neighbours);

        tvNeighbours.setOnClickListener(this);
        tvNeighbours.setPaintFlags(tvNeighbours.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        gsmEngine = new CellEngine(getActivity());
        gsmEngine.addCellListener(this);

        fillTextViews();

        return rootView;
    }

    private void fillTextViews() {
        ArrayList<CellEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();
        ArrayList<Map<String, Object>> neighboursData = new ArrayList<>();
        Map<String, Object> itemData;
        String na = getString(R.string.info_na);

        boolean isRegistered = false;
        for (CellEngine.GSMInfo gsmItem : gsmInfoArray) {
            String gen, type, mnc, mcc, lac, cid, psc, power;

            gen = gsmItem.networkGen();
            gen = gen.equals("unknown") ? na : gen;
            type = gsmItem.networkType();
            type = type.equals("unknown") ? na : type;
            mcc = "" + gsmItem.getMcc();
            mnc = "" + gsmItem.getMnc();
            lac = "" + gsmItem.getLac();
            cid = "" + gsmItem.getCid();
            psc = "" + gsmItem.getPsc();
            power = "" + gsmItem.getRssi() + " dBm";

            if (gsmItem.isActive()) {
                isRegistered = true;
                tvOperator.setText(gsmEngine.getNetworkOperator());
                tvGen.setText(gen);
                tvType.setText(type);
                tvMCC.setText(mcc);
                tvMNC.setText(mnc);
                tvLAC.setText(lac);
                tvCID.setText(cid);
                tvPSC.setText(psc);
                tvPower.setText(power);
            } else {
                itemData = new HashMap<>();
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
        }

        if (!isRegistered) {
            tvOperator.setText(na);
            tvGen.setText(na);
            tvType.setText(na);
            tvMCC.setText(na);
            tvMNC.setText(na);
            tvLAC.setText(na);
            tvCID.setText(na);
            tvPSC.setText(na);
            tvPower.setText(na);
        }

        int size = gsmInfoArray.size();
        size = isRegistered ? size - 1 : size;
        tvNeighbours.setText(size + getNeighboursString());
        tvNeighbours.setTag("" + size);

        String[] from = { CELL_GEN, CELL_TYPE, CELL_MCC, CELL_MNC, CELL_LAC, CELL_CID, CELL_PSC, CELL_POWER };
        int[] to = {R.id.tv_network_gen, R.id.tv_network_type, R.id.tv_network_mcc, R.id.tv_network_mnc,
                R.id.tv_network_lac, R.id.tv_network_cid, R.id.tv_network_psc, R.id.tv_network_power};
        SimpleAdapter saNeighbours = new SimpleAdapter(getActivity(), neighboursData, R.layout.info_cell_neighbour_row, from, to);

        lvNeighbours.setAdapter(saNeighbours);
    }

    @Override
    public void onCellInfoChanged() {
        fillTextViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_network_neighbours:
                int active, text;

//                if (lvNeighbours.getVisibility() == View.INVISIBLE) {
                if (llActiveCell.getVisibility() == View.VISIBLE) {
                    if (lvNeighbours.getCount() == 0)
                        return;

                    active = View.GONE;
//                    neighbours = View.VISIBLE;
                    text = R.string.info_collapse;
                } else {
                    active = View.VISIBLE;
//                    neighbours = View.INVISIBLE;
                    text = R.string.info_expand;
                }

                ((TextView) v).setText(text);
                llActiveCell.setVisibility(active);
//                lvNeighbours.setVisibility(neighbours);
                tvNeighbours.setText(tvNeighbours.getTag() + getNeighboursString());
                break;
        }
    }

    private String getNeighboursString() {
        String hint = llActiveCell.getVisibility() == View.VISIBLE ? getString(R.string.info_expand) : getString(R.string.info_collapse);
        return " (" + hint + ")";
    }
}
