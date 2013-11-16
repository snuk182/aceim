package aceim.app.view.page.personalinfo;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ToggleFeature;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.DialogUtils;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import aceim.app.widgets.bottombar.BottomBarButton;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.androidquery.AQuery;

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
		
		ViewUtils.fillIcon(R.id.icon, aq, buddy.getFilename(), getMainActivity());
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
			
			AQuery aqi = new AQuery(item);
			
			ImageView iicon = (ImageView) item.findViewById(R.id.icon);
			//TODO fix
			iicon.getLayoutParams().width = 0;
			
			aqi.id(R.id.key).text(key);
			aqi.id(R.id.value).text(mInfo.getProperties().getString(key));
			
			container.addView(item);
		}
		
		if (buddy != null) {
			ProtocolResources res = getMainActivity().getProtocolResourcesForAccount(a);
			try {
				Resources protocolResources = res.getNativeResourcesForProtocol(getMainActivity().getPackageManager());
				
				for (String key : buddy.getOnlineInfo().getFeatures().keySet()) {
					ProtocolServiceFeature f = res.getFeature(key);
					if (f == null) {
						continue;
					}
					
					View item = inflater.inflate(R.layout.personal_info_item, null);
					
					AQuery aqi = new AQuery(item);
					
					aqi.id(R.id.key).text(f.getFeatureName());
					
					if (f instanceof ListFeature) {
						ListFeature lf = (ListFeature) f;
						byte v = buddy.getOnlineInfo().getFeatures().getByte(key, (byte) -1);
						if (v > -1) {
							aqi.id(R.id.value).text(protocolResources.getString(lf.getNames()[v]));							
						}
					} else if (f instanceof ToggleFeature) {
						aqi.id(R.id.value).text(Boolean.toString(((ToggleFeature)f).getValue()));
					}
				}
			} catch (AceImException e) {
				Logger.log(e);
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
