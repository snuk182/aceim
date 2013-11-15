package aceim.app.view.page.personalinfo;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ToggleFeature;
import aceim.api.service.ApiConstants;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.DialogUtils;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import aceim.app.widgets.bottombar.BottomBarButton;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.BitmapAjaxCallback;

public class PersonalInfoPage extends Page {
	
	private final PersonalInfo mInfo;
	
	private BottomBarButton mAddBtn;
	private BottomBarButton mMoveBtn;
	private BottomBarButton mRenameBtn;
	private BottomBarButton mDeleteBtn;
	private BottomBarButton mJoinBtn;
	private BottomBarButton mLeaveBtn;
	private BottomBarButton mCopyAllBtn;

	public PersonalInfoPage(PersonalInfo info) {
		this.mInfo = info;
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_info_details);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.personal_info_X, mInfo.getProtocolUid());
	}
	
	@Override
	protected View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.personal_info, null);
		
		mCopyAllBtn = (BottomBarButton) view.findViewById(R.id.copy);
		mAddBtn = (BottomBarButton) view.findViewById(R.id.add);
		mMoveBtn = (BottomBarButton) view.findViewById(R.id.move);
		mRenameBtn = (BottomBarButton) view.findViewById(R.id.rename);
		mDeleteBtn = (BottomBarButton) view.findViewById(R.id.remove);
		mJoinBtn = (BottomBarButton) view.findViewById(R.id.join);
		mLeaveBtn = (BottomBarButton) view.findViewById(R.id.leave);
		
		AQuery aq = new AQuery(view);
		
		Buddy buddy;
		try {
			buddy = getMainActivity().getCoreService().getBuddy(mInfo.getServiceId(), mInfo.getProtocolUid());
		} catch (RemoteException e) {
			getMainActivity().onRemoteException(e);
			return view;
		}
		
		final String name = mInfo.getProperties().containsKey(PersonalInfo.INFO_NICK) ? mInfo.getProperties().getString(PersonalInfo.INFO_NICK) : mInfo.getProtocolUid();
		
		BitmapAjaxCallback callback = new BitmapAjaxCallback();
		callback.fallback(R.drawable.dummy_icon).animation(android.R.anim.slide_in_left);
		
		aq.id(R.id.image_icon).image(ViewUtils.getBitmapFile(getMainActivity(), buddy.getFilename()), true, 0, callback);			
		aq.id(R.id.name).text(name);
		aq.id(R.id.protocolUid).text(mInfo.getProtocolUid());
		
		mAddBtn.setVisibility(mInfo.isMultichat() || buddy != null ? View.GONE : View.VISIBLE);
		mDeleteBtn.setVisibility(buddy == null ? View.GONE : View.VISIBLE);
		mRenameBtn.setVisibility(buddy == null ? View.GONE : View.VISIBLE);
		mMoveBtn.setVisibility(mInfo.isMultichat() || buddy == null ? View.GONE : View.VISIBLE);
		
		mJoinBtn.setVisibility(!mInfo.isMultichat() || (buddy != null && buddy.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1) > -1) ? View.GONE : View.VISIBLE);
		mLeaveBtn.setVisibility(!mInfo.isMultichat() || buddy == null || (buddy.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1) < 0) ? View.GONE : View.VISIBLE);
		
		final Account a;
		try {
			a = getMainActivity().getCoreService().getAccount(mInfo.getServiceId());
		} catch (RemoteException e1) {
			getMainActivity().onRemoteException(e1);
			return view;
		}
		
		mAddBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mInfo.isMultichat()) {
					Buddy buddy = new Buddy(mInfo.getProtocolUid(), a.getProtocolUid(), a.getProtocolName(), mInfo.getServiceId());
					buddy.setGroupId(ApiConstants.NOT_IN_LIST_GROUP_ID);
					buddy.setName(name.toString());
					
					DialogUtils.showAddBuddyDialog(buddy, a, getMainActivity());
				}
			}
		});
		
		mDeleteBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogUtils.showConfirmRemoveBuddyDialog(a.getBuddyByProtocolUid(mInfo.getProtocolUid()), getMainActivity());
			}
		});
		
		mRenameBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogUtils.showBuddyRenameDialog(a.getBuddyByProtocolUid(mInfo.getProtocolUid()), getMainActivity());
			}
		});
		
		mMoveBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogUtils.showBuddyMoveDialog(a.getBuddyByProtocolUid(mInfo.getProtocolUid()), a, getMainActivity());
			}
		});
		
		mJoinBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					getMainActivity().getCoreService().joinChat(mInfo.getServiceId(), mInfo.getProtocolUid());
				} catch (RemoteException e) {
					getMainActivity().onRemoteException(e);
				}
			}
		});
		
		mLeaveBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					getMainActivity().getCoreService().leaveChat(mInfo.getServiceId(), mInfo.getProtocolUid());
				} catch (RemoteException e) {
					getMainActivity().onRemoteException(e);
				}
			}
		});
		
		/*MultiChatRoom chat = new MultiChatRoom(mInfo.getProtocolUid(), a.getProtocolUid(), a.getProtocolName(), mInfo.getServiceId());
		chat.setName(mInfo.getProperties().getString(PersonalInfo.INFO_NICK));*/		
		
		LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
		
		for (String key : mInfo.getProperties().keySet()) {
			if (key.equals(PersonalInfo.INFO_NICK)) continue; 
			
			View item = inflater.inflate(R.layout.personal_info_item, null);
			
			ImageView iicon = (ImageView) item.findViewById(R.id.icon);
			//TODO fix
			iicon.getLayoutParams().width = 0;
			
			TextView keyView = (TextView) item.findViewById(R.id.key);
			TextView valueView = (TextView) item.findViewById(R.id.value);
			
			keyView.setText(key);
			valueView.setText(mInfo.getProperties().getString(key));
			
			container.addView(item);
		}
		
		if (buddy != null) {
			ProtocolResources res = getMainActivity().getProtocolResourcesForAccount(a);
			for (String key : buddy.getOnlineInfo().getFeatures().keySet()) {
				ProtocolServiceFeature f = res.getFeature(key);
				if (f == null) {
					continue;
				}
				
				View item = inflater.inflate(R.layout.personal_info_item, null);
				TextView keyView = (TextView) item.findViewById(R.id.key);
				TextView valueView = (TextView) item.findViewById(R.id.value);
				
				keyView.setText(f.getFeatureName());
				
				if (f instanceof ListFeature) {
					ListFeature lf = (ListFeature) f;
					byte v = buddy.getOnlineInfo().getFeatures().getByte(key, (byte) -1);
					if (v > -1) {
						valueView.setText(lf.getNames()[v]);
					}
				} else if (f instanceof ToggleFeature) {
					valueView.setText(Boolean.toString(((ToggleFeature)f).getValue()));
				}
			}
		}
		
		return view;
	}
	
	/**
	 * @return the mInfo
	 */
	public PersonalInfo getInfo() {
		return mInfo;
	}
}
