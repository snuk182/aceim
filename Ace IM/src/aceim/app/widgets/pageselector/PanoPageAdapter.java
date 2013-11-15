package aceim.app.widgets.pageselector;

import java.util.List;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.view.page.Page;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class PanoPageAdapter extends PageAdapter {
	
	private int mPageWidth;
	
	public PanoPageAdapter(MainActivity activity, OnClickListener tabClickListener, int itemLayoutId, List<Page> pages) {
		super(activity, tabClickListener, R.layout.screen_pano_item, pages);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Page oldPage = (Page) (convertView != null ? convertView.getTag() : null);
		
		View view = super.getView(position, convertView, parent);
		view.getLayoutParams().width = mPageWidth;
		
		Page page = getItem(position);
		view.setTag(page);
		
		fillWithImageAndTitle(view, page);
		
		if (oldPage == null || oldPage != page) {
			FrameLayout container = (FrameLayout) view.findViewWithTag(getContext().getString(android.R.string.untitled));
			container.removeAllViews();
			container.addView(page.onCreateView(LayoutInflater.from(getContext()), null, null));
		}
			
		/*if (oldPage == null || oldPage != page) {
			
			FragmentManager fm = ((MainActivity) getContext()).getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction().disallowAddToBackStack();
			Page newPage = (Page) fm.findFragmentByTag(page.getPageId());
			
			if (oldPage != null) {
				ft.detach(oldPage);
			}
			
			//http://code.google.com/p/android/issues/detail?id=27741
			View container = view.findViewWithTag(getContext().getString(android.R.string.untitled));
			
			if (newPage == null) {
				newPage = page;
				
				if (newPage.getId() != 0) {
					container.setId(newPage.getId());
				} else {
					if (container.getId() == -1) {
						if (mContainerIdIndex >= Integer.MAX_VALUE) {
							mContainerIdIndex = 0;
						}
						
						container.setId(++mContainerIdIndex);
					}	
				}
				
				ft.attach(newPage);			
			} else {
				container.setId(newPage.getId());				
			}
			
			ft.replace(container.getId(), page);
			
			ft.commit();
		}		*/
		
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
