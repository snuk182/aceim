package aceim.app.preference;

import aceim.app.Constants.OptionKey;
import aceim.app.R;
import aceim.app.widgets.preference.EditablePasswordPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

public abstract class OptionsPage extends OptionsFragment {

	public OptionsPage(int xmlId, String sharedPreferencesName) {
		super(xmlId, sharedPreferencesName);
	}

	protected void onPreferenceAttached(PreferenceScreen root, int xmlId, OptionKey[] keys) {
		for (OptionKey k : keys) {
			Preference p = findPreference(k.getStringKey());
			if (p != null) {
				p.setOnPreferenceClickListener(this);
				p.setOnPreferenceChangeListener(this);

				if (p instanceof ListPreference) {
					ListPreference listPref = (ListPreference) p;
					p.setSummary(listPref.getEntry());
				} else if (p instanceof EditablePasswordPreference) {
					EditablePasswordPreference ePref = (EditablePasswordPreference) p;
					p.setSummary(ePref.getText() != null && ePref.getText().length() > 0 ? R.string.yes : R.string.no);
				}
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}
}
