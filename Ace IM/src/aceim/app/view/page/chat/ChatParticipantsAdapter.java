package aceim.app.view.page.chat;

import java.util.Collections;
import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.themeable.dataentity.ContactListItemThemeResource;
import aceim.app.utils.ViewUtils;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class ChatParticipantsAdapter extends BaseExpandableListAdapter {
	
	private final List<BuddyGroup> mParticipantGroups;
	private final ProtocolResources mResources;

	public ChatParticipantsAdapter(List<BuddyGroup> participantGroups, ProtocolResources resources) {
		if (participantGroups != null) {
			this.mParticipantGroups = participantGroups;
		} else {
			this.mParticipantGroups = Collections.emptyList();
		}
		
		this.mResources = resources;
	}

	@Override
	public int getGroupCount() {
		return mParticipantGroups.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mParticipantGroups.get(groupPosition).getBuddyList().size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mParticipantGroups.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mParticipantGroups.get(groupPosition).getBuddyList().get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return mParticipantGroups.get(groupPosition).getId().hashCode();
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return mParticipantGroups.get(groupPosition).getBuddyList().get(childPosition).getId();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, final ViewGroup parent) {
		final BuddyGroup g = (BuddyGroup) getGroup(groupPosition);

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
		text.setTextSize(parent.getContext().getResources().getDimension(R.dimen.chat_participant_group_header_size));

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
		
		return item;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, final ViewGroup parent) {
		Buddy buddy = (Buddy) getChild(groupPosition, childPosition);
		
		MainActivity activity = (MainActivity)parent.getContext();	
		ContactListItemThemeResource itemResource = activity.getThemesManager().getViewResources().getGridItemLayout();

		View view = convertView;
		
		if (convertView == null) {
			view = ViewUtils.fromThemeResource(itemResource);
		}
		
		if (view.getTag() == null || view.getTag() != buddy) {
			view.setTag(buddy);
			/*view.setOnClickListener(new OnClickListener() {
				
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
			});*/
		} 
		
		TextView username = (TextView) view.findViewById(itemResource.getTitleTextViewId());
		username.setText(buddy.getSafeName());
		
		ViewUtils.fillBuddyPlaceholder(parent.getContext(), buddy, view, mResources, itemResource, childPosition, groupPosition, (AbsListView) parent);
		
		/*int size = parent.getContext().getResources().getDimensionPixelSize(R.dimen.contact_list_grid_item_size);
		view.setLayoutParams(new AbsListView.LayoutParams(size, size));*/
		
		return view;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	

}
