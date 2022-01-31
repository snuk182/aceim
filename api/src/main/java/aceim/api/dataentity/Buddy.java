package aceim.api.dataentity;

import java.util.Comparator;

import aceim.api.service.ApiConstants;
import aceim.api.utils.Utils;
import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Buddy entity.
 * 
 * @author Sergiy Plygun
 *
 */
public class Buddy extends Entity implements EntityWithID, Comparable<Buddy> {

	/**
	 * Buddy ID (rarely used, but required for some protocols)
	 */
	private int id;	
	
	/**
	 * Buddy's protocol service name
	 */
	private final String serviceName;	
	
	/**
	 * Buddy's name (nick)
	 */
	private String name;	
	
	/**
	 * Buddy's protocol UID (very important!)
	 */
	private final String protocolUid;	
	
	/**
	 * Buddy's owner account protocol UID (very important!)
	 */
	private final String ownerUid;	
	
	/**
	 * The number of unread messages buddy have. Used mostly within core.
	 */
	private byte unread = 0;	
	
	/**
	 * Buddy's group UID.
	 */
	private String groupId = ApiConstants.NO_GROUP_ID;	
	
	/**
	 * Buddy's online info (status, features etc)
	 */
	private final OnlineInfo onlineInfo;
	
	public String getSafeName() {
		return name!=null ? name : protocolUid;
	}
	
	public Buddy(Parcel in){
		super(in);
		id = in.readInt();
		serviceName = in.readString();
		name = in.readString();
		protocolUid = in.readString();
		ownerUid = in.readString();
		unread = in.readByte();
		groupId = in.readString();
		onlineInfo = in.readParcelable(OnlineInfo.class.getClassLoader());
	}
	
	/**
	 * @param protocolUid buddy's protocol UID
	 * @param ownerUid owner account's protocol UID
	 * @param serviceName protocol service name
	 * @param serviceId owner account service ID
	 */
	public Buddy(String protocolUid, String ownerUid, String serviceName, byte serviceId) {
		super(serviceId);
		this.protocolUid = protocolUid;
		this.ownerUid = ownerUid;
		this.serviceName = serviceName;
		this.onlineInfo = new OnlineInfo(serviceId, protocolUid);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(id);
		dest.writeString(serviceName);
		dest.writeString(name);
		dest.writeString(protocolUid);
		dest.writeString(ownerUid);
		dest.writeByte(unread);
		dest.writeString(groupId);
		dest.writeParcelable(onlineInfo, flags);
	}
	
	public static final Parcelable.Creator<Buddy> CREATOR = new Parcelable.Creator<Buddy>(){

		@Override
		public Buddy createFromParcel(Parcel in) {
			return Utils.unparcelEntity(in, Buddy.class);
		}

		@Override
		public Buddy[] newArray(int size) {
			return new Buddy[size];
		}
		
	};

	/**
	 * Merge buddy with new data.
	 * 
	 * @param origin the new data holder.
	 */
	public void merge(Buddy origin){
		if (origin == null || origin == this){
			return;
		}
		
		name = origin.name;
		unread = origin.unread;
		groupId = origin.groupId;
		id = origin.id;
		
		onlineInfo.merge(origin.onlineInfo);
	}
	
	/**
	 * Returns holder account id, in form of "123456789 ICQ"
	 * 
	 * @see Account#getAccountId()
	 * @return id
	 */
	public String getOwnerAccountId(){
		return Utils.escapeGeneralDividers(ownerUid) + ApiConstants.GENERAL_DIVIDER + Utils.escapeGeneralDividers(serviceName);
	}
	
	/**
	 * Comparator. First checks status, then name, ignoring case.
	 * 
	 * @see Comparator
	 */
	@Override
	public int compareTo(Buddy another) {
		if (onlineInfo.getFeatures().containsKey(ApiConstants.FEATURE_STATUS) && another.getOnlineInfo().getFeatures().containsKey(ApiConstants.FEATURE_STATUS)) {
			byte myStatus = onlineInfo.getFeatures().getByte(ApiConstants.FEATURE_STATUS);
			byte hisStatus = another.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS);
			
			if (myStatus != hisStatus) {
				return hisStatus - myStatus;
			}
		} else if (onlineInfo.getFeatures().containsKey(ApiConstants.FEATURE_STATUS)) {
			return -1;
		} else if (another.getOnlineInfo().getFeatures().containsKey(ApiConstants.FEATURE_STATUS)) {
			return 1;
		}
		
		return getSafeName().compareToIgnoreCase(another.getSafeName());
	}

	/**
	 * Obtain filename for buddy's additional data (history, icon etc) 
	 * 
	 * @return
	 */
	public String getFilename() {
		return getOwnerAccountId() + ApiConstants.GENERAL_DIVIDER + Utils.escapeGeneralDividers(protocolUid);
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
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
	 * @return the unread
	 */
	public byte getUnread() {
		return unread;
	}

	/**
	 * @param unread the unread to set
	 */
	public void setUnread(byte unread) {
		this.unread = unread;
	}
	
	public void incrementUnread() {
		this.unread++;
	}

	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @return the protocolUid
	 */
	public String getProtocolUid() {
		return protocolUid;
	}

	/**
	 * @return the ownerUid
	 */
	public String getOwnerUid() {
		return ownerUid;
	}

	/**
	 * @return the onlineInfo
	 */
	public OnlineInfo getOnlineInfo() {
		return onlineInfo;
	}

	@Override
	public String getEntityId() {
		return getFilename();
	}
}
