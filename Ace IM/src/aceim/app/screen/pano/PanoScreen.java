package aceim.app.screen.pano;

import static aceim.app.utils.linq.KindaLinq.from;

import java.util.ArrayList;
import java.util.List;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.screen.Screen;
import aceim.app.utils.linq.KindaLinq;
import aceim.app.utils.linq.KindaLinqRule;
import aceim.app.view.page.Page;
import aceim.app.view.page.contactlist.ContactList;
import aceim.app.view.page.other.Splash;
import aceim.app.widgets.HorizontalListView;
import aceim.app.widgets.bottombar.BottomBarButton;
import aceim.app.widgets.pageselector.PanoPageAdapter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;

public class PanoScreen extends Screen {

	private final HorizontalListView mList;
	private final BottomBarButton mMenuButton;
	private final PanoPageAdapter mPageAdapter;
	
	private Page mSelectedPage;
	
	private final List<Page> mPages = new ArrayList<Page>();
	
	private final OnHierarchyChangeListener mTabChangedListener = new OnHierarchyChangeListener() {
		
		@Override
		public void onChildViewRemoved(View parent, View child) {}
		
		@Override
		public void onChildViewAdded(View parent, View child) {
			setSelectedPage((Page) child.getTag());
		}
	};
	
	public PanoScreen(MainActivity activity) {
		super(activity);
		LayoutInflater.from(activity).inflate(R.layout.screen_pano, this);
		
		mList = (HorizontalListView) findViewById(R.id.horizontal_list);
		mList.setOnHierarchyChangeListener(mTabChangedListener);
		
		mMenuButton = (BottomBarButton) findViewById(R.id.menu_button);
		mMenuButton.setOnClickListener(mMenuButtonClickListener);
		
		mPageAdapter = new PanoPageAdapter(activity, mTabClickListener, R.layout.screen_pano_item, mPages);
		mPageAdapter.setNotifyOnChange(true);
		
		int width = activity.getResources().getDisplayMetrics().widthPixels;
		int height = activity.getResources().getDisplayMetrics().heightPixels;
		
		int pageWidth = (int) (0.8 * (width < height ? width : height));
		
		mPageAdapter.setPageWidth(pageWidth);
		mList.setAdapter(mPageAdapter);
	}

	@Override
	public void addPage(Page page, boolean setAsCurrent) {
		page.setMainActivity(getActivity());
		
		mPageAdapter.add(page);
		
		if (setAsCurrent) {
			setSelectedPage(page);
		}
		
		if (!(page instanceof Splash)) {
			setMenuButtonAvailability();
		}
	}

	private void setMenuButtonAvailability() {
		mMenuButton.setVisibility(
				Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(getContext()).hasPermanentMenuKey()) ? 
						View.GONE : 
							View.VISIBLE);
	}

	@Override
	public Page findPage(final String pageId) {
		return from(mPages).where(new KindaLinqRule<Page>() {
			
			@Override
			public boolean match(Page t) {
				return t.getPageId().equals(pageId);
			}
		}).first();
	}

	@Override
	public void onPageChanged(String pageId) {}

	@Override
	public void setSelectedPage(String pageId) {
		setSelectedPage(findPage(pageId));
	}

	@Override
	public List<Page> findPagesByRule(KindaLinqRule<Page> rule) {
		return KindaLinq.from(mPages).where(rule).all();
	}

	@Override
	public Page getSelectedPage() {
		return mSelectedPage;
	}

	@Override
	public Page getSelectedContactList() {
		return (mSelectedPage instanceof ContactList) ? mSelectedPage : null;
	}

	@Override
	public void removePage(Page page) {
		mPageAdapter.remove(page);
		
		if (mPages.size() > 0) {
			setSelectedPage(mPages.get(0).getPageId());
		} else {
			getActivity().exitApplication();
		}
	}

	@Override
	public void updateTabWidget(Page p) {
		View tab = mList.findViewWithTag(p);
		
		if (tab == null) {
			return;
		}
		
		View tabWidget = tab.findViewById(R.id.indicator);
		
		mPageAdapter.fillWithImageAndTitle(tabWidget, p);
	}

	@Override
	public List<Page> getAllPages() {
		return mPages;
	}

	@Override
	public void storeScreenSpecificData(Bundle bundle) {}

	@Override
	public void recoverScreenSpecificData(Bundle bundle) {}

	private void setSelectedPage(Page page) {
		this.mSelectedPage = page;
		
		mList.setSelected(page);
	}	
}
