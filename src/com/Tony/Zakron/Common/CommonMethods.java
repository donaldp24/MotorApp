package com.Tony.Zakron.Common;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by xiaoxue on 8/21/14.
 */
public class CommonMethods {

    /*
     * show alert with title and content of alert
     */
    public static void showAlertUsingTitle(String titleString, String messageString) {
        // have to re-implement.
    }

    /*
     * convert date to string
     * @param
     *  formatString : format of date : "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"
     */
    public static String date2str(Date date, String formatString) {
        SimpleDateFormat format = new SimpleDateFormat(formatString);
        return format.format(date);
    }

    /*
     * convert string to date
     * @param
     *  formatString : format of date ex) "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"
     */
    public static Date str2date(String dateString, String formatString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            CommonMethods.Log("error parsing date : " + e.getMessage());
        }
        return convertedDate;
    }

    /*
     * play tap sound
     */
    public static void playTapSound() {
        // have to re-implement
    }

    public static void Log(String formatMsg, Object ...values) {
        Log.v(CommonDefs.TAG, String.format(formatMsg, values));
    }

    public static int compareOnlyDate(Date date1, Date date2)
    {
        String str1 = CommonMethods.date2str(date1, CommonDefs.DATE_FORMAT);
        String str2 = CommonMethods.date2str(date2, CommonDefs.DATE_FORMAT);
        Date date3 = CommonMethods.str2date(str1, CommonDefs.DATE_FORMAT);
        Date date4 = CommonMethods.str2date(str2, CommonDefs.DATE_FORMAT);
        return date3.compareTo(date4);
    }

    public static void showAlertMessage(Context ctx, String msg)
    {
        Toast newToast = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
        newToast.setGravity(Gravity.CENTER, 0, 0);
        newToast.show();
    }

    public static void showAlertMessage(Context ctx, int msgid)
    {
        Toast newToast = Toast.makeText(ctx, msgid, Toast.LENGTH_LONG);
        newToast.setGravity(Gravity.CENTER, 0, -50);
        newToast.show();
    }

    /* Convert a 16-byte array to a UUID */
    public static UUID toUUID(byte[] byteArray) {
    	
    	try{
    	byte[] p = byteArray;
    	String strUUID = String.format("%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X", 
    			p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12], p[13], p[14], p[15]);
    	Log(strUUID);
    	UUID result = UUID.fromString(strUUID);
    	/*
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (byteArray[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (byteArray[i] & 0xff);
        }
        UUID result = new UUID(msb, lsb);
        */
    	return result;
    	} catch (Exception e) {
    		Log(e.getMessage());
    	}
        return null;
    }

    /* Convet a 2-byte array to a UUID */
    public static UUID toUUID(byte b0, byte b1) {
        //"00002415-0000-1000-8000-00805F9B34FB
        byte btbaseUUID[] = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
                            (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x5F, (byte)0x9B, (byte)0x34, (byte)0xFB};
        btbaseUUID[2] = b0;
        btbaseUUID[3] = b1;

        return toUUID(btbaseUUID);
    }

    /* convert hex data to string */
    public static String convertByteArrayToString(byte[] array) {
        String ret = "";
        if (array == null || array.length == 0)
            return ret;
        for (int i = 0; i < array.length; i++) {
            ret += String.format("%02X", (array[i] & 0xFF));
            ret += " ";
        }
        return ret;
    }
}
