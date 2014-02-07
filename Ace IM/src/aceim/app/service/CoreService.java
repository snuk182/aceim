package aceim.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.IProtocolService;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolException.Cause;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.ServiceHelper;
import aceim.app.AceImException;
import aceim.app.AceImException.AceImExceptionReason;
import aceim.app.Constants;
import aceim.app.Constants.OptionKey;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountOptionKeys;
import aceim.app.dataentity.AccountService;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.service.ProtocolServicesManager.ProtocolListener;
import aceim.app.utils.DataStorage;
import aceim.app.utils.ImportAndExport;
import aceim.app.utils.LocationSender;
import aceim.app.utils.Notificator;
import aceim.app.utils.Notificator.LEDNotificationMode;
import aceim.app.utils.Notificator.SoundNotificationMode;
import aceim.app.utils.Notificator.StatusBarNotificationMode;
import aceim.app.utils.OptionsReceiver;
import aceim.app.utils.OptionsReceiver.OnOptionChangedListener;
import aceim.app.utils.ViewUtils;
import aceim.app.utils.history.HistorySaver;
import aceim.app.utils.history.impl.JsonHistorySaver;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.androidquery.callback.BitmapAjaxCallback;

public class CoreService extends Service {
	
	private static byte sReconnectionAttempts;

	private WifiManager.WifiLock wifiLock = null;
	private PowerManager.WakeLock powerLock = null;

	private final ServiceHelper mServiceHelper = new ServiceHelper(this);
	private ProtocolServicesManager mProtocolServiceManager;

	private OptionsReceiver mOptionsReceiver;
	private Notificator mNotificator;
	private LocationSender mLocationSender;
	
	private final List<AccountService> mAccounts = new ArrayList<AccountService>();
	private volatile boolean mProtocolsReady = false;
	private volatile boolean mAccountsReady = false;
	private volatile boolean mExiting = false;

	private DataStorage mStorage;
	private HistorySaver mHistorySaver;

	private IUserInterface mInterface;
	//private boolean uiVisible = true;

	private Bundle mSavedInstanceState = null;
	private final Handler mHandler = new Handler();
	private final ScheduledExecutorService mScheduledExecutor = Executors.newScheduledThreadPool(1);
	
	private final Runnable mSaveAccountsRunnable = new Runnable() {
		
		@Override
		public void run() {
			mStorage.saveAccounts(mAccounts);
		}
	};

	private final ProtocolListener mProtocolListener = new ProtocolListener() {

		@Override
		public void onAction(ProtocolService protocol, ItemAction action) {
			/*for (int i = mAccounts.size() - 1; i >= 0; i--) {
				AccountService acs = mAccounts.get(i);
				if (acs != null && acs.getProtocolService().getProtocolServicePackageName().equals(protocol.getProtocolServicePackageName())) {
					AccountService newAcs = initAccount(acs.getAccount());
					mAccounts.set(i, newAcs);
				}
			}*/
			
			if (!mAccountsReady) {
				return;
			}

			switch(action) {
			case JOINED:
				for (AccountService as : mAccounts) {
					if (as != null && as.getProtocolService().getProtocolServicePackageName().equals(protocol.getProtocolServicePackageName())) {
						try {
							Account a = as.getAccount();
							as.getProtocolService().getProtocol().addAccount(a.getServiceId(), a.getProtocolUid());
							mProtocolCallback.connectionStateChanged(as.getAccount().getServiceId(), ConnectionState.DISCONNECTED, -1);
						} catch (RemoteException e) {
							Logger.log(e);
						}
					}
				}
				break;
			case LEFT:
				//TODO notification
				break;
			default:
				if (mInterface != null) {
					try {
						mInterface.terminate();
						exitService(true);
					} catch (RemoteException e) {
						Logger.log(e);
					}
				}
				break;
			}
		}
	};

	private final OnOptionChangedListener mOptionsReceiverListener = new OnOptionChangedListener() {

		@Override
		public void onOptionChanged(OptionKey key, String value, byte serviceId) {
			Account account = null;
			if (serviceId > -1) {
				AccountService acs = mAccounts.get(serviceId);
				if (acs != null) {
					account = acs.getAccount();
				}
			}

			onOptionChangedInternal(key, value, account);
		}
	};

	private final Runnable mInitProtocolsRunnable = new Runnable() {

		@Override
		public void run() {
			Logger.log("Init protocols", LoggerLevel.VERBOSE);
			mProtocolServiceManager.initProtocolServices();
			mProtocolsReady = true;
		}
	};

	private final Runnable mInitAccountsRunnable = new Runnable() {

		@Override
		public void run() {
			initAccounts();
			initApplicationOptions();
			initAutoconnection();
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		//uiVisible = true;
		return mInterfaceBinder;
	}

	private void initAutoconnection() {
		boolean autoconnect = getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getBoolean(GlobalOptionKeys.AUTOCONNECT.name(), false);

		for (AccountService a : mAccounts) {
			if (a == null || !a.getAccount().isEnabled()) {
				continue;
			}

			// if service connection was interrupted or auto connection is
			// toggled, do the connection
			if (a.getAccount().getConnectionState() != ConnectionState.DISCONNECTED || autoconnect) {
				a.getAccount().setConnectionState(ConnectionState.DISCONNECTED);
				connectInternal(a.getAccount().getServiceId());
			}
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mInterface = null;
		//uiVisible = false;
		return false;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		initLocals();

		// To avoid deadlocks between several service binders (UI & protocols),
		// we should distinguish their initializing from main lifecycle methods
		ThreadFactory tf = Executors.defaultThreadFactory();
		tf.newThread(mInitProtocolsRunnable).start();
		tf.newThread(mInitAccountsRunnable).start();
	}

	@Override
	public void onDestroy() {
		try {
			cleanupResources();
		} catch (Exception e) {
		}

		super.onDestroy();
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

	private void handleCommand(final Intent intent) {
		if (intent == null || !intent.hasExtra(Constants.INTENT_EXTRA_CLASS_NAME)) {
			return;
		}

		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				String classNameExtra = intent.getStringExtra(Constants.INTENT_EXTRA_CLASS_NAME);
				if (classNameExtra.equals(FileMessage.class.getName()) || classNameExtra.equals(ServiceMessage.class.getName())) {
					Message m = intent.getParcelableExtra(Constants.INTENT_EXTRA_MESSAGE);
					String accept = intent.getData().getHost();
					try {
						boolean doAcceptFile = accept.equals(getBaseContext().getString(android.R.string.ok));
						mInterfaceBinder.respondMessage(m, doAcceptFile);
						
						AccountService acs = mAccounts.get(m.getServiceId());
						
						if (acs != null) {
							acs.getProtocolService().getProtocol().messageResponse(m, doAcceptFile);
						}
					} catch (RemoteException e) {
						Logger.log(e);
					}
				}
			}
		}).start();
	}

	private void initLocals() {
		PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
		powerLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AceIM Power Lock");
		powerLock.acquire();
		WifiManager wlanManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		if (wlanManager != null) {
			wifiLock = wlanManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "AceIM Wireless Lock");
			wifiLock.acquire();
		}
		sReconnectionAttempts = (byte) getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getInt(GlobalOptionKeys.RECONNECTION_ATTEMPTS.name(), Integer.parseInt(getString(R.string.default_reconnection_attempts)));
		mServiceHelper.doStartForeground();
		mOptionsReceiver = new OptionsReceiver(mOptionsReceiverListener);
		LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mOptionsReceiver, new IntentFilter(Constants.INTENT_ACTION_OPTION));
		mNotificator = new Notificator(getApplicationContext());
		mNotificator.setVolumeLevel(getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getInt(GlobalOptionKeys.SOUND_NOTIFICATION_VOLUME.name(), 100) / 100f);
		mStorage = new DataStorage(getApplicationContext());
		mHistorySaver = new JsonHistorySaver(getApplicationContext());
		mProtocolServiceManager = new ProtocolServicesManager(getApplicationContext(), mProtocolCallback, mProtocolListener);
		mLocationSender = new LocationSender(this);
	}

	private void initAccounts() {
		Logger.log("Init accounts... waiting for protocols", LoggerLevel.VERBOSE);
		while (!mProtocolsReady) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
		}

		Logger.log("Protocols got", LoggerLevel.VERBOSE);
		List<Account> accounts = mStorage.getAccounts();

		for (Account account : accounts) {
			if (account == null || account.getProtocolUid() == null) continue;
			AccountService acs = initAccount(account);
			
			boolean disabled = getSharedPreferences(account.getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.DISABLED.name(), false);
			account.setEnabled(!disabled);			
			
			mAccounts.add(acs);
			// test(account);
		}

		mAccountsReady = true;
	}

	private AccountService initAccount(Account account) {
		Logger.log("Init " + account.getAccountId(), LoggerLevel.VERBOSE);
		
		AccountService acc = ServiceUtils.makeAccount(account, mProtocolServiceManager);
		
		if (acc.getProtocolService() == null && !getBaseContext().getSharedPreferences(account.getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.DISABLED.getStringKey(), false)) {
			account.setEnabled(false);
			getBaseContext().getSharedPreferences(account.getAccountId(), ServiceUtils.getAccessMode()).edit().putBoolean(AccountOptionKeys.DISABLED.getStringKey(), true).commit();
			mNotificator.processException(new AceImException(AceImExceptionReason.NO_PROTOCOL_FOUND), account);
		}
		
		acc.setConnectionAttempts(sReconnectionAttempts);
		return acc;
	}

	private void initApplicationOptions() {
		Context c = getBaseContext();
		SharedPreferences options = c.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0);

		LEDNotificationMode ledMode = LEDNotificationMode.valueOf(options.getString(GlobalOptionKeys.LED_BLINKER.name(), c.getString(R.string.default_led_blinker)));
		mNotificator.setLEDNotificationMode(ledMode);

		SoundNotificationMode soundMode = SoundNotificationMode.valueOf(options.getString(GlobalOptionKeys.SOUND_NOTIFICATION_TYPE.name(), c.getString(R.string.default_sound_notification)));
		mNotificator.setSoundMode(soundMode);

		StatusBarNotificationMode statusbarMode = StatusBarNotificationMode.valueOf(options.getString(GlobalOptionKeys.STATUSBAR_NOTIFICATION_TYPE.name(), c.getString(R.string.default_statusbar_notification)));
		mNotificator.setStatusBarMode(statusbarMode);

		boolean messageSoundOnly = options.getBoolean(GlobalOptionKeys.MESSAGE_SOUND_ONLY.name(), Boolean.parseBoolean(c.getString(R.string.default_message_sound_only)));
		mNotificator.setMessageSoundOnly(messageSoundOnly);

		mNotificator.onAccountStateChanged(mAccounts);
	}

	private void onOptionChangedInternal(OptionKey key, String value, Account account) {

		if (key instanceof GlobalOptionKeys) {
			GlobalOptionKeys k = (GlobalOptionKeys) key;

			switch (k) {
			case LED_BLINKER:
				LEDNotificationMode ledMode = LEDNotificationMode.valueOf(value);
				mNotificator.setLEDNotificationMode(ledMode);
				break;
			case SOUND_NOTIFICATION_TYPE:
				SoundNotificationMode soundMode = SoundNotificationMode.valueOf(value);
				mNotificator.setSoundMode(soundMode);
				break;
			case SOUND_NOTIFICATION_VOLUME:
				int level = Integer.valueOf(value);
				mNotificator.setVolumeLevel(level / 100f);
				break;
			case RECONNECTION_ATTEMPTS:
				sReconnectionAttempts = Byte.parseByte(value);				
				break;
			case STATUSBAR_NOTIFICATION_TYPE:
				StatusBarNotificationMode barMode = StatusBarNotificationMode.valueOf(value);
				mNotificator.setStatusBarMode(barMode);

				mNotificator.onAccountStateChanged(mAccounts);
			case LOG_TO_FILE:
				boolean logToFile = Boolean.parseBoolean(value);
				Logger.logToFile = logToFile;
				for (ProtocolService protocol: mProtocolServiceManager.getProtocolsList()) {
					try {
						protocol.getProtocol().logToFile(logToFile);
					} catch (RemoteException e) {
						Logger.log(e);
					}
				}
				break;
			default:
				break;
			}
		}
	}

	private final ICoreService.Stub mInterfaceBinder = new ICoreService.Stub() {

		@Override
		public long sendMessage(Message message) throws RemoteException {
			AccountService as = mAccounts.get(message.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return 0;
			}
			
			Logger.log("UI sends message ", LoggerLevel.VERBOSE);
			Buddy buddy = getBuddy(message.getServiceId(), message.getContactUid());
			
			if (buddy.getOnlineInfo().getFeatures().containsKey(ApiConstants.FEATURE_BUDDY_RESOURCE)) {
				message.setContactDetail(buddy.getOnlineInfo().getFeatures().getString(ApiConstants.FEATURE_BUDDY_RESOURCE));
			}
			
			long messageId = as.getProtocolService().getProtocol().sendMessage(message);
			Logger.log("UI file sending got message ID #" + messageId);
			
			message.setMessageId(messageId);
			
			if (message instanceof FileMessage) {
				FileProgress progress = new FileProgress(message.getServiceId(), messageId, ((FileMessage)message).getFiles().get(0).getFilename(), 0, 0, false, message.getContactUid(), null);

				if (mInterface != null) {
					mInterface.onFileProgress(progress);
				}
			}
			
			mHistorySaver.saveMessage(buddy, message);
			return messageId;
		}

		@Override
		public Account createAccount(String protocolServiceClassName, List<ProtocolOption> options) throws RemoteException {
			Logger.log("UI creates account " + protocolServiceClassName, LoggerLevel.VERBOSE);
			
			String protocolName = mProtocolServiceManager.getProtocolServiceByName(protocolServiceClassName).getProtocol().getProtocolName();
			if (findAccountServiceByProtocolUidAndProtocolName(options.get(0).getValue(), protocolName) != null) {
				ViewUtils.showAlertToast(getBaseContext(), android.R.drawable.ic_menu_info_details, R.string.simple_placeholder, options.get(0).getValue());
				return null;
			} else {
				Account account = new Account((byte) mAccounts.size(), options.get(0).getValue(), protocolName, protocolServiceClassName);
				mStorage.saveAccount(account, options, true);
				mAccounts.add(initAccount(account));

				return account;
			}
		}

		@Override
		public void deleteAccount(Account account) throws RemoteException {
			AccountService as = mAccounts.get(account.getServiceId());
			
			if (as == null) {
				return;
			}
			
			if (as.getProtocolService() != null && as.getAccount().getConnectionState() != ConnectionState.DISCONNECTED) {
				as.getProtocolService().getProtocol().disconnect(account.getServiceId());
			}
			
			mNotificator.removeAccountIcon(account);
			mHistorySaver.removeAccount(account);
			ViewUtils.removeAccountIcons(account, getBaseContext());			
			mAccounts.set(account.getServiceId(), null);
			mStorage.removeAccount(account);
		}

		@Override
		public void editAccount(Account account, List<ProtocolOption> options, String protocolServicePackageName) throws RemoteException {
			AccountService as = mAccounts.get(account.getServiceId());
			
			if (as == null) {
				return;
			}
			
			Logger.log("UI edits account " + account.getAccountId(), LoggerLevel.VERBOSE);
			
			if (account.getConnectionState() == ConnectionState.CONNECTED) {
				mAccounts.get(account.getServiceId()).getProtocolService().getProtocol().disconnect(account.getServiceId());
			}

			if (protocolServicePackageName != null && !protocolServicePackageName.equals(account.getProtocolServicePackageName())) {
				Logger.log("Setting protocol class " + protocolServicePackageName + " to " + account.getAccountId(), LoggerLevel.VERBOSE);
				account = new Account(account.getServiceId(), account.getProtocolUid(), account.getProtocolName(), protocolServicePackageName);
				as = initAccount(account);
				
				mAccounts.set(account.getServiceId(), as);
			}
			
			if (account.isEnabled()) {
				mNotificator.onAccountStateChanged(mAccounts);
			} else {
				if (account.getConnectionState() != ConnectionState.CONNECTED) {
					mAccounts.get(account.getServiceId()).getProtocolService().getProtocol().disconnect(account.getServiceId());
				}
				mNotificator.removeAccountIcon(account);
			}

			mStorage.saveAccount(account, options, false);
			
			if (mInterface != null) {
				mInterface.onAccountUpdated(account, ItemAction.MODIFIED);
			}
		}

		@Override
		public List<Account> getAccounts(boolean disabledToo) throws RemoteException {
			Logger.log("UI requests accounts", LoggerLevel.VERBOSE);
			while (!mAccountsReady) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			List<Account> accounts = new ArrayList<Account>(mAccounts.size());

			for (AccountService a : mAccounts) {
				if (!disabledToo && !a.getAccount().isEnabled()) {
					continue;
				}
				accounts.add(a.getAccount());
			}

			return accounts;
		}

		@Override
		public Buddy getBuddy(byte serviceId, String buddyProtocolUid) throws RemoteException {
			Logger.log("UI requests buddy " + buddyProtocolUid, LoggerLevel.VERBOSE);
			return getAccount(serviceId).getBuddyByProtocolUid(buddyProtocolUid);
		}

		@Override
		public List<Buddy> getBuddies(byte serviceId, List<String> buddyProtocolUid) throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Account getAccount(byte serviceId) throws RemoteException {
			Logger.log("UI requests account " + serviceId, LoggerLevel.VERBOSE);
			return mAccounts.get(serviceId).getAccount();
		}

		@Override
		public void connect(byte serviceId) throws RemoteException {
			Logger.log("UI requests connect " + serviceId, LoggerLevel.VERBOSE);
			 connectInternal(serviceId);
		}

		@Override
		public void connectAll() throws RemoteException {
			for (byte i=0; i<mAccounts.size(); i++) {
				connectInternal(i);
			}
		}

		@Override
		public void disconnectAll() throws RemoteException {
			disconnectAllInternal(false);
		}

		@Override
		public void disconnect(byte serviceId) throws RemoteException {
			Logger.log("UI requests disconnect " + serviceId, LoggerLevel.VERBOSE);
			disconnectInternal(serviceId);
		}

		@Override
		public void resetUnread(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled()) {
				return;
			}
			
			Logger.log("UI requests unreads reset for " + buddy, LoggerLevel.VERBOSE);
			Account a = as.getAccount();
			Buddy b = a.getBuddyByBuddyId(buddy.getId());

			if (b != buddy && b.getUnread() > 0) {
				b.setUnread((byte) 0);
			}

			mNotificator.removeMessageNotification(b, mAccounts);
		}

		@Override
		public void addBuddy(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyAction(ItemAction.ADDED, buddy);
		}

		@Override
		public void removeBuddy(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}			
			
			if (buddy instanceof MultiChatRoom) {
				as.getProtocolService().getProtocol().leaveChatRoom(buddy.getServiceId(), buddy.getProtocolUid());
				as.getAccount().removeBuddyByUid(buddy.getProtocolUid());
				onContactListUpdated(as.getAccount());
			} else {
				as.getProtocolService().getProtocol().buddyAction(ItemAction.DELETED, buddy);
			}
		}

		@Override
		public void renameBuddy(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyAction(ItemAction.MODIFIED, buddy);
		}

		@Override
		public void moveBuddy(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyAction(ItemAction.MODIFIED, buddy);
		}

		@Override
		public void addGroup(BuddyGroup group) throws RemoteException {
			AccountService as = mAccounts.get(group.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyGroupAction(ItemAction.ADDED, group);
		}

		@Override
		public void removeGroup(BuddyGroup group) throws RemoteException {
			AccountService as = mAccounts.get(group.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyGroupAction(ItemAction.DELETED, group);
		}

		@Override
		public void renameGroup(BuddyGroup group) throws RemoteException {
			AccountService as = mAccounts.get(group.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyGroupAction(ItemAction.MODIFIED, group);
		}

		@Override
		public void setGroupCollapsed(byte serviceId, String groupId, boolean collapsed) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null) {
				return;
			}			
			
			as.getAccount().getBuddyGroupByGroupId(groupId).setCollapsed(collapsed);
			
			mStorage.saveAccount(as.getAccount(), false);
		}

		@Override
		public void requestBuddyShortInfo(byte serviceId, String uid) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().requestFullInfo(serviceId, uid, true);
		}

		@Override
		public void requestBuddyFullInfo(byte serviceId, String uid) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().requestFullInfo(serviceId, uid, false);
		}

		@Override
		public void respondMessage(Message msg, boolean accept) throws RemoteException {
			AccountService as = mAccounts.get(msg.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().messageResponse(msg, accept);
		}

		@Override
		public void cancelFileTransfer(byte serviceId, long messageId) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().cancelFileFransfer(serviceId, messageId);
		}

		@Override
		public void exit(boolean terminate) throws RemoteException {
			exitService(terminate);
		}

		@Override
		public void sendTyping(byte serviceId, String buddyUid) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("UI requests typing notification " + serviceId, LoggerLevel.VERBOSE);
			as.getProtocolService().getProtocol().sendTypingNotification(serviceId, buddyUid);
		}

		@Override
		public void editBuddy(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().buddyAction(ItemAction.MODIFIED, buddy);
		}

		@Override
		public void leaveChat(byte serviceId, String chatId) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().leaveChatRoom(serviceId, chatId);
		}

		@Override
		public void joinChat(byte serviceId, String chatId) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			boolean loadIcons = getBaseContext().getSharedPreferences(as.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			
			as.getProtocolService().getProtocol().joinChatRoom(serviceId, chatId, loadIcons);
		}

		@Override
		public void registerCallback(IUserInterface callback) throws RemoteException {
			Logger.log("UI callback registering", LoggerLevel.VERBOSE);
			mInterface = callback;
		}

		@Override
		public List<ProtocolResources> getAllProtocolResources(boolean getProtocolInfo) throws RemoteException {
			Logger.log("UI requests protocol resources", LoggerLevel.VERBOSE);
			while (!mAccountsReady) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			List<ProtocolService> protocols = mProtocolServiceManager.getProtocolsList();
			List<ProtocolResources> list = new ArrayList<ProtocolResources>(protocols.size());
			for (ProtocolService p : protocols) {
				list.add(p.getResources(getProtocolInfo));
			}
			return list;
		}

		@Override
		public List<ProtocolOption> getProtocolOptions(String protocolServiceClass, byte serviceId) throws RemoteException {
			Logger.log("UI requests protocol options for " + protocolServiceClass, LoggerLevel.VERBOSE);
			while (!mProtocolsReady) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			ProtocolOption[] options = mProtocolServiceManager.getProtocolServiceByName(protocolServiceClass).getProtocol().getProtocolOptions();

			if (serviceId > -1) {
				Account account = mAccounts.get(serviceId).getAccount();
				for (ProtocolOption o : options) {
					o.setValue(mStorage.getProtocolOptionValue(o.getKey(), account));
				}
			}

			return Arrays.asList(options);
		}

		@Override
		public void notifyUnread(Message message, Buddy buddy) throws RemoteException {
			Logger.log("UI requests message notification: " + message, LoggerLevel.VERBOSE);
			Account a = mAccounts.get(message.getServiceId()).getAccount();

			Buddy b = a.getBuddyByBuddyId(buddy.getId());
			// If we run CoreService in a different process, then buddy instances are different, so we need to update one within CoreService.
			if (b != buddy && b.getUnread() != buddy.getUnread()) {
				b.setUnread(buddy.getUnread());
			}

			mNotificator.onMessage(message, a);
		}

		@Override
		public List<Message> getLastMessages(Buddy buddy) throws RemoteException {
			Logger.log("UI requests last messages for " + buddy, LoggerLevel.VERBOSE);
			return mHistorySaver.getMessages(buddy);
		}

		@Override
		public List<Message> getMessages(Buddy buddy, int startFrom, int maxMessagesToRead) throws RemoteException {
			Logger.log("UI requests " + maxMessagesToRead + " messages for " + buddy + ", starting from " + startFrom, LoggerLevel.VERBOSE);
			return mHistorySaver.getMessages(buddy, startFrom, maxMessagesToRead);
		}

		@Override
		public boolean deleteMessagesHistory(Buddy buddy) throws RemoteException {
			return mHistorySaver.deleteHistory(buddy);
		}

		@Override
		public void saveInstanceState(Bundle bundle) throws RemoteException {
			mSavedInstanceState = bundle;
		}

		@Override
		public Bundle restoreInstanceState() throws RemoteException {
			return mSavedInstanceState;
		}

		@Override
		public void sendLocation(Buddy buddy) throws RemoteException {
			mLocationSender.requestLocationForBuddy(buddy);
		}

		@Override
		public void importAccounts(String password, FileProgress progress) throws RemoteException {
			ImportAndExport.importData(progress, password, mInterface, CoreService.this);
		}

		@Override
		public void exportAccounts(String password, FileProgress progress) throws RemoteException {
			ImportAndExport.exportData(progress, password, mInterface, getBaseContext());
		}

		@Override
		public void uploadAccountPhoto(byte serviceId, String filename) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().uploadAccountPhoto(serviceId, filename);
		}

		@Override
		public void removeAccountPhoto(byte serviceId) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			as.getProtocolService().getProtocol().removeAccountPhoto(serviceId);
			ViewUtils.removeIcon(getBaseContext(), as.getAccount().getFilename());
			
			if (mInterface != null) {
				try {
					mInterface.onAccountIcon(serviceId);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}

		@Override
		public void setFeature(String featureId, OnlineInfo info) throws RemoteException {
			AccountService as = mAccounts.get(info.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Account account = as.getAccount();
			
			as.getProtocolService().getProtocol().setFeature(featureId, info);
			
			if (info.getProtocolUid().equals(account.getProtocolUid())) {
				OnlineInfo onlineInfo = account.getOnlineInfo();
				
				if (onlineInfo != info) {
					//TODO fix for other feature types
					as.getAccount().getOnlineInfo().getFeatures().putByte(featureId, info.getFeatures().getByte(featureId));
					onlineInfo.setXstatusName(info.getXstatusName());
					onlineInfo.setXstatusDescription(info.getXstatusDescription());
				}
				
				if (mInterface != null) {
					mInterface.onAccountStateChanged(info);
				}
				
				mStorage.saveAccount(account, true);
			} else {
				Buddy b = account.getBuddyByProtocolUid(info.getProtocolUid());
				
				if (b != null && b.getOnlineInfo() != info) {
					b.getOnlineInfo().getFeatures().putByte(featureId, info.getFeatures().getByte(featureId));
				}
				
				if (mInterface != null) {
					mInterface.onBuddyStateChanged(Arrays.asList(b));
				}
			}
		}
	};

	private ICoreProtocolCallback.Stub mProtocolCallback = new ICoreProtocolCallback.Stub() {

		@Override
		public void typingNotification(byte serviceId, String ownerUid) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Typing notification from: " + ownerUid, LoggerLevel.VERBOSE);
			Account a = as.getAccount();
			Buddy b = a.getBuddyByProtocolUid(ownerUid);

			final Object icon;
			final String params;

			if (b != null) {
				icon = ViewUtils.getIcon(getBaseContext(), b.getFilename());
				params = b.getSafeName();
			} else {
				icon = new Object();
				params = ownerUid;
			}

			mHandler.post(new Runnable() {

				@Override
				public void run() {
					ViewUtils.showInformationToast(getBaseContext(), icon, R.string.logparam_typing, params);
				}
			});
		}

		@Override
		public void message(Message message) throws RemoteException {
			AccountService as = mAccounts.get(message.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Protocol's message: " + message, LoggerLevel.VERBOSE);
			
			Account a = as.getAccount();
			
			while (a.getConnectionState() == ConnectionState.CONNECTING) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
			}
			
			onMessageInternal(message);
		}

		@Override
		public void searchResult(byte serviceId, List<PersonalInfo> infoList) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Search result list for #" + serviceId + ": ", LoggerLevel.VERBOSE);
			
			if (mInterface != null) {
				mInterface.onSearchResult(serviceId, infoList);
			}
		}

		@Override
		public void personalInfo(PersonalInfo info, boolean isShortInfo) throws RemoteException {
			AccountService as = mAccounts.get(info.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			
			if (info.getProtocolUid().equals(as.getAccount().getProtocolUid())) {
				OnlineInfo aou = as.getAccount().getOnlineInfo();
				String name = info.getProperties().getString(PersonalInfo.INFO_NICK);
				if (!TextUtils.isEmpty(name)) {
					aou.setName(name);
					if (mInterface != null) {
						mInterface.onAccountStateChanged(aou);
					}
				}
			} else {
				//TODO use this somehow
			}		
			
			if (!isShortInfo) {
				if (mInterface != null) {
					mInterface.onPersonalInfo(info);
				}
			}
		}

		@Override
		public void notification(byte serviceId, final String message) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled()) {
				return;
			}
			
			Account a = as.getAccount();
			final Bitmap icon = ViewUtils.getIcon(getBaseContext(), a.getFilename());
			mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					ViewUtils.showInformationToast(getBaseContext(), icon, R.string.simple_placeholder, message);
				}
			});
		}

		@Override
		public void messageAck(byte serviceId, String ownerUid, long messageId, MessageAckState state) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Protocol's message ack " + state + " for " + ownerUid, LoggerLevel.VERBOSE);
			if (mInterface != null) {
				mInterface.onMessageAck(serviceId, messageId, ownerUid, state);
			}
		}

		@Override
		public void iconBitmap(final byte serviceId, final String ownerUid, byte[] data, String hash) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled()) {
				return;
			}
			
			Logger.log("Protocol's icon for " + ownerUid, LoggerLevel.VERBOSE);
			
			Account a = as.getAccount();
			if (a.getProtocolUid().equals(ownerUid)) {
				ViewUtils.storeImageFile(getBaseContext(), data, a.getFilename(), hash, new Runnable() {

					@Override
					public void run() {						
						BitmapAjaxCallback.clearCache();
						
						if (mInterface != null) {
							try {
								mInterface.onAccountIcon(serviceId);
							} catch (RemoteException e) {
								Logger.log(e);
							}
						}
					}
				});

			} else {
				Buddy b = a.getBuddyByProtocolUid(ownerUid);
				if (b != null) {
					ViewUtils.storeImageFile(getBaseContext(), data, b.getFilename(), hash, new Runnable() {

						@Override
						public void run() {
							BitmapAjaxCallback.clearCache();
							
							if (mInterface != null) {
								try {
									mInterface.onBuddyIcon(serviceId, ownerUid);
								} catch (RemoteException e) {
									Logger.log(e);
								}
							}
						}
					});

				} else {
					Logger.log("No target found for bitmap " + ownerUid);
				}
			}
		}

		@Override
		public void groupAction(ItemAction action, final BuddyGroup newGroup) throws RemoteException {
			AccountService as = mAccounts.get(newGroup.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			final Account account = as.getAccount();
			BuddyGroup group = account.getBuddyGroupByGroupId(newGroup.getId());
			
			int textId = 0;
			switch(action) {
			case DELETED:
				if (group != null) {
					account.getBuddyGroupList().remove(group);
					textId = R.string.X_deleted;
				}
				break;
			case ADDED:
				if (group != null) {
					Logger.log("Group with id #"+newGroup.getId()+" already exists", LoggerLevel.WARNING);
				} else {
					account.getBuddyGroupList().add(newGroup);
					textId = R.string.X_added;
				}
				break;
			case MODIFIED:
				if (group != null) {
					group.setName(newGroup.getName());
					textId = R.string.X_modified;
				} else {
					Logger.log("Group with id #"+newGroup.getId()+" not found", LoggerLevel.WARNING);
				}
				break;
			default:
				Logger.log("Unsupported group action: "+action, LoggerLevel.INFO);
			}
			
			if (textId != 0) {
				final int id = textId;
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						ViewUtils.showInformationToast(getBaseContext(), ViewUtils.getIcon(getBaseContext(), account.getFilename()), id, newGroup.getName());
					}
				});
			}
			
			onContactListUpdated(account);
		}

		@Override
		public void fileProgress(FileProgress fp) throws RemoteException {
			AccountService as = mAccounts.get(fp.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			mNotificator.onFileTransferProgress(fp);

			if (mInterface != null) {
				mInterface.onFileProgress(fp);
			}
		}

		@Override
		public void connectionStateChanged(byte serviceId, ConnectionState connState, int extraParameter) throws RemoteException {
			Logger.log("Protocol's connection state " + connState + " for account #" + serviceId, LoggerLevel.VERBOSE);
			
			if (mExiting || serviceId >= mAccounts.size()) {
				Logger.log("Service is not yet inited", LoggerLevel.INFO);
				return;
			}

			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled()) {
				return;
			}
			
			final Account account = as.getAccount();
			
			if (connState != ConnectionState.CONNECTED) {
				for (Buddy b : account.getBuddyList()) {
					ViewUtils.resetFeaturesForOffline(b.getOnlineInfo(), as.getProtocolService().getResources(false), true);
				}
			}

			if (connState == ConnectionState.DISCONNECTED) {
				
				if (account.getConnectionState() != ConnectionState.DISCONNECTED && account.getConnectionState() != ConnectionState.DISCONNECTING) {
					byte reconnectionAttempts = as.getConnectionAttempts();
					if (reconnectionAttempts > 0) {
						as.setConnectionAttempts((byte) (reconnectionAttempts - 1));
						mScheduledExecutor.schedule(new ReconnectionTask(account.getServiceId()), 5, TimeUnit.SECONDS);
					}
				}
				
				if (extraParameter > -1) {
					final ProtocolException e = new ProtocolException(Cause.values()[extraParameter]);
					mNotificator.processException(e, account);	
				}
			}
			
			account.setConnectionState(connState);

			mNotificator.onAccountStateChanged(mAccounts);
			
			if (connState == ConnectionState.CONNECTED) {
				mStorage.saveServiceState(mAccounts);
				as.setConnectionAttempts(sReconnectionAttempts);
				as.resetConnectionTimeout();
			} else if (connState == ConnectionState.CONNECTING) {
				if (!as.isUnderConnectionMonitoring()) {
					as.setConnectionTimeoutAction(mScheduledExecutor.schedule(new ConnectionTimeoutTask(as), 120, TimeUnit.SECONDS));
				}
			}

			if (mInterface != null) {
				mInterface.onConnectionStateChanged(serviceId, connState, extraParameter);
			}
		}

		@Override
		public void buddyStateChanged(List<OnlineInfo> infos) throws RemoteException {
			if (infos == null || infos.size() < 1) {
				return;
			}
			
			AccountService as = mAccounts.get(infos.get(0).getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Protocol's buddy state " + infos, LoggerLevel.VERBOSE);
			
			boolean loadIcons = getBaseContext().getSharedPreferences(
					as.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			
			List<Buddy> affectedBuddies = new ArrayList<Buddy>(infos.size());
			for (OnlineInfo info : infos) {
				Buddy buddy = as.getAccount().getBuddyByProtocolUid(info.getProtocolUid());
				if (buddy == null) {
					Logger.log("Cannot find buddy for online info #" + info.getProtocolUid(), LoggerLevel.WARNING);
				} else {
					buddy.getOnlineInfo().merge(info);
					
					affectedBuddies.add(buddy);
					if (loadIcons) {
						checkIcon(buddy.getFilename(), buddy.getOnlineInfo(), as);
					}
				}
			}
			
			if (mInterface != null) {
				mInterface.onBuddyStateChanged(affectedBuddies);
			}
		}

		@Override
		public void buddyListUpdated(byte serviceId, List<BuddyGroup> buddyList) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled()) {
				return;
			}
			
			Logger.log("Protocol's buddy list updated for account #" + serviceId, LoggerLevel.VERBOSE);
			
			boolean saveNotInList = getBaseContext().getSharedPreferences(as.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.SAVE_NOT_IN_LIST.name(), Boolean.parseBoolean(getBaseContext().getString(R.string.default_save_not_in_list)));
			as.getAccount().removeAllBuddies(saveNotInList);
			as.getAccount().setBuddyList(buddyList);

			onContactListUpdated(as.getAccount());
			
			Executors.defaultThreadFactory().newThread(new IconChecker(as)).start();
		}

		@Override
		public void buddyAction(ItemAction action, Buddy newBuddy) throws RemoteException {
			AccountService as = mAccounts.get(newBuddy.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Buddy #" + newBuddy + " with action: " + action, LoggerLevel.VERBOSE);			
			
			final Account account = as.getAccount();
			Buddy oldBuddy = account.getBuddyByProtocolUid(newBuddy.getProtocolUid());
			
			int textId = 0;
			switch(action){
			case ADDED:
				textId = R.string.X_added;
			case MODIFIED:
				if (textId == 0) {
					textId = R.string.X_modified;
				}
			case JOINED:
				if (oldBuddy != null) {
					//account.removeBuddyByUid(newBuddy.getProtocolUid());
					oldBuddy.merge(newBuddy);
					newBuddy = oldBuddy;
				} else {
					account.addBuddyToList(newBuddy);
					if (textId != 0) {
						final int id = textId;
						final String param1 = newBuddy.getSafeName();
						mHandler.post(new Runnable() {
							
							@Override
							public void run() {
								ViewUtils.showInformationToast(getBaseContext(), ViewUtils.getIcon(getBaseContext(), account.getFilename()), id, param1);
							}
						});
					}
				}
				
				if (newBuddy instanceof MultiChatRoom) {
					MultiChatRoom mcr = (MultiChatRoom) newBuddy;
					
					for (BuddyGroup occupantsGroup : mcr.getOccupants()) {
						for (Buddy occupant : occupantsGroup.getBuddyList()) {
							if (occupant.getProtocolUid().equals(account.getProtocolUid())) {
								occupant.getOnlineInfo().merge(account.getOnlineInfo());
							} else {
								Buddy b = account.getBuddyByProtocolUid(occupant.getProtocolUid());
								if (b != null) {
									occupant.merge(b);
								}
							}
						}
					}
				}
				
				if (mInterface != null) {
					mInterface.onBuddyStateChanged(Arrays.asList(newBuddy));
				}
				
				break;
			case LEFT:
				if (oldBuddy != null) {
					//account.removeBuddyByUid(newBuddy.getProtocolUid());
					oldBuddy.merge(newBuddy);
					newBuddy = oldBuddy;
					newBuddy.getOnlineInfo().getFeatures().remove(ApiConstants.FEATURE_STATUS);
					
					if (mInterface != null) {
						mInterface.onBuddyStateChanged(Arrays.asList(newBuddy));
					}
				}
				break;
			case DELETED:
				if (oldBuddy != null) {
					BuddyGroup oldGroup = account.getBuddyGroupByGroupId(oldBuddy.getGroupId());
					oldGroup.getBuddyList().remove(oldBuddy);
					final String param2 = newBuddy.getSafeName();
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							ViewUtils.showInformationToast(getBaseContext(), ViewUtils.getIcon(getBaseContext(), account.getFilename()), R.string.X_deleted, param2);
						}
					});
				}
				break;
			}
			
			onContactListUpdated(account);
		}

		@Override
		public void accountStateChanged(OnlineInfo info) throws RemoteException {
			if (info == null) {
				return;
			}

			AccountService as = mAccounts.get(info.getServiceId());
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}
			
			Logger.log("Protocol's account state " + info, LoggerLevel.VERBOSE);
			
			Account account = as.getAccount();
			
			account.setOwnName(info.getName());
			account.getOnlineInfo().merge(info);

			mInterface.onAccountUpdated(account, ItemAction.MODIFIED);

			boolean loadIcons = getBaseContext().getSharedPreferences(
					as.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			if (loadIcons) {
				checkIcon(account.getFilename(), account.getOnlineInfo(), as);
			}
		}

		@Override
		public String requestPreference(byte serviceId, String preferenceName) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled()) {
				return null;
			}
			
			Logger.log("Protocol requests value of " + preferenceName + " for account #" + serviceId, LoggerLevel.VERBOSE);
			return mStorage.getProtocolOptionValue(preferenceName, as.getAccount());
		}

		@Override
		public void accountActivity(byte serviceId, String text) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public void multiChatParticipants(byte serviceId, String chatUid, List<BuddyGroup> participantList) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void showFeatureInputForm(byte serviceId, String uid, InputFormFeature feature) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}			
			
			if (mInterface != null) {
				mInterface.showFeatureInputForm(serviceId, uid, feature);
			}
		}
	};

	public void exitService(boolean terminate) {
		Logger.log("Preparing exit", LoggerLevel.VERBOSE);
		mExiting = true;
		
		disconnectAllInternal(terminate);		
		cleanupResources();		
		
		if (!terminate) {
			Executors.defaultThreadFactory().newThread(mSaveAccountsRunnable).start();
		}
		
		mServiceHelper.doStopForeground();
		stopSelf();
	}

	private void disconnectInternal(byte serviceId) {
		AccountService as = mAccounts.get(serviceId);
		
		if (as == null 
				|| as.getAccount().getConnectionState() == ConnectionState.DISCONNECTED 
				|| as.getAccount().getConnectionState() == ConnectionState.DISCONNECTING) {
			return;
		}
		
		try {
			as.getAccount().setConnectionState(ConnectionState.DISCONNECTED);
			as.resetConnectionTimeout();
			
			as.getProtocolService().getProtocol().disconnect(serviceId);
			
			if (mInterface != null) {
				try {
					mInterface.onConnectionStateChanged(as.getAccount().getServiceId(), as.getAccount().getConnectionState(), -1);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}

			mStorage.saveServiceState(mAccounts);
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	private void connectInternal(byte serviceId) {
		AccountService as = mAccounts.get(serviceId);
		
		if (as == null || !as.getAccount().isEnabled() || as.getAccount().getConnectionState() != ConnectionState.DISCONNECTED) {
			Logger.log("Trying to connect disabled or already connected account " + as.getAccount().getAccountId(), LoggerLevel.INFO);
			return;
		}
		
		try {
			as.getProtocolService().getProtocol().connect(as.getAccount().getOnlineInfo());
			
			as.setConnectionTimeoutAction(mScheduledExecutor.schedule(new ConnectionTimeoutTask(as), 120, TimeUnit.SECONDS));
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	private void onContactListUpdated(Account account) throws RemoteException {
		mStorage.saveAccount(account, false);

		if (mInterface != null) {
			mInterface.onContactListUpdated(account);
		}
	}

	private void disconnectAllInternal(boolean doNotSave) {
		
		for (byte i=0; i<mAccounts.size(); i++) {
			disconnectInternal(i);
		}
		
		if (!doNotSave) {
			mStorage.saveServiceState(mAccounts);
		}
	}

	private void cleanupResources() {
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
			wifiLock = null;
		}
		if (powerLock != null && powerLock.isHeld()) {
			powerLock.release();
			powerLock = null;
		}
		
		mNotificator.removeAppIcon();

		for (AccountService as : mAccounts) {
			mNotificator.removeAccountIcon(as.getAccount());
		}
		
		mProtocolServiceManager.onExit();

		LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(mOptionsReceiver);
	}

	private void onMessageInternal(Message message) {
		AccountService as = mAccounts.get(message.getServiceId());
		Buddy buddy = as.getAccount().getBuddyByProtocolUid(message.getContactUid());

		if (buddy == null) {
			Logger.log("No buddy found for " + message, LoggerLevel.VERBOSE);
			boolean noAuthFromAliens = getBaseContext().getSharedPreferences(as.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.DENY_MESSAGES_FROM_ALIENS.name(), false);
			if (noAuthFromAliens) {
				return;
			}
			buddy = buddyNotFromList(message, as);
		}

		mHistorySaver.saveMessage(buddy, message);

		mNotificator.messageSound();

		if (message instanceof FileMessage) {
			mNotificator.onMessage(message, as.getAccount());

			if (mInterface != null) {
				try {
					mInterface.onMessage(message);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		} else {
			if (mInterface != null) {
				try {
					mInterface.onMessage(message);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			} else {
				buddy.incrementUnread();
				mNotificator.onMessage(message, as.getAccount());
			}
		}
		
		if (buddy instanceof MultiChatRoom && buddy.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1) < 0) {
			boolean loadIcons = getBaseContext().getSharedPreferences(as.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			
			try {
				as.getProtocolService().getProtocol().joinChatRoom(message.getServiceId(), buddy.getProtocolUid(), loadIcons);
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}
	}

	private Buddy buddyNotFromList(Message message, AccountService accountService) {
		Logger.log("Creating alien buddy instance: " + message.getContactUid(), LoggerLevel.VERBOSE);
		Account account = accountService.getAccount();
		IProtocolService protocol = accountService.getProtocolService().getProtocol();
		try {
			String protocolName = protocol.getProtocolName();
			final Buddy newBuddy = new Buddy(message.getContactUid(), account.getProtocolUid(), protocolName, message.getServiceId());
			newBuddy.setGroupId(ApiConstants.NOT_IN_LIST_GROUP_ID);
			account.addBuddyToList(newBuddy);

			if (mInterface != null) {
				mInterface.onContactListUpdated(account);
			}

			protocol.requestFullInfo(account.getServiceId(), newBuddy.getProtocolUid(), true);

			return newBuddy;
		} catch (RemoteException e) {
			Logger.log(e);
		}

		return null;
	}
	
	private void checkIcon(String filename, OnlineInfo info, AccountService accountService) {
		String hash = ViewUtils.getIconHash(getBaseContext(), filename);
		
		if (TextUtils.isEmpty(hash) || !hash.equals(info.getIconHash())) {
			try {
				accountService.getProtocolService().getProtocol().requestIcon(info.getServiceId(), info.getProtocolUid());
			} catch (Exception e) {
				Logger.log(e);
			}
		}
	}
	
	private AccountService findAccountServiceByProtocolUidAndProtocolName(String protocolUid, String protocolName) {
		for (AccountService as : mAccounts) {
			if (as != null && as.getAccount().getProtocolUid().equals(protocolUid) && as.getAccount().getProtocolName().equals(protocolName)) {
				return as;
			}
		}
		
		return null;
	}

	public void sendLocation(Buddy buddy, String url) {
		TextMessage message = new TextMessage(buddy.getServiceId(), buddy.getProtocolUid());
		message.setIncoming(false);
		message.setText(url);
		message.setTime(System.currentTimeMillis());

		try {
			mInterfaceBinder.sendMessage(message);
			if (mInterface != null) {
				mInterface.onMessage(message);
			}
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	public void importAccounts(List<Account> accounts) {
		for (Account a : accounts) {
			if (findAccountServiceByProtocolUidAndProtocolName(a.getProtocolUid(), a.getProtocolName()) != null) {
				Logger.log("Will not import existing account #" + a.getProtocolUid(), LoggerLevel.INFO);
				ViewUtils.showInformationToast(getBaseContext(), android.R.drawable.ic_menu_myplaces, R.string.simple_placeholder, a.getProtocolUid());
			} else {
				mAccounts.add(initAccount(a));
			}			
		}
		
		mStorage.saveServiceState(mAccounts);;
	}
	
	private final class ReconnectionTask implements Runnable {

		private final byte serviceId;

		private ReconnectionTask(byte serviceId) {
			this.serviceId = serviceId;
		}
		
		@Override
		public void run() {
			connectInternal(serviceId);
		}		
	}
	
	private final class ConnectionTimeoutTask implements Runnable {
		
		private final AccountService mAccountService;

		private ConnectionTimeoutTask(AccountService as) {
			this.mAccountService = as;
		}

		@Override
		public void run() {
			try {
				if (mAccountService.getAccount().getConnectionState() != ConnectionState.DISCONNECTED) {
					mAccountService.getProtocolService().getProtocol().disconnect(mAccountService.getAccount().getServiceId());
				}
				
				connectInternal(mAccountService.getAccount().getServiceId());
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}		
	}
	
	private final class IconChecker implements Runnable {
		
		private final AccountService accountService;

		IconChecker(AccountService accountService) {
			this.accountService = accountService;
		}

		@Override
		public void run() {
			boolean loadIcons = getBaseContext().getSharedPreferences(
					accountService.getAccount().getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			if (loadIcons) {
				for (Buddy b : accountService.getAccount().getBuddyList()) {
					checkIcon(b.getFilename(), b.getOnlineInfo(), accountService);
				}
			}			
		}
	}
}
