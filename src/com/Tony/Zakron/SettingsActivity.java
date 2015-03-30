package com.Tony.Zakron;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by donal_000 on 3/29/2015.
 */
public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    private View llPositionError;
    private TextView textPositionError;

    private View llKP;
    private TextView textKP;

    private View llKD;
    private TextView textKD;

    private View llKI;
    private TextView textKI;

    private View llIntegrationLimit;
    private TextView textIntegrationLimit;

    private View llOutputLimit;
    private TextView textOutputLimit;

    private View llCurrentLimit;
    private TextView textCurrentLimit;

    private View llAmpDeadBand;
    private TextView textAmpDeadBand;

    private View llServoRate;
    private TextView textServoRate;

    private View llSystemVelocity;
    private TextView textSystemVelocity;

    private View llSystemAcceleration;
    private TextView textSystemAcceleration;

    private View llEncoderCounts;
    private TextView textEncoderCounts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        llPositionError = findViewById(R.id.llPositionError);
        textPositionError = (TextView)findViewById(R.id.textPositionError);

        llKP = findViewById(R.id.llKP);
        textKP = (TextView)findViewById(R.id.textKP);

        llKD = findViewById(R.id.llKD);
        textKD = (TextView)findViewById(R.id.textKD);

        llKI = findViewById(R.id.llKI);
        textKI = (TextView)findViewById(R.id.textKI);

        llIntegrationLimit = findViewById(R.id.llIntegrationLimit);
        textIntegrationLimit = (TextView)findViewById(R.id.textIntegrationLimit);

        llOutputLimit = findViewById(R.id.llOutputLimit);
        textOutputLimit = (TextView)findViewById(R.id.textOutputLimit);

        llCurrentLimit = findViewById(R.id.llCurrentLimit);
        textCurrentLimit = (TextView)findViewById(R.id.textCurrentLimit);

        llAmpDeadBand = findViewById(R.id.llAmpDeadBand);
        textAmpDeadBand = (TextView)findViewById(R.id.textAmpDeadBand);

        llServoRate = findViewById(R.id.llServoRate);
        textServoRate = (TextView)findViewById(R.id.textServoRate);

        llSystemVelocity = findViewById(R.id.llSystemVelocity);
        textSystemVelocity = (TextView)findViewById(R.id.textSystemVelocity);

        llSystemAcceleration = findViewById(R.id.llSystemAcceleration);
        textSystemAcceleration = (TextView)findViewById(R.id.textSystemAcceleration);

        llPositionError.setOnClickListener(this);
        llKP.setOnClickListener(this);
        llKD.setOnClickListener(this);
        llKI.setOnClickListener(this);
        llIntegrationLimit.setOnClickListener(this);
        llOutputLimit.setOnClickListener(this);
        llCurrentLimit.setOnClickListener(this);
        llAmpDeadBand.setOnClickListener(this);
        llServoRate.setOnClickListener(this);

        llSystemVelocity.setOnClickListener(this);
        llSystemAcceleration.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //
    }
}