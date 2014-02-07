package aceim.app.themeable.dataentity;

import android.content.Context;

public abstract class ThemeResource {

	private final Context mContext;
	private final int mId;
	/**
	 * @return the mContext
	 */
	public Context getContext() {
		return mContext;
	}
	/**
	 * @return the mId
	 */
	public int getId() {
		return mId;
	}
	
	protected ThemeResource(Context context, int id) {
		this.mContext = context;
		this.mId = id;
	}
}
