package aceim.app.widgets.bottombar;

import aceim.app.AceIMActivity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

public class BottomBar extends LinearLayout {
	
	private Drawable sBackground;

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	@SuppressWarnings("deprecation")
	private void init(Context context, AttributeSet attrs) {
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		
		initVariables((AceIMActivity) context);
		
		setBackgroundDrawable(sBackground);
	}
	
	private void initVariables(AceIMActivity activity) {
		TypedArray array = activity.getThemesManager().getCurrentTheme().obtainStyledAttributes(aceim.res.R.styleable.Ace_IM_Theme);
		
		for (int i =0; i< array.getIndexCount(); i++) {
			int res = array.getIndex(i);
			
			switch (res) {
			case aceim.res.R.styleable.Ace_IM_Theme_bottom_bar_background:
				sBackground = array.getDrawable(i);
				break;
			}
		}
		
		array.recycle();
	}
}
