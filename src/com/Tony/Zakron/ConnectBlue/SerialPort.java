package com.Tony.Zakron.ConnectBlue;

import android.bluetooth.*;
import android.os.Handler;
import com.Tony.Zakron.Common.CommonMethods;
import com.Tony.Zakron.ble.BleManager;
import com.Tony.Zakron.ble.BlePeripheral;
import com.Tony.Zakron.event.EventManager;
import com.Tony.Zakron.event.SEvent;
import com.Tony.Zakron.helper.Logger;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/25/14
 * Time: 4:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class SerialPort {

    public static final String TAG = "SerialPort";

    public static final int SP_CURRENT_SERIAL_PORT_VERSION = 2;

    private final byte serialPortServiceUuid[] = {
            (byte)0x24, (byte)0x56, (byte)0xe1, (byte)0xb9, (byte)0x26, (byte)0xe2, (byte)0x8f, (byte)0x83,
            (byte)0xe7, (byte)0x44, (byte)0xf3, (byte)0x4f, (byte)0x01, (byte)0xe9, (byte)0xd7, (byte)0x01};

    private final byte serialPortFifoCharactUuid[] = {
            (byte)0x24, (byte)0x56, (byte)0xe1, (byte)0xb9, (byte)0x26, (byte)0xe2, (byte)0x8f, (byte)0x83,
            (byte)0xe7, (byte)0x44, (byte)0xf3, (byte)0x4f, (byte)0x01, (byte)0xe9, (byte)0xd7, (byte)0x03};

    private final byte creditsCharactUuid[] = {
            (byte)0x24, (byte)0x56, (byte)0xe1, (byte)0xb9, (byte)0x26, (byte)0xe2, (byte)0x8f, (byte)0x83,
            (byte)0xe7, (byte)0x44, (byte)0xf3, (byte)0x4f, (byte)0x01, (byte)0xe9, (byte)0xd7, (byte)0x04};

    private final byte deviceIdServiceUuid[]           = {0x18, 0x0a};
    private final byte modelNumberCharactUuid[]        = {0x2a, 0x24};
    private final byte serialNumberCharactUuid[]       = {0x2a, 0x25};
    private final byte firmwareRevisionCharactUuid[]   = {0x2a, 0x26};

    public static final int DEVICE_NOTCONNECTED = 0;
    public static final int DEVICE_CONNECTING = 1;
    public static final int DEVICE_CONNECTED = 2;
    public static final int DEVICE_DISCONNECTED = 3;
    public static final int DEVICE_OPENED = 4;

    private int mDeviceState = DEVICE_NOTCONNECTED;

    public static final String kSerialPortDataUpdatedNotification = "serial port data updated notification";
    public static final String kSerialPortConnectedNotification = "serial port connected notification";
    public static final String kSerialPortDisconnectedNotification = "serial port disconnected notification";
    public static final String kSerialPortReadDataNotification = "serial port read data notification";

    public static final String kLocalCharacteristicNotificationFailed = "kLocalCharacteristicNotificationFailed";

    public static final int SP_MAX_WRITE_SIZE = 20;

    public int _rssi = 0;

    private int                 _serialPortVersion;

    // services and characteristics
    BluetoothGattService          _service;
    BluetoothGattCharacteristic   _fifoCharacteristic;

    BluetoothGattService          _deviceIdService;
    BluetoothGattCharacteristic     _fwVersionCharacteristic;
    BluetoothGattCharacteristic     _modelNumberCharacteristic;

    // communication timeout in minutes
    // writing - reading,discovering
    public static final int BLECOMMUNICATE_TIMEOUT = 60;

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.log(TAG, "SyncingDevice.DataService - characteristic notification timeout");
            EventManager.sharedInstance().post(kLocalCharacteristicNotificationFailed, _peripheral);
        }
    };

    private BlePeripheral _peripheral;
    private boolean _isRegistered;

    public static class ReadData {
        public byte[] bytes;
        public SerialPort port;
    }

    public SerialPort(BlePeripheral peripheral) {
        _serialPortVersion = 0;
        _peripheral = peripheral;

        EventManager.sharedInstance().register(this);
        _isRegistered = true;

        // init services and characteristics
        _service = null;
        _fifoCharacteristic = null;

        _deviceIdService = null;
        _fwVersionCharacteristic = null;
        _modelNumberCharacteristic = null;

        // check state
        if (peripheral != null) {
            if (peripheral.connectionState() == BlePeripheral.STATE_CONNECTED) {
                if (mDeviceState == DEVICE_NOTCONNECTED) {
                    _peripheralConnected(peripheral);
                }
            }
        }
    }

    public int getDeviceStatus() {
        return mDeviceState;
    }

    protected int _getVersionFromString(String sVersion)  {
        String arr[] = sVersion.split("\\.");

        int major = Integer.parseInt(arr[0]);
        int minor = Integer.parseInt(arr[1]);

        String arr2[] = arr[2].split(" ");

        int subminor = Integer.parseInt(arr2[0]);

        return ((major << 16) | (minor << 8) | (subminor));
    }

    protected int _getSerialPortVersion() {
        return SP_CURRENT_SERIAL_PORT_VERSION;
        /*
   	
        int version = 0;

        // Version of different models that implement the current serial port service version
        String sOLS = "OLS";
        int vOLS = 0x00010100;

        String sOLP = "OLP";
        int vOLP = 0x00010200;

        String sOBS421 = "OBS421";
        int vOBS421 = 0x00050100;

        if((_modelNumberCharacteristic != null) && (_fwVersionCharacteristic != null) &&
                (_modelNumberCharacteristic.getValue() != null) && (_fwVersionCharacteristic.getValue() != null)) {
            String sModel = null;
            try {
                sModel = new String(_modelNumberCharacteristic.getValue(), "utf8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            String sFwVersion = null;
            try {
                sFwVersion = new String(_fwVersionCharacteristic.getValue(), "utf8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            int fwVersion = _getVersionFromString(sFwVersion);

            if(sModel.contains(sOBS421) && (fwVersion < vOBS421)) {
                version = 1;
            }
            else if(sModel.contains(sOLS) && (fwVersion < vOLS)) {
                version = 1;
            }
            else if(sModel.contains(sOLP) && (fwVersion < vOLP)) {
                version = 1;
            }
            else {
                version = SP_CURRENT_SERIAL_PORT_VERSION;
            }
        }
        return version;
        */
    }

    public void _openSerialPortService() {
        CommonMethods.Log("_openSerialPortService ---");
        _serialPortVersion = _getSerialPortVersion();

        CommonMethods.Log("serialPortVersion = %d", _serialPortVersion);

        this.timeoutHandler.postDelayed(timeoutRunnable, BLECOMMUNICATE_TIMEOUT * 1000);

        if(_serialPortVersion == 1) {
            _peripheral.setCharacteristicNotificationV1(_fifoCharacteristic, true);
        }
        else {
            _peripheral.setCharacteristicNotification(_fifoCharacteristic, false);
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _peripheral.setCharacteristicNotification(_fifoCharacteristic, true);
        }
    }

    public void _discoverCharacteristics() {
        // discover for service
        List<BluetoothGattCharacteristic> characteristics = _service.getCharacteristics();
        CommonMethods.Log("_discoverCharacteristics - service - characteristics count : %d", characteristics.size());

        if (characteristics.size() > 0) {
            _fifoCharacteristic = _service.getCharacteristic(CommonMethods.toUUID(serialPortFifoCharactUuid));
            if (_fifoCharacteristic == null) {
                CommonMethods.Log("_discoverCharacteristics - fifoCharacteristic is null");
            }
        }

        // discover for deviceIdService
        characteristics = _deviceIdService.getCharacteristics();
        CommonMethods.Log("_discoverCharacteristics - deviceIdService - characteristics count : %d", characteristics.size());
        if (characteristics.size() > 0) {
            _modelNumberCharacteristic = _deviceIdService.getCharacteristic(CommonMethods.toUUID(modelNumberCharactUuid[0], modelNumberCharactUuid[1]));
            if (_modelNumberCharacteristic == null) {
                CommonMethods.Log("_discoverCharacteristics - _modelNumberCharacteristic is null");
            }
            _fwVersionCharacteristic = _deviceIdService.getCharacteristic(CommonMethods.toUUID(firmwareRevisionCharactUuid[0], firmwareRevisionCharactUuid[1]));
            if (_fwVersionCharacteristic == null) {
                CommonMethods.Log("_discoverCharacteristics -_fwVersionCharacteristic is null");
            }
        }
        else {
            CommonMethods.Log("_discoverCharacteristics - there is no characteristics on _deviceIdService");
        }

        if ((_fifoCharacteristic != null) &&
                (_modelNumberCharacteristic != null) /*&&
                (_modelNumberCharacteristic.getValue() != null) */ &&
                (_fwVersionCharacteristic != null)/* &&
                (_fwVersionCharacteristic.getValue() != null)*/) {
            CommonMethods.Log("_discoverCharacteristics calling _openSerialPortService()");
            _openSerialPortService();
        }
        else {
            CommonMethods.Log("_discoverCharacteristics not called _openSerialPortService() == null");
        }
    }

    public void onEvent(SEvent e) {
        if (EventManager.isEvent(e, BleManager.kBLEManagerConnectedPeripheralNotification)) {
            _peripheralConnected((BlePeripheral)e.object);
        }
        else if (EventManager.isEvent(e, BleManager.kBLEManagerDisconnectedPeripheralNotification)) {
            _peripheralDisconnected((BlePeripheral)e.object);
        }
        else if (EventManager.isEvent(e, BleManager.kBLEManagerPeripheralServiceDiscovered)) {
            _retrievedCharacteristics((BlePeripheral)e.object);
        }
        else if (EventManager.isEvent(e, BleManager.kBLEManagerPeripheralDescriptorWrite)) {
            BleManager.DescriptorData data = (BleManager.DescriptorData)e.object;
            _descriptorWrite(data.peripheral, data.descriptor, data.success);
        }
        else if (EventManager.isEvent(e, kLocalCharacteristicNotificationFailed)) {
            _notificationFailed((BlePeripheral) e.object);
        }
        else if (EventManager.isEvent(e, BleManager.kBLEManagerPeripheralDataAvailable)) {
            BleManager.CharacteristicData data = (BleManager.CharacteristicData)e.object;
            _readCharacteristic(data.peripheral, data.characteristic, data.value);
        }
        else if (EventManager.isEvent(e, BleManager.kBLEManagerPeripheralCharacteristicWrite)) {
            BleManager.CharacteristicWriteData data = (BleManager.CharacteristicWriteData)e.object;
            _writedCharacteristic(data.peripheral, data.characteristic, data.success);
        }
        else if (EventManager.isEvent(e, BleManager.kBLEManagerPeripheralRssiUpdated)) {
            _rssiUpdated((BlePeripheral) e.object);
        }
    }

    protected void _peripheralConnected(BlePeripheral peripheral) {
        if (peripheral != _peripheral || peripheral == null)
            return;

        if (mDeviceState == DEVICE_NOTCONNECTED) {
            Logger.log(TAG, "SerialPort._peripheralConnected (%s), state == BAND_NOTCONNECTED => BAND_CONNECTING", peripheral.address());
            mDeviceState = DEVICE_CONNECTING;
        }
        else {
            Logger.log(TAG, "SerialPort._peripheralConnected (%s), state (%d) != BAND_NOTCONNECTED", peripheral.address(), mDeviceState);
        }

        EventManager.sharedInstance().post(kSerialPortDataUpdatedNotification, this);
    }

    protected void _peripheralDisconnected(BlePeripheral peripheral) {
        if (peripheral != _peripheral || peripheral == null)
            return;

        Logger.log(TAG, "SerialPort._peripheralDisconnected (%s)", peripheral.address());

        if (mDeviceState == DEVICE_NOTCONNECTED) {
            mDeviceState = DEVICE_DISCONNECTED;
            Logger.log(TAG, "SerialPort._peripheralDisconnected (%s), state => BAND_DISCONNECTED", peripheral.address());
        }
        else if (mDeviceState == DEVICE_CONNECTING) {
            Logger.log(TAG, "SerialPort._peripheralDisconnected, mBandState == BAND_CONNECTING");
            mDeviceState = DEVICE_DISCONNECTED;
            _peripheral.disconnect();
        }
        else if (mDeviceState == DEVICE_CONNECTED) {
            Logger.log(TAG, "SerialPort._peripheralDisconnected, mBandState == BAND_CONNECTED, change state to DISCONNECTED");
            mDeviceState = DEVICE_DISCONNECTED;
            Logger.log(TAG, "SerialPort._peripheralDisconnected (%s), state => BAND_DISCONNECTED", peripheral.address());
        }
        else if (mDeviceState == DEVICE_OPENED) {
            mDeviceState = DEVICE_DISCONNECTED;
        }

        EventManager.sharedInstance().post(kSerialPortDisconnectedNotification, this);
    }

    protected void _retrievedCharacteristics(BlePeripheral peripheral) {
        if (peripheral != _peripheral || peripheral == null)
            return;

        Logger.log(TAG, "_retrievedCharacteristics, peripheral(%s)", _peripheral.address());
        List<BluetoothGattService> serviceList = peripheral.getSupportedGattServices();
        Iterator i = serviceList.iterator(); // Must be in synchronized block
        while (i.hasNext()) {
            BluetoothGattService service = (BluetoothGattService)i.next();
            if (service.getUuid().equals(CommonMethods.toUUID(serialPortServiceUuid))) {
                _service = service;
            }
            else if (service.getUuid().equals(CommonMethods.toUUID(deviceIdServiceUuid[0], deviceIdServiceUuid[1]))) {
                _deviceIdService = service;
            }
        }

        if (_service == null) {
            Logger.logError(TAG, "_retrievedCharacteristics , _peripheral(%s), retrievedCharacteristics error", _peripheral.address());
            _peripheral.disconnect();
        }
        else if (_deviceIdService == null) {
            Logger.logError(TAG, "_retrievedCharacteristics , _peripheral(%s), retrievedCharacteristics error", _peripheral.address());
            _peripheral.disconnect();
        }
        else {
            _discoverCharacteristics();
        }
    }

    // called when characteristic config written
    protected void _descriptorWrite(BlePeripheral peripheral, BluetoothGattDescriptor descriptor, boolean success) {
        if (peripheral != _peripheral || peripheral == null)
            return;

        // remove timeout handler
        this.timeoutHandler.removeCallbacks(this.timeoutRunnable);

        if (success == false) {
            Logger.log(TAG, "_descriptorWrite failed, peripheral (%s), BAND_CONNECTED", peripheral.address());
            EventManager.sharedInstance().post(kLocalCharacteristicNotificationFailed, _peripheral);
        }
        else {
            mDeviceState = DEVICE_OPENED;
            Logger.log(TAG, "_descriptorWrite success, peripheral (%s), BAND_CONNECTED", peripheral.address());
            EventManager.sharedInstance().post(kSerialPortConnectedNotification, this);
        }
    }

    protected void _notificationFailed(BlePeripheral peripheral) {
        if (peripheral != _peripheral)
            return;

        Logger.e("SerialPort._notificationFailed, peripheral(%s)", peripheral.address());
        mDeviceState = DEVICE_DISCONNECTED;
        _peripheral.disconnect();
    }

    protected void _readCharacteristic(BlePeripheral peripheral, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (peripheral != _peripheral || peripheral == null)
            return;
        ReadData data = new ReadData();
        data.port = this;
        data.bytes = value;
        EventManager.sharedInstance().post(kSerialPortReadDataNotification, data);
    }

    protected void _writedCharacteristic(BlePeripheral peripheral, BluetoothGattCharacteristic characteristic, boolean success) {
        if (peripheral != _peripheral || peripheral == null)
            return;
    }

    protected void _rssiUpdated(BlePeripheral peripheral) {
        if (_peripheral != peripheral) {
            return;
        }

        _rssi = _peripheral.rssi();
    }


    public boolean open() {
        if (!_isRegistered) {
            EventManager.sharedInstance().register(this);
            _isRegistered = true;
        }

        boolean ok = false;

        CommonMethods.Log("open serial port---");

        if (mDeviceState == DEVICE_DISCONNECTED)
            return false;

        if (mDeviceState == DEVICE_NOTCONNECTED) {
            BleManager.sharedInstance().connectPeripheral(_peripheral);
            return true;
        }
        else if (mDeviceState == DEVICE_CONNECTING ||
                mDeviceState == DEVICE_CONNECTED ||
                mDeviceState == DEVICE_OPENED) {
            return true;
        }

        return ok;
    }

    public void close() {
        BleManager.sharedInstance().disconnectPeripheral(_peripheral);

        if (_isRegistered) {
            EventManager.sharedInstance().unregister(this);
            _isRegistered = false;
        }

        if (mDeviceState == DEVICE_CONNECTING ||
                mDeviceState == DEVICE_CONNECTED ||
                mDeviceState == DEVICE_OPENED) {
            Logger.log(TAG, "disconnect() mDeviceState(%d) => BAND_DISCONNECTED", mDeviceState);
            mDeviceState = DEVICE_DISCONNECTED;
        }
    }

    public void setDeviceState(int deviceState) {
        this.mDeviceState = deviceState;
        EventManager.sharedInstance().post(kSerialPortDataUpdatedNotification, this);
    }

    public String name()
    {
        return _peripheral.name();
    }

    public boolean write(byte[] data) {
        boolean ok = false;

        if (data == null || data.length == 0) {
            CommonMethods.Log("serial port write error, data == null or data.length == 0");
            return false;
        }

        if (mDeviceState == DEVICE_OPENED) {
            if (data.length <= SP_MAX_WRITE_SIZE) {
                if (_serialPortVersion == 1) {
                    _peripheral.writeCharacteristic(_fifoCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, data);
                } else {
                    _peripheral.writeCharacteristic(_fifoCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, data);
                }
                ok = true;
            }
        }

        return ok;
    }
}
