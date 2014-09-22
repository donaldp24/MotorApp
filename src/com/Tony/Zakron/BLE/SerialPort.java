package com.Tony.Zakron.BLE;

import android.bluetooth.*;
import com.Tony.Zakron.Common.AppContext;
import com.Tony.Zakron.Common.CommonMethods;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/25/14
 * Time: 4:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class SerialPort {

    // If no ACK is used for writing, a "CoreBluetooth[ERROR] XPC connection interrupted, resetting" may occur
    // especially for full duplex data

    // Configuration of write with or without response.
    // -1 Write without response
    // 1 Write with response
    // n every n write is with response and all other writes are without
    public static final int SP_WRITE_WITH_RESPONSE_CNT = -1;

    // Delay in seconds of write complete when writing without response
    public static final float SP_WRITE_COMPLETE_TIMEOUT = 0.0f;

    // Max packets the remote device may transmit without getting more credits
    public static final int SP_MAX_CREDITS = 10;

    public static final int SP_CURRENT_SERIAL_PORT_VERSION = 2;

    public static final int SERIAL_PORT_SERVICE_UUID_LEN = 16;
    public static final int CHARACT_UUID_SERIAL_LEN = 16;
    
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

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



    public enum SPState {
        SP_S_CLOSED(0),
        SP_S_WAIT_SERVICE_SEARCH(1),
        SP_S_WAIT_CHARACT_SEARCH(2),
        SP_S_WAIT_INITIAL_TX_CREDITS(3),
        SP_S_OPEN(4),
        SP_S_ERROR(5);
        private int value;
        private SPState(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    public enum SPStateTx {
        SP_S_TX_IDLE(0),
        SP_S_TX_IN_PROGRESS(1);
        private int value;
        private SPStateTx(int value) {
            this.value = value;
        }
        public int getValue() {
        	return this.value;
        }
    }

    public static final int SP_MAX_WRITE_SIZE = 20;

    public SerialPortDelegate _delegate = null;
    public ConnectionStatusChangeDelegate _connDelegate = null;

    public BluetoothDevice _device = null;
    public String _deviceAddress = null;
    public int _rssi = 0;
    public int _connectionState = STATE_DISCONNECTED;

    private BluetoothGatt _bluetoothGatt;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    // public memebers
    public boolean _isOpen;
    //public boolean _isWriting;
    //public String _name;

    // private members
    private SPState             _state;
    private SPStateTx           _stateTx;

    private int                 _nRxCredits;
    private byte[]             _dataRxCredits;

    private int                 _serialPortVersion;

    private int                 _nTxCredits;
    private int                 _nTxCnt;

    private byte[]              _pendingData;
    private boolean             _pendingCredits;

    private byte[]              _disconnectCredit;

    BluetoothGattService          _service;
    BluetoothGattCharacteristic   _creditsCharacteristic;
    BluetoothGattCharacteristic   _fifoCharacteristic;

    BluetoothGattService          _deviceIdService;
    BluetoothGattCharacteristic     _fwVersionCharacteristic;
    BluetoothGattCharacteristic     _modelNumberCharacteristic;
    
    ConcurrentLinkedQueue<MsgItem>	_queue;
    boolean _finishOperation;
    
    Thread _threadOperation = new Thread(new Runnable() {
    	@Override
    	public void run() {
    		while(!_finishOperation)
    		{
    			MsgItem item = _queue.poll();
    			if (item != null)
    				doOperation(item);
				else
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		}
    	}
    });

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _bluetoothGatt = gatt;
                _connectionState = STATE_CONNECTED;

                MsgItem item = MsgItem.onConnectItem(true);
                _queue.offer(item);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                _connectionState = STATE_DISCONNECTED;
                MsgItem item = MsgItem.onDisconnectedItem();
                _queue.offer(item);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	
            if (status == BluetoothGatt.GATT_SUCCESS) {
                MsgItem item = MsgItem.onServiceDiscoveredItem(true);
                _queue.offer(item);
            } else {
            	MsgItem item = MsgItem.onServiceDiscoveredItem(false);
                _queue.offer(item);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	MsgItem item = MsgItem.onCharacteristicReadItem(characteristic, true);
            	_queue.offer(item);
            }
            else {
            	MsgItem item = MsgItem.onCharacteristicReadItem(characteristic, false);
            	_queue.offer(item);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	MsgItem item = MsgItem.onCharacteristicChangedItem(characteristic, true);
        	_queue.offer(item);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
        	
        	if (status == BluetoothGatt.GATT_SUCCESS)
        	{
        		MsgItem item = MsgItem.onCharacteristicWriteItem(characteristic, true);
        		_queue.offer(item);
        	}
        	else
        	{
        		MsgItem item = MsgItem.onCharacteristicWriteItem(characteristic, false);
        		_queue.offer(item);
        	}
        }
        
        @Override
        public void onDescriptorRead (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
        	if (status == BluetoothGatt.GATT_SUCCESS)
        	{
        		MsgItem item = MsgItem.onDescriptorReadItem(descriptor.getCharacteristic(), true);
        		_queue.offer(item);
        	}
        	else
        	{
        		MsgItem item = MsgItem.onDescriptorReadItem(descriptor.getCharacteristic(), false);
        		_queue.offer(item);
        	}
        }
        
        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
        	if (status == BluetoothGatt.GATT_SUCCESS)
        	{
        		MsgItem item = MsgItem.onDescriptorWriteItem(descriptor.getCharacteristic(), true);
        		_queue.offer(item);
        	}
        	else
        	{
        		MsgItem item = MsgItem.onDescriptorWriteItem(descriptor.getCharacteristic(), false);
        		_queue.offer(item);
        	}
        }
    };

    public SerialPort(String deviceAddress, SerialPortDelegate delegate, ConnectionStatusChangeDelegate connDelegate) {
        BluetoothDevice device = AppContext._bluetoothAdapter.getRemoteDevice(deviceAddress);
        initialize(device, delegate, connDelegate);
    }

    public SerialPort(BluetoothDevice device, SerialPortDelegate delegate, ConnectionStatusChangeDelegate connDelegate) {
        initialize(device, delegate, connDelegate);
    }

    private void initialize(BluetoothDevice device, SerialPortDelegate delegate, ConnectionStatusChangeDelegate connDelegate) {
        _device = device;
        _deviceAddress = _device.getAddress();
        _delegate = delegate;
        _connDelegate = connDelegate;

        _serialPortVersion = 0;

        _dataRxCredits = new byte[1];
        _dataRxCredits[0] = SP_MAX_CREDITS;
        _disconnectCredit = new byte[]{(byte)0xFF};

        _isOpen = false;

        _state = SPState.SP_S_CLOSED;
        _stateTx = SPStateTx.SP_S_TX_IDLE;
        
        _queue = new ConcurrentLinkedQueue<SerialPort.MsgItem>();
        _finishOperation = false;
        _threadOperation.start();
    }

    public int getVersionFromString(String sVersion)
    {
        String arr[] = sVersion.split("\\.");

        int major = Integer.parseInt(arr[0]);
        int minor = Integer.parseInt(arr[1]);

        String arr2[] = arr[2].split(" ");

        int subminor = Integer.parseInt(arr2[0]);

        return ((major << 16) | (minor << 8) | (subminor));
    }

    public int getSerialPortVersion()
    {
   	
        int version = 0;

        // Version of different models that implement the current serial port service version
        String sOLS = "OLS";
        int vOLS = 0x00010100;

        String sOLP = "OLP";
        int vOLP = 0x00010200;

        String sOBS421 = "OBS421";
        int vOBS421 = 0x00050100;

        if((_modelNumberCharacteristic != null) && (_fwVersionCharacteristic != null) &&
                (_modelNumberCharacteristic.getValue() != null) && (_fwVersionCharacteristic.getValue() != null))
        {
            String sModel = null;
            try {
                sModel = new String(_modelNumberCharacteristic.getValue(), "utf8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            String sFwVersion = null;
            try {
                sFwVersion = new String(_fwVersionCharacteristic.getValue(), "utf8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            int fwVersion = getVersionFromString(sFwVersion);

            if(sModel.contains(sOBS421) && (fwVersion < vOBS421))
            {
                version = 1;
            }
            else if(sModel.contains(sOLS) && (fwVersion < vOLS))
            {
                version = 1;
            }
            else if(sModel.contains(sOLP) && (fwVersion < vOLP))
            {
                version = 1;
            }
            else
            {
                version = SP_CURRENT_SERIAL_PORT_VERSION;
            }
        }

        return version;
    	
    	//return SP_CURRENT_SERIAL_PORT_VERSION;
    }

    public void initServicesAndCharacteristics()
    {
        _service = null;
        _fifoCharacteristic = null;
        _creditsCharacteristic = null;

        _deviceIdService = null;
        _modelNumberCharacteristic = null;
        _fwVersionCharacteristic = null;

        // get service & characteristics
        if (_bluetoothGatt != null)
        {
        	List<BluetoothGattService> services = _bluetoothGatt.getServices();
        	if (services.size() > 0)
        	{
		        _service = _bluetoothGatt.getService(CommonMethods.toUUID(serialPortServiceUuid));
		        if (_service != null)
		        {
		        	List<BluetoothGattCharacteristic> characteristics = _service.getCharacteristics();
		        	if (characteristics.size() > 0)
		        	{
			            _fifoCharacteristic = _service.getCharacteristic(CommonMethods.toUUID(serialPortFifoCharactUuid));
			            _creditsCharacteristic = _service.getCharacteristic(CommonMethods.toUUID(creditsCharactUuid));
		        	}
		        }
		
		        _deviceIdService = _bluetoothGatt.getService(CommonMethods.toUUID(deviceIdServiceUuid));
		        if (_deviceIdService != null)
		        {
		        	List<BluetoothGattCharacteristic> characteristics = _service.getCharacteristics();
		        	if (characteristics.size() > 0)
		        	{
			            _modelNumberCharacteristic = _deviceIdService.getCharacteristic(CommonMethods.toUUID(modelNumberCharactUuid[0], modelNumberCharactUuid[1]));
			            _fwVersionCharacteristic = _deviceIdService.getCharacteristic(CommonMethods.toUUID(firmwareRevisionCharactUuid[0], firmwareRevisionCharactUuid[1]));
		        	}
		        }
        	}
        }
    }

    public void openSerialPortService()
    {
		// TODO Auto-generated method stub
		CommonMethods.Log("openSerialPortService ---");
        _serialPortVersion = getSerialPortVersion();
        
        CommonMethods.Log("serialPortVersion = %d, changing state to SP_S_WAIT_INITIAL_TX_CREDITS", _serialPortVersion);

        _state = SPState.SP_S_WAIT_INITIAL_TX_CREDITS;

        if(_serialPortVersion == 1)
        {
        	MsgItem item = MsgItem.setNotificationItem(_fifoCharacteristic, true);
        	_queue.offer(item);
        	item = MsgItem.setNotificationItem(_creditsCharacteristic, true);
            _queue.offer(item);
        }
        else
        {
        	MsgItem item = MsgItem.setNotificationItem(_creditsCharacteristic, true);
        	_queue.offer(item);
            item = MsgItem.setNotificationItem(_fifoCharacteristic, true);
            _queue.offer(item);
        }
        
        MsgItem item = MsgItem.writeItem(_creditsCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, _dataRxCredits);
        _queue.offer(item);
    }

    public void discoverServices()
    {
        _bluetoothGatt.discoverServices();
    }

    public void discoverCharacteristics()
    {
    	// discover for service
    	List<BluetoothGattCharacteristic> characteristics = _service.getCharacteristics();
    	CommonMethods.Log("discoverCharacteristics - service - characteristics count : %d", characteristics.size());
    	
    	if (characteristics.size() > 0)
        {
            _fifoCharacteristic = _service.getCharacteristic(CommonMethods.toUUID(serialPortFifoCharactUuid));
            _creditsCharacteristic = _service.getCharacteristic(CommonMethods.toUUID(creditsCharactUuid));
        }
    	
        if (_fifoCharacteristic == null)
        {
        	CommonMethods.Log("discoverCharacteristics _fifo == null");
        }
        if (_creditsCharacteristic == null)
        {
        	CommonMethods.Log("discoverCharacteristics _credit == null");
        }
    	
        // discover for deviceIdService
        characteristics = _deviceIdService.getCharacteristics();
    	CommonMethods.Log("discoverCharacteristics - deviceIdService - characteristics count : %d", characteristics.size());
    	if (characteristics.size() > 0)
    	{
            _modelNumberCharacteristic = _deviceIdService.getCharacteristic(CommonMethods.toUUID(modelNumberCharactUuid[0], modelNumberCharactUuid[1]));
            _fwVersionCharacteristic = _deviceIdService.getCharacteristic(CommonMethods.toUUID(firmwareRevisionCharactUuid[0], firmwareRevisionCharactUuid[1]));
    	}
    	
    	if (_fwVersionCharacteristic == null)
        {
        	CommonMethods.Log("discoverCharacteristics _fwVersionCharacteristic == null");
        }
        else
        {
        	MsgItem item = MsgItem.readItem(_fwVersionCharacteristic);
        	_queue.offer(item);
        }
    	
        if (_modelNumberCharacteristic == null)
        {
        	CommonMethods.Log("discoverCharacteristics modelnumber == null");
        }
        else
        {
        	MsgItem item = MsgItem.readItem(_modelNumberCharacteristic);
        	_queue.offer(item);
        }
        
        if ((_fifoCharacteristic != null) &&
        		(_creditsCharacteristic != null) &&
        		((_creditsCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) &&
        		(_modelNumberCharacteristic != null) &&
        		(_modelNumberCharacteristic.getValue() != null) &&
        		(_fwVersionCharacteristic != null) &&
        		(_fwVersionCharacteristic.getValue() != null))
        {
        	CommonMethods.Log("discoverCharacteristics calling openSerialPortService()");
        	
        	openSerialPortService();
        }
        else
        {
        	CommonMethods.Log("discoverCharacteristics not called openSerialPortService() == null");
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect() {

        if (_device == null) {
            CommonMethods.Log("Device not found.  Unable to connect.");
            return false;
        }

        if (AppContext._bluetoothAdapter == null || _device == null) {
            CommonMethods.Log("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (_bluetoothGatt != null) {
            CommonMethods.Log("Trying to use an existing mBluetoothGatt for connection.");
            if (_bluetoothGatt.connect()) {
                _connectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        _bluetoothGatt = _device.connectGatt(AppContext._mainContext, true, mGattCallback);
        CommonMethods.Log("Trying to create a new connection.");
        _connectionState = STATE_CONNECTING;
        return true;
    }

    public boolean connect(String deviceAddress) {
        BluetoothDevice device = AppContext._bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null)
        {
            CommonMethods.Log("Device not found. Unable to connect. %s", deviceAddress);
            return false;
        }
        _device = device;
        return connect();
    }

    public boolean disconnect() {
        close();
        if (_bluetoothGatt != null)
            _bluetoothGatt.disconnect();
        return true;
    }

    public boolean open() {

        boolean ok = false;

        CommonMethods.Log("open serial port---");
        if(_connectionState == STATE_CONNECTED)
        {
            //unsigned char *p = (unsigned char*)dataRxCredits.bytes;

            _nRxCredits = (int)(_dataRxCredits[0]);
            _nTxCredits = 0;
            //peripheral.delegate = self;
            _pendingData = null;
            _pendingCredits = false;

            initServicesAndCharacteristics();

            if((_service == null) || (_deviceIdService == null))
            {
                _state = SPState.SP_S_WAIT_SERVICE_SEARCH;

                discoverServices();
            }
            else if((_fifoCharacteristic == null) ||
                    (_creditsCharacteristic == null) ||
                    (_fwVersionCharacteristic == null) ||
                    (_modelNumberCharacteristic == null))
            {
                _state = SPState.SP_S_WAIT_CHARACT_SEARCH;

                discoverCharacteristics();
            }
            else if((_fwVersionCharacteristic.getValue() == null) || (_modelNumberCharacteristic.getValue() == null))
            {
                _state = SPState.SP_S_WAIT_CHARACT_SEARCH;

                _bluetoothGatt.readCharacteristic(_modelNumberCharacteristic);
                _bluetoothGatt.readCharacteristic(_fwVersionCharacteristic);
            }
            else
            {
                openSerialPortService();
            }

            ok = true;
        }

        return ok;
    }

    public void close() {
        _isOpen = false;
        _state = SPState.SP_S_CLOSED;

        if(_connectionState == STATE_CONNECTED)
        {
            if(_serialPortVersion == 1)
            {
                if(_creditsCharacteristic != null)
                {
                	MsgItem item = MsgItem.setNotificationItem(_creditsCharacteristic, false);
                    _queue.offer(item);
                }
                if(_fifoCharacteristic != null)
                {
                	MsgItem item = MsgItem.setNotificationItem(_fifoCharacteristic, false);
                    _queue.offer(item);
                }
            }
            else
            {
                if (_creditsCharacteristic != null)
                {
                	MsgItem item = MsgItem.writeItem(_creditsCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, _disconnectCredit);
                    _queue.offer(item);
                }
            }
        }

        //peripheral.delegate = nil;
    }


    public String name()
    {
        return _device.getName();
    }

    public boolean isWriting()
    {
        boolean res = false;

        if( (_state == SPState.SP_S_OPEN) && (_stateTx != SPStateTx.SP_S_TX_IDLE))
        {
            res = true;
        }

        return res;
    }

    public void writeCompleteSelector()
    {
        _delegate.writeComplete(SerialPort.this, 0);
    }

    public boolean write(byte[] data) {
        boolean ok = false;

        if (data == null || data.length == 0)
        {
            CommonMethods.Log("serial port write error, data == null or data.length == 0");
            return false;
        }

        if (_connectionState == STATE_CONNECTED && _state == SPState.SP_S_OPEN)
        {
            if(data.length <= SP_MAX_WRITE_SIZE)
            {
                if((_nTxCredits > 0) && (_stateTx == SPStateTx.SP_S_TX_IDLE))
                {
                    _nTxCredits--;
                    _nTxCnt++;

                    if((_nTxCnt < SP_WRITE_WITH_RESPONSE_CNT) || (_nTxCnt == -1) || (_serialPortVersion == 1))
                    {
                    	MsgItem item = MsgItem.writeItem(_fifoCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, data);
                        _queue.offer(item);
                        writeCompleteSelector();
                    }
                    else
                    {
                    	MsgItem item = MsgItem.writeItem(_fifoCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, data);
                    	_queue.offer(item);

                        _nTxCnt = 0;

                        _stateTx = SPStateTx.SP_S_TX_IN_PROGRESS;
                    }
                }
                else
                {
                    //NSAssert2(pendingData == nil, @"%s, %d", __FILE__, __LINE__);
                    if (_pendingData != null)
                    {
                        CommonMethods.Log("serial port write error : pending data is not null");
                    }

                    _pendingData = data;
                }

                ok = true;
            }
        }

        return ok;
    }

    public void didUpdateValueForCharacteristic(BluetoothGattCharacteristic ch, Error error)
    {
    	CommonMethods.Log("didUpdateValueForCharacteristic - stat ");
        switch (_state)
        {
            case SP_S_WAIT_CHARACT_SEARCH:
            	CommonMethods.Log("SP_S_WAIT_CHARACT_SEARCH - credit property : %d", (_creditsCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY));
            	if (_modelNumberCharacteristic.getValue() == null)
            	{
            		CommonMethods.Log("_modelNumberCharacteristic.getValue() == null");
            	}
            	if (_fwVersionCharacteristic.getValue() == null)
            	{
            		CommonMethods.Log("_fwVersionCharacteristic.getValue() == null");
            	}
            	
                if((_fifoCharacteristic != null) &&
                        (_creditsCharacteristic != null) &&
                        ((_creditsCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) &&
                        (_modelNumberCharacteristic != null) && (_modelNumberCharacteristic.getValue() != null) &&
                        (_fwVersionCharacteristic != null) && (_fwVersionCharacteristic.getValue() != null))
                {
                	CommonMethods.Log("calling openSerialPortService on didUpdateValueForCharacteristics");
                    openSerialPortService();
                }
                else
                {
                	CommonMethods.Log("openSerialPortService not called on didUpdateValueForCharacteristics");
                }
                break;

            case SP_S_WAIT_INITIAL_TX_CREDITS:
            	CommonMethods.Log("didUpdateValueForCharacteristics --- SP_S_WAIT_INITIAL_TX_CREDITS");
                if( (ch == _creditsCharacteristic) && (ch.getValue().length == 1))
                {
                    byte []p = ch.getValue();

                    _nTxCredits += (int)(p[0]);
                    _nTxCnt = 0;

                    _isOpen = true;

                    _state = SPState.SP_S_OPEN;
                    _stateTx = SPStateTx.SP_S_TX_IDLE;

                    _delegate.portEvent(SerialPort.this, SerialPortDelegate.SPEvent.SP_EVT_OPEN, 0);
                }
                else
                {
                	CommonMethods.Log("(ch == _creditsCharacteristic) && (ch.getValue().length == 1) not true");
                }
                break;

            case SP_S_OPEN:
            	CommonMethods.Log("didUpdateValueForCharacteristics --- SP_S_OPEN");
                if( (ch == _creditsCharacteristic) && (ch.getValue().length == 1))
                {
                	CommonMethods.Log("(ch == _creditsCharacteristic) && (ch.getValue().length == 1)");
                	
                    byte []p = ch.getValue();

                    _nTxCredits += (int)(p[0]);

                    if( (_nTxCredits > 0) && (_stateTx == SPStateTx.SP_S_TX_IDLE) && (_pendingData != null))
                    {
                        _nTxCredits--;

                        _nTxCnt++;

                        if((_nTxCnt < SP_WRITE_WITH_RESPONSE_CNT) || (_nTxCnt == -1) || (_serialPortVersion == 1))
                        {
                        	MsgItem item = MsgItem.writeItem(_fifoCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, _pendingData);
                        	_queue.offer(item);

                            _pendingData = null;

                            //[self performSelector:@selector(writeCompleteSelector) withObject:nil afterDelay:SP_WRITE_COMPLETE_TIMEOUT];
                            writeCompleteSelector();
                        }
                        else
                        {
                        	MsgItem item = MsgItem.writeItem(_fifoCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, _pendingData);
                        	_queue.offer(item);

                            _pendingData = null;

                            _nTxCnt = 0;

                            _stateTx = SPStateTx.SP_S_TX_IN_PROGRESS;
                        }
                    }
                }
                else if(ch == _fifoCharacteristic)
                {
                    _delegate.receivedData(SerialPort.this, _fifoCharacteristic.getValue());

                    _nRxCredits--;

                    //NSLog(@"Rx: Credits %d", nRxCredits);

                    //if(FALSE)
                    if(_nRxCredits == 0)
                    {
                        byte []p = _dataRxCredits;

                        //if(TRUE)
                        if(_stateTx == SPStateTx.SP_S_TX_IDLE)
                        {
                            _nRxCredits = (int)(p[0]);

                            if(_serialPortVersion == 1)
                            {
                            	MsgItem item = MsgItem.writeItem(_creditsCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, _dataRxCredits);
                            	_queue.offer(item);
                            }
                            else
                            {
                            	MsgItem item = MsgItem.writeItem(_creditsCharacteristic, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, _dataRxCredits);
                            	_queue.offer(item);

                                _stateTx = SPStateTx.SP_S_TX_IN_PROGRESS;
                            }

                            //NSLog(@"New Credits: Credits %d", nRxCredits);
                        }
                        else
                        {
                            _pendingCredits = true;
                        }
                    }
                }
                break;

            default:
                break;
        }

    }
    
    public boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, boolean enabled)
    {
    	boolean ret = bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    	if (ret == false)
    		return ret;
    	BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
    	if (descriptor == null)
    		return false;
    	descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    	return bluetoothGatt.writeDescriptor(descriptor);
    }
    
    
    private void doOperation(MsgItem item)
    {
    	boolean success = false;
    	if (item == null)
    		return;
    	switch(item.iOperation)
    	{
    	case MsgItem.ON_CONNECTED:
    		if (_connDelegate != null)
                _connDelegate.didConnectWithError(SerialPort.this, null);

            CommonMethods.Log("Connected to GATT server.");
            break;
            
    	case MsgItem.ON_DISCONNECTED:
            if (_connDelegate != null)
                _connDelegate.didDisconnectWithError(SerialPort.this, null);
            
            CommonMethods.Log("Disconnected from GATT server.");
    		break;
    		
    	case MsgItem.ON_SERVICE_DISCOVERED:
    		
    		if (item.value[0] == 1)
    			success = true;
    		else
    			success = false;
    		if (success)
    		{
	        	_service = null;
	        	_deviceIdService = null;
	        	
	            // get service & characteristics
	        	List<BluetoothGattService> services = _bluetoothGatt.getServices();
	        	CommonMethods.Log("onServiceDiscovered : count - %d", services.size());
	        	
	        	if (services.size() > 0)
	        	{
	        		_service = _bluetoothGatt.getService(CommonMethods.toUUID(serialPortServiceUuid));
	        		_deviceIdService = _bluetoothGatt.getService(CommonMethods.toUUID(deviceIdServiceUuid[0], deviceIdServiceUuid[1]));
	        	}
	        	
	            if (_service != null)
	            {
	            	CommonMethods.Log("onServicesDiscovered serialPortService discovered");
	            }
	            else
	            {
	            	CommonMethods.Log("onServicesDiscovered serialPortService == null");
	            }
	
	            
	            if (_deviceIdService != null)
	            {
	            	CommonMethods.Log("onServicesDiscovered deviceIdService discovered");
	            }
	            else
	            {
	            	CommonMethods.Log("onServicesDiscovered _deviceIdService == null");
	            }
	
	            if((_service != null) && (_deviceIdService != null))
	            {
	            	CommonMethods.Log("onServicesDiscovered state changing to SP_S_WAIT_CHARCT_SEARCH -- discovering characteristics");
	            	
	                _state = SPState.SP_S_WAIT_CHARACT_SEARCH;
	                discoverCharacteristics();
	            }
	            else
	            {
	            	CommonMethods.Log("onServicesDiscovered service == null or deviceIdService == null");
	            	
	                _state = SPState.SP_S_ERROR;
	                _delegate.portEvent(SerialPort.this, SerialPortDelegate.SPEvent.SP_EVT_OPEN, -1);
	            }
	
	            CommonMethods.Log("onServicesDiscovered end-  --------------- ");
    		}
    		else
    		{
    			CommonMethods.Log("onServicesDiscovered false");
    		}
    		break;
    		
    	case MsgItem.ON_CHARACTERISTIC_READ:
    		if (item.value[0] == 1)
    			success = true;
    		else
    			success = false;
    		if (success)
    		{
	    		 CommonMethods.Log("onCharacteristicRead ---------");
	             didUpdateValueForCharacteristic(item.characteristic, null);
    		}
    		else
    		{
    			CommonMethods.Log("onCharacteristicRead failed !");
    		}
    		break;
    		
    	case MsgItem.ON_CHARACTERISTIC_CHANGED:
    		CommonMethods.Log("onCharacteristicChanged ---------");
            didUpdateValueForCharacteristic(item.characteristic, null);
    		break;
    		
    	case MsgItem.ON_CHARACTERISTIC_WRITE:
    		CommonMethods.Log("onCharacteristicWrite ---------");
    		if (item.value[0] == 1)
    			success = true;
    		else
    			success = false;
    		if (success)
    		{
	        	/*
	
	            if((characteristic == _creditsCharacteristic) || (characteristic == _fifoCharacteristic))
	            {
	            	if (_stateTx != SPStateTx.SP_S_TX_IN_PROGRESS){
	            		CommonMethods.Log("_stattx != SPStateTx.SP_S_TX_IN_PROGRESS");
	            	}
	                //NSAssert2(stateTx == SP_S_TX_IN_PROGRESS, @"%s, %d", __FILE__, __LINE__);
	
	                _stateTx = SPStateTx.SP_S_TX_IDLE;
	
	                if(_pendingCredits == true)
	                {
	                    byte[] p = _dataRxCredits;
	
	                    _nRxCredits = (int)(p[0]);
	
	                    if(_serialPortVersion == 1)
	                    {
	                        _creditsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
	                        _creditsCharacteristic.setValue(_dataRxCredits);
	                        _bluetoothGatt.writeCharacteristic(_creditsCharacteristic);
	                    }
	                    else
	                    {
	                        _creditsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
	                        _creditsCharacteristic.setValue(_dataRxCredits);
	                        _bluetoothGatt.writeCharacteristic(_creditsCharacteristic);
	
	                        _stateTx = SPStateTx.SP_S_TX_IN_PROGRESS;
	                    }
	
	                    _pendingCredits = false;
	                }
	                else if( (_nTxCredits > 0) && (_pendingData != null))
	                {
	                    _nTxCredits--;
	
	                    _nTxCnt++;
	
	                    if((_nTxCnt < SP_WRITE_WITH_RESPONSE_CNT) || (_nTxCnt == -1) || (_serialPortVersion == 1))
	                    {
	                        _fifoCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
	                        _fifoCharacteristic.setValue(_pendingData);
	                        _bluetoothGatt.writeCharacteristic(_fifoCharacteristic);
	
	                        _pendingData = null;
	
	                        //[self performSelector:@selector(writeCompleteSelector) withObject:nil afterDelay:SP_WRITE_COMPLETE_TIMEOUT];
	                        writeCompleteSelector();
	                    }
	                    else
	                    {
	                        _fifoCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
	                        _fifoCharacteristic.setValue(_pendingData);
	                        _bluetoothGatt.writeCharacteristic(_fifoCharacteristic);
	
	                        _pendingData = null;
	                        _nTxCnt = 0;
	
	                        _stateTx = SPStateTx.SP_S_TX_IN_PROGRESS;
	                    }
	                }
	
	                if(characteristic == _fifoCharacteristic)
	                {
	                    if(status == BluetoothGatt.GATT_SUCCESS)
	                        _delegate.writeComplete(SerialPort.this, 0);
	                    else
	                        _delegate.writeComplete(SerialPort.this, -1);
	                }
	            }
	        	 */
    		}
    		else
    		{
    			//
    		}
    		break;
    	case MsgItem.ON_DESCRIPTOR_READ:
    		if (item.value[0] == 1)
    			success = true;
    		else
    			success = false;
    		if (success)
        	{
        		CommonMethods.Log("onDescriptorRead-- success");
        	}
        	else
        	{
        		CommonMethods.Log("onDescriptorRead-- failed");
        	}
    		break;
    		
    	case MsgItem.ON_DESCRIPTOR_WRITE:
    		if (item.value[0] == 1)
    			success = true;
    		else
    			success = false;
    		if (success)
        	{
        		CommonMethods.Log("onDescriptorWrite-- success");
        	}
        	else
        	{
        		CommonMethods.Log("onDescriptorWrite-- failed");
        	}
    		break;
    		
    	case MsgItem.READ_CHARACTERISTIC:
    		_bluetoothGatt.readCharacteristic(item.characteristic);
    		break;
    		
    	case MsgItem.WRITE_CHARACTERISTIC:
    		int writeType = item.value[0];
    		byte value[] = new byte[item.value.length - 1];
    		System.arraycopy(item.value, 1, value, 0, item.value.length - 1);
    		item.characteristic.setValue(value);
    		item.characteristic.setWriteType(writeType);
    		_bluetoothGatt.writeCharacteristic(item.characteristic);
    		break;
    		
    	case MsgItem.SET_CHARACTERNOTIFICATION:
    		boolean enabled = false;
    		if (item.value[0] == 1)
    			enabled = true;
    		else
    			enabled = false;
    		setCharacteristicNotification(_bluetoothGatt, item.characteristic, enabled);
    		break;
    		
		default:
			break;
    	}
    }
    
    private static class MsgItem
    {
    	public byte value[];
    	public int iOperation;
    	public BluetoothGattCharacteristic characteristic;
    	
    	// constants
    	public static final int ON_CONNECTED = 0;
    	public static final int ON_DISCONNECTED = 1;
    	public static final int ON_SERVICE_DISCOVERED = 2;
    	public static final int ON_CHARACTERISTIC_READ = 3;
    	public static final int ON_CHARACTERISTIC_WRITE = 4;
    	public static final int ON_CHARACTERISTIC_CHANGED = 5;
    	public static final int ON_DESCRIPTOR_WRITE = 6;
    	public static final int ON_DESCRIPTOR_READ = 7;
    	public static final int READ_CHARACTERISTIC = 8;
    	public static final int WRITE_CHARACTERISTIC = 9;
    	public static final int SET_CHARACTERNOTIFICATION = 10;
    
		public static MsgItem readItem(BluetoothGattCharacteristic characteristic)
    	{
    		MsgItem item = new MsgItem();
    		item.value = null;
    		item.iOperation = READ_CHARACTERISTIC;
    		item.characteristic = characteristic;
    		
    		return item;
    	}
    	
    	public static MsgItem writeItem(BluetoothGattCharacteristic characteristic, int writeType, byte[] value)
    	{
    		MsgItem item = new MsgItem();
    		item.value = new byte[value.length + 1];
    		item.value[0] = (byte)writeType;
    		System.arraycopy(value, 0, item.value, 1, value.length);
    		item.iOperation = WRITE_CHARACTERISTIC;
    		item.characteristic = characteristic;
    		
    		return item;
    	}
    	
    	public static MsgItem setNotificationItem(BluetoothGattCharacteristic characteristic, boolean enabled)
    	{
    		MsgItem item = new MsgItem();
    		item.value = new byte[1];
    		if (enabled)
    			item.value[0] = 1;
    		else
    			item.value[0] = 0;
    		item.iOperation = SET_CHARACTERNOTIFICATION;
    		item.characteristic = characteristic;
    		
    		return item;
    	}
    	
    	public static MsgItem onConnectItem(boolean success)
    	{
    		MsgItem item = new MsgItem();
    		item.value = new byte[1];
    		if (success)
    			item.value[0] = 1;
    		else
    			item.value[0] = 0;
    		item.iOperation = ON_CONNECTED;
    		item.characteristic = null;
    		
    		return item;
    	}
    	
    	public static MsgItem onDisconnectedItem()
    	{
    		MsgItem item = new MsgItem();
    		item.iOperation = ON_DISCONNECTED;
    		item.characteristic = null;
    		
    		return item;
    	}
    	
    	public static MsgItem onServiceDiscoveredItem(boolean success)
    	{
    		MsgItem item = new MsgItem();
    		item.value = new byte[1];
    		if (success)
    			item.value[0] = 1;
    		else
    			item.value[0] = 0;
    		item.iOperation = ON_SERVICE_DISCOVERED;
    		item.characteristic = null;
    		
    		return item;
    	}
    	
    	public static MsgItem onCharacteristicReadItem(BluetoothGattCharacteristic characteristic, boolean success)
    	{
    		MsgItem item = new MsgItem();
    		if (success)
    		{
    			item.value = new byte[1 + characteristic.getValue().length];
    			item.value[0] = 1;
    			System.arraycopy(characteristic.getValue(), 0, item.value, 1, characteristic.getValue().length);
    		}
    		else
    		{
    			item.value = new byte[1];
    		}
    		
    		item.iOperation = ON_CHARACTERISTIC_READ;
    		return item;
    	}
    	
    	public static MsgItem onCharacteristicChangedItem(BluetoothGattCharacteristic characteristic, boolean success)
    	{
    		MsgItem item = new MsgItem();
    		if (success)
    		{
    			item.value = new byte[1 + characteristic.getValue().length];
    			item.value[0] = 1;
    			System.arraycopy(characteristic.getValue(), 0, item.value, 1, characteristic.getValue().length);
    		}
    		else
    		{
    			item.value = new byte[1];
    		}
    		
    		item.iOperation = ON_CHARACTERISTIC_CHANGED;
    		return item;
    	} 
    	
    	public static MsgItem onCharacteristicWriteItem(BluetoothGattCharacteristic characteristic, boolean success)
    	{
    		MsgItem item = new MsgItem();
    		if (success)
    		{
    			item.value[0] = 1;
    		}
    		else
    		{
    			item.value = new byte[1];
    		}
    		
    		item.iOperation = ON_CHARACTERISTIC_WRITE;
    		return item;
    	}
    	
    	public static MsgItem onDescriptorReadItem(BluetoothGattCharacteristic characteristic, boolean success)
    	{
    		MsgItem item = new MsgItem();
    		if (success)
    		{
    			item.value[0] = 1;
    		}
    		else
    		{
    			item.value = new byte[1];
    		}
    		
    		item.iOperation = ON_DESCRIPTOR_READ;
    		return item;
    	}
    	
    	public static MsgItem onDescriptorWriteItem(BluetoothGattCharacteristic characteristic, boolean success)
    	{
    		MsgItem item = new MsgItem();
    		if (success)
    		{
    			item.value[0] = 1;
    		}
    		else
    		{
    			item.value = new byte[1];
    		}
    		
    		item.iOperation = ON_DESCRIPTOR_WRITE;
    		return item;
    	}
    }
}
