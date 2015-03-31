package com.Tony.Zakron.Motor;

import android.content.Context;
import com.Tony.Zakron.event.EventManager;
import com.Tony.Zakron.helper.Logger;

/**
 * Created by donal_000 on 3/29/2015.
 */
public class CalibrationManager {

    public static final String TAG = "CalibrationManager";

    public static final String EVENT_STATE_UPDATED = "calibrationmanager_EVENT_STATE_UPDATED";
    public static final String EVENT_STATE_DATA = "calibrationmanager_EVENT_STATE_DATA";

    private Motor _motor;
    private Context _context;

    private int i;
    private int calFlag;
    private int delta;
    private int delta_A;
    private int visualCount;
    private int limitStatus;
    private int isCalibrated;

    private byte motoStatus;

    public static final int PHASE_NONE = 0;
    public static final int PHASE_CAL1 = 1;
    public static final int PHASE_CAL2 = 2;
    public static final int PHASE_CAL3 = 3;
    public static final int PHASE_CAL4 = 4;
    public static final int PHASE_CAL5 = 5;
    public static final int PHASE_CAL6 = 6;
    public static final int PHASE_IDLE = 7;

    private int _phase = PHASE_NONE;

    private Thread thread = null;
    private boolean _stopped = false;

    public static class CalibratingData {
        public boolean limit1;
        public boolean limit2;
        public boolean isCalibrated;
    }

    public CalibrationManager(Context context, Motor motor) {
        _context = context;
        _motor = motor;

        calFlag = 1;
        delta = 0;
        delta_A = 0;
        visualCount = 0;
        limitStatus = 0;
        isCalibrated = 0;

        motoStatus = motor._status;

        thread = null;
    }

    protected void sendAmuletString(String msg) {
        Logger.log(TAG, "%s", msg);
        EventManager.sharedInstance().post(EVENT_STATE_UPDATED, msg);
    }

    protected void sendStatus() {
        CalibratingData data = new CalibratingData();
        data.limit1 = (_motor.buffer()[0] & Motor.STATBIT_LIMITREAR) == Motor.STATBIT_LIMITREAR;
        data.limit2 = (_motor.buffer()[0] & Motor.STATBIT_LIMITREAR) == Motor.STATBIT_LIMITRAM;
        data.isCalibrated = isCalibrated != 0;

        EventManager.sharedInstance().post(EVENT_STATE_DATA, data);
    }

    public void startCalibration() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!_stopped) {
                    reCal();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    public void stopCalibration() {
        _stopped = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread = null;
    }

    protected void reCal() {

        if (_phase == PHASE_NONE) {
            /*** Send message to Amulet to say we're calibrating ***/
            if (motoStatus == Motor.STATUS_ON) {
                sendAmuletString("Calibrating");
                _motor._systemVelocity = _motor._systemVelocity / Motor.CALSPEEDFACTOR;
            }

            /*** run the motor backwards till rear limit switch opens ***/
            if (motoStatus == Motor.STATUS_ON) {
                _motor.stopMotor();
                _motor.startMotor();
                _motor.setMotoHome(Motor.LIMITREAR);
                _motor.velMotor(Motor.DIRREV);

                visualCount = 0;
            }
            limitStatus = 0;
            _phase = PHASE_CAL1;

            sendStatus();
            Logger.log(TAG, "calibrating - change to PHASE_CAL1");
        }
        else if (_phase == PHASE_CAL1) {
            if ((_motor.buffer()[0] & Motor.STATBIT_LIMITREAR) == 0) // Wait for limit break
            {
                if (motoStatus == Motor.STATUS_ON) {
                    _motor.requestStatusPacket();
                }

                visualCount++;

                if (motoStatus == Motor.STATUS_OFF) {
                    limitStatus |= 0x01;
                }

                if (motoStatus == Motor.STATUS_ON && (limitStatus & 0x01) == 0) {
                    if (visualCount == 25) {
                        sendAmuletString("Calibrating.");
                    }
                    else if (visualCount == 50) {
                        sendAmuletString("Calibrating..");
                    }
                    else if (visualCount == 75) {
                        sendAmuletString("Calibrating...");
                        visualCount = 0;
                    }
                }

                // Turn off the motor that's hit the limit and send a visual message
                if ((_motor.buffer()[0] & Motor.STATBIT_LIMITREAR) != 0 && (limitStatus & 0x01) == 0) {
                    _motor.stopMotor();
                    sendAmuletString("Rear Limit");
                    sendStatus();
                    limitStatus |= 0x01;
                }

                if (limitStatus == 1) {
                    _phase = PHASE_CAL2;
                }
            }
        } // CAL1
        else if (_phase == PHASE_CAL2) {

            // Wait for the motor to stop turning
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /*** Come off Rear Limit ***/
            if (motoStatus == Motor.STATUS_ON) {
                _motor._systemVelocity = _motor._systemVelocity / Motor.CALSPEEDFACTOR;
                sendAmuletString("Indexing");
                _motor.startMotor();
                _motor.setMotoHome(Motor.LIMITREAR);
                _motor.velMotor(Motor.DIRFWD);
                visualCount = 0;
            }

            limitStatus = 0;
            _phase = PHASE_CAL3;
        } //CAL2
        else if (_phase == PHASE_CAL3) {
            if ((_motor.buffer()[0] & Motor.STATBIT_LIMITREAR) == Motor.STATBIT_LIMITREAR) {
                if (motoStatus == Motor.STATUS_ON) {
                    _motor.requestStatusPacket();
                }

                visualCount++;

                if (motoStatus == Motor.STATUS_OFF) {
                    limitStatus |= 0x01;
                }

                if (motoStatus == Motor.STATUS_ON) {
                    if (visualCount == 50) {
                        sendAmuletString("Indexing.");
                    }
                    else if (visualCount == 100) {
                        sendAmuletString("Indexing..");
                    }
                    else if (visualCount == 150) {
                        sendAmuletString("Indexing...");
                        visualCount = 0;
                    }
                }

                // Turn off the motor that's hit the limit
                if ((_motor.buffer()[0] & Motor.STATBIT_LIMITREAR) != Motor.STATBIT_LIMITREAR && (limitStatus & 0x01) == 0) {
                    _motor.stopMotor();
                    limitStatus |= 001;
                    sendStatus();
                }

                // If all the motors are at their limit, get out of the loop and go to the next step
                if (limitStatus == 0x01) {
                    _phase = PHASE_CAL4;
                }
            }
        } // CAL3
        else if (_phase == PHASE_CAL4) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /*** Home on Next Index ***/
            if (motoStatus == Motor.STATUS_ON) {
                _motor.resetMotor();
                _motor.setMotoHome(Motor.LIMITINDEX);
                _motor.velMotor(Motor.DIRFWD);
                visualCount = 0;
            }

            limitStatus = 0;
            _phase = PHASE_CAL5;
        } // CAL4
        else if (_phase == PHASE_CAL5) {

            // Wait for limit break
            if (_motor.getHomePosition() == 0) {
                if (motoStatus == Motor.STATUS_ON) {
                    _motor.requestStatusPacket();
                }

                visualCount++;

                if (motoStatus == Motor.STATUS_OFF) {
                    limitStatus |= 0x01;
                }

                if ((limitStatus & 0x01) == 0) {
                    if (_motor.getHomePosition() > (Motor.ONEREVOLUTION + (Motor.ONEREVOLUTION * 1/4))) // check to see if we've gone too far
                    {
                        visualCount = 5000;
                        _phase = PHASE_CAL6;
                        return;
                    }
                }

                if (motoStatus == Motor.STATUS_ON) {
                    if (visualCount == 25) {
                        sendAmuletString("Indexing.");
                    }
                    else if (visualCount == 50) {
                        sendAmuletString("Indexing..");
                    }
                    else if (visualCount == 75) {
                        sendAmuletString("Indexing...");
                        visualCount = 0;
                    }
                }

                if (_motor.getHomePosition() != 0 && (limitStatus & 0x01) == 0) {
                    _motor.stopMotor();
                    limitStatus |= 0x01;
                    sendStatus();
                }

                if (limitStatus == 0x01) {
                    _phase = PHASE_CAL6;
                }
            }
        } // CAL5
        else if (_phase == PHASE_CAL6) {
            // Double check to make sure we haven't gone too far
            if (_motor.getHomePosition() > (Motor.ONEREVOLUTION + (Motor.ONEREVOLUTION * 1/4))) // check to see if we've gone too far
            {
                visualCount = 5000;
            }
            /*** Reset our motors ***/
            if (motoStatus == Motor.STATUS_ON) {
                _motor.resetMotor(); // Set Motor to 0
            }

            // Set system velocity
            if(motoStatus == Motor.STATUS_ON)
            {
                _motor._systemVelocity = (_motor._systemVelocity * Motor.CALSPEEDFACTOR);
                _motor._systemVelocity = (_motor._systemVelocity * Motor.CALSPEEDFACTOR);
            } // if

            if (motoStatus == Motor.STATUS_ON && visualCount == 5000) {
                sendAmuletString("Error");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                sendAmuletString("Restarting");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                visualCount = 0;
                _phase = PHASE_NONE; //goto reCal;
                return;
            }

            if (motoStatus == Motor.STATUS_ON) {
                _motor.startMotor(); //Servo
                sendAmuletString("Indexed");
                sendStatus();
            }

            try {
                Thread.sleep(100); //FINGERDELAY
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (motoStatus == Motor.STATUS_ON) {
                sendAmuletString("Calibrated");
                isCalibrated |= 0x01;
            }
            else {
                isCalibrated |= 0x01; // If the motor is off, say that it's calibrated
            }
            sendStatus();

            try {
                Thread.sleep(1000); // For visual pleasure
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            calFlag = 0;
            _phase = PHASE_IDLE;
        }
    }
}
