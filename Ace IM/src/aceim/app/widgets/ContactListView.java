package aceim.app.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

public class ContactListView extends ExpandableListView {

	public ContactListView(Context context) {
		super(context);
	}

	public ContactListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ContactListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public long getSelectedPosition(){
		return PACKED_POSITION_VALUE_NULL;
	}
}
