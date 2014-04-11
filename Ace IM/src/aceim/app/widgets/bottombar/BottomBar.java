package aceim.app.widgets.bottombar;

import aceim.app.AceIMActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

public class BottomBar extends LinearLayout {
	
	private Drawable mBackground;

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	@SuppressWarnings("deprecation")
	private void init(Context context, AttributeSet attrs) {
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		
		initVariables((AceIMActivity) context);
		
		setBackgroundDrawable(mBackground);
	}
	
	private void initVariables(AceIMActivity activity) {
		Resources themeResources = activity.getThemesManager().getCurrentThemeContext().getResources();
		mBackground = themeResources.getDrawable(activity.getThemesManager().getViewResources().getBottomBarBackgroundId());			
	}
}
