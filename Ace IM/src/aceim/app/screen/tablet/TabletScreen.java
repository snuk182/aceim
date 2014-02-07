package aceim.app.screen.tablet;

import static aceim.app.utils.linq.KindaLinq.from;

import java.util.ArrayList;
import java.util.List;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.screen.Screen;
import aceim.app.utils.PageManager;
import aceim.app.utils.linq.KindaLinqRule;
import aceim.app.view.page.Page;
import aceim.app.view.page.contactlist.ContactList;
import aceim.app.view.page.other.Splash;
import aceim.app.widgets.bottombar.BottomBarButton;
import aceim.app.widgets.pageselector.PageAdapter;
import aceim.app.widgets.pageselector.TabSelector;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;

public class TabletScreen extends Screen {

	private final PageAdapter mPageAdapterLeft;
	private final TabSelector mTabHolderLeft;
	private final PageManager mPageManagerLeft;
	
	private final PageAdapter mPageAdapterRight;
	private final TabSelector mTabHolderRight;
	private final PageManager mPageManagerRight;
	
	private final BottomBarButton mMenuButton;
	
	private final OnHierarchyChangeListener mTabChangedListener = new OnHierarchyChangeListener() {
		
		@Override
		public void onChildViewRemoved(View parent, View child) {
			if (parent == mTabHolderLeft) {
				findViewById(R.id.fragment_holder_left).setBackgroundResource(mPageAdapterLeft.getCount() > 0 ? 0 : R.drawable.cornered_background);
			} else {
				findViewById(R.id.fragment_holder_right).setBackgroundResource(mPageAdapterRight.getCount() > 0 ? 0 : R.drawable.cornered_background);
			}
		}
		
		@Override
		public void onChildViewAdded(View parent, View child) {
			if (parent == mTabHolderLeft) {
				mTabHolderLeft.setSelectedPage((Page) child.getTag());
				findViewById(R.id.fragment_holder_left).setBackgroundResource(0);
			} else {
				mTabHolderRight.setSelectedPage((Page) child.getTag());
				findViewById(R.id.fragment_holder_right).setBackgroundResource(0);
			}
		}
	};
	
	public TabletScreen(MainActivity activity) {
		super(activity);
		LayoutInflater.from(activity).inflate(R.layout.screen_tablet, this);
		
		mMenuButton = (BottomBarButton) findViewById(R.id.menu_button);
		mMenuButton.setOnClickListener(mMenuButtonClickListener);
		mMenuButton.setOnLongClickListener(mMenuButtonLongClickListener);
		
		mTabHolderLeft = (TabSelector) findViewById(R.id.tab_selector_left);
		mPageManagerLeft = new PageManager(R.id.fragment_holder_left, activity);
		mPageAdapterLeft = new PageAdapter(activity, mTabClickListener, activity.getThemesManager().getViewResources().getTabItemLayout(), mPageManagerLeft.getPages());
		mPageAdapterLeft.setNotifyOnChange(true);
		mTabHolderLeft.setPageAdapter(mPageAdapterLeft);
		mTabHolderLeft.setOnHierarchyChangeListener(mTabChangedListener);
		
		mTabHolderRight = (TabSelector) findViewById(R.id.tab_selector_right);
		mPageManagerRight = new PageManager(R.id.fragment_holder_right, activity);
		mPageAdapterRight = new PageAdapter(activity, mTabClickListener, activity.getThemesManager().getViewResources().getTabItemLayout(), mPageManagerRight.getPages());
		mPageAdapterRight.setNotifyOnChange(true);
		mTabHolderRight.setPageAdapter(mPageAdapterRight);
		mTabHolderRight.setOnHierarchyChangeListener(mTabChangedListener);		
	}

	@Override
	public void addPage(Page page, boolean setAsCurrent) {
		String pageId = page.getPageId();
		
		if (findPage(pageId) == null) {
			if (page instanceof ContactList) {
				mPageAdapterLeft.add(page);
			} else {
				mPageAdapterRight.add(page);
			}
		}
		
		if (setAsCurrent) {
			setSelectedPage(pageId);
		}
		
		if (!(page instanceof Splash)) {
			setMenuButtonAvailability();
		}
	}

	@Override
	public Page findPage(final String pageId) {
		return from(getAllPages()).where(new KindaLinqRule<Page>() {
			
			@Override
			public boolean match(Page t) {
				return t.getPageId().equals(pageId);
			}
		}).first();
	}

	@Override
	public void onPageChanged(String pageId) {
		if (pageId.startsWith(ContactList.class.getSimpleName())) {
			mPageManagerLeft.onPageChanged(findPage(mPageManagerLeft.getPages(), pageId));
		} else {
			mPageManagerRight.onPageChanged(findPage(mPageManagerRight.getPages(), pageId));
		}
	}

	@Override
	public void setSelectedPage(String pageId) {
		onPageChanged(pageId);
		
		if (pageId.startsWith(ContactList.class.getSimpleName())) {
			Page page = findPage(mPageManagerLeft.getPages(), pageId);		
			mTabHolderLeft.setSelectedPage(page);
		} else {
			Page page = findPage(mPageManagerRight.getPages(), pageId);		
			mTabHolderRight.setSelectedPage(page);
		}
	}

	@Override
	public List<Page> findPagesByRule(KindaLinqRule<Page> rule) {
		return from(getAllPages()).where(rule).all();		
	}

	@Override
	public Page getSelectedPage() {
		return mPageManagerRight.getSelectedPage() != null ? mPageManagerRight.getSelectedPage() : mPageManagerLeft.getSelectedPage();
	}

	@Override
	public Page getSelectedContactList() {
		return mPageManagerLeft.getSelectedPage();
	}

	@Override
	public void removePage(Page page) {
		
		if (mPageManagerRight.getPages().contains(page)) {
			mPageAdapterRight.remove(page);
			mPageManagerRight.onPageRemoved(page);
			
			if (mPageAdapterRight.getCount() > 0) {
				setSelectedPage(mPageManagerRight.getPages().get(0).getPageId());
			}
		} else {
			mPageAdapterLeft.remove(page);
			mPageManagerLeft.onPageRemoved(page);
			
			if (mPageAdapterLeft.getCount() > 0) {
				setSelectedPage(mPageManagerLeft.getPages().get(0).getPageId());
			}
		}
		
		if (mPageAdapterLeft.getCount() + mPageAdapterRight.getCount() < 1) {
			getActivity().exitApplication();
		}
	}

	@Override
	public void updateTabWidget(Page p) {
		if (mPageManagerLeft.getPages().contains(p)) {
			View tabWidget = mTabHolderLeft.findViewWithTag(p);
			
			mPageAdapterLeft.fillWithImageAndTitle(tabWidget, p);
		} else {
			View tabWidget = mTabHolderRight.findViewWithTag(p);
			
			mPageAdapterRight.fillWithImageAndTitle(tabWidget, p);
		}
	}

	@SuppressWarnings("serial")
	@Override
	public List<Page> getAllPages() {
		return new ArrayList<Page>(mPageAdapterLeft.getCount() + mPageAdapterRight.getCount()){
			{ 
				addAll(mPageManagerLeft.getPages()); 
				addAll(mPageManagerRight.getPages());
			}
		};
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		Page leftSelectedPage = mPageManagerLeft.getSelectedPage();
		Page rightSelectedPage = mPageManagerRight.getSelectedPage();
		
		if (rightSelectedPage != null) {
			rightSelectedPage.onCreateOptionsMenu(menu, menuInflater);
		}
		if (leftSelectedPage != null) {
			leftSelectedPage.onCreateOptionsMenu(menu, menuInflater);
		}
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		Page leftSelectedPage = mPageManagerLeft.getSelectedPage();
		Page rightSelectedPage = mPageManagerRight.getSelectedPage();
		
		if (rightSelectedPage != null && rightSelectedPage.hasMenu()) {
			rightSelectedPage.onPrepareOptionsMenu(menu);
		}
		if (leftSelectedPage != null) {
			leftSelectedPage.onPrepareOptionsMenu(menu);
		}
	}

	@Override
	public void onOptionsItemSelected(MenuItem item) {
		Page leftSelectedPage = mPageManagerLeft.getSelectedPage();
		Page rightSelectedPage = mPageManagerRight.getSelectedPage();
		
		if (rightSelectedPage != null) {
			rightSelectedPage.onOptionsItemSelected(item);
		}
		if (leftSelectedPage != null) {
			leftSelectedPage.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onCurrentPageKeyDown(int i, KeyEvent event) {
		Page leftSelectedPage = mPageManagerLeft.getSelectedPage();
		Page rightSelectedPage = mPageManagerRight.getSelectedPage();
		
		if (rightSelectedPage != null) {
			return rightSelectedPage.onKeyDown(i, event);
		}
		if (leftSelectedPage != null) {
			return leftSelectedPage.onKeyDown(i, event);
		}
		
		return false;
	}

	@Override
	public void storeScreenSpecificData(Bundle bundle) {}

	@Override
	public void recoverScreenSpecificData(Bundle bundle) {}
	
	private Page findPage(List<Page> pages, final String pageId) {
		return from(pages).where(new KindaLinqRule<Page>() {
			
			@Override
			public boolean match(Page t) {
				return t.getPageId().equals(pageId);
			}
		}).first();
	}
	
	@SuppressLint("NewApi")
	private void setMenuButtonAvailability() {
		mMenuButton.setVisibility(
				Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(getContext()).hasPermanentMenuKey()) ? 
						View.GONE : 
							View.VISIBLE);
	}
}
