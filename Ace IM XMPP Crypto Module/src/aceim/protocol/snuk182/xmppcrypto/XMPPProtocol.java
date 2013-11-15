package aceim.protocol.snuk182.xmppcrypto;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolOption.ProtocolOptionType;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.IProtocolService;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolService;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;
import org.jivesoftware.smack.util.StringUtils;

import aceim.protocol.snuk182.xmppcrypto.utils.ResourceUtils;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;

public class XMPPProtocol extends ProtocolService {
	
	private static ProtocolOption[] OPTIONS;
	
	private final IProtocolService.Stub stub = new IProtocolService.Stub(){

		@Override
		public void registerCallback(ICoreProtocolCallback callback) throws RemoteException {
			setCallback(callback);
		}

		@Override
		public void addAccount(byte serviceId, String protocolUid) throws RemoteException {
			addAccountService(serviceId, protocolUid);
		}

		@Override
		public void removeAccount(byte serviceId) throws RemoteException {
			removeAccountService(serviceId);
		}

		@Override
		public void shutdown() throws RemoteException {
			XMPPProtocol.this.shutdown();
		}

		@Override
		public void requestFullInfo(byte serviceId, String uid, boolean shortInfo) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.getBuddyInfo(uid, shortInfo);
		}

		@Override
		public void buddyAction(ItemAction action, Buddy buddy) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(buddy.getServiceId());
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.buddyAction(action, buddy);
		}

		@Override
		public void buddyGroupAction(ItemAction action, BuddyGroup group) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(group.getServiceId());
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.groupAction(action, group);
		}

		@Override
		public void setFeature(String featureId, OnlineInfo info) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(info.getServiceId());
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			if (featureId.equals(ApiConstants.FEATURE_STATUS)) {
				service.setStatus(info.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) 0));
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ADD_BUDDY)) {
				processAddBuddyFeature(featureId, info, service);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_GROUPCHATS)) {
				service.requestAvailableGroupchats();
			} else if (featureId.equals(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION)) {
				service.getChatConfigurationForm(info.getProtocolUid());
			} else if (featureId.equals(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_RESULT)) {
				processChatRoomResultFeature(featureId, info, service);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ADD_GROUPCHAT)) {
				processAddGroupchatFeature(featureId, info, service);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_DESTROY_CHAT_ROOM)) {
				ServiceMessage message = new ServiceMessage(service.getServiceId(), info.getProtocolUid(), true);
				message.setIncoming(true);
				message.setContactDetail(getBaseContext().getString(R.string.confirm_destroy_room, info.getProtocolUid()));
				getCallback().message(message);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ENCRYPTION_ON)) {
				service.encryptionOff(info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ENCRYPTION_OFF)) {
				service.encryptionOn(info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ADD_PUBLIC_KEY)) {
				processAddPublicKeyFeature(featureId, info, service);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_REMOVE_PUBLIC_KEY)) {
				service.removeBuddyPGPKey(info);
			}
		}

		@Override
		public void disconnect(byte serviceId) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.disconnect();
		}

		@Override
		public void connect(OnlineInfo info) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(info.getServiceId());
			
			if (service.getCurrentState() != ConnectionState.DISCONNECTED) {
				return;
			}
			
			try {
				service.connect(info);
			} catch (ProtocolException e) {
				Logger.log(e);
			}
		}

		@Override
		public long sendMessage(Message message) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(message.getServiceId());
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return 0;
			}
			
			if (message instanceof FileMessage) {
				return service.sendFile((FileMessage) message);
			} else if (message instanceof TextMessage) {
				return service.sendMessage((TextMessage) message);
			} else {
				return 0;
			}			
		}

		@Override
		public void requestIcon(byte serviceId, String ownerUid) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.loadCard(ownerUid);
		}

		@Override
		public void messageResponse(Message message, boolean accept) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(message.getServiceId());
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			if (message instanceof FileMessage){
				service.fileTransferResponse((FileMessage)message, accept);
			} else if (message instanceof ServiceMessage) {
				if (message.getContactDetail().equals(getBaseContext().getString(R.string.ask_authorization))) {
					service.authorizationResponse(message.getContactUid(), accept);
				} else if (message.getContactDetail().equals(getBaseContext().getString(R.string.confirm_destroy_room)) && accept) {
					service.destroyChatRoom(message.getContactUid());
				}
			}			
		}

		@Override
		public void cancelFileFransfer(byte serviceId, long messageId) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.cancelTransfer(messageId);
		}

		@Override
		public void sendTypingNotification(byte serviceId, String ownerUid) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.sendTyping(ownerUid);
		}

		@Override
		public void getChatRooms(byte serviceId) throws RemoteException {
			
		}

		@Override
		public void uploadAccountPhoto(byte serviceId, String filePath) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.uploadIcon(Utils.scaleAccountIcon(filePath, 120));
		}

		@Override
		public void removeAccountPhoto(byte serviceId) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			if (service.getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			service.uploadIcon(null);
		}

		@Override
		public ProtocolServiceFeature[] getProtocolFeatures() throws RemoteException {
			return ResourceUtils.getFeatures(getBaseContext());
		}

		@Override
		public List<ProtocolOption> getProtocolOptions() throws RemoteException {
			if (OPTIONS == null) {
				OPTIONS = new ProtocolOption[]{ 
						new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_JID, null, R.string.jid, true),
						new ProtocolOption(ProtocolOptionType.PASSWORD, ResourceUtils.KEY_PASSWORD, null, R.string.password, true), 
						new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_SERVER_HOST, XMPPApiConstants.DEFAULT_HOST, R.string.server_host, true),
						new ProtocolOption(ProtocolOptionType.INTEGER, ResourceUtils.KEY_SERVER_PORT, XMPPApiConstants.DEFAULT_PORT, R.string.server_port, true), 
						new ProtocolOption(ProtocolOptionType.CHECKBOX, ResourceUtils.KEY_SECURE_CONNECTION, "true", R.string.label_secure_connection, false),
						new ProtocolOption(ProtocolOptionType.LIST, ResourceUtils.KEY_PROXY_TYPE, null, R.string.proxy_type, false, null, getBaseContext().getResources().getStringArray(R.array.xmpp_proxy_names)),
						new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_PROXY_HOST, null, R.string.proxy_host, false),
						new ProtocolOption(ProtocolOptionType.INTEGER, ResourceUtils.KEY_PROXY_PORT, null, R.string.proxy_port, false),
						new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_PROXY_USERNAME, null, R.string.proxy_username, false),
						new ProtocolOption(ProtocolOptionType.PASSWORD, ResourceUtils.KEY_PROXY_PASSWORD, null, R.string.proxy_password, false),
						new ProtocolOption(ProtocolOptionType.FILE, ResourceUtils.KEY_PRIVATEKEY_FILE, null, R.string.pgp_key_file, false, null, "*.asc"),
						new ProtocolOption(ProtocolOptionType.PASSWORD, ResourceUtils.KEY_PRIVATEKEY_PASSWORD, null, R.string.pgp_key_password, false),						
						new ProtocolOption(ProtocolOptionType.INTEGER, ResourceUtils.KEY_PING_TIMEOUT, "200", R.string.ping_timeout, false),
				};
			}
			return Arrays.asList(OPTIONS);
		}

		@Override
		public String getProtocolName() throws RemoteException {
			return XMPPProtocol.this.getProtocolName();
		}

		@Override
		public void joinChatRoom(byte serviceId, String chatId) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			InputFormFeature feature = ResourceUtils.getAddGroupchatFeature(getBaseContext());
			
			for (TKV tkv : feature.getEditorFields()) {
				if (tkv.getKey().equals(getBaseContext().getString(R.string.host))) {
					tkv.setValue(StringUtils.parseServer(chatId));
				} else if (tkv.getKey().equals(getBaseContext().getString(R.string.chat_room_id))) {
					tkv.setValue(StringUtils.parseName(chatId));
				} else if (tkv.getKey().equals(getBaseContext().getString(R.string.nickname))) {
					tkv.setValue(service.getOnlineInfo().getName());
				}
			}
			
			getCallback().showFeatureInputForm(serviceId, chatId, feature);
		}

		@Override
		public void leaveChatRoom(byte serviceId, String chatId) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			service.leaveChat(chatId);
		}

		@Override
		public void getChatRoomOccupants(byte serviceId, String chatId, boolean loadOccupantIcons) throws RemoteException {
			XMPPService service = (XMPPService) findAccountServiceById(serviceId);
			
			
		}
		
	};

	public XMPPProtocol() {
		super();
		setMainService(stub);
	}

	private void processAddPublicKeyFeature(String featureId, OnlineInfo info, XMPPService service) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String pgpKey = getBaseContext().getString(R.string.pgp_key_file);
		
		String pgp = null;
		
		for (Parcelable pp : p) {
			TKV tkv = (TKV) pp;
			if (tkv.getKey().equals(pgpKey)) {
				pgp = tkv.getValue();
			} 
		}
		
		if (TextUtils.isEmpty(pgp)) {
			Logger.log("Empty PGP value in request", LoggerLevel.INFO);
		} else {
			service.addBuddyPGPKey(info, pgp);
		}
	}

	private void processAddBuddyFeature(String featureId, OnlineInfo info, XMPPService service) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String jidKey = getBaseContext().getString(R.string.jid);
		String nicknameKey = getBaseContext().getString(R.string.nickname);
		
		String jid = null;
		String nickname = null;
		
		for (Parcelable pp : p) {
			TKV tkv = (TKV) pp;
			if (tkv.getKey().equals(nicknameKey)) {
				nickname = tkv.getValue();
			} else if (tkv.getKey().equals(jidKey)) {
				jid = tkv.getValue();
			}
		}
		
		if (TextUtils.isEmpty(jid)) {
			Logger.log("Empty JID value in add buddy request", LoggerLevel.INFO);
		} else {
			service.addBuddy(jid, nickname);
		}
	}

	private void processChatRoomResultFeature(String featureId, OnlineInfo info, XMPPService service) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		Map<String, String> values = new HashMap<String, String>();
		
		for (Parcelable pp : p) {
			TKV tkv = (TKV) pp;
			values.put(tkv.getKey(), tkv.getValue());
		}
		
		service.chatRoomConfiguration(info.getProtocolUid(), values);
	}

	private void processAddGroupchatFeature(String featureId, OnlineInfo info, XMPPService service) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String actionKey = getBaseContext().getString(R.string.chat_action);
		String hostKey = getBaseContext().getString(R.string.host);
		String chatKey = getBaseContext().getString(R.string.chat_room_id);
		String nicknameKey = getBaseContext().getString(R.string.nickname);
		String passwordKey = getBaseContext().getString(R.string.password);
		
		String action = null;
		String host = null;
		String chat = null;
		String nickname = null;
		String password = null;
		
		for (Parcelable pp : p) {
			TKV tkv = (TKV) pp;
			if (tkv.getKey().equals(nicknameKey)) {
				nickname = tkv.getValue();
			} else if (tkv.getKey().equals(chatKey)) {
				chat = tkv.getValue();
			} else if (tkv.getKey().equals(passwordKey)) {
				password = tkv.getValue();
			} else if (tkv.getKey().equals(hostKey)) {
				host = tkv.getValue();
			} else if (tkv.getKey().equals(actionKey)) {
				action = tkv.getValue();
			}
		}
		
		if (TextUtils.isEmpty(host) || TextUtils.isEmpty(chat)) {
			Logger.log("Empty host or chat value in add buddy request", LoggerLevel.INFO);
		} else {
			service.joinChat(host, chat, nickname, password, action != null && action.equals(getBaseContext().getString(R.string.create_new_chat)));
		}
	}

	@Override
	protected AccountService createService(byte serviceId, String protocolUid) {
		XMPPService service = new XMPPService(serviceId, protocolUid, this);
		return service;
	}

	@Override
	protected String getProtocolName() {
		return XMPPApiConstants.PROTOCOL_NAME;
	}

}
