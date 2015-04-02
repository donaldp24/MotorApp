package com.Tony.Zakron;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.Tony.Zakron.Common.AppContext;
import com.Tony.Zakron.Settings.SettingsManager;
import com.Tony.Zakron.ble.BleManager;
import com.Tony.Zakron.event.EventManager;
import com.crashlytics.android.Crashlytics;

public class SplashScreen extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private Handler handler;
    private boolean isBleEnabled;
    public final int LOADINGVIEW_TIMEOUT = 3000;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        setContentView(R.layout.splash);

        initVariables();
        setupControls();
        checkBluetooth();
    }

    protected void initVariables() {
        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if (msg.what == 0) {
                    goNext();
                }
            }
        };
    }

    protected void setupControls() {
    }

    protected void checkBluetooth() {
        isBleEnabled = false;

        BleManager.initialize(getApplicationContext(), ((MotorApplication) getApplication()).getBluetoothCrashResolver());
        if (!BleManager.sharedInstance().isBleSupported()) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!BleManager.sharedInstance().isBleAvailable()) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        isBleEnabled = BleManager.sharedInstance().isBleEnabled();

        if (isBleEnabled) {
            handler.sendEmptyMessageDelayed(0, LOADINGVIEW_TIMEOUT);
        }
    }

    protected void goNext() {
        UIManager.initialize(getApplicationContext());
        EventManager.initalize(getApplicationContext());
        AppContext.initialize(getApplicationContext());
        SettingsManager.initialize(getApplicationContext());

        // scan bluetooth
        // BleManager.sharedInstance().scanForPeripheralsWithServices(null, true);

        Intent intent = new Intent(this, DeviceScanActivity.class);
        intent.putExtra("from", this.getClass().getName());
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.fade, R.anim.alpha);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!isBleEnabled) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
            else if (resultCode == Activity.RESULT_OK) {
                isBleEnabled = BleManager.sharedInstance().isBleEnabled();
                handler.sendEmptyMessageDelayed(0, LOADINGVIEW_TIMEOUT);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
