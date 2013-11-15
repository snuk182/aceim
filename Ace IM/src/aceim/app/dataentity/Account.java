package aceim.app.dataentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.Entity;
import aceim.api.dataentity.EntityWithID;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Account view part, for using in views.
 * 
 * @author Sergiy Plygun
 * 
 */
public class Account extends Entity implements EntityWithID {

	private final List<Buddy> noGroupBuddies = Collections.synchronizedList(new ArrayList<Buddy>());

	private final ReentrantReadWriteLock buddyGroupLock = new ReentrantReadWriteLock(true);
	private final ReadLock readLock = buddyGroupLock.readLock();
	private final WriteLock writeLock = buddyGroupLock.writeLock();

	/**
	 * "Account enabled" flag
	 */
	private boolean isEnabled = true;

	/**
	 * Protocol name (ICQ, XMPP etc)
	 */
	private final String protocolName;

	/**
	 * Protocol service name, which processes this account
	 */
	private final String protocolServicePackageName;

	/**
	 * Protocol-specific identifier (444555666 for ICQ, user@server.com for XMPP
	 * and so on)
	 */
	private final String protocolUid;

	private final OnlineInfo onlineInfo;

	/**
	 * Buddy group list.
	 */
	private final List<BuddyGroup> buddyGroupList = Collections.synchronizedList(new ArrayList<BuddyGroup>());

	/**
	 * Buddies with unread messages temporary storage. Buddy uid - number of
	 * unread messages. Non-serializable.
	 */
	private Map<String, Byte> unreadsMap = new HashMap<String, Byte>();

	/**
	 * Undeletable buddies temporary storage. Non-serializable.
	 */
	private List<Buddy> undeletable;

	/**
	 * Account connection state.
	 */
	private ConnectionState connectionState = ConnectionState.DISCONNECTED;

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(protocolServicePackageName);
		dest.writeString(protocolName);
		dest.writeString(protocolUid);
		dest.writeParcelable(onlineInfo, flags);
		dest.writeList(buddyGroupList);
		dest.writeList(noGroupBuddies);
		dest.writeParcelable(connectionState, flags);
		dest.writeByte((byte) (isEnabled ? 1 : 0));
	}

	public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {

		@Override
		public Account createFromParcel(Parcel source) {
			// Omitting classname variable used for class hierarchy parcelable support
			source.readString();
			return new Account(source);
		}

		@Override
		public Account[] newArray(int size) {
			return new Account[size];
		}

	};

	@SuppressWarnings("unchecked")
	private Account(Parcel in) {
		super(in);
		protocolServicePackageName = in.readString();
		protocolName = in.readString();
		protocolUid = in.readString();
		onlineInfo = in.readParcelable(OnlineInfo.class.getClassLoader());
		buddyGroupList.clear();
		buddyGroupList.addAll(in.readArrayList(BuddyGroup.class.getClassLoader()));
		noGroupBuddies.clear();
		noGroupBuddies.addAll(in.readArrayList(Buddy.class.getClassLoader()));
		connectionState = in.readParcelable(ConnectionState.class.getClassLoader());
		isEnabled = in.readByte() != 0;
	}

	public Account(byte serviceId, String protocolUid, String protocolName, String protocolServiceClassName) {
		super(serviceId);
		this.protocolUid = protocolUid;
		this.protocolName = protocolName;
		this.protocolServicePackageName = protocolServiceClassName;
		this.onlineInfo = new OnlineInfo(serviceId, protocolUid);
	}

	/**
	 * Find buddy by protocol uid in this account.
	 * 
	 * @param uid
	 *            input uid
	 * @return buddy or null
	 */
	public Buddy getBuddyByProtocolUid(String uid) {
		try {
			readLock.lock();

			for (Buddy buddy : getBuddyList()) {
				if (uid.equals(buddy.getProtocolUid())) {
					return buddy;
				}
			}

			return null;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Find buddy group by protocol uid in this account.
	 * 
	 * @param id
	 *            group id to find
	 * @return group or null
	 */
	public BuddyGroup getBuddyGroupByGroupId(String id) {
		try {
			readLock.lock();

			for (BuddyGroup group : buddyGroupList) {
				if (group.getId().equals(id)) {
					return group;
				}
			}

			return null;
		} finally {
			readLock.unlock();
		}
	}

	public int getUnreadMessages() {
		int unread = 0;
		readLock.lock();
		for (Buddy buddy : getBuddyList()) {
			unread += buddy.getUnread();
		}
		readLock.unlock();
		return unread;
	}

	/**
	 * Merge existing buddy with new values
	 * 
	 * @param newBuddy
	 *            container with new values for buddy
	 * @param updateStatus
	 *            if true, buddy status will also be updated.
	 * @return merged buddy
	 */
	public Buddy editBuddy(Buddy newBuddy, boolean updateStatus) {
		Buddy buddy = getBuddyByProtocolUid(newBuddy.getProtocolUid());

		if (buddy != null) {
			writeLock.lock();

			buddy.merge(newBuddy);

			if (!buddy.getGroupId().equals(newBuddy.getGroupId())) {
				BuddyGroup oldGroup = getBuddyGroupByGroupId(buddy.getGroupId());
				BuddyGroup newGroup = getBuddyGroupByGroupId(newBuddy.getGroupId());

				buddy.setGroupId(newBuddy.getGroupId());

				oldGroup.getBuddyList().remove(buddy);
				newGroup.getBuddyList().add(buddy);
			}

			writeLock.unlock();
		}

		return buddy;
	}

	/**
	 * Returns account id, in form of "123456789 ICQ"
	 * 
	 * @return protocolUid+" "+protocolName
	 */
	public String getAccountId() {
		return Utils.escapeGeneralDividers(protocolUid) + ApiConstants.GENERAL_DIVIDER + Utils.escapeGeneralDividers(protocolName);
	}

	/**
	 * Tell account that it has been disconnected to perform appropriate actions
	 * (reset buddies' state etc...)
	 */
	public void disconnected() {
		writeLock.lock();

		for (Buddy buddy : getBuddyList()) {
			buddy.getOnlineInfo().getFeatures().remove(ApiConstants.FEATURE_STATUS);
		}
		connectionState = ConnectionState.DISCONNECTED;

		writeLock.unlock();
	}

	/**
	 * Remove all buddies from account
	 * 
	 * @param keepNotInList
	 *            do not remove buddies that marked with
	 *            {@link AccountService#NOT_IN_LIST_GROUP_ID}, as well as group
	 *            chat records.
	 */
	public void removeAllBuddies(boolean keepNotInList) {
		writeLock.lock();
		undeletable = new LinkedList<Buddy>();
		
		for (Buddy bu : getBuddyList()) {
			if (bu.getUnread() > 0) {
				unreadsMap.put(bu.getProtocolUid(), bu.getUnread());
			}
			if ((bu.getGroupId().equals(ApiConstants.NOT_IN_LIST_GROUP_ID) && keepNotInList) || bu instanceof MultiChatRoom) {
				undeletable.add(bu);
			}
		}
		
		noGroupBuddies.clear();
		buddyGroupList.clear();

		writeLock.unlock();
	}

	/**
	 * Remove buddy from uid.
	 * 
	 * @param buddy
	 */
	public void removeBuddyByUid(String buddyUid) {
		writeLock.lock();
		for (BuddyGroup group : buddyGroupList) {
			for (int i = group.getBuddyList().size() - 1; i >= 0; i--) {
				if (group.getBuddyList().get(i).getProtocolUid().equals(buddyUid)) {
					group.getBuddyList().remove(i);
				}
			}
		}
		for (int i = noGroupBuddies.size() - 1; i >= 0; i--) {
			if (noGroupBuddies.get(i).getProtocolUid().equals(buddyUid)) {
				noGroupBuddies.remove(i);
			}
		}
		writeLock.unlock();
	}

	/**
	 * Find buddy by it's internal id.
	 * 
	 * @param id
	 * @return buddy, if found, or null.
	 */
	public Buddy getBuddyByBuddyId(int id) {
		try {
			readLock.lock();

			for (Buddy buddy : getBuddyList()) {
				if (buddy.getId() == id) {
					return buddy;
				}
			}
			return null;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Add buddy to account's buddy list, according to group mark within buddy
	 * 
	 * @param buddy
	 */
	public void addBuddyToList(Buddy buddy) {
		// removeBuddyByUid(buddy);

		if (getBuddyByProtocolUid(buddy.getProtocolUid()) != null) {
			Logger.log("Buddy " + buddy.getProtocolUid() + " already exists in " + protocolUid, LoggerLevel.INFO);
			return;
		}

		List<Buddy> target = null;

		writeLock.lock();
		for (BuddyGroup group : buddyGroupList) {
			if (group.getId().equals(buddy.getGroupId())) {
				target = group.getBuddyList();
				break;
			}
		}

		if (target == null) {
			target = noGroupBuddies;
		}

		target.add(buddy);
		writeLock.unlock();
	}

	/**
	 * Edit group.
	 * 
	 * @param newGroup
	 *            a group's new data holder.
	 */
	public void editGroup(BuddyGroup newGroup) {
		for (BuddyGroup group : buddyGroupList) {
			if (group.getId().equals(newGroup.getId())) {
				writeLock.lock();
				group.setName(newGroup.getName());
				writeLock.unlock();
				break;
			}
		}
	}

	/**
	 * Remove buddy group.
	 * 
	 * @param group
	 */
	public void removeGroup(BuddyGroup group) {
		for (int i = 0; i < buddyGroupList.size(); i++) {
			if (buddyGroupList.get(i).getId().equals(group.getId())) {
				writeLock.lock();
				buddyGroupList.remove(i);
				writeLock.unlock();
				break;
			}
		}
	}

	/**
	 * Merge account with a new data.
	 * 
	 * @param origin
	 *            a new data holder for an account.
	 */
	public void merge(Account origin) {
		if (origin == null || origin == this || origin.getServiceId() != getServiceId()) {
			return;
		}

		writeLock.lock();

		synchronized (buddyGroupList) {
			buddyGroupList.clear();
			buddyGroupList.addAll(origin.buddyGroupList);
		}
		
		synchronized (noGroupBuddies) {
			noGroupBuddies.clear();
			noGroupBuddies.addAll(origin.noGroupBuddies);
		}
		
		connectionState = origin.connectionState;

		onlineInfo.merge(origin.getOnlineInfo());

		writeLock.unlock();
	}

	/**
	 * Get preferences storage file name for an account.
	 * 
	 * @return filename
	 */
	public String getFilename() {
		return getAccountId();
	}

	/**
	 * Get human-readable account nickname. If Nickname is empty, protocol UID
	 * is returned.
	 * 
	 * @return
	 */
	public String getSafeName() {
		return (onlineInfo.getName() != null && onlineInfo.getName().length() > 0) ? onlineInfo.getName() : protocolUid;
	}

	public List<Buddy> getBuddyList() {
		List<Buddy> list = new ArrayList<Buddy>();
		readLock.lock();
		list.addAll(noGroupBuddies);
		
		for (BuddyGroup g : buddyGroupList) {
			list.addAll(g.getBuddyList());
		}

		readLock.unlock();
		return list;
	}

	/**
	 * Set new buddy list.
	 * 
	 * @param buddyList
	 */
	public void setBuddyList(List<BuddyGroup> buddyList) {
		writeLock.lock();
		List<BuddyGroup> old = new ArrayList<BuddyGroup>();
		old.addAll(this.buddyGroupList);
		this.buddyGroupList.clear();
		this.buddyGroupList.addAll(buddyList);
		for (Iterator<BuddyGroup> i = this.buddyGroupList.iterator(); i.hasNext();) {
			boolean done = false;
			BuddyGroup bg = i.next();

			if (bg.getId() == null || bg.getId().equals(ApiConstants.NO_GROUP_ID) || bg.getId().equals(ApiConstants.NOT_IN_LIST_GROUP_ID)) {
				noGroupBuddies.addAll(bg.getBuddyList());
				i.remove();
			} 

			for (BuddyGroup obg : old) {
				if (bg.getId().equals(obg.getId())) {
					bg.setCollapsed(obg.isCollapsed());
					done = true;
					break;
				}
			}
			if (done) {
				continue;
			}
		}

		for (Iterator<String> unreads = unreadsMap.keySet().iterator(); unreads.hasNext();) {
			String unreadKey = unreads.next();
			for (Buddy bu : getBuddyList()) {
				if (bu.getProtocolUid().equals(unreadKey)) {
					bu.setUnread((byte) unreadsMap.get(unreadKey));
				}
			}
		}
		if (undeletable != null) {
			noGroupBuddies.addAll(undeletable);
			undeletable = null;
		}
		unreadsMap.clear();
		writeLock.unlock();
	}

	@Override
	public String toString() {
		return getSafeName();
	}

	/**
	 * @return the isEnabled
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * @param isEnabled
	 *            the isEnabled to set
	 */
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * @return the ownName
	 */
	public String getOwnName() {
		return onlineInfo.getName();
	}

	/**
	 * @param ownName
	 *            the ownName to set
	 */
	public void setOwnName(String ownName) {
		this.onlineInfo.setName(ownName);
	}

	/**
	 * @return the connectionState
	 */
	public ConnectionState getConnectionState() {
		return connectionState;
	}

	/**
	 * @param connectionState
	 *            the connectionState to set
	 */
	public void setConnectionState(ConnectionState connectionState) {
		this.connectionState = connectionState;
	}

	/**
	 * @return the protocolName
	 */
	public String getProtocolName() {
		return protocolName;
	}

	/**
	 * @return the protocolServicePackageName
	 */
	public String getProtocolServicePackageName() {
		return protocolServicePackageName;
	}

	/**
	 * @return the protocolUid
	 */
	public String getProtocolUid() {
		return protocolUid;
	}

	/**
	 * @return the buddyGroupList
	 */
	public List<BuddyGroup> getBuddyGroupList() {
		return buddyGroupList;
	}

	/**
	 * @return the onlineInfo
	 */
	public OnlineInfo getOnlineInfo() {
		return onlineInfo;
	}

	@Override
	public String getEntityId() {
		return getAccountId();
	}

	/**
	 * @return the noGroupBuddies
	 */
	public List<Buddy> getNoGroupBuddies() {
		return noGroupBuddies;
	}
}
