package aceim.protocol.snuk182.mrim;

import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ProtocolService;
import aceim.protocol.snuk182.mrim.utils.ResourceUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class MrimProtocol extends ProtocolService<MrimService> {

	private final InternetConnectionChecker mInternetConnectionChecker = new InternetConnectionChecker();

	@Override
	protected MrimService createService(byte serviceId, String protocolUid) {
		MrimService service = new MrimService(serviceId, protocolUid, getCallback(), getBaseContext());
		return service;
	}

	@Override
	protected String getProtocolName() {
		return ResourceUtils.PROTOCOL_NAME;
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return ResourceUtils.getFeatures(getBaseContext());
	}

	@Override
	protected ProtocolOption[] getProtocolOptions() {
		return ResourceUtils.OPTIONS;
	}

	@Override
	public void onDestroy() {
		getBaseContext().unregisterReceiver(mInternetConnectionChecker);
	}

	@Override
	public void onCreate() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		getBaseContext().registerReceiver(mInternetConnectionChecker, intentFilter);
	}

	private class InternetConnectionChecker extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras() != null) {
				if (intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY)) {
					for (MrimService service : mAccountServices) {
						if (service.getCurrentState() != ConnectionState.DISCONNECTED) {
							service.getProtocol().disconnect();
						}
					}
				}
			}
		}
	}
}
