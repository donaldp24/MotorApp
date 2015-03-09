package com.Tony.Zakron.helper;


public class Logger {
	public static final String TAG = "Logger";
	public static void log(String tag, String format, Object...args) {
		android.util.Log.w(TAG + ": " + tag, String.format(format, args));
	}

	public static void logError(String tag, String format, Object...args) {
		android.util.Log.e(TAG + ": " + tag, String.format(format, args));
	}

	public static void e(String tag, String format, Object...args) {
		logError(tag, format, args);
	}
}
