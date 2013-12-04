package aceim.app.dataentity;

import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.utils.Logger;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

public class ProtocolResources extends PluginResources {

	private String protocolServiceName;
	private String protocolName;
	private String protocolVersion;
	private String protocolInfo;
	private String apiVersion;
	
	private ProtocolServiceFeature[] mFeatures;

	public ProtocolResources(ProtocolService service) {

		super(service.getProtocolServicePackageName(), null);
		
		fillResources(service);
	}

	private void fillResources(ProtocolService service) {
		try {
			Resources r = getNativeResourcesForProtocol(service.getContext().getPackageManager());
			
			mFeatures = service.getProtocol().getProtocolFeatures();
			protocolName = service.getProtocol().getProtocolName();
			
			this.protocolServiceName = r.getString(service.getServiceInfo().applicationInfo.labelRes);
		} catch (Exception e) {
			Logger.log(e);
			this.protocolServiceName = service.getServiceClassName();
		}
	}	
	
	public ProtocolResources(Parcel in) {
		super(in);
		this.protocolName = in.readString();
		this.protocolServiceName = in.readString();
		this.protocolInfo = in.readString();
		this.protocolVersion = in.readString();
		this.apiVersion = in.readString();
		this.mFeatures = (ProtocolServiceFeature[]) in.readParcelableArray(ProtocolServiceFeature.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(protocolName);
		dest.writeString(protocolServiceName);
		dest.writeString(protocolInfo);
		dest.writeString(protocolVersion);
		dest.writeString(apiVersion);
		dest.writeParcelableArray(mFeatures, flags);
	}

	public static final Parcelable.Creator<ProtocolResources> CREATOR = new Parcelable.Creator<ProtocolResources>() {
		public ProtocolResources createFromParcel(Parcel in) {
			in.readString();
			return new ProtocolResources(in);
		}

		public ProtocolResources[] newArray(int size) {
			return new ProtocolResources[size];
		}
	};

	public int getProtocolServiceIconId() {
		return 0;
	}

	public String getProtocolServicePackageName() {
		return getPackageName();
	}
	
	/**
	 * @return the protocolName
	 */
	public String getProtocolName() {
		return protocolName;
	}
	
	@Override
	public String toString() {
		return protocolServiceName;
	}

	public ProtocolServiceFeature[] getFeatures() {
		return mFeatures;
	}

	public ProtocolServiceFeature getFeature(String featureId){
		if (mFeatures == null || mFeatures.length < 1 || featureId == null) {
			return null;
		}
		
		for (ProtocolServiceFeature feature : mFeatures) {
			if (feature.getFeatureId().equals(featureId)) {
				return feature;
			}
		}
		
		return null;
	}

	public ProtocolServiceFeature getFeature(int featureIdHash) {
		if (mFeatures == null || mFeatures.length < 1 || featureIdHash == 0) {
			return null;
		}
		
		for (ProtocolServiceFeature feature : mFeatures) {
			if (feature.getFeatureId().hashCode() == featureIdHash) {
				return feature;
			}
		}
		
		return null;
	}

	/**
	 * @return the protocolVersion
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * @param protocolVersion the protocolVersion to set
	 */
	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	/**
	 * @return the protocolInfo
	 */
	public String getProtocolInfo() {
		return protocolInfo;
	}

	/**
	 * @param protocolInfo the protocolInfo to set
	 */
	public void setProtocolInfo(String protocolInfo) {
		this.protocolInfo = protocolInfo;
	}

	/**
	 * @return the apiVersion
	 */
	public String getApiVersion() {
		return apiVersion;
	}

	/**
	 * @param apiVersion the apiVersion to set
	 */
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
}
