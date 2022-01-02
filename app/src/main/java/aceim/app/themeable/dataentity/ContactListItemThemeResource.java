package aceim.app.themeable.dataentity;

import android.content.Context;

public abstract class ContactListItemThemeResource extends ContactThemeResource {

	protected ContactListItemThemeResource(Context context, int id) {
		super(context, id);

		setIconImageId(getContext().getResources().getIdentifier("image_icon", "id", getContext().getPackageName()));
		setBuddyStatusImageId(getContext().getResources().getIdentifier("image_status", "id", getContext().getPackageName()));
		setTitleTextViewId(getContext().getResources().getIdentifier("username", "id", getContext().getPackageName()));
		setXstatusTextViewId(getContext().getResources().getIdentifier("label_xstatus", "id", getContext().getPackageName()));
		
		int[] cache = new int[10];
		int i=0;
		while (i < cache.length) {
			int iid = getContext().getResources().getIdentifier("image_extra_" + (i + 1), "id", getContext().getPackageName());
			
			if (iid != 0) {
				cache[i] = iid;
				i++;
			} else {
				break;
			}
		}
		
		setExtraImageIDs(cache);
	}
}
