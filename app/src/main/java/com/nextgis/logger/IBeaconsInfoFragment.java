package com.nextgis.logger;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


public class IBeaconsInfoFragment extends Fragment implements Observer, AdapterView.OnItemClickListener {

    private static final String BEACON_ID1      = "id1";
    private static final String BEACON_ID2      = "id2";
    private static final String BEACON_ID3      = "id3";
    private static final String BEACON_BT_ADDR  = "bt_addr";
    private static final String BEACON_BT_NAME  = "bt_name";
    private static final String BEACON_TX       = "tx";
    private static final String BEACON_RSSI     = "rssi";
    private static final String BEACON_DISTANCE = "distance";


    private ListView lvBeacons;
    private BeaconEngine beaconsEngine;

    @Override
    public void onPause() {
        super.onPause();

        //beaconsEngine.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        //beaconsEngine.onResume();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_ibeacon_item_list, container, false);

        lvBeacons = (ListView) rootView.findViewById(R.id.lv_beacons);

        lvBeacons.setOnItemClickListener(this);

        beaconsEngine = new BeaconEngine(getActivity());
        beaconsEngine.addObserver(this);

        fillTextViews.run();

        return rootView;
    }


    @Override
    public void update(Observable observable, Object data) {
        if (!isAdded())
            return;
        if (observable instanceof BeaconEngine) {
            getActivity().runOnUiThread(fillTextViews);
        }
    }

    private Runnable fillTextViews = new Runnable() {
        public void run() {
            Collection<Beacon> beacons = beaconsEngine.getBeacons();

            ArrayList<Map<String, Object>> beaconsData = new ArrayList<>();
            Map<String, Object> itemData;
            String na = getString(R.string.info_na);

            for (Beacon  beacon: beacons) {
                itemData = new HashMap<>();

                itemData.put(BEACON_ID1, beacon.getId1());
                itemData.put(BEACON_ID2, beacon.getId2());
                itemData.put(BEACON_ID3, beacon.getId3());
                itemData.put(BEACON_BT_NAME, beacon.getBluetoothName());
                itemData.put(BEACON_BT_ADDR, beacon.getBluetoothAddress());
                itemData.put(BEACON_TX, beacon.getTxPower());
                itemData.put(BEACON_RSSI, beacon.getRssi());
                itemData.put(BEACON_DISTANCE, beacon.getDistance());
                beaconsData.add(itemData);
            }

            String[] from = { BEACON_ID1, BEACON_ID2, BEACON_ID3, BEACON_BT_NAME, BEACON_BT_ADDR, BEACON_TX, BEACON_RSSI, BEACON_DISTANCE };
            int[] to = {R.id.tv_id1, R.id.tv_id2, R.id.tv_id3, R.id.tv_bt_name, R.id.tv_bt_addr,
                    R.id.tv_tx, R.id.tv_rssi, R.id.tv_distance};

            BeaconAdapter beaconAdapter = new BeaconAdapter(getActivity(), beaconsData, R.layout.info_cell_ibeacon_row, from, to);
            lvBeacons.setAdapter(beaconAdapter);
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private class BeaconAdapter extends SimpleAdapter {
        LayoutInflater mInflater;

        public BeaconAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View result;
            Map<String, ?> item = (Map<String, ?>) getItem(position);

            result = super.getView(position, convertView, parent);


            return result;
        }
    }
}






