package aceim.app.screen.pano;

import java.util.List;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.view.page.Page;
import aceim.app.widgets.pageselector.PageAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

class PanoPageAdapter extends PageAdapter {
	
	private int mPageWidth;
	
	public PanoPageAdapter(MainActivity activity, OnClickListener tabClickListener, List<Page> pages) {
		super(activity, tabClickListener, activity.getThemesManager().getViewResources().getTabItemLayout(), pages);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Page oldPage = (Page) (convertView != null ? convertView.getTag() : null);
		
		View view;
		
		if (convertView != null) {
			view = convertView;
		} else {
			view = LayoutInflater.from(getContext()).inflate(R.layout.screen_pano_item, null);
			View tab = super.getView(position, convertView, parent);
			((FrameLayout)view.findViewById(R.id.indicator)).addView(tab, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			view.setLayoutParams(new LayoutParams(mPageWidth, LayoutParams.MATCH_PARENT));
		}
		
		Page page = getItem(position);
		view.setTag(page);
		
		fillWithImageAndTitle(view, page);
		
		if (oldPage == null || oldPage != page) {
			FrameLayout container = (FrameLayout) view.findViewWithTag(getContext().getString(android.R.string.untitled));
			container.removeAllViews();
			container.addView(page.onCreateView(LayoutInflater.from(getContext()), null, null));
		}
		
		return view;
	}
	
	/**
	 * @return the mPageWidth
	 */
	public int getPageWidth() {
		return mPageWidth;
	}

	/**
	 * @param mPageWidth the mPageWidth to set
	 */
	public void setPageWidth(int mPageWidth) {
		this.mPageWidth = mPageWidth;
	}

}
