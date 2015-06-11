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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WifiConnectActivity extends ActionBarActivity {

    WifiManager mWifiManager;
    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
    private int mNetId;
    private String mSSID;
    private long startTime = 0L;
    private long timeMs = 0L;
    private boolean connecting = false;
    private boolean disconnecting = false;
    private StringBuilder builder = new StringBuilder();
    private BroadcastReceiver mWifiStateReceiver;
    private ListView apList;
    private ArrayList<String> SSID_list;
    private TextView textStatus;
    private EditText numCycles;
    private ToggleButton mAutoConnectToggle;
    private int autoConnectIndex = 0;
    private int mAutoConnectCycles = 0;
    private String TAG = "Connectivity-Test:AP-connect";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_connect);
        apList = (ListView)findViewById(R.id.list_ap);
        textStatus = (TextView) findViewById((R.id.text_connection_status));
        mAutoConnectToggle = (ToggleButton) findViewById(R.id.toggleButton);
        numCycles = (EditText) findViewById(R.id.numCycle);
        SSID_list = new ArrayList<String>();

        mWifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo nInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

                    //Same network selected. on disconnect, start connection
                    if (nInfo.getState() == NetworkInfo.State.DISCONNECTED && disconnecting) {
                        disconnecting = false;
                        mWifiManager.enableNetwork(mNetId, true);
                        startTime = SystemClock.uptimeMillis();
                        connecting = true;
                        builder.append("Disconnected!. Connecting to: " + mSSID);
                        textStatus.setText(builder.toString());
                    }

                    if (mNetId == wifiInfo.getNetworkId() && connecting) {
                        if (nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                            textStatus.setText(builder.toString());
                        }

                        if (nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                            timeMs = SystemClock.uptimeMillis() - startTime;
                            textStatus.setText("Connected to: " + wifiInfo.getSSID() + "in " + timeMs + " milliseconds ");
                            Log.i(TAG, textStatus.getText().toString());
                            connecting = false;

                            //auto connect
                            if (mAutoConnectCycles == 0) {
                                mAutoConnectToggle.setChecked(false);
                                numCycles.setText("5");
                                numCycles.setEnabled(true);
                            } else {
                                numCycles.setText(Objects.toString(mAutoConnectCycles));
                            }
                            handleAutoConnect();
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
                    SSID_list.add(c.SSID);
               }
        }
        final ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, SSID_list);
        apList.setAdapter(adapter);

        if (mWifiManager.getConnectionInfo().getNetworkId() != -1) {
            textStatus.setText("Wifi Connected to:" + mWifiManager.getConnectionInfo().getSSID());
        }

        apList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                final int pos = position;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        apConnect(pos);
                    }
                }).start();
            }
        });

        //Pass extra parameter from command line to start auto toggle test
        if (getIntent().getExtras()!= null) {
            int cycles;
            mAutoConnectCycles = getIntent().getIntExtra("cycles", 1);
            Log.i(TAG, "starting auto connect test for " + mAutoConnectCycles + "cycle");
            mAutoConnectToggle.setChecked(true);
            numCycles.setText(Objects.toString(mAutoConnectCycles));
            numCycles.setEnabled(false);
            handleAutoConnect();
        }
    } //end of onCreate

    //Connect to ap in SSID_list of index pos
    private void apConnect(int pos) {
        String ssid = SSID_list.get(pos);
        for (Map.Entry<String, Integer> entry : mIdMap.entrySet()) {
            if (ssid.equals(entry.getKey())) {
                mNetId = entry.getValue();
                mSSID = ssid;
                if (mNetId == mWifiManager.getConnectionInfo().getNetworkId()) {
                    builder = new StringBuilder();
                    builder.append("Same network selected! Disconnecting...\n");
                    disconnecting = true;   //use it to connect to same network on DISCONNECTED event
                    mWifiManager.disconnect();
                } else {
                    mWifiManager.enableNetwork(mNetId, true);
                    startTime = SystemClock.uptimeMillis();
                    connecting = true;
                    builder = new StringBuilder();
                    builder.append("Network selected: " + ssid);
                }
                break;
            }
        }
    }

    void handleAutoConnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mAutoConnectToggle.isChecked() & mAutoConnectCycles > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { }
                    apConnect(autoConnectIndex);
                    autoConnectIndex = (++autoConnectIndex) % SSID_list.size();
                    mAutoConnectCycles--;
                }
            }
        }).start();
    }

    public void autoConnect(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            mAutoConnectCycles = Integer.parseInt(numCycles.getText().toString());
            numCycles.setEnabled(false);
            autoConnectIndex = 0;
            handleAutoConnect();
        } else {
            numCycles.setText("5");
            numCycles.setEnabled(true);
        }
    }

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
