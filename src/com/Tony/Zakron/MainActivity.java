package com.Tony.Zakron;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.Tony.Zakron.Motor.Motor;
import com.Tony.Zakron.event.EventManager;
import com.Tony.Zakron.event.SEvent;

/**
 * Created by xiaoxue on 3/9/2015.
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private View llBluetooth;
    private View llCalibrate;
    private View llProgram;
    private View llSettings;

    private Motor _motor;
    private Object _dlg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getActionBar().setTitle(getString(R.string.title_mainmenu));

        llBluetooth = findViewById(R.id.llBluetooth);
        llCalibrate = findViewById(R.id.llCalibrate);
        llProgram = findViewById(R.id.llProgram);
        llSettings = findViewById(R.id.llSettings);

        llBluetooth.setOnClickListener(this);
        llCalibrate.setOnClickListener(this);
        llProgram.setOnClickListener(this);
        llSettings.setOnClickListener(this);

        EventManager.sharedInstance().register(this);

        _motor = new Motor(((MotorApplication)getApplication()).getSerialPort());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventManager.sharedInstance().unregister(this);
    }

    @Override
    public void onClick(View v) {
        if (llBluetooth == v) {
            onBluetoothClicked();
        }
        else if (llCalibrate == v) {
            onCaliabrateClicked();
        }
        else if (llProgram == v) {
            onProgramClicked();
        }
        else if (llSettings == v) {
            onSettingsClicked();
        }
    }

    protected void onBluetoothClicked() {
    }

    protected void onCaliabrateClicked() {
        Intent intent = new Intent(this, CalibrationActivity.class);
        startActivity(intent);
    }

    protected void onProgramClicked() {
        //
    }

    protected void onSettingsClicked() {
        //
    }

    public void onEventMainThread(SEvent e) {
        if (EventManager.isEvent(e, Motor.kMotorConnectedNotification)) {
            _dlg = UIManager.sharedInstance().showProgressDialog(this, null, "Initializing motor...", true);

            _motor.initMotor();
        }
        else if (EventManager.isEvent(e, Motor.kMotorDisconnectedNotification)) {
            if (_dlg != null) {
                UIManager.sharedInstance().dismissProgressDialog(_dlg);
                _dlg = null;
            }
        }
        else if (EventManager.isEvent(e, Motor.kMotorInitedNotification)) {
            if (_dlg != null) {
                UIManager.sharedInstance().dismissProgressDialog(_dlg);
                _dlg = null;
            }

            UIManager.sharedInstance().showToastMessage(this, "Motor initialized successfully!");
        }
    }
}