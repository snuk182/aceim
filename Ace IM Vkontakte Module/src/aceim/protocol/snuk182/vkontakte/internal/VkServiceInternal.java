package aceim.protocol.snuk182.vkontakte.internal;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	private final Set<Long> chats = Collections.synchronizedSet(new HashSet<Long>());

	private final Map<Long, String> iconPaths = Collections.synchronizedMap(new HashMap<Long, String>());
	private final LongPollCallback callback = new LongPollCallback() {

		@Override
		public void onlineInfo(VkOnlineInfo vi) {
			OnlineInfo info = VkEntityAdapter.vkOnlineInfo2OnlineInfo(vi, service.getServiceId());
			service.getCoreService().buddyStateChanged(info);
		}

		@Override
		public void message(VkMessage vkm) {
			Message message = VkEntityAdapter.vkMessage2Message(vkm, service.getServiceId());
			service.getCoreService().message(message);
		}

		@Override
		public void typingNotification(long contactId, long chatParticipantId) {
			service.getCoreService().typingNotification(Long.toString(chatParticipantId != 0 ? chatParticipantId : contactId));
		}

		@Override
		public void disconnected(String reason) {
			onLogout(reason);
		}

		@Override
		public void messageAck(VkMessage vkm) {
			service.getCoreService().messageAck(Long.toString(vkm.getPartnerId()), vkm.getMessageId(), MessageAckState.SERVER_ACK);
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
				// TODO
			}
		}
	};
	
	private final Runnable getGroupchatsRunnable = new Runnable() {
		
		@Override
		public void run() {
			try {
				List<VkChat> vkchats = engine.getGroupChats();
				
				for (VkChat chat : vkchats) {
					chats.add(chat.getId());
				}
				
				service.getCoreService().searchResult(VkEntityAdapter.vkChats2PersonalInfoList(vkchats, service.getServiceId()));
			} catch (RequestFailedException e) {
				Logger.log(e);
				service.getCoreService().notification(e.getLocalizedMessage());
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
			
			getMyPersonalInfo();
		} catch (RequestFailedException e) {
			Logger.log(e);
			onLogout(e.getCause().getLocalizedMessage());
		}
	}
	

	public void leaveChat(final String chatId) {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				service.getCoreService().buddyAction(ItemAction.LEFT, new MultiChatRoom(chatId, service.getProtocolUid(), VkConstants.PROTOCOL_NAME, service.getServiceId()));
			}
		});
	}
	
	public void joinChat(final String chatId, boolean loadIcons) {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					VkChat vkChat = engine.getChatById(chatId);
					
					List<VkBuddy> occupants = engine.getUsersByIdList(vkChat.getUsers());
					
					MultiChatRoom chat = VkEntityAdapter.vkChat2MultiChatRoom(vkChat, service.getProtocolUid(), service.getServiceId());
					chat.getOccupants().addAll(VkEntityAdapter.vkChatOccupants2ChatOccupants(vkChat, occupants, accessToken.getUserID(), service.getProtocolUid(), service.getServiceId()));
					chat.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
					
					chats.add(vkChat.getId());
					
					service.getCoreService().buddyAction(ItemAction.JOINED, chat);
					service.getCoreService().buddyStateChanged(chat.getOnlineInfo());
					
					/*List<VkMessage> vkMessages = engine.getLastChatMessages(chat.getProtocolUid(), true);
					
					for (VkMessage vkm : vkMessages) {
						service.getCoreService().message(VkEntityAdapter.vkChatMessage2Message(vkChat.getId(), vkm, service.getServiceId()));
					}*/
				} catch (RequestFailedException e) {
					Logger.log(e);
				}
			}
		}).start();
	}

	public void typingNotification(final String uid) {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					engine.sendTypingNotifications(uid, isChatUid(Long.parseLong(uid)));
				} catch (Exception e) {
					Logger.log(e);
				}
			}
		}).start();
	}

	private void getMyPersonalInfo() {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					VkBuddy myInfo = engine.getMyInfo();			
					OnlineInfo info = new OnlineInfo(service.getServiceId(), service.getProtocolUid());
					info.getFeatures().putBoolean(VkApiConstants.FEATURE_GROUPCHATS, true);
					
					service.getCoreService().accountStateChanged(info);
					service.getCoreService().personalInfo(VkEntityAdapter.vkBuddy2PersonalInfo(myInfo, service.getServiceId(), service.getProtocolUid()), true);
					service.getCoreService().iconBitmap(service.getProtocolUid(), engine.getIcon(myInfo.getPhotoPath()), myInfo.getPhotoPath());
				} catch (RequestFailedException e) {
					Logger.log(e);
				}
			}
		}).start();
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
			accessToken = engine.getAccessToken(code);
			checkTokenAndLogin();
		} catch (RequestFailedException e) {
			Logger.log(e);
			service.getCoreService().notification(e.getLocalizedMessage());
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

	private void onLogout(String reason) {
		connectionState = ConnectionState.DISCONNECTED;

		service.getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, 0);
		if (reason != null) {
			service.getCoreService().notification(reason);
		}
	}
	
	public void requestAvailableGroupchats() {
		Executors.defaultThreadFactory().newThread(getGroupchatsRunnable).start();
	}

	public void requestIcon(final String uid) {
		if (connectionState == ConnectionState.CONNECTED) {
			iconDownloader.addUrl(uid);
		}
	}

	private void requestIconInternal(String uid) {
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
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	private boolean isChatUid(long uid) {
		return chats.contains(uid);
	}

	public long sendMessage(Message message) {

		try {
			if (message instanceof TextMessage) {
				return engine.sendMessage(VkEntityAdapter.textMessage2VkMessage((TextMessage) message, isChatUid(Long.parseLong(message.getContactUid()))));
			}
		} catch (NumberFormatException e) {
			Logger.log(e);
		} catch (RequestFailedException e) {
			Logger.log(e);
			onLogout(e.getCause().getLocalizedMessage());
		}

		return 0;
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
