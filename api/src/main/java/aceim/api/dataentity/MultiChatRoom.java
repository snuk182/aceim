package aceim.api.dataentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Multi-user chat room entity. Recognized as {@link Buddy}.
 */
public class MultiChatRoom extends Buddy{

	/**
	 * Chat participants, grouped. Thread-safe.
	 */
	private final List<BuddyGroup> groups = Collections.synchronizedList(new ArrayList<BuddyGroup>());
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeList(groups);
	}
	
	@SuppressWarnings("unchecked")
	public MultiChatRoom(Parcel in){
		super(in);
		groups.addAll(in.readArrayList(BuddyGroup.class.getClassLoader()));
	}
	
	public MultiChatRoom(String protocolUid, String ownerUid, String serviceName, byte serviceId) {
		super(protocolUid, ownerUid, serviceName, serviceId);
	}

	public static final Parcelable.Creator<MultiChatRoom> CREATOR = new Parcelable.Creator<MultiChatRoom>(){

		@Override
		public MultiChatRoom createFromParcel(Parcel source) {
			//Omitting classname variable used for class hierarchy parcelable support
			source.readString();
			return new MultiChatRoom(source);
		}

		@Override
		public MultiChatRoom[] newArray(int size) {
			return new MultiChatRoom[size];
		}
		
	};

	/**
	 * @return the groups
	 */
	public List<BuddyGroup> getOccupants() {
		return groups;
	}
	
	public Buddy findOccupantByUid(String uid) {
		if (uid != null) {
			synchronized (groups) {
				for (BuddyGroup group : groups) {
					synchronized (group.getBuddyList()) {
						for (Buddy occupant : group.getBuddyList()) {
							if (uid.equals(occupant.getProtocolUid())) {
								return occupant;
							}
						}
					}
				}
			}
		}	
		
		return null;
	}
	
	@Override
	public void merge(Buddy origin){
		super.merge(origin);
		
		if (origin != this && origin instanceof MultiChatRoom && origin.getProtocolUid().equals(getProtocolUid())) {
			synchronized (groups) {
				groups.clear();
				groups.addAll(((MultiChatRoom)origin).getOccupants());
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MultiChatRoom [groups=" + groups + "\\n]";
	}
}
