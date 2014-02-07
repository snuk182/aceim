package aceim.app.themeable.dataentity;

import android.content.Context;

public class TabThemeResource extends ThemeResource {
	
	private final int iconImageViewId = android.R.id.icon;
	private final int titleTextViewId = android.R.id.title;

	public TabThemeResource(Context context, int id) {
		super(context, id);
	}

	/**
	 * @return the iconImageViewId
	 */
	public int getIconImageViewId() {
		return iconImageViewId;
	}

	/**
	 * @return the titleTextViewId
	 */
	public int getTitleTextViewId() {
		return titleTextViewId;
	}
}
