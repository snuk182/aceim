package aceim.app.screen.simple;

import static aceim.app.utils.linq.KindaLinq.from;

import java.util.List;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.screen.Screen;
import aceim.app.utils.PageManager;
import aceim.app.utils.linq.KindaLinqRule;
import aceim.app.view.page.Page;
import aceim.app.view.page.Page.PageChangedListener;
import aceim.app.view.page.contactlist.ContactList;
import aceim.app.view.page.other.Splash;
import aceim.app.widgets.bottombar.BottomBarButton;
import aceim.app.widgets.pageselector.PageAdapter;
import aceim.app.widgets.pageselector.TabSelector;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;

public class SimpleScreen extends Screen implements PageChangedListener {
	
	private final PageAdapter mPageAdapter;
	private final TabSelector mTabHolder;
	private final BottomBarButton mMenuButton;
	private final PageManager mPageManager;
	
	private final OnHierarchyChangeListener mTabChangedListener = new OnHierarchyChangeListener() {
		
		@Override
		public void onChildViewRemoved(View parent, View child) {}
		
		@Override
		public void onChildViewAdded(View parent, View child) {
			mTabHolder.setSelectedPage((Page) child.getTag());
		}
	};
	
	public SimpleScreen(MainActivity activity) {
		super(activity);
		LayoutInflater.from(activity).inflate(R.layout.screen_simple, this);
		mTabHolder = (TabSelector) findViewById(R.id.tab_selector);
		mMenuButton = (BottomBarButton) findViewById(R.id.menu_button);
		
		mPageManager = new PageManager(R.id.fragment_holder, activity);
		mPageAdapter = new PageAdapter(activity, mTabClickListener, activity.getThemesManager().getViewResources().getTabItemLayout(), mPageManager.getPages());
		mPageAdapter.setNotifyOnChange(true);
		mTabHolder.setPageAdapter(mPageAdapter);
		mTabHolder.setOnHierarchyChangeListener(mTabChangedListener);
		mMenuButton.setOnClickListener(mMenuButtonClickListener);
		mMenuButton.setOnLongClickListener(mMenuButtonLongClickListener);
	}
	
	@Override
	public void addPage(Page page, boolean setAsCurrent) {
		String pageId = page.getPageId();
		
		if (findPage(pageId) == null) {
			mPageAdapter.add(page);
		}
		
		if (setAsCurrent) {
			setSelectedPage(pageId);
		}
	}

	//TODO move to root Screen class
	@SuppressLint("NewApi")
	private void setMenuButtonAvailability() {
		mMenuButton.setVisibility(
				Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(getContext()).hasPermanentMenuKey()) ? 
						View.GONE : 
							View.VISIBLE);
	}

	@Override
	public Page findPage(final String pageId) {
		return from(mPageManager.getPages()).where(new KindaLinqRule<Page>() {
			
			@Override
			public boolean match(Page t) {
				return t.getPageId().equals(pageId);
			}
		}).first();
	}

	@Override
	public void onPageChanged(String pageId) {
		mPageManager.onPageChanged(findPage(pageId));
	}

	@Override
	public void setSelectedPage(String pageId) {
		onPageChanged(pageId);
		
		Page page = findPage(pageId);		
		mTabHolder.setSelectedPage(page);
		
		if (!(page instanceof Splash) && page.hasMenu()) {
			setMenuButtonAvailability();
		} else {
			mMenuButton.setVisibility(View.GONE);
		}
	}

	@Override
	public List<Page> findPagesByRule(KindaLinqRule<Page> rule) {
		return from(mPageManager.getPages()).where(rule).all();		
	}

	@Override
	public Page getSelectedPage() {
		return mPageManager.getSelectedPage();
	}

	@Override
	public Page getSelectedContactList() {
		return (mPageManager.getSelectedPage() instanceof ContactList) ? mPageManager.getSelectedPage() : null;
	}

	@Override
	public void removePage(Page page) {
		mPageAdapter.remove(page);
		mPageManager.onPageRemoved(page);
		if (mPageManager.getPages().size() > 0) {
			setSelectedPage(mPageManager.getPages().get(0).getPageId());
		} else {
			getActivity().exitApplication();
		}
	}

	@Override
	public void updateTabWidget(Page p) {
		View tabWidget = mTabHolder.findViewWithTag(p);
		
		mPageAdapter.fillWithImageAndTitle(tabWidget, p);
	}

	@Override
	public List<Page> getAllPages() {
		return mPageManager.getPages();
	}

	@Override
	public void storeScreenSpecificData(Bundle bundle) {}

	@Override
	public void recoverScreenSpecificData(Bundle bundle) {}
}
