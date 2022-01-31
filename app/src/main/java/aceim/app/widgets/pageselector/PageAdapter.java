package aceim.app.widgets.pageselector;

import java.util.List;

import aceim.app.themeable.dataentity.TabThemeResource;
import aceim.app.view.page.Page;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.androidquery.AQuery;

public class PageAdapter extends ArrayAdapter<Page> {

	private final OnClickListener mOnClickListener;
	private final TabThemeResource mTabResource;
	
	private AQuery mAq;
	
	public PageAdapter(Context context, OnClickListener onClickListener, TabThemeResource tabThemeResource, List<Page> pages) {
		super(context, 0, 0, pages);
		this.mOnClickListener = onClickListener;
		this.mTabResource = tabThemeResource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			convertView = mTabResource.getView();
			if (mOnClickListener != null) {
				convertView.setOnClickListener(mOnClickListener);
			}
		}

		Page page = getItem(position);

		convertView.setTag(page);
		fillWithImageAndTitle(convertView, page);
		
		return convertView;
	}

	public void fillWithImageAndTitle(View view, Page page) {
		if (view == null || page != view.getTag()) {
			return;
		}
		
		if (mAq == null) {
			mAq = new AQuery(view);
		} else {
			mAq.recycle(view);
		}
		
		mAq.id(android.R.id.icon).image(page.getIcon(getContext()));
		mAq.id(android.R.id.title).text(page.getTitle(getContext()));
	}
}
