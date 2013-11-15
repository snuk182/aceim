package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ICQBuddyList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5979496006961145797L;

	public Set<Short> existingIDs = new HashSet<Short>();
	
	public List<ICQBuddy> buddyList = new ArrayList<ICQBuddy>();
	public List<ICQBuddyGroup> buddyGroupList = new ArrayList<ICQBuddyGroup>();
	public Map<String, Short> permitList = new HashMap<String, Short>();
	public Map<String, Short> denyList = new HashMap<String, Short>();
	
	public List<ICQOnlineInfo> buddyInfos = Collections.synchronizedList(new ArrayList<ICQOnlineInfo>());
	
	public Date lastUpdateTime;
	public int itemNumber = 0;
	public byte ssiVersion;

	public List<ICQBuddy> notAuthList = new ArrayList<ICQBuddy>();
	
	public ICQOnlineInfo getByUin(String uin){
		for (ICQOnlineInfo info:buddyInfos){
			if (info.uin.equals(uin)){
				return info;
			}
		}
		return null;
	}

	public ICQBuddy removeFromNotAuthListByUin(String uin) {
		for (int i=notAuthList.size()-1; i>=0; i--){
			if (notAuthList.get(i).uin.equals(uin)){
				return notAuthList.remove(i);
			}
		}
		return null;
	}

	public ICQBuddyGroup findGroupById(int groupId) {
		for (ICQBuddyGroup group : buddyGroupList) {
			if (group.groupId == groupId) {
				return group;
			}
		}
		
		return null;
	}
	
	public ICQBuddy findBuddyByUin(String uin) {
		for (ICQBuddy b : buddyList) {
			if (b.uin.equals(uin)) {
				return b;
			}
		}
		
		return null;
	}
	
	public List<Integer> getBuddyGroupIds(){
		List<Integer> list = new ArrayList<Integer>();
		
		for (ICQBuddyGroup group : buddyGroupList){
			list.add(group.groupId);
		}
		
		return list;
	}
}
