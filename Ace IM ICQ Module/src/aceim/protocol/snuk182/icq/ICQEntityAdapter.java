package aceim.protocol.snuk182.icq;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.ApiConstants;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMMessage;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyGroup;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQFileInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQPersonalInfo;
import aceim.protocol.snuk182.icq.utils.Base64;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

public final class ICQEntityAdapter {
	
	static final byte[] ACCOUNT_VISIBILITY_MAPPING = {ICQConstants.VIS_INVISIBLE, ICQConstants.VIS_TO_PERMITTED, ICQConstants.VIS_TO_BUDDIES, ICQConstants.VIS_EXCEPT_DENIED, ICQConstants.VIS_TO_ALL};
	static final byte[] BUDDY_VISIBILITY_MAPPING = {ICQConstants.VIS_DENIED, ICQConstants.VIS_PERMITTED};
	
	static final ICQBuddy buddy2ICQBuddy(Buddy buddy) {
		ICQBuddy icqBuddy = new ICQBuddy();
		icqBuddy.itemId = buddy.getId();
		if (buddy.getGroupId() != null) {
			icqBuddy.groupId = Integer.parseInt(buddy.getGroupId());
		}
		icqBuddy.screenName = buddy.getName();
		icqBuddy.uin = buddy.getProtocolUid();
		
		icqBuddy.visibility = buddyVisibility2IcqBuddyVisibility(buddy.getOnlineInfo());

		return icqBuddy;
	}

	static final ICQBuddyGroup buddyGroup2ICQBuddyGroup(BuddyGroup ggroup) {
		ICQBuddyGroup group = new ICQBuddyGroup();
		group.name = ggroup.getName();
		if (ggroup.getId() != null) {
			group.groupId = Integer.parseInt(ggroup.getId());
		}
		group.buddies = new ArrayList<Integer>(ggroup.getBuddyList().size());

		for (Buddy b : ggroup.getBuddyList()) {
			group.buddies.add(b.getId());
		}

		return group;
	}

	static final Buddy ICQBuddy2Buddy(ICQBuddy icqBuddy, String ownerUid, byte serviceId) {
		Buddy buddy = new Buddy(icqBuddy.uin, ownerUid, ICQService.SERVICE_NAME, serviceId);
		buddy.setName(icqBuddy.screenName);
		buddy.setId(icqBuddy.itemId);
		buddy.setGroupId(Integer.toString(icqBuddy.groupId));

		byte status;
		if ((icqBuddy.onlineInfo.userStatus & ICQConstants.STATUS_AWAY) > 0) {
			status = IcqApiConstants.STATUS_AWAY;
		} else if ((icqBuddy.onlineInfo.userStatus & ICQConstants.STATUS_NA) > 0) {
			status = IcqApiConstants.STATUS_NA;
		} else if ((icqBuddy.onlineInfo.userStatus & ICQConstants.STATUS_OCCUPIED) > 0) {
			status = IcqApiConstants.STATUS_BUSY;
		} else if ((icqBuddy.onlineInfo.userStatus & ICQConstants.STATUS_DND) > 0) {
			status = IcqApiConstants.STATUS_DND;
		} else if ((icqBuddy.onlineInfo.userStatus & ICQConstants.STATUS_FREE4CHAT) > 0) {
			status = IcqApiConstants.STATUS_FREE4CHAT;
		} else if ((icqBuddy.onlineInfo.userStatus & ICQConstants.STATUS_INVISIBLE) > 0) {
			status = IcqApiConstants.STATUS_INVISIBLE;
		} else if ((icqBuddy.onlineInfo.userStatus == ICQConstants.STATUS_OFFLINE)) {
			status = IcqApiConstants.STATUS_OFFLINE;
		} else {
			status = IcqApiConstants.STATUS_ONLINE;
		}
		
		boolean xstatusFound = false;
		if (icqBuddy.onlineInfo.capabilities != null) {
			for (int j = icqBuddy.onlineInfo.capabilities.size() - 1; j > -1; j--) {
				String cap = icqBuddy.onlineInfo.capabilities.get(j);

				if (!xstatusFound) {
					for (int i = 0; i < ICQConstants.XSTATUS_CLSIDS.length; i++) {
						String xClsid = ProtocolUtils.getHexString(ICQConstants.XSTATUS_CLSIDS[i]);
						if (xClsid.equalsIgnoreCase(cap)) {
							buddy.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_XSTATUS, (byte) i);
							xstatusFound = true;
							break;
						}
					}
				}

				if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_ANGRY))) {
					status = IcqApiConstants.STATUS_ANGRY;
					break;
				}
				if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_FREE4CHAT))) {
					status = IcqApiConstants.STATUS_FREE4CHAT;
					break;
				}
				if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_DEPRESSION))) {
					status = IcqApiConstants.STATUS_DEPRESS;
					break;
				}
				if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_HOME))) {
					status = IcqApiConstants.STATUS_HOME;
					break;
				}
				if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_LUNCH))) {
					status = IcqApiConstants.STATUS_DINNER;
					break;
				}
				if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_WORK))) {
					status = IcqApiConstants.STATUS_WORK;
					break;
				}
			}
		}
		buddy.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_STATUS, status);
		
		icqBuddyVisibility2BuddyVisibility(icqBuddy.visibility, buddy.getOnlineInfo());
		
		return buddy;
	}

	static void icqBuddyVisibility2BuddyVisibility(byte icqVisibility, OnlineInfo info) {
		switch(icqVisibility) {
		case ICQConstants.VIS_NOT_AUTHORIZED:
			info.getFeatures().putBoolean(IcqApiConstants.FEATURE_AUTHORIZATION, true);
			break;
		case ICQConstants.VIS_REGULAR:
			info.getFeatures().putByte(IcqApiConstants.FEATURE_BUDDY_VISIBILITY, (byte) -1);
			break;
		default:
			for (int i=0; i<BUDDY_VISIBILITY_MAPPING.length; i++) {
				if (BUDDY_VISIBILITY_MAPPING[i] == icqVisibility) {
					info.getFeatures().putByte(IcqApiConstants.FEATURE_BUDDY_VISIBILITY, (byte) i);
					break;
				}
			}
			break;	
		}
	}
	
	static byte buddyVisibility2IcqBuddyVisibility(OnlineInfo info){
		byte v = info.getFeatures().getByte(IcqApiConstants.FEATURE_BUDDY_VISIBILITY, (byte) -1);
		
		if (v < 0 || v >= BUDDY_VISIBILITY_MAPPING.length) {
			return ICQConstants.VIS_REGULAR;
		} else {
			return BUDDY_VISIBILITY_MAPPING[v];
		}
	}

	static final BuddyGroup ICQBuddyGroup2BuddyGroup(ICQBuddyGroup icqGroup, String ownerUid, byte serviceId, List<ICQBuddy> icqbuddies, List<Buddy> buddies) {
		BuddyGroup group = new BuddyGroup(Integer.toString(icqGroup.groupId), ownerUid, serviceId);
		group.setName(icqGroup.name);
		
		if (buddies != null) {
			for (Buddy b : buddies) {
				if (b.getGroupId().equals(group.getId())) {
					group.getBuddyList().add(b);
				}
			}
		} else {
			for (ICQBuddy b : icqbuddies) {
				if (b.groupId == icqGroup.groupId) {
					group.getBuddyList().add(ICQBuddy2Buddy(b, ownerUid, serviceId));
				}
			}
		}

		return group;
	}

	static final List<Buddy> ICQBuddyList2Buddylist(List<ICQBuddy> buddyList, String ownerUid, byte serviceId) {
		List<Buddy> buddies = new ArrayList<Buddy>();
		for (ICQBuddy icqBuddy : buddyList) {
			buddies.add(ICQBuddy2Buddy(icqBuddy, ownerUid, serviceId));
		}
		return buddies;
	}

	static final List<ICQBuddy> buddyList2ICQBuddyList(List<Buddy> buddies) {
		if (buddies == null) {
			return null;
		}
		List<ICQBuddy> icqBuddies = new ArrayList<ICQBuddy>(buddies.size());
		for (Buddy buddy : buddies) {
			icqBuddies.add(buddy2ICQBuddy(buddy));
		}
		return icqBuddies;
	}

	static final List<ICQBuddyGroup> buddyGroupList2ICQBuddyGroupList(List<BuddyGroup> groups) {
		if (groups == null) {
			return null;
		}
		List<ICQBuddyGroup> icqGroups = new ArrayList<ICQBuddyGroup>(groups.size());
		for (BuddyGroup group : groups) {
			icqGroups.add(buddyGroup2ICQBuddyGroup(group));
		}
		return icqGroups;
	}

	static final List<BuddyGroup> ICQBuddyGroupList2BuddyGroupList(List<ICQBuddyGroup> groupList, String ownerId, byte serviceId, List<ICQBuddy> icqBuddies, List<Buddy> buddies) {
		List<BuddyGroup> groups = new ArrayList<BuddyGroup>();
		for (ICQBuddyGroup icqGroup : groupList) {
			groups.add(ICQEntityAdapter.ICQBuddyGroup2BuddyGroup(icqGroup, ownerId, serviceId, icqBuddies, buddies));
		}
		return groups;
	}

	static final ICBMMessage textMessage2ICBMMessage(TextMessage txtMessage) {
		if (txtMessage == null)
			return null;
		ICBMMessage msg = new ICBMMessage();
		msg.text = txtMessage.getText();
		msg.receiverId = txtMessage.getContactUid();
		msg.messageType = ICQConstants.MTYPE_PLAIN;
		msg.messageId = txtMessage.getMessageId()!=0 ? ProtocolUtils.long2ByteBE(txtMessage.getMessageId()) : null;
		return msg;
	}

	static final TextMessage icbmMessage2TextMessage(ICBMMessage msg, byte serviceId) {
		if (msg == null)
			return null;
		TextMessage txtMsg = new TextMessage(serviceId, msg.senderId);
		txtMsg.setText(msg.text);
		txtMsg.setIncoming(true);
		txtMsg.setMessageId(ProtocolUtils.bytes2LongBE(msg.messageId));
		if (msg.receivingTime != null) {
			txtMsg.setTime(msg.receivingTime.getTime());
		} else {
			txtMsg.setTime(System.currentTimeMillis());
		}
		
		txtMsg.setMessageId(ProtocolUtils.bytes2LongBE(msg.messageId, 0));
		return txtMsg;
	}

	static final ICQOnlineInfo onlineInfo2ICQOnlineInfo(OnlineInfo info, String accountUin) {
		if (info == null) {
			return null;
		}
		
		ICQOnlineInfo out = new ICQOnlineInfo();
		out.createTime = info.getCreateTime();
		out.extIP = info.getExtIP();
		out.idleTime = info.getIdleTime();
		out.memberSinceTime = info.getMemberSinceTime();
		out.name = info.getName();
		out.onlineTime = (int) info.getOnlineTime();
		out.signonTime = info.getSignonTime();
		out.uin = info.getProtocolUid();
		out.personalText = info.getXstatusName();
		out.extendedStatus = info.getXstatusDescription();
		out.extendedStatusId = info.getFeatures().getByte(ApiConstants.FEATURE_XSTATUS, ICQOnlineInfo.NO_XSTATUS_ID);

		out.userStatus = 0;
		out.qipStatus = null;
		byte status = info.getFeatures().getByte(ApiConstants.FEATURE_STATUS);
		out.userStatus = ICQEntityAdapter.userStatus2ICQUserStatus(status);
		if (out.userStatus < 0) {
			out.userStatus = ICQConstants.STATUS_ONLINE;
			out.qipStatus = ICQEntityAdapter.userQipStatus2ICQQipStatus(status);
		} else {
			out.qipStatus = null;
		}
		
		if (accountUin.equals(info.getProtocolUid())) {
			byte val = info.getFeatures().getByte(IcqApiConstants.FEATURE_ACCOUNT_VISIBILITY, (byte) 0);
			/*if (val < 0) {
				val = 0;
			}*/
			out.visibility = ACCOUNT_VISIBILITY_MAPPING[val];
		} else {
			out.visibility = buddyVisibility2IcqBuddyVisibility(info);
		}
		
		return out;
	}
	
	static final OnlineInfo icqOnlineInfo2OnlineInfo(ICQOnlineInfo in, String accountUin, byte serviceId) {
		if (in == null)
			return null;
		OnlineInfo out = new OnlineInfo(serviceId, in.uin);
		
		out.setCreateTime(in.createTime);
		out.setExtIP(in.extIP);
		out.setIdleTime(in.idleTime);
		out.setMemberSinceTime(in.memberSinceTime);
		out.setName(in.name);
		out.setOnlineTime(in.onlineTime);
		out.setSignonTime(in.signonTime);
		out.setXstatusName(in.personalText);
		out.setXstatusDescription(in.extendedStatus);

		if (in.iconData != null && in.iconData.iconId == 1 && in.iconData.flags == 1) {
			out.setIconHash(Base64.encodeBytes(in.iconData.hash));
		}

		byte userStatus;
		if ((in.userStatus & ICQConstants.STATUS_AWAY) > 0) {
			userStatus = IcqApiConstants.STATUS_AWAY;
		} else if ((in.userStatus & ICQConstants.STATUS_NA) > 0) {
			userStatus = IcqApiConstants.STATUS_NA;
		} else if ((in.userStatus & ICQConstants.STATUS_OCCUPIED) > 0) {
			userStatus = IcqApiConstants.STATUS_BUSY;
		} else if ((in.userStatus & ICQConstants.STATUS_DND) > 0) {
			userStatus = IcqApiConstants.STATUS_DND;
		} else if ((in.userStatus & ICQConstants.STATUS_INVISIBLE) > 0) {
			userStatus = IcqApiConstants.STATUS_INVISIBLE;
		} else if ((in.userStatus & ICQConstants.STATUS_FREE4CHAT) > 0) {
			userStatus = IcqApiConstants.STATUS_FREE4CHAT;
		} else if ((in.userStatus == ICQConstants.STATUS_OFFLINE)) {
			userStatus = IcqApiConstants.STATUS_OFFLINE;
		} else {
			userStatus = IcqApiConstants.STATUS_ONLINE;
		}
		
		boolean xstatusFound = in.extendedStatusId > -1;
		boolean statusFound = false;
		out.getFeatures().putByte(ApiConstants.FEATURE_XSTATUS, in.extendedStatusId);
		
		String canFileShare = ProtocolUtils.getHexString(ICQConstants.CLSID_AIM_FILESEND);

		if (in.capabilities != null) {
			for (int j = in.capabilities.size() - 1; j > -1; j--) {
				String cap = in.capabilities.get(j);

				if (cap.equals(canFileShare)) {
					out.getFeatures().putBoolean(ApiConstants.FEATURE_FILE_TRANSFER, true);
					continue;
				}

				if (statusFound && xstatusFound) {
					continue;
				}

				if (!xstatusFound) {
					for (int i = 0; i < ICQConstants.XSTATUS_CLSIDS.length; i++) {
						String xClsid = ProtocolUtils.getHexString(ICQConstants.XSTATUS_CLSIDS[i]);
						if (xClsid.equalsIgnoreCase(cap)) {
							out.getFeatures().putByte(ApiConstants.FEATURE_XSTATUS, (byte) i);
							xstatusFound = true;
							break;
						}
					}
				}

				if (!statusFound) {
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_ANGRY))) {
						userStatus = IcqApiConstants.STATUS_ANGRY;
						statusFound = true;
					}
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_FREE4CHAT))) {
						userStatus = IcqApiConstants.STATUS_FREE4CHAT;
						statusFound = true;
					}
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_DEPRESSION))) {
						userStatus = IcqApiConstants.STATUS_DEPRESS;
						statusFound = true;
					}
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_HOME))) {
						userStatus = IcqApiConstants.STATUS_HOME;
						statusFound = true;
					}
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_LUNCH))) {
						userStatus = IcqApiConstants.STATUS_DINNER;
						statusFound = true;
					}
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_STATUS_WORK))) {
						userStatus = IcqApiConstants.STATUS_WORK;
						statusFound = true;
					}
				}
			}
		}

		out.getFeatures().putByte(ApiConstants.FEATURE_STATUS, userStatus);
		
		if (userStatus != IcqApiConstants.STATUS_OFFLINE) {
			out.getFeatures().putBoolean(ApiConstants.FEATURE_FILE_TRANSFER, true);
		}
		
		if (accountUin.equals(in.uin)) {
			for (int i=0; i<ACCOUNT_VISIBILITY_MAPPING.length; i++) {
				if (in.visibility == ACCOUNT_VISIBILITY_MAPPING[i]){
					out.getFeatures().putByte(IcqApiConstants.FEATURE_ACCOUNT_VISIBILITY, (byte) i);
					break;
				}
			}
			out.getFeatures().putBoolean(ApiConstants.FEATURE_BUDDY_MANAGEMENT, true);
			out.getFeatures().putBoolean(ApiConstants.FEATURE_GROUP_MANAGEMENT, true);
			out.getFeatures().putBoolean(ApiConstants.FEATURE_ACCOUNT_MANAGEMENT, true);
			out.getFeatures().putBoolean(IcqApiConstants.FEATURE_BUDDY_SEARCH, true);
		} else {
			icqBuddyVisibility2BuddyVisibility(in.visibility, out);
		}
		
		return out;
	}

	static final int userStatus2ICQUserStatus(Byte status) {
		switch (status) {
		case IcqApiConstants.STATUS_AWAY:
			return ICQConstants.STATUS_AWAY;
		case IcqApiConstants.STATUS_BUSY:
			return ICQConstants.STATUS_OCCUPIED;
		case IcqApiConstants.STATUS_DND:
			return ICQConstants.STATUS_DND;
		case IcqApiConstants.STATUS_INVISIBLE:
			return ICQConstants.STATUS_INVISIBLE;
		case IcqApiConstants.STATUS_NA:
			return ICQConstants.STATUS_NA;
		case IcqApiConstants.STATUS_ONLINE:
			return ICQConstants.STATUS_ONLINE;
			/*
			 * case Buddy.ST_FREE4CHAT: return ICQConstants.STATUS_FREE4CHAT;
			 */
		default:
			return -1;
		}
	}

	static final byte[] userQipStatus2ICQQipStatus(Byte status) {
		switch (status) {
		case IcqApiConstants.STATUS_FREE4CHAT:
			return ICQConstants.CLSID_STATUS_FREE4CHAT;
		case IcqApiConstants.STATUS_ANGRY:
			return ICQConstants.CLSID_STATUS_ANGRY;
		case IcqApiConstants.STATUS_DEPRESS:
			return ICQConstants.CLSID_STATUS_DEPRESSION;
		case IcqApiConstants.STATUS_DINNER:
			return ICQConstants.CLSID_STATUS_LUNCH;
		case IcqApiConstants.STATUS_HOME:
			return ICQConstants.CLSID_STATUS_HOME;
		case IcqApiConstants.STATUS_WORK:
			return ICQConstants.CLSID_STATUS_WORK;
		default:
			return null;
		}
	}

	static final PersonalInfo icqPersonalInfo2PersonalInfo(ICQPersonalInfo icqInfo, Context context, byte serviceId) {
		PersonalInfo info = new PersonalInfo(serviceId);
		info.setProtocolUid(icqInfo.uin);

		// ICQ is 1(female), 2(male)
		String gender;
		
		switch(icqInfo.gender) {
		case 1:
			gender = "female";
			break;
		case 2:
			gender = "male";
			break;
		default:
			gender = null;
			break;
		}

		Bundle bundle = new Bundle();
		bundle.putString(PersonalInfo.INFO_EMAIL, icqInfo.email);
		bundle.putString(PersonalInfo.INFO_FIRST_NAME, icqInfo.firstName);
		bundle.putString(PersonalInfo.INFO_LAST_NAME, icqInfo.lastName);
		bundle.putString(PersonalInfo.INFO_NICK, icqInfo.nickname);
		bundle.putString(PersonalInfo.INFO_GENDER, gender);
		bundle.putString(PersonalInfo.INFO_AGE, Integer.toString(icqInfo.age));
		bundle.putString(PersonalInfo.INFO_STATUS, Integer.toString(icqInfo.status));
		bundle.putString(PersonalInfo.INFO_REQUIRES_AUTH, icqInfo.authRequired > 0 ? "Yes" : "No");

		Set<String> names = icqInfo.params.keySet();
		for (String name : names) {
			if (name.indexOf("ountry") > -1) {
				String[] countryNames = context.getResources().getStringArray(R.array.icq_country_names);
				int[] countryCodes = context.getResources().getIntArray(R.array.icq_country_codes);

				try {
					int code = (Integer) icqInfo.params.get(name);
					for (int i = 0; i < countryCodes.length; i++) {
						if (countryCodes[i] == code) {
							bundle.putString(name, countryNames[i]);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (name.indexOf("ccupation") > -1) {
				String[] occuNames = context.getResources().getStringArray(R.array.icq_occupation_names);
				int[] occuCodes = context.getResources().getIntArray(R.array.icq_occupation_codes);

				try {
					int code = (Integer) icqInfo.params.get(name);
					for (int i = 0; i < occuCodes.length; i++) {
						if (occuCodes[i] == code) {
							bundle.putString(name, occuNames[i]);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (name.indexOf("anguage") > -1) {
				String[] langNames = context.getResources().getStringArray(R.array.icq_language_names);
				int[] langCodes = context.getResources().getIntArray(R.array.icq_language_codes);

				try {
					int code = (Integer) icqInfo.params.get(name);
					for (int i = 0; i < langCodes.length; i++) {
						if (langCodes[i] == code) {
							bundle.putString(name, langNames[i]);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (name.indexOf("GMT") > -1) {
				String[] gmtNames = context.getResources().getStringArray(R.array.icq_gmt_names);
				int[] gmtCodes = context.getResources().getIntArray(R.array.icq_gmt_codes);

				try {
					int code = (Integer) icqInfo.params.get(name);
					for (int i = 0; i < gmtCodes.length; i++) {
						if (gmtCodes[i] == code) {
							bundle.putString(name, gmtNames[i]);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (name.indexOf("Family status") > -1) {
				String[] maritalNames = context.getResources().getStringArray(R.array.icq_marital_names);
				int[] maritalCodes = context.getResources().getIntArray(R.array.icq_marital_codes);

				try {
					int code = (Integer) icqInfo.params.get(name);
					for (int i = 0; i < maritalCodes.length; i++) {
						if (maritalCodes[i] == code) {
							bundle.putString(name, maritalNames[i]);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (name.indexOf("past ") > -1) {
				String past = name.split("past ")[1];
				try {
					int pastId = Integer.parseInt(past);
					String[] pastNames = context.getResources().getStringArray(R.array.icq_past_names);
					int[] pastCodes = context.getResources().getIntArray(R.array.icq_past_codes);

					for (int i = 0; i < pastCodes.length; i++) {
						if (pastCodes[i] == pastId) {
							bundle.putString(pastNames[i], (String) icqInfo.params.get(name));
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (name.indexOf("affiliation ") > -1) {
				String aff = name.split("affiliation ")[1];
				try {
					int affId = Integer.parseInt(aff);
					String[] affNames = context.getResources().getStringArray(R.array.icq_affiliation_names);
					int[] affCodes = context.getResources().getIntArray(R.array.icq_affiliation_codes);

					for (int i = 0; i < affCodes.length; i++) {
						if (affCodes[i] == affId) {
							bundle.putString(affNames[i], (String) icqInfo.params.get(name));
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				bundle.putString(name, (String) icqInfo.params.get(name));
			}
		}

		Log.d("", bundle.toString());

		info.setProperties(bundle);
		return info;
	}

	static final List<PersonalInfo> icqPersonalInfos2PersonalInfos(List<ICQPersonalInfo> icqinfos, Context context, byte serviceId) {
		if (icqinfos == null) {
			return null;
		}
		List<PersonalInfo> infos = new ArrayList<PersonalInfo>(icqinfos.size());
		for (ICQPersonalInfo info : icqinfos) {
			infos.add(icqPersonalInfo2PersonalInfo(info, context, serviceId));
		}
		return infos;
	}

	static final FileMessage icbmMessage2FileMessage(ICBMMessage icbmMessage, byte serviceId) {
		if (icbmMessage == null)
			return null;

		FileMessage message = new FileMessage(serviceId, icbmMessage.senderId);
		message.setMessageId(ProtocolUtils.bytes2LongBE(icbmMessage.messageId, 0));
		message.getFiles().addAll(icqFileInfoList2FileInfoList(icbmMessage.files, serviceId));
		message.setTime(icbmMessage.receivingTime.getTime());
		message.setIncoming(true);
		return message;
	}

	static final ICBMMessage fileMessage2IcbmMessage(FileMessage message, String senderId) {
		if (message == null) {
			return null;
		}

		ICBMMessage imsg = new ICBMMessage();
		imsg.messageId = message.getMessageId()!=0 ? ProtocolUtils.long2ByteBE(message.getMessageId()) : null;
		imsg.sendingTime = new Date(message.getTime());
		imsg.receiverId = message.getContactUid();
		imsg.senderId = senderId;

		imsg.files.addAll(fileInfoList2IcqFileInfoList(message.getFiles()));

		return imsg;
	}

	private static final List<ICQFileInfo> fileInfoList2IcqFileInfoList(List<FileInfo> files) {
		if (files == null) {
			return null;
		}

		List<ICQFileInfo> ifiles = new ArrayList<ICQFileInfo>(files.size());

		for (FileInfo fi : files) {
			ifiles.add(fileInfo2IcqFileInfo(fi));
		}
		return ifiles;
	}

	private static final ICQFileInfo fileInfo2IcqFileInfo(FileInfo fi) {
		ICQFileInfo ifi = new ICQFileInfo();
		ifi.filename = fi.getFilename();
		ifi.size = fi.getSize();

		return ifi;
	}

	private static List<FileInfo> icqFileInfoList2FileInfoList(List<ICQFileInfo> files, byte serviceId) {
		if (files == null) {
			return null;
		}

		List<FileInfo> infos = new ArrayList<FileInfo>(files.size());
		for (ICQFileInfo file : files) {
			FileInfo info = new FileInfo(serviceId);
			info.setFilename(file.filename);
			info.setSize(file.size);

			infos.add(info);
		}
		return infos;
	}

	static MessageAckState icqMessageAck2MessageAck(byte ack) {
		switch (ack) {
		case 2:
			return MessageAckState.RECIPIENT_ACK;
		default:
			return MessageAckState.SERVER_ACK;
		}
	}

	static ConnectionState icqConnectionState2ConnectionState(byte currentState) {
		switch (currentState) {
		case ICQServiceInternal.STATE_DISCONNECTED:
			return ConnectionState.DISCONNECTED;
		case ICQServiceInternal.STATE_CONNECTED:
			return ConnectionState.CONNECTED;
		default:
			return ConnectionState.CONNECTING;
		}
	}

	public static Map<String, String> searchTKVListToMap(Parcelable[] p, Context context) {
		Map<String, String> map = new HashMap<String, String>(p.length);
		
		String uinKey = context.getString(R.string.uin);
		String screenName = context.getString(R.string.screenname);
		
		for (int i=0; i<p.length; i++) {
			TKV tkv = (TKV) p[i];
			
			if (!TextUtils.isEmpty(tkv.getValue())) {
				if (uinKey.equals(tkv.getKey())) {
					map.put(ICQConstants.SEARCHPARAM_UIN, tkv.getValue());
				} else if (screenName.equals(tkv.getKey())) {
					map.put(screenName, tkv.getValue());
				} else {
					map.put(tkv.getKey(), tkv.getValue());
				}
			}
		}
		
		return map;
	}

	public static Message authRequestToServiceMessage(byte serviceId, String from, String reasonText, Context context) {
		ServiceMessage message = new ServiceMessage(serviceId, from, true);
		message.setText(reasonText);
		message.setContactDetail(context.getString(R.string.ask_authorization));
		return message;
	}
}
