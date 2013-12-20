package aceim.protocol.snuk182.vkontakte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.protocol.snuk182.vkontakte.model.VkBuddy;
import aceim.protocol.snuk182.vkontakte.model.VkBuddyGroup;
import aceim.protocol.snuk182.vkontakte.model.VkMessage;
import aceim.protocol.snuk182.vkontakte.model.VkMessageAttachment;
import aceim.protocol.snuk182.vkontakte.model.VkOnlineInfo;
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

	public static List<BuddyGroup> vkBuddiesAndGroups2BuddyList(List<VkBuddy> buddies, List<VkBuddyGroup> groups, List<VkOnlineInfo> onlineInfos, String ownerUid, Byte serviceId) {
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
			Buddy b = vkBuddy2Buddy(vkb, ownerUid, serviceId);
			
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

	public static Buddy vkBuddy2Buddy(VkBuddy vkb, String ownerUid, Byte serviceId) {
		if (vkb == null) {
			return null;
		}
		
		Buddy b = new Buddy(Long.toString(vkb.getUid()), ownerUid, VkConstants.PROTOCOL_NAME, serviceId);
		
		long groupId = vkb.getGroupId();
		b.setGroupId(groupId != 0 ? Long.toString(groupId) : ApiConstants.NO_GROUP_ID);
		
		String nick = vkb.getNickName();
		
		if (TextUtils.isEmpty(nick)) {
			String fn = vkb.getFirstName();
			String ln = vkb.getLastName();
			
			b.setName((TextUtils.isEmpty(fn) ? "" : fn) + " " + (TextUtils.isEmpty(ln) ? "" : ln));
		} else {
			b.setName(nick);
		}
		
		return b;
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
}
