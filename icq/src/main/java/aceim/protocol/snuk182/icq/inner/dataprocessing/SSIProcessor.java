package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyGroup;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQIconData;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.MD5;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class SSIProcessor {
	
	byte[] newIcon = null;

	private ICQServiceInternal service;
	private ICQBuddyGroup tmpGroup;
	private ICQBuddy tmpBuddy;
	// private boolean authRequired = false;

	private static final byte ACTION_NONE = 0;
	private static final byte ACTION_ADD_BUDDY = 1;
	private static final byte ACTION_DELETE_BUDDY = 2;
	private static final byte ACTION_ADD_GROUP = 3;
	private static final byte ACTION_DELETE_GROUP = 4;
	private static final byte ACTION_RENAME_BUDDY = 5;
	private static final byte ACTION_RENAME_GROUP = 6;
	private static final byte ACTION_MOVE_BUDDY = 7;
	//private static final byte ACTION_UPLOAD_ICON = 8;;

	private byte currentAction = 0;

	public SSIProcessor(ICQServiceInternal icqServiceInternal) {
		this.service = icqServiceInternal;
	}

	public void modifyGroup(ICQBuddyGroup group) {
		currentAction = ACTION_RENAME_GROUP;
		tmpGroup = group;
		service.getRunnableService().sendToSocket(getModifyGroupFlap(group));
	}

	public void addGroup(ICQBuddyGroup group) {
		ICQBuddyGroup masterGroup = new ICQBuddyGroup();
		masterGroup.name = "";
		masterGroup.groupId = 0;
		masterGroup.buddies.addAll(service.getBuddyList().getBuddyGroupIds());
		masterGroup.buddies.add(group.groupId);

		currentAction = ACTION_ADD_GROUP;
		tmpGroup = group;

		service.getRunnableService().sendMultipleToSocket(new Flap[] { getSSIEditStartFlap(), getAddGroupFlap(group), getModifyGroupFlap(masterGroup), getSSIEditEndFlap() });
	}

	public void moveBuddy(ICQBuddy buddy, ICQBuddyGroup oldGroup, ICQBuddyGroup newGroup) {
		currentAction = ACTION_MOVE_BUDDY;
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getSSIEditStartFlap(), getDeleteBuddyFlap(buddy, false), getModifyGroupFlap(oldGroup) });
		buddy.groupId = newGroup.groupId;
		tmpBuddy = buddy;
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getAddBuddyFlap(buddy, newGroup, buddy.visibility == ICQConstants.VIS_NOT_AUTHORIZED, false), getModifyGroupFlap(newGroup), getSSIEditEndFlap() });
	}

	public void modifyBuddy(ICQBuddy buddy) {
		currentAction = ACTION_RENAME_BUDDY;
		tmpBuddy = buddy;
		service.getRunnableService().sendToSocket(getModifyBuddyFlap(buddy, null));
	}

	private Flap getAddGroupFlap(ICQBuddyGroup group) {
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;

		byte[] nameBytes;
		try {
			nameBytes = group.name.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			nameBytes = group.name.getBytes();
		}
		byte[] header = new byte[10 + nameBytes.length];
		int pos = 0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) nameBytes.length), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(nameBytes, 0, header, pos, nameBytes.length);
		pos += nameBytes.length;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) group.groupId), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 1), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
		pos += 2;

		data2.plainData = header;
		flap2.data = data2;

		return flap2;
	}

	private Flap getModifyBuddyFlap(ICQBuddy buddy, ICQOnlineInfo info) {
		if (buddy == null && info == null){
			return null;
		}
		
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_GROUPHEADERUPD;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_GROUPHEADERUPD;

		byte[] uinBytes;
		if (buddy != null){
			try {
				uinBytes = buddy.uin.getBytes("ASCII");
			} catch (UnsupportedEncodingException e) {
				uinBytes = buddy.uin.getBytes();
			}
		} else {
			uinBytes = new byte[0];
		}
		byte[] header = new byte[10 + uinBytes.length];
		int pos = 0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) uinBytes.length), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(uinBytes, 0, header, pos, uinBytes.length);
		pos += uinBytes.length;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) (buddy != null ? buddy.groupId : 0)), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) (buddy != null ? buddy.itemId : info.itemId)), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) (buddy != null ? 0 : 4)), 0, header, pos, 2);
		pos += 2;

		if (buddy != null){
			String screenname;
			if (buddy.screenName != null) {
				screenname = buddy.screenName;
			} else {
				screenname = buddy.uin;
			}

			TLV tlv = new TLV();
			tlv.type = 0x131;
			byte[] screennameBytes;
			try {
				screennameBytes = screenname.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				screennameBytes = screenname.getBytes();
			}
			tlv.value = screennameBytes;

			TLV email = new TLV();
			email.type = 0x137;

			TLV sms = new TLV();
			sms.type = 0x13a;

			TLV comment = new TLV();
			comment.type = 0x13c;

			if (buddy.visibility == ICQConstants.VIS_NOT_AUTHORIZED) {
				TLV authTlv = new TLV();
				authTlv.type = 0x66;
				data2.data = new TLV[] { tlv, sms, email, comment, authTlv };
			} else {
				data2.data = new TLV[] { tlv, sms, email, comment };
			}

			System.arraycopy(ProtocolUtils.short2ByteBE((short) (16 + screennameBytes.length + (buddy.visibility == ICQConstants.VIS_NOT_AUTHORIZED ? 4 : 0))), 0, header, pos, 2);
			pos += 2;
		} else {
			TLV visTlv = new TLV();
			visTlv.type = 0xca;
			visTlv.value = new byte[]{info.visibility};
			
			TLV permTlv = new TLV();
			permTlv.type = 0xcb;
			permTlv.value = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
			
			TLV xz1 = new TLV();
			xz1.type = 0xd0;
			xz1.value = new byte[]{1};
			
			TLV xz2 = new TLV();
			xz2.type = 0xd1;
			xz2.value = new byte[]{1};
			
			TLV xz3 = new TLV();
			xz3.type = 0xd2;
			xz3.value = new byte[]{1};
			
			TLV xz4 = new TLV();
			xz4.type = 0xd3;
			xz4.value = new byte[]{1};
			
			data2.data = new TLV[] {visTlv, permTlv, xz1, xz2, xz3, xz4};
			
			System.arraycopy(ProtocolUtils.short2ByteBE((short) 33), 0, header, pos, 2);
			pos += 2;
		}

		data2.plainData = header;
		flap2.data = data2;

		return flap2;
	}

	private Flap getDeleteBuddyFlap(ICQBuddy buddy, boolean fixPermissions) {
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;

		byte[] uinBytes;
		try {
			uinBytes = buddy.uin.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uinBytes = buddy.uin.getBytes();
		}
		byte[] header = new byte[10 + uinBytes.length];
		int pos = 0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) uinBytes.length), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(uinBytes, 0, header, pos, uinBytes.length);
		pos += uinBytes.length;
		
		int groupId;
		if (!fixPermissions){
			groupId = buddy.groupId;
		} else {
			groupId = 0;
		}
		System.arraycopy(ProtocolUtils.short2ByteBE((short) groupId), 0, header, pos, 2);
		pos += 2;
		
		int itemId;
		if (!fixPermissions){
			itemId = buddy.itemId;
		} else {
			try {
				ICQOnlineInfo nfo = service.getBuddyList().getByUin(buddy.uin);
				switch (nfo.visibility) {
				case ICQConstants.VIS_DENIED:
					itemId = service.getBuddyList().denyList.remove(buddy.uin);
					break;
				case ICQConstants.VIS_PERMITTED:
					itemId = service.getBuddyList().permitList.remove(buddy.uin);
					break;
				default:
					return null;
				}
			} catch (Exception e) {
				return null;
			}
		}
		System.arraycopy(ProtocolUtils.short2ByteBE((short) itemId), 0, header, pos, 2);
		pos += 2;
		if (!fixPermissions) {
			System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
		} else {
			ICQOnlineInfo nfo = service.getBuddyList().getByUin(buddy.uin);
			switch (nfo.visibility) {
			case ICQConstants.VIS_DENIED:
				System.arraycopy(ProtocolUtils.short2ByteBE((short) 3), 0, header, pos, 2);
				break;
			case ICQConstants.VIS_PERMITTED:
				System.arraycopy(ProtocolUtils.short2ByteBE((short) 2), 0, header, pos, 2);
				break;
			default:
				return null;
			}
		}
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
		pos += 2;

		data2.plainData = header;
		flap2.data = data2;

		return flap2;
	}

	public void removeBuddyFromContactList(ICQBuddy buddy) {
		/*
		 * Flap flap1 = new Flap(); flap1.channel =
		 * ICQConstants.FLAP_CHANNELL_DATA; flap1.sequenceNumber =
		 * service.getFlapSeqNumber();
		 * 
		 * Snac data1 = new Snac(); data1.serviceId =
		 * ICQConstants.SNAC_FAMILY_SERVERSIDEINFO; data1.subtypeId =
		 * ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITSTART; data1.requestId =
		 * ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITSTART;
		 * 
		 * flap1.data = data1;
		 */

		/*
		 * Flap flap3 = new Flap(); flap3.channel =
		 * ICQConstants.FLAP_CHANNELL_DATA; flap3.sequenceNumber =
		 * service.getFlapSeqNumber();
		 * 
		 * Snac data3 = new Snac(); data3.serviceId =
		 * ICQConstants.SNAC_FAMILY_SERVERSIDEINFO; data3.subtypeId =
		 * ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITEND; data3.requestId =
		 * ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITEND;
		 * 
		 * flap3.data = data3;
		 */

		currentAction = ACTION_DELETE_BUDDY;
		tmpBuddy = buddy;
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getSSIEditStartFlap(), getDeleteBuddyFlap(buddy, false), getSSIEditEndFlap() });
	}

	public void removeGroup(ICQBuddyGroup group) {
		currentAction = ACTION_DELETE_GROUP;
		tmpGroup = group;
		service.getRunnableService().sendToSocket(getDeleteGroupFlap(group));
	}

	private Flap getDeleteGroupFlap(ICQBuddyGroup group) {
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;

		byte[] nameBytes;
		try {
			nameBytes = group.name.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			nameBytes = group.name.getBytes();
		}
		byte[] header = new byte[10 + nameBytes.length];
		int pos = 0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) nameBytes.length), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(nameBytes, 0, header, pos, nameBytes.length);
		pos += nameBytes.length;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) group.groupId), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 1), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
		pos += 2;

		data2.plainData = header;
		flap2.data = data2;

		return flap2;
	}

	public void parseAuthRequest(byte[] plainData) {
		if (plainData == null || plainData.length < 2) {
			return;
		}
		byte uinLength = plainData[0];

		byte[] uinBytes = new byte[uinLength];
		System.arraycopy(plainData, 1, uinBytes, 0, uinLength);
		String uin = new String(uinBytes);

		int reasonLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, 1 + uinLength));
		byte[] reasonBytes = new byte[reasonLength];
		System.arraycopy(plainData, 3 + uinLength, reasonBytes, 0, reasonLength);
		String reason;
		try {
			reason = new String(reasonBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			reason = new String(reasonBytes);
		}

		service.getServiceResponse().respond(ICQServiceResponse.RES_AUTHREQUEST, uin, reason);
	}

	public void parseSSIResponse(byte[] plainData) {
		if (plainData == null || plainData.length < 2) {
			return;
		}
		switch (plainData[1]) {
		case 0xe:
			switch(currentAction){
			case ACTION_MOVE_BUDDY:
				tmpBuddy.visibility = ICQConstants.VIS_NOT_AUTHORIZED;
				//moveBuddy(tmpBuddy);
				break;
			case ACTION_RENAME_BUDDY:
				tmpBuddy.visibility = ICQConstants.VIS_NOT_AUTHORIZED;
				modifyBuddy(tmpBuddy);
				break;
			case ACTION_ADD_BUDDY:
				tmpBuddy.visibility = ICQConstants.VIS_NOT_AUTHORIZED;
				tmpGroup.buddies.add(tmpBuddy.itemId);
				addBuddyToContactList(tmpBuddy, tmpGroup, true);
				break;
			}
		case 0x0:
			switch (currentAction) {
			case ACTION_RENAME_GROUP:
				service.getServiceResponse().respond(ICQServiceResponse.RES_GROUPMODIFIED, tmpGroup);
				tmpGroup = null;
				currentAction = ACTION_NONE;
				break;
			case ACTION_MOVE_BUDDY:
			case ACTION_RENAME_BUDDY:
				service.getServiceResponse().respond(ICQServiceResponse.RES_BUDDYMODIFIED, tmpBuddy);
				tmpBuddy = null;
				currentAction = ACTION_NONE;
				break;
			case ACTION_DELETE_BUDDY:
				service.getServiceResponse().respond(ICQServiceResponse.RES_BUDDYDELETED, tmpBuddy);
				tmpBuddy = null;
				currentAction = ACTION_NONE;
				break;
			case ACTION_ADD_BUDDY:
				service.getServiceResponse().respond(ICQServiceResponse.RES_BUDDYADDED, tmpBuddy);
				tmpBuddy = null;
				currentAction = ACTION_NONE;
				break;
			case ACTION_ADD_GROUP:
				service.getBuddyList().buddyGroupList.add(tmpGroup);
				service.getServiceResponse().respond(ICQServiceResponse.RES_GROUPADDED, tmpGroup);
				tmpGroup = null;
				currentAction = ACTION_NONE;
				break;
			case ACTION_DELETE_GROUP:
				service.getServiceResponse().respond(ICQServiceResponse.RES_GROUPDELETED, tmpGroup);
				tmpGroup = null;
				currentAction = ACTION_NONE;
				break;
			/*case ACTION_UPLOAD_ICON:
				currentAction = ACTION_NONE;
				service.getBuddyIconEngine().requestIconUpload(newIcon);
				newIcon = null;
				break;*/
			}
			break;
		}
		// sendSSIEditEnd();
	}

	private Flap getSSIEditStartFlap() {
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data1 = new Snac();
		data1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITSTART;
		data1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITSTART;

		flap1.data = data1;

		return flap1;
	}

	private Flap getSSIEditEndFlap() {
		Flap flap3 = new Flap();
		flap3.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data3 = new Snac();
		data3.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data3.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITEND;
		data3.requestId = ICQConstants.SNAC_SERVERSIDEINFO_CONTACTSEDITEND;

		flap3.data = data3;

		return flap3;
	}

	private Flap getAddBuddyFlap(ICQBuddy buddy, ICQBuddyGroup group, boolean authorizationRequired, boolean fixPermissions) {
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;

		byte[] uinBytes;
		try {
			uinBytes = buddy.uin.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uinBytes = buddy.uin.getBytes();
		}
		byte[] header = new byte[10 + uinBytes.length];
		int pos = 0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) uinBytes.length), 0, header, pos, 2);
		pos += 2;
		System.arraycopy(uinBytes, 0, header, pos, uinBytes.length);
		pos += uinBytes.length;
		
		int groupId;
		if (!fixPermissions){
			if (group!=null){
				groupId = group.groupId;
			} else {
				groupId = buddy.groupId;
			}
		} else {
			groupId = 0;
		}
		System.arraycopy(ProtocolUtils.short2ByteBE((short) groupId), 0, header, pos, 2);
		pos += 2;
		
		int itemId;
		if (!fixPermissions){
			itemId = buddy.itemId;
		} else {
			switch (buddy.visibility) {
			case ICQConstants.VIS_DENIED:
				do {
					itemId = (short) new Random().nextInt(0x7fff);
				} while (service.getBuddyList().denyList.containsValue(itemId));
				service.getBuddyList().denyList.put(buddy.uin, (short) itemId);
				break;
			case ICQConstants.VIS_PERMITTED:
				do {
					itemId = (short) new Random().nextInt(0x7fff);
				} while (service.getBuddyList().permitList.containsValue(itemId));
				service.getBuddyList().permitList.put(buddy.uin, (short) itemId);
				break;
			default:
				return null;
			}
		}
		System.arraycopy(ProtocolUtils.short2ByteBE((short) itemId), 0, header, pos, 2);
		pos += 2;
		
		int itemType;
		if (!fixPermissions) {
			itemType = 0;
		} else {
			switch (buddy.visibility) {
			case ICQConstants.VIS_DENIED:
				itemType = 3;
				break;
			case ICQConstants.VIS_PERMITTED:
				itemType = 2;
				break;
			default:
				return null;
			}
		}
		System.arraycopy(ProtocolUtils.short2ByteBE((short) itemType), 0, header, pos, 2);
		
		pos += 2;

		String screenname;
		if (buddy.screenName != null) {
			screenname = buddy.screenName;
		} else {
			screenname = buddy.uin;
		}

		TLV tlv = new TLV();
		tlv.type = 0x131;
		byte[] screennameBytes;
		try {
			screennameBytes = screenname.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			screennameBytes = screenname.getBytes();
		}
		tlv.value = screennameBytes;

		TLV email = new TLV();
		email.type = 0x137;

		TLV sms = new TLV();
		sms.type = 0x13a;

		TLV comment = new TLV();
		comment.type = 0x13c;

		if (authorizationRequired) {
			TLV authTlv = new TLV();
			authTlv.type = 0x66;
			data2.data = new TLV[] { tlv, sms, email, comment, authTlv };
			group.buddies.add(buddy.itemId);
			tmpGroup = group;
		} else {
			data2.data = new TLV[] { tlv, sms, email, comment };
		}

		System.arraycopy(ProtocolUtils.short2ByteBE((short) (16 + screennameBytes.length + (authorizationRequired ? 4 : 0))), 0, header, pos, 2);
		pos += 2;

		data2.plainData = header;
		flap2.data = data2;

		return flap2;
	}

	private Flap getAddBuddiesFlap(List<ICQBuddy> buddies, ICQBuddyGroup group) {
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;

		List<byte[]> bytes = new ArrayList<byte[]>(buddies.size());
		int bytesCount = 0;
		for (ICQBuddy buddy : buddies) {
			byte[] uinBytes;
			try {
				uinBytes = buddy.uin.getBytes("ASCII");
			} catch (UnsupportedEncodingException e) {
				uinBytes = buddy.uin.getBytes();
			}
			byte[] header = new byte[10 + uinBytes.length];
			int pos = 0;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) uinBytes.length), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(uinBytes, 0, header, pos, uinBytes.length);
			pos += uinBytes.length;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) group.groupId), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) buddy.itemId), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
			pos += 2;

			String screenname;
			if (buddy.screenName != null) {
				screenname = buddy.screenName;
			} else {
				screenname = buddy.uin;
			}

			TLV tlv = new TLV();
			tlv.type = 0x131;
			byte[] screennameBytes;
			try {
				screennameBytes = screenname.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				screennameBytes = screenname.getBytes();
			}
			tlv.value = screennameBytes;

			TLV email = new TLV();
			email.type = 0x137;

			TLV sms = new TLV();
			sms.type = 0x13a;

			TLV comment = new TLV();
			comment.type = 0x13c;

			if (buddy.visibility == ICQConstants.VIS_NOT_AUTHORIZED) {
				TLV authTlv = new TLV();
				authTlv.type = 0x66;
				data2.data = new TLV[] { tlv, sms, email, comment, authTlv };
				group.buddies.add(buddy.itemId);
				tmpGroup = group;
			} else {
				data2.data = new TLV[] { tlv, sms, email, comment };
			}

			System.arraycopy(ProtocolUtils.short2ByteBE((short) (16 + screennameBytes.length + ((buddy.visibility == ICQConstants.VIS_NOT_AUTHORIZED) ? 4 : 0))), 0, header, pos, 2);
			pos += 2;

			bytes.add(header);
			bytesCount += header.length;
		}

		byte[] main = new byte[bytesCount];
		bytesCount = 0;
		for (byte[] part : bytes) {
			System.arraycopy(part, 0, main, bytesCount, part.length);
			bytesCount += part.length;
		}

		data2.plainData = main;
		flap2.data = data2;

		return flap2;
	}

	private Flap getModifyGroupFlap(ICQBuddyGroup group) {
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac snac1 = new Snac();
		snac1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		snac1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_GROUPHEADERUPD;
		snac1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_GROUPHEADERUPD;
		byte[] groupNameBytes;
		try {
			groupNameBytes = group.name.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			groupNameBytes = group.name.getBytes();
		}
		TLV groupContentsTlv = new TLV();
		groupContentsTlv.type = 0xc8;
		byte[] contents = new byte[2 * group.buddies.size()];
		for (int i = 0; i < group.buddies.size(); i++) {
			System.arraycopy(ProtocolUtils.short2ByteBE(group.buddies.get(i).shortValue()), 0, contents, 2 * i, 2);
		}
		groupContentsTlv.value = contents;
		byte[] tlvBytes = service.getDataParser().tlvs2Bytes(new TLV[] { groupContentsTlv });
		byte[] result = new byte[10 + groupNameBytes.length + tlvBytes.length];

		int pos = 0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) groupNameBytes.length), 0, result, pos, 2);
		pos += 2;
		System.arraycopy(groupNameBytes, 0, result, pos, groupNameBytes.length);
		pos += groupNameBytes.length;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) group.groupId), 0, result, pos, 2);
		pos += 2;
		System.arraycopy(new byte[] { 0, 0 }, 0, result, pos, 2);
		pos += 2;
		System.arraycopy(new byte[] { 0, 1 }, 0, result, pos, 2);
		pos += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) tlvBytes.length), 0, result, pos, 2);
		pos += 2;
		System.arraycopy(tlvBytes, 0, result, pos, tlvBytes.length);
		// pos+=tlvBytes.length;
		snac1.plainData = result;
		flap1.data = snac1;

		return flap1;
	}

	public void addBuddyToContactList(ICQBuddy buddy, ICQBuddyGroup group, boolean authorizationRequired) {
		this.tmpGroup = group;
		this.tmpBuddy = buddy;
		currentAction = ACTION_ADD_BUDDY;
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getSSIEditStartFlap(), getAddBuddyFlap(buddy, group, authorizationRequired, false), getModifyGroupFlap(group), getSSIEditEndFlap() });
	}

	public void sendAuthorizationRequest(String uin, String reason) {
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getGrantFutureAuthorizationFlap(uin), getAuthRequestFlap(uin, reason) });
	}

	public void sendAuthorizationReply(String buddyUid, boolean authorized) {
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getGrantFutureAuthorizationFlap(buddyUid), getAuthReplyFlap(buddyUid, authorized) });
	}

	private Flap getGrantFutureAuthorizationFlap(String buddyUid) {
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac snac1 = new Snac();
		snac1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		snac1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_CLIENTAUTHGRANT;
		snac1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_CLIENTAUTHGRANT;

		byte[] uinBytes;
		try {
			uinBytes = buddyUid.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uinBytes = buddyUid.getBytes();
		}
		byte[] data = new byte[5 + uinBytes.length];
		data[0] = (byte) uinBytes.length;
		System.arraycopy(uinBytes, 0, data, 1, uinBytes.length);
		System.arraycopy(new byte[] { 0, 0, 0, 0 }, 0, data, uinBytes.length + 1, 4);

		snac1.plainData = data;
		flap1.data = snac1;

		return flap1;
	}

	private Flap getAuthRequestFlap(String uin, String reason) {
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac snac1 = new Snac();
		snac1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		snac1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_AUTHREQSEND;
		snac1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_AUTHREQSEND;

		byte[] uinBytes;
		try {
			uinBytes = uin.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uinBytes = uin.getBytes();
		}
		byte[] reasonBytes;
		try {
			reasonBytes = reason.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			reasonBytes = reason.getBytes();
		}
		byte[] data = new byte[reasonBytes.length + uinBytes.length + 5];
		data[0] = (byte) uinBytes.length;
		System.arraycopy(uinBytes, 0, data, 1, uinBytes.length);
		System.arraycopy(ProtocolUtils.short2ByteBE((short) reasonBytes.length), 0, data, 1 + uinBytes.length, 2);
		System.arraycopy(reasonBytes, 0, data, uinBytes.length + 3, reasonBytes.length);
		data[uinBytes.length + 3 + reasonBytes.length] = 0;
		data[uinBytes.length + 4 + reasonBytes.length] = 0;
		snac1.plainData = data;
		flap1.data = snac1;

		return flap1;
	}

	private Flap getAuthReplyFlap(String buddyUid, boolean granted) {
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac snac1 = new Snac();
		snac1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		snac1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_AUTHRESSEND;
		snac1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_AUTHRESSEND;

		byte[] uinBytes;
		try {
			uinBytes = buddyUid.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uinBytes = buddyUid.getBytes();
		}
		byte[] data = new byte[uinBytes.length + 4];
		data[0] = (byte) uinBytes.length;
		System.arraycopy(uinBytes, 0, data, 1, uinBytes.length);
		data[uinBytes.length + 1] = (byte) (granted ? 1 : 0);
		System.arraycopy(new byte[] { 0, 0 }, 0, data, uinBytes.length + 2, 2);

		snac1.plainData = data;
		flap1.data = snac1;

		return flap1;
	}

	public void moveBuddies(List<ICQBuddy> buddies, ICQBuddyGroup oldGroup, ICQBuddyGroup newGroup) {
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getSSIEditStartFlap(), getRemoveBuddiesFlap(buddies), getModifyGroupFlap(oldGroup) });
		for (ICQBuddy buddy : buddies) {
			buddy.groupId = newGroup.groupId;
		}
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getAddBuddiesFlap(buddies, newGroup), getModifyGroupFlap(newGroup), getSSIEditEndFlap() });
	}

	public void removeBuddies(List<ICQBuddy> buddies) {
		service.getRunnableService().sendMultipleToSocket(new Flap[] { getSSIEditStartFlap(), getRemoveBuddiesFlap(buddies), getSSIEditEndFlap() });
	}

	private Flap getRemoveBuddiesFlap(List<ICQBuddy> buddies) {
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data2.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;
		data2.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;

		List<byte[]> bytes = new ArrayList<byte[]>(buddies.size());
		int bytesCount = 0;
		for (ICQBuddy buddy : buddies) {
			byte[] uinBytes;
			try {
				uinBytes = buddy.uin.getBytes("ASCII");
			} catch (UnsupportedEncodingException e) {
				uinBytes = buddy.uin.getBytes();
			}
			byte[] header = new byte[10 + uinBytes.length];
			int pos = 0;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) uinBytes.length), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(uinBytes, 0, header, pos, uinBytes.length);
			pos += uinBytes.length;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) buddy.groupId), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) buddy.itemId), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, header, pos, 2);
			pos += 2;

			bytes.add(header);
			bytesCount += header.length;
		}

		byte[] main = new byte[bytesCount];
		bytesCount = 0;
		for (byte[] part : bytes) {
			System.arraycopy(part, 0, main, bytesCount, part.length);
			bytesCount += part.length;
		}

		data2.plainData = main;
		flap2.data = data2;

		return flap2;
	}

	public void modifyMyVisibility(ICQOnlineInfo onlineInfo) {
		service.getRunnableService().sendToSocket(getModifyBuddyFlap(null, onlineInfo));
	}

	public void modifyVisibility(ICQBuddy icqBuddy) {
		service.getRunnableService().sendMultipleToSocket(new Flap[]{getDeleteBuddyFlap(icqBuddy, true), getAddBuddyFlap(icqBuddy, null, false, true)});
	}

	public void requestIconUpload(byte[] icon) {
		newIcon = icon;
		
		if (newIcon == null) {
			service.log("Icon to upload is null!");
			return;
		}
		
		Snac data1 = new Snac();
		
		ICQIconData iconData = service.getOnlineInfo().iconData;
		if (newIcon.length > 0 && (iconData == null || iconData.ssiItemId == 0)) {
			service.getOnlineInfo().iconData = new ICQIconData();
			service.getOnlineInfo().iconData.ssiItemId = service.generateNewItemId();
			
			data1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
			data1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;
			data1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMADD;

			prepareSSIIconActionData(new MD5().calculate(newIcon), (byte) 1, data1);
		} else {
			data1.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
			data1.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;
			data1.requestId = ICQConstants.SNAC_SERVERSIDEINFO_ITEMREMOVE;
			
			if (iconData.hash == null) {
				prepareSSIIconActionData(new MD5().calculate(newIcon), (byte) 1, data1);			
			} else {
				prepareSSIIconActionData(iconData.hash, (byte) 1, data1);
			}
			
			if (newIcon.length < 1) {
				newIcon = null;
			}
		}
		
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;
		flap1.data = data1;
		
		service.getRunnableService().sendToSocket(flap1);
	}
	
	private void prepareSSIIconActionData(byte[] hash, byte flags, Snac container) {
		byte[] tlvData = new byte[hash.length + 2];
		tlvData[0] = flags;
		tlvData[1] = (byte) hash.length;
		System.arraycopy(hash, 0, tlvData, 2, hash.length);
		
		TLV hashTlv = new TLV((short) 0xd5, tlvData);
		TLV xz = new TLV((short) 0x131, new byte[0]);
		
		byte[] uinBytes;
		try {
			uinBytes = "1".getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			service.log(e);
			uinBytes = "1".getBytes();
		}
		
		byte[] data = new byte[2 + uinBytes.length + 2 + 2 + 2 + 2];
		int index = 0;
		
		System.arraycopy(ProtocolUtils.short2ByteBE((short) uinBytes.length), 0, data, index, 2);
		index += 2;
		System.arraycopy(uinBytes, 0, data, index, uinBytes.length);
		index += uinBytes.length;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0), 0, data, index, 2);
		index += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE(service.getOnlineInfo().iconData != null ? service.getOnlineInfo().iconData.ssiItemId : 0), 0, data, index, 2);
		index += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 0x14), 0, data, index, 2);
		index += 2;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) (tlvData.length + 8)), 0, data, index, 2);
		
		container.plainData = data;
		container.data = new TLV[]{hashTlv, xz};
	}

	public void parseAuthResponse(byte[] plainData) {
		if (plainData == null || plainData.length < 2) {
			return;
		}
		
		int index = 0;
		byte uinLength = plainData[index];
		index += 1;

		byte[] uinBytes = new byte[uinLength];
		System.arraycopy(plainData, index, uinBytes, 0, uinLength);
		String uin = new String(uinBytes);

		index += uinLength;
		
		boolean acceptedAuth = plainData[index] == 1;
		index += 1;
		
		service.log("Authorization reply from " + uin + ": " + (acceptedAuth ? "Granted" : "Declined"));
		
		if (index < plainData.length){
			short reasonLength = ProtocolUtils.bytes2ShortBE(plainData, index);
			index += 2;
			
			byte[] reasonBytes = new byte[reasonLength];
			System.arraycopy(plainData, index, reasonBytes, 0, reasonLength);
			String reason;
			try {
				reason = new String(reasonBytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				reason = new String(reasonBytes);
			}
			
			service.log("Reason: " + reason);
		}
		
		if (acceptedAuth) {
			service.getBuddyList().removeFromNotAuthListByUin(uin);
		}
	}
}
