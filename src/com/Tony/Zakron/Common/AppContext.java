package com.Tony.Zakron.Common;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/25/14
 * Time: 2:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class AppContext {
    private static final String APP_SHARED_PREFS = "ControlApp";
    private SharedPreferences shared_preferences;
    private SharedPreferences.Editor shared_preferences_editor;

    private static  AppContext _instance = null;
    private Context _context = null;

    private AppContext(Context context) {
        this.shared_preferences = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        this.shared_preferences_editor = shared_preferences.edit();
        _context = context;
    }

    public static void initialize(Context context) {
        if (_instance == null)
            _instance = new AppContext(context);
    }
}
