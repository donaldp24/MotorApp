package com.Tony.Zakron.Motor;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/27/14
 * Time: 9:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Motor {
    public static final byte STATUS_OFF = 0;
    public static final byte STATUS_ON = 1;

    public byte _address = 0;
    public byte _status = STATUS_OFF;
    public long _gain[] = new long[9];
    public long _systemVelocity = 0;
    public long _systemAcceleration = 0;

    public byte _buffer[] = new byte[16];
}
