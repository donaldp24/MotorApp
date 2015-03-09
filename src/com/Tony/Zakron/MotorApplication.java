package com.Tony.Zakron;

import android.app.Application;
import com.Tony.Zakron.ConnectBlue.SerialPort;
import com.radiusnetworks.bluetooth.BluetoothCrashResolver;

/**
 * Created by xiaoxue on 3/4/2015.
 */
public class MotorApplication extends Application {
    private BluetoothCrashResolver bluetoothCrashResolver = null;
    private SerialPort _selectedPort;

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothCrashResolver = new BluetoothCrashResolver(this);
        bluetoothCrashResolver.start();
    }

    public BluetoothCrashResolver getBluetoothCrashResolver() {
        return bluetoothCrashResolver;
    }

    public void setSerialPort(SerialPort serialPort) {
        _selectedPort = serialPort;
    }

    public SerialPort getSerialPort() {
        return _selectedPort;
    }

}
