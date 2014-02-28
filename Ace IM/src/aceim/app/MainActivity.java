package aceim.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.ActivityResult;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.dataentity.listeners.IHasAccount;
import aceim.app.dataentity.listeners.IHasAccountList;
import aceim.app.dataentity.listeners.IHasBuddy;
import aceim.app.dataentity.listeners.IHasFilePicker;
import aceim.app.dataentity.listeners.IHasFileProgress;
import aceim.app.dataentity.listeners.IHasMessages;
import aceim.app.screen.Screen;
import aceim.app.service.ICoreService;
import aceim.app.service.IUserInterface;
import aceim.app.utils.DialogUtils;
import aceim.app.utils.LinqRules.AccountListPageLinqRule;
import aceim.app.utils.LinqRules.AccountPageLinqRule;
import aceim.app.utils.LinqRules.BuddyPageLinqRule;
import aceim.app.utils.LinqRules.FileProgressPageLinqRule;
import aceim.app.utils.LinqRules.MessagePageLinqRule;
import aceim.app.utils.LinqRules.ProtocolAccountPageLinqRule;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import aceim.app.view.page.chat.Chat;
import aceim.app.view.page.contactlist.ContactList;
import aceim.app.view.page.inputform.InputFormFeaturePage;
import aceim.app.view.page.transfers.FileTransfers;
import aceim.app.widgets.bottombar.BottomBarButtonInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.androidquery.callback.BitmapAjaxCallback;
import com.androidquery.util.AQUtility;

public class MainActivity extends AceIMActivity {
	
	private boolean mActivityVisible = false;

	private final Map<String, ProtocolResources> mProtocolResources = new HashMap<String, ProtocolResources>();
	private SmileysManager mSmileysManager;
	
	private ICoreService mCoreService;

	private Intent mCoreServiceIntent = null;
	private ActivityResult mActivityResult = null;

	private Screen mScreen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Logger.log("Starting main activity", LoggerLevel.VERBOSE);

		super.onCreate(savedInstanceState);
		
		//Debug.startMethodTracing();
		
		prepareImagesCache();

		mScreen = Screen.getScreen(this);
		setContentView(mScreen);
		Page.addSplash(mScreen);

		mSmileysManager = new SmileysManager(this);
		mSmileysManager.resetSmileysPopup();
		
		initCoreService();
	}
	
	/**
	 * This method initializes cache with common image resources, 
	 * so they can be used despite of the context the particular view is built within. 
	 */
	private void prepareImagesCache() {
		BitmapAjaxCallback.setCacheLimit(Integer.MAX_VALUE);
		
		BitmapAjaxCallback.getMemoryCached(getBaseContext(), R.drawable.dummy_icon);
		BitmapAjaxCallback.getMemoryCached(getBaseContext(), R.drawable.btn_check_off);
		BitmapAjaxCallback.getMemoryCached(getBaseContext(), R.drawable.btn_check_off_disable);
		BitmapAjaxCallback.getMemoryCached(getBaseContext(), R.drawable.btn_check_on);
		BitmapAjaxCallback.getMemoryCached(getBaseContext(), R.drawable.btn_check_on_disable);
		BitmapAjaxCallback.getMemoryCached(getBaseContext(), R.drawable.btn_check_on_selected);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mActivityVisible = true;
		
		//mScreen.setSelectedPage(mScreen.getSelectedPage().getPageId());
		
		mScreen.getSelectedPage().onSetMeSelected();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mActivityVisible = false;
	}

	private final Runnable mCoreServiceConnectionWaiter = new Runnable() {

		@Override
		public void run() {
			try {
				List<ProtocolResources> protocols = mCoreService.getAllProtocolResources(false);
				for (ProtocolResources r : protocols) {
					mProtocolResources.put(r.getPackageId(), r);
				}
				runOnUiThread(mContinueCreating);
			} catch (RemoteException e) {
				onRemoteException(e);
			}
		}
	};

	private final Runnable mContinueCreating = new Runnable() {

		@Override
		public void run() {
			continueCreating();
		}
	};

	private void initScreen() {
		String masterPw = getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getString(GlobalOptionKeys.MASTER_PASSWORD.name(), null);
		Boolean needPassword = masterPw != null && masterPw.length() > 0;
		if (needPassword) {
			Page.addPasswordPage(mScreen);
			Page.removeSplash(mScreen);
			return;
		}

		proceedInitScreen();
	}

	public void proceedInitScreen() {
		List<Account> accounts;

		try {
			accounts = mCoreService.getAccounts(false);
		} catch (RemoteException e) {
			onRemoteException(e);
			return;
		}

		initAccounts(accounts);

		Page.removeSplash(mScreen);

		initSavedData(accounts);
		restoreActivityResult();
	}

	private void initSavedData(List<Account> accounts) {
		Bundle savedState;
		try {
			savedState = mCoreService.restoreInstanceState();
		} catch (RemoteException e) {
			onRemoteException(e);
			return;
		}
		if (savedState == null) {
			return;
		}

		List<String> pages = savedState.getStringArrayList(Constants.SAVED_STATE_PAGES);
		String selectedPage = savedState.getString(Constants.SAVED_STATE_SELECTED_PAGE);

		if (pages != null) {
			for (String pageId : pages) {
				Page.recoverPageById(accounts, pageId, (Bundle) savedState.getParcelable(pageId), this);
			}
		}

		if (selectedPage != null) {
			mScreen.setSelectedPage(selectedPage);
		}
	}

	private void initCoreService() {
		if (mCoreServiceIntent == null) {
			mCoreServiceIntent = new Intent("aceim.app.service.CoreService");
		}
		startService(mCoreServiceIntent);
		bindService(mCoreServiceIntent, mCoreServiceConnection, 0);
	}

	private final ServiceConnection mCoreServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (mCoreService == null) {
				mCoreService = ICoreService.Stub.asInterface(service);
				try {
					mCoreService.registerCallback(mCoreServiceCallback);
					Executors.defaultThreadFactory().newThread(mCoreServiceConnectionWaiter).start();
				} catch (Exception e) {
					Logger.log(e);
				}
			} else {
				Logger.log("onServiceConnected run again");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			finish();
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString(Constants.DISABLED_SUFFIX, Constants.DISABLED_SUFFIX);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		Logger.log("Destroy", LoggerLevel.VERBOSE);
		super.onDestroy();
		
		mSmileysManager.onExit();		
		saveInstanceState();
		unbindService(mCoreServiceConnection);
		AQUtility.cleanCacheAsync(this);
		
		//Debug.stopMethodTracing();
	}
	
	@Override
	public void onLowMemory() {
		Logger.log("Low memory", LoggerLevel.VERBOSE);
		BitmapAjaxCallback.clearCache();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		setIntent(intent);
		
		String classNameExtra;
		if (intent == null || (classNameExtra = intent.getStringExtra(Constants.INTENT_EXTRA_CLASS_NAME)) == null) {
			return;
		}

		if (classNameExtra.equals(Chat.class.getName())) {
			Buddy b = intent.getParcelableExtra(Constants.INTENT_EXTRA_BUDDY);
			Account a = intent.getParcelableExtra(Constants.INTENT_EXTRA_ACCOUNT);
			Page.getChatPage(mScreen, b, a);
		} else if (classNameExtra.equals(ContactList.class.getName())) {
			Account a = intent.getParcelableExtra(Constants.INTENT_EXTRA_ACCOUNT);
			Page.getContactListPage(this, a);
		} else if (classNameExtra.equals(FileTransfers.class.getName())) {
			Account a = intent.getParcelableExtra(Constants.INTENT_EXTRA_ACCOUNT);
			Page.getFileTransfersPage(mScreen, a);
			processFileTransferIntent(intent);
		}
	}

	private void processFileTransferIntent(Intent intent) {
		Buddy b = intent.getParcelableExtra(Constants.INTENT_EXTRA_BUDDY);
		FileMessage m = intent.getParcelableExtra(Constants.INTENT_EXTRA_MESSAGE);

		DialogUtils.showFileMessageDialog(m, b, this);
	}

	private void saveInstanceState() {
		Bundle bundle = new Bundle();
		List<Page> openedPages = mScreen.getAllPages();

		ArrayList<String> storingPages = new ArrayList<String>(openedPages.size());

		for (Page page : openedPages) {

			if (ViewUtils.allowPageStoring(page)) {
				storingPages.add(page.getPageId());

				bundle.putParcelable(page.getPageId(), page.getPageDataForStorage());
			}
		}

		try {
			bundle.putStringArrayList(Constants.SAVED_STATE_PAGES, storingPages);
			Page selectedPage = mScreen.getSelectedPage();
			if (selectedPage != null) {
				bundle.putString(Constants.SAVED_STATE_SELECTED_PAGE, selectedPage.getPageId());
			}
		} catch (Exception e1) {
			Logger.log(e1);
		}

		mScreen.storeScreenSpecificData(bundle);

		try {
			if (mCoreService != null) {
				mCoreService.saveInstanceState(bundle);
			}
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	private void continueCreating() {
		initScreen();
	}

	private void addNoProtocolsInstalledMessagePage() {
		Resources r = getResources();

		String title = r.getString(R.string.app_name);
		Drawable icon = r.getDrawable(android.R.drawable.ic_dialog_info);
		Drawable buttonIcon = r.getDrawable(R.drawable.ic_launcher_market_holo);

		BottomBarButtonInfo button = new BottomBarButtonInfo(buttonIcon) {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(Constants.MARKET_SEARCH_PROTOCOL_URI));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				try {
					startActivity(i);
				} catch (Exception e) {
					Logger.log(e);
				}
			}
		};

		Page.addMessagePage(mScreen, title, icon, R.string.no_protocol_services_installed_found, Arrays.asList(button));
	}

	public void initAccounts(List<Account> accounts) {
		try {
			if (mProtocolResources.size() > 0) {
				if (accounts.size() > 0) {
					for (Account a : accounts) {
						Page.getContactListPage(this, a);
					}
					mScreen.setSelectedPage(Page.getPageIdForEntityWithId(ContactList.class, accounts.get(0)));
				} 

				if (mScreen.getAllPages().size() < 2) { //Splash and at least one Contact list should present
					Page.addAccountManagerPage(mScreen, mCoreService.getAccounts(true));
				}
			} else {
				if (accounts.size() > 0) {
					Page.addAccountManagerPage(mScreen, mCoreService.getAccounts(true));
				} else {
					addNoProtocolsInstalledMessagePage();
				}
			}
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	/*
	 * public void updateOption(Account account, String key, String value){
	 * final byte serviceId = account != null ? account.serviceId : -1; try {
	 * mCoreService.saveOption(key, value, serviceId); } catch (RemoteException
	 * e) { onRemoteException(e); }
	 * 
	 * if (serviceId > -1) { for (Page p : mScreen.findPagesByRule(new
	 * AccountPageLinqRule(serviceId))) { //((IHasAccount)p).update(); } } }
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logger.log("Activity result", LoggerLevel.VERBOSE);

		this.mActivityResult = new ActivityResult(requestCode, resultCode, data);

		if (mScreen != null && mScreen.getSelectedPage() instanceof IHasFilePicker) {
			restoreActivityResult();
		}
	}

	public void onRemoteException(RemoteException e) {
		Logger.log(e);
	}

	public ProtocolResources getProtocolResourcesForAccount(Account account) {
		return mProtocolResources.get(account.getProtocolServicePackageName());
	}

	private void restoreActivityResult() {
		Logger.log("Restore activity result", LoggerLevel.VERBOSE);

		if (mActivityResult == null) {
			return;
		}

		Page page = mScreen.getSelectedPage();
		if (page != null && page instanceof IHasFilePicker) {
			((IHasFilePicker) page).onFilePicked(mActivityResult, this);
		}

		mActivityResult = null;
	}

	private final IUserInterface mCoreServiceCallback = new IUserInterface.Stub() {

		@Override
		public void onProtocolUpdated(ProtocolResources resources, ItemAction action) throws RemoteException {
			switch (action) {
			case ADDED:
				mProtocolResources.put(resources.getPackageId(), resources);
				break;
			case MODIFIED:
				mProtocolResources.put(resources.getPackageId(), resources);
				for (Page p : mScreen.findPagesByRule(new ProtocolAccountPageLinqRule(resources.getPackageId()))) {
					Account account = ((IHasAccount) p).getAccount();
					((IHasAccount) p).onConnectionStateChanged(account.getConnectionState(), -1);
				}
				break;
			case DELETED:
				for (Page p : mScreen.findPagesByRule(new ProtocolAccountPageLinqRule(resources.getPackageId()))) {
					mScreen.removePage(p);
				}
				mProtocolResources.remove(resources.getPackageId());
				break;
			default:
				Logger.log("Unsupportable action: " + action, LoggerLevel.DEBUG);
				break;
			}
		}

		@Override
		public void onConnectionStateChanged(final byte serviceId, final ConnectionState connState, final int extraParameter) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new AccountPageLinqRule(serviceId))) {
						((IHasAccount) p).onConnectionStateChanged(connState, extraParameter);
						mScreen.updateTabWidget(p);
					}
				}
			});
		}

		@Override
		public void onContactListUpdated(final Account account) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new AccountPageLinqRule(account.getServiceId()))) {
						((IHasAccount) p).onContactListUpdated(account);
					}
				}
			});
		}

		@Override
		public void onBuddyStateChanged(final List<Buddy> buddies) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new BuddyPageLinqRule(buddies))) {
						((IHasBuddy) p).onBuddyStateChanged(buddies);
					}
				}
			});
		}

		@Override
		public void onMessage(final Message message) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					try {
						if (message instanceof FileMessage) {
							DialogUtils.showFileMessageDialog((FileMessage) message, mCoreService.getBuddy(message.getServiceId(), message.getContactUid()), MainActivity.this);
						} else if (message instanceof ServiceMessage) {
							ServiceMessage sm = (ServiceMessage) message;
							if (sm.isRequireAcceptDeclineAnswer()) {
								DialogUtils.showAcceptDeclineDialog(message, mCoreService.getBuddy(message.getServiceId(), message.getContactUid()), MainActivity.this);
							}
						} else {
							for (Page p : mScreen.findPagesByRule(new MessagePageLinqRule(message.getServiceId(), message.getContactUid()))) {
								((IHasMessages) p).onMessageReceived(message);
								mScreen.updateTabWidget(p);
							}
						}
					} catch (RemoteException e) {
						onRemoteException(e);
					}
				}
			});
		}

		@Override
		public void onMessageAck(final byte serviceId, final long messageId, final String senderUid, final MessageAckState ackState) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new MessagePageLinqRule(serviceId, senderUid))) {
						((IHasMessages) p).onMessageAckReceived(messageId, ackState);
					}
				}
			});
		}

		@Override
		public void onAccountIcon(final byte serviceId) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new AccountPageLinqRule(serviceId))) {
						((IHasAccount) p).onAccountIcon(serviceId);
					}
				}
			});
		}

		@Override
		public void onBuddyIcon(final byte serviceId, final String buddyProtocolUid) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new BuddyPageLinqRule(serviceId, buddyProtocolUid))) {
						((IHasBuddy) p).onBuddyIcon(serviceId, buddyProtocolUid);
						mScreen.updateTabWidget(p);
					}
				}

			});
		}

		@Override
		public void onFileProgress(final FileProgress progress) throws RemoteException {

			final FileProgressPageLinqRule rule = new FileProgressPageLinqRule();
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					List<Page> pages = mScreen.findPagesByRule(rule);
					if (pages.size() < 1 && progress.getServiceId() > -1) {
						try {
							Page.getFileTransfersPage(mScreen, mCoreService.getAccount(progress.getServiceId()));
							pages = mScreen.findPagesByRule(rule);
						} catch (RemoteException e) {
							onRemoteException(e);
						}
					}

					for (Page p : pages) {
						((IHasFileProgress) p).onFileProgress(progress);
						mScreen.updateTabWidget(p);
					}
				}
			});
		}

		@Override
		public void onAccountStateChanged(final OnlineInfo info) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (Page p : mScreen.findPagesByRule(new AccountPageLinqRule(info.getServiceId()))) {
						((IHasAccount) p).onOnlineInfoChanged(info);
						mScreen.updateTabWidget(p);
					}
				}
			});
		}

		@Override
		public void onSearchResult(byte serviceId, final List<PersonalInfo> infoList) throws RemoteException {
			final Account a = mCoreService.getAccount(serviceId);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Page.addSearchResultPage(getScreen(), a, infoList, (getScreen().getSelectedPage() instanceof InputFormFeaturePage));
				}
			});
		}

		@Override
		public void onPersonalInfo(final PersonalInfo info) throws RemoteException {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Page.getPersonalInfoPage(mScreen, info);
				}
			});
		}

		@Override
		public void showFeatureInputForm(final byte serviceId, final String uid, final InputFormFeature feature) throws RemoteException {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Account a = mCoreService.getAccount(serviceId);
						ProtocolResources p = getProtocolResourcesForAccount(a);
						
						OnlineInfo info;
						if (a.getProtocolUid().equals(uid)) {
							info = a.getOnlineInfo();
						} else {
							Buddy b = a.getBuddyByProtocolUid(uid);
							if (b != null) {
								info = b.getOnlineInfo();
							} else {
								Logger.log("Could not find buddy with uid #" + uid, LoggerLevel.INFO);
								return;
							}
						}
						
						Page.getInputFormPage(MainActivity.this, feature, info, p);
						
					} catch (RemoteException e) {
						onRemoteException(e);
					}					
				}
			});
		}

		@Override
		public void terminate() throws RemoteException {
			finish();
		}

		@Override
		public void onAccountUpdated(final Account account, final ItemAction action) throws RemoteException {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					switch (action) {
					case MODIFIED:
						List<Page> pages = mScreen.findPagesByRule(new AccountPageLinqRule(account.getServiceId()));
						
						if (account.isEnabled()) {
							if (pages.size() > 0) {
								for (Page page : pages) {
									IHasAccount accountPage = (IHasAccount) page;
									accountPage.onOnlineInfoChanged(account.getOnlineInfo());
								}
							} else {
								Page.getContactListPage(MainActivity.this, account);
							}
						} else {
							for (Page page : pages) {
								mScreen.removePage(page);
							}
						}
						break;
					case ADDED:
						Page.getContactListPage(MainActivity.this, account);
						break;
					case DELETED:
						onAccountRemoved(account);
						break;
					default:
						Logger.log("Unsupported operation " + action, LoggerLevel.DEBUG);
						break;
					}
				}
			});
		}
	};

	public ICoreService getCoreService() {
		return mCoreService;
	}

	public Map<String, ProtocolResources> getProtocolResources() {
		return mProtocolResources;
	}

	public Screen getScreen() {
		return mScreen;
	}

	public void exitApplication() {
		try {
			mCoreService.exit(false);
		} catch (RemoteException e) {
			onRemoteException(e);
		}

		mCoreService = null;
		finish();
	}
	
	public void openOptions(Account mAccount) {
		Intent i = ViewUtils.getOpenOptionsIntent(this, mAccount);
		startActivity(i);
		
		//We need delay to smoother activities change
		getScreen().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				finish();
				
				//Forcing MainActivity to die, as of its "singleTask" lifecycle, 
				//which does not guarantee destroying with simple finish().
				android.os.Process.killProcess(android.os.Process.myPid());				
			}
		}, 500);
	}

	public void accountAdded(Account account) {
		Page.getContactListPage(this, account);
		List<Page> accountListPages = mScreen.findPagesByRule(new AccountListPageLinqRule());

		for (Page p : accountListPages) {
			((IHasAccountList) p).onAccountAdded(account);
		}
	}

	public void accountRemoved(Account account) {
		try {
			mCoreService.deleteAccount(account);

			onAccountRemoved(account);
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	private void onAccountRemoved(Account account) {
		List<Page> accountPages = mScreen.findPagesByRule(new AccountPageLinqRule(account.getServiceId()));
		for (Page p : accountPages) {
			mScreen.removePage(p);
		}

		List<Page> accountListPages = mScreen.findPagesByRule(new AccountListPageLinqRule());

		for (Page p : accountListPages) {
			((IHasAccountList) p).onAccountRemoved(account);
		}
	}

	public void onChatRequest(Buddy buddy) {
		try {
			Account account = getCoreService().getAccount(buddy.getServiceId());
			Page.getChatPage(mScreen, buddy, account);

			resetUnread(buddy);
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	public void resetUnread(Buddy buddy) {
		buddy.setUnread((byte) 0);
		
		if (mCoreService != null) {
			try {
				mCoreService.resetUnread(buddy);
			} catch (RemoteException e) {
				onRemoteException(e);
			}
		}

		for (Page p : mScreen.findPagesByRule(new BuddyPageLinqRule(buddy))) {
			((IHasBuddy) p).onBuddyStateChanged(Arrays.asList(buddy));
			mScreen.updateTabWidget(p);
		}
	}
	
	/*@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			return true;
		} else {
			return super.onKeyLongPress(keyCode, event);
		}        
    }*/

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mScreen.onCurrentPageKeyDown(keyCode, event)) {
			return true;
		} else {
			if (keyCode == KeyEvent.KEYCODE_BACK) {

				// if master password is required, then force destroying
				// activity for password being asked during next activity start
				String masterPw = getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getString(GlobalOptionKeys.MASTER_PASSWORD.name(), null);
				Boolean needPassword = masterPw != null && masterPw.length() > 0;

				if (needPassword) {
					finish();
					return true;
				} else {
					return super.onKeyDown(keyCode, event);
				}
			} else {
				return super.onKeyDown(keyCode, event);
			}
		}
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu){
	 * mScreen.onCreateOptionsMenu(menu, new MenuInflater(getBaseContext()));
	 * return true; }
	 */

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();

		mScreen.onCreateOptionsMenu(menu, new MenuInflater(getBaseContext()));
		mScreen.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mScreen.onOptionsItemSelected(item);
		return true;
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		ActivityCompat.invalidateOptionsMenu(this);
	}
	
	/*@Override
	protected void onPause() {
		super.onPause();
		if (mCoreService != null) {
			try {
				mCoreService.setUIVisible(false);
			} catch (RemoteException e) {
				onRemoteException(e);
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mCoreService != null) {
			try {
				mCoreService.setUIVisible(true);
			} catch (RemoteException e) {
				onRemoteException(e);
			}
		}
	}*/

	public void onBuddyContextMenuRequest(Buddy buddy, ProtocolResources resources) {
		try {
			Account a = mCoreService.getAccount(buddy.getServiceId());
			if (a.getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}

			DialogUtils.buddyContextMenu(this, a, buddy, resources);
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	public void onBuddyGroupContextMenuRequest(BuddyGroup g, ProtocolResources resources) {
		try {
			Account a = mCoreService.getAccount(g.getServiceId());
			if (a.getConnectionState() != ConnectionState.CONNECTED) {
				return;
			}

			DialogUtils.buddyGroupContextMenu(this, a, g, resources);
		} catch (RemoteException e) {
			onRemoteException(e);
		}
	}

	public SmileysManager getSmileysManager() {
		return mSmileysManager;
	}
	
	/**
	 * @return the mActivityVisible
	 */
	public boolean isActivityVisible() {
		return mActivityVisible;
	}
}
