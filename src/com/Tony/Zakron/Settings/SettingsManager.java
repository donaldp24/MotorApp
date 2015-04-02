package com.Tony.Zakron.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by donal_000 on 4/2/2015.
 */
public class SettingsManager {
    private static SettingsManager _instance;
    private Context _context;
    private SharedPreferences _sharedPreferences;
    private SharedPreferences.Editor _editor;

    public static final long DEFAULT_POSITIONERROR = 0xFA0; // 4000
    public static final long DEFAULT_KP = 1500;//0x3E8; // 1000
    public static final long DEFAULT_KD = 5000;//0x1388; // 5000
    public static final long DEFAULT_KI = 200;//0x32; // 50
    public static final long DEFAULT_INTEGRATIONLIMIT = 0;//0xC8; // 200
    public static final long DEFAULT_OUTPUTLIMIT = 0xFF; // 255
    public static final long DEFAULT_CURRENTLIMIT = 0;//0x35; // 53
    public static final long DEFAULT_AMPDEADBAND = 0;
    public static final long DEFAULT_SERVORATE = 1;
    public static final long DEFAULT_SYSTEMVELOCITY = 1500000;
    public static final long DEFAULT_SYSTEMACCELERATION = 1000;
    public static final long DEFAULT_ENCODERCOUNTS = 200;

    public static SettingsManager initialize(Context context) {
        if (_instance == null)
            _instance = new SettingsManager(context);
        return _instance;
    }

    public static SettingsManager sharedInstance() {
        return _instance;
    }

    private SettingsManager(Context context) {
        _context = context;

        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        _editor = _sharedPreferences.edit();
    }

    public long getPositionError() {
        return _sharedPreferences.getLong("positionerror", DEFAULT_POSITIONERROR);
    }
    public void setPositionError(long positionError) {
        _editor.putLong("positionerror", positionError);
        _editor.commit();
    }

    public long getKP() {
        return _sharedPreferences.getLong("kp", DEFAULT_KP);
    }

    public void setKP(long kp) {
        _editor.putLong("kp", kp);
        _editor.commit();
    }

    public long getKD() {
        return _sharedPreferences.getLong("kd", DEFAULT_KD);
    }

    public void setKD(long kd) {
        _editor.putLong("kd", kd);
        _editor.commit();
    }

    public long getKI() {
        return _sharedPreferences.getLong("ki", DEFAULT_KI);
    }

    public void setKI(long ki) {
        _editor.putLong("ki", ki);
        _editor.commit();
    }

    public long getIntegrationLimit() {
        return _sharedPreferences.getLong("integrationlimit", DEFAULT_INTEGRATIONLIMIT);
    }

    public void setIntegrationLimit(long integrationLimit) {
        _editor.putLong("integrationlimit", integrationLimit);
        _editor.commit();
    }

    public long getOutputLimit() {
        return  _sharedPreferences.getLong("outputlimit", DEFAULT_OUTPUTLIMIT);
    }

    public void setOutputLimit(long outputLimit) {
        _editor.putLong("outputlimit", outputLimit);
        _editor.commit();
    }

    public long getCurrentLimit() {
        return _sharedPreferences.getLong("currentlimit", DEFAULT_CURRENTLIMIT);
    }

    public void setCurrentLimit(long currentLimit) {
        _editor.putLong("currentlimit", currentLimit);
        _editor.commit();
    }

    public long getAmpDeadBand() {
        return _sharedPreferences.getLong("ampdeadband", DEFAULT_AMPDEADBAND);
    }

    public void setAmpDeadBand(long ampDeadBand) {
        _editor.putLong("ampdeadband", ampDeadBand);
        _editor.commit();
    }

    public long getServoRate() {
        return _sharedPreferences.getLong("servorate", DEFAULT_SERVORATE);
    }

    public void setServoRate(long servoRate) {
        _editor.putLong("servorate", servoRate);
        _editor.commit();
    }

    public long getSystemVelocity() {
        return _sharedPreferences.getLong("systemvelocity", DEFAULT_SYSTEMVELOCITY);
    }

    public void setSystemVelocity(long systemVelocity) {
        _editor.putLong("systemvelocity", systemVelocity);
        _editor.commit();
    }

    public long getSystemAcceleration() {
        return _sharedPreferences.getLong("systemacceleration", DEFAULT_SYSTEMACCELERATION);
    }

    public void setSystemAcceleration(long systemAcceleration) {
        _editor.putLong("systemacceleration", systemAcceleration);
        _editor.commit();
    }

    public long getEncoderCounts() {
        return _sharedPreferences.getLong("encodercounts", DEFAULT_ENCODERCOUNTS);
    }

    public void setEncoderCounts(long encoderCounts) {
        _editor.putLong("encodercounts", encoderCounts);
        _editor.commit();
    }

    public void reset() {
        setPositionError(DEFAULT_POSITIONERROR);
        setKP(DEFAULT_KP);
        setKD(DEFAULT_KD);
        setKI(DEFAULT_KI);
        setIntegrationLimit(DEFAULT_INTEGRATIONLIMIT);
        setOutputLimit(DEFAULT_OUTPUTLIMIT);
        setCurrentLimit(DEFAULT_CURRENTLIMIT);
        setAmpDeadBand(DEFAULT_AMPDEADBAND);
        setServoRate(DEFAULT_SERVORATE);
        setSystemVelocity(DEFAULT_SYSTEMVELOCITY);
        setSystemAcceleration(DEFAULT_SYSTEMACCELERATION);
        setEncoderCounts(DEFAULT_ENCODERCOUNTS);
    }

}
