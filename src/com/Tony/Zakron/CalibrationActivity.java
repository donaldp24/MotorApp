package com.Tony.Zakron;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import com.Tony.Zakron.Motor.CalibrationManager;
import com.Tony.Zakron.event.EventManager;
import com.Tony.Zakron.event.SEvent;

/**
 * Created by donal_000 on 3/29/2015.
 */
public class CalibrationActivity extends BaseActivity implements View.OnClickListener {

    private TextView textActualPosition;

    private Switch switchMotorEnable;
    private ImageView ivCalibrated1;
    private ImageView ivCalibrated2;
    private ImageView ivLimitSwitch1;
    private ImageView ivLimitSwitch2;

    private TextView textStatus;

    private View llBtnGo;
    private View llBtnStop;

    private CalibrationManager _calibrationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        textActualPosition = (TextView)findViewById(R.id.textActualPosition);

        switchMotorEnable = (Switch)findViewById(R.id.switchMotorEnable);
        ivCalibrated1 = (ImageView)findViewById(R.id.ivCalibrated1);
        ivCalibrated2 = (ImageView)findViewById(R.id.ivCalibrated2);
        ivLimitSwitch1 = (ImageView)findViewById(R.id.ivLimitSwitch1);
        ivLimitSwitch2 = (ImageView)findViewById(R.id.ivLimitSwitch2);

        textStatus = (TextView)findViewById(R.id.textStatus);

        llBtnGo = findViewById(R.id.llBtnGo);
        llBtnStop = findViewById(R.id.llBtnStop);

        llBtnGo.setOnClickListener(this);
        llBtnStop.setOnClickListener(this);

        EventManager.sharedInstance().register(this);

        llBtnGo.setEnabled(true);
        llBtnStop.setEnabled(false);
    }

    @Override
    public void onDestroy() {
        EventManager.sharedInstance().unregister(this);
        super.onDestroy();
    }

    public void onEventMainThread(SEvent e) {
        if (EventManager.isEvent(e, CalibrationManager.EVENT_STATE_UPDATED)) {
            String msg = (String)e.object;
            textStatus.setText(msg);
        }
        else if (EventManager.isEvent(e, CalibrationManager.EVENT_STATE_DATA)) {
            CalibrationManager.CalibratingData data = (CalibrationManager.CalibratingData)e.object;
            if (data.isCalibrated)
                ivCalibrated1.setImageResource(R.drawable.radio_selected);
            if (data.limit1)
                ivLimitSwitch1.setImageResource(R.drawable.radio_selected);
            if (data.limit2)
                ivLimitSwitch2.setImageResource(R.drawable.radio_selected);
        }
    }

    @Override
    public void onClick(View v) {
        if (llBtnGo == v) {
            onGoClicked();
        }
        else if (llBtnStop == v) {
            onStopClicked();
        }
    }

    private void onGoClicked() {
        ivLimitSwitch1.setImageResource(R.drawable.radio_normal);
        ivLimitSwitch2.setImageResource(R.drawable.radio_normal);
        ivCalibrated1.setImageResource(R.drawable.radio_normal);
        ivCalibrated2.setImageResource(R.drawable.radio_normal);

        MotorApplication application = (MotorApplication)getApplication();
        _calibrationManager = new CalibrationManager(this, application.getMotor());
        _calibrationManager.startCalibration();
        llBtnGo.setEnabled(false);
        llBtnStop.setEnabled(true);
    }

    private void onStopClicked() {
        _calibrationManager.stopCalibration();

        llBtnGo.setEnabled(true);
        llBtnStop.setEnabled(false);
    }
}