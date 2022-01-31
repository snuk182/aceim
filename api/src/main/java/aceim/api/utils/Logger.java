package aceim.api.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

/**
 * Simple logger with ability to log messages to the file.
 */
public final class Logger {
	
	private static final String APPNAME = "AceIM";

	/**
	 * Should the following data be logged to a file?
	 */
	public static boolean logToFile = false;
	
	private static final WriteLock lock = new ReentrantReadWriteLock(true).writeLock();

	/**
	 * Log data, according to level (similar to in-built Android logging levels)
	 * @param string message to log
	 * @param level logging level
	 */
	@SuppressLint("NewApi")
	public static void log(String string, LoggerLevel level) {
		
		if (string == null) {
			return;
		}
		
		switch(level) {
		case DEBUG:
			Log.d(APPNAME, string);
			break;
		case ERROR:
			Log.e(APPNAME, string);
			break;
		case INFO:
			Log.i(APPNAME, string);
			break;
		case VERBOSE:
			Log.v(APPNAME, string);
			break;
		case WARNING:
			Log.w(APPNAME, string);
			break;
		case WTF:
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
				Log.e(APPNAME, string);
			} else {
				Log.wtf(APPNAME, string);
			}
			break;
		}
		
		if (logToFile) {
			
			String storageState = Environment.getExternalStorageState();
			if (storageState.equals(Environment.MEDIA_MOUNTED)) {
				lock.lock();
				try {
					File root = Environment.getExternalStorageDirectory();
					File downloads = new File(root, APPNAME);
					downloads.mkdirs();
					File logFile = new File(downloads, APPNAME + ".log");
					
					if (!logFile.exists()) {
						logFile.createNewFile();
					}
					FileOutputStream fos = new FileOutputStream(logFile, true);
					fos.write(new String(new Date() + " --> " + string + "\n").getBytes());
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				lock.unlock();
			}
		}
	}

	/**
	 * Log data, with DEBUG logging level.
	 * @param string
	 */
	public static void log(String string) {
		log(string, LoggerLevel.DEBUG);
	}

	/**
	 * Log exception data, with WARNING logging level.
	 * @param e
	 */
	public static void log(Throwable e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.toString());
		for (StackTraceElement el : e.getStackTrace()) {
			sb.append("\n" + el);
		}
		sb.append("\n-----------------------------------------------------------------\n");
		log(sb.toString(), LoggerLevel.WARNING);
	}
	
	/**
	 * Logging levels, according to in-built Android logging levels.
	 * @author Sergiy P
	 *
	 */
	public enum LoggerLevel {
		/**
		 * {@link Log#d(String, String)}
		 */
		DEBUG,
		
		/**
		 * {@link Log#v(String, String)}
		 */
		VERBOSE,
		
		/**
		 * {@link Log#i(String, String)}
		 */
		INFO,
		
		/**
		 * {@link Log#w(String, String)}
		 */
		WARNING,
		
		/**
		 * {@link Log#e(String, String)}
		 */
		ERROR,
		
		/**
		 * {@link Log#wtf(String, String)}
		 */
		WTF
	}
}
