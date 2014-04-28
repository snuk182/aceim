package aceim.api.dataentity;

import aceim.api.service.ApiConstants;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An {@link InputFormFeature} which acts only as a marker for core ({@link ApiConstants#FEATURE_BUDDY_MANAGEMENT}, {@link ApiConstants#FEATURE_FILE_TRANSFER} etc).
 */
public class MarkerFeature extends ProtocolServiceFeature {

	public MarkerFeature(String featureId, String featureName, int iconId, boolean showInIconList, boolean availableOffline, ProtocolServiceFeatureTarget[] targets) {
		super(featureId, featureName, iconId, showInIconList, false, availableOffline, targets);
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
	}

	public static final Parcelable.Creator<MarkerFeature> CREATOR = new Parcelable.Creator<MarkerFeature>() {
		public MarkerFeature createFromParcel(Parcel in) {
			in.readString();
			return new MarkerFeature(in);
		}

		public MarkerFeature[] newArray(int size) {
			return new MarkerFeature[size];
		}
	};

	public MarkerFeature(Parcel in) {
		super(in);
	}
}
