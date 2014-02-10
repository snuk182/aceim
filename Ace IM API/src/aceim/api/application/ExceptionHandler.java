package aceim.api.application;

import aceim.api.utils.Logger;


/**
 * Overridden exception handler for storing stack traces in text log file.
 * 
 * @author Sergiy
 *
 */
class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    Thread.UncaughtExceptionHandler oldHandler;

    ExceptionHandler() {
        oldHandler = Thread.getDefaultUncaughtExceptionHandler(); 
    }

	public void uncaughtException(Thread thread, Throwable ex) {
		Logger.logToFile = true;
		Logger.log(ex);
		if(oldHandler != null) {
        	oldHandler.uncaughtException(thread, ex); 
        } 
	}    
}
