package aceim.app.view.page.contactlist;

import ua.snuk182.expandablegrid.ExpandableGridView;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.OnlineInfo;
import aceim.app.AceIMActivity;
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

public final class GridContactList extends ContactList {
	
	private ExpandableGridView mGridView;
	private ContactListBottomBar mBottomBar;
	
	private int mGridItemSize = 0;
	
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
		if (mGridItemSize < 1) {
			initVariables((AceIMActivity)inflater.getContext());
		}
		
		View view = inflater.inflate(R.layout.grid_contact_list, group, false);
		mGridView = (ExpandableGridView) view.findViewById(R.id.grid);
		mGridView.setAdapter(getAdapter());
		mGridView.setColumnWidth(mGridItemSize);
		
		mBottomBar = (ContactListBottomBar) view.findViewById(R.id.bottom_bar);
		
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
		return GridContactListAdapter.class;
	}
	
	private void initVariables(AceIMActivity activity) {
		if (mGridItemSize < 1) {
			Resources themeResources = getMainActivity().getThemesManager().getCurrentThemeContext().getResources();
			mGridItemSize = themeResources.getDimensionPixelSize(getMainActivity().getThemesManager().getViewResources().getGridItemSizeId());
		}		
	}
}
