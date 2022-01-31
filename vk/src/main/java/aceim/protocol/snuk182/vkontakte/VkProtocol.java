package aceim.protocol.snuk182.vkontakte;

import aceim.api.dataentity.ActionFeature;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolOption.ProtocolOptionType;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ProtocolServiceFeatureTarget;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolService;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.protocol.snuk182.vkontakte.internal.VkApiConstants;
import android.content.Intent;

public class VkProtocol extends ProtocolService<VkService> {

	@Override
	protected VkService createService(byte arg0, String arg1) {
		return new VkService(arg0, arg1, getCallback(), getBaseContext());
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return new ProtocolServiceFeature[]{
				new ActionFeature(VkConstants.FEATURE_GROUPCHATS,
						getBaseContext().getString(R.string.groupchats),
						R.drawable.ic_menu_allfriends,
						false,
						false,
						new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
				new ListFeature(ApiConstants.FEATURE_XSTATUS, 
						getBaseContext().getString(R.string.status_string), 
						android.R.drawable.ic_menu_edit, 
						false, 
						true, 
						true, 
						null, 
						null, 
						true, 
						new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT})				
		};
	}

	@Override
	protected String getProtocolName() {
		return VkConstants.PROTOCOL_NAME;
	}

	@Override
	protected ProtocolOption[] getProtocolOptions() {
		ProtocolOption[] a = new ProtocolOption[]{
				new ProtocolOption(ProtocolOptionType.STRING, VkConstants.KEY_USERNAME, null, R.string.username, true),
				new ProtocolOption(ProtocolOptionType.PASSWORD, VkConstants.KEY_PASSWORD, null, R.string.password, true),
				//new ProtocolOption(ProtocolOptionType.CHECKBOX, VkConstants.KEY_AUTO_SUBMIT_AUTH_DIALOG, "true", R.string.auto_submit_auth_dialog, true),
				//new ProtocolOption(ProtocolOptionType.STRING, VkConstants.KEY_AUTH_SERVER_URL, VkConstants.OAUTH_SERVER, R.string.auth_server_url, true),
				new ProtocolOption(ProtocolOptionType.INTEGER, VkConstants.KEY_LONGPOLL_WAIT_TIME, VkConstants.POLL_WAIT_TIME, R.string.longpoll_wait_time, true),
		};
		return a;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		return START_NOT_STICKY;
	}

	private void handleCommand(Intent intent) {
		String uid = intent.getStringExtra(VkConstants.KEY_PROTOCOL_ID);
		
		for (VkService as : mAccountServices) {
			if (as.getProtocolUid().equals(uid)) {
				as.loginResult(intent.getExtras());
				return;
			}
		}
		
		Logger.log("Cannot find account service #" + uid, LoggerLevel.WTF);
	}
}
