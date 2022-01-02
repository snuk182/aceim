package aceim.app.dataentity;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class ActivityResult implements Parcelable {
	
	private final int requestCode;
	private final int resultCode;
	private final Intent data;

	public ActivityResult(int requestCode, int resultCode, Intent data) {		
		this.requestCode = requestCode;
		this.resultCode = resultCode;
		this.data = data;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(requestCode);
		out.writeInt(resultCode);
		out.writeParcelable(data, flags);
	}

	public static final Parcelable.Creator<ActivityResult> CREATOR = new Parcelable.Creator<ActivityResult>() {
		public ActivityResult createFromParcel(Parcel in) {
			return new ActivityResult(in);
		}

		public ActivityResult[] newArray(int size) {
			return new ActivityResult[size];
		}
	};

	private ActivityResult(Parcel in) {
		this.requestCode = in.readInt();
		this.resultCode = in.readInt();
		this.data = in.readParcelable(Intent.class.getClassLoader());
	}

	/**
	 * @return the requestCode
	 */
	public int getRequestCode() {
		return requestCode;
	}

	/**
	 * @return the resultCode
	 */
	public int getResultCode() {
		return resultCode;
	}

	/**
	 * @return the data
	 */
	public Intent getData() {
		return data;
	}
}
