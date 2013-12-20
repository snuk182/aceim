package aceim.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class NetworkConnectionListener extends BroadcastReceiver {
	
	private final Context mContext;	
	private final OnConnectionChangedListener mListener;
	
	private final IntentFilter mIntentFilter;
	
	private byte[] servicesToRestore = null; 
	
	NetworkConnectionListener(Context context, OnConnectionChangedListener listener) {
		this.mContext = context;
		this.mListener = listener;
		
		this.mIntentFilter = new IntentFilter();
		this.mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		
		context.registerReceiver(this, mIntentFilter);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getExtras() != null) {
			if (intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY)){
				if (mListener != null) {
					servicesToRestore = mListener.onConnectionDisappeared();
				}
			}
		}
		
		if (mListener != null) {
			if (isNetworkAvailable()) {
				mListener.onConnectionAppeared(servicesToRestore);
			} else {
				servicesToRestore = mListener.onConnectionDisappeared();
			}
		}
	}

	public boolean isNetworkAvailable() {
		ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		
		return networkInfo != null && networkInfo.isConnected();
	}
	
	public void onExit() {
		mContext.unregisterReceiver(this);
	}

	interface OnConnectionChangedListener {
		void onConnectionAppeared(byte[] servicesToStore);
		byte[] onConnectionDisappeared();
	}
}
