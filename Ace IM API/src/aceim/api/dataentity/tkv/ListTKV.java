package aceim.api.dataentity.tkv;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * List item picker {@link TKV} (text strings only).
 */
public final class ListTKV extends TKV {
	
	/**
	 * Choices list.
	 */
	private final String[] choices;

	public ListTKV(String[] choices, String key, boolean mandatory, String defaultValue) {
		super(key, mandatory, defaultValue);
		this.choices = choices;
	}

	public ListTKV(Parcel in) {
		super(in);
		this.choices = in.createStringArray();
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeStringArray(choices);
	}

	public static final Parcelable.Creator<ListTKV> CREATOR = new Parcelable.Creator<ListTKV>() {
		public ListTKV createFromParcel(Parcel in) {
			in.readString();
			return new ListTKV(in);
		}

		public ListTKV[] newArray(int size) {
			return new ListTKV[size];
		}
	};

	/**
	 * @return the choices
	 */
	public String[] getChoices() {
		return choices;
	}
}
