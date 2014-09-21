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

public class SplashScreen extends Activity {

    private RelativeLayout mainLayout;
    private boolean bInitialized = false;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;


    private Handler handler;
    public final int LOADINGVIEW_TIMEOUT = 3000;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        AppContext._mainContext = getApplicationContext();

        // Initialize Global
        AppContext.initialize(this);

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        AppContext._bluetoothAdapter = mBluetoothAdapter;

        mainLayout = (RelativeLayout)findViewById(R.id.RLSplashRoot);
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if (bInitialized == false)
                        {
                            Rect r = new Rect();
                            mainLayout.getLocalVisibleRect(r);
                            ResolutionSet._instance.setResolution(r.width(), r.height(), true);
                            ResolutionSet._instance.iterateChild(findViewById(R.id.RLSplashRoot));
                            bInitialized = true;
                        }
                    }
                }
        );

        //manager = ScanManager.managerWithListner(this, ScanManagerListenerInstance.sharedInstance());
        // have to be commented
        // manager.startScan();

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if (msg.what == 0)
                {
                    startActivity(new Intent(SplashScreen.this, ControlActivity.class));
                    overridePendingTransition(R.anim.fade, R.anim.alpha);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            handler.sendEmptyMessageDelayed(0, LOADINGVIEW_TIMEOUT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                finish();
                return;
            }
            else
            {
                handler.sendEmptyMessageDelayed(0, LOADINGVIEW_TIMEOUT);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
