package com.Tony.Zakron.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import com.Tony.Zakron.helper.Logger;
import com.radiusnetworks.bluetooth.BluetoothCrashResolver;

import java.util.ArrayList;

public class BleScanner {

	public static final String TAG = "BleScanner";

	private static BleScanner _instance = null;
	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothCrashResolver _bluetoothCrashResolver;

	public BleScannerListener listner = null;
	private boolean isStarted = false;
	private boolean isForceStopped = true;

	protected static final boolean USE_REAPEAT_SCANNING = true;

	protected static final int SCANNING_ONCE_PERIOD = 5 * 1000; // milliseconds
	protected static final int SCANNING_INTERVAL = 2 * 1000; // milliseconds

	private ArrayList<BluetoothDevice> scannedDevices;

	private Handler mHandler;
	private boolean mStartPhase = true;
	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			if (USE_REAPEAT_SCANNING) {
				if (!mStartPhase) {
					_stopScanLocally();
					if (!isForceStopped)
						mHandler.postDelayed(this, SCANNING_INTERVAL);
					mStartPhase = true;
				}
				else {
					if (!isForceStopped) {
						_startScanLocally();
						mHandler.postDelayed(this, SCANNING_ONCE_PERIOD);
					}
					mStartPhase = false;
				}
			}
		}
	};

	public static BleScanner initialize(Context context, BluetoothCrashResolver bluetoothCrashResolver) {
		if (_instance == null)
			_instance = new BleScanner(context, bluetoothCrashResolver);
		return _instance;
	}

	public static BleScanner sharedInstance() {
		return _instance;
	}

	private BleScanner(Context context, BluetoothCrashResolver bluetoothCrashResolver) {
		this.mContext = context;
		_bluetoothCrashResolver = bluetoothCrashResolver;
		scannedDevices = new ArrayList<BluetoothDevice>();
		final BluetoothManager mBluetoothManager = (BluetoothManager) this.mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		mHandler = new Handler(context.getMainLooper());
	}

	public boolean start() {
		Logger.log(TAG, "BleScanner start()");

		scannedDevices.clear();

		isForceStopped = false;
		mStartPhase = true;

		if (USE_REAPEAT_SCANNING) {
			mHandler.post(mRunnable);
			return true;
		}
		else {
			return _startScanLocally();
		}
	}

	public void stop() {
		Logger.log(TAG, "BleScanner stop()");

		isForceStopped = true;
	}

	public boolean isStopped() {
		return isForceStopped;
	}

	protected boolean _startScanLocally() {
		synchronized (Thread.currentThread()) {
			if (isStarted) {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				Logger.log(TAG, "BleScanner already started, stop and restart");
			}

			scannedDevices.clear();

			if (mBluetoothAdapter == null) {
				Logger.log(TAG, "BleScanner _startScanLocally() - mBluetoothAdapter == null, not started");
				return false;
			}

			if (!mBluetoothAdapter.isEnabled()) {
				Logger.log(TAG, "BleScanner _startScanLocally() - mBluetoothAdapter.isEnabled() == false, not started");
				return false;
			}

			if (mBluetoothAdapter.startLeScan(mLeScanCallback)) {
				Logger.log(TAG, "BleScanner _startScanLocally() - started succesfully");
				isStarted = true;
				return true;
			} else {
				Logger.log(TAG, "BleScanner _startScanLocally() - cannot start successfully");
				return false;
			}
		}
	}

	protected void _stopScanLocally() {
		Logger.log(TAG, "BleScanner _stopScanLocally()");
		synchronized (Thread.currentThread()) {
			if (mBluetoothAdapter == null) {
				Logger.log(TAG, "BleScanner _stopScanLocally() - mBluetoothAdapter == null, not stopped");
				return;
			}
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			isStarted = false;
		}
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,byte[] scanRecord) {
			_bluetoothCrashResolver.notifyScannedDevice(device, mLeScanCallback);

			if (isForceStopped) {
				Logger.log(TAG, "BleScanner mLeScanCallback - device(%s - %s), isForceStopped = true, ignore it", device.getAddress(), device.getAddress());
				return;
			}

			Logger.log(TAG, "BleScanner mLeScanCallback - device(%s - %s)", device.getAddress(), device.getAddress());
			if (!scannedDevices.contains(device))
				scannedDevices.add(device);

			String strHex = "";
			for (int i = 0; i < scanRecord.length; i++) {
				strHex += String.format("%02X ", scanRecord[i]);
			}
			Logger.log(TAG, device.getAddress() + " - " + device.getName() + " : " + strHex);

			if (listner != null) {
				if (listner.shouldCheckDevice(device, rssi, scanRecord))
					listner.deviceScanned(device, rssi, scanRecord);
			}
		}
	};
}
