package aceim.api.service;

import java.util.ArrayList;
import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.ServiceHelper;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Protocol service base, contains all protocol and account management logic. Should be nested by protocol implementation. 
 * @param <T>
 */
public abstract class ProtocolService<T extends AccountService> extends Service {
	
	/**
	 * Service helper utility entity.
	 */
	private final ServiceHelper mHelper = new ServiceHelper(this);
	
	/**
	 * List of running accounts.
	 */
	protected final List<T> mAccountServices = new ArrayList<T>();
	
	private ICoreProtocolCallback callback = null;
	
	/**
	 * AIDL core-to-protocol interface stub.
	 */
	private final IProtocolService.Stub mainService = new IProtocolService.Stub() {
		
		@Override
		public void uploadAccountPhoto(byte serviceId, String filePath) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().uploadAccountPhoto(filePath);
		}
		
		@Override
		public void shutdown() throws RemoteException {
			shutdownInternal();
		}
		
		@Override
		public void setFeature(String featureId, OnlineInfo info) throws RemoteException {
			T service = findAccountServiceById(info.getServiceId());
			service.getProtocol().setFeature(featureId, info);
		}
		
		@Override
		public void sendTypingNotification(byte serviceId, String ownerUid) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().sendTypingNotification(ownerUid);
		}
		
		@Override
		public long sendMessage(Message message) throws RemoteException {
			T service = findAccountServiceById(message.getServiceId());
			return service.getProtocol().sendMessage(message);
		}
		
		@Override
		public void requestIcon(byte serviceId, String ownerUid) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().requestIcon(ownerUid);
		}
		
		@Override
		public void requestFullInfo(byte serviceId, String uid, boolean shortInfo) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().requestFullInfo(uid, shortInfo);
		}
		
		@Override
		public void removeAccountPhoto(byte serviceId) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().removeAccountPhoto();
		}
		
		@Override
		public void removeAccount(byte serviceId) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().disconnect();
			removeAccountService(serviceId);
		}
		
		@Override
		public void registerCallback(ICoreProtocolCallback callback) throws RemoteException {
			ProtocolService.this.callback = callback;
		}
		
		@Override
		public void messageResponse(Message message, boolean accept) throws RemoteException {
			T service = findAccountServiceById(message.getServiceId());
			service.getProtocol().messageResponse(message, accept);
		}
		
		@Override
		public void leaveChatRoom(byte serviceId, String chatId) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().leaveChatRoom(chatId);
		}
		
		@Override
		public void joinChatRoom(byte serviceId, String chatId, boolean loadOccupantsIcons) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().joinChatRoom(chatId, loadOccupantsIcons);
		}
		
		@Override
		public ProtocolOption[] getProtocolOptions() throws RemoteException {
			return ProtocolService.this.getProtocolOptions();
		}
		
		@Override
		public String getProtocolName() throws RemoteException {
			return ProtocolService.this.getProtocolName();
		}
		
		@Override
		public ProtocolServiceFeature[] getProtocolFeatures() throws RemoteException {
			return ProtocolService.this.getProtocolFeatures();
		}
		
		@Override
		public void disconnect(byte serviceId) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().disconnect();
		}
		
		@Override
		public void connect(OnlineInfo info) throws RemoteException {
			T service = findAccountServiceById(info.getServiceId());
			service.getProtocol().connect(info);
		}
		
		@Override
		public void cancelFileFransfer(byte serviceId, long messageId) throws RemoteException {
			T service = findAccountServiceById(serviceId);
			service.getProtocol().cancelFileFransfer(messageId);
		}
		
		@Override
		public void buddyGroupAction(ItemAction action, BuddyGroup group) throws RemoteException {
			T service = findAccountServiceById(group.getServiceId());
			service.getProtocol().buddyGroupAction(action, group);
		}
		
		@Override
		public void buddyAction(ItemAction action, Buddy buddy) throws RemoteException {
			T service = findAccountServiceById(buddy.getServiceId());
			service.getProtocol().buddyAction(action, buddy);
		}
		
		@Override
		public void addAccount(byte serviceId, String protocolUid) throws RemoteException {
			addAccountService(serviceId, protocolUid);
		}

		@Override
		public void logToFile(boolean enable) throws RemoteException {
			Logger.logToFile = enable;
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		Logger.log(getPackageName() + ": CoreService binded", LoggerLevel.VERBOSE);
		disconnectAll();
		mAccountServices.clear();
		return mainService;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Logger.log(getPackageName() + ": CoreService unbinded", LoggerLevel.VERBOSE);
		shutdownInternal();
		return false;
    }
	
	public ICoreProtocolCallback getCallback() {
		return callback;
	}
	
	/**
	 * Create account entity by service ID and protocol UID.
	 * 
	 * @param serviceId
	 * @param protocolUid
	 */
	private void addAccountService(byte serviceId, String protocolUid) {
		for (T service : mAccountServices) {
			if (service.getProtocolUid().equals(protocolUid)) {
				Logger.log(getPackageName() + ": Existing account service found for id#" + protocolUid, LoggerLevel.VERBOSE);				
				service.getProtocol().disconnect();
				mAccountServices.remove(service);
				break;
			}
		}
		
		Logger.log(getPackageName() + ": Adding account service " + protocolUid, LoggerLevel.VERBOSE);
		if (mAccountServices.size() < 1) {
			mHelper.doStartForeground();
		}
		mAccountServices.add(createService(serviceId, protocolUid));
	}
	
	/**
	 * Remove account entity by service ID.
	 * 
	 * @param serviceId
	 */
	private void removeAccountService(byte serviceId) {
		Logger.log(getPackageName() + ": Removing account service " + serviceId, LoggerLevel.VERBOSE);
		mAccountServices.remove(findAccountServiceById(serviceId));
		if (mAccountServices.size() < 1) {
			mHelper.doStopForeground();
		}
	}
	
	private void shutdownInternal() {
		Logger.log(getPackageName() + ": Shutdown request", LoggerLevel.VERBOSE);
		disconnectAll();
		
		mHelper.doStopForeground();
		//stopSelfResult(startId);		
		stopSelf();
	}
	
	private void disconnectAll() {
		Logger.log(getPackageName() + ": Disconnect all", LoggerLevel.VERBOSE);
		for (T service : mAccountServices) {
			try {
				mainService.disconnect(service.getServiceId());
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}
	}

	protected T findAccountServiceById(byte serviceId) {
		for (T s : mAccountServices) {
			if (s.getServiceId() == serviceId) {
				return s;
			}
		}
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.log(getPackageName() + ": Destroyed", LoggerLevel.VERBOSE);
	}
	
	/**
	 * Get {@link ProtocolOption} set for current protocol, for account editor form.
	 * 
	 * @return options list, very likely to be ordered.
	 */
	protected abstract ProtocolOption[] getProtocolOptions();
	
	/**
	 * Get {@link ProtocolServiceFeature} set for current protocol.
	 * 
	 * @return features list, may not preserve order.
	 */
	protected abstract ProtocolServiceFeature[] getProtocolFeatures();
	
	/**
	 * Create account implementation, by service ID & protocol UID.
	 * 
	 * @param serviceId
	 * @param protocolUid
	 * @return
	 */
	protected abstract T createService(byte serviceId, String protocolUid) ;
	
	/**
	 * Get implemented protocol name (XMPP, ICQ etc)
	 * @return
	 */
	protected abstract String getProtocolName();
}
