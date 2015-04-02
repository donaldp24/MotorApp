package com.Tony.Zakron;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.Tony.Zakron.Settings.SettingsManager;

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

    private interface SettingValueListener {
        public void onSet(String value);
    }

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

        llEncoderCounts = findViewById(R.id.llEncoderCounts);
        textEncoderCounts = (TextView)findViewById(R.id.textEncoderCounts);

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

        llEncoderCounts.setOnClickListener(this);

        refreshTexts();

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
    }

    private void refreshTexts() {
        textPositionError.setText("" + SettingsManager.sharedInstance().getPositionError());
        textKP.setText("" + SettingsManager.sharedInstance().getKP());
        textKD.setText("" + SettingsManager.sharedInstance().getKD());
        textKI.setText("" + SettingsManager.sharedInstance().getKI());
        textIntegrationLimit.setText("" + SettingsManager.sharedInstance().getIntegrationLimit());
        textOutputLimit.setText("" + SettingsManager.sharedInstance().getOutputLimit());
        textCurrentLimit.setText("" + SettingsManager.sharedInstance().getCurrentLimit());
        textAmpDeadBand.setText("" + SettingsManager.sharedInstance().getAmpDeadBand());
        textServoRate.setText("" + SettingsManager.sharedInstance().getServoRate());
        textSystemVelocity.setText("" + SettingsManager.sharedInstance().getSystemVelocity());
        textSystemAcceleration.setText("" + SettingsManager.sharedInstance().getSystemAcceleration());
        textEncoderCounts.setText("" + SettingsManager.sharedInstance().getEncoderCounts());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        else if (item.getItemId() == R.id.menu_reset) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Confirm")
                    .setMessage("Please confirm to reset parameters in default.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SettingsManager.sharedInstance().reset();
                            refreshTexts();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing.
                        }
                    })
                    .show();

        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (llPositionError == v) {
            onPositionErrorClicked();
        }
        else if (llKP == v) {
            onKPClicked();
        }
        else if (llKD == v) {
            onKDClicked();
        }
        else if (llKI == v) {
            onKIClicked();
        }
        else if (llIntegrationLimit == v) {
            onIntegrationLimitClicked();
        }
        else if (llOutputLimit == v) {
            onOutputLimitClicked();
        }
        else if (llCurrentLimit == v) {
            onCurrentLimitClicked();
        }
        else if (llAmpDeadBand == v) {
            onAmpDeadBandClicked();
        }
        else if (llServoRate == v) {
            onServoRateClicked();
        }
        else if (llSystemVelocity == v) {
            onSystemVelocityClicked();
        }
        else if (llSystemAcceleration == v) {
            onSystemAccelerationClicked();
        }
        else if (llEncoderCounts == v) {
            onEncoderCountsClicked();
        }
    }

    void onPositionErrorClicked() {
        showDialog("Position Error (PE)", "Range: 0 - 65535, default: " + SettingsManager.DEFAULT_POSITIONERROR,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setPositionError(intval);
                        refreshTexts();
                    }
                });
    }

    void onKPClicked() {
        showDialog("KP", "Range: 0 - 65535, default: " + SettingsManager.DEFAULT_KP,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setKP(intval);
                        refreshTexts();
                    }
                });
    }

    void onKDClicked() {
        showDialog("KD", "Range: 0 - 65535, default: " + SettingsManager.DEFAULT_KD,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setKD(intval);
                        refreshTexts();
                    }
                });
    }

    void onKIClicked() {
        showDialog("KI", "Range: 0 - 65535, default: " + SettingsManager.DEFAULT_KI,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setKI(intval);
                        refreshTexts();
                    }
                });
    }

    void onIntegrationLimitClicked() {
        showDialog("Integration Limit (IL)", "Range: 0 - 65535, default: " + SettingsManager.DEFAULT_INTEGRATIONLIMIT,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setIntegrationLimit(intval);
                        refreshTexts();
                    }
                });
    }

    void onOutputLimitClicked() {
        showDialog("Output Limit (OL)", "Range: 0 - 255, default: " + SettingsManager.DEFAULT_OUTPUTLIMIT,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setOutputLimit(intval);
                        refreshTexts();
                    }
                });
    }

    void onCurrentLimitClicked() {
        showDialog("Current Limit (CL)", "Range: 0 - 255, default: " + SettingsManager.DEFAULT_CURRENTLIMIT,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setCurrentLimit(intval);
                        refreshTexts();
                    }
                });
    }

    void onAmpDeadBandClicked() {
        showDialog("Amp DeadBand (Amp)", "Range: 0 - 255, default: " + SettingsManager.DEFAULT_AMPDEADBAND,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setAmpDeadBand(intval);
                        refreshTexts();
                    }
                });
    }

    void onServoRateClicked() {
        showDialog("Servo Rate (SR)", "Range: 0 - 255, default: " + SettingsManager.DEFAULT_SERVORATE,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setServoRate(intval);
                        refreshTexts();
                    }
                });
    }

    void onSystemVelocityClicked() {
        showDialog("System Velocity", "Range: 0 - 2,147,483,647, default: " + SettingsManager.DEFAULT_SYSTEMVELOCITY,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setSystemVelocity(intval);
                        refreshTexts();
                    }
                });
    }

    void onSystemAccelerationClicked() {
        showDialog("System Acceleration", "Range: 0 - 65535, default: " + SettingsManager.DEFAULT_SYSTEMACCELERATION,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setSystemAcceleration(intval);
                        refreshTexts();
                    }
                });
    }

    void onEncoderCountsClicked() {
        showDialog("Encoder Counts", "Range: 0 - 51000, default: " + SettingsManager.DEFAULT_ENCODERCOUNTS,
                new SettingValueListener() {
                    @Override
                    public void onSet(String value) {
                        int intval = Integer.parseInt(value);
                        SettingsManager.sharedInstance().setEncoderCounts(intval);
                        refreshTexts();
                    }
                });
    }

    private void showDialog(String title, String message, final SettingValueListener listener) {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = input.getText().toString();
                        listener.onSet(value);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing.
                    }
                })
                .show();
    }
}