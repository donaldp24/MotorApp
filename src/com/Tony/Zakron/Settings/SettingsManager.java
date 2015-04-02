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

    public static final int DEFAULT_POSITIONERROR = 0xFA0; // 4000
    public static final int DEFAULT_KP = 0x3E8; // 1000
    public static final int DEFAULT_KD = 0x1388; // 5000
    public static final int DEFAULT_KI = 0x32; // 50
    public static final int DEFAULT_INTEGRATIONLIMIT = 0xC8; // 200
    public static final int DEFAULT_OUTPUTLIMIT = 0xFF; // 255
    public static final int DEFAULT_CURRENTLIMIT = 0x35; // 53
    public static final int DEFAULT_AMPDEADBAND = 0;
    public static final int DEFAULT_SERVORATE = 1;
    public static final int DEFAULT_SYSTEMVELOCITY = 1500000;
    public static final int DEFAULT_SYSTEMACCELERATION = 1000;
    public static final int DEFAULT_ENCODERCOUNTS = 200;

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

    public int getPositionError() {
        return _sharedPreferences.getInt("positionerror", DEFAULT_POSITIONERROR);
    }
    public void setPositionError(int positionError) {
        _editor.putInt("positionerror", positionError);
        _editor.commit();
    }

    public int getKP() {
        return _sharedPreferences.getInt("kp", DEFAULT_KP);
    }

    public void setKP(int kp) {
        _editor.putInt("kp", kp);
        _editor.commit();
    }

    public int getKD() {
        return _sharedPreferences.getInt("kd", DEFAULT_KD);
    }

    public void setKD(int kd) {
        _editor.putInt("kd", kd);
        _editor.commit();
    }

    public int getKI() {
        return _sharedPreferences.getInt("ki", DEFAULT_KI);
    }

    public void setKI(int ki) {
        _editor.putInt("ki", ki);
        _editor.commit();
    }

    public int getIntegrationLimit() {
        return _sharedPreferences.getInt("integrationlimit", DEFAULT_INTEGRATIONLIMIT);
    }

    public void setIntegrationLimit(int integrationLimit) {
        _editor.putInt("integrationlimit", integrationLimit);
        _editor.commit();
    }

    public int getOutputLimit() {
        return  _sharedPreferences.getInt("outputlimit", DEFAULT_OUTPUTLIMIT);
    }

    public void setOutputLimit(int outputLimit) {
        _editor.putInt("outputlimit", outputLimit);
        _editor.commit();
    }

    public int getCurrentLimit() {
        return _sharedPreferences.getInt("currentlimit", DEFAULT_CURRENTLIMIT);
    }

    public void setCurrentLimit(int currentLimit) {
        _editor.putInt("currentlimit", currentLimit);
        _editor.commit();
    }

    public int getAmpDeadBand() {
        return _sharedPreferences.getInt("ampdeadband", DEFAULT_AMPDEADBAND);
    }

    public void setAmpDeadBand(int ampDeadBand) {
        _editor.putInt("ampdeadband", ampDeadBand);
        _editor.commit();
    }

    public int getServoRate() {
        return _sharedPreferences.getInt("servorate", DEFAULT_SERVORATE);
    }

    public void setServoRate(int servoRate) {
        _editor.putInt("servorate", servoRate);
        _editor.commit();
    }

    public int getSystemVelocity() {
        return _sharedPreferences.getInt("systemvelocity", DEFAULT_SYSTEMVELOCITY);
    }

    public void setSystemVelocity(int systemVelocity) {
        _editor.putInt("systemvelocity", systemVelocity);
        _editor.commit();
    }

    public int getSystemAcceleration() {
        return _sharedPreferences.getInt("systemacceleration", DEFAULT_SYSTEMACCELERATION);
    }

    public void setSystemAcceleration(int systemAcceleration) {
        _editor.putInt("systemacceleration", systemAcceleration);
        _editor.commit();
    }

    public int getEncoderCounts() {
        return _sharedPreferences.getInt("encodercounts", DEFAULT_ENCODERCOUNTS);
    }

    public void setEncoderCounts(int encoderCounts) {
        _editor.putInt("encodercounts", encoderCounts);
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
