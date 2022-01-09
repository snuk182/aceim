package aceim.protocol.snuk182.icq;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ProtocolService;
import aceim.protocol.snuk182.icq.utils.ResourceUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class ICQProtocol extends ProtocolService<ICQService> {
	
	private final InternetConnectionChecker mInternetConnectionChecker = new InternetConnectionChecker();

	@Override
	protected ICQService createService(byte serviceId, String protocolUid) {
		return new ICQService(serviceId, protocolUid, getCallback(), getBaseContext());
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return ResourceUtils.getFeatures(getBaseContext());
	}

	@Override
	protected String getProtocolName() {
		return IcqApiConstants.PROTOCOL_NAME;
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
		
		private volatile boolean isRunning = false;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!isRunning) {
				ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
				scheduledExecutorService.schedule(new Runnable() {
					
					@Override
					public void run() {
						for (final ICQService service : mAccountServices) {
							if (service.getCurrentState() != ConnectionState.DISCONNECTED) {
								service.getProtocol().disconnect();
							}
						}
						isRunning = false;
					}
					
				}, 2, TimeUnit.SECONDS);
				
			}
		}
	}
}
