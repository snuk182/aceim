package aceim.app.view.page.contactlist;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.OnlineInfo;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.ViewUtils;
import aceim.app.widgets.bottombar.ContactListBottomBar;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.androidquery.AQuery;
import com.androidquery.callback.BitmapAjaxCallback;

public final class GridContactList extends ContactList {
	
	private ExpandableListView mGridView;
	private ContactListBottomBar mBottomBar;
	
	public GridContactList(Account account, ProtocolResources resources, Resources applicationResources) {
		super(account, resources, applicationResources);
	}

	@Override
	public void onConnectionStateChanged(ConnectionState connState, int extraParameter) {
		super.onConnectionStateChanged(connState, extraParameter);
		if (mBottomBar != null) {
			mBottomBar.onConnectionStateChanged(connState, extraParameter);	
		}	
	}

	@Override
	protected View onCreateContactListView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.contact_list, group, false);
		mGridView = (ExpandableListView) view.findViewById(R.id.grid);
		mGridView.setAdapter(getAdapter());
		mBottomBar = (ContactListBottomBar) view.findViewById(R.id.bottom_bar);
		
		onAccountIcon(mAccount.getServiceId());
		
		return view;
	}
	
	@Override
	public void onAccountIcon(byte serviceId) {
		ViewUtils.fillAccountPlaceholder(getMainActivity(), mAccount, mBottomBar, mProtocolResources);
	}
	
	@Override
	public void onBuddyIcon(byte serviceId, String protocolUid){
		if (mGridView == null) {
			return;
		}
		
		Buddy b = mAccount.getBuddyByProtocolUid(protocolUid);
		
		if (b == null){
			return;
		}
		
		View v = mGridView.findViewWithTag(b);
		
		if (v != null) {
			ViewUtils.fillIcon(R.id.image_icon, v, b.getFilename(), getMainActivity());	
		}
	}
	
	@Override
	public void onOnlineInfoChanged(OnlineInfo info) {
		super.onOnlineInfoChanged(info);
		
		ViewUtils.fillAccountPlaceholder(getMainActivity(), mAccount, mBottomBar, mProtocolResources);
	}

	@Override
	protected Class<? extends ContactListAdapter> getContactListAdapterClassName() {
		return ExpandableGridAdapter.class;
	}
}
