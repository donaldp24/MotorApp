package com.Tony.Zakron.Motor;

import android.util.Log;
import com.Tony.Zakron.BLE.SerialPort;
import com.Tony.Zakron.BLE.SerialPortDelegate;
import com.Tony.Zakron.Common.CommonMethods;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xiaoxue
 * Date: 8/27/14
 * Time: 7:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class MotorControl implements SerialPortDelegate{

    /*** Motor stuff ***/
// Directions and Limit Stuff
    public static final int DIRFWD = 0x00;			// Forward Movement
    public static final int DIRREV = 0x40;			// Reverse Movement
    public static final int LIMITREAR = 0x01;		// Limit One Bit (for set home Command)
    public static final int LIMITRAM = 0x02;		// Limit Two bit (for set Home Command)
    public static final int LIMITINDEX = 0x08;		// Index Bit (for setHome Command)

    // Status Bits etc.
    public static final int STATBIT_DONE = 0x01;	// Move done bit of status Byte
    public static final int STATBIT_LIMITREAR = 0x20;	// Limit1 input Bit of Status Byte
    public static final int STATBIT_LIMITRAM = 0x40; // Limit2 input bit of Status Byte
    public static final int STATBIT_HOME = 0x80;	// Home in progress bit of Status Byte
    public static final int STATBIT_INDEX = 0x01;	// Index bit of Aux Status Byte


    public SerialPort _serialPort = null;

    private ArrayList<Byte> _buffer = new ArrayList<Byte>();

    public int statusPacketLength = 0;
    public int motorBufferSize = 0;
    public byte motorBuffer[] = new byte[16];
    public int packetFlag = 0;
    public int motoTimeOut = 0;
    
    private Motor _motor = new Motor();
    
    private MotorControlListener listener = null;;

    public MotorControl(MotorControlListener listener) {
    	this.listener = listener;
        main();
    }

    private void memset(byte[] mem, byte val, int size)
    {
        for (int i = 0; i < size; i++)
            mem[i] = val;
    }

    private void putch0(byte bt) {
        if ((bt & 0xFF) == 0xAA)
        {
            memset(motorBuffer, (byte)0xFF, 16);
            motorBufferSize = 0;
        }
        _buffer.add(bt);
    }

    private byte putchc0(byte bt, byte checksum) {
        putch0(bt);
        int ch = (checksum & 0xFF);
        ch = ch + (bt & 0xFF);
        return (byte)(ch & 0xFF);
    }


    private void flush() {
        if (_serialPort != null)
        {
            byte data[] = new byte[_buffer.size()];
            for (int i = 0; i < _buffer.size(); i++)
                data[i] = _buffer.get(i);
            _serialPort.write(data);
            
            try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        _buffer.clear();
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
    long bytetolong (byte bytes0, byte bytes1,byte bytes2, byte bytes3)
    {
        long num;

        num = (((bytes3 & 0xFF) * 0x1000000) & 0xFF000000);
        num += (((bytes2 & 0xFF) * 0x10000) & 0x00FF0000);
        num += (((bytes1 & 0xFF) * 0x100) & 0x0000FF00);
        num += (bytes0 & 0xFF);

        return(num);
    } // bytetolong

    // Sets the Address of the Motor at the given address to the new address
    public void addressMotor(Motor motor, byte newAddress) {
        byte chkSum;
        chkSum = 0x00;
        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x21, chkSum);
        chkSum = putchc0(newAddress, chkSum);
        chkSum = putchc0((byte)0xFF, chkSum);
        putch0(chkSum);

        flush();

        motor._address = newAddress;
    }

    // Stops (smoothly) any motion currently in progress
    public void stopMotor(Motor motor) {
        byte chkSum;
        chkSum = 0x00;

        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x17, chkSum);
        chkSum = putchc0((byte)0x03, chkSum); // Turn Motor Off (No servo)
        putch0(chkSum);

        flush();

        recieveStatusPacket(motor);
        clearMotoStickyBits(motor);
        /*** Trigger LED Off ***/

        motor._status = Motor.STATUS_OFF;
    }

    //Turns on the amp, sets to stop smoothly, then sets gain
    public void startMotor(Motor motor) {
        byte chkSum;
        chkSum = 0x00;

        motorBufferSize = 0;
        memset(motorBuffer, (byte)0xFF, 16);
        // Set gain as defined
        setGain(motor);

        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x17, chkSum);
        chkSum = putchc0((byte)0x09, chkSum); // Turns on Pos servo R.Z 103107
        putch0(chkSum);

        flush();

        recieveStatusPacket(motor);
        motor._status = Motor.STATUS_ON;
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

    //sends to the picservo all the commands to correctly change the gain
    public void setGain(Motor motor) {
        byte bytes[] = new byte[4];
        byte chkSum;
        chkSum = 0x00;

        packetFlag = 1;
        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0xF6, chkSum); //command byte

        longtobyte(motor._gain[0], bytes);
        chkSum = putchc0(bytes[0], chkSum); //kp
        chkSum = putchc0(bytes[1], chkSum);

        longtobyte(motor._gain[1], bytes);
        chkSum = putchc0(bytes[0], chkSum); //kd
        chkSum = putchc0(bytes[1], chkSum);

        longtobyte(motor._gain[2], bytes);
        chkSum = putchc0(bytes[0], chkSum); //ki
        chkSum = putchc0(bytes[1], chkSum);

        longtobyte(motor._gain[3], bytes);
        chkSum = putchc0(bytes[0], chkSum); //integration limit
        chkSum = putchc0(bytes[1], chkSum);

        chkSum = putchc0((byte)(motor._gain[4] & 0xFF), chkSum); //output limit

        chkSum = putchc0((byte)(motor._gain[5] & 0xFF), chkSum); //current limit

        longtobyte(motor._gain[6], bytes);
        chkSum = putchc0(bytes[0], chkSum); //error limit
        chkSum = putchc0(bytes[1], chkSum);

        chkSum = putchc0((byte)(motor._gain[7] & 0xFF), chkSum); //servo limit

        chkSum = putchc0((byte)(motor._gain[8] & 0xFF), chkSum); //deadband compensation

        chkSum = putchc0 ((byte)01, chkSum); //added by me
        
        putch0(chkSum); //CheckSum

        flush();

        packetFlag = 0;
        recieveStatusPacket(motor); //Retrieve response
    }

    //Moves Motor to a a given position, with current vel and accel
    //picservo requires the motor to NOT be moving in order to send new instructions
    //note to any programmer, this function does NOT take that into account
    //DO NOT use this WITHOUT MAKING SURE THE MOTOR IS STOPPED
    public void moveMotor(Motor motor, long destPosition) {
        byte chkSum;
        byte tempbytes[] = new byte[4];
        byte controlByte;
        long currentPos;


        /*** We have to decide the fastest route (forwards or backwards, no rounding the horn) ***/
        currentPos = getMotoPosition(motor);

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
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0xD4, chkSum); //command byte
        chkSum = putchc0(controlByte, chkSum); //control byte 10010111

        //send the next four x three bytes (goal position, vel, then acceleration)
        longtobyte(destPosition, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        longtobyte(motor._systemVelocity, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        longtobyte(motor._systemAcceleration, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        putch0(chkSum); // CheckSum

        flush();

        packetFlag = 0;
        recieveStatusPacket(motor); //Retrieve response status packet
        /*** Trigger LED On ***/
    }

    //Velocity profile mode. No need for position send.
    public void velMotor(Motor motor, char direction) {
        byte chkSum;
        byte tempbytes[] = new byte[4];
        byte controlByte;

        chkSum = 0x00;
        controlByte = (byte)(0xB6 | direction); //control byte 10110110 (we toggle bit 6)

        putch0((byte)0xAA);			//header byte
        chkSum = putchc0(motor._address, chkSum);	//address byte
        chkSum = putchc0((byte)0x94, chkSum); //command byte
        chkSum = putchc0(controlByte, chkSum);

        //Remaing four by two packets containing velocity, then acceleration
        longtobyte(motor._systemVelocity, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        longtobyte(motor._systemAcceleration, tempbytes);
        chkSum = putchc0(tempbytes[0], chkSum);
        chkSum = putchc0(tempbytes[1], chkSum);
        chkSum = putchc0(tempbytes[2], chkSum);
        chkSum = putchc0(tempbytes[3], chkSum);

        putch0(chkSum); // Checksum

        flush();

        recieveStatusPacket(motor); // Recieve response from moto
        /*** Trigger LED On ***/
    }

    // Moves the motor Forward at the given Speed
    // Note, we 'move' the motor towards either end at the given speed.
    // This way we dont overun and hit either end, and we dont have to poll positions.
    public void jogMotor(Motor motor, char direction, long velocity) {
        /*	unsigned char chkSum;
        unsigned char tempbytes[4];
        unsigned char controlByte;
        long destPosition;

        /*** Set our direction, and our e3nding point based on Direction ***/
    /*	controlByte = (0x97 | direction); //control byte 10110110 (we toggle bit 6)
        if(direction == DIRFWD)
        {
            destPosition = metricToPositionX(maxValues[IBEDI]);
        }
        else
        {
            destPosition = 0;
        } // if

        /*** Prepare and send the command ***/
    /*	chkSum = 0x00;

        putch0(0xAA);
        putchc0(address, &chkSum);
        putchc0(0xD4, &chkSum); //command byte
        putchc0(controlByte, &chkSum); //control byte 10010111

        //send the next four x three bytes (goal position, vel, then acceleration)
        longtobyte(destPosition, tempbytes);
        putchc0(tempbytes[0], &chkSum);
        putchc0(tempbytes[1], &chkSum);
        putchc0(tempbytes[2], &chkSum);
        putchc0(tempbytes[3], &chkSum);

        longtobyte(velocity, tempbytes);
        putchc0(tempbytes[0], &chkSum);
        putchc0(tempbytes[1], &chkSum);
        putchc0(tempbytes[2], &chkSum);
        putchc0(tempbytes[3], &chkSum);

        longtobyte(systemAcceleration[address - 0x01], tempbytes);
        putchc0(tempbytes[0], &chkSum);
        putchc0(tempbytes[1], &chkSum);
        putchc0(tempbytes[2], &chkSum);
        putchc0(tempbytes[3], &chkSum);

        putch0(chkSum); // CheckSum
        /*** Trigger LED On ***/
    }

    // Stop Smoothly when index is changed
    // Home captured on change of given limit, and stop smoothly once home is captured
    public void setMotoHome(Motor motor, char limit) {
        byte chkSum;
        chkSum = 0x00;

        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x19, chkSum);
        if(limit == LIMITINDEX)
        {
            chkSum = putchc0((byte)(0x10 | limit), chkSum);
        }
        else
        {
            chkSum = putchc0((byte)(0x20 | limit), chkSum);
        } // if
        putch0(chkSum);

        flush();

        recieveStatusPacket(motor); //Recieve response from moto
    }

    //Capture home on change of index, no change in motion
    public void setMotoGoPastHome(Motor motor, char limit) {
        byte chkSum;
        chkSum = 0x00;

        // Set Moto into Homing Mode (Looking for edge)
        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x19, chkSum);
        chkSum = putchc0((byte)limit, chkSum); // 00001000 Trigger home on CHANGE of index (sensor triggered)
        putch0(chkSum); // soft CheckSum

        flush();

        recieveStatusPacket(motor); //Recieve response from moto
    }

    //Clears Motor, and resets position home, and all related variables
    public void resetMotor(Motor motor) {
        byte chkSum;
        chkSum = 0x00;

        stopMotor(motor);
        clearMotoStickyBits(motor);

        //Reset Position value to 0
        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x00, chkSum);
        putch0(chkSum);

        flush();

        recieveStatusPacket(motor);

        chkSum = 0x00;
        // Set Home to 0
        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x0C, chkSum);
        putch0(chkSum);

        flush();

        recieveStatusPacket(motor);
    }

    //Clear Sticky Bits
    public void clearMotoStickyBits(Motor motor) {
        byte chkSum;
        chkSum = 0x00;

        putch0((byte)0xAA); //clear sticky bits
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x0B, chkSum);
        putch0(chkSum);

        flush();

        recieveStatusPacket(motor);
    }

    // Ask for and recieve a status packet
    public void requestStatusPacket(Motor motor) {
        byte chkSum;
        chkSum = 0x00;

        putch0((byte)0xAA);
        chkSum = putchc0(motor._address, chkSum);
        chkSum = putchc0((byte)0x0E, chkSum);    // R.Z. 110107  This is the new NOP  was  0x0d
        putch0(chkSum);

        flush();

        //recieveStatusPacket(motor);
    }

    // A delay sequence that waits for a full and complete status packet to be
    // recieved from the moto controller
    // If a proper packet is not recieved within set limit (baudrate based), a request is sent for another one.
    // This 'should' render our delayLoop useless, and cut our waiting time into fractions.
    public void recieveStatusPacket(Motor motor) {
        requestStatusPacket(motor);
    }

    // Returns the currently stored Motor Position
    public long getMotoPosition(Motor motor) {
        return bytetolong(motor._buffer[1], motor._buffer[2], motor._buffer[3], motor._buffer[4]);
    }

    // Returns the currently stored Home Position
    public long getHomePosition(Motor motor) {
        return bytetolong(motor._buffer[9], motor._buffer[10], motor._buffer[11], motor._buffer[12]);
    }

    // Tests to see if the motor is moving
    public byte isMotorMoving(Motor motor) {
        if((motor._buffer[0] & STATBIT_DONE) == STATBIT_DONE)
        {
            return 0x00; // Not moving (done)
        }
        return 0x01; // Moving (not done)
    }

    @Override
    public void portEvent(SerialPort serialPort, SPEvent ev, int error) {
        //To change body of implemented methods use File | Settings | File Templates.
        if (ev == SerialPortDelegate.SPEvent.SP_EVT_OPEN)
        {
            if (error == 0)
            {
                CommonMethods.Log("SerialPort open success");
                
                // init motor
                _motor._address = 0;
                
                /*
                new Thread(new Runnable() {
                	public void run()
                	{
                		initMotor(_motor);
                		
                		moveMotor(_motor, 1000000);
                	}
                }).start();
                */
                
                listener.onPortOpened(true);
            }
            else
            {
                CommonMethods.Log("SerialPort open failed");
                
                listener.onPortOpened(false);
            }
        }
        else if (ev == SPEvent.SP_EVT_CLOSED)
        {
            if (error == 0)
            {
                CommonMethods.Log("SerialPort close success");
            }
            else
            {
                CommonMethods.Log("SerialPort close failed");
            }
        }

    }

    @Override
    public void writeComplete(SerialPort serialPort, int err) {
        //To change body of implemented methods use File | Settings | File Templates.
        CommonMethods.Log("write complete");
    }

    @Override
    public void receivedData(SerialPort serialPort, byte[] data) {
        //To change body of implemented methods use File | Settings | File Templates.
        String strData = CommonMethods.convertByteArrayToString(data);
        CommonMethods.Log("receivedData : %d bytes - %s", data.length, strData);

        if (motorBufferSize + data.length > statusPacketLength)
        {
            CommonMethods.Log("receivedData : error motorBufferSize(%d) + data.length(%d) > statusPacketLength(%d)", motorBufferSize, data.length, statusPacketLength);
            return;
        }

        // add to motorBuffer
        for (int i = 0; i < data.length; i++)
        {
            motorBuffer[motorBufferSize] = data[i];
            motorBufferSize++;
            _motor._buffer[i] = data[i];
        }

        CommonMethods.Log("motorBufferSize : %d", motorBufferSize);
    }

    public void main() {
        packetFlag = 0;
        memset(motorBuffer, (byte)0xFF, 16);
        motorBufferSize = 0;
        statusPacketLength = 14;
    }

    // Initilaizes the motor, by changing it's baud rate to 57600, adjusting UART0 appropriately
// and then preparing it to send our desired status packet
    public void initMotor(Motor motor)
    {
//
//        int motoStatus[3];
//        int i;
//        UART u0; // UART0 = Motor Controller
//
//        for(i = 0; i < 3; i++)
//        {
//            motoStatus[i] = motorStatus[i];
//        } // for
//
//        baudRate = 19200;
        /*** Flush PicServo ***/
        for(int ii = 0; ii < 255; ii++)
        {
            putch0((byte)0x00);
        }
        flush();
        //flush_uart0();

        /*** Address our Motors ***/
        addressMotor(motor, (byte)1);

        /*** Hard Reset (Return to Power-up state) ***/
    /*	putch0(0xAA);
        putch0(0xFF);	// Note Group Address, send to all motors
        putch0(0x0F);
        putch0(0x0E);
        delayLoop(100);

        putch0(0xAA);
        putch0(0xFF);
        putch0(0x1A);
        putch0(0x0A);  // 0x0A = 115200 baud
        putch0(0x123);
        delayLoop(100);

        setUART0Baud(115200);

    /*	UART0_LCTL |= 0x80;
        UART0_BRG_L |= 0x02;
        UART0_BRG_L = 0x1B;
        UART0_BRG_H = 0x00;
    //	UART0_BRG_H |= 0x00;
        UART0_LCTL &= 0x7F;

	delayLoop(50); */

        /*** Defining our desired Status Packet ***/
        statusPacketLength = 14;
        putch0((byte)0xAA);
        putch0(motor._address);
        putch0((byte)0x12);
        putch0((byte)0x1F); // 00011101
        putch0((byte)(0x31 + motor._address));
        flush();
        recieveStatusPacket(motor);

        resetMotor(motor); // Set to 0
        startMotor(motor); // Servo
        //relayOnB(RELAY_SQFINGERS);

        if(motor._status == Motor.STATUS_OFF)
        {
            stopMotor(motor);
            //relayOnB(RELAY_SQFINGERS);
        } // if
        
        
        listener.onMotorInited(motor);


        //updateXY(1);
    } // initMotor
    
    public long inchToPositionValue(double inch)
    {
    	long ret = 0;
    	ret = (long)(inch * 2540);
    	return ret;
    }
    
    public double positionValueToInch(long positionValue)
    {
    	double ret = 0;
    	ret = ((double)positionValue) / 2540.0;
    	return ret;
    }

}
