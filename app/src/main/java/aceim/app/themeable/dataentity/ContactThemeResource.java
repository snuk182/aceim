package aceim.app.themeable.dataentity;

import android.content.Context;

public class ContactThemeResource extends ThemeResource {

	private int buddyStatusImageId;
	private int xstatusTextViewId;
	private int iconImageId;
	private int titleTextViewId;
	private int[] extraImageIDs;
	
	public ContactThemeResource(Context context, int id) {
		super(context, id);
	}

	/**
	 * @return the buddyStatusImageId
	 */
	public int getBuddyStatusImageId() {
		return buddyStatusImageId;
	}

	/**
	 * @param buddyStatusImageId the buddyStatusImageId to set
	 */
	protected void setBuddyStatusImageId(int buddyStatusImageId) {
		this.buddyStatusImageId = buddyStatusImageId;
	}

	/**
	 * @return the xstatusTextViewId
	 */
	public int getXstatusTextViewId() {
		return xstatusTextViewId;
	}

	/**
	 * @param xstatusTextViewId the xstatusTextViewId to set
	 */
	protected void setXstatusTextViewId(int xstatusTextViewId) {
		this.xstatusTextViewId = xstatusTextViewId;
	}

	/**
	 * @return the iconImageId
	 */
	public int getIconImageId() {
		return iconImageId;
	}

	/**
	 * @param iconImageId the iconImageId to set
	 */
	protected void setIconImageId(int iconImageId) {
		this.iconImageId = iconImageId;
	}

	/**
	 * @return the titleTextViewId
	 */
	public int getTitleTextViewId() {
		return titleTextViewId;
	}

	/**
	 * @param titleTextViewId the titleTextViewId to set
	 */
	protected void setTitleTextViewId(int titleTextViewId) {
		this.titleTextViewId = titleTextViewId;
	}

	/**
	 * @return the extraImageIDs
	 */
	public int[] getExtraImageIDs() {
		return extraImageIDs;
	}

	/**
	 * @param extraImageIDs the extraImageIDs to set
	 */
	protected void setExtraImageIDs(int[] extraImageIDs) {
		this.extraImageIDs = extraImageIDs;
	}

}
