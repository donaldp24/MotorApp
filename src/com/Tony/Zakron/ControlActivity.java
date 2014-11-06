package com.Tony.Zakron;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.Tony.Zakron.BLE.ConnectionStatusChangeDelegate;
import com.Tony.Zakron.BLE.SerialPort;
import com.Tony.Zakron.BLE.SerialPortDelegate;
import com.Tony.Zakron.Common.CommonMethods;
import com.Tony.Zakron.Motor.Motor;
import com.Tony.Zakron.Motor.MotorControl;
import com.Tony.Zakron.Motor.MotorControlListener;

import java.util.ArrayList;
import android.bluetooth.BluetoothDevice;

/**
 * Created with IntelliJ IDEA.
 * User: donal_000
 * Date: 8/25/14
 * Time: 3:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ControlActivity extends Activity implements ConnectionStatusChangeDelegate, DeviceScanListener, MotorControlListener {

    private RelativeLayout mainLayout;
    private boolean bInitialized = false;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView lblDeviceName = null;
    private TextView lblStatus = null;
    private TextView lblAbsolutePosition = null;
    private EditText editNewPosition = null;
    
    private Button btnOpen = null;
    private Button btnInit = null;
    private Button btnClose = null;
    private Button btnGoto = null;
    
    private SerialPort _serialPort = null;
    private MotorControl _motorControl = null;
    private Motor _motor = null;
    
    private DeviceScanManager _scanManager = null;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mainLayout = (RelativeLayout)findViewById(R.id.RLMain);
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if (bInitialized == false) {
                            Rect r = new Rect();
                            mainLayout.getLocalVisibleRect(r);
                            ResolutionSet._instance.setResolution(r.width(), r.height(), true);
                            ResolutionSet._instance.iterateChild(findViewById(R.id.RLMain));
                            bInitialized = true;
                        }
          
                    }
                }
        );

        lblDeviceName = (TextView)findViewById(R.id.lblDeviceName);
        lblStatus = (TextView)findViewById(R.id.lblStatus);

        btnOpen = (Button)findViewById(R.id.btnOpen);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_motorControl != null && _serialPort != null)
                {
                    _motor = new Motor();
                    _motor._address = 0;

                    _serialPort.open();
                    btnOpen.setEnabled(false);
                }
            }
        });
        
        btnOpen.setEnabled(false);

        btnInit = (Button)findViewById(R.id.btnInit);
        btnInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_motorControl != null && _serialPort != null && _motor != null)
                {
                    _motorControl.initMotor(_motor);
                    btnInit.setEnabled(false);
                }
            }
        });
        
        btnInit.setEnabled(false);

        btnClose = (Button)findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_motorControl != null && _serialPort != null && _motor != null)
                {
                    //_motorControl.close(_motor);
                    _serialPort.close();
                }
            }
        });
        
        btnClose.setEnabled(false);
        
        btnGoto = (Button)findViewById(R.id.btnGo);
        btnGoto.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (_motorControl != null && _serialPort != null && _motor != null)
				{
					String value = editNewPosition.getText().toString();
					/*
					double inch = Double.parseDouble(value);
					final long positionValue = _motorControl.inchToPositionValue(inch);
					*/
					final long positionValue = Long.parseLong(value);
					new Thread(new Runnable () {
						@Override
						public void run() {
							_motorControl.moveMotor(_motor, positionValue);
						}
					}).start();
				}
			}
		});
        
        btnGoto.setEnabled(false);
        
        lblAbsolutePosition = (TextView)findViewById(R.id.lblAbsolutePosition);
        
        editNewPosition = (EditText)findViewById(R.id.txtNewPosition);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (_serialPort == null)
        {
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        else
        {
            if (_serialPort._connectionState == SerialPort.STATE_CONNECTED) {
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);

                menu.findItem(R.id.menu_refresh).setActionView(null);
            }
            else if (_serialPort._connectionState == SerialPort.STATE_CONNECTING) {
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);


                menu.findItem(R.id.menu_refresh).setActionView(
                        R.layout.actionbar_indeterminate_progress);
            }
            else {
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);

                menu.findItem(R.id.menu_refresh).setActionView(null);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_scan:
                final Intent intent = new Intent(this, DeviceScanActivity.class);
                startActivityForResult(intent, 1);
                return true;
            case R.id.menu_connect:
                _serialPort.connect();
                //invalidateOptionsMenu();
                return true;
            case R.id.menu_disconnect:
                _serialPort.disconnect();
                return true;
            //case android.R.id.home:
            //    onBackPressed();
            //    return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onScanned(ArrayList<BluetoothDevice> devices)
    {
    	if (devices.size() <= 0)
    		return;
    	BluetoothDevice device = devices.get(0);
    	if (devices.size() > 1)
    		device = devices.get(1);
    	
    	String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        CommonMethods.Log("device selected : %s - %s", deviceName, deviceAddress);

        lblDeviceName.setText(deviceName);

        if (_serialPort != null && _serialPort._deviceAddress.equals(deviceAddress))
        {
            // ignore selection
        }
        else
        {
            // create motor control object
            _motorControl = new MotorControl(this);
            _motorControl.main();

            // create new serial port
            _serialPort = new SerialPort(deviceAddress, null, this);
            _serialPort.connect();
        }

        setStatusWithConnectionStatus(_serialPort._connectionState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                //
            }
            else
            {
                String deviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
                String deviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);

                CommonMethods.Log("device selected : %s - %s", deviceName, deviceAddress);

                lblDeviceName.setText(deviceName);

                if (_serialPort != null && _serialPort._deviceAddress.equals(deviceAddress))
                {
                    // ignore selection
                }
                else
                {
                    // create motor control object
                    _motorControl = new MotorControl(this);
                    _motorControl.main();

                    // create new serial port
                    _serialPort = new SerialPort(deviceAddress, null, this);
                    _serialPort.connect();
                }

                setStatusWithConnectionStatus(_serialPort._connectionState);
            }
            invalidateOptionsMenu();
        }
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	
    	//if (_scanManager == null)
    	//{
    	//	_scanManager = new DeviceScanManager(this);
    	//	_scanManager.startScan();
    	//}
    }

    private void setStatusWithConnectionStatus(int connectionStatus) {
        switch(connectionStatus) {
            case SerialPort.STATE_CONNECTED:
                lblStatus.setText(R.string.connected);
                break;
            case SerialPort.STATE_DISCONNECTED:
                lblStatus.setText(R.string.disconnected);
                break;
            case SerialPort.STATE_CONNECTING:
                lblStatus.setText(R.string.connecting);
                break;
            default:
                lblStatus.setText(R.string.unknown_status);
                break;
        }
    }

    @Override
    public void didConnectWithError(SerialPort sp, Error error) {
        //To change body of implemented methods use File | Settings | File Templates.
        if (error != null)
        {
            CommonMethods.Log("didConnectWithError : %s", error.getMessage());
        }
        else
        {
            CommonMethods.Log("didConnectWithError : connect success!");
            
            

            if (_motorControl == null)
                _motorControl = new MotorControl(this);

            _motorControl._serialPort = sp;
            sp._delegate = _motorControl;
            _serialPort = sp;
            /*
            if (sp.open() == false)
            {
                CommonMethods.Log("open serial port failed");
            }
            */
            
            
        }
        

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setStatusWithConnectionStatus(_motorControl._serialPort._connectionState);
                
                	btnOpen.setEnabled(true);
            }
        });
    }

    @Override
    public void didDisconnectWithError(SerialPort sp, Error error) {
        //To change body of implemented methods use File | Settings | File Templates.
        if (error != null)
        {
            CommonMethods.Log("didDisconnectWithError : %s", error.getMessage());
        }
        else
        {
            CommonMethods.Log("didDisconnectWithError : device disconnected");
        }

        if (_motorControl != null)
        {
            _motorControl._serialPort = null;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	if (_motorControl == null || _motorControl._serialPort == null)
            		setStatusWithConnectionStatus(SerialPort.STATE_DISCONNECTED);
            	else
            		setStatusWithConnectionStatus(_motorControl._serialPort._connectionState);
            }
        });
    }

	@Override
	public void onPortOpened(boolean success) {
		// TODO Auto-generated method stub
		if (success)
		{
			runOnUiThread(new Runnable() {
	            @Override
	            public void run() {
	            	lblAbsolutePosition.setText("0");            		
            		btnInit.setEnabled(true);
	            }
	        });
		}
	}

	@Override
	public void onMotorInited(Motor motor) {
		// TODO Auto-generated method stub
		
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	//initMotor(_motor);
        		//moveMotor(_motor, 1000000);
        		long positionValue = _motorControl.getMotoPosition(_motor);
        		//double inch = _motorControl.positionValueToInch(positionValue);
        		//lblAbsolutePosition.setText(String.format("%.2f", inch));
        		lblAbsolutePosition.setText(String.format("%d", positionValue));
        		btnGoto.setEnabled(true);
            }
        });
	}

	@Override
	public void onReceivedStatusPacket(Motor motor) {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	//initMotor(_motor);
        		//moveMotor(_motor, 1000000);
        		long positionValue = _motorControl.getMotoPosition(_motor);
        		int ps = (int)positionValue;
        		positionValue = (ps & 0xFFFFFFFF);
        		//double inch = _motorControl.positionValueToInch(positionValue);
        		//lblAbsolutePosition.setText(String.format("%.2f", inch));
        		Log.v("motorapp", String.format("positionvalue : %d", positionValue));
        		lblAbsolutePosition.setText(String.format("%d", positionValue));
            }
        });
	}
	
	
}