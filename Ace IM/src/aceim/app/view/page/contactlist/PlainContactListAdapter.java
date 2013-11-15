package aceim.app.view.page.contactlist;

import java.util.List;

import aceim.api.dataentity.Buddy;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.contactlist.ContactListUpdater.ContactListModelGroup;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class PlainContactListAdapter extends ContactListAdapter {

	public PlainContactListAdapter(List<ContactListModelGroup> groups, ProtocolResources resources) {
		super(groups, resources);
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, final ViewGroup parent) {
		final ContactListModelGroup g = getGroup(groupPosition);

		View item;
		if (convertView == null) {
			Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(context);
			item = (View) inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
		} else {
			item = convertView;
		}

		TextView text = (TextView) item.findViewById(android.R.id.text1);
		text.setText(g.toString());

		if (g.isCollapsed()) {
			((ExpandableListView) parent).collapseGroup(groupPosition);
		} else {
			((ExpandableListView) parent).expandGroup(groupPosition);
		}

		item.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				g.setCollapsed(!g.isCollapsed());
				if (g.isCollapsed()) {
					((ExpandableListView) parent).collapseGroup(groupPosition);
				} else {
					((ExpandableListView) parent).expandGroup(groupPosition);
				}
			}
		});
		
		if (!g.isPredefined()) {
			item.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					((MainActivity)parent.getContext()).onBuddyGroupContextMenuRequest(g, mResources);
					return true;
				}
			});
		}

		return item;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, final ViewGroup parent) {
		Buddy buddy = getChild(groupPosition, childPosition);
		View view;
		
		if (convertView == null) {
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list_plain_item, null);
		} else {
			view = convertView;
		}
		
		if (view.getTag() == null || view.getTag() != buddy) {
			view.setTag(buddy);
			view.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					MainActivity activity = (MainActivity) parent.getContext();
					Buddy buddy = (Buddy) v.getTag();
					activity.onChatRequest(buddy);
				}
			});
			
			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					MainActivity activity = (MainActivity) parent.getContext();
					Buddy buddy = (Buddy) v.getTag();
					activity.onBuddyContextMenuRequest(buddy, mResources);
					return true;
				}
			});
		} 
		
		TextView name = (TextView) view.findViewById(R.id.username);
		name.setText(buddy.getSafeName());
		
		ViewUtils.fillBuddyPlaceholder(parent.getContext(), buddy, view, mResources);
		
		return view;
	}

}
