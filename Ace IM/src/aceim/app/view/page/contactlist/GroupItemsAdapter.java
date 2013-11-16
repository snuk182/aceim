package aceim.app.view.page.contactlist;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.ViewUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class GroupItemsAdapter extends ArrayAdapter<Buddy> {

	private final ProtocolResources mResources;

	public GroupItemsAdapter(MainActivity activity, int resource, int textViewResourceId, List<Buddy> buddyList, ProtocolResources resources) {
		super(activity, resource, textViewResourceId, buddyList);
		this.mResources = resources;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Buddy buddy = getItem(position);
		View view = super.getView(position, convertView, parent);

		if (view.getTag() == null || view.getTag() != buddy) {
			view.setTag(buddy);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					MainActivity activity = (MainActivity) getContext();
					Buddy buddy = (Buddy) v.getTag();
					activity.onChatRequest(buddy);
				}
			});

			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					MainActivity activity = (MainActivity) getContext();
					Buddy buddy = (Buddy) v.getTag();
					activity.onBuddyContextMenuRequest(buddy, mResources);
					return true;
				}
			});
		}
		
		TextView name = (TextView) view.findViewById(R.id.username);
		name.setText(buddy.getSafeName());
		
		ViewUtils.fillBuddyPlaceholder(getContext(), buddy, view, mResources);

		return view;
	}
}
