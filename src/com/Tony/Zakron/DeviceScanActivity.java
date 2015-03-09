package com.Tony.Zakron;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.Tony.Zakron.Common.AppContext;
import com.Tony.Zakron.ConnectBlue.SerialPort;
import com.Tony.Zakron.ble.BleManager;
import com.Tony.Zakron.ble.BlePeripheral;
import com.Tony.Zakron.ble.BleScanner;
import com.Tony.Zakron.event.EventManager;
import com.Tony.Zakron.event.SEvent;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/25/14
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */

public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;
    private Handler mHandler;
    private Runnable mRunnable;
    private boolean scanUpdated;
    private Handler mStopHandler;
    private Runnable mStopRunnable;
    private BlePeripheral _selectedPeripheral;
    private SerialPort _serialPort;
    private Object _dlg;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);

        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler(this.getMainLooper());
        mStopHandler = new Handler(this.getMainLooper());

        startScan();

    }

    protected void startScan() {
        scanUpdated = false;
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (scanUpdated && !BleScanner.sharedInstance().isStopped()) {
                    ArrayList<BlePeripheral> scannedPeripherals = BleManager.sharedInstance().getScannedPeripherals();
                    mLeDeviceListAdapter.replaceWith(scannedPeripherals);
                    scanUpdated = false;

                    int nCount = scannedPeripherals.size();
                    String subtitle = String.format("%d device(s) scanned", nCount);
                    getActionBar().setSubtitle(subtitle);
                }
                if (!BleScanner.sharedInstance().isStopped()) {
                    mHandler.postDelayed(mRunnable, 1000);
                }
            }
        };

        mHandler.postDelayed(mRunnable, 1000);

        mScanning = BleManager.sharedInstance().scanForPeripheralsWithServices(null, true);

        mStopRunnable = new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        };

        mStopHandler.postDelayed(mStopRunnable, SCAN_PERIOD);

        invalidateOptionsMenu();
    }

    protected void stopScan() {
        scanUpdated = false;
        mHandler.removeCallbacks(mRunnable);
        mStopHandler.removeCallbacks(mStopRunnable);

        mScanning = false;
        BleManager.sharedInstance().stopScan();

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                startScan();
                break;
            case R.id.menu_stop:
                stopScan();
                break;
        }
        invalidateOptionsMenu();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventManager.sharedInstance().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventManager.sharedInstance().unregister(this);
        stopScan();
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        stopScan();

        final BlePeripheral peripheral = mLeDeviceListAdapter.getDevice(position);
        if (peripheral == null)
            return;

        _selectedPeripheral = peripheral;

        // create serialport
        _serialPort = new SerialPort(_selectedPeripheral);
        boolean ret = _serialPort.open();
        if (ret == false) {
            UIManager.sharedInstance().showToastMessage(this, "connecting failed");
            return;
        }

        _dlg = UIManager.sharedInstance().showProgressDialog(this, null, "connecting...", true);

        /*
        final Intent intent = new Intent(this, ControlActivity.class);
        intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        if (mScanning) {
            AppContext._bluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }

        setResult(Activity.RESULT_OK, intent);
        finish();
        */
    }

    public void onEventMainThread(SEvent e) {
        if (EventManager.isEvent(e, BleManager.kBLEManagerDiscoveredPeripheralNotification)) {
            scanUpdated = true;
        }
        else if (EventManager.isEvent(e, SerialPort.kSerialPortConnectedNotification)) {
            if (_serialPort == e.object) {
                if (_dlg != null) {
                    UIManager.sharedInstance().dismissProgressDialog(_dlg);
                    _dlg = null;
                }

                UIManager.sharedInstance().showToastMessage(this, "Opened successfully");
                // show maint menu activity

                MotorApplication application = (MotorApplication)getApplication();
                application.setSerialPort(_serialPort);

                // start main menu activity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
        else if (EventManager.isEvent(e, SerialPort.kSerialPortDisconnectedNotification)) {
            if (_serialPort == e.object) {
                if (_dlg != null) {
                    UIManager.sharedInstance().dismissProgressDialog(_dlg);
                    _dlg = null;
                }
                UIManager.sharedInstance().showToastMessage(this, "Failed to open serial port");
            }
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BlePeripheral> mPeripherals;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mPeripherals = new ArrayList<BlePeripheral>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void replaceWith(ArrayList<BlePeripheral> devices) {
            mPeripherals = devices;
            notifyDataSetChanged();
        }

        public BlePeripheral getDevice(int position) {
            return mPeripherals.get(position);
        }

        public void clear() {
            mPeripherals.clear();
        }

        @Override
        public int getCount() {
            return mPeripherals.size();
        }

        @Override
        public Object getItem(int i) {
            return mPeripherals.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                ResolutionSet._instance.iterateChild(view);

                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BlePeripheral peripheral = mPeripherals.get(i);
            final String deviceName = peripheral.name();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(peripheral.address());

            byte[] scanRecord = peripheral.scanRecord();
            if (scanRecord != null) {
                StringBuilder sbData = new StringBuilder();
                sbData.append('{');
                int t;
                for (i = 0; i < scanRecord.length; i++) {
                    t = (int) (scanRecord[i] & 0xFF);
                    sbData.append(String.format("%02X", t));
                    if (i < scanRecord.length - 1)
                        sbData.append(' ');
                }
                sbData.append('}');
                viewHolder.deviceAddress.setText(sbData);
            }
            else {
                viewHolder.deviceAddress.setText("");
            }

            int rssi = peripheral.rssi();
            String strRssi = String.format("%d", rssi);
            viewHolder.deviceRssi.setText(strRssi);

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }
}