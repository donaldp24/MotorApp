package com.Tony.Zakron.BLE;

import android.bluetooth.*;
import com.Tony.Zakron.Common.AppContext;
import com.Tony.Zakron.Common.CommonMethods;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                _bluetoothGatt = gatt;
                _connectionState = STATE_CONNECTED;

                if (_connDelegate != null)
                    _connDelegate.didConnectWithError(SerialPort.this, null);

                CommonMethods.Log("Connected to GATT server.");

                // Attempts to discover services after successful connection.
                //CommonMethods.Log("Attempting to start service discovery:" + _bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //intentAction = ACTION_GATT_DISCONNECTED;
                _connectionState = STATE_DISCONNECTED;
                CommonMethods.Log("Disconnected from GATT server.");
                //broadcastUpdate(intentAction);
                if (_connDelegate != null)
                    _connDelegate.didDisconnectWithError(SerialPort.this, null);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            	_service = null;
            	
                // get service & characteristics
            	List<BluetoothGattService> services = gatt.getServices();
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

            } else {
                CommonMethods.Log("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	 CommonMethods.Log("onCharacteristicRead ---------");
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                 didUpdateValueForCharacteristic(characteristic, null);
            }
            else {
                CommonMethods.Log("onCharacteristicRead failed : %d", status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	CommonMethods.Log("onCharacteristicChanged ---------");
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            didUpdateValueForCharacteristic(characteristic, null);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
        	
        	CommonMethods.Log("onCharacteristicWrite ---------");
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
        
        @Override
        public void onDescriptorRead (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
        	if (status == BluetoothGatt.GATT_SUCCESS)
        	{
        		CommonMethods.Log("onDescriptorRead-- success");
        	}
        	else
        	{
        		CommonMethods.Log("onDescriptorRead-- failed");
        	}
        	if (descriptor.getCharacteristic() == _creditsCharacteristic)
        	{
        		//
        	}
        }
        
        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
        	if (status == BluetoothGatt.GATT_SUCCESS)
        	{
        		CommonMethods.Log("onDescriptorWrite-- success");
        	}
        	else
        	{
        		CommonMethods.Log("onDescriptorWrite-- failed");
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
    	Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				CommonMethods.Log("openSerialPortService ---");
		        _serialPortVersion = getSerialPortVersion();
		        
		        CommonMethods.Log("serialPortVersion = %d, changing state to SP_S_WAIT_INITIAL_TX_CREDITS", _serialPortVersion);

		        _state = SPState.SP_S_WAIT_INITIAL_TX_CREDITS;

		        if(_serialPortVersion == 1)
		        {
		            _bluetoothGatt.setCharacteristicNotification(_fifoCharacteristic, true);
		            _bluetoothGatt.setCharacteristicNotification(_creditsCharacteristic, true);
		        }
		        else
		        {
		        	
		            // Current version
		        	_bluetoothGatt.setCharacteristicNotification(_creditsCharacteristic, false);
		            BluetoothGattDescriptor descriptor = _creditsCharacteristic.getDescriptor(
		                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
		            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		            _bluetoothGatt.writeDescriptor(descriptor);
		            try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            
		            _bluetoothGatt.setCharacteristicNotification(_creditsCharacteristic, true);
		            descriptor = _creditsCharacteristic.getDescriptor(
		                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
		            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		            _bluetoothGatt.writeDescriptor(descriptor);
		            
		            try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            
		            _bluetoothGatt.setCharacteristicNotification(_fifoCharacteristic, false);
		            descriptor = _fifoCharacteristic.getDescriptor(
		                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
		            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		            _bluetoothGatt.writeDescriptor(descriptor);
		            
		            try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            
		            _bluetoothGatt.setCharacteristicNotification(_fifoCharacteristic, true);
		            descriptor = _fifoCharacteristic.getDescriptor(
		                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
		            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		            _bluetoothGatt.writeDescriptor(descriptor);
		            try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        }
		        
		        boolean bval = _creditsCharacteristic.setValue(_dataRxCredits);
		        if (bval == false)
		        {
		        	CommonMethods.Log("failed to setValue - _creditsCharactersitic");
		        }
		        _creditsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		        boolean bret = _bluetoothGatt.writeCharacteristic(_creditsCharacteristic);
		        if (bret == false)
		        {
		        	CommonMethods.Log("failed to write characteristics : _creditsCharacteristic");
		        }
			}
		});
    	
    	thread.start();
    }

    public void discoverServices()
    {
        _bluetoothGatt.discoverServices();
    }

    public void discoverCharacteristics()
    {
    	Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
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
		        	if (_bluetoothGatt.readCharacteristic(_fwVersionCharacteristic) == true)
		        	{
		        		CommonMethods.Log("discoverCharacteristics - readCharacteristics : _fwVersionCharacteristic : success");
		        	}
		        	else
		        	{
		        		CommonMethods.Log("discoverCharacteristics - readCharacteristics : _fwVersionCharacteristic : success");
		        	}
		        }
		    	
		    	try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	
		        if (_modelNumberCharacteristic == null)
		        {
		        	CommonMethods.Log("discoverCharacteristics modelnumber == null");
		        }
		        else
		        {
		        	if (_bluetoothGatt.readCharacteristic(_modelNumberCharacteristic) == true)
		        	{
		        		CommonMethods.Log("discoverCharacteristics - readCharacteristics : modelNumberCharacteristic : success");
		        	}
		        	else
		        	{
		        		CommonMethods.Log("discoverCharacteristics - readCharacteristics : modelNumberCharacteristic : failed");
		        	}
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
		});
    	
    	thread.start();

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
                    _bluetoothGatt.setCharacteristicNotification(_creditsCharacteristic, false);
                if(_fifoCharacteristic != null)
                    _bluetoothGatt.setCharacteristicNotification(_fifoCharacteristic, false);
            }
            else
            {
                if (_creditsCharacteristic != null)
                {
                    _creditsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    _creditsCharacteristic.setValue(_disconnectCredit);
                    _bluetoothGatt.writeCharacteristic(_creditsCharacteristic);
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
                        _fifoCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        _fifoCharacteristic.setValue(data);
                        _bluetoothGatt.writeCharacteristic(_fifoCharacteristic);
                        //[self performSelector:@selector(writeCompleteSelector) withObject:nil afterDelay:SP_WRITE_COMPLETE_TIMEOUT];
                        writeCompleteSelector();
                    }
                    else
                    {
                        _fifoCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        _fifoCharacteristic.setValue(data);
                        _bluetoothGatt.writeCharacteristic(_fifoCharacteristic);
                        //[peripheral writeValue:data forCharacteristic:fifoCharacteristic type:CBCharacteristicWriteWithResponse];

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
}
