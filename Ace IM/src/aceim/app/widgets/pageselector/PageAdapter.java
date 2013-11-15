package aceim.app.widgets.pageselector;

import java.util.List;

import aceim.app.R;
import aceim.app.view.page.Page;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PageAdapter extends ArrayAdapter<Page> {

	private OnClickListener mOnClickListener;
	
	public PageAdapter(Context context, OnClickListener onClickListener, int itemLayoutId, List<Page> pages) {
		super(context, itemLayoutId, android.R.id.title, pages);
		this.mOnClickListener = onClickListener;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = super.getView(position, convertView, parent);

		if (convertView == null && mOnClickListener != null) {
			view.setOnClickListener(mOnClickListener);
		}
		
		Page page = getItem(position);

		//if (view.getTag() != page) {
			view.setTag(page);
			fillWithImageAndTitle(view, page);
		//}
		
		return view;
	}

	public void fillWithImageAndTitle(View view, Page page) {
		if (view == null || page != view.getTag()) {
			return;
		}
		
		ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
		TextView title = (TextView) view.findViewById(android.R.id.title);
		icon.setImageDrawable(page.getIcon(getContext()));
		title.setText(page.getTitle(getContext()));
	}
}
