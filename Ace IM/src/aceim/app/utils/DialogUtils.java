package aceim.app.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.ActionFeature;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ProtocolServiceFeatureTarget;
import aceim.api.dataentity.ToggleFeature;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.service.ServiceUtils;
import aceim.app.view.page.Page;
import aceim.app.widgets.adapters.FileTransferRequestAdapter;
import aceim.app.widgets.adapters.IconTitleAdapter;
import aceim.app.widgets.adapters.IconTitleAdapter.IconTitleItem;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;

import com.androidquery.AQuery;

public final class DialogUtils {

	private static final Map<Long, Dialog> sFileTransferDialogs = new HashMap<Long, Dialog>();

	private DialogUtils() {}

	public static void showBrandedDialog(Dialog dialog) {
		dialog.show();
	
		// This seems to be the only way to set custom background to a dialog
		// content. I'm really sorry :(
		View message = dialog.getWindow().findViewById(android.R.id.message);
		if (message != null) {
			message.setBackgroundResource(R.drawable.cornered_background_small);
		}
	}

	public static void showFileMessageDialog(final FileMessage message, Buddy buddy, final MainActivity activity) {
		showAcceptDeclineDialog(message, buddy, activity);
	}

	public static void showEditListFeatureDialog(final MainActivity activity, final OnlineInfo info, ProtocolResources protocolResources, final ListFeature feature) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(feature.getFeatureName());
		
		final OnlineInfo tmp = new OnlineInfo(info.getServiceId(), info.getProtocolUid());
		
		View view = LayoutInflater.from(activity).inflate(R.layout.list_feature_dialog, null);
		final GridView grid = (GridView) view.findViewById(R.id.grid);
	
		final EditText titleEditor = (EditText) view.findViewById(android.R.id.text1);
		final EditText textEditor = (EditText) view.findViewById(android.R.id.text2);
	
		if (feature.getFeatureId().equals(ApiConstants.FEATURE_XSTATUS)) {
			titleEditor.setVisibility(View.VISIBLE);
			textEditor.setVisibility(View.VISIBLE);
	
			titleEditor.setText(info.getXstatusName());
			textEditor.setText(info.getXstatusDescription());
		}
		
		if (feature.getDrawables() != null && feature.getNames() != null) {
			//grid.setColumnWidth(activity.getResources().getDimensionPixelSize(R.dimen.smiley_column_width));
			
			final IconTitleAdapter adapter;
			try {
				adapter = IconTitleAdapter.fromListFeature(activity, R.layout.status_item, protocolResources, feature);
				grid.setAdapter(adapter);
			} catch (AceImException e) {
				Logger.log(e);
				return;
			}
		
			grid.setOnItemClickListener(new OnItemClickListener() {
				
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (position < 0 || position >= adapter.getCount()) {
						return;
					}
		
					IconTitleItem item = adapter.getItem(position);
		
					if (feature.getFeatureId().equals(ApiConstants.FEATURE_XSTATUS)) {
						String currentTitle = titleEditor.getText().toString().trim();
		
						if (TextUtils.isEmpty(currentTitle) || currentTitle.equals(adapter.getItem(tmp.getFeatures().getByte(feature.getFeatureId()) + 1).getTitle())) {
							titleEditor.setText(item.getTitle());
						}
					}
		
					adapter.setSelectedItem(position);
					adapter.notifyDataSetChanged();
					tmp.getFeatures().putByte(feature.getFeatureId(), (byte) (feature.isNullable() ? position - 1 : position));
				}
			});
			
			grid.setAdapter(adapter);
			tmp.getFeatures().putByte(feature.getFeatureId(), info.getFeatures().getByte(feature.getFeatureId(), (byte) (feature.isNullable() ? -1 : 0)));
		} else {
			grid.setVisibility(View.GONE);
		}
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					byte value = tmp.getFeatures().getByte(feature.getFeatureId());
					info.getFeatures().putByte(feature.getFeatureId(), value);
	
					if (feature.getFeatureId().equals(ApiConstants.FEATURE_XSTATUS)) {
						if (value > -1) {
							info.setXstatusName(titleEditor.getText().toString().trim());
							info.setXstatusDescription(textEditor.getText().toString().trim());
						} else {
							info.setXstatusName(null);
							info.setXstatusDescription(null);
						}
					}
	
					activity.getCoreService().setFeature(feature.getFeatureId(), info);
				} catch (RemoteException e) {
					activity.onRemoteException(e);
				}
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
	
		builder.setView(view);
		
		showBrandedDialog(builder.create());
	
		if (feature.getDrawables() != null && feature.getNames() != null) {
			byte currentValue = info.getFeatures().getByte(feature.getFeatureId());
			
			grid.performItemClick(null, feature.isNullable() ? currentValue + 1 : currentValue, 0);
		}
	}

	public static void buddyGroupContextMenu(final MainActivity mainActivity, final Account account, final BuddyGroup group, final ProtocolResources protocolResources) {
		boolean groupManagementAllowed = account.getOnlineInfo().getFeatures().getBoolean(ApiConstants.FEATURE_GROUP_MANAGEMENT, false);
		
		if (!groupManagementAllowed) {
			//TODO fix for group-specific features (later, when available)
			return;
		}
		
		final Dialog dialog = new Dialog(mainActivity);
		dialog.setTitle(mainActivity.getString(R.string.contact_menu, group.getName()));
	
		ListView list = new ListView(mainActivity);
		int pad = mainActivity.getResources().getDimensionPixelSize(R.dimen.default_padding);
		list.setPadding(pad, 0, pad, 0);
		
		TypedArray items = mainActivity.getResources().obtainTypedArray(R.array.group_menu_names);
	
		final List<IconTitleItem> itemList = new ArrayList<IconTitleItem>();
		for (int i = 0; i < items.length(); i++){
			String item = items.getString(i);
			
			IconTitleItem t = new IconTitleItem();
			t.setTitle(item);
			
			itemList.add(t);
		}
		
		items.recycle();
		
		/*Resources r;
		try {
			r = protocolResources.getNativeResourcesForProtocol(null);
		} catch (AceImException e) {
			Logger.log(e);
			return;
		}*/
		
		for (ProtocolServiceFeature feature : protocolResources.getFeatures()) {
			if (!feature.isEditable() || !feature.isAppliedToTarget(ProtocolServiceFeatureTarget.GROUP)) {
				continue;
			}
			
			String item;
			
			item = feature.getFeatureName();
			
			IconTitleItem t = new IconTitleItem();
			t.setTitle(item);
			t.setId(feature.getFeatureId());
			
			itemList.add(t);
		}
	
		final IconTitleAdapter adapter = new IconTitleAdapter(mainActivity, android.R.layout.simple_list_item_1, android.R.id.text1, itemList, new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		list.setAdapter(adapter);
		
		list.setOnItemClickListener(new OnItemClickListener() {
	
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				IconTitleItem item = adapter.getItem(position);
				String name = item.getTitle();
				
				if (name.equals(mainActivity.getString(R.string.menu_value_add_group))) {
					showAddOrRenameGroupDialog(null, account, mainActivity);
				} else if (name.equals(mainActivity.getString(R.string.menu_value_rename))) {
					showAddOrRenameGroupDialog(group, account, mainActivity);
				} else if (name.equals(mainActivity.getString(R.string.menu_value_delete_group))) {
					showConfirmRemoveGroupDialog(group, mainActivity);
				}
				
				dialog.dismiss();
			}
		});
		
		dialog.addContentView(list, new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		dialog.setCancelable(true);
		DialogUtils.showBrandedDialog(dialog);
	}

	public static void buddyContextMenu(final MainActivity mainActivity, final Account account, final Buddy buddy, final ProtocolResources protocolResources) {
		final Dialog dialog = new Dialog(mainActivity);
		dialog.setTitle(mainActivity.getString(R.string.contact_menu, buddy.getSafeName()));
	
		ListView list = new ListView(mainActivity);
		int pad = mainActivity.getResources().getDimensionPixelSize(R.dimen.default_padding);
		list.setPadding(pad, 0, pad, 0);
		
		TypedArray items = mainActivity.getResources().obtainTypedArray(R.array.contact_menu_names);
		
		boolean buddyManagementAllowed = account.getOnlineInfo().getFeatures().getBoolean(ApiConstants.FEATURE_BUDDY_MANAGEMENT, false);
	
		final List<IconTitleItem> itemList = new ArrayList<IconTitleItem>();
		for (int i = 0; i < items.length(); i++) {
			String item = items.getString(i);
	
			if (item.equals(mainActivity.getString(R.string.menu_value_add_to_list))) {
				if ((buddy instanceof MultiChatRoom) ||buddy.getGroupId() != ApiConstants.NOT_IN_LIST_GROUP_ID) {
					continue;
				}
			}
			if (item.equals(mainActivity.getString(R.string.menu_value_move))) {
				if ((buddy instanceof MultiChatRoom) ||!buddyManagementAllowed || buddy.getGroupId() == ApiConstants.NOT_IN_LIST_GROUP_ID || !hasGroupsToMove(buddy, account)) {
					continue;
				}
			}
			if (item.equals(mainActivity.getString(R.string.menu_value_rename))) {
				if ((buddy instanceof MultiChatRoom) ||!buddyManagementAllowed || buddy.getGroupId() == ApiConstants.NOT_IN_LIST_GROUP_ID) {
					continue;
				}
			}
			if (item.equals(mainActivity.getString(R.string.menu_value_add_to_list))) {
				if ((buddy instanceof MultiChatRoom) ||!buddyManagementAllowed || buddy.getGroupId() != ApiConstants.NOT_IN_LIST_GROUP_ID) {
					continue;
				}
			}			
			if (item.equals(mainActivity.getString(R.string.menu_value_join_chat))) {
				if (!(buddy instanceof MultiChatRoom) || buddy.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1) > -1) {
					continue;
				}
			}
			if (item.equals(mainActivity.getString(R.string.menu_value_leave_chat))) {
				if (!(buddy instanceof MultiChatRoom) || buddy.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1) < 0) {
					continue;
				}
			}
			
			if (item.equals(mainActivity.getString(R.string.menu_value_delete_contact))) {
				if (!(buddy instanceof MultiChatRoom) && !buddyManagementAllowed) {
					continue;
				}
			}
	
			IconTitleItem t = new IconTitleItem();
			t.setTitle(item);
			
			itemList.add(t);
		}
	
		items.recycle();
		
		Resources r;
		try {
			r = protocolResources.getNativeResourcesForProtocol(null);
		} catch (AceImException e) {
			Logger.log(e);
			return;
		}
		
		Bundle buddyFeatures = buddy.getOnlineInfo().getFeatures();
		
		for (String featureId : buddy.getOnlineInfo().getFeatures().keySet()) {
			ProtocolServiceFeature feature = protocolResources.getFeature(featureId);
			
			if (feature == null || !feature.isEditable() || !feature.isAppliedToTarget(ProtocolServiceFeatureTarget.BUDDY)) {
				continue;
			}
			
			String item;
			
			if (feature instanceof ListFeature){
				ListFeature lf = (ListFeature) feature;
				
				byte value = buddyFeatures.getByte(feature.getFeatureId(), (byte) -1);
				
				if (value > -1) {
					item = r.getString(lf.getNames()[value]);
				} else {
					item = feature.getFeatureName();
				}
			} else {
				item = feature.getFeatureName();
			}
			
			IconTitleItem t = new IconTitleItem();
			t.setTitle(item);
			t.setId(feature.getFeatureId());
			
			itemList.add(t);
		}
		
		final IconTitleAdapter adapter = new IconTitleAdapter(mainActivity, android.R.layout.simple_list_item_1, android.R.id.text1, itemList, new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {
	
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				IconTitleItem item = adapter.getItem(position);
				
				try {
					if (item.getId() != null) {
						ProtocolServiceFeature f = protocolResources.getFeature(item.getId());
						
						if (f instanceof ListFeature) {
							showEditListFeatureDialog(mainActivity, buddy.getOnlineInfo(), protocolResources, (ListFeature)f);
						} else if (f instanceof InputFormFeature) {
							Page.getInputFormPage(mainActivity, (InputFormFeature) f, buddy.getOnlineInfo(), protocolResources);
						} else if (f instanceof ToggleFeature) {
							OnlineInfo info = buddy.getOnlineInfo();
							info.getFeatures().putBoolean(f.getFeatureId(), !info.getFeatures().getBoolean(f.getFeatureId(), ((ToggleFeature) f).getValue()));
							mainActivity.getCoreService().setFeature(f.getFeatureId(), info);
						} else if (f instanceof ActionFeature) {
							mainActivity.getCoreService().setFeature(f.getFeatureId(), buddy.getOnlineInfo());
						} else {
							ViewUtils.showAlertToast(mainActivity, android.R.drawable.ic_menu_info_details, R.string.unknown_command, item.getId());
						}
						
					} else {
						String name = item.getTitle();
						if (name.equals(mainActivity.getString(R.string.menu_value_view_info))) {
							mainActivity.getCoreService().requestBuddyFullInfo(buddy.getServiceId(), buddy.getProtocolUid());
						} else if (name.equals(mainActivity.getString(R.string.menu_value_add_to_list))) {
							showAddBuddyDialog(buddy, account, mainActivity);
						} else if (name.equals(mainActivity.getString(R.string.menu_value_rename))) {
							showBuddyRenameDialog(buddy, mainActivity);
						} else if (name.equals(mainActivity.getString(R.string.menu_value_delete_contact))) {
							showConfirmRemoveBuddyDialog(buddy, mainActivity);
						} else if (name.equals(mainActivity.getString(R.string.menu_value_move))) {
							showBuddyMoveDialog(buddy, account, mainActivity);
						} else if (name.equals(mainActivity.getString(R.string.menu_value_join_chat))) {
							mainActivity.getCoreService().joinChat(buddy.getServiceId(), buddy.getProtocolUid());
						} else if (name.equals(mainActivity.getString(R.string.menu_value_leave_chat))) {
							mainActivity.getCoreService().leaveChat(buddy.getServiceId(), buddy.getProtocolUid());
						} else {
							ViewUtils.showAlertToast(mainActivity, android.R.drawable.ic_menu_info_details, R.string.unknown_command, name);
						}
					}
				} catch (RemoteException e) {
					mainActivity.onRemoteException(e);
				}
				
				dialog.dismiss();
			}
		});
	
		dialog.addContentView(list, new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		dialog.setCancelable(true);
		DialogUtils.showBrandedDialog(dialog);
	}

	public static final void showBuddyRenameDialog(final Buddy buddy, final MainActivity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final EditText editor = new EditText(activity);
		editor.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		builder.setTitle(activity.getString(R.string.rename_X, buddy.getSafeName()));
		builder.setView(editor);
		
		editor.setText(buddy.getName());
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = editor.getText().toString().trim();
				if (TextUtils.isEmpty(text)) {
					//showAlertToast(activity, android.R.drawable.ic_dialog_alert, R.string., params);
				} else {
					if (!text.equals(buddy.getName())) {
						try {
							Buddy clone = ServiceUtils.cloneBuddy(buddy);
							clone.setName(text);
							activity.getCoreService().renameBuddy(clone);
						} catch (RemoteException e) {
							activity.onRemoteException(e);
						}
					}
				}
				
				dialog.dismiss();
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		showBrandedDialog(builder.create());
	}

	public static void showConfirmRemoveBuddyDialog(final Buddy buddy, final MainActivity mainActivity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(R.string.confirm_delete);
		builder.setMessage(mainActivity.getString(R.string.are_you_sure_you_want_to_remove, buddy.getSafeName()));
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					mainActivity.getCoreService().removeBuddy(buddy);
				} catch (RemoteException e) {
					mainActivity.onRemoteException(e);
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		showBrandedDialog(builder.create());
	}

	public static void showBuddyMoveDialog(final Buddy buddy, Account account, final MainActivity mainActivity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(mainActivity.getString(R.string.move_X, buddy.getSafeName()));
	
		final Spinner spinner = new Spinner(mainActivity);
		ArrayAdapter<BuddyGroup> adapter = new ArrayAdapter<BuddyGroup>(mainActivity, android.R.layout.simple_spinner_item, android.R.id.text1, account.getBuddyGroupList());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		builder.setView(spinner);
		
		for (int i=0; i<adapter.getCount(); i++) {
			if (buddy.getGroupId().equals(adapter.getItem(i).getId())) {
				spinner.setSelection(i);
				break;
			}
		}		
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Buddy clone = ServiceUtils.cloneBuddy(buddy);
				clone.setGroupId(((BuddyGroup)spinner.getSelectedItem()).getId());
				try {
					mainActivity.getCoreService().moveBuddy(clone);
				} catch (RemoteException e) {
					mainActivity.onRemoteException(e);
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		showBrandedDialog(builder.create());
	}

	public static void showAddBuddyDialog(final Buddy buddy, Account account, final MainActivity mainActivity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(mainActivity.getString(R.string.add_X, buddy.getSafeName()));
	
		final Spinner spinner = new Spinner(mainActivity);
		ArrayAdapter<BuddyGroup> adapter = new ArrayAdapter<BuddyGroup>(mainActivity, android.R.layout.simple_spinner_item, android.R.id.text1, account.getBuddyGroupList());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		builder.setView(spinner);
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Buddy clone = ServiceUtils.cloneBuddy(buddy);
				clone.setGroupId(((BuddyGroup)spinner.getSelectedItem()).getId());
				try {
					mainActivity.getCoreService().addBuddy(clone);
				} catch (RemoteException e) {
					mainActivity.onRemoteException(e);
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		showBrandedDialog(builder.create());
	}

	public static void showConfirmRemoveGroupDialog(final BuddyGroup group, final MainActivity mainActivity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(R.string.confirm_delete);
		builder.setMessage(mainActivity.getString(R.string.are_you_sure_you_want_to_remove, group.getName()));
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					mainActivity.getCoreService().removeGroup(group);;
				} catch (RemoteException e) {
					mainActivity.onRemoteException(e);
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		showBrandedDialog(builder.create());
	}

	public static void showAddOrRenameGroupDialog(final BuddyGroup group, final Account account, final MainActivity mainActivity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(mainActivity.getString(R.string.add_group));
	
		final EditText editor = new EditText(mainActivity);
		editor.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		if (group != null) {
			editor.setText(group.getName());
		}
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = editor.getText().toString().trim();
				
				if (TextUtils.isEmpty(text)) {
					ViewUtils.showAlertToast(mainActivity, android.R.drawable.ic_dialog_alert, R.string.X_cannot_be_empty, mainActivity.getString(R.string.name));
					return;
				}
				
				if (group != null) {
					BuddyGroup clone = ServiceUtils.cloneBuddyGroup(group);
					clone.setName(text);
					try {
						mainActivity.getCoreService().renameGroup(clone);
					} catch (RemoteException e) {
						mainActivity.onRemoteException(e);
					}
				} else {
					BuddyGroup group = new BuddyGroup(null, account.getProtocolUid(), account.getServiceId());
					group.setName(text);
					
					try {
						mainActivity.getCoreService().addGroup(group);
					} catch (RemoteException e) {
						mainActivity.onRemoteException(e);
					}
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setView(editor);
		
		showBrandedDialog(builder.create());
	}

	private static boolean hasGroupsToMove(Buddy buddy, Account account) {
		for (BuddyGroup g : account.getBuddyGroupList()) {
			if (!g.getId().equals(ApiConstants.NO_GROUP_ID) && !g.getId().equals(ApiConstants.NOT_IN_LIST_GROUP_ID) && !g.getId().equals(buddy.getGroupId())) {
				return true;
			}
		}
		
		return false;
	}

	public static void showAcceptDeclineDialog(final Message message, Buddy buddy, final MainActivity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setCancelable(true);
		
		if (message instanceof FileMessage) {
			View view = LayoutInflater.from(activity).inflate(R.layout.file_transfer_accept_dialog, null);
			
			AQuery aq = new AQuery(view);
			
			ViewUtils.fillIcon(R.id.icon, aq, buddy.getFilename(), activity);	
			aq.id(R.id.list).adapter(new FileTransferRequestAdapter(activity, ((FileMessage)message).getFiles()));
			aq.id(R.id.title).text(activity.getString(R.string.buddy_sends_files, buddy.getSafeName()));
			
			builder.setView(view);
			builder.setOnCancelListener(new OnCancelListener() {
		
				@Override
				public void onCancel(DialogInterface dialog) {
					sFileTransferDialogs.remove(message.getMessageId());
				}
			});
		} else {
			builder.setTitle(activity.getString(R.string.accept_dialog_header_format, buddy.getSafeName(), message.getContactDetail()));
			builder.setMessage(message.getText());
		}
		
		builder.setPositiveButton(R.string.accept, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					activity.getCoreService().respondMessage(message, true);
				} catch (RemoteException e) {
					activity.onRemoteException(e);
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.decline, new OnClickListener() {
	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					activity.getCoreService().respondMessage(message, false);
				} catch (RemoteException e) {
					activity.onRemoteException(e);
				}
				dialog.dismiss();
			}
		});
		
	
		Dialog d = builder.create();
		sFileTransferDialogs.put(message.getMessageId(), d);
		showBrandedDialog(d);
	}

}
