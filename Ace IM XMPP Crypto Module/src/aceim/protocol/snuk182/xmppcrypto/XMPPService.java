package aceim.protocol.snuk182.xmppcrypto;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import aceim.api.IProtocol;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.ProtocolException;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;
import aceim.protocol.snuk182.xmppcrypto.utils.ResourceUtils;
import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;

public class XMPPService extends AccountService {
	
	private final XMPPServiceInternal internal = new XMPPServiceInternal(this);

	public XMPPService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}

	@Override
	protected ConnectionState getCurrentState() {
		return (internal.getConnection() != null && internal.getConnection().isConnected()) ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED;
	}

	@Override
	public IProtocol getProtocol() {
		return mProtocol;
	}

	@Override
	protected void keepaliveRequest() {
		kaRequestInternal();
	}

	

	@Override
	protected void timeoutReconnect() {
		closeKeepaliveThread();
		internal.disconnect();
		try {
			internal.connect(null);
		} catch (ProtocolException e) {
			Logger.log(e);
		}
	}
	
	private final IProtocol mProtocol = new IProtocol() {
		
		@Override
		public void requestFullInfo(String uid, boolean shortInfo) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.getBuddyInfo(uid, shortInfo);
		}

		@Override
		public void buddyAction(ItemAction action, Buddy buddy) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.buddyAction(action, buddy);
		}

		@Override
		public void buddyGroupAction(ItemAction action, BuddyGroup group) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.groupAction(action, group);
		}

		@Override
		public void setFeature(String featureId, OnlineInfo info) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			if (featureId.equals(ApiConstants.FEATURE_STATUS)) {
				internal.setStatus(info.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) 0));
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ADD_BUDDY)) {
				processAddBuddyFeature(featureId, info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_GROUPCHATS)) {
				internal.requestAvailableGroupchats();
			} else if (featureId.equals(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION)) {
				internal.getChatConfigurationForm(info.getProtocolUid());
			} else if (featureId.equals(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_RESULT)) {
				processChatRoomResultFeature(featureId, info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ADD_GROUPCHAT)) {
				processAddGroupchatFeature(featureId, info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_DESTROY_CHAT_ROOM)) {
				ServiceMessage message = new ServiceMessage(getServiceId(), info.getProtocolUid(), true);
				message.setIncoming(true);
				message.setContactDetail(getContext().getString(R.string.confirm_destroy_room, info.getProtocolUid()));
				getCoreService().message(message);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ENCRYPTION_ON)) {
				internal.encryptionOff(info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ENCRYPTION_OFF)) {
				internal.encryptionOn(info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_ADD_PUBLIC_KEY)) {
				processAddPublicKeyFeature(featureId, info);
			} else if (featureId.equals(XMPPApiConstants.FEATURE_REMOVE_PUBLIC_KEY)) {
				internal.removeBuddyPGPKey(info);
			}
		}

		@Override
		public void disconnect() {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.disconnect();
		}

		@Override
		public void connect(OnlineInfo info) {
			if (getCurrentState() != ConnectionState.DISCONNECTED) {
				return;
			}
			
			try {
				String ping = getCoreService().requestPreference( ResourceUtils.KEY_PING_TIMEOUT);

				if (ping != null){
					try {
						pingTimeout = Integer.parseInt(ping);
					} catch (Exception e) {
						Logger.log(e);
					}
				}
				
				internal.connect(info);
			} catch (ProtocolException e) {
				Logger.log(e);
			}
		}

		@Override
		public long sendMessage(Message message) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return 0;
			}
			
			if (message instanceof FileMessage) {
				return internal.sendFile((FileMessage) message);
			} else if (message instanceof TextMessage) {
				return internal.sendMessage((TextMessage) message);
			} else {
				return 0;
			}			
		}

		@Override
		public void requestIcon(String ownerUid) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.loadCard(ownerUid);
		}

		@Override
		public void messageResponse(Message message, boolean accept) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			if (message instanceof FileMessage){
				internal.fileTransferResponse((FileMessage)message, accept);
			} else if (message instanceof ServiceMessage) {
				if (message.getContactDetail().equals(getContext().getString(R.string.ask_authorization))) {
					internal.authorizationResponse(message.getContactUid(), accept);
				} else if (message.getContactDetail().equals(getContext().getString(R.string.confirm_destroy_room)) && accept) {
					internal.destroyChatRoom(message.getContactUid());
				}
			}			
		}

		@Override
		public void cancelFileFransfer(long messageId) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.cancelTransfer(messageId);
		}

		@Override
		public void sendTypingNotification(String ownerUid) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.sendTyping(ownerUid);
		}

		@Override
		public void uploadAccountPhoto(String filePath) {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.uploadIcon(Utils.scaleAccountIcon(filePath, 120));
		}

		@Override
		public void removeAccountPhoto() {
			if (getCurrentState() != ConnectionState.CONNECTED) {
				return;
			}
			
			internal.uploadIcon(null);
		}

		@Override
		public void joinChatRoom(String chatId, boolean getOccupantsIcons) {
			InputFormFeature feature = ResourceUtils.getAddGroupchatFeature(getContext());
			
			for (TKV tkv : feature.getEditorFields()) {
				if (tkv.getKey().equals(getContext().getString(R.string.host))) {
					tkv.setValue(StringUtils.parseServer(chatId));
				} else if (tkv.getKey().equals(getContext().getString(R.string.chat_room_id))) {
					tkv.setValue(StringUtils.parseName(chatId));
				} else if (tkv.getKey().equals(getContext().getString(R.string.nickname))) {
					tkv.setValue(internal.getOnlineInfo().getName());
				}
			}
			
			getCoreService().showFeatureInputForm(chatId, feature);
		}

		@Override
		public void leaveChatRoom(String chatId) {
			internal.leaveChat(chatId);
		}

	};

	private void kaRequestInternal() {
		if (getCurrentState() != ConnectionState.CONNECTED) {
			return;
		}

		try {
			internal.serviceDiscoveryRequest();
			resetHeartbeat();
		} catch (XMPPException e) {
			Logger.log(e);
		}
	}
	
	private void processAddPublicKeyFeature(String featureId, OnlineInfo info) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String pgpKey = getContext().getString(R.string.pgp_key_file);
		
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
			internal.addBuddyPGPKey(info, pgp);
		}
	}

	private void processAddBuddyFeature(String featureId, OnlineInfo info) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String jidKey = getContext().getString(R.string.jid);
		String nicknameKey = getContext().getString(R.string.nickname);
		
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
			internal.addBuddy(jid, nickname);
		}
	}

	private void processChatRoomResultFeature(String featureId, OnlineInfo info) {
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
		
		internal.chatRoomConfiguration(info.getProtocolUid(), values);
	}

	private void processAddGroupchatFeature(String featureId, OnlineInfo info) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String actionKey = getContext().getString(R.string.chat_action);
		String hostKey = getContext().getString(R.string.host);
		String chatKey = getContext().getString(R.string.chat_room_id);
		String nicknameKey = getContext().getString(R.string.nickname);
		String passwordKey = getContext().getString(R.string.password);
		
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
			internal.joinChat(host, chat, nickname, password, action != null && action.equals(getContext().getString(R.string.create_new_chat)));
		}
	}
}
