package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyGroup;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyList;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQIconData;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public abstract class AbstractFlapProcessor implements IFlapProcessor {

	protected ICQServiceInternal service;	
	List<ICQBuddy> buddies = null;
	List<ICQBuddyGroup> groups = null;
	
	Map<String, Short> permitList = new HashMap<String, Short>();
	Map<String, Short> denyList = new HashMap<String, Short>();
	List<ICQBuddy> ignoreList = new ArrayList<ICQBuddy>();
	List<ICQBuddy> notAuthList = new ArrayList<ICQBuddy>();
	
	@Override
	public void init(ICQServiceInternal service) throws ICQException {
		this.service = service;		
	}		
	
	@Override
	public void process(Flap flap) throws ICQException{
		internalFlapMap(flap);
	}
	
	protected abstract void internalFlapMap(Flap flap) throws ICQException;

	protected void fillOnlineInfoParams(TLV[] tlvs, ICQOnlineInfo info) throws ICQException{
		for (int i=0; i<tlvs.length; i++){
			TLV tlv = tlvs[i];
			switch(tlv.type){
			case 0xca:
				info.visibility = tlv.value[0];
				break;
			case 0xcb:
				//user class visibility - unused for now;
				break;
			case 0xcc:
				try {
					info.handheldNotification = tlv.value[7]!=2;
					info.idleTimeNotification = tlv.value[5]==4;
					info.typingNotification = tlv.value[5]==4;
				} catch (Exception e) {
					service.log(e);
				}
				break;
			}			
		}
	}	
	
	protected void parseBuddyList(byte[] plainData, boolean hasMore) throws ICQException{
		service.log("  parse buddy list "+ProtocolUtils.getSpacedHexString(plainData));
		
		ICQBuddyList buddyList = service.getBuddyList();
		buddyList.ssiVersion = plainData[0];
		int itemCount = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, 1));
		service.log("  item count "+itemCount);
		buddyList.itemNumber = itemCount;
		if (buddies == null || groups == null){
			buddies = new ArrayList<ICQBuddy>();
			groups = new ArrayList<ICQBuddyGroup>();
		}
		int pos = 3;
		int itemPos = 0;
		
		List<Short> existingIDs = new ArrayList<Short>(itemCount);
		
		while (itemPos<itemCount){
			int nameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, pos));
			pos+=2;
			String name = null;
			
			try {
				name = new String(plainData, pos, nameLength, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				name = new String(plainData, pos, nameLength);
			}			
			pos+=nameLength;
			
			short groupId = ProtocolUtils.bytes2ShortBE(plainData, pos);
			pos+=2;
			
			short itemId = ProtocolUtils.bytes2ShortBE(plainData, pos);
			pos+=2;
			
			short flagType = ProtocolUtils.bytes2ShortBE(plainData, pos);
			pos+=2;
			
			int tlvLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, pos));
			pos+=2;
			
			existingIDs.add(itemId);
			
			TLV[] tlvs = null;
			if (tlvLength>0){
				byte[] buffer = new byte[tlvLength];
				System.arraycopy(plainData, pos, buffer, 0, tlvLength);				

				try {				
					tlvs = service.getDataParser().parseTLV(buffer);
				} catch (ICQException e) {
					service.log(e);
				}
			}
			service.log("  item name "+name +" flag "+flagType);
			
			pos+=tlvLength;
			switch(flagType){
			case 0x0:
				ICQBuddy buddy = new ICQBuddy();
				buddy.uin = name;
				buddy.groupId = groupId;
				buddy.itemId = itemId;
				buddy.flagType = flagType;
				buddy.setAdditional(tlvs, notAuthList);
				buddies.add(buddy);
				break;
			case 0x1:
				if (groupId!=0x0){
					ICQBuddyGroup group = new ICQBuddyGroup();
					group.name = name;
					group.groupId = groupId;
					group.setAdditional(tlvs);
					groups.add(group);
				}
				break;
			case 0x2:
				permitList.put(name, itemId);
				break;
			case 0x3:
				denyList.put(name, itemId);
				break;
			case 0x4:
				service.getOnlineInfo().itemId = itemId;
				fillOnlineInfoParams(tlvs, service.getOnlineInfo());
				break;
			case 0x5:
				//presence info
				break;
			case 0x9:
				//wtf
				break;
			case 0xe:
				ICQBuddy ignored = new ICQBuddy();
				ignored.uin = name;
				ignored.groupId = groupId;
				ignored.itemId = itemId;
				ignored.flagType = flagType;
				ignored.setAdditional(tlvs, notAuthList);
				ignored.visibility = ICQConstants.VIS_IGNORED;
				ignoreList.add(ignored);
				break;
			case 0xf:
				//service.getBuddyList().setLastUpdateTime(Utils.bytes2Date());
				break;
			case 0x10:
				//non-icq contact
				break;
			case 0x13:
				//roster import time
				break;
			case 0x14:
				service.log("icon info ");
				ICQIconData iconData = new ICQIconData();
				iconData.ssiItemId = itemId;
				
				service.getOnlineInfo().iconData = iconData;
				break;
			}			
			itemPos++;
		}
		
		buddyList.lastUpdateTime = new Date(ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(plainData, pos))*1000);
		
		/*Map<String, String> ssiMap = new HashMap<String, String>();
		ssiMap.put(ICQConstants.SAVEDPREFERENCES_SSI_ITEM_COUNT, itemCount+"");
		ssiMap.put(ICQConstants.SAVEDPREFERENCES_SSI_UPDATE_DATE, buddyList.lastUpdateTime.getTime()+"");
		service.getServiceResponse().respond(ICQServiceResponse.RES_SAVETOSTORAGE, ICQConstants.SAVEDPREFERENCES_NAME, ssiMap);*/
		
		if (!hasMore){
			Set<String> permits = permitList.keySet();
			for (String uin:permits){
				for (ICQBuddy buddy:buddies){
					if (uin.equalsIgnoreCase(buddy.uin)){
						buddy.visibility = ICQConstants.VIS_PERMITTED;
						service.log("permitted "+uin);
						break;
					}
				}
			}
			Set<String> denys = denyList.keySet();
			for (String uin:denys){
				for (ICQBuddy buddy:buddies){
					if (uin.equalsIgnoreCase(buddy.uin)){
						buddy.visibility = ICQConstants.VIS_DENIED;
						service.log("denied "+uin);
						break;
					}
				}
			}
			
			service.setBuddyList(buddies, groups, permitList, denyList, notAuthList, existingIDs);
			
			buddies = null;
			groups = null;
		}
	}
	
	protected abstract void internalTLVMap(TLV tlv)throws ICQException;
	
	protected abstract void internalSnacMap(Snac snac)throws ICQException;
}
