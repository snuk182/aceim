package aceim.api.application;

import android.app.Application;

/**
 * All protocol implementations are recommended to be extended from this class.
 */
public class AceIMBaseApplication extends Application {

	@Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        super.onCreate();
    }

}
