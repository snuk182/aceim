package aceim.api.dataentity.tkv;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File picker {@link TKV}.
 */
public final class FileTKV extends TKV {
	
	/**
	 * File MIME type (better to use one that supported by Android).
	 */
	private final String mimeType;

	public FileTKV(String mimeType, String key, boolean mandatory, String defaultValue) {
		super(key, mandatory, defaultValue);
		this.mimeType = mimeType;
	}

	public FileTKV(Parcel in) {
		super(in);
		this.mimeType = in.readString();
	}
	
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(mimeType);
	}

	public static final Parcelable.Creator<FileTKV> CREATOR = new Parcelable.Creator<FileTKV>() {
		public FileTKV createFromParcel(Parcel in) {
			in.readString();
			return new FileTKV(in);
		}

		public FileTKV[] newArray(int size) {
			return new FileTKV[size];
		}
	};

	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}
}
