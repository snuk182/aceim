package aceim.app;

import aceim.api.utils.Logger;


/**
 * Overridden exception handler for storing stack traces in text log file.
 * 
 * @author Sergiy
 *
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    Thread.UncaughtExceptionHandler oldHandler;

    public ExceptionHandler() {
        oldHandler = Thread.getDefaultUncaughtExceptionHandler(); 
    }

	public void uncaughtException(Thread thread, Throwable ex) {
		Logger.log(ex);
		if(oldHandler != null) {
        	oldHandler.uncaughtException(thread, ex); 
        } 
	}    
}
