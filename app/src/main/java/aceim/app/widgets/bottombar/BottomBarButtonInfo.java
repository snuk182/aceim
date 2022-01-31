package aceim.app.widgets.bottombar;

import android.graphics.drawable.Drawable;
import android.view.View.OnClickListener;

public abstract class BottomBarButtonInfo implements OnClickListener {
	
	public final Drawable icon;
	
	public BottomBarButtonInfo(Drawable icon) {
		this.icon = icon;
	}
}
