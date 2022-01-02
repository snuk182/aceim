package aceim.app.view.page.contactlist;

import java.util.List;

import aceim.app.MainActivity;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.themeable.dataentity.ContactListItemThemeResource;
import aceim.app.view.page.contactlist.ContactListUpdater.ContactListModelGroup;
import android.view.View;
import android.view.ViewGroup;

public class DoubleListAdapter extends ContactListAdapter {

	public DoubleListAdapter(List<ContactListModelGroup> groups, ProtocolResources resources) {
		super(groups, resources);
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		final MainActivity activity = (MainActivity) parent.getContext();
		ContactListItemThemeResource ctr = activity.getThemesManager().getViewResources().getListItemLayout();
		
		return constructChildViewFromThemeResource(groupPosition, childPosition, isLastChild, convertView, parent, ctr);
	}
}
