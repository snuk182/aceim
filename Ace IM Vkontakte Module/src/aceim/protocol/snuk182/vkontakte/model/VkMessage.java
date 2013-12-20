package aceim.protocol.snuk182.vkontakte.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import aceim.api.utils.Logger;
import aceim.protocol.snuk182.vkontakte.model.LongPollResponse.LongPollResponseUpdate;


public class VkMessage extends ApiObject {

	private final long messageId;
	private final long partnerId;
	private final int flags;
	private final long timestamp;
	private final String subject;
	private final String text;
	private final VkMessageAttachment[] attachments;
	
	public static VkMessage fromLongPollUpdate(LongPollResponseUpdate response) {
		if (response == null) return null;
		
		long messageId = response.getId();
		int flags = Integer.parseInt(response.getParams()[0]);
		long partnerId = Long.parseLong(response.getParams()[1]);
		long timestamp = Long.parseLong(response.getParams()[2]);
		String subject = response.getParams()[3];
		String text = response.getParams()[4];
		
		return new VkMessage(messageId, partnerId, flags, timestamp, subject, text, null);
	}

	public VkMessage(long messageId, long partnerId, int flags, long timestamp, String subject, String text, VkMessageAttachment[] attachments) {
		super();
		this.messageId = messageId;
		this.partnerId = partnerId;
		this.flags = flags;
		this.timestamp = timestamp;
		this.subject = subject;
		this.text = text;
		this.attachments = attachments != null ? attachments : new VkMessageAttachment[0];
	}
	
	@Override
	public String toString() {
		JSONObject jo = new JSONObject();
		try {
			jo.put("message", text);
			
			if ((flags & 16) > 0) {
				jo.put("chat_id", partnerId);
			} else {
				jo.put("uid", partnerId);
			}
			
			return jo.toString();
		} catch (JSONException e) {
			Logger.log(e);
			return null;
		}
	}
	
	public Map<String, String> toParamsMap() {
		Map<String, String> map = new HashMap<String, String>(3);
		
		if ((flags & 16) > 0) {
			map.put("chat_id", Long.toString(partnerId));
		} else {
			map.put("uid", Long.toString(partnerId));
		}
				
		map.put("message", text);
		
		return map;
	}
	
	public boolean isUnread() {
		return (flags & 1) > 0; 
	}
	
	public boolean isOutgoing() {
		return (flags & 2) > 0; 
	}
	
	public boolean isReplied() {
		return (flags & 4) > 0; 
	}
	
	public boolean isImportant() {
		return (flags & 8) > 0; 
	}
	
	public boolean isChat() {
		return (flags & 16) > 0; 
	}
	
	public boolean isFriends() {
		return (flags & 32) > 0; 
	}
	
	public boolean isSpam() {
		return (flags & 64) > 0; 
	}
	
	public boolean isDeleted() {
		return (flags & 128) > 0; 
	}
	
	public boolean isFixed() {
		return (flags & 256) > 0; 
	}
	
	public boolean isMedia() {
		return (flags & 512) > 0; 
	}

	/**
	 * @return the messageId
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * @return the partnerId
	 */
	public long getPartnerId() {
		return partnerId;
	}

	/**
	 * @return the flags
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return the attachments
	 */
	public VkMessageAttachment[] getAttachments() {
		return attachments;
	}
}
