package com.Tony.Zakron;

import java.util.ArrayList;

import com.Tony.Zakron.Common.AppContext;
import com.Tony.Zakron.Common.CommonMethods;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

public class DeviceScanManager {
	private boolean mScanning;
	private Handler mHandler;
	private ArrayList<BluetoothDevice> mLeDevices;
	private DeviceScanListener mListener;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 3000;
    
    DeviceScanManager(DeviceScanListener listener) {
    	mHandler = new Handler();
    	mLeDevices = new ArrayList<BluetoothDevice>();
    	mScanning = false;
    	mListener = listener;
    }
    
    void startScan() {
    	if (mScanning)
    		return;
    	scanLeDevice(true);
    }
    
    void stopScan() {
    	if (!mScanning)
    		return;
    	scanLeDevice(false);
    }
    
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    AppContext._bluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (mListener != null)
                    	mListener.onScanned(mLeDevices);
                }
            }, SCAN_PERIOD);


            mScanning = true;
            AppContext._bluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            AppContext._bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        
    }
    
 // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                	CommonMethods.Log("device scanned - ");
                	if(!mLeDevices.contains(device)) {
                        mLeDevices.add(device);
                    }
                }
            };
}
