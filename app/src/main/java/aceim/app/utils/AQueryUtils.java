package aceim.app.utils;

import ua.snuk182.expandablegrid.ExpandableGridView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import com.androidquery.AQuery;
import com.androidquery.callback.BitmapAjaxCallback;

public final class AQueryUtils {

	private AQueryUtils() {}
	
	public static boolean shouldDelay(int groupPosition, int childPosition, View convertView, ViewGroup parent, String url){
		
		if(url == null || BitmapAjaxCallback.isMemoryCached(url)){
			return false;
		}
		
		AbsListView lv = (AbsListView) parent;
		
		
		OnScrollListener sl = (OnScrollListener) parent.getTag(AQuery.TAG_SCROLL_LISTENER);
		
		if(sl == null){
			sl = new ExpandableGridScrollListener();
			lv.setOnScrollListener(sl);
			parent.setTag(AQuery.TAG_SCROLL_LISTENER, sl);
		}
		
		Integer scrollState = (Integer) lv.getTag(AQuery.TAG_NUM);
		
		if(scrollState == null || scrollState == OnScrollListener.SCROLL_STATE_IDLE || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
			return false;
		}
		
		/*long packed = childPosition;
		if(parent instanceof ExpandableListView){
			packed = ExpandableListView.getPackedPositionForChild(groupPosition, childPosition);
		}
		convertView.setTag(AQuery.TAG_NUM, packed);*/
		
		//TODO add draw count and skip drawing list if possible
		
		return true;
	}
	
	private static class ExpandableGridScrollListener implements OnScrollListener {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			
			if(view instanceof ExpandableGridView){
				onScrollStateChanged((ExpandableListView) view, scrollState); 
			}
		}
		
		private void onScrollStateChanged(ExpandableListView elv, int scrollState){
			
			elv.setTag(AQuery.TAG_NUM, scrollState);
			
			if(scrollState == SCROLL_STATE_IDLE){
				
				int first = elv.getFirstVisiblePosition();
				int last = elv.getLastVisiblePosition();
				
				int count = last - first;
				
				ExpandableListAdapter ela = elv.getExpandableListAdapter();
				
				for(int i = 0; i <= count; i++){
				
					long packed = elv.getExpandableListPosition(i + first);
					
					int group = ExpandableListView.getPackedPositionGroup(packed);
					int child = ExpandableListView.getPackedPositionChild(packed);
					
					if(group >= 0){
						
						View convertView = elv.getChildAt(i);
						//Long targetPacked = (Long) convertView.getTag(AQuery.TAG_NUM);						
						//if(targetPacked != null && targetPacked.longValue() == packed){
						
							if(child == -1){
							
								ela.getGroupView(group, elv.isGroupExpanded(group), convertView, elv);
								
							}else{
								
								ela.getChildView(group, child, child == ela.getChildrenCount(group) - 1, convertView, elv);
								
							}
							//convertView.setTag(AQuery.TAG_NUM, null);
						//}						
					}
				}
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
		
	}
}
