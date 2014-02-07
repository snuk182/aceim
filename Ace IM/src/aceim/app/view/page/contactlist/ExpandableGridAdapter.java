package aceim.app.view.page.contactlist;

import java.util.List;

import aceim.api.dataentity.BuddyGroup;
import aceim.app.AceIMActivity;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.view.page.contactlist.ContactListUpdater.ContactListModelGroup;
import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.GridView;

public class ExpandableGridAdapter extends ContactListAdapter {
	
	private static int sGridItemSize = 0;
	private static int sGridItemSpacing;
	
	public ExpandableGridAdapter(List<ContactListModelGroup> groups, ProtocolResources resources) {
		super(groups, resources);
		
				
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		ContactListModelGroup g = getGroup(groupPosition);
		
		if (sGridItemSize < 1) {
			initVariables((AceIMActivity) parent.getContext());
		}

		int colWidthDp = sGridItemSize;
		int rowHeightDp = sGridItemSize;

		GridView view = getGroupContentView(convertView, parent, groupPosition, sGridItemSpacing, colWidthDp, g);		

		// calculate the column and row counts based on your display
		final int colCount = (int) Math.floor(parent.getWidth() / (colWidthDp + sGridItemSpacing));
		final int rowCount = (int) Math.ceil((g.getBuddyList().size() + 0d) / colCount);

		// calculate the height for the current grid
		final int gridHeightDp = Math.round(rowCount * (rowHeightDp + sGridItemSpacing));

		// set the height of the current grid
		view.getLayoutParams().height = gridHeightDp;

		return view;
	}

	@SuppressLint("InlinedApi")
	protected GridView getGroupContentView(View convertView, View parent, int groupPosition, int spacingDp, int colWidthDp, BuddyGroup g) {
		GridView view;
		if (convertView == null) {
			view = new GridView(parent.getContext());
			view.setVerticalSpacing(spacingDp);
			view.setHorizontalSpacing(spacingDp);
			view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			view.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
			view.setNumColumns(GridView.AUTO_FIT);
			view.setColumnWidth(colWidthDp);
			view.setPadding(spacingDp, spacingDp, spacingDp, spacingDp);
		} else {
			view = (GridView) convertView;
		}

		if (view.getTag() == null || view.getTag() != g) {
			MainActivity activity = (MainActivity) parent.getContext();
			view.setAdapter(new GroupItemsAdapter(activity, activity.getThemesManager().getViewResources().getGridItemLayout(), g.getBuddyList(), mResources, groupPosition));
			view.setTag(g);
		} else {
			((GroupItemsAdapter)view.getAdapter()).notifyDataSetChanged();
		}
		
		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return 1;
	}
	
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}
	
	@Override
	public boolean hasStableIds() {
        return false;
    }
	
	private void initVariables(AceIMActivity activity) {
		sGridItemSpacing = activity.getResources().getDimensionPixelSize(R.dimen.contact_list_grid_items_spacing);
		
		TypedArray array = activity.getThemesManager().getCurrentTheme().obtainStyledAttributes(aceim.res.R.styleable.Ace_IM_Theme);
		
		for (int i =0; i< array.getIndexCount(); i++) {
			int res = array.getIndex(i);
			
			switch (res) {
			case aceim.res.R.styleable.Ace_IM_Theme_grid_item_size:
				sGridItemSize = array.getDimensionPixelSize(i, 100);
				break;
			}
		}
		
		array.recycle();
	}
}
