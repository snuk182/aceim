package aceim.api.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A {@link ProtocolServiceFeature}, which value can be only set to YES or NO
 */
public class ToggleFeature extends ProtocolServiceFeature {
	
	/**
	 * Feature value.
	 */
	private boolean value;
	
	public ToggleFeature(String featureId, String featureName, int iconId, boolean showInIconList, boolean editable, boolean availableOffline, ProtocolServiceFeatureTarget[] targets, boolean defaultValue) {
		super(featureId, featureName, iconId, showInIconList, editable, availableOffline, targets);
		this.value = defaultValue;
	}

	public ToggleFeature(Parcel in) {
		super(in);
		value = in.readByte() > 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeByte((byte) (value ? 1 : 0));
	}

	public static final Parcelable.Creator<ToggleFeature> CREATOR = new Parcelable.Creator<ToggleFeature>() {
		public ToggleFeature createFromParcel(Parcel in) {
			in.readString();
			return new ToggleFeature(in);
		}

		public ToggleFeature[] newArray(int size) {
			return new ToggleFeature[size];
		}
	};

	/**
	 * @return the value
	 */
	public boolean getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(boolean value) {
		this.value = value;
	}
}
