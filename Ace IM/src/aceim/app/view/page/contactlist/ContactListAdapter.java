package aceim.app.view.page.contactlist;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.view.page.contactlist.ContactListUpdater.ContactListModelGroup;
import android.widget.BaseExpandableListAdapter;

public abstract class ContactListAdapter extends BaseExpandableListAdapter {

	protected final List<ContactListModelGroup> mGroupList;
	protected final ProtocolResources mResources;

	public ContactListAdapter(List<ContactListModelGroup> groups, ProtocolResources resources) {
		this.mGroupList = groups;
		this.mResources = resources;
	}

	@Override
	public ContactListModelGroup getGroup(int groupPosition) {
		return mGroupList.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mGroupList.size();
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return mGroupList.get(groupPosition).getBuddyList().size();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	@Override
	public Buddy getChild(int groupPosition, int childPosition) {
		return mGroupList.get(groupPosition).getBuddyList().get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return getChild(groupPosition, childPosition).getId();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}
}
