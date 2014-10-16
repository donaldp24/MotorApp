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
    public long _gain[] = {0x3E8, 0x1388, 0x32, 0xC8, 0xFF, 0x35, 0xFA0, 0x01, 00};
    public long _systemVelocity = 1500000;
    public long _systemAcceleration = 1000;

    public byte _buffer[] = new byte[16];
}
