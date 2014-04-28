package aceim.api.dataentity;

import aceim.api.IProtocol;
import aceim.api.dataentity.tkv.TKV;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A {@link ProtocolServiceFeature}, that uses some input form as an action.
 */
public class InputFormFeature extends ProtocolServiceFeature {
	
	private final TKV[] editorFields;
	
	/**
	 * @param featureId feature ID (key) 
	 * @param featureName human-readable feature name
	 * @param iconId feature icon resource ID, for showing in account's/buddy's/group's list of features along with name. Zero value means no icon.
	 * @param showInIconList may this feature be picked by user (false, if you don't plan to process it via {@link IProtocol#setFeature(String, OnlineInfo)})
	 * @param availableOffline should this feature be available to user while offline (for example, to be stored with account, like status or extended status). As of core version 0.9.3, used only for {@link ProtocolServiceFeatureTarget#ACCOUNT} 
	 * @param editorFields input form fields
	 * @param targets a list of targets this feature should be applied to (status set is applied to account, buddy visibility is applied to buddy etc etc)
	 */
	public InputFormFeature(String featureId, String featureName, int iconId, boolean showInIconList, boolean availableOffline, TKV[] editorFields, ProtocolServiceFeatureTarget[] targets) {
		super(featureId, featureName, iconId, showInIconList, true, availableOffline, targets);
		this.editorFields = editorFields;
	}

	public InputFormFeature(Parcel in) {
		super(in);
		Parcelable[] p = in.readParcelableArray(TKV.class.getClassLoader());
		if (p != null){
			this.editorFields = new TKV[p.length];
			for (int i=0; i<p.length; i++) {
				this.editorFields[i] = (TKV) p[i];
			}
		} else {
			this.editorFields = null;
		}
	}
	
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeParcelableArray(editorFields, flags);
	}

	public static final Parcelable.Creator<InputFormFeature> CREATOR = new Parcelable.Creator<InputFormFeature>() {
		public InputFormFeature createFromParcel(Parcel in) {
			//Omitting classname variable used for class hierarchy parcelable support
			in.readString();
			return new InputFormFeature(in);
		}

		public InputFormFeature[] newArray(int size) {
			return new InputFormFeature[size];
		}
	};

	/**
	 * @return the editorFields
	 */
	public TKV[] getEditorFields() {
		return editorFields;
	}
}
