package aceim.app.themeable.widgets.related;

import aceim.app.AceIMActivity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class GridViewForStyleables extends GridView {

	public GridViewForStyleables(Context context) {
		super(context);
	}

	public GridViewForStyleables(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GridViewForStyleables(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
