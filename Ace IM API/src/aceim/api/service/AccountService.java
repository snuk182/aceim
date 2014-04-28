package aceim.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import aceim.api.ICoreService;
import aceim.api.IProtocol;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import android.content.Context;
import android.os.DeadObjectException;
import android.os.RemoteException;

/**
 * Account service base, contains all inner-account logic. Should be nested in protocol implementation.
 */
public abstract class AccountService {
	/**
	 * Account service ID.
	 */
	private final byte serviceId;
	
	/**
	 * Account protocol UID.
	 */
	private final String protocolUid;
	
	/**
	 * AIDL core callback.
	 */
	private final ICoreProtocolCallback callback;
	
	/**
	 * Android context.
	 */
	private final Context context;
	
	private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	private final Map<String, OnlineInfo> onlineInfos = new ConcurrentHashMap<String, OnlineInfo>();
	private ScheduledFuture<?> onlineInfoUpdater = null;

	/**
	 * {@link ICoreService}-to-AIDL translator.
	 */
	private final ICoreService coreService = new ICoreService() {
		
		@Override
		public void typingNotification(String ownerUid) {
			try {
				callback.typingNotification(serviceId, ownerUid);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}			
		}
		
		@Override
		public void showFeatureInputForm(String uid, InputFormFeature feature) {
			try {
				callback.showFeatureInputForm(serviceId, uid, feature);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void searchResult(List<PersonalInfo> infoList) {
			try {
				callback.searchResult(serviceId, infoList);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public String requestPreference(String preferenceName) {
			try {
				return callback.requestPreference(serviceId, preferenceName);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}
			
			return null;
		}
		
		@Override
		public void personalInfo(PersonalInfo info, boolean isShortInfo) {
			try {
				callback.personalInfo(info, isShortInfo);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void notification(String message) {
			try {
				callback.notification(serviceId, message);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void multiChatParticipants(String chatUid, List<BuddyGroup> participantList) {
			try {
				callback.multiChatParticipants(serviceId, chatUid, participantList);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void messageAck(String ownerUid, long messageId, MessageAckState state) {
			try {
				callback.messageAck(serviceId, ownerUid, messageId, state);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void message(Message message) {
			try {
				callback.message(message);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void iconBitmap(String ownerUid, byte[] data, String hash) {
			try {
				callback.iconBitmap(serviceId, ownerUid, data, hash);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void groupAction(ItemAction action, BuddyGroup newGroup) {
			try {
				callback.groupAction(action, newGroup);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void fileProgress(FileProgress progress) {
			try {
				callback.fileProgress(progress);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}
		}
		
		@Override
		public void connectionStateChanged(ConnectionState connState, int extraParameter) {
			try {
				callback.connectionStateChanged(serviceId, connState, extraParameter);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}			
		}
		
		@Override
		public void buddyStateChanged(List<OnlineInfo> infos) {
			
			for (OnlineInfo info : infos) {
				onlineInfos.put(info.getProtocolUid(), info);
			}	
			
			if (onlineInfoUpdater == null) {
				onlineInfoUpdater = scheduledExecutor.schedule(onlineInfosRunnable, 1, TimeUnit.SECONDS);
			}
		}
		
		@Override
		public void buddyListUpdated(List<BuddyGroup> buddyList) {
			try {
				callback.buddyListUpdated(serviceId, buddyList);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void buddyAction(ItemAction action, Buddy newBuddy) {
			try {
				callback.buddyAction(action, newBuddy);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void accountStateChanged(OnlineInfo info) {
			try {
				callback.accountStateChanged(info);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}	
		}
		
		@Override
		public void accountActivity(String text) {
			try {
				callback.accountActivity(serviceId, text);
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	};
	
	/**
	 * As new online infos may arrive too often, we cache them and make calls to core periodically.
	 */
	private final Runnable onlineInfosRunnable = new Runnable() {
		
		@Override
		public void run() {
			try {
				while(onlineInfos.size() > 0) {
					callback.buddyStateChanged(new ArrayList<OnlineInfo>(onlineInfos.values()));
					onlineInfos.clear();
				}
			} catch (RemoteException e) { 
				onRemoteException(e); 
			} catch (Exception e) {
				Logger.log(e);
			}
			
			onlineInfoUpdater = null;
		}
	};
	
	protected AccountService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		this.serviceId = serviceId;
		this.protocolUid = protocolUid;
		this.callback = callback;
		this.context = context;
	}
	
	private void onRemoteException(RemoteException e) {
		if (e instanceof DeadObjectException) {
			Logger.log("Callback is dead - shutting down");
		} else {
			Logger.log(e);
		}
	}

	public abstract IProtocol getProtocol();
	protected abstract void timeoutReconnect(); 	
	protected abstract ConnectionState getCurrentState();
	
	/**
	 * @return the serviceId
	 */
	public byte getServiceId() {
		return serviceId;
	}

	/**
	 * @return the protocolUid
	 */
	public String getProtocolUid() {
		return protocolUid;
	}
	
	/**
	 * @return the coreService
	 */
	public ICoreService getCoreService() {
		return coreService;
	}

	/**
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	/*
	 * The following code implements simple trying to override the "dead socket" issue (http://code.google.com/p/android/issues/detail?id=6144). 
	 * It simply sends some message, which needs to be answered by server in some amount of time. Of course, different protocols have different 
	 * ways to implement this logic, but in any case it should be some protocol message, that does not influence on overall client-server 
	 * conversation logic, but do the responding. For example, ICQ has some deprecated or rarely used API calls with responding. 
	 */
	public static final String PING_TIMEOUT = "pingtimeout";

	private KeepaliveTimer keepaliveTimer = new KeepaliveTimer();
	
	private ScheduledFuture<?> task;
	
	private Runnable timeoutRunnable = new Runnable(){
		
		@Override
		public void run() {
			if (getCurrentState() == ConnectionState.CONNECTED && keepaliveTimer.running){
				keepaliveTimer.running = false;
				try {
					Logger.log(getProtocolUid() + " could not wait for heartbeat, disconnecting", LoggerLevel.DEBUG);
					timeoutReconnect();
				} catch (Exception e1) {
					Logger.log(e1);
				}
			}			
		}
		
	};

	public long pingTimeout = 0;
	
	public void resetHeartbeat() {
		Logger.log(getProtocolUid() + " got heartbeat", LoggerLevel.DEBUG);
		
		if (task!=null){
			task.cancel(false);
		}	
		if (pingTimeout < 1){
			return;
		}
		keepaliveTimer.running = true;
		task = scheduledExecutor.schedule(keepaliveTimer, pingTimeout , TimeUnit.SECONDS);
	}
	
	public void closeKeepaliveThread() {
		resetHeartbeat();
	}

	private void schedule(){
		if (pingTimeout < 1){
			return;
		}
		Logger.log("schedule... "+getCurrentState(), LoggerLevel.DEBUG);
		try {
			if (getCurrentState() == ConnectionState.CONNECTED){
				keepaliveRequest();
				task = scheduledExecutor.schedule(timeoutRunnable, pingTimeout , TimeUnit.SECONDS);
				keepaliveTimer.running = true;	
				Logger.log(getProtocolUid() + " sent heartbeat request", LoggerLevel.DEBUG);
			}
		} catch (Exception e) {
			Logger.log(e);
		}
	}
	
	protected abstract void keepaliveRequest();
	
	public void sendKeepalive(){
		if (pingTimeout > 0){
			Logger.log("start keepalive "+getProtocolUid(), LoggerLevel.VERBOSE);
			resetHeartbeat();
		} else {
			Logger.log("skip keepalive "+getProtocolUid(), LoggerLevel.VERBOSE);
		}
	}
	
	class KeepaliveTimer extends Thread{
		
		public volatile boolean running = true;
		
		@Override
		public void run(){
			if (running){
				if (task!=null){
					task.cancel(false);
				}
				schedule();
			}			
		}		
	}
}
