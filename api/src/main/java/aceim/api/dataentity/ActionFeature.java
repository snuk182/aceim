package aceim.api.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A feature designed to fire some action on protocol's side, that does not require additional parameters, by user request. For example, obtain all available chat rooms.
 */
public class ActionFeature extends ProtocolServiceFeature {
	
	/**
	 * See {@link ProtocolServiceFeature#ProtocolServiceFeature(String, String, int, boolean, boolean, boolean, ProtocolServiceFeatureTarget[])} for parameter details.
	 */
	public ActionFeature(String featureId, String featureName, int iconId, boolean showInIconList, boolean availableOffline, ProtocolServiceFeatureTarget[] targets) {
		super(featureId, featureName, iconId, showInIconList, true, availableOffline, targets);
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
	}

	public static final Parcelable.Creator<ActionFeature> CREATOR = new Parcelable.Creator<ActionFeature>() {
		public ActionFeature createFromParcel(Parcel in) {
			in.readString();
			return new ActionFeature(in);
		}

		public ActionFeature[] newArray(int size) {
			return new ActionFeature[size];
		}
	};

	public ActionFeature(Parcel in) {
		super(in);
	}

}
