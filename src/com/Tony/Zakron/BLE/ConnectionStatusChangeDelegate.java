package com.Tony.Zakron.ble;

import com.Tony.Zakron.ConnectBlue.SerialPort;

/**
 * Created with IntelliJ IDEA.
 * User: donal_000
 * Date: 9/3/14
 * Time: 8:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ConnectionStatusChangeDelegate {
    public void didConnectWithError(SerialPort sp, Error error);
    public void didDisconnectWithError(SerialPort sp, Error error);
}
