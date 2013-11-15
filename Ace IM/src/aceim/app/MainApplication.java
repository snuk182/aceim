package aceim.app;

import android.app.Application;

/**
 * This custom Application class overrides standard JVM's uncaught exception handler 
 * to write possible exceptions to log file, if enabled in settings, along with console output.
 * 
 * @author Sergiy Plygun
 *
 */
public class MainApplication extends Application {

	@Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        super.onCreate();
    }
}
