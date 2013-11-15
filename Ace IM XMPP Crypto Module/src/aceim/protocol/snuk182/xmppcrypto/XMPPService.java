package aceim.protocol.snuk182.xmppcrypto;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolException.Cause;
import aceim.api.service.ProtocolService;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Privacy;
import org.jivesoftware.smack.packet.PrivacyItem;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.EncryptedDataProvider;
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

import aceim.protocol.snuk182.xmppcrypto.utils.ResourceUtils;
import android.content.SharedPreferences.Editor;
import android.os.RemoteException;

public class XMPPService extends AccountService implements ConnectionListener {

	private final OnlineInfo mMyOnlineInfo = new OnlineInfo(getServiceId(), getProtocolUid());

	private XMPPConnection connection;

	private EncryptedDataProvider edProvider = null;
	private final XMPPRosterListener mRosterListener = new XMPPRosterListener(this);
	/**
	 * @return the mRosterListener
	 */
	XMPPRosterListener getRosterListener() {
		return mRosterListener;
	}

	/**
	 * @return the mChatListener
	 */
	XMPPChatListener getChatListener() {
		return mChatListener;
	}

	/**
	 * @return the mFileTransferListener
	 */
	XMPPFileTransferListener getFileTransferListener() {
		return mFileTransferListener;
	}

	private final XMPPChatListener mChatListener = new XMPPChatListener(this);
	private final XMPPFileTransferListener mFileTransferListener = new XMPPFileTransferListener(this);
	
	private boolean secureConnection = false;

	private ServiceDiscoveryManager mServiceDiscoveryManager;

	public XMPPService(byte serviceId, String protocolUid, ProtocolService protocolService) {
		super(serviceId, protocolUid, protocolService);
	}

	@Override
	protected ConnectionState getCurrentState() {
		return (connection != null && connection.isConnected()) ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED;
	}

	@Override
	protected void keepaliveRequest() {
		if (mServiceDiscoveryManager == null || getCurrentState() != ConnectionState.CONNECTED) {
			return;
		}

		try {
			DiscoverItems discoItems = mServiceDiscoveryManager.discoverItems(connection.getServiceName());
			resetHeartbeat();

			Iterator<Item> it = discoItems.getItems();
			while (it.hasNext()) {
				DiscoverItems.Item item = (DiscoverItems.Item) it.next();
				log("Service available:" + item.getEntityID() + " " + item.getName());
			}
		} catch (XMPPException e) {
			log(e);
		}
	}

	@Override
	protected void timeoutDisconnect() {
		closeKeepaliveThread();
		disconnect();
	}

	public void onXmppException(XMPPException e) {
		Logger.log(e);
		try {
			getProtocolService().getCallback().notification(getServiceId(), ResourceUtils.xmppExceptionToString(e));
		} catch (RemoteException e1) {
			Logger.log(e1);
		}
	}

	public void connect(OnlineInfo info) throws ProtocolException, RemoteException {
		
		final String jid = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_JID);
		final String password = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PASSWORD);
		final String host = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_SERVER_HOST);
		final String port = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_SERVER_PORT);
		String ping = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PING_TIMEOUT);

		String proxyType = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PROXY_TYPE);
		String proxyHost = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PROXY_HOST);
		String proxyPort = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PROXY_PORT);
		String proxyUsername = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PROXY_USERNAME);
		String proxyPassword = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PASSWORD);
		
		String isSecure = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_SECURE_CONNECTION);
		
		if (ping != null){
			try {
				pingTimeout = Integer.parseInt(ping);
			} catch (Exception e) {
				Logger.log(e);
			}
		}
		
		if (jid == null || password == null){
			throw new ProtocolException(Cause.BROKEN_AUTH_DATA);
		}	
		
		final ProxyInfo proxyInfo;
		if (proxyHost != null && proxyPort != null && proxyHost.length() > 0 && proxyPort.length() > 0 && proxyType != null && !proxyType.equalsIgnoreCase("none")) {
			proxyInfo = new ProxyInfo(getProxyType(proxyType), proxyHost, Integer.parseInt(proxyPort.trim().replaceAll("\n", "")), proxyUsername, proxyPassword);
		} else {
			proxyInfo = null;
		}
		
		//this.onlineInfo.getFeatures()
		if (isSecure != null) {
			try {
				secureConnection = Boolean.parseBoolean(isSecure);
			} catch (Exception e) {
				Logger.log(e);
			}
		}
		
		mMyOnlineInfo.merge(info);
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				
				String un;
				String serviceName;
				
				if (jid.contains("@")) {
					String[] jidParams = jid.split("@");
					un = jidParams[0];
					serviceName = jidParams[1];
				} else {
					un = jid;
					serviceName = host;
				}
				
				SmackConfiguration.setPacketReplyTimeout(120000);
				ConnectionConfiguration config;

				if (proxyInfo != null) {
					config = new ConnectionConfiguration(host, Integer.parseInt(port), proxyInfo);
				} else {
					config = new ConnectionConfiguration(host, Integer.parseInt(port));
				}

				String login;

				if (isGmail(serviceName) && secureConnection) {
					SASLAuthentication.supportSASLMechanism("PLAIN", 0);
					login = mMyOnlineInfo.getProtocolUid();
				} else {
					login = un;
				}

				config.setSASLAuthenticationEnabled(secureConnection);

				config.setServiceName(serviceName);
				
				try {
					configure(ProviderManager.getInstance());
					connection = new XMPPConnection(config);
					mRosterListener.setContactListReady(false);
					
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 1);
					connection.connect();
					
					mServiceDiscoveryManager = new ServiceDiscoveryManager(connection);
					
					Roster roster = connection.getRoster();
					roster.setSubscriptionMode(SubscriptionMode.manual);
					
					//Attempting to fix buddy presence changes missing
					Thread.sleep(1000);
					
					roster.addRosterListener(mRosterListener);
					
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 2);					
					
					connection.login(login, password, getProtocolService().getString(R.string.app_name));
					
					connection.addPacketListener(mRosterListener, mRosterListener);
					
					connection.addConnectionListener(XMPPService.this);
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 3);
					
					setStatus(mMyOnlineInfo.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) 0));
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 4);
					
					while (!isRosterInitialized(roster)) {
						Logger.log("Roster not ready", LoggerLevel.VERBOSE);
						Thread.sleep(1000);
					}
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 6);
					
					List<BuddyGroup> groups = mRosterListener.getContactList();
					connection.getChatManager().addChatListener(mChatListener);
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 7);
					try {
						List<MultiChatRoom> joinedChats = mChatListener.getJoinedChatRooms();
						XMPPEntityAdapter.addGroupChats(groups, joinedChats, jid, getServiceId());
					} catch (Exception e) {
						log(e);
					}
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 8);
					getProtocolService().getCallback().buddyListUpdated(getServiceId(), groups);
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 9);
					fillFeatures();
					getProtocolService().getCallback().accountStateChanged(mMyOnlineInfo);
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTED, 0);
					
					mRosterListener.getBuddyInfo(jid, true, false);
					
					mChatListener.setMessageEventManager(new MessageEventManager(connection));
					
					FileTransferManager ftm = new FileTransferManager(connection);
					ftm.addFileTransferListener(mFileTransferListener);
					mFileTransferListener.setFileTransferManager(ftm);
					
					sendKeepalive();
					
					mRosterListener.setContactListReady(true);
					mRosterListener.checkCachedInfos();
				} catch (Exception e) {
					log(e);
					connection = null;
					connectionClosedOnError(e);
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}
	
	private void fillFeatures() {
		if (!mMyOnlineInfo.getFeatures().containsKey(ApiConstants.FEATURE_STATUS)) {
			mMyOnlineInfo.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
		}
		
		mMyOnlineInfo.getFeatures().putBoolean(ApiConstants.FEATURE_BUDDY_MANAGEMENT, true);
		mMyOnlineInfo.getFeatures().putBoolean(ApiConstants.FEATURE_GROUP_MANAGEMENT, true);
		mMyOnlineInfo.getFeatures().putBoolean(ApiConstants.FEATURE_ACCOUNT_MANAGEMENT, true);
		mMyOnlineInfo.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ADD_BUDDY, true);
		try {
			if (mChatListener.hasGroupchatSupport()) {
				mMyOnlineInfo.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ADD_GROUPCHAT, true);
				mMyOnlineInfo.getFeatures().putBoolean(XMPPApiConstants.FEATURE_GROUPCHATS, true);
			} else {
				mMyOnlineInfo.getFeatures().remove(XMPPApiConstants.FEATURE_GROUPCHATS);
				mMyOnlineInfo.getFeatures().remove(XMPPApiConstants.FEATURE_ADD_GROUPCHAT);
			}
		} catch (XMPPException e) {
			mMyOnlineInfo.getFeatures().remove(XMPPApiConstants.FEATURE_GROUPCHATS);
			mMyOnlineInfo.getFeatures().remove(XMPPApiConstants.FEATURE_ADD_GROUPCHAT);
		}
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		log("Connection closed " + getProtocolUid());
		log(e);
		closeKeepaliveThread();
		mRosterListener.setContactListReady(false);
		try {
			getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.DISCONNECTED, -1);
			if ((e instanceof IOException) || (e instanceof XMPPException && ((XMPPException) e).getXMPPError() != null && !((XMPPException) e).getXMPPError().getCondition().equals("remote-server-timeout"))) {
				getProtocolService().getCallback().notification(getServiceId(), e.getLocalizedMessage());				
			} 
		} catch (RemoteException e1) {
			log(e1);
		}
	}
	
	private boolean isRosterInitialized(Roster roster) {
		try {
			Field field = Roster.class.getDeclaredField("rosterInitialized");
	        field.setAccessible(true);	        
			return (Boolean) field.get(roster);
		} catch (Exception e) {
			Logger.log(e);
			return true;
		}
	}

	private void configure(ProviderManager pm) throws RemoteException {
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
			log(e);
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
		
		String pgpKey = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PRIVATEKEY_FILE);
		String pgpKeyPassword = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PRIVATEKEY_PASSWORD);
		
		if (pgpKey != null && pgpKeyPassword != null){
			edProvider = new EncryptedDataProvider();
			edProvider.setMyKey(pgpKey);
			edProvider.setMyKeyPw(pgpKeyPassword);
			pm.addExtensionProvider("x", "jabber:x:signed", edProvider);
			pm.addExtensionProvider("x", "jabber:x:encrypted", edProvider);		
			
			Security.addProvider(edProvider.getProvider());			
		} else {
			edProvider = null;
			pm.removeExtensionProvider("x", "jabber:x:signed");
			pm.removeExtensionProvider("x", "jabber:x:encrypted");
		}
	}

	private boolean isGmail(String serviceName) {
		return serviceName.equals("gmail.com") || serviceName.equals("googlemail.com");
	}

	private ProxyType getProxyType(String proxyType) {
		if (proxyType.equalsIgnoreCase("http")) {
			return ProxyType.HTTP;
		} else if (proxyType.equalsIgnoreCase("socks4")) {
			return ProxyType.SOCKS4;
		} else if (proxyType.equalsIgnoreCase("socks5")) {
			return ProxyType.SOCKS5;
		}
		return ProxyType.NONE;
	}
	
	@Override
	public void connectionClosed() {
		log("Connection closed " + getProtocolUid());
		closeKeepaliveThread();
		mRosterListener.setContactListReady(false);
		try {
			getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.DISCONNECTED, -1);
		} catch (RemoteException e) {
			Logger.log(e);
		}
		
	}

	@Override
	public void reconnectingIn(int seconds) {
		log("Connection reconnect " + getProtocolUid() + " in " + seconds);
		try {
			getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, 1);
		} catch (RemoteException e) {
			Logger.log(e);
		}
		
	}

	@Override
	public void reconnectionSuccessful() {
		log("Reconnected " + getProtocolUid());
		try {
			getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTED, -1);
		} catch (RemoteException e) {
			Logger.log(e);
		}		
	}

	@Override
	public void reconnectionFailed(Exception e) {
		log("Reconnection failed " + getProtocolUid());
		connectionClosedOnError(e);
	}

	/**
	 * @return the edProvider
	 */
	public EncryptedDataProvider getEdProvider() {
		return edProvider;
	}

	/**
	 * @return the connection
	 */
	public XMPPConnection getConnection() {
		return connection;
	}

	/**
	 * @return the onlineInfo
	 */
	public OnlineInfo getOnlineInfo() {
		return mMyOnlineInfo;
	}

	public long sendFile(FileMessage message) {
		List<File> files = new ArrayList<File>(message.getFiles().size());
		for (FileInfo info : message.getFiles()) {
			files.add(new File(info.getFilename()));
		}
		
		return mFileTransferListener.sendFile(message.getContactUid() + "/" + message.getContactDetail(), files);		
	}

	public void fileTransferResponse(FileMessage message, boolean accept) {
		mFileTransferListener.fileRespond(message, accept);
	}

	public void disconnect() {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				connection.getChatManager().removeChatListener(mChatListener);
				connection.getRoster().removeRosterListener(mRosterListener);
				connection.removeConnectionListener(XMPPService.this);
				connection.removePacketListener(mRosterListener);
				
				connection.disconnect();
				
				mChatListener.onDisconnect();
				mRosterListener.onDisconnect();
				mFileTransferListener.onDisconnect();
				
				try {
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.DISCONNECTED, 0);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}).start();
	}

	public void getBuddyInfo(String jid, boolean shortInfo) {
		mRosterListener.getBuddyInfo(jid, shortInfo, false);
	}

	public void buddyAction(ItemAction action, Buddy buddy) {
		switch(action) {
		case ADDED:
			mRosterListener.addBuddy(buddy);
			break;
		case DELETED:
			if (buddy instanceof MultiChatRoom) {
				mChatListener.leaveChat(buddy.getProtocolUid());
				try {
					getProtocolService().getCallback().buddyAction(ItemAction.DELETED, buddy);
				} catch (RemoteException e1) {
					Logger.log(e1);
				}
			} else {
				mRosterListener.removeBuddy(buddy);
			}
			break;
		case MODIFIED:
			RosterEntry e = connection.getRoster().getEntry(buddy.getProtocolUid());
			
			if (!e.getName().equals(buddy.getName())) {
				mRosterListener.renameBuddy(buddy);				
			}
			if (e.getGroups().size() < 1 || (e.getGroups().size() > 0 && !e.getGroups().iterator().next().getName().equals(buddy.getGroupId()))) {
				mRosterListener.moveBuddy(buddy);
			}
			break;
		default:
			Logger.log("Unsupported buddy action: "+action, LoggerLevel.INFO);
			break;
		}
	}

	public void loadCard(String buddyId) {
		mRosterListener.loadCard(buddyId);
	}

	public void groupAction(ItemAction action, BuddyGroup group) {
		switch(action) {
		case ADDED:
			mRosterListener.addGroup(group);
			break;
		case DELETED:
			mRosterListener.removeGroup(group);
			break;
		case MODIFIED:
			mRosterListener.renameGroup(group);
			break;
		default:
			Logger.log("Unsupported group action: "+action, LoggerLevel.INFO);
			break;
		}
	}

	public void setStatus(byte statusId) {
		if (!connection.isConnected()) {
			return;
		}
		
		//TODO add full visibility list control support
		if (statusId == XMPPEntityAdapter.INVISIBLE_STATUS_ID) {
			Privacy privacy1 = new Privacy();
			
			PrivacyItem item = new PrivacyItem(null, false, 1);
			item.setFilterPresence_out(true);
			
			privacy1.setPrivacyList("invisible", Arrays.asList(new PrivacyItem[]{ item }));
			connection.sendPacket(privacy1);
			
			Privacy privacy2 = new Privacy();
			privacy2.setActiveName("invisible");
			privacy2.setType(Type.SET);
			connection.sendPacket(privacy2);
		}
		
		Presence presence = XMPPEntityAdapter.userStatus2XMPPPresence(statusId, getEdProvider());
		connection.sendPacket(presence);
	}

	public long sendMessage(TextMessage message) {
		try {
			return mChatListener.sendMessage(message);
		} catch (Exception e) {
			Logger.log(e);
			try {
				getProtocolService().getCallback().notification(message.getServiceId(), e.getLocalizedMessage());
			} catch (RemoteException e1) {
				Logger.log(e1);
			}
			return 0;
		}
	}

	public void cancelTransfer(long messageId) {
		mFileTransferListener.cancel(messageId);
	}

	public void sendTyping(String ownerUid) {
		mChatListener.sendTyping(ownerUid);
	}

	public void uploadIcon(byte[] bytes) {
		mRosterListener.uploadIcon(bytes);
	}

	public void addBuddy(String jid, String nickname) {
		Buddy buddy = new Buddy(jid, getProtocolUid(), XMPPApiConstants.PROTOCOL_NAME, getServiceId());
		buddy.setName(nickname);
		buddy.setGroupId(ApiConstants.NO_GROUP_ID);
		mRosterListener.addBuddy(buddy);
	}

	public void authorizationResponse(String contactUid, boolean accept) {
		mRosterListener.authorizationResponse(contactUid, accept);
	}

	public void requestAvailableGroupchats() {
		mChatListener.requestAvailableGroupchats();
	}

	public void leaveChat(String chatId) {
		mChatListener.leaveChat(chatId);
	}

	public void joinChat(String host, String chat, String nickname, String password, boolean createChat) {
		mChatListener.joinChat(host, chat, nickname, password, createChat);
	}

	public void getChatConfigurationForm(String protocolUid) {
		mChatListener.getChatConfigurationForm(protocolUid);
	}

	public void chatRoomConfiguration(String protocolUid, Map<String, String> values) {
		mChatListener.chatRoomConfiguration(protocolUid, values);
	}

	public void destroyChatRoom(String contactUid) {
		mChatListener.destroyChatRoom(contactUid);
	}

	public void encryptionOn(OnlineInfo info) {
		String buddyPGPKey;
		
		if (edProvider != null && (buddyPGPKey = getProtocolService().getSharedPreferences(getProtocolUid(), 0).getString(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid(), null)) != null) {
			edProvider.getKeyStorage().put(info.getProtocolUid(), buddyPGPKey);
		}
		
		info.getFeatures().remove(XMPPApiConstants.FEATURE_ENCRYPTION_OFF);
		info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ENCRYPTION_ON, true);
		
		mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		try {
			getProtocolService().getCallback().buddyStateChanged(info);
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	public void encryptionOff(OnlineInfo info) {
		if (edProvider != null) {
			edProvider.getKeyStorage().put(info.getProtocolUid(), null);
		}
		
		info.getFeatures().remove(XMPPApiConstants.FEATURE_ENCRYPTION_ON);
		info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ENCRYPTION_OFF, true);
		
		mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		try {
			getProtocolService().getCallback().buddyStateChanged(info);
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	public void addBuddyPGPKey(OnlineInfo info, String pgp) {
		Editor e = getProtocolService().getSharedPreferences(getProtocolUid(), 0).edit();
		e.putString(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid(), pgp);
		e.commit();
		
		info.getFeatures().remove(XMPPApiConstants.FEATURE_ADD_PUBLIC_KEY);
		info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ENCRYPTION_OFF, true);
		info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_REMOVE_PUBLIC_KEY, true);
		
		mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		try {
			getProtocolService().getCallback().buddyStateChanged(info);
		} catch (RemoteException e1) {
			onRemoteException(e1);
		}
	}

	public void removeBuddyPGPKey(OnlineInfo info) {
		Editor e = getProtocolService().getSharedPreferences(getProtocolUid(), 0).edit();
		e.remove(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid());
		e.commit();
		
		info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ADD_PUBLIC_KEY, true);
		info.getFeatures().remove(XMPPApiConstants.FEATURE_ENCRYPTION_OFF);
		info.getFeatures().remove(XMPPApiConstants.FEATURE_ENCRYPTION_ON);
		info.getFeatures().remove(XMPPApiConstants.FEATURE_REMOVE_PUBLIC_KEY);
		
		mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		try {
			getProtocolService().getCallback().buddyStateChanged(mMyOnlineInfo);
		} catch (RemoteException e1) {
			onRemoteException(e1);
		}
	}
}