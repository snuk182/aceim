package aceim.app.widgets.adapters;

import java.util.ArrayList;
import java.util.List;

import aceim.api.dataentity.ListFeature;
import aceim.app.AceImException;
import aceim.app.R;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.widgets.adapters.IconTitleAdapter.IconTitleItem;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class IconTitleAdapter extends ArrayAdapter<IconTitleItem> {
	
	private int selectedItem = -1;
	private final int textViewResourceId;
	private final LayoutParams layoutParams;

	public IconTitleAdapter(Context context, int resource, List<IconTitleItem> objects) {
		this(context, resource, android.R.id.title, objects);
	}
	
	public IconTitleAdapter(Context context, int resource, int textViewResourceId, List<IconTitleItem> objects) {
		this(context, resource, textViewResourceId, objects, null);
	}
	
	public IconTitleAdapter(Context context, int resource, int textViewResourceId, List<IconTitleItem> objects, LayoutParams layoutParams) {
		super(context, resource, textViewResourceId, objects);
		this.textViewResourceId = textViewResourceId;
		this.layoutParams = layoutParams;
	}

	@SuppressWarnings("rawtypes")
	public View getView(final int position, View convertView, final ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		TextView title = (TextView) v.findViewById(textViewResourceId);
		
		IconTitleItem item = getItem(position);
		
		if (icon != null) {
			icon.setImageDrawable(item.icon);
		}
		if (title != null) {
			title.setText(item.title);
		}
		
		v.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((AdapterView)parent).performItemClick(v, position, 0);
			}
		});
		
		if (selectedItem > -1 && selectedItem == position) {
			v.setBackgroundResource(R.drawable.dummy_icon);
		} else {
			v.setBackgroundResource(android.R.drawable.menuitem_background);
		}
		
		if (item.id != null) {
			v.setTag(item.id);
		}
		
		if (layoutParams != null) {
			v.setLayoutParams(layoutParams);
		}
		
		return v;
	}

	public static class IconTitleItem {
		private Drawable icon;
		private String title;
		private String id;
		
		/**
		 * @return the icon
		 */
		public Drawable getIcon() {
			return icon;
		}
		
		/**
		 * @param icon the icon to set
		 */
		public void setIcon(Drawable icon) {
			this.icon = icon;
		}
		
		/**
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}
		
		/**
		 * @param title the title to set
		 */
		public void setTitle(String title) {
			this.title = title;
		}

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @param id the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}
	}
	
	public static IconTitleAdapter fromListFeature(Context context, int resource, ProtocolResources protocolResources, ListFeature feature) throws AceImException{
		List<IconTitleItem> objects = new ArrayList<IconTitleItem>();
		
		Resources resources = protocolResources.getNativeResourcesForProtocol(null);
		
		if (feature != null) {
			if (feature.isNullable()) {
				IconTitleItem noitem = new IconTitleItem();
				noitem.setTitle(context.getString(R.string.clear));
				noitem.setIcon(context.getResources().getDrawable(R.drawable.empty));
				objects.add(noitem);				
			}
			
			int[] names = feature.getNames();
			int[] drawables = feature.getDrawables();
			
			for (int i =0 ; i<Math.min(names.length, drawables.length); i++ ) {
				IconTitleItem item = new IconTitleItem();
				item.setTitle(resources.getString(names[i]));
				item.setIcon(resources.getDrawable(drawables[i]));
				objects.add(item);
			}
		}
		
		return new IconTitleAdapter(context, resource, objects);
	}

	/**
	 * @return the selectedItem
	 */
	public int getSelectedItem() {
		return selectedItem;
	}

	/**
	 * @param selectedItem the selectedItem to set
	 */
	public void setSelectedItem(int selectedItem) {
		this.selectedItem = selectedItem;
	}
}
