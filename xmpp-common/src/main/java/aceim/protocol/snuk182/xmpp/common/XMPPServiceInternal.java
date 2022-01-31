package aceim.protocol.snuk182.xmpp.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

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
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolException.Cause;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.protocol.snuk182.xmpp.common.utils.ResourceUtils;
import android.text.TextUtils;

public class XMPPServiceInternal implements ConnectionListener {
	
	public XMPPServiceInternal(XMPPCommonService service) {
		this.service = service;
		this.mMyOnlineInfo = new OnlineInfo(service.getServiceId(), service.getProtocolUid());
	}

	private final XMPPCommonService service;

	private final OnlineInfo mMyOnlineInfo;

	private XMPPConnection connection;

	private final XMPPRosterListener mRosterListener = new XMPPRosterListener(this);
	private final XMPPChatListener mChatListener = new XMPPChatListener(this);
	private final XMPPFileTransferListener mFileTransferListener = new XMPPFileTransferListener(this);
	
	private boolean secureConnection = true;

	private ServiceDiscoveryManager mServiceDiscoveryManager;

	
	public void onXmppException(XMPPException e) {
		Logger.log(e);
		getService().getCoreService().notification( ResourceUtils.xmppExceptionToString(e));
	}

	public void connect(OnlineInfo info) throws ProtocolException {
		
		final String jid = getService().getCoreService().requestPreference( ResourceUtils.KEY_JID);
		final String password = getService().getCoreService().requestPreference( ResourceUtils.KEY_PASSWORD);
		final String host = getService().getCoreService().requestPreference( ResourceUtils.KEY_SERVER_HOST);
		final String port = getService().getCoreService().requestPreference( ResourceUtils.KEY_SERVER_PORT);
		
		String proxyType = getService().getCoreService().requestPreference( ResourceUtils.KEY_PROXY_TYPE);
		String proxyHost = getService().getCoreService().requestPreference( ResourceUtils.KEY_PROXY_HOST);
		String proxyPort = getService().getCoreService().requestPreference( ResourceUtils.KEY_PROXY_PORT);
		String proxyUsername = getService().getCoreService().requestPreference( ResourceUtils.KEY_PROXY_USERNAME);
		String proxyPassword = getService().getCoreService().requestPreference( ResourceUtils.KEY_PASSWORD);
		
		String isSecure = getService().getCoreService().requestPreference( ResourceUtils.KEY_SECURE_CONNECTION);
		
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
		
		if (info != null) {
			
		}
		
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
					getService().configure(ProviderManager.getInstance());
					connection = new XMPPConnection(config);
					mRosterListener.setContactListReady(false);
					
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 1);
					connection.connect();
					
					mServiceDiscoveryManager = new ServiceDiscoveryManager(connection);
					ServiceDiscoveryManager.setIdentityName(getService().getContext().getString(R.string.app_name));
					
					EntityCapsManager capsManager = EntityCapsManager.getInstanceFor(connection);
					capsManager.enableEntityCaps();
					
					Roster roster = connection.getRoster();
					roster.setSubscriptionMode(SubscriptionMode.manual);
					
					//Attempting to fix buddy presence changes missing
					Thread.sleep(1000);
					
					roster.addRosterListener(mRosterListener);
					
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 2);					
					
					connection.login(login, password, getService().getContext().getString(R.string.app_name));
					
					connection.addPacketListener(mRosterListener, mRosterListener);
					
					connection.addConnectionListener(XMPPServiceInternal.this);
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 3);
					
					setStatus(mMyOnlineInfo.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) 0));
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 4);
					
					while (!isRosterInitialized(roster)) {
						Logger.log("Roster not ready", LoggerLevel.VERBOSE);
						Thread.sleep(1000);
					}
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 6);
					
					List<BuddyGroup> groups = mRosterListener.getContactList();
					connection.getChatManager().addChatListener(mChatListener);
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 7);
					try {
						List<MultiChatRoom> joinedChats = mChatListener.getJoinedChatRooms();
						getService().getEntityAdapter().addGroupChats(groups, joinedChats, jid, getService().getServiceId());
					} catch (Exception e) {
						Logger.log(e);
					}
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 8);
					getService().getCoreService().buddyListUpdated( groups);
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTING, 9);
					fillFeatures();
					getService().getCoreService().accountStateChanged(mMyOnlineInfo);
					getService().getCoreService().connectionStateChanged( ConnectionState.CONNECTED, 0);
					
					mRosterListener.getBuddyInfo(jid, true, false);
					
					mChatListener.setMessageEventManager(new MessageEventManager(connection));
					
					FileTransferManager ftm = new FileTransferManager(connection);
					ftm.addFileTransferListener(mFileTransferListener);
					mFileTransferListener.setFileTransferManager(ftm);
					
					getService().sendKeepalive();
					
					mRosterListener.setContactListReady(true);
					mRosterListener.checkCachedInfos();
				} catch (Exception e) {
					Logger.log(e);
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
		Logger.log("Connection closed " + getService().getProtocolUid());
		Logger.log(e);
		getService().closeKeepaliveThread();
		mRosterListener.setContactListReady(false);
		getService().getCoreService().connectionStateChanged( ConnectionState.DISCONNECTED, -1);
		if ((e instanceof IOException) || (e instanceof XMPPException && ((XMPPException) e).getXMPPError() != null && !((XMPPException) e).getXMPPError().getCondition().equals("remote-server-timeout"))) {
			getService().getCoreService().notification(e.getLocalizedMessage());				
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
		Logger.log("Connection closed " + getService().getProtocolUid());
		getService().closeKeepaliveThread();
		mRosterListener.setContactListReady(false);
		getService().getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, -1);
		
	}

	@Override
	public void reconnectingIn(int seconds) {
		Logger.log("Connection reconnect " + getService().getProtocolUid() + " in " + seconds);
		getService().getCoreService().connectionStateChanged(ConnectionState.CONNECTING, 1);
	}

	@Override
	public void reconnectionSuccessful() {
		Logger.log("Reconnected " + mMyOnlineInfo.getProtocolUid());
		getService().getCoreService().connectionStateChanged(ConnectionState.CONNECTED, -1);		
	}

	@Override
	public void reconnectionFailed(Exception e) {
		Logger.log("Reconnection failed " + mMyOnlineInfo.getProtocolUid());
		connectionClosedOnError(e);
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
				connection.removeConnectionListener(XMPPServiceInternal.this);
				connection.removePacketListener(mRosterListener);
				
				connection.disconnect();
				
				mChatListener.onDisconnect();
				mRosterListener.onDisconnect();
				mFileTransferListener.onDisconnect();
				
				getService().getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, 0);
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
				getService().getCoreService().buddyAction(ItemAction.DELETED, buddy);
			} else {
				mRosterListener.removeBuddy(buddy);
			}
			break;
		case MODIFIED:
			RosterEntry e = connection.getRoster().getEntry(buddy.getProtocolUid());
			
			if (TextUtils.isEmpty(e.getName()) || !e.getName().equals(buddy.getName())) {
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
		
		Presence presence = getService().getEntityAdapter().userStatus2XMPPPresence(statusId);
		connection.sendPacket(presence);
	}

	public long sendMessage(TextMessage message) {
		try {
			return mChatListener.sendMessage(message);
		} catch (Exception e) {
			Logger.log(e);
			getService().getCoreService().notification(e.getLocalizedMessage());
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
		Buddy buddy = new Buddy(jid, mMyOnlineInfo.getProtocolUid(), XMPPApiConstants.PROTOCOL_NAME, getService().getServiceId());
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

	/**
	 * @return the service
	 */
	public XMPPCommonService getService() {
		return service;
	}

	public void serviceDiscoveryRequest() throws XMPPException {
		DiscoverItems discoItems = mServiceDiscoveryManager.discoverItems(connection.getServiceName());
		Iterator<Item> it = discoItems.getItems();
		while (it.hasNext()) {
			DiscoverItems.Item item = (DiscoverItems.Item) it.next();
			Logger.log("Service available:" + item.getEntityID() + " " + item.getName());
		}
	}
	
	/**
	 * @return the mRosterListener
	 */
	public XMPPRosterListener getRosterListener() {
		return mRosterListener;
	}

	/**
	 * @return the mChatListener
	 */
	public XMPPChatListener getChatListener() {
		return mChatListener;
	}

	/**
	 * @return the mFileTransferListener
	 */
	public XMPPFileTransferListener getFileTransferListener() {
		return mFileTransferListener;
	}
}
