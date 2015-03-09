package com.Tony.Zakron.event;

import android.content.Context;
import de.greenrobot.event.EventBus;

/**
 * Created by donal_000 on 1/12/2015.
 */
public class EventManager {
    public static EventManager _instance;
    protected Context _context;

    public static EventManager initalize(Context context) {
        if (_instance == null)
            _instance = new EventManager(context);
        return _instance;
    }

    public static EventManager sharedInstance() {
        return _instance;
    }

    private EventManager(Context context) {
        _context = context;
    }

    public void post(String name) {
        EventBus.getDefault().post(new SEvent(name));
    }

    public void post(String name, Object object) {
        EventBus.getDefault().post(new SEvent(name, object));
    }

    public void post(String name, Object object, String msg) {
        EventBus.getDefault().post(new SEvent(name, object, msg));
    }

    public void register(Object objSubscriber) {
        EventBus.getDefault().register(objSubscriber);
    }

    public void unregister(Object objSubscriber) {
        EventBus.getDefault().unregister(objSubscriber);
    }

    public static boolean isEvent(SEvent e, String name) {
        if (name.equals(e.name))
            return true;
        return false;
    }
}
