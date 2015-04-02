package com.Tony.Zakron.Motor;

import android.util.Log;
import com.Tony.Zakron.Common.CommonMethods;
import com.Tony.Zakron.ConnectBlue.SerialPort;
import com.Tony.Zakron.Settings.SettingsManager;
import com.Tony.Zakron.event.EventManager;
import com.Tony.Zakron.event.SEvent;
import com.Tony.Zakron.helper.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/27/14
 * Time: 9:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Motor {
    public static final String TAG = "Motor";

    // status of the motor
    public static final byte STATUS_OFF = 0;
    public static final byte STATUS_ON = 1;

    public static final int MOTOR_NOTCONNECTED = 0;
    public static final int MOTOR_CONNECTED = 1;
    public static final int MOTOR_INITIALIZING = 2;
    public static final int MOTOR_INITED = 3;
    public static final int MOTOR_DISCONNECTED = -1;

    private int mMotorStatus = MOTOR_NOTCONNECTED;

    // movement direction
    public static final byte DIRFWD = 0x00;			// Forward Movement
    public static final byte DIRREV = 0x40;			// Reverse Movement

    public static final byte LIMITREAR = 0x01;		// Limit One Bit (for set home Command)
    public static final byte LIMITRAM = 0x02	;	// Limit Two bit (for set Home Command)
    public static final byte LIMITINDEX = (byte)0x08;		// Index Bit (for setHome Command)

    public static final int CALSPEEDFACTOR = 10;

    // Status Bits etc.
    public static final int STATBIT_DONE = 0x01;	// Move done bit of status Byte
    public static final int STATBIT_LIMITREAR = 0x20;	// Limit1 input Bit of Status Byte
    public static final int STATBIT_LIMITRAM = 0x40; // Limit2 input bit of Status Byte
    public static final int STATBIT_HOME = 0x80;	// Home in progress bit of Status Byte
    public static final int STATBIT_INDEX = 0x01;	// Index bit of Aux Status Byte

    // Time Out limits
    public static final int ONEREVOLUTION = 2000;	// Time out count for our cal sequence

    public static final String kMotorConnectedNotification = "kMotorConnectedNotification";
    public static final String kMotorDisconnectedNotification = "kMotorDisconnectedNotification";
    public static final String kMotorInitedNotification = "kMotorInitedNotification";


    public int packetFlag = 0;
    public byte _address = 0;
    public byte _status = STATUS_OFF;
    public long _gain[] = {0x3E8 /*kp*/, 0x1388 /*kd*/, 0x32 /*ki*/, 0xC8 /*IL*/, 0xFF/*OL*/, 0x35/*CL*/, 0xFA0/*EL*/, 0x01/*SR*/, 00/*DB*/, 01/*SM*/};
    public long _systemVelocity = SettingsManager.DEFAULT_SYSTEMVELOCITY;
    public long _systemAcceleration = SettingsManager.DEFAULT_SYSTEMACCELERATION;

    private byte _commandBuffer[] = new byte[160];
    private byte _commandLength = 0;

    private int statusPacketLength = 14;

    private byte[] _buffer = new byte[160];
    private int _length;

    private byte[] _motorReadingBuffer = new byte[160];
    private int _motorReadingBufferSize = 0;
    private int _newBufferSize = 0;

    private SerialPort _serialPort;

    // last time at received status packet
    private Long lastTimeForStatusPacket = 0L;

    private boolean registered = false;

    public Motor(SerialPort serialPort) {
        _serialPort = serialPort;
        registered = false;

        initialize();
    }

    public void initialize() {
        //_serialPort.initialize();

        _length = 0;
        _commandLength = 0;
        _address = 0;
        if (_serialPort.getDeviceStatus() == SerialPort.DEVICE_CONNECTED) {
            _motorConnected(_serialPort);
        }

        if (!registered) {
            EventManager.sharedInstance().register(this);
            registered = true;
        }

        _systemVelocity = SettingsManager.sharedInstance().getSystemVelocity();
        _systemAcceleration = SettingsManager.sharedInstance().getSystemAcceleration();
    }

    public byte[] buffer() {
        return _buffer;
    }

    public SerialPort serialPort() {
        return _serialPort;
    }

    // Sets the Address of the Motor at the given address to the new address
    public void addressMotor(byte newAddress) {
        byte chkSum;
        chkSum = 0x00;
        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x21, chkSum);
        chkSum = putchc0(newAddress, chkSum);
        chkSum = putchc0((byte)0xFF, chkSum);
        putch0(chkSum);

        flush();

        _address = newAddress;
    }

    // Stops (smoothly) any motion currently in progress
    public void stopMotor() {
        Logger.log(TAG, "stopMotor()");
        byte chkSum;
        chkSum = 0x00;

        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x17, chkSum);
        chkSum = putchc0((byte)0x03, chkSum); // Turn Motor Off (No servo)
        putch0(chkSum);

        flush();

        recieveStatusPacket();
        clearMotoStickyBits();
        /*** Trigger LED Off ***/

        _status = STATUS_OFF;
    }

    //Turns on the amp, sets to stop smoothly, then sets gain
    public void startMotor() {
        Logger.log(TAG, "startMotor()");
        byte chkSum;
        chkSum = 0x00;

        memset(_commandBuffer, (byte)0xFF, 16);
        // Set gain as defined
        setGain();

        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x17, chkSum);
        chkSum = putchc0((byte)0x09, chkSum); // Turns on Pos servo R.Z 103107
        putch0(chkSum);

        flush();

        recieveStatusPacket();
        _status = Motor.STATUS_ON;
    }

    //sends to the picservo all the commands to correctly change the gain
    public void setGain() {
        byte bytes[] = new byte[4];
        byte chkSum;
        chkSum = 0x00;

        packetFlag = 1;
        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0xF6, chkSum); //command byte


        longtobyte(SettingsManager.sharedInstance().getKP(), bytes);
        chkSum = putchc0(bytes[0], chkSum); //kp
        chkSum = putchc0(bytes[1], chkSum);

        longtobyte(SettingsManager.sharedInstance().getKD(), bytes);
        chkSum = putchc0(bytes[0], chkSum); //kd
        chkSum = putchc0(bytes[1], chkSum);

        longtobyte(SettingsManager.sharedInstance().getKI(), bytes);
        chkSum = putchc0(bytes[0], chkSum); //ki
        chkSum = putchc0(bytes[1], chkSum);

        longtobyte(SettingsManager.sharedInstance().getIntegrationLimit(), bytes);
        chkSum = putchc0(bytes[0], chkSum); //integration limit
        chkSum = putchc0(bytes[1], chkSum);

        chkSum = putchc0((byte)(SettingsManager.sharedInstance().getOutputLimit() & 0xFF), chkSum); //output limit

        chkSum = putchc0((byte)(SettingsManager.sharedInstance().getCurrentLimit() & 0xFF), chkSum); //current limit

        longtobyte(SettingsManager.sharedInstance().getPositionError(), bytes);
        chkSum = putchc0(bytes[0], chkSum); //error limit
        chkSum = putchc0(bytes[1], chkSum);

        chkSum = putchc0((byte)(SettingsManager.sharedInstance().getServoRate() & 0xFF), chkSum); //servo limit

        chkSum = putchc0((byte)(SettingsManager.sharedInstance().getAmpDeadBand() & 0xFF), chkSum); //deadband compensation

        chkSum = putchc0 ((byte)(SettingsManager.sharedInstance().getEncoderCounts() / 200), chkSum); // sm

        putch0(chkSum); //CheckSum

        flush();

        packetFlag = 0;
        recieveStatusPacket(); //Retrieve response
    }

    //Turns on the motor's amplifier
    public void enableMotor(Motor motor) {
        //
    }

    //Turns off the amplifier, as well as the position servo
    //stops any current velocity immediately
    public void disableMotor(Motor motor) {
        //
    }

    //Moves Motor to a a given position, with current vel and accel
    //picservo requires the motor to NOT be moving in order to send new instructions
    //note to any programmer, this function does NOT take that into account
    //DO NOT use this WITHOUT MAKING SURE THE MOTOR IS STOPPED
    public void moveMotor(long destPosition) {
        byte chkSum;
        byte tempbytes[] = new byte[4];
        byte controlByte;
        long currentPos;

        /*** We have to decide the fastest route (forwards or backwards, no rounding the horn) ***/
        currentPos = getMotorPosition();

        if((currentPos - destPosition) > 0)// In new PicServoSC it uses this bit for absolute or relative moves
        {
            controlByte = (byte)(0x97 | DIRREV); //control byte 10110110 (we toggle bit 6) We use Absolute only
        }
        else
        {
            controlByte = (byte)(0x97 | DIRFWD); //control byte 10110110 (we toggle bit 6)
        }// if

        //	/*** Prepare and send the command *** /
        controlByte = (byte)0x97; // R.Z. 110107 Keep Bit 6 zero for absolute moves old Pic's didn't use it anyway
        // in Trapezoidal moves. In new PicservoSc it controls Absolute or Relative Moves
        chkSum = 0x00;

        packetFlag = 1;
        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0xD4, chkSum); //command byte
        chkSum = putchc0(controlByte, chkSum); //control byte 10010111

        //send the next four x three bytes (goal position, vel, then acceleration)
        longtobyte(destPosition, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        longtobyte(_systemVelocity, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        longtobyte(_systemAcceleration, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        putch0(chkSum); // CheckSum

        flush();

        packetFlag = 0;
        recieveStatusPacket(); //Retrieve response status packet
        /*** Trigger LED On ***/
    }

    //Velocity profile mode. No need for position send.
    void velMotor(byte direction)
    {
        Logger.log(TAG, "velMotor()");
        byte chkSum;
        byte tempbytes[] = new byte[4];
        byte controlByte;

        chkSum = 0x00;
        controlByte = (byte)(0xB6 | direction); //control byte 10110110 (we toggle bit 6)

        putch0((byte)0xAA);			//header byte
        chkSum = putchc0(_address, chkSum);	//address byte
        chkSum = putchc0((byte)0x94, chkSum); //command byte
        chkSum = putchc0(controlByte, chkSum);

        //Remaing four by two packets containing velocity, then acceleration
        longtobyte(_systemVelocity, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        longtobyte(_systemAcceleration, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        putch0(chkSum); // Checksum
        flush();

        recieveStatusPacket(); // Recieve response from moto
        /*** Trigger LED On ***/
    } // velMotor


    // Stop Smoothly when index is changed
    // Home captured on change of given limit, and stop smoothly once home is captured
    void setMotoHome(byte limit)
    {
        Logger.log(TAG, "setMotoHome()");

        byte chkSum;

        chkSum = 0;

        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x19, chkSum);
        if (limit == LIMITINDEX)
        {
            chkSum = putchc0((byte)(0x10 | limit), chkSum);
        }
        else
        {
            chkSum = putchc0((byte)(0x20 | limit), chkSum);
        } // if

        putch0(chkSum);
        flush();

        recieveStatusPacket(); //Recieve response from moto
    } //setMotoHome


    //Clear Sticky Bits
    public void clearMotoStickyBits() {
        byte chkSum;
        chkSum = 0x00;

        putch0((byte)0xAA); //clear sticky bits
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x0B, chkSum);
        putch0(chkSum);

        flush();

        recieveStatusPacket();
    }

    // Ask for and recieve a status packet
    public boolean requestStatusPacket() {
        Logger.log(TAG, "requestStatusPacket");
        byte chkSum;
        chkSum = 0x00;
/*
        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x0E, chkSum);    // R.Z. 110107  This is the new NOP  was  0x0d
        putch0(chkSum);
*/

        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x13, chkSum);
        chkSum = putchc0((byte)0x1F, chkSum);
        putch0(chkSum);


        /*
        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x10, chkSum);
        chkSum = putchc0((byte)0x31, chkSum);
        putch0(chkSum);
        */

        boolean ret = flush();

        if (ret == false) {
            Logger.log(TAG, "requestStatusPacket, flush() failed");
            return false;
        }
        else {
            lastTimeForStatusPacket = System.currentTimeMillis();

            Logger.log(TAG, "requestStatusPacket, calling receiveStatusPacket");
            recieveStatusPacket();
            return true;
        }
    }

    // A delay sequence that waits for a full and complete status packet to be
    // recieved from the moto controller
    // If a proper packet is not recieved within set limit (baudrate based), a request is sent for another one.
    // This 'should' render our delayLoop useless, and cut our waiting time into fractions.
    public void recieveStatusPacket() {
        Logger.log(TAG, "receiveStatusPacket...");
        int i = 0;
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
            if (i >= 20)
                break;
            if (_newBufferSize >= statusPacketLength)
                break;
        }

        if (_newBufferSize >= statusPacketLength) {
            Logger.log(TAG, "receiveStatusPacket..._newBufferSize >= statusPacketLength, return;");
            return;
        }
        else {
            Logger.log(TAG, "receiveStatusPacket...!(_newBufferSize >= statusPacketLength), calling requestStatusPacket()");
            boolean ret = requestStatusPacket();
            if (ret == false) {
                Logger.log(TAG, "receiveStatusPacket... called requestStatusPacket() - failed, return");
                return;
            }
        }
    }

    // Returns the currently stored Motor Position
    synchronized public long getMotorPosition() {
        return bytetolong(_buffer[1], _buffer[2], _buffer[3], _buffer[4]);
    }

    // Returns the currently stored Home Position
    synchronized public long getHomePosition() {
        return bytetolong(_buffer[9], _buffer[10], _buffer[11], _buffer[12]);
    }

    // Tests to see if the motor is moving
    synchronized public byte isMotorMoving() {
        if((_buffer[0] & STATBIT_DONE) == STATBIT_DONE) {
            return 0x00; // Not moving (done)
        }
        return 0x01; // Moving (not done)
    }

    public void onEvent(SEvent e) {
        if (EventManager.isEvent(e, SerialPort.kSerialPortConnectedNotification)) {
            _motorConnected((SerialPort)e.object);
        }
        else if (EventManager.isEvent(e, SerialPort.kSerialPortDisconnectedNotification)) {
            _motorDisconnected((SerialPort)e.object);
        }
        else if (EventManager.isEvent(e, SerialPort.kSerialPortReadDataNotification)) {
            _motorReadData((SerialPort.ReadData)e.object);
        }
    }

    protected void _motorConnected(SerialPort serialPort) {
        if (serialPort != _serialPort)
            return;
        if (mMotorStatus == MOTOR_NOTCONNECTED) {
            mMotorStatus = MOTOR_CONNECTED;
        }
        else {
            //
        }

        EventManager.sharedInstance().post(kMotorConnectedNotification, this);
    }

    protected void _motorDisconnected(SerialPort serialPort) {
        if (serialPort != _serialPort)
            return;
        mMotorStatus = MOTOR_DISCONNECTED;

        EventManager.sharedInstance().post(kMotorDisconnectedNotification, this);
    }

    protected void _motorReadData(SerialPort.ReadData data) {
        if (data.port != _serialPort)
            return;

        // received data
        String strReadData = CommonMethods.convertByteArrayToString(data.bytes);
        Logger.log(TAG, "read data : %s", strReadData);

        if (data.bytes == null || data.bytes.length == 0)
            return;

        synchronized (_motorReadingBuffer) {
            if (_motorReadingBufferSize < statusPacketLength) {
                for (int i = 0; i < data.bytes.length; i++) {
                    _motorReadingBuffer[_motorReadingBufferSize + i] = data.bytes[i];
                }
                _motorReadingBufferSize = _motorReadingBufferSize + data.bytes.length;
            }
            else {
                for (int i = 0; i < data.bytes.length; i++) {
                    _motorReadingBuffer[i] = data.bytes[i];
                }
                _motorReadingBufferSize = data.bytes.length;
            }

            if (_motorReadingBufferSize == statusPacketLength) {
                byte checksum = calCheckSum(_motorReadingBuffer, statusPacketLength - 1);
                if (checksum == _motorReadingBuffer[statusPacketLength - 1]) {
                    for (int i = 0; i < statusPacketLength; i++) {
                        _buffer[i] = _motorReadingBuffer[i];
                    }
                    _length = statusPacketLength;
                    _newBufferSize = statusPacketLength;
                }
            }
        }
    }

    public void initMotor() {
        /*
        The previous subsections have hinted at various operations required for network initialization.  Here
        is a specific list of the actions which should be taken on power-up, or after a network-wide reset :
        1.   Set the host baud communications port to 19,200 Baud, 1 start bit, 1 stop bit, no parity.
        2.   Send out a string of 20 null bytes (0x00) to fill up any partially filled command buffers.  Wait for
        at least 1 millisecond, and then flush any incoming bytes from the host’s receive buffer.
        3.   Use the Set Address command, as described in Section 5.2, to assign unique individual addresses
        to each module.  At this point, set all group addresses to 0xFF, and do not declare any group
        leaders.  (If you are not going to change the Baud rate from the default 19,200, you can set both
        the individual and group addresses at this time.)
        4.   Verify that the number of modules found matches the number expected.
        5.   Different NMC controller modules will have different type numbers and different version
        numbers (PIC-SERVO  = type 0). Use the Read Status command to read the type and version
        numbers for each module and verify that they match the types and versions expected.
        6.   Send a Set Baud command to the group address 0xFF to change the baud rate to the desired value.
                No status will be returned.  (Only required if using other than 19,200 Baud.)
        7.   Change the host’s Baud rate to match the rate just specified.
        8.   Poll each of the individual modules (using a No Op command) to verify that all modules are
        operating properly at the new Baud rate.
        9.   Use the Set Address command to assign any group addresses as needed.
        */
        /*** Flush PicServo ***/
        for(int ii = 0; ii < /*255*/15; ii++) {
            putch0((byte)0x00);
        }
        flush();

        Logger.log(TAG, "calling addressMotor(1)");
        /*** Address our Motors ***/
        addressMotor((byte)1);

        /*** Defining our desired Status Packet ***/
        Logger.log(TAG, "defining status packet...");
        statusPacketLength = 14;
        putch0((byte)0xAA);
        putch0(_address);
        putch0((byte)0x12);
        putch0((byte) 0x1F); // 00011111
        putch0((byte)(0x31 + _address));
        flush();


        Logger.log(TAG, "receiveStatusPacket for defining status packet...");
        // request status packet
        recieveStatusPacket();

        // reset motor
        resetMotor();

        startMotor(); // Servo
        // relayOnB(RELAY_SQFINGERS);

        if(_status == Motor.STATUS_OFF) {
            Logger.e(TAG, "start motor failed in initMotor()");
            stopMotor();
            // relayOnB(RELAY_SQFINGERS);
        } // if

    }

    //Clears Motor, and resets position home, and all related variables
    public void resetMotor() {
        Logger.log(TAG, "resetMotor()");
        byte chkSum;
        chkSum = 0x00;

        stopMotor();
        clearMotoStickyBits();

        //Reset Position value to 0
        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x00, chkSum);
        putch0(chkSum);

        flush();

        recieveStatusPacket();

        chkSum = 0x00;
        // Set Home to 0
        putch0((byte)0xAA);
        chkSum = putchc0(_address, chkSum);
        chkSum = putchc0((byte)0x0C, chkSum);
        putch0(chkSum);

        flush();

        recieveStatusPacket();
    }

    private byte calCheckSum(byte[] data, int length) {
        int ch = 0;
        for (int i = 0; i < length; i++) {
            ch = ch + (data[i] & 0xFF);
            ch = (byte)(ch & 0xFF);
        }
        return (byte)(ch & 0xFF);
    }

    private void putch0(byte bt) {
        synchronized (_commandBuffer) {
            if ((bt & 0xFF) == 0xAA) {
                memset(_commandBuffer, (byte) 0xFF, 16);
                _commandLength = 0;

                memset(_motorReadingBuffer, (byte) 0xFF, 16);
                _motorReadingBufferSize = 0;

                _newBufferSize = 0;
            }
            _commandBuffer[_commandLength] = bt;
            _commandLength++;
        }
    }

    private byte putchc0(byte bt, byte checksum) {
        putch0(bt);
        int ch = (checksum & 0xFF);
        ch = ch + (bt & 0xFF);
        return (byte)(ch & 0xFF);
    }

    private void memset(byte[] mem, byte val, int size) {
        for (int i = 0; i < size; i++)
            mem[i] = val;
    }

    private boolean flush() {
        boolean ret = true;
        synchronized (_commandBuffer) {
            if (_serialPort != null) {
                byte data[] = new byte[_commandLength];
                String s = "";
                for (int i = 0; i < _commandLength; i++) {
                    data[i] = _commandBuffer[i];
                    s = String.format("%s %02X", s, (int) data[i]);
                }
                Logger.log(TAG, String.format("writing data : %s", s));
                ret = _serialPort.write(data);
            } else {
                Logger.e(TAG, "flush() failed, serial port is null");
                ret = false;
            }
            _commandLength = 0;
        }

        // sleep
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    //takes 2000 and turns to 000007D0
    /*** NOTE ***
     note -> [0] is D0
     note -> [1] is 07
     note -> [2] is 00
     note -> [3] is 00 */
    private void longtobyte(long num, byte[] bytes) {
        bytes[3] = (byte)(num / 0x1000000);
        bytes[2] = (byte)((num - ((bytes[3] & 0xFF) * 0x1000000)) / 0x10000);
        bytes[1] = (byte)((num - (((bytes[3] & 0xFF) *0x1000000) + ((bytes[2] & 0xFF) * 0x10000))) / 0x100);
        bytes[0] = (byte)(num - (((bytes[3] & 0xFF) * 0x1000000) + ((bytes[2] & 0xFF) * 0x10000) + ((bytes[1] & 0xFF) * 0x100)));
    }

    // Turns 000007D0 into 2000
    /*** NOTE ***
     note -> [0] is D0
     note -> [1] is 07
     note -> [2] is 00
     note -> [3] is 00 */
    private long bytetolong (byte bytes0, byte bytes1,byte bytes2, byte bytes3) {
        long num;

        num = (((bytes3 & 0xFF) * 0x1000000) & 0xFF000000);
        num += (((bytes2 & 0xFF) * 0x10000) & 0x00FF0000);
        num += (((bytes1 & 0xFF) * 0x100) & 0x0000FF00);
        num += (bytes0 & 0xFF);

        return(num);
    } // bytetolong
}
