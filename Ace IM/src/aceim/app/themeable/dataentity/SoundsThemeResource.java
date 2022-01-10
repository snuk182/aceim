package aceim.app.themeable.dataentity;

import android.content.Context;
import android.view.View;

public class SoundsThemeResource extends ThemeResource {

	private final int messageSoundId;
	private final int onlineSoundId;

	public SoundsThemeResource(Context context) {
		super(context, 0);
		
		messageSoundId = context.getResources().getIdentifier("message", "raw", context.getPackageName());
		onlineSoundId = context.getResources().getIdentifier("online", "raw", context.getPackageName());
	}

	/**
	 * @return the messageSoundId
	 */
	public int getMessageSoundId() {
		return messageSoundId;
	}

	/**
	 * @return the onlineSoundId
	 */
	public int getOnlineSoundId() {
		return onlineSoundId;
	}
	
	@Override
	public View getView() {
		throw new IllegalStateException("No views available in SoundsThemeResource");
	}
}
