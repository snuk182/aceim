package aceim.api.utils;

import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

/**
 * This helper deals between several versions of making service
 * "uncloseable", or, in Android terms, foreground. All these actions tell
 * the system, that calling service should live as long as possible.
 */
public final class ServiceHelper {

	private final Service service;
	
	public ServiceHelper(Service service) {
		super();
		this.service = service;
	}

	public void doStartForeground() {
		mNM = (NotificationManager) service.getSystemService(Service.NOTIFICATION_SERVICE);
		try {
			mStartForeground = service.getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = service.getClass().getMethod("stopForeground", mStopForegroundSignature);
		} catch (Exception e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
		try {
			mSetForeground = service.getClass().getMethod("setForeground", mSetForegroundSignature);
		} catch (Exception e) {
			mSetForeground = null;
		}
		startForegroundCompat(android.R.string.ok, new Notification());
	}

	private final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
	private final Class<?>[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
	private final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private static NotificationManager mNM;
	private Method mSetForeground;
	private static Method mStartForeground;
	private static Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	private void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(service, args);
		} catch (Exception e) {
			Logger.log(e);
		} 
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	private void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}

		// Fall back on the old API.
		mSetForegroundArgs[0] = Boolean.TRUE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
		mNM.notify(id, notification);
	}
	
	public void doStopForeground(){
		stopForegroundCompat(android.R.string.ok);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	private void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mStopForeground, mStopForegroundArgs);
			return;
		}

		if (mNM != null) {
			// Fall back on the old API. Note to cancel BEFORE changing the
			// foreground state, since we could be killed at that point.
			mNM.cancel(id);
			mSetForegroundArgs[0] = Boolean.FALSE;
			invokeMethod(mSetForeground, mSetForegroundArgs);
		}
	}
}
