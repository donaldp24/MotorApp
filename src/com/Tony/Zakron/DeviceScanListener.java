package com.Tony.Zakron;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;

public interface DeviceScanListener {
	
	public void onScanned(ArrayList<BluetoothDevice> devices);
}
