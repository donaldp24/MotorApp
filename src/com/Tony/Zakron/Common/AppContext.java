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
    // singleton
    private static AppContext _sharedData;
    public static Context _mainContext;
    public static Activity _currentActivity;
    public static BluetoothAdapter _bluetoothAdapter;

    private static final String APP_SHARED_PREFS = "ControlApp";
    private SharedPreferences shared_preferences;
    private SharedPreferences.Editor shared_preferences_editor;

    private AppContext(Context context) {
        this.shared_preferences = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        this.shared_preferences_editor = shared_preferences.edit();
    }

    public static void initialize(Context context) {
        if (_sharedData == null) {
            _sharedData = new AppContext(context);

            // load settings;
            _sharedData.loadInitData();
        }
    }

    public static AppContext sharedData() {
        return _sharedData;
    }

    /*
     * load properties from preferences
     */
    private void loadInitData() {
        /*settingTemp = shared_preferences.getInt(KEY_TEMPERATURE, TEMP_FAHRENHEIT);
        settingArea = shared_preferences.getInt(KEY_AREA, AREA_FEET);
        settingDateFormat = shared_preferences.getString(KEY_DATEFORMAT, DATEFORMAT_US);
        /*
        isSaved = shared_preferences.getBoolean(KEY_ISSAVED, false);
        selectedJobID = shared_preferences.getLong(KEY_JOBID, 0);
        selectedLocID = shared_preferences.getLong(KEY_LOCID, 0);
        selectedLocProductID = shared_preferences.getLong(KEY_LOCPRODUCTID, 0);

        this.resetSavedData();
        */
    }
}
