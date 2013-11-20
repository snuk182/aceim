package aceim.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
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
import aceim.app.dataentity.ProtocolService;
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
	
	private final Runnable mSaveAccountsRunnable = new Runnable() {
		
		@Override
		public void run() {
			mStorage.saveAccounts(mAccounts);
		}
	};

	private final ProtocolListener mProtocolListener = new ProtocolListener() {

		@Override
		public void onAction(ProtocolService protocol, ItemAction action) {
			for (int i = mAccounts.size() - 1; i >= 0; i--) {
				AccountService acs = mAccounts.get(i);
				if (acs.getProtocolService().getProtocolServicePackageName().equals(protocol.getProtocolServicePackageName())) {
					AccountService newAcs = initAccount(acs.getAccount());
					mAccounts.set(i, newAcs);
				}
			}

			if (mInterface != null) {
				try {
					//mInterface.onProtocolUpdated(protocol.getResources(), action);
					mInterface.terminate();
					exitService(true);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}
	};

	private final OnOptionChangedListener mOptionsReceiverListener = new OnOptionChangedListener() {

		@Override
		public void onOptionChanged(OptionKey key, String value, byte serviceId) {
			Account account;
			if (serviceId < 0) {
				account = null;
			} else {
				account = mAccounts.get(serviceId).getAccount();
			}

			// mStorage.savePreference(key, value, account);

			onOptionChangedInternal(key, value, account);
		}
	};

	private final Runnable mInitProtocolsRunnable = new Runnable() {

		@Override
		public void run() {
			Logger.log("Init protocols", LoggerLevel.VERBOSE);
			mProtocolServiceManager.initProtocolServices(null);
			mProtocolServiceManager.addProtocolListener(mProtocolListener);
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
			if (!a.getAccount().isEnabled()) {
				continue;
			}

			// if service connection was interrupted or auto connection is
			// toggled, do the connection
			if (a.getAccount().getConnectionState() != ConnectionState.DISCONNECTED || autoconnect) {
				a.getAccount().setConnectionState(ConnectionState.CONNECTING);
				try {
					a.getProtocolService().getProtocol().connect(a.getAccount().getOnlineInfo());
				} catch (RemoteException e) {
					Logger.log(e);
					a.getAccount().setConnectionState(ConnectionState.DISCONNECTED);
					if (mInterface != null) {
						try {
							mInterface.onConnectionStateChanged(a.getAccount().getServiceId(), a.getAccount().getConnectionState(), -1);
						} catch (RemoteException e1) {
							Logger.log(e1);
						}
					}
				}
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
						mAccounts.get(m.getServiceId()).getProtocolService().getProtocol().messageResponse(m, doAcceptFile);
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

		mServiceHelper.doStartForeground();
		mOptionsReceiver = new OptionsReceiver(mOptionsReceiverListener);
		LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mOptionsReceiver, new IntentFilter(Constants.INTENT_ACTION_OPTION));
		mNotificator = new Notificator(getApplicationContext());
		mStorage = new DataStorage(getApplicationContext());
		mHistorySaver = new JsonHistorySaver(getApplicationContext());
		mProtocolServiceManager = new ProtocolServicesManager(getApplicationContext(), mProtocolCallback);
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
			mAccounts.add(acs);
			// test(account);
		}

		mAccountsReady = true;
	}

	private AccountService initAccount(Account account) {
		Logger.log("Init " + account.getAccountId(), LoggerLevel.VERBOSE);
		AccountService acc = ServiceUtils.makeAccount(account, mProtocolServiceManager.getProtocols());
		if (acc.getProtocolService() == null && !getBaseContext().getSharedPreferences(account.getAccountId(), ServiceUtils.getAccessMode()).getBoolean(AccountOptionKeys.DISABLED.getStringKey(), false)) {
			account.setEnabled(false);
			getBaseContext().getSharedPreferences(account.getAccountId(), ServiceUtils.getAccessMode()).edit().putBoolean(AccountOptionKeys.DISABLED.getStringKey(), true).commit();
			mNotificator.processException(new AceImException(AceImExceptionReason.NO_PROTOCOL_FOUND), account);
		}
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

		for (AccountService as : mAccounts) {
			mNotificator.onAccountStateChanged(as);
		}
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
			case STATUSBAR_NOTIFICATION_TYPE:
				StatusBarNotificationMode barMode = StatusBarNotificationMode.valueOf(value);
				mNotificator.setStatusBarMode(barMode);

				for (AccountService as : mAccounts) {
					mNotificator.onAccountStateChanged(as);
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
			Logger.log("UI sends message ", LoggerLevel.VERBOSE);
			Buddy buddy = getBuddy(message.getServiceId(), message.getContactUid());
			
			if (buddy.getOnlineInfo().getFeatures().containsKey(ApiConstants.FEATURE_BUDDY_RESOURCE)) {
				message.setContactDetail(buddy.getOnlineInfo().getFeatures().getString(ApiConstants.FEATURE_BUDDY_RESOURCE));
			}
			
			long messageId = mAccounts.get(message.getServiceId()).getProtocolService().getProtocol().sendMessage(message);
			Logger.log("UI file sending got message ID #" + messageId);
			
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
			
			if (findAccountServiceByProtocolUid(options.get(0).getValue()) != null) {
				ViewUtils.showAlertToast(getBaseContext(), android.R.drawable.ic_menu_info_details, R.string.simple_placeholder, options.get(0).getValue());
				return null;
			} else {
				String protocolName = mProtocolServiceManager.getProtocols().get(protocolServiceClassName).getProtocol().getProtocolName();
				Account account = new Account((byte) mAccounts.size(), options.get(0).getValue(), protocolName, protocolServiceClassName);
				mStorage.saveAccount(account, options, true);
				mAccounts.add(initAccount(account));

				return account;
			}
		}

		@Override
		public void deleteAccount(Account account) throws RemoteException {
			mNotificator.removeAccountIcon(account);
			mHistorySaver.removeAccount(account);
			ViewUtils.removeAccountIcons(account, getBaseContext());			
			mAccounts.remove(account.getServiceId());
			mStorage.removeAccount(account);
		}

		@Override
		public void editAccount(Account account, List<ProtocolOption> options, String protocolServicePackageName) throws RemoteException {
			Logger.log("UI edits account " + account.getAccountId(), LoggerLevel.VERBOSE);
			
			if (account.getConnectionState() == ConnectionState.CONNECTED) {
				mAccounts.get(account.getServiceId()).getProtocolService().getProtocol().disconnect(account.getServiceId());
			}

			if (protocolServicePackageName != null && !protocolServicePackageName.equals(account.getProtocolServicePackageName())) {
				Logger.log("Setting protocol class " + protocolServicePackageName + " to " + account.getAccountId(), LoggerLevel.VERBOSE);
				account = new Account(account.getServiceId(), account.getProtocolUid(), account.getProtocolName(), protocolServicePackageName);
				AccountService as = initAccount(account);
				
				mAccounts.set(account.getServiceId(), as);
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
			AccountService as = mAccounts.get(serviceId);

			as.getProtocolService().getProtocol().connect(as.getAccount().getOnlineInfo());
		}

		@Override
		public void connectAll() throws RemoteException {
			for (AccountService as : mAccounts) {
				as.getProtocolService().getProtocol().connect(as.getAccount().getOnlineInfo());
			}
		}

		@Override
		public void disconnectAll() throws RemoteException {
			disconnectAllInternal(false);
		}

		@Override
		public void disconnect(byte serviceId) throws RemoteException {
			Logger.log("UI requests disconnect " + serviceId, LoggerLevel.VERBOSE);
			AccountService as = mAccounts.get(serviceId);
			as.getAccount().setConnectionState(ConnectionState.DISCONNECTED);
			as.getProtocolService().getProtocol().disconnect(serviceId);

			mStorage.saveServiceState(mAccounts);
		}

		@Override
		public void resetUnread(Buddy buddy) throws RemoteException {
			Logger.log("UI requests unreads reset for " + buddy, LoggerLevel.VERBOSE);
			AccountService as = mAccounts.get(buddy.getServiceId());
			Account a = as.getAccount();
			Buddy b = a.getBuddyByBuddyId(buddy.getId());

			if (b != buddy && b.getUnread() > 0) {
				b.setUnread((byte) 0);
			}

			mNotificator.removeMessageNotification(b, as);
		}

		@Override
		public void addBuddy(Buddy buddy) throws RemoteException {
			mAccounts.get(buddy.getServiceId()).getProtocolService().getProtocol().buddyAction(ItemAction.ADDED, buddy);
		}

		@Override
		public void removeBuddy(Buddy buddy) throws RemoteException {
			AccountService as = mAccounts.get(buddy.getServiceId());
			
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
			mAccounts.get(buddy.getServiceId()).getProtocolService().getProtocol().buddyAction(ItemAction.MODIFIED, buddy);
		}

		@Override
		public void moveBuddy(Buddy buddy) throws RemoteException {
			mAccounts.get(buddy.getServiceId()).getProtocolService().getProtocol().buddyAction(ItemAction.MODIFIED, buddy);
		}

		@Override
		public void addGroup(BuddyGroup group) throws RemoteException {
			mAccounts.get(group.getServiceId()).getProtocolService().getProtocol().buddyGroupAction(ItemAction.ADDED, group);
		}

		@Override
		public void removeGroup(BuddyGroup group) throws RemoteException {
			mAccounts.get(group.getServiceId()).getProtocolService().getProtocol().buddyGroupAction(ItemAction.DELETED, group);
		}

		@Override
		public void renameGroup(BuddyGroup group) throws RemoteException {
			mAccounts.get(group.getServiceId()).getProtocolService().getProtocol().buddyGroupAction(ItemAction.MODIFIED, group);
		}

		@Override
		public void setGroupCollapsed(byte serviceId, String groupId, boolean collapsed) throws RemoteException {
			Account a = mAccounts.get(serviceId).getAccount();
			
			a.getBuddyGroupByGroupId(groupId).setCollapsed(collapsed);
			
			mStorage.saveAccount(a, false);
		}

		@Override
		public void requestBuddyShortInfo(byte serviceId, String uid) throws RemoteException {
			mAccounts.get(serviceId).getProtocolService().getProtocol().requestFullInfo(serviceId, uid, true);
		}

		@Override
		public void requestBuddyFullInfo(byte serviceId, String uid) throws RemoteException {
			mAccounts.get(serviceId).getProtocolService().getProtocol().requestFullInfo(serviceId, uid, false);
		}

		@Override
		public void respondMessage(Message msg, boolean accept) throws RemoteException {
			mAccounts.get(msg.getServiceId()).getProtocolService().getProtocol().messageResponse(msg, accept);
		}

		@Override
		public void cancelFileTransfer(byte serviceId, long messageId) throws RemoteException {
			mAccounts.get(serviceId).getProtocolService().getProtocol().cancelFileFransfer(serviceId, messageId);
		}

		@Override
		public void exit(boolean terminate) throws RemoteException {
			exitService(terminate);
		}

		@Override
		public void sendTyping(byte serviceId, String buddyUid) throws RemoteException {
			Logger.log("UI requests typing notification " + serviceId, LoggerLevel.VERBOSE);
			mAccounts.get(serviceId).getProtocolService().getProtocol().sendTypingNotification(serviceId, buddyUid);
		}

		@Override
		public void editBuddyVisibility(Buddy buddy) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public void editBuddy(Buddy buddy) throws RemoteException {
			mAccounts.get(buddy.getServiceId()).getProtocolService().getProtocol().buddyAction(ItemAction.MODIFIED, buddy);
		}

		@Override
		public void editMyVisibility(byte serviceId, byte visibility) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public void requestAvailableChatRooms(byte serviceId) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public void createChat(byte serviceId, String chatId, List<Buddy> invitedBuddies) throws RemoteException {
			
		}

		@Override
		public void leaveChat(byte serviceId, String chatId) throws RemoteException {
			mAccounts.get(serviceId).getProtocolService().getProtocol().leaveChatRoom(serviceId, chatId);
		}

		@Override
		public void joinChat(byte serviceId, String chatId) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
			
			boolean loadIcons = getBaseContext().getSharedPreferences(as.getAccount().getAccountId(), 0).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			
			as.getProtocolService().getProtocol().joinChatRoom(serviceId, chatId, loadIcons);
		}

		@Override
		public void registerCallback(IUserInterface callback) throws RemoteException {
			Logger.log("UI callback registering", LoggerLevel.VERBOSE);
			mInterface = callback;
		}

		@Override
		public List<ProtocolResources> getAllProtocolResources() throws RemoteException {
			Logger.log("UI requests protocol resources", LoggerLevel.VERBOSE);
			while (!mAccountsReady) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			Map<String, ProtocolService> protocols = mProtocolServiceManager.getProtocols();
			List<ProtocolResources> list = new ArrayList<ProtocolResources>(protocols.size());
			for (ProtocolService p : protocols.values()) {
				list.add(p.getResources());
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

			List<ProtocolOption> options = mProtocolServiceManager.getProtocols().get(protocolServiceClass).getProtocol().getProtocolOptions();

			if (serviceId > -1) {
				Account account = mAccounts.get(serviceId).getAccount();
				for (ProtocolOption o : options) {
					o.setValue(mStorage.getProtocolOptionValue(o.getKey(), account));
				}
			}

			return options;
		}

		@Override
		public void notifyUnread(Message message, Buddy buddy) throws RemoteException {
			Logger.log("UI requests message notification: " + message, LoggerLevel.VERBOSE);
			Account a = mAccounts.get(message.getServiceId()).getAccount();

			Buddy b = a.getBuddyByBuddyId(buddy.getId());
			// If we run CoreService in a different proccess, then buddy
			// instances are different, so we need to update one within
			// CoreService.
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
			mAccounts.get(serviceId).getProtocolService().getProtocol().uploadAccountPhoto(serviceId, filename);
		}

		@Override
		public void removeAccountPhoto(byte serviceId) throws RemoteException {
			AccountService as = mAccounts.get(serviceId);
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
					mInterface.onBuddyStateChanged(b);
				}
			}
		}

		/*@Override
		public void setUIVisible(boolean visible) throws RemoteException {
			uiVisible = visible;
		}*/
	};

	private ICoreProtocolCallback.Stub mProtocolCallback = new ICoreProtocolCallback.Stub() {

		@Override
		public void typingNotification(final byte serviceId, final String ownerUid) throws RemoteException {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					Logger.log("Typing notification from: " + ownerUid, LoggerLevel.VERBOSE);
					Account a = mAccounts.get(serviceId).getAccount();
					Buddy b = a.getBuddyByProtocolUid(ownerUid);

					Object icon;
					String params;

					if (b != null) {
						icon = ViewUtils.getIcon(getBaseContext(), b.getFilename());
						params = b.getSafeName();
					} else {
						icon = new Object();
						params = ownerUid;
					}

					ViewUtils.showInformationToast(getBaseContext(), icon, R.string.logparam_typing, params);
				}
			});
		}

		@Override
		public void message(Message message) throws RemoteException {
			Logger.log("Protocol's message: " + message, LoggerLevel.VERBOSE);
			onMessageInternal(message);
		}

		@Override
		public void searchResult(byte serviceId, List<PersonalInfo> infoList) throws RemoteException {
			Logger.log("Search result list for #" + serviceId + ": ", LoggerLevel.VERBOSE);
			
			if (mInterface != null) {
				mInterface.onSearchResult(serviceId, infoList);
			}
		}

		@Override
		public void personalInfo(PersonalInfo info) throws RemoteException {
			AccountService as = mAccounts.get(info.getServiceId());
			
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
				if (mInterface != null) {
					mInterface.onPersonalInfo(info);
				}
			}		
		}

		@Override
		public void notification(byte serviceId, final String message) throws RemoteException {
			mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					ViewUtils.showInformationToast(getBaseContext(), null, R.string.simple_placeholder, message);
				}
			});
		}

		@Override
		public void messageAck(byte serviceId, String ownerUid, long messageId, MessageAckState state) throws RemoteException {
			Logger.log("Protocol's message ack " + state + " for " + ownerUid, LoggerLevel.VERBOSE);
			if (mInterface != null) {
				mInterface.onMessageAck(serviceId, messageId, ownerUid, state);
			}
		}

		@Override
		public void iconBitmap(final byte serviceId, final String ownerUid, byte[] data, String hash) throws RemoteException {
			Logger.log("Protocol's icon for " + ownerUid, LoggerLevel.VERBOSE);
			Account a = mAccounts.get(serviceId).getAccount();
			if (a.getProtocolUid().equals(ownerUid)) {
				ViewUtils.storeImageFile(getBaseContext(), data, a.getFilename(), new Runnable() {

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
					ViewUtils.storeImageFile(getBaseContext(), data, b.getFilename(), new Runnable() {

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
			AccountService accountService = mAccounts.get(newGroup.getServiceId());
			final Account account = accountService.getAccount();
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
			mNotificator.onFileTransferProgress(fp);

			if (mInterface != null) {
				mInterface.onFileProgress(fp);
			}
		}

		@Override
		public void connectionStateChanged(byte serviceId, ConnectionState connState, int extraParameter) throws RemoteException {
			Logger.log("Protocol's connection state " + connState + " for account #" + serviceId, LoggerLevel.VERBOSE);
			
			if (mExiting) {
				return;
			}

			if (serviceId >= mAccounts.size()) {
				Logger.log("Service is not yet inited", LoggerLevel.INFO);
			}

			AccountService service = mAccounts.get(serviceId);
			Account account = service.getAccount();

			if (connState == ConnectionState.DISCONNECTED) {
				boolean reconnect = false;

				if (account.getConnectionState() != ConnectionState.DISCONNECTED && extraParameter < 0) {
					reconnect = true;
				}

				if (extraParameter > -1) {
					mNotificator.processException(new ProtocolException(Cause.values()[extraParameter]), account);
				}

				if (reconnect) {
					reconnect(service);
				} else {
					mStorage.saveServiceState(mAccounts);
				}
			}
			
			if (connState != ConnectionState.CONNECTED) {
				for (Buddy b : account.getBuddyList()) {
					ViewUtils.resetFeaturesForOffline(b.getOnlineInfo(), service.getProtocolService().getResources(), true);
				}
			}

			account.setConnectionState(connState);

			mNotificator.onAccountStateChanged(service);

			if (mInterface != null) {
				mInterface.onConnectionStateChanged(serviceId, connState, extraParameter);
			}

			if (connState == ConnectionState.CONNECTED) {
				mStorage.saveServiceState(mAccounts);
			}
		}

		@Override
		public void buddyStateChanged(OnlineInfo info) throws RemoteException {
			Logger.log("Protocol's buddy state " + info, LoggerLevel.VERBOSE);
			AccountService accountService = mAccounts.get(info.getServiceId());

			Buddy buddy = accountService.getAccount().getBuddyByProtocolUid(info.getProtocolUid());
			if (buddy == null) {
				return;
			}

			buddy.getOnlineInfo().merge(info);

			boolean loadIcons = getBaseContext().getSharedPreferences(accountService.getAccount().getAccountId(), 0).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			if (loadIcons) {
				for (Buddy b : accountService.getAccount().getBuddyList()) {
					accountService.getProtocolService().getProtocol().requestIcon(info.getServiceId(), b.getProtocolUid());
				}
			}

			if (mInterface != null) {
				mInterface.onBuddyStateChanged(buddy);
			}
		}

		@Override
		public void buddyListUpdated(byte serviceId, List<BuddyGroup> buddyList) throws RemoteException {
			Logger.log("Protocol's buddy list updated for account #" + serviceId, LoggerLevel.VERBOSE);
			AccountService accountService = mAccounts.get(serviceId);

			boolean saveNotInList = getBaseContext().getSharedPreferences(accountService.getAccount().getAccountId(), 0).getBoolean(AccountOptionKeys.SAVE_NOT_IN_LIST.name(), Boolean.parseBoolean(getBaseContext().getString(R.string.default_save_not_in_list)));
			accountService.getAccount().removeAllBuddies(saveNotInList);
			accountService.getAccount().setBuddyList(buddyList);

			boolean loadIcons = getBaseContext().getSharedPreferences(accountService.getAccount().getAccountId(), 0).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			if (loadIcons) {
				for (Buddy b : accountService.getAccount().getBuddyList()) {
					accountService.getProtocolService().getProtocol().requestIcon(serviceId, b.getProtocolUid());
				}
			}

			onContactListUpdated(accountService.getAccount());
		}

		@Override
		public void buddyAction(ItemAction action, Buddy newBuddy) throws RemoteException {
			Logger.log("Buddy #" + newBuddy + " with action: " + action, LoggerLevel.VERBOSE);			
			
			AccountService accountService = mAccounts.get(newBuddy.getServiceId());
			final Account account = accountService.getAccount();
			Buddy oldBuddy = account.getBuddyByProtocolUid(newBuddy.getProtocolUid());
			
			int textId = 0;
			switch(action){
			case ADDED:
				textId = R.string.X_added;
			case MODIFIED:
			case JOINED:
			case LEFT:
				if (textId == 0) {
					textId = R.string.X_modified;
				}
				
				if (oldBuddy != null) {
					//account.removeBuddyByUid(newBuddy.getProtocolUid());
					oldBuddy.merge(newBuddy);
					newBuddy = oldBuddy;
				} else {
					account.addBuddyToList(newBuddy);
					final int id = textId;
					final String param1 = newBuddy.getSafeName();
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							ViewUtils.showInformationToast(getBaseContext(), ViewUtils.getIcon(getBaseContext(), account.getFilename()), id, param1);
						}
					});
				}
				
				if (mInterface != null) {
					mInterface.onBuddyStateChanged(newBuddy);
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

			Logger.log("Protocol's account state " + info, LoggerLevel.VERBOSE);
			AccountService accountService = mAccounts.get(info.getServiceId());

			Account account = accountService.getAccount();
			
			account.setOwnName(info.getName());
			account.getOnlineInfo().merge(info);

			// mInterface.onAccountUpdated(account);

			boolean loadIcons = getBaseContext().getSharedPreferences(account.getAccountId(), 0).getBoolean(AccountOptionKeys.LOAD_ICONS.name(),
					Boolean.parseBoolean(getBaseContext().getString(R.string.default_load_icons)));
			if (loadIcons) {
				accountService.getProtocolService().getProtocol().requestIcon(info.getServiceId(), account.getProtocolUid());
			}
		}

		@Override
		public String requestPreference(byte serviceId, String preferenceName) throws RemoteException {
			Logger.log("Protocol requests value of " + preferenceName + " for account #" + serviceId, LoggerLevel.VERBOSE);
			AccountService accountService = mAccounts.get(serviceId);
			return mStorage.getProtocolOptionValue(preferenceName, accountService.getAccount());
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
			if (mInterface != null) {
				mInterface.showFeatureInputForm(serviceId, uid, feature);
			}
		}
	};

	public void exitService(boolean terminate) {
		Logger.log("Preparing exit", LoggerLevel.VERBOSE);
		mExiting = true;
		
		cleanupResources();		
		disconnectAllInternal(terminate);		
		
		if (!terminate) {
			Executors.defaultThreadFactory().newThread(mSaveAccountsRunnable).start();
		}
		
		mServiceHelper.doStopForeground();
		stopSelf();
	}

	private void onContactListUpdated(Account account) throws RemoteException {
		mStorage.saveAccount(account, false);

		if (mInterface != null) {
			mInterface.onContactListUpdated(account);
		}
	}

	private void disconnectAllInternal(boolean doNotSave) {
		for (AccountService as : mAccounts) {
			Account a = as.getAccount();
			a.setConnectionState(ConnectionState.DISCONNECTED);
			
			if (mInterface != null) {
				try {
					mInterface.onConnectionStateChanged(a.getServiceId(), a.getConnectionState(), -1);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}
		
		if (!doNotSave) {
			mStorage.saveServiceState(mAccounts);
		}
	}

	private void reconnect(final AccountService service) {
		Executors.newScheduledThreadPool(3).schedule(new Runnable() {

			@Override
			public void run() {
				Account account = service.getAccount();
				if (account.getConnectionState() == ConnectionState.DISCONNECTED) {
					try {
						service.getProtocolService().getProtocol().connect(account.getOnlineInfo());
					} catch (RemoteException e) {
						Logger.log(e);
					}
				}
			}

		}, 2, TimeUnit.SECONDS);
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

		for (ProtocolService s : mProtocolServiceManager.getProtocols().values()) {
			try {
				s.getProtocol().shutdown();
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}

		LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(mOptionsReceiver);
	}

	private void onMessageInternal(Message message) {
		AccountService as = mAccounts.get(message.getServiceId());
		Buddy buddy = as.getAccount().getBuddyByProtocolUid(message.getContactUid());

		if (buddy == null) {
			Logger.log("No buddy found for " + message, LoggerLevel.VERBOSE);
			boolean noAuthFromAliens = getBaseContext().getSharedPreferences(as.getAccount().getAccountId(), 0).getBoolean(AccountOptionKeys.DENY_MESSAGES_FROM_ALIENS.name(), false);
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
	
	private AccountService findAccountServiceByProtocolUid(String protocolUid) {
		for (AccountService as : mAccounts) {
			if (as.getAccount().getProtocolUid().equals(protocolUid)) {
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
			if (findAccountServiceByProtocolUid(a.getProtocolUid()) != null) {
				Logger.log("Will not import existing account #" + a.getProtocolUid(), LoggerLevel.INFO);
				ViewUtils.showInformationToast(getBaseContext(), android.R.drawable.ic_menu_myplaces, R.string.simple_placeholder, a.getProtocolUid());
			} else {
				mAccounts.add(initAccount(a));
			}			
		}
		
		mStorage.saveServiceState(mAccounts);;
	}
}
