package aceim.app.themeable.widgets;

import aceim.app.AceIMActivity;
import android.content.Context;
import android.util.AttributeSet;
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

}
