package com.nvidia.connectivity_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class WifiConnectActivity extends ActionBarActivity {

    WifiManager mWifiManager;
    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
    private int mNetId;
    private long startTime = 0L;
    private long timeMs = 0L;
    private boolean connecting = false;
    private StringBuilder builder = new StringBuilder();
    private BroadcastReceiver mWifiStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_connect);
        final ListView apList = (ListView)findViewById(R.id.list_ap);
        final TextView textStatus = (TextView) findViewById((R.id.text_connection_status));
        final ArrayList<String> list = new ArrayList<String>();

        mWifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo nInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (mNetId == wifiInfo.getNetworkId() && connecting) {
                        if (nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                            timeMs = SystemClock.uptimeMillis() - startTime;
                            builder.append("\nConnection done in ms: " + timeMs);
                            builder.append("\nconnected to: ");
                            builder.append(wifiInfo.getSSID());
                            textStatus.setText(builder.toString());
                            connecting = false;
                        }
                    }
                }
            }
        };
        registerReceiver(mWifiStateReceiver, filter);

        List<WifiConfiguration> configList = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration c : configList) {
               if(c.status != WifiConfiguration.Status.DISABLED) {
                    mIdMap.put(c.SSID, c.networkId);
                    list.add(c.SSID);
               }
        }
        final ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        apList.setAdapter(adapter);

        if (mWifiManager.getConnectionInfo().getNetworkId() != -1) {
            textStatus.setText("Wifi Connected to:" + mWifiManager.getConnectionInfo().getSSID());
        }

        apList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                String ssid = list.get(position);
                int netId;
                for (Map.Entry<String, Integer> entry : mIdMap.entrySet()) {
                    if (ssid.equals(entry.getKey())) {
                        mNetId = entry.getValue();
                        if (mNetId == mWifiManager.getConnectionInfo().getNetworkId()) {
                            textStatus.setText("Same network selected!");
                        } else {
                            mWifiManager.enableNetwork(mNetId, true);
                            builder = new StringBuilder();
                            builder.append("Network selected: " + ssid);
                            textStatus.setText(builder.toString());
                            connecting = true;
                            startTime = SystemClock.uptimeMillis();
                        }
                        break;
                    }
                }
            }
        });
    } //end of onCreate

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mWifiStateReceiver);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
