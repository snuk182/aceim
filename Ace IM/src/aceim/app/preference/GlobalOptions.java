package aceim.app.preference;

import aceim.app.Constants;
import aceim.app.R;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.utils.OptionsReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;

public class GlobalOptions extends OptionsPage  {
	
	public GlobalOptions() {
		super(R.xml.global_preferences, Constants.SHARED_PREFERENCES_GLOBAL);
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_preferences);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.general_options);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return onPreferenceChangeInternal(preference, newValue);
	}

	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
		onPreferenceAttached(root, xmlId, GlobalOptionKeys.values());
	}
	
	private boolean onPreferenceChangeInternal(Preference p, Object newValue){
		Intent i = new Intent(getActivity(), OptionsReceiver.class);
		i.putExtra(Constants.INTENT_EXTRA_OPTION_VALUE, newValue.toString());
		i.putExtra(Constants.INTENT_EXTRA_OPTION_KEY, (Parcelable)GlobalOptionKeys.valueOf(p.getKey()));
		i.setAction(Constants.INTENT_ACTION_OPTION);
		
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
		
		return true;
	}
}
