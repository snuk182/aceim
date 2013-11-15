package aceim.app.widgets.bottombar;

import aceim.app.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

public class BottomBar extends LinearLayout {

	public BottomBar(Context context) {
		super(context);
		init();
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		setBackgroundResource(R.drawable.top_bottom_bar_background);
		//setPadding(0, getContext().getResources().getDimensionPixelSize(R.dimen.top_bottom_bar_top_padding), 0, 0);
	}
}
