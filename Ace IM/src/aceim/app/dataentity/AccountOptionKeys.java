package aceim.app.dataentity;

import aceim.app.Constants.OptionKey;
import android.os.Parcel;
import android.os.Parcelable;

public enum AccountOptionKeys implements OptionKey, Parcelable {
	CONTACT_LIST_TYPE,
	SAVE_NOT_IN_LIST,
	LOAD_ICONS,
	SHOW_GROUPS,
	SHOW_OFFLINE,
	DISABLED,
	DENY_MESSAGES_FROM_ALIENS,
	TYPING_NOTIFICATIONS;	
	
	@Override
	public String getStringKey() {
		return toString();
	}

	@Override
	public OptionKey fromStringKey(String key) {
		return AccountOptionKeys.valueOf(key);
	}	

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name());
	}

	public static final Parcelable.Creator<AccountOptionKeys> CREATOR = new Parcelable.Creator<AccountOptionKeys>() {
		public AccountOptionKeys createFromParcel(Parcel in) {
			return AccountOptionKeys.valueOf(in.readString());
		}

		public AccountOptionKeys[] newArray(int size) {
			return new AccountOptionKeys[size];
		}
	};
}
