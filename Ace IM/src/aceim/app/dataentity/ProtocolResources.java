package aceim.app.dataentity;

import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.AceImException.AceImExceptionReason;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

public class ProtocolResources implements Parcelable {

	private final String protocolServicePackageName;
	private String protocolServiceName;
	private String protocolName;
	
	private Resources nativeProtocolResources;

	private ProtocolServiceFeature[] mFeatures;

	public ProtocolResources(ProtocolService service) {

		this.protocolServicePackageName = service.getProtocolServicePackageName();
		
		fillResources(service);
	}

	private void fillResources(ProtocolService service) {
		try {
			nativeProtocolResources = getNativeResourcesForProtocol(service.getContext().getPackageManager());
			
			mFeatures = service.getProtocol().getProtocolFeatures();
			protocolName = service.getProtocol().getProtocolName();
			
			this.protocolServiceName = nativeProtocolResources.getString(service.getServiceInfo().applicationInfo.labelRes);
		} catch (Exception e) {
			Logger.log(e);
			this.protocolServiceName = service.getServiceInfo().name;
		}
	}	
	
	public ProtocolResources(Parcel in) {
		this.protocolServicePackageName = in.readString();
		this.protocolServiceName = in.readString();
		this.mFeatures = (ProtocolServiceFeature[]) in.readParcelableArray(ProtocolServiceFeature.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(protocolServicePackageName);
		dest.writeString(protocolServiceName);
		dest.writeParcelableArray(mFeatures, flags);
	}

	public static final Parcelable.Creator<ProtocolResources> CREATOR = new Parcelable.Creator<ProtocolResources>() {
		public ProtocolResources createFromParcel(Parcel in) {
			return new ProtocolResources(in);
		}

		public ProtocolResources[] newArray(int size) {
			return new ProtocolResources[size];
		}
	};

	public Resources getNativeResourcesForProtocol(PackageManager packageManager) throws AceImException {
		if (nativeProtocolResources == null) {
			if (packageManager != null) {
				ApplicationInfo info;
				try {
					info = packageManager.getApplicationInfo(protocolServicePackageName, 0);
					nativeProtocolResources = packageManager.getResourcesForApplication(info);
				} catch (NameNotFoundException e) {
					throw new AceImException(e, AceImExceptionReason.EXCEPTION);
				}
			} else {
				throw new AceImException(AceImExceptionReason.RESOURCE_NOT_INITIALIZED);
			}
		}
		return nativeProtocolResources;
	}

	public int getProtocolServiceIconId() {
		return 0;
	}

	public String getProtocolServicePackageName() {
		return protocolServicePackageName;
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
}
