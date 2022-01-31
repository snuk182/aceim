package aceim.api.dataentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolService;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Buddy group entity.
 * 
 * @author Sergiy Plygun.
 *
 */
public class BuddyGroup extends Entity implements Parcelable, Comparable<BuddyGroup> {
	
	/**
	 * Group UID
	 */
	private final String id;
	
	/**
	 * Group name
	 */
	private String name;
	
	/**
	 * Group's owner account UID
	 */
	private final String ownerUid;
	
	/**
	 * Flag of group being collapsed in UI, used in core.
	 */
	private boolean isCollapsed = false;
	
	/**
	 * Buddy list of group. Thread-safe access.
	 */
	private final List<Buddy> buddyList = Collections.synchronizedList(new ArrayList<Buddy>());
	
	/**
	 * Group's features set. See {@link ProtocolService#getProtocolFeatures()} for details.
	 */
	private final Bundle features;

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(id);
		dest.writeString(name);
		dest.writeString(ownerUid);
		dest.writeByte((byte) (isCollapsed? 1: 0));
		dest.writeBundle(features);
		dest.writeList(buddyList);	
	}
	
	@SuppressWarnings("unchecked")
	private BuddyGroup(Parcel in){
		super(in);
		id = in.readString();
		name = in.readString();
		ownerUid = in.readString();
		isCollapsed = in.readByte() != 0;
		features = in.readBundle();		
		buddyList.addAll(in.readArrayList(Buddy.class.getClassLoader()));
	}
	
	/**
	 * @param id group UID
	 * @param accountId owner account's protocol UID
	 * @param serviceId owner account's service ID
	 */
	public BuddyGroup(String id, String accountId, Byte serviceId) {
		super(serviceId);
		this.id = id != null ? id : ApiConstants.NO_GROUP_ID;
		this.ownerUid = accountId;
		features = new Bundle();
	}

	public static final Parcelable.Creator<BuddyGroup> CREATOR = new Parcelable.Creator<BuddyGroup>(){

		@Override
		public BuddyGroup createFromParcel(Parcel in) {
			in.readString();
			
			return new BuddyGroup(in);
		}

		@Override
		public BuddyGroup[] newArray(int size) {
			return new BuddyGroup[size];
		}
		
	};
	
	/**
	 * Safe name getter. If no human-readable name found, the empty string is returned.
	 */
	@Override
	public String toString(){
		return name != null ? name : id;
	}

	@Override
	public int compareTo(BuddyGroup another) {
		return id.compareToIgnoreCase(another.id);
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
	 * @return the isCollapsed
	 */
	public boolean isCollapsed() {
		return isCollapsed;
	}

	/**
	 * @param isCollapsed the isCollapsed to set
	 */
	public void setCollapsed(boolean isCollapsed) {
		this.isCollapsed = isCollapsed;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the ownerUid
	 */
	public String getOwnerUid() {
		return ownerUid;
	}

	/**
	 * @return the buddyList
	 */
	public List<Buddy> getBuddyList() {
		return buddyList;
	}

	/**
	 * @return the features
	 */
	public Bundle getFeatures() {
		return features;
	}
}
