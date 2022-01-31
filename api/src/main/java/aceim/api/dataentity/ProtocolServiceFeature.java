package aceim.api.dataentity;

import java.lang.reflect.Constructor;

import aceim.api.IProtocol;
import aceim.api.utils.Logger;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Base class for different types of protocol service features.
 */
public abstract class ProtocolServiceFeature implements Parcelable {
	
	private final String featureId;
	private final String featureName;
	private final int iconId;
	private final boolean showInIconList;
	private final boolean editable;
	private final boolean availableOffline;
	private final ProtocolServiceFeatureTarget[] targets;

	/**
	 * @param featureId feature ID (key) 
	 * @param featureName human-readable feature name
	 * @param iconId feature icon resource ID, for showing in account's/buddy's/group's list of features along with name. Zero value means no icon.
	 * @param showInIconList may this feature be picked by user (false, if you don't plan to process it via {@link IProtocol#setFeature(String, OnlineInfo)})
	 * @param availableOffline should this feature be available to user while offline (for example, to be stored with account, like status or extended status). As of core version 0.9.3, used only for {@link ProtocolServiceFeatureTarget#ACCOUNT} 
	 * @param targets a list of targets this feature should be applied to (status set is applied to account, buddy visibility is applied to buddy etc etc)
	 */
	protected ProtocolServiceFeature(String featureId, String featureName, int iconId, boolean showInIconList, boolean editable, boolean availableOffline, ProtocolServiceFeatureTarget[] targets) {
		this.featureId = featureId;
		this.featureName = featureName;
		this.targets = targets;
		this.iconId = iconId;
		this.showInIconList = showInIconList;
		this.availableOffline = availableOffline;
		this.editable = editable;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		// The trick to support inheritance.
		out.writeString(getClass().getName());
		
		out.writeString(featureId);
		out.writeString(featureName);
		out.writeInt(iconId);
		out.writeByte((byte) (showInIconList ? 1 : 0));
		out.writeByte((byte) (editable ? 1 : 0));
		out.writeByte((byte) (availableOffline ? 1 : 0));
		out.writeParcelableArray(targets, flags);
	}

	public static final Parcelable.Creator<ProtocolServiceFeature> CREATOR = new Parcelable.Creator<ProtocolServiceFeature>() {
		@SuppressWarnings("unchecked")
		public ProtocolServiceFeature createFromParcel(Parcel in) {
			String className = in.readString();
			try {
				Class<? extends ProtocolServiceFeature> cls = (Class<? extends ProtocolServiceFeature>) Class.forName(className);
				Class<?>[] paramTypes = { Parcel.class }; 
				Constructor<? extends ProtocolServiceFeature> constructor = cls.getConstructor(paramTypes);
				return constructor.newInstance(in);
			} catch (Exception e) {
				Logger.log(e);
			}
			
			return null;
		}

		public ProtocolServiceFeature[] newArray(int size) {
			return new ProtocolServiceFeature[size];
		}
	};

	protected ProtocolServiceFeature(Parcel in) {
		featureId = in.readString();
		featureName = in.readString();
		iconId = in.readInt();
		showInIconList = in.readByte() > 0;
		editable = in.readByte() > 0;
		availableOffline = in.readByte() > 0;
		Parcelable[] p = in.readParcelableArray(ProtocolServiceFeatureTarget.class.getClassLoader());
		
		if (p != null) {
			targets = new ProtocolServiceFeatureTarget[p.length];
			
			for (int i=0; i<p.length; i++) {
				Parcelable pp = p[i];
				targets[i] = (ProtocolServiceFeatureTarget) pp;
			}
		} else {
			targets = null;
		}
	}

	/**
	 * @return the editable
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * @return the availableOffline
	 */
	public boolean isAvailableOffline() {
		return availableOffline;
	}

	/**
	 * @return the featureId
	 */
	public String getFeatureId() {
		return featureId;
	}

	/**
	 * @return the featureName
	 */
	public String getFeatureName() {
		return featureName;
	}

	public boolean isAppliedToTarget(ProtocolServiceFeatureTarget target) {
		if (targets == null) {
			return true;
		}
		for (ProtocolServiceFeatureTarget ptarget : targets){
			if (ptarget == target) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * @return the iconId
	 */
	public int getIconId() {
		return iconId;
	}

	/**
	 * @return the showInIconList
	 */
	public boolean isShowInIconList() {
		return showInIconList;
	}
}
