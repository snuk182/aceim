package aceim.app.themeable.dataentity;

import android.content.Context;

public class ChatMessageItemThemeResource extends ThemeResource {

	// Chat item IDs
	private final int senderTextViewId;
	private final int timeTextViewId;
	private final int messageStatusImageId;
	private final int messageTextViewId;
	private final int checkboxId;
	private final int iconImageViewId;

	public ChatMessageItemThemeResource(Context context, int id) {
		super(context, id);
		
		senderTextViewId = getContext().getResources().getIdentifier("sender", "id", getContext().getPackageName());
		messageStatusImageId = getContext().getResources().getIdentifier("status", "id", getContext().getPackageName());
		messageTextViewId = getContext().getResources().getIdentifier("message", "id", getContext().getPackageName());
		timeTextViewId = getContext().getResources().getIdentifier("time", "id", getContext().getPackageName());
		checkboxId = getContext().getResources().getIdentifier("checkbox", "id", getContext().getPackageName());
		iconImageViewId = getContext().getResources().getIdentifier("icon", "id", getContext().getPackageName());
	}

	/**
	 * @return the senderTextViewId
	 */
	public int getSenderTextViewId() {
		return senderTextViewId;
	}

	/**
	 * @return the timeTextViewId
	 */
	public int getTimeTextViewId() {
		return timeTextViewId;
	}

	/**
	 * @return the messageStatusImageId
	 */
	public int getMessageStatusImageId() {
		return messageStatusImageId;
	}

	/**
	 * @return the messageTextViewId
	 */
	public int getMessageTextViewId() {
		return messageTextViewId;
	}

	/**
	 * @return the checkboxId
	 */
	public int getCheckboxId() {
		return checkboxId;
	}

	/**
	 * @return the iconImageViewId
	 */
	public int getIconImageViewId() {
		return iconImageViewId;
	}

}
