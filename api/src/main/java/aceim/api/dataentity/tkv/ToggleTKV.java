package aceim.api.dataentity.tkv;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A {@link TKV}, which value can be only set to YES or NO.
 */
public final class ToggleTKV extends TKV {

	public ToggleTKV(String key, boolean mandatory, boolean defaultValue) {
		super(key, mandatory, Boolean.toString(defaultValue));
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
	}

	public static final Parcelable.Creator<ToggleTKV> CREATOR = new Parcelable.Creator<ToggleTKV>() {
		public ToggleTKV createFromParcel(Parcel in) {
			in.readString();
			return new ToggleTKV(in);
		}

		public ToggleTKV[] newArray(int size) {
			return new ToggleTKV[size];
		}
	};

	public ToggleTKV(Parcel in) {
		super(in);
	}

}
