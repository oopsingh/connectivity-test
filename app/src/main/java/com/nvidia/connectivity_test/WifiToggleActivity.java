package com.nvidia.connectivity_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;


public class WifiToggleActivity extends ActionBarActivity {

    String TAG = "Connectivity-test:WiFiToggle";
    private WifiManager mWifiManager;
    private BroadcastReceiver mReceiver;

    private boolean autoToggleEnable = false;
    private int autoToggleCount = 0;

    private int enableCount = 0;
    private int disableCount = 0;
    private boolean on_timer_started = false;
    private boolean off_timer_started = false;

    private long enableStartTime = 0L;
    private long disableStartTime = 0L;

    private long enableCurTime = 0L;
    private long enableAvgTime = 0L;
    private long enableMaxTime = 0L;
    private long totalEnableTime = 0L;

    private long disableCurTime = 0L;
    private long disableAvgTime = 0L;
    private long disableMaxTime = 0L;
    private long totalDisableTime = 0L;

    TextView onCurTime;
    TextView onAvgTime;
    TextView onMaxTime;

    TextView offCurTime;
    TextView offAvgTime;
    TextView offMaxTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_toggle);
        mWifiManager = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);

        final Switch mSwitchBar = (Switch) findViewById(R.id.switch_wifi);
        final CheckBox autoToggle = (CheckBox) findViewById(R.id.checkBoxAutoToggle);
        final EditText toggleNumber = (EditText) findViewById(R.id.text_toggle_number);
        final TextView textCycle = (TextView) findViewById(R.id.text_cycles);

        onCurTime = (TextView) findViewById(R.id.on_cur_time);
        onAvgTime = (TextView) findViewById(R.id.on_avg_time);
        onMaxTime = (TextView) findViewById(R.id.on_max_time);

        offCurTime = (TextView) findViewById(R.id.off_cur_time);
        offAvgTime = (TextView) findViewById(R.id.off_avg_time);
        offMaxTime = (TextView) findViewById(R.id.off_max_time);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                    int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    switch (state) {
                        case WifiManager.WIFI_STATE_ENABLING:
                            mSwitchBar.setEnabled(false);
                            break;
                        case WifiManager.WIFI_STATE_ENABLED:
                            mSwitchBar.setChecked(true);
                            mSwitchBar.setEnabled(true);
                            wifiEnableTimer(false);
                            if (autoToggleCount > 0) {
                                toggleWiFi(false);
                            }
                            break;
                        case WifiManager.WIFI_STATE_DISABLING:
                            mSwitchBar.setEnabled(false);
                            break;
                        case WifiManager.WIFI_STATE_DISABLED:
                            mSwitchBar.setChecked(false);
                            mSwitchBar.setEnabled(true);
                            wifiDisableTimer(false);
                            if (autoToggleCount > 0) {
                                //For autoToggle test have 1 second default delay before enabling WiFi
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) { }
                                toggleWiFi(true);
                                autoToggleCount--;
                                toggleNumber.setText(Objects.toString(autoToggleCount));
                            }
                            break;
                        default:
                            mSwitchBar.setChecked(false);
                            mSwitchBar.setEnabled(true);
                    }
                }
            }
        };
        registerReceiver(mReceiver, filter);

        /* set initial switch button state */
        if (mWifiManager.isWifiEnabled())
            mSwitchBar.setChecked(true);
        else
            mSwitchBar.setChecked(false);

        mSwitchBar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSwitchBar.setEnabled(false);
                    toggleWiFi(true);
                } else {
                    mSwitchBar.setEnabled(false);
                    toggleWiFi(false);
                }
            }
        });

        autoToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                  @Override
                                                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                      if (isChecked) {
                                                          textCycle.setText("Cycles pending:");
                                                          toggleNumber.setEnabled(false);
                                                          autoToggleCount = Integer.parseInt(toggleNumber.getText().toString());
                                                          if (mWifiManager.isWifiEnabled())
                                                              toggleWiFi(false);
                                                          else
                                                              toggleWiFi(true);
                                                      } else {
                                                          autoToggleCount = 0;
                                                          textCycle.setText("No of Cycles:");
                                                          toggleNumber.setEnabled(true);
                                                      }

                                                  }
                                              }
        );

        //Pass extra parameter from command line to start auto toggle test
        if (getIntent().getExtras()!= null) {
            int cycles;
            cycles = getIntent().getIntExtra("cycles", 1);
            Log.i(TAG, "starting test for " + cycles + " cycles");
            toggleNumber.setText(Objects.toString(cycles));
            autoToggle.setChecked(true);
        }
    }

    private void toggleWiFi(boolean status) {
        if (status == true && !mWifiManager.isWifiEnabled()) {
            wifiEnableTimer(true);
            mWifiManager.setWifiEnabled(true);
        } else if (status == false && mWifiManager.isWifiEnabled()) {
            wifiDisableTimer(true);
            mWifiManager.setWifiEnabled(false);
        }
    }

    private void wifiEnableTimer(boolean start) {
        if (start) {
            enableCount++;
            enableStartTime = SystemClock.uptimeMillis();
            on_timer_started = true;
        } else {
            if (on_timer_started) {
                // stop enable timer and update
                enableCurTime = SystemClock.uptimeMillis() - enableStartTime;
                totalEnableTime = totalEnableTime + enableCurTime;
                enableAvgTime = totalEnableTime/enableCount;
                if (enableCurTime > enableMaxTime)
                    enableMaxTime = enableCurTime;

                //update result on TextView
                onCurTime.setText(Objects.toString(enableCurTime));
                onAvgTime.setText(Objects.toString(enableAvgTime));
                onMaxTime.setText(Objects.toString(enableMaxTime));
                on_timer_started = false;
            }
        }

    }

    private void wifiDisableTimer(boolean start) {
        if (start) {
            disableCount++;
            disableStartTime = SystemClock.uptimeMillis();
            off_timer_started = true;
        } else {
            if (off_timer_started) {
                // stop disable timer and update
                disableCurTime = SystemClock.uptimeMillis() - disableStartTime;
                totalDisableTime = totalDisableTime + disableCurTime;
                disableAvgTime = totalDisableTime/disableCount;
                if (disableCurTime > disableMaxTime)
                    disableMaxTime = disableCurTime;

                //update result on TextView
                offCurTime.setText(Objects.toString(disableCurTime));
                offAvgTime.setText(Objects.toString(disableAvgTime));
                offMaxTime.setText(Objects.toString(disableMaxTime));
                off_timer_started = false;
            }
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi_toggle, menu);
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
