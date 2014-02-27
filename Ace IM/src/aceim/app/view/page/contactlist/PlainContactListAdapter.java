package aceim.app.view.page.contactlist;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.app.MainActivity;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.themeable.dataentity.ContactListItemThemeResource;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.contactlist.ContactListUpdater.ContactListModelGroup;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

public class PlainContactListAdapter extends ContactListAdapter {

	public PlainContactListAdapter(List<ContactListModelGroup> groups, ProtocolResources resources) {
		super(groups, resources);
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, final ViewGroup parent) {
		Buddy buddy = getChild(groupPosition, childPosition);
		
		final MainActivity activity = (MainActivity) parent.getContext();
		ContactListItemThemeResource ctr = activity.getThemesManager().getViewResources().getListItemLayout();
		
		View view = convertView;
		
		if (convertView == null) {
			view = ctr.getView();
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
		
		TextView name = (TextView) view.findViewById(ctr.getTitleTextViewId());
		name.setText(buddy.getSafeName());
		
		ViewUtils.fillBuddyPlaceholder(parent.getContext(), buddy, view, mResources, ctr, childPosition, groupPosition, (AbsListView) parent);
		
		return view;
	}

}
