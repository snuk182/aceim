package aceim.protocol.snuk182.xmpp.common;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

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
import aceim.protocol.snuk182.xmpp.common.utils.ResourceUtils;
import android.content.Context;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;

public abstract class XMPPCommonService extends AccountService {

	protected final XMPPServiceInternal internal = new XMPPServiceInternal(this);
	private XMPPEntityAdapter entityAdapter;

	protected XMPPEntityAdapter initEntityAdapter() {
		return new XMPPEntityAdapter();
	}

	public XMPPCommonService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}
	
	public XMPPEntityAdapter getEntityAdapter() {
		if (entityAdapter == null) {
			entityAdapter = initEntityAdapter();
		}
		
		return entityAdapter;
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
			
			doSetFeature(featureId, info);
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
	
	protected void doSetFeature(String featureId, OnlineInfo info) {
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

	public void configure(ProviderManager pm) throws RemoteException {
		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			// Not sure what's happening here.
		}

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
		pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());

		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e) {
			Logger.log(e);
		}

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());
		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());
		// Chat State
		pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
		pm.addIQProvider("open", "http://jabber.org/protocol/ibb", new OpenIQProvider());
		pm.addIQProvider("close", "http://jabber.org/protocol/ibb", new CloseIQProvider());
		pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb", new DataPacketProvider());
		
		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference", new GroupChatInvitation.Provider());
		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());
		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
		// Offline Message Indicator
		pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
	}
}
