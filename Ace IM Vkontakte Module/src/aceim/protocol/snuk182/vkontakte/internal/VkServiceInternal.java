package aceim.protocol.snuk182.vkontakte.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.protocol.snuk182.vkontakte.R;
import aceim.protocol.snuk182.vkontakte.VkConstants;
import aceim.protocol.snuk182.vkontakte.VkEntityAdapter;
import aceim.protocol.snuk182.vkontakte.VkService;
import aceim.protocol.snuk182.vkontakte.internal.VkEngine.LongPollCallback;
import aceim.protocol.snuk182.vkontakte.internal.VkEngine.RequestFailedException;
import aceim.protocol.snuk182.vkontakte.model.AccessToken;
import aceim.protocol.snuk182.vkontakte.model.VkBuddy;
import aceim.protocol.snuk182.vkontakte.model.VkBuddyGroup;
import aceim.protocol.snuk182.vkontakte.model.VkChat;
import aceim.protocol.snuk182.vkontakte.model.VkMessage;
import aceim.protocol.snuk182.vkontakte.model.VkOnlineInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

public class VkServiceInternal {

	private ConnectionState connectionState = ConnectionState.DISCONNECTED;

	private final VkService service;
	private VkEngine engine;

	private AccessToken accessToken;

	private final IconDownloader iconDownloader = new IconDownloader();

	private final Set<Long> connectedChats = new CopyOnWriteArraySet<Long>();
	private final Set<VkChat> chats = new CopyOnWriteArraySet<VkChat>();

	private final Map<Long, String> iconPaths = Collections.synchronizedMap(new HashMap<Long, String>());
	private final LongPollCallback callback = new LongPollCallback() {

		@Override
		public void onlineInfo(VkOnlineInfo vi) {
			if (vi == null) {
				return;
			}

			OnlineInfo info;
			if (vi.getStatus() == 0) {
				info = requestStatusInternal(vi.getUid(), null);
				info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) (vi.getStatus() == 0 ? 0 : -1));
			} else {
				info = VkEntityAdapter.vkOnlineInfo2OnlineInfo(vi, accessToken.getUserID(), service.getProtocolUid(), service.getServiceId());
			}

			service.getCoreService().buddyStateChanged(Arrays.asList(info));
		}

		@Override
		public void message(VkMessage vkm) {
			//Despite of official documentation to say flag #16 is for "isChat", tests show it is not, but rather for "isAck".
			if (vkm.getPartnerId() != accessToken.getUserID() && vkm.isOutgoing() && !vkm.isChat()) {
				
				MessageAckState ackState;
				if (vkm.isUnread()){
					ackState = MessageAckState.RECIPIENT_ACK;			
				} else {
					ackState = MessageAckState.READ_ACK;			
				}
				
				service.getCoreService().messageAck(Long.toString(vkm.getPartnerId()), vkm.getMessageId(), ackState);			
			} else {
				Message message = VkEntityAdapter.vkMessage2Message(vkm, service.getServiceId(), service.getProtocolUid(), accessToken.getUserID());

				if ((isChatUid(vkm.getPartnerId()) && !isChatJoined(vkm.getPartnerId())) // the case chat exists but not joined
						|| message.getContactDetail() != null) { // the case chat has just been created
					joinChatInternal(message.getContactUid(), false);
				}
				
				service.getCoreService().message(message);
			}
		}

		@Override
		public void typingNotification(long contactId, long chatParticipantId) {
			service.getCoreService().typingNotification(Long.toString(chatParticipantId != 0 ? chatParticipantId : contactId));
		}

		@Override
		public void disconnected(String reason) {
			onLogout(reason);
		}
	};

	private final Runnable proceedLoginRunnable = new Runnable() {

		@Override
		public void run() {
			if (accessToken != null) {

				SharedPreferences.Editor editor = service.getContext().getSharedPreferences(VkConstants.PREFS, Context.MODE_PRIVATE).edit();
				editor.putString(VkConstants.KEY_TOKEN, accessToken.getToken());
				editor.putLong(VkConstants.KEY_USER_ID, accessToken.getUserID());
				editor.putLong(VkConstants.KEY_EXP_TIME_SECONDS, accessToken.getExpirationTime().getTime());
				editor.putBoolean(VkConstants.KEY_UNEXPIRABLE_TOKEN, accessToken.isUnexpirable());
				editor.commit();

				proceedLogin();
			} else {
				onLogout("Empty access token");
			}
		}
	};

	private final Runnable loginRunnable = new Runnable() {

		@Override
		public void run() {
			if (connectionState != ConnectionState.DISCONNECTED) {
				Logger.log("Already connected", LoggerLevel.INFO);
				return;
			}

			String token = service.getContext().getSharedPreferences(VkConstants.PREFS, Context.MODE_PRIVATE).getString(VkConstants.KEY_TOKEN, null);
			long userId = service.getContext().getSharedPreferences(VkConstants.PREFS, Context.MODE_PRIVATE).getLong(VkConstants.KEY_USER_ID, 0);
			long expirationTime = service.getContext().getSharedPreferences(VkConstants.PREFS, Context.MODE_PRIVATE).getLong(VkConstants.KEY_EXP_TIME_SECONDS, 0);
			boolean unexpirableToken = service.getContext().getSharedPreferences(VkConstants.PREFS, Context.MODE_PRIVATE).getBoolean(VkConstants.KEY_UNEXPIRABLE_TOKEN, false);

			if (token == null) {
				showLoginDialog();
			} else {
				loginResult(token, expirationTime, unexpirableToken, userId);
			}
		}
	};

	public VkServiceInternal(VkService service) {
		this.service = service;
	}

	public void login(OnlineInfo info) {
		Executors.defaultThreadFactory().newThread(loginRunnable).start();
	}

	private void proceedLogin() {
		if (!isTokenFresh()) {
			renewToken();
		} else {
			this.engine = new VkEngine(accessToken.getToken(), Long.toString(accessToken.getUserID()));
			connect();
		}
	}

	private void connect() {
		try {
			connectionState = ConnectionState.CONNECTING;

			service.getCoreService().connectionStateChanged(ConnectionState.CONNECTING, 1);

			List<VkBuddyGroup> groups = engine.getBuddyGroupList();
			List<VkBuddy> buddies = engine.getBuddyList();

			service.getCoreService().connectionStateChanged(ConnectionState.CONNECTING, 4);

			fillIconMap(buddies);

			List<VkOnlineInfo> onlineInfos = engine.getOnlineBuddies();

			service.getCoreService().connectionStateChanged(ConnectionState.CONNECTING, 7);
			List<BuddyGroup> buddyList = VkEntityAdapter.vkBuddiesAndGroups2BuddyList(buddies, groups, onlineInfos, accessToken.getUserID(), service.getProtocolUid(), service.getServiceId());
			connectionState = ConnectionState.CONNECTED;

			service.getCoreService().connectionStateChanged(ConnectionState.CONNECTED, 0);
			service.getCoreService().buddyListUpdated(buddyList);

			int pollWaitTime = Integer.parseInt(service.getCoreService().requestPreference(VkConstants.KEY_LONGPOLL_WAIT_TIME));

			engine.connectLongPoll(pollWaitTime, callback);

			getAvailableGroupchats();
			getMyPersonalInfoInternal();
			getStatuses(onlineInfos);
		} catch (RequestFailedException e) {
			onRequestFailed(e);
		}
	}

	private void getStatuses(final List<VkOnlineInfo> onlineInfos) {
		if (engine == null) {
			onLogout(null);
			return;
		}

		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				List<OnlineInfo> infos = new ArrayList<OnlineInfo>(onlineInfos.size());

				for (VkOnlineInfo vkInfo : onlineInfos) {
					OnlineInfo info = requestStatusInternal(vkInfo.getUid(), onlineInfos);
					if (info != null) {
						infos.add(info);
					}
				}

				service.getCoreService().buddyStateChanged(infos);
			}
		}).start();
	}

	private OnlineInfo requestStatusInternal(long uid, List<VkOnlineInfo> onlineInfos) {
		if (engine == null) {
			onLogout(null);
			return null;
		}

		String status;
		try {
			status = engine.requestStatus(uid);
			Logger.log(uid + " has status: " + status, LoggerLevel.VERBOSE);
		} catch (RequestFailedException e) {
			Logger.log(e);
			status = "";
		}

		OnlineInfo info;
		if (accessToken.getUserID() == uid) {
			info = new OnlineInfo(service.getServiceId(), service.getProtocolUid());
			info.setXstatusName(status);
		} else {
			info = new OnlineInfo(service.getServiceId(), Long.toString(uid));
			info.setXstatusName(status);
			if (onlineInfos != null) {
				for (VkOnlineInfo vkInfo : onlineInfos) {
					if (vkInfo.getUid() == uid) {
						info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
						break;
					}
				}
			}
		}

		return info;

	}

	public void leaveChat(final String chatId) {
		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				connectedChats.remove(Long.parseLong(chatId));
				service.getCoreService().buddyAction(ItemAction.LEFT, new MultiChatRoom(chatId, service.getProtocolUid(), VkConstants.PROTOCOL_NAME, service.getServiceId()));
			}
		}).start();
	}

	public void joinChat(final String chatId, final boolean loadIcons) {
		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				joinChatInternal(chatId, loadIcons);
			}
		}).start();

	}

	private void joinChatInternal(final String chatId, boolean loadIcons) {
		try {
			if (engine == null) {
				onLogout(null);
				return;
			}

			if (!isChatUid(Long.parseLong(chatId))) {
				service.getCoreService().notification(service.getContext().getString(R.string.x_is_not_a_chat, chatId));
				return;
			}

			final VkChat vkChat = engine.getChatById(chatId);

			List<VkBuddy> occupants = engine.getUsersByIdList(vkChat.getUsers());

			MultiChatRoom chat = VkEntityAdapter.vkChat2MultiChatRoom(vkChat, service.getProtocolUid(), service.getServiceId());
			chat.getOccupants().addAll(VkEntityAdapter.vkChatOccupants2ChatOccupants(vkChat, occupants, accessToken.getUserID(), service.getProtocolUid(), service.getServiceId()));
			chat.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);

			chats.add(vkChat);
			connectedChats.add(vkChat.getId());

			service.getCoreService().buddyAction(ItemAction.JOINED, chat);
			service.getCoreService().buddyStateChanged(Arrays.asList(chat.getOnlineInfo()));

			/*
			 * List<VkMessage> vkMessages =
			 * engine.getLastChatMessages(chat.getProtocolUid(), true);
			 * 
			 * for (VkMessage vkm : vkMessages) {
			 * service.getCoreService().message
			 * (VkEntityAdapter.vkChatMessage2Message(vkChat.getId(), vkm,
			 * service.getServiceId())); }
			 */

			if (loadIcons) {
				Executors.defaultThreadFactory().newThread(new Runnable() {

					@Override
					public void run() {
						for (long uid : vkChat.getUsers()) {
							requestIcon(Long.toString(uid));
						}
					}
				});
			}
		} catch (RequestFailedException e) {
			onRequestFailed(e);
		}
	}

	public void typingNotification(final String uid) {
		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				if (engine == null) {
					onLogout(null);
					return;
				}

				try {
					engine.sendTypingNotifications(uid, isChatUid(Long.parseLong(uid)));
				} catch (NumberFormatException e) {
					Logger.log(e);
				} catch (RequestFailedException e) {
					onRequestFailed(e);
				}
			}
		}).start();
	}

	private void getMyPersonalInfoInternal() {
		if (engine == null) {
			onLogout(null);
			return;
		}

		try {
			VkBuddy myInfo = engine.getMyInfo();

			OnlineInfo info = requestStatusInternal(accessToken.getUserID(), null);
			info.getFeatures().putBoolean(VkApiConstants.FEATURE_GROUPCHATS, true);
			info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
			info.getFeatures().putByte(ApiConstants.FEATURE_XSTATUS, (byte) 0);

			service.getCoreService().accountStateChanged(info);
			service.getCoreService().personalInfo(VkEntityAdapter.vkBuddy2PersonalInfo(myInfo, service.getServiceId(), accessToken.getUserID(), service.getProtocolUid()), true);
			service.getCoreService().iconBitmap(service.getProtocolUid(), engine.getIcon(myInfo.getPhotoPath()), myInfo.getPhotoPath());
		} catch (RequestFailedException e) {
			onRequestFailed(e);
		}
	}

	private void fillIconMap(List<VkBuddy> buddies) {
		if (buddies == null)
			return;

		for (VkBuddy vkb : buddies) {
			iconPaths.put(vkb.getUid(), vkb.getPhotoPath());
		}
	}

	private void renewToken() {
		showLoginDialog();
	}

	private boolean isTokenFresh() {
		if (accessToken.isUnexpirable()) {
			return true;
		} else {
			Calendar exp = Calendar.getInstance();
			exp.setTime(accessToken.getExpirationTime());
			return Calendar.getInstance().before(exp);
		}
	}

	private void showLoginDialog() {
		String password = service.getCoreService().requestPreference(VkConstants.KEY_PASSWORD);
		boolean autoSubmitDialog = Boolean.parseBoolean(service.getCoreService().requestPreference(VkConstants.KEY_AUTO_SUBMIT_AUTH_DIALOG));

		Bundle options = new Bundle();
		options.putCharSequence(VkConstants.KEY_PROTOCOL_ID, service.getProtocolUid());
		options.putCharSequence(VkConstants.KEY_PASSWORD, password);
		options.putBoolean(VkConstants.KEY_AUTO_SUBMIT_AUTH_DIALOG, autoSubmitDialog);

		Intent intent = new Intent();
		intent.setClass(service.getContext(), LoginActivity.class);
		intent.putExtras(options);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | intent.getFlags());

		service.getContext().startActivity(intent);
	}

	private void checkTokenAndLogin() {
		Executors.defaultThreadFactory().newThread(proceedLoginRunnable).start();
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	public void loginResult(String code) {
		try {
			accessToken = VkEngine.getAccessToken(code);
			checkTokenAndLogin();
		} catch (RequestFailedException e) {
			Logger.log(e);
			onLogout(e.getLocalizedMessage());
		}
	}

	public void loginResult(String token, long expirationTimeMillis, boolean unexpirableToken, long internalUserId) {
		accessToken = new AccessToken(token, internalUserId, expirationTimeMillis, unexpirableToken);
		checkTokenAndLogin();
	}

	public void logout() {
		if (engine != null) {
			engine.disconnect(null);
		}
		onLogout(null);
	}

	public void requestAvailableGroupchats() {
		if (engine == null) {
			onLogout(null);
			return;
		}

		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				service.getCoreService().searchResult(VkEntityAdapter.vkChats2PersonalInfoList(new ArrayList<VkChat>(chats), service.getServiceId()));
			}
		}).start();
	}

	public void setStatus(final OnlineInfo info) {
		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				String statusString;

				if (!TextUtils.isEmpty(info.getXstatusName()) && !TextUtils.isEmpty(info.getXstatusDescription())) {
					statusString = info.getXstatusName() + ": " + info.getXstatusDescription();
				} else {
					statusString = TextUtils.isEmpty(info.getXstatusDescription()) ? info.getXstatusName() : info.getXstatusDescription();
				}

				setStatusInternal(statusString);
			}
		}).start();
	}

	private void setStatusInternal(String statusString) {
		if (engine == null) {
			onLogout(null);
			return;
		}

		if (statusString == null) {
			statusString = "";
		}

		try {
			engine.setStatus(statusString);
		} catch (RequestFailedException e) {
			onRequestFailed(e);
		}
	}

	private void onLogout(String reason) {
		connectionState = ConnectionState.DISCONNECTED;

		service.getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, 0);
		if (reason != null) {
			service.getCoreService().notification(reason);
		}
	}

	private void getAvailableGroupchats() {
		if (engine == null) {
			onLogout(null);
			return;
		}

		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				try {
					List<VkChat> vkchats = engine.getGroupChats();

					chats.clear();
					chats.addAll(vkchats);

					List<OnlineInfo> onlineInfos = VkEntityAdapter.vkChats2OnlineInfoList(chats, connectedChats, service.getProtocolUid(), service.getServiceId());

					service.getCoreService().buddyStateChanged(onlineInfos);
				} catch (RequestFailedException e) {
					onRequestFailed(e);
				}
			}
		}).start();
	}

	public void requestIcon(final String uid) {
		if (connectionState == ConnectionState.CONNECTED) {
			iconDownloader.addUrl(uid);
		}
	}

	private void requestIconInternal(String uid) {
		if (engine == null) {
			onLogout(null);
			return;
		}

		try {
			long id;
			if (uid.equals(service.getProtocolUid())) {
				id = accessToken.getUserID();
			} else {
				id = Long.parseLong(uid);
			}

			String path = iconPaths.get(id);

			if (TextUtils.isEmpty(path)) {
				Logger.log("No icon available for " + uid, LoggerLevel.VERBOSE);
				return;
			} else {
				Logger.log("Icon request for " + uid + " / " + path, LoggerLevel.VERBOSE);
			}

			if (!path.startsWith("http://")) {
				path = "http://" + path;
			}

			byte[] icon = engine.getIcon(path);
			if (icon != null) {
				service.getCoreService().iconBitmap(uid, icon, path);
			}
		} catch (RequestFailedException e) {
			onRequestFailed(e);
		}
	}

	private boolean isChatUid(long uid) {
		for (VkChat chat : chats) {
			if (chat.getId() == uid) {
				return true;
			}
		}

		return false;
	}

	private boolean isChatJoined(long uid) {
		return isChatUid(uid) && connectedChats.contains(uid);
	}

	public long sendMessage(Message message) {
		if (engine == null) {
			onLogout(null);
			return 0;
		}

		long id = Long.parseLong(message.getContactUid());
		boolean isChat = isChatUid(id);

		try {
			if (isChat) {
				if (!isChatJoined(id)) {
					joinChatInternal(message.getContactUid(), false);
				}
			}

			if (message instanceof TextMessage) {
				return engine.sendMessage(VkEntityAdapter.textMessage2VkMessage((TextMessage) message, isChat));
			}
		} catch (NumberFormatException e) {
			Logger.log(e);
		} catch (RequestFailedException e) {
			onRequestFailed(e);
		}

		return 0;
	}

	private void onRequestFailed(RequestFailedException e) {
		if (e != null) {
			Logger.log(e);
			onLogout(e.getLocalizedMessage());
		}
	}

	private final class IconDownloader implements Runnable {

		private final Set<String> requests = Collections.synchronizedSet(new HashSet<String>());

		private volatile boolean isRunning = false;

		void addUrl(String url) {
			requests.add(url);
			if (!isRunning) {
				Executors.defaultThreadFactory().newThread(this).start();
			}
		}

		@Override
		public void run() {
			isRunning = true;
			synchronized (requests) {
				while (requests.size() > 0) {
					for (Iterator<String> i = requests.iterator(); i.hasNext();) {
						String url = i.next();
						requestIconInternal(url);
						i.remove();
					}
				}
			}
			isRunning = false;
		}
	}
}
