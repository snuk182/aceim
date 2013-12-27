package aceim.protocol.snuk182.vkontakte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.protocol.snuk182.vkontakte.model.VkBuddy;
import aceim.protocol.snuk182.vkontakte.model.VkBuddyGroup;
import aceim.protocol.snuk182.vkontakte.model.VkChat;
import aceim.protocol.snuk182.vkontakte.model.VkMessage;
import aceim.protocol.snuk182.vkontakte.model.VkMessageAttachment;
import aceim.protocol.snuk182.vkontakte.model.VkOnlineInfo;
import android.os.Bundle;
import android.text.TextUtils;

public final class VkEntityAdapter {

	private VkEntityAdapter(){}

	public static List<NameValuePair> map2NameValuePairs(Map<String, String> params) {
		if (params == null) return null;
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
		
		for (String key : params.keySet()) {
			pairs.add(new BasicNameValuePair(key, params.get(key)));			
		}
		
		return pairs;
	}

	public static List<BuddyGroup> vkBuddiesAndGroups2BuddyList(List<VkBuddy> buddies, List<VkBuddyGroup> groups, List<VkOnlineInfo> onlineInfos, long myId, String ownerUid, Byte serviceId) {
		if (buddies == null) {
			return null;
		}
		
		Map<String, BuddyGroup> result = new HashMap<String,BuddyGroup>(groups.size() + 1);
		BuddyGroup noGroup = new BuddyGroup(ApiConstants.NO_GROUP_ID, ownerUid, serviceId);
		
		for (VkBuddyGroup vkb : groups) {
			BuddyGroup bg = vkBuddyGroup2BuddyGroup(vkb, ownerUid, serviceId);
			result.put(bg.getId(), bg);
		}
		
		for (VkBuddy vkb : buddies) {
			Buddy b = vkBuddy2Buddy(vkb, myId, ownerUid, serviceId);
			
			for (VkOnlineInfo vki : onlineInfos) {
				if (vki.getUid() == vkb.getUid()) {
					b.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_STATUS, vki.getStatus());
					break;
				}
			}
			
			if (b.getGroupId().equals(ApiConstants.NO_GROUP_ID)) {
				noGroup.getBuddyList().add(b);
			} else {
				result.get(b.getGroupId()).getBuddyList().add(b);
			}
		}
		
		if (noGroup.getBuddyList().size() > 0) {
			result.put(ApiConstants.NO_GROUP_ID, noGroup);
		}
		
		return Collections.unmodifiableList(new ArrayList<BuddyGroup>(result.values()));
	}
	
	public static List<OnlineInfo> vkOnlineInfos2OnlineInfos(List<VkOnlineInfo> vkOnlineInfos, long myId, String ownerUid, Byte serviceId) {
		if (vkOnlineInfos == null) return null;
		
		List<OnlineInfo> infos = new ArrayList<OnlineInfo>(vkOnlineInfos.size());
		for (VkOnlineInfo vko : vkOnlineInfos) {
			OnlineInfo info = new OnlineInfo(serviceId, vko.getUid() == myId ? ownerUid : Long.toString(vko.getUid()));
			info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
			infos.add(info);
		}
		
		return infos;
	}

	public static Buddy vkBuddy2Buddy(VkBuddy vkb, long myId, String ownerUid, Byte serviceId) {
		if (vkb == null) {
			return null;
		}
		
		Buddy b = new Buddy(vkb.getUid() != myId ? Long.toString(vkb.getUid()) : ownerUid, ownerUid, VkConstants.PROTOCOL_NAME, serviceId);
		
		long groupId = vkb.getGroupId();
		b.setGroupId(groupId != 0 ? Long.toString(groupId) : ApiConstants.NO_GROUP_ID);
		
		b.setName(getNickOfVkBuddy(vkb));
		
		return b;
	}

	private static String getNickOfVkBuddy(VkBuddy vkb) {
		String nick = vkb.getNickName();
		
		if (TextUtils.isEmpty(nick)) {
			String fn = vkb.getFirstName();
			String ln = vkb.getLastName();
			
			return (TextUtils.isEmpty(fn) ? "" : fn) + " " + (TextUtils.isEmpty(ln) ? "" : ln);
		} else {
			return nick;
		}
	}

	public static BuddyGroup vkBuddyGroup2BuddyGroup(VkBuddyGroup vkb, String ownerUid, Byte serviceId) {
		if (vkb == null) {
			return null;
		}
		
		BuddyGroup bg = new BuddyGroup(Long.toString(vkb.getId()), ownerUid, serviceId);
		bg.setName(vkb.getName());
		
		return bg;
	}

	public static OnlineInfo vkOnlineInfo2OnlineInfo(VkOnlineInfo vi, byte serviceId) {
		if (vi == null) return null;
		
		OnlineInfo info = new OnlineInfo(serviceId, Long.toString(vi.getUid()));
		info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, vi.getStatus());
		
		return info;
	}

	public static Message vkMessage2Message(VkMessage vkm, byte serviceId) {
		if (vkm == null) return null;
		
		//TODO support for other message types
		TextMessage tm = new TextMessage(serviceId, Long.toString(vkm.getPartnerId()));
		tm.setTime(vkm.getTimestamp());
		tm.setIncoming(true);
		tm.setMessageId(vkm.getMessageId());
		tm.setText(vkm.getText());
		
		for (VkMessageAttachment attachment : vkm.getAttachments()) {
			if (attachment.getAuthorId() != 0) {
				tm.setContactDetail(Long.toString(attachment.getAuthorId()));
			}			
		}
		
		return tm;
	}

	public static VkMessage textMessage2VkMessage(TextMessage message, boolean isChat) {		
		VkMessage vkm = new VkMessage(0, Long.parseLong(message.getContactUid()), isChat ? 16 : 0, System.currentTimeMillis(), null, message.getText(), null);
		return vkm;
	}

	public static PersonalInfo vkBuddy2PersonalInfo(VkBuddy vkb, byte serviceId, String ownerUid) {
		if (vkb == null) return null;
		
		PersonalInfo info = new PersonalInfo(serviceId);
		info.setProtocolUid(ownerUid != null ? ownerUid : Long.toString(vkb.getUid()));

		Bundle bundle = new Bundle();
		bundle.putString(PersonalInfo.INFO_NICK, getNickOfVkBuddy(vkb));
		bundle.putString(PersonalInfo.INFO_FIRST_NAME, vkb.getFirstName());
		bundle.putString(PersonalInfo.INFO_LAST_NAME, vkb.getLastName());
		info.setProperties(bundle);
		
		return info;
	}

	public static List<PersonalInfo> vkChats2PersonalInfoList(List<VkChat> chats, byte serviceId) {
		if (chats == null) return null;
		
		List<PersonalInfo> pinfoList = new ArrayList<PersonalInfo>(chats.size());
		for (VkChat vkChat : chats) {
			PersonalInfo pinfo = vkChat2PersonalInfo(vkChat, serviceId);
			if (pinfo != null) {
				pinfoList.add(pinfo);
			}
		}
		
		return pinfoList;
	}

	private static PersonalInfo vkChat2PersonalInfo(VkChat vkChat, byte serviceId) {
		if (vkChat == null) return null;
		
		PersonalInfo pinfo = new PersonalInfo(serviceId);
		pinfo.setProtocolUid(Long.toString(vkChat.getId()));
		pinfo.setMultichat(true);
		pinfo.getProperties().putString(PersonalInfo.INFO_NICK, vkChat.getTitle());
		
		return pinfo;
	}

	public static MultiChatRoom vkChat2MultiChatRoom(VkChat vkChat, String ownerUid, byte serviceId) {
		if (vkChat == null) return null;
		
		MultiChatRoom chat = new MultiChatRoom(Long.toString(vkChat.getId()), ownerUid, VkConstants.PROTOCOL_NAME, serviceId);
		chat.setName(vkChat.getTitle());
		
		return chat;
	}

	public static List<BuddyGroup> vkChatOccupants2ChatOccupants(VkChat vkChat, List<VkBuddy> occupants, long myId, String ownerUid, Byte serviceId) {
		if (occupants == null) return null;
		
		BuddyGroup moderators = new BuddyGroup(Integer.toString(1), ownerUid, serviceId);
		BuddyGroup all = new BuddyGroup(Integer.toString(0), ownerUid, serviceId);
		moderators.setName("Moderators");
		all.setName("All");
		
		for (VkBuddy vkBuddy : occupants) {
			Buddy buddy = vkBuddy2Buddy(vkBuddy, myId, ownerUid, serviceId);
			if (vkBuddy.getUid() == vkChat.getAdminId()) {
				moderators.getBuddyList().add(buddy);
			} else {
				all.getBuddyList().add(buddy);
			}
		}		
		
		return Arrays.asList(moderators, all);
	}

	public static Message vkChatMessage2Message(long chatId, VkMessage vkm, byte serviceId) {
		if (vkm == null) return null;
		
		//TODO support for other message types
		TextMessage tm = new TextMessage(serviceId, Long.toString(chatId));
		tm.setContactDetail(Long.toString(vkm.getPartnerId()));
		tm.setTime(vkm.getTimestamp());
		tm.setIncoming(true);
		tm.setMessageId(vkm.getMessageId());
		tm.setText(vkm.getText());
		
		for (VkMessageAttachment attachment : vkm.getAttachments()) {
			if (attachment.getAuthorId() != 0) {
				tm.setContactDetail(Long.toString(attachment.getAuthorId()));
			}			
		}
		
		return tm;
	}
}
