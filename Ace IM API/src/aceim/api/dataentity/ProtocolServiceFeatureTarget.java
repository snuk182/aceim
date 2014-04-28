package aceim.api.dataentity;

import aceim.api.service.ApiConstants;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Target type for {@link ProtocolServiceFeature}. 
 */
public enum ProtocolServiceFeatureTarget implements Parcelable {
	/**
	 * Applicable to account (my status, extended status etc)
	 */
	ACCOUNT,
	
	/**
	 * Applicable to buddy (visibility to buddy, buddy's status etc)
	 */
	BUDDY,
	
	/**
	 * Applicable to buddy group (of account, not multi-user chat). Example: {@link ApiConstants#FEATURE_GROUP_MANAGEMENT}
	 */
	GROUP,
	
	/**
	 * Applicable to whole protocol service.
	 */
	PROTOCOL_SERVICE;
	
	private ProtocolServiceFeatureTarget(){}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name());
	}

	public static final Parcelable.Creator<ProtocolServiceFeatureTarget> CREATOR = new Parcelable.Creator<ProtocolServiceFeatureTarget>() {
		public ProtocolServiceFeatureTarget createFromParcel(Parcel in) {
			return ProtocolServiceFeatureTarget.valueOf(in.readString());
		}

		public ProtocolServiceFeatureTarget[] newArray(int size) {
			return new ProtocolServiceFeatureTarget[size];
		}
	};
}
