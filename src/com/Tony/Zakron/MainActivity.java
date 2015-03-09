package com.Tony.Zakron;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by xiaoxue on 3/9/2015.
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private View llBluetooth;
    private View llCalibrate;
    private View llProgram;
    private View llSettings;

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
    }

    protected void onProgramClicked() {
        //
    }

    protected void onSettingsClicked() {
        //
    }
}