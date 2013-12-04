package aceim.app.view.page.contactlist;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.List;

import aceim.api.dataentity.ActionFeature;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ProtocolServiceFeatureTarget;
import aceim.api.dataentity.ToggleFeature;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;
import aceim.app.AceImException;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountOptionKeys;
import aceim.app.dataentity.ActivityResult;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.dataentity.listeners.IHasAccount;
import aceim.app.dataentity.listeners.IHasFilePicker;
import aceim.app.dataentity.listeners.IHasMessages;
import aceim.app.service.ICoreService;
import aceim.app.service.ServiceUtils;
import aceim.app.utils.DialogUtils;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import aceim.app.view.page.chat.Chat;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;

public abstract class ContactList extends Page implements IHasAccount, IHasMessages, IHasFilePicker {

	private static final String SAVE_PARAM_URI = "SaveParamUri";
	
	protected final Account mAccount;
	protected final ProtocolResources mProtocolResources;
	private final ContactListUpdater mUpdater;
	
	private Uri mAwaitingUri = null;
	
	private final OnLongClickListener mUpdateIconListener = new OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			if (mAccount.getConnectionState() != ConnectionState.CONNECTED || !mAccount.getOnlineInfo().getFeatures().getBoolean(ApiConstants.FEATURE_ACCOUNT_MANAGEMENT, false)) {
				return true;
			}
			
			Context context = getMainActivity();
			
			final Dialog dialog = new Dialog(context);
			dialog.setTitle(R.string.setting_account_icon);
			
			ListView list = new ListView(context);
			
			String[] buttonList;
			if (ViewUtils.hasIcon(context, mAccount.getFilename())) {
				buttonList = new String[]{context.getString(R.string.take_photo), context.getString(R.string.choose_photo), context.getString(R.string.remove_photo)};
			} else {
				buttonList = new String[]{context.getString(R.string.take_photo), context.getString(R.string.choose_photo)};
			}
			
			list.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, android.R.id.text1, buttonList));
			list.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					ContactList cl = ContactList.this;
					Intent intent = null;
					
					switch(position) {
					case 0:
						intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						
						mAwaitingUri = Uri.fromFile(Utils.createLocalFileForReceiving(android.text.format.DateFormat.getLongDateFormat(getMainActivity()).format(Calendar.getInstance().getTime()) + ".jpg", null, System.currentTimeMillis()));
						intent.putExtra(MediaStore.EXTRA_OUTPUT, mAwaitingUri);
						
						break;
					case 1:
						intent = new Intent(Intent.ACTION_GET_CONTENT);
						intent.setType("image/*");
						break;
					case 2:
						try {
							getMainActivity().getCoreService().removeAccountPhoto(mAccount.getServiceId());
						} catch (RemoteException e) {
							getMainActivity().onRemoteException(e);
						}
						dialog.dismiss();
						return;
					}
					
					int requestCode = ServiceUtils.getRequestCodeForActivity(cl.getAccount().getAccountId().hashCode());
					try {
						cl.getMainActivity().startActivityForResult(intent, requestCode);
					} catch (Exception e){
						Logger.log(e);
						ViewUtils.showAlertToast(getMainActivity(), android.R.drawable.ic_dialog_alert, R.string.no_app_for_picking_file_found, null);
					}
					
					dialog.dismiss();
				}
			});
			
			dialog.addContentView(list, new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
			dialog.setCancelable(true);
			DialogUtils.showBrandedDialog(dialog);
			return true;
		}
	};

	protected ContactList(Account mAccount, ProtocolResources mProtocolResources, Resources applicationResources) {
		this.mAccount = mAccount;
		this.mProtocolResources = mProtocolResources;
		this.mUpdater = new ContactListUpdater(mAccount.getServiceId(), applicationResources, getContactListAdapterClassName(), this.mProtocolResources);
		//this.setHasOptionsMenu(true);	
	}

	@SuppressWarnings("unchecked")
	public static ContactList createContactListPage(MainActivity activity, Account account) {
		if (account == null) {
			return null;
		}

		ProtocolResources resources = activity.getProtocolResourcesForAccount(account);
		
		if (resources == null) {
			Logger.log("Cannot find resources with id #" + account.getProtocolServicePackageName() + " for account #" + account.getProtocolUid(), LoggerLevel.WARNING);
			return null;
		}
		
		Resources appResources = activity.getResources();
		
		try {
			// let's try to resolve contact list class by it's name
			String contactListType = activity
					.getSharedPreferences(account.getAccountId(), 0)
					.getString(AccountOptionKeys.CONTACT_LIST_TYPE.name(), 
							activity.getString(R.string.default_contact_list_type));
			Class<ContactList> cls = (Class<ContactList>) Class.forName(contactListType);
			Constructor<ContactList> constructor = cls.getConstructor(Account.class, ProtocolResources.class, Resources.class);
			ContactList contactList = constructor.newInstance(account, resources, appResources);
			return contactList;
		} catch (NullPointerException e1) {
			Logger.log("No contact list type set", LoggerLevel.INFO);
		} catch (ClassNotFoundException e1) {
			Logger.log("Unknown contact list type " + e1.getMessage(), LoggerLevel.WTF);
		} catch (Exception e1) {
			Logger.log(e1);
		}
		return new GridContactList(account, resources, appResources);
	}

	@Override
	public Account getAccount() {
		return mAccount;
	}
	
	@Override
	public void onOnlineInfoChanged(OnlineInfo info) {
		if (mAccount.getOnlineInfo() != info && mAccount.getServiceId() == info.getServiceId()) {
			mAccount.getOnlineInfo().merge(info);
		}
	}
	
	@Override
	public void onConnectionStateChanged(ConnectionState connState, int extraParameter){
		if (mAccount.getConnectionState() != connState) {
			mAccount.setConnectionState(connState);
			
			if (mAccount.getConnectionState() != ConnectionState.CONNECTED) {
				for (Buddy b : mAccount.getBuddyList()) {
					ViewUtils.resetFeaturesForOffline(b.getOnlineInfo(), mProtocolResources, true);
				}
			}
		}
		
		if (mAccount.getConnectionState() == ConnectionState.DISCONNECTED) {
			onContactListUpdated(mAccount);
		}
	}
	
	
	@Override
	public View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = onCreateContactListView(inflater, group, saved);
		
		onOnlineInfoChanged(mAccount.getOnlineInfo());	
		onContactListUpdated(mAccount);
		
		new AQuery(view).id(R.id.image_icon).longClicked(mUpdateIconListener);
		
		return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle saver){
		saver.putParcelable(SAVE_PARAM_URI, mAwaitingUri);
	}
	
	@Override
	public Bundle getPageDataForStorage(){
		Bundle saver = new Bundle();
		
		onSaveInstanceState(saver);
		
		return saver;
	}
	
	@Override
	public void recoverFromStoredData(Bundle bundle){
		if (bundle == null) {
			return;
		}
		
		mAwaitingUri = bundle.getParcelable(SAVE_PARAM_URI);
	}
	
	@Override
	public void onContactListUpdated(Account account) {
		mUpdater.onContactListUpdated(account, getMainActivity());
	}	
	
	@Override
	public void onBuddyStateChanged(Buddy buddy) {
		mUpdater.onBuddyStateChanged(buddy);
	}
	
	@Override
	public Buddy getBuddy(){
		return null;
	}
	
	@Override
	public Buddy getBuddyWithParameters(byte serviceId, String protocolUid) {
		if (serviceId != mAccount.getServiceId()) {
			return null;
		}
		
		return mAccount.getBuddyByProtocolUid(protocolUid);
	}

	@Override
	public Drawable getIcon(Context context) {
		return ViewUtils.getAccountStatusIcon(context, mAccount, mProtocolResources);
	}

	@Override
	public String getTitle(Context context) {
		return mAccount.getSafeName();
	}
	
	@Override
	public String getPageId() {
		return getPageIdForEntityWithId(ContactList.class, mAccount);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.contact_list_menu, menu);		
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem connect = menu.findItem(R.id.menuitem_connect);
		if (mAccount.getConnectionState() == ConnectionState.DISCONNECTED) {
			connect.setIcon(R.drawable.ic_menu_login);
			connect.setTitle(R.string.connect);
		} else {
			connect.setIcon(R.drawable.ic_menu_logout);
			connect.setTitle(R.string.disconnect);
		}
		
		Resources r;
		try {
			r = mProtocolResources.getNativeResourcesForProtocol(null);
		} catch (AceImException e) {
			Logger.log(e);
			return;
		}
		
		Resources myRes = getMainActivity().getBaseContext().getResources();
		
		Bundle accountFeatures = mAccount.getOnlineInfo().getFeatures();
		
		if (mAccount.getConnectionState() != ConnectionState.CONNECTING) {
			
			for (String featureId : accountFeatures.keySet()) {
				ProtocolServiceFeature feature = mProtocolResources.getFeature(featureId);
				
				if (feature == null || !feature.isEditable() || !feature.isAppliedToTarget(ProtocolServiceFeatureTarget.ACCOUNT)) {
					continue;
				}
				
				int featureMenuId = feature.getFeatureId().hashCode();
				MenuItem item = menu.add(0, featureMenuId, 1, feature.getFeatureName());
				
				if (feature instanceof ListFeature) {
					
					ListFeature lf = (ListFeature) feature;
					
					Drawable icon;
					String title;
					byte value = accountFeatures.getByte(feature.getFeatureId(), (byte) -1);
					
					if (value > -1) {
						icon = r.getDrawable(lf.getDrawables()[value]);
						title = r.getString(lf.getNames()[value]);
					} else {
						icon = myRes.getDrawable(R.drawable.empty);
						title = lf.getFeatureName();
					}
					
					item.setIcon(icon);
					item.setTitle(title);
				} else if (feature instanceof InputFormFeature || feature instanceof ActionFeature) {
					if (feature.getIconId() != 0){
						item.setIcon(r.getDrawable(feature.getIconId()));
					}
				} else if (feature instanceof ToggleFeature) {
					ToggleFeature tf = (ToggleFeature) feature;
					item.setIcon(tf.getValue() ? android.R.drawable.button_onoff_indicator_on : android.R.drawable.button_onoff_indicator_off);
				}
			}
		}
		
		MenuItem addGroup = menu.findItem(R.id.menuitem_add_group);
		if (addGroup != null) {
			addGroup.setVisible(mAccount.getConnectionState() == ConnectionState.CONNECTED && accountFeatures.getBoolean(ApiConstants.FEATURE_GROUP_MANAGEMENT, false));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MainActivity activity = getMainActivity();
		ICoreService service = activity.getCoreService();
		
		try {
			switch(item.getItemId()) {
			case R.id.menuitem_connect:
				if (mAccount.getConnectionState() == ConnectionState.DISCONNECTED) {
					service.connect(mAccount.getServiceId());
				} else {
					service.disconnect(mAccount.getServiceId());
				}
				break;
			case R.id.menuitem_exit:
				activity.exitApplication();
				break;
			case R.id.menuitem_account:
				//Page.addAccountEditorPage(activity.getScreen(), mAccount);
				Intent i = ViewUtils.getOpenOptionsIntent(getMainActivity(), mAccount);
				activity.startActivity(i);
				activity.finish();
				break;
			case R.id.menuitem_accounts:
				List<Account> accounts = getMainActivity().getCoreService().getAccounts(true);
				Page.addAccountManagerPage(activity.getScreen(), accounts);
				break;
			case R.id.menuitem_prefs:
				i = ViewUtils.getOpenOptionsIntent(getMainActivity(), null);
				activity.startActivity(i);
				activity.finish();
				break;
			case R.id.menuitem_add_group:
				DialogUtils.showAddOrRenameGroupDialog(null, mAccount, activity);
				break;
			case R.id.menuitem_about:
				Page.getAboutPage(activity.getScreen());
				break;
			default:
				ProtocolServiceFeature feature = mProtocolResources.getFeature(item.getItemId());
				
				if (feature instanceof ListFeature) {
					ListFeature lf = (ListFeature) feature;
					
					DialogUtils.showEditListFeatureDialog(getMainActivity(), mAccount.getOnlineInfo(), mProtocolResources, lf);					
				} else if (feature instanceof InputFormFeature) {
					InputFormFeature iff = (InputFormFeature) feature;
					Page.getInputFormPage(getMainActivity(), iff, mAccount.getOnlineInfo(), mProtocolResources);
				} else if (feature instanceof ActionFeature) {
					activity.getCoreService().setFeature(feature.getFeatureId(), mAccount.getOnlineInfo());
				}
				break;
			}
		} catch (RemoteException e) {
			activity.onRemoteException(e);
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int i, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			//Here we need to minimize activity, so override default logic of choosing 1st page.
			return false;
		}
		
		return super.onKeyDown(i, event);
	}

	/* (non-Javadoc)
	 * @see aceim.app.dataentity.listeners.IHasMessages#messageReceived(aceim.app.api.dataentity.Message)
	 */
	@Override
	public void onMessageReceived(Message message) {
		if (message instanceof FileMessage || !message.isIncoming()) {
			return;
		}
		
		Buddy b = mAccount.getBuddyByProtocolUid(message.getContactUid());
		
		if (b == null) {
			Logger.log("No buddy found for " + message.getContactUid(), LoggerLevel.WARNING);
			return;
		}
		
		Page selectedPage = getMainActivity().getScreen().getSelectedPage();
		if (selectedPage.getPageId().equals(Chat.class.getSimpleName() + " " + b.getFilename())) {
			return;
		}
		
		b.incrementUnread();
		onBuddyStateChanged(b);
		try {
			getMainActivity().getCoreService().notifyUnread(message, b);
		} catch (RemoteException e) {
			getMainActivity().onRemoteException(e);
		}
	}

	/* (non-Javadoc)
	 * @see aceim.app.dataentity.listeners.IHasMessages#messageAckReceived(long, aceim.app.api.dataentity.MessageAckState)
	 */
	@Override
	public void onMessageAckReceived(long messageId, MessageAckState ack) {}

	/* (non-Javadoc)
	 * @see aceim.app.dataentity.listeners.IHasMessages#hasMessagesOfBuddy(byte, java.lang.String)
	 */
	@Override
	public boolean hasMessagesOfBuddy(byte serviceId, String buddyProtocolUid) {
		return mAccount.getServiceId() == serviceId;
	}

	/* (non-Javadoc)
	 * @see aceim.app.dataentity.listeners.IHasBuddy#onBuddyIcon(byte, java.lang.String)
	 */
	/*@Override
	public void onBuddyIcon(byte serviceId, String protocolUid) {
		mUpdater.onBuddyIcon(serviceId, protocolUid);
	}*/

	/* (non-Javadoc)
	 * @see aceim.app.dataentity.listeners.IHasAccount#onAccountIcon(byte)
	 */
	@Override
	public void onAccountIcon(byte serviceId) {}
	
	@Override
	public boolean hasThisBuddy(byte serviceId, String protocolUid) {
		return mAccount.getServiceId() == serviceId && mAccount.getBuddyByProtocolUid(protocolUid) != null;
	}	

	@Override
	public void onFilePicked(ActivityResult result, MainActivity activity) {
		if (result.getRequestCode() != ServiceUtils.getRequestCodeForActivity(mAccount.getEntityId().hashCode()) || result.getResultCode() != Activity.RESULT_OK) {
			return;
		}
		
		String filePath;
		if (mAwaitingUri != null) {			
			filePath = mAwaitingUri.getPath();
			mAwaitingUri = null;
		} else {
			filePath = result.getData().getData().getPath();
		}
		
		File file = new File(filePath);
		
		if (!file.exists()) {
			Uri selectedImage = result.getData().getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = activity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            file = new File(filePath);
            cursor.close();
		}
		
		if (!file.exists()) {
			Logger.log("No file found under path " + filePath, LoggerLevel.INFO);
			return;
		}
		
		Logger.log("File picked " + filePath, LoggerLevel.VERBOSE);
		
		try {
			activity.getCoreService().uploadAccountPhoto(mAccount.getServiceId(), filePath);
		} catch (RemoteException e) {
			activity.onRemoteException(e);
		}
	}
	
	protected ContactListAdapter getAdapter() {
		return mUpdater.getAdapter();
	}

	protected abstract View onCreateContactListView(LayoutInflater inflater, ViewGroup group, Bundle saved);
	
	protected abstract Class<? extends ContactListAdapter> getContactListAdapterClassName();	
}
