package com.Tony.Zakron.ble;

import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import com.Tony.Zakron.helper.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by donal_000 on 12/1/2014.
 */
public class BlePeripheral {

    public static final String TAG = "BlePeripheral";

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice mBluetoothDevice;
    private int _rssi;
    private String _serialno;

    public long scannedTime;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final int CONNECTION_TIMEOUT = 20;
    public static final int DISCOVERING_TIMEOUT = 60;

    public BlePeripheralDelegate delegate;

    private String mAddress;
    private String _name;
    private byte[] _scanRecord;

    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private Runnable timeoutDiscoverRunnable;


    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                timeoutHandler.removeCallbacks(timeoutRunnable);

                if (delegate != null)
                    delegate.gattConnected(BlePeripheral.this);

                Logger.log(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Logger.log(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

                timeoutHandler.postDelayed(timeoutDiscoverRunnable, TimeUnit.SECONDS.toMillis(DISCOVERING_TIMEOUT));

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                mConnectionState = STATE_DISCONNECTED;
                timeoutHandler.removeCallbacks(timeoutRunnable);

                Logger.log(TAG, "Disconnected from GATT server.");

                if (delegate != null)
                    delegate.gattDisconnected(BlePeripheral.this);

                try {
                    close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                timeoutHandler.removeCallbacks(timeoutDiscoverRunnable);
                if (delegate != null)
                    delegate.gattServicesDiscovered(BlePeripheral.this);
            } else {
                Logger.log(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Logger.log(TAG, "onCharacteristicRead, length = %d", (characteristic.getValue() != null)?(characteristic.getValue().length):0);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (delegate != null)
                    delegate.gattDataAvailable(BlePeripheral.this, characteristic, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Logger.log(TAG, "onCharacteristicChanged, length = %d", (characteristic.getValue() != null)?(characteristic.getValue().length):0);
            if (delegate != null)
                delegate.gattDataAvailable(BlePeripheral.this, characteristic, characteristic.getValue());
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            _rssi = rssi;
            if (delegate != null)
                delegate.gattReadRemoteRssi(BlePeripheral.this, rssi);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (delegate != null)
                delegate.gattDescriptorWrite(BlePeripheral.this, descriptor, status == BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (delegate != null)
                delegate.gattCharacteristicWrite(BlePeripheral.this, characteristic, status == BluetoothGatt.GATT_SUCCESS);
        }
    };

    private BlePeripheral() {
        //
    }

    public BlePeripheral(Context context, String address, int rssi, byte[] scanRecord) {
        this.mContext = context;
        this.mBluetoothDevice = null;
        this.mAddress = address;
        this._rssi = rssi;
        this._scanRecord = scanRecord;

        this.scannedTime = System.currentTimeMillis();

        this.timeoutHandler = new Handler(context.getMainLooper());
        this.timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (connectionState() != STATE_CONNECTED) {
                    Logger.log(TAG, "BlePeripheral timoutRunnable - disconnect()");
                    disconnect();
                    if (delegate != null) {
                        delegate.gattDisconnected(BlePeripheral.this);
                    }
                }
            }
        };

        this.timeoutDiscoverRunnable = new Runnable() {
            @Override
            public void run() {
                if (connectionState() == STATE_CONNECTED) {
                    Logger.log(TAG, "BlePeripheral discovering timeout - disconnect()");
                    disconnect();
                }
            }
        };

        mBluetoothAdapter = BleManager.sharedInstance().bluetoothAdapter();
        if (mBluetoothAdapter == null) {
            Logger.log("BlePeripheral(%s) - blemanager.bluetoothadapter is null", address);
            return;
        }

        this.mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mAddress);
        if (this.mBluetoothDevice == null) {
            Logger.log(TAG, "BlePeripheral(%s) - bluetoothadapter.getremotedevice is null", address);
            return;
        }

        _name = this.mBluetoothDevice.getName();
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect() {
        if (mBluetoothAdapter == null || mAddress == null) {
            Logger.log(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && mAddress.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Logger.log(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                timeoutHandler.postDelayed(timeoutRunnable, TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mAddress);
        if (device == null) {
            Logger.log(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        Logger.log(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = mAddress;
        mConnectionState = STATE_CONNECTING;

        timeoutHandler.postDelayed(timeoutRunnable, TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logger.log(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Logger.log(TAG, "BlePeripheral mBluetoothGatt.disconnect()");
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public int connectionState() {
        return mConnectionState;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logger.log(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, int writeType, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logger.log(TAG, "BluetoothAdaptor or mBluetoothGatt is null");
            return false;
        }

        if (characteristic == null) {
            Logger.log(TAG, "characteristic is null");
            return false;
        }
        characteristic.setValue(value);
        characteristic.setWriteType(writeType);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logger.log(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void setCharacteristicNotificationV1(BluetoothGattCharacteristic characteristic, boolean enabled) {
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public String name() {
        return _name;
    }

    public String address() {
        return mAddress;
    }

    public int rssi() {
        return _rssi;
    }

    public void setRssi(int rssi) {
        _rssi = rssi;
    }

    public void updateRSSI() {
        if (mConnectionState == STATE_CONNECTED)
            mBluetoothGatt.readRemoteRssi();
    }

    public void setScanRecord(byte[] scanRecord) {
        _scanRecord = scanRecord;
    }

    public byte[] scanRecord() {
        return _scanRecord;
    }

    public String serialno() {
        return _serialno;
    }

    public void setSerialNo(String serialNo) {
        _serialno = serialNo;
    }

    public String getCode() {
        String name = name();
        if (name == null)
            return "";
        if (name.length() == 0)
            return "";
        if (name.length() < "Power Band".length() + 6)
            return "";
        String code = name.substring(11);
        return code;
    }
}
