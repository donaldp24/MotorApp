package com.Tony.Zakron.BLE;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/25/14
 * Time: 4:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SerialPortDelegate {

    public static enum SPEvent
    {
        SP_EVT_OPEN(0),
        SP_EVT_CLOSED(1);
        private int value;
        private SPEvent(int value) {
            this.value = value;
        }
    }

    public void portEvent(SerialPort serialPort, SPEvent ev, int error);
    public void writeComplete(SerialPort serialPort, int err);
    public void receivedData(SerialPort serialPort, byte[] data);
}
