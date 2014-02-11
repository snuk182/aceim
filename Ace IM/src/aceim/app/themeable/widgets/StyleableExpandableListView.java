package aceim.app.themeable.widgets;

import aceim.app.AceIMActivity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class StyleableExpandableListView extends ExpandableListView {

	public StyleableExpandableListView(Context context) {
		super(context);
	}

	public StyleableExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		if (context instanceof AceIMActivity) {
			((AceIMActivity)context).setStyle(this, attrs);
		}
	}

	public StyleableExpandableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		if (context instanceof AceIMActivity) {
			((AceIMActivity)context).setStyle(this, attrs);
		}
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params){
		ViewGroup.LayoutParams oldParams = getLayoutParams();
		
		if (params != null && oldParams != null && (oldParams.width == AceIMActivity.ARTIFICIAL_LAYOUT_MARKER || oldParams.height == AceIMActivity.ARTIFICIAL_LAYOUT_MARKER)) {
			if (oldParams.width != AceIMActivity.ARTIFICIAL_LAYOUT_MARKER) {
				params.width = oldParams.width;
			}
			if (oldParams.height != AceIMActivity.ARTIFICIAL_LAYOUT_MARKER) {
				params.height = oldParams.height;
			}
		}
		
		super.setLayoutParams(params);
	}
	
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		LayoutParams lp = super.generateLayoutParams(attrs);
		
		if (getContext() instanceof AceIMActivity) {
			((AceIMActivity)getContext()).fillLayoutParams(lp, attrs);
		}
		
		return lp;
    }
}
