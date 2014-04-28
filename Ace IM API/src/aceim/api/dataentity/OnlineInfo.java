package aceim.api.dataentity;

import java.util.Date;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Online info entity. Carries buddy/account online info update.
 * 
 * @author Sergiy Plygun
 *
 */
public class OnlineInfo extends Entity implements Parcelable {
	
	/**
	 * A set of {@link ProtocolServiceFeature}
	 */
	private final Bundle features;
	
	/**
	 * Extended status name.
	 */
	private String xstatusName = null;
	
	/**
	 * Extended status description.
	 */
	private String xstatusDescription = null;
	
	/**
	 * Owner's IP address.
	 */
	private String extIP;
	
	/**
	 * Owners's time of being online.
	 */
	private long onlineTime;
	
	/**
	 * Owner's moment being joined to protocol network.
	 */
	private long memberSinceTime;
	
	/**
	 * Owner's inactivity time.
	 */
	private int idleTime;
	
	/**
	 * Owner name.
	 */
	private String name;
	
	/**
	 * Owner's protocol UID
	 */
	private final String protocolUid;
	
	/**
	 * Owner's icon hash.
	 */
	private String iconHash;
	
	public static final Parcelable.Creator<OnlineInfo> CREATOR = new Parcelable.Creator<OnlineInfo>(){

		@Override
		public OnlineInfo createFromParcel(Parcel source) {
			//Omitting classname variable used for class hierarchy parcelable support
			source.readString();
			return new OnlineInfo(source);
		}

		@Override
		public OnlineInfo[] newArray(int size) {
			return new OnlineInfo[size];
		}
		
	};
	
	protected OnlineInfo(Parcel in) {
		super(in);
		extIP = in.readString();
		onlineTime = in.readLong();
		memberSinceTime = in.readLong();
		idleTime = in.readInt();
		name = in.readString();
		protocolUid = in.readString();
		iconHash = in.readString();
		xstatusName = in.readString();
		xstatusDescription = in.readString();
		features = in.readBundle();
		
		features.setClassLoader(Entity.class.getClassLoader());
	}

	public OnlineInfo(byte serviceId, String protocolUid) {
		super(serviceId);
		this.protocolUid = protocolUid;
		this.features = new Bundle();
		
		features.setClassLoader(Entity.class.getClassLoader());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(extIP);
		dest.writeLong(onlineTime);
		dest.writeLong(memberSinceTime);
		dest.writeInt(idleTime);
		dest.writeString(name);
		dest.writeString(protocolUid);
		dest.writeString(iconHash);
		dest.writeString(xstatusName);
		dest.writeString(xstatusDescription);
		dest.writeBundle(features);
	}
	
	public void merge(OnlineInfo info) {
		if (info == null || info.getServiceId() != getServiceId())
			return;

		setExtIP(info.getExtIP());
		setIconHash(info.getIconHash());
		setIdleTime(info.getIdleTime());
		setMemberSinceTime(info.getMemberSinceTime());
		setName(info.getName());
		setOnlineTime(info.getOnlineTime());
		setXstatusDescription(info.getXstatusDescription());
		setXstatusName(info.getXstatusName());
		getFeatures().clear();
		getFeatures().putAll(info.getFeatures());
	}

	/**
	 * @return the xstatusName
	 */
	public String getXstatusName() {
		return xstatusName;
	}

	/**
	 * @param xstatusName the xstatusName to set
	 */
	public void setXstatusName(String xstatusName) {
		this.xstatusName = xstatusName;
	}

	/**
	 * @return the xstatusDescription
	 */
	public String getXstatusDescription() {
		return xstatusDescription;
	}

	/**
	 * @param xstatusDescription the xstatusDescription to set
	 */
	public void setXstatusDescription(String xstatusDescription) {
		this.xstatusDescription = xstatusDescription;
	}

	/**
	 * @return the extIP
	 */
	public String getExtIP() {
		return extIP;
	}

	/**
	 * @param extIP the extIP to set
	 */
	public void setExtIP(String extIP) {
		this.extIP = extIP;
	}

	/**
	 * @return the onlineTime
	 */
	public long getOnlineTime() {
		return onlineTime;
	}

	/**
	 * @param onlineTime the onlineTime to set
	 */
	public void setOnlineTime(long onlineTime) {
		this.onlineTime = onlineTime;
	}

	/**
	 * @return the memberSinceTime
	 */
	public Date getMemberSinceTime() {
		if (memberSinceTime != 0) {
			return new Date(memberSinceTime);
		} else {
			return null;
		}
	}

	/**
	 * @param memberSinceTime the memberSinceTime to set
	 */
	public void setMemberSinceTime(Date memberSinceTime) {
		if (memberSinceTime != null) {
			this.memberSinceTime = memberSinceTime.getTime();
		} else {
			this.memberSinceTime = 0;
		}
	}

	/**
	 * @return the idleTime
	 */
	public int getIdleTime() {
		return idleTime;
	}

	/**
	 * @param idleTime the idleTime to set
	 */
	public void setIdleTime(int idleTime) {
		this.idleTime = idleTime;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the protocolUid
	 */
	public String getProtocolUid() {
		return protocolUid;
	}

	/**
	 * @return the iconHash
	 */
	public String getIconHash() {
		return iconHash;
	}

	/**
	 * @param iconHash the iconHash to set
	 */
	public void setIconHash(String iconHash) {
		this.iconHash = iconHash;
	}

	/**
	 * @return the features
	 */
	public Bundle getFeatures() {
		return features;
	}

	
}
