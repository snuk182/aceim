package aceim.app.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;

import aceim.app.Constants;
import aceim.app.Constants.OptionKey;
import aceim.app.dataentity.AccountOptionKeys;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OptionsReceiver extends BroadcastReceiver {
	
	private final List<OnOptionChangedListener> mListeners = new CopyOnWriteArrayList<OnOptionChangedListener>();

	public OptionsReceiver(OnOptionChangedListener listener) {
		if (listener != null) {
			registerListener(listener);
		}
	}
	
	public void registerListener(OnOptionChangedListener listener) {
		mListeners.add(listener);
	}
	
	public void unregisterListener(OnOptionChangedListener listener) {
		mListeners.remove(listener);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		OptionKey key = (OptionKey)intent.getParcelableExtra(Constants.INTENT_EXTRA_OPTION_KEY);
		byte serviceId = intent.getByteExtra(Constants.INTENT_EXTRA_SERVICE_ID, (byte) -1);
		
		if (key == null || (key instanceof AccountOptionKeys && serviceId < 0)) {
			Logger.log("OptionsReceiver got broken intent!", LoggerLevel.INFO);
			return;
		}
		
		Object value = intent.getExtras().get(Constants.INTENT_EXTRA_OPTION_VALUE);
		
		for (OnOptionChangedListener listener : mListeners) {
			listener.onOptionChanged(key, value.toString(), serviceId);
		}
	}

	public interface OnOptionChangedListener {		
		void onOptionChanged(OptionKey key, String value, byte serviceId);
	}
}
