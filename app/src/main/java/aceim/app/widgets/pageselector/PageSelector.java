package aceim.app.widgets.pageselector;

import aceim.app.view.page.Page;

public interface PageSelector {

	public PageAdapter getPageAdapter();
	
	public void setPageAdapter(PageAdapter adapter);
	
	public void setSelectedPage(Page page);
}
