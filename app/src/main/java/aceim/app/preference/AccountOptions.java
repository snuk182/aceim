package aceim.app.preference;

import aceim.app.Constants;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountOptionKeys;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;

public class AccountOptions extends OptionsPage {
	
	private final Account mAccount;
	
	public AccountOptions(Account a) {
		super(R.xml.account_preferences, a.getAccountId());
		this.mAccount = a;
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_preferences);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.options_for, mAccount.getSafeName());
	}

	@Override
	void onPreferenceAttached(PreferenceScreen root, int xmlId) {
		onPreferenceAttached(root, xmlId, AccountOptionKeys.values());
	}
	
	@Override
	protected boolean onPreferenceChangeInternal(Preference p, Object newValue){
		Intent i = new Intent();
		i.putExtra(Constants.INTENT_EXTRA_SERVICE_ID, mAccount.getServiceId());
		i.putExtra(Constants.INTENT_EXTRA_OPTION_VALUE, newValue.toString());
		i.putExtra(Constants.INTENT_EXTRA_OPTION_KEY, (Parcelable)AccountOptionKeys.valueOf(p.getKey()));
		i.setAction(Constants.INTENT_ACTION_OPTION);
		
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
		
		return true;
	}
}
