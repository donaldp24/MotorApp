package com.Tony.Zakron.event;

/**
 * Created by donal_000 on 12/1/2014.
 */
public class SEvent {
    public String name;
    public Object object;
    public String msg;
    public SEvent(String name) {
        this.name = name;
        this.object = null;
    }

    public SEvent(String name, Object object) {
        this.name = name;
        this.object = object;
    }

    public SEvent(String name, Object object, String msg) {
        this.name = name;
        this.object = object;
        this.msg = msg;
    }

    public static final String EVENT_BLUETOOTH_STATE_CHANGED = "bluetooth adapter's state changed";
    public static final String EVENT_NETWORK_STATE_CHANGED = "network adapter's state changed";
}
