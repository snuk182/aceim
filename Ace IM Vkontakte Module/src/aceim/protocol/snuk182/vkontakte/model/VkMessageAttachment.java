package aceim.protocol.snuk182.vkontakte.model;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressLint("DefaultLocale")
public class VkMessageAttachment extends ApiObject {
	
	private final VkMessageAttachmentType type;
	private final String id;
	private final String fwd;
	private final long authorId;
	
	private VkMessageAttachment(VkMessageAttachmentType type, String id, String fwd, long authorId) {
		super();
		this.type = type;
		this.id = id;
		this.fwd = fwd;
		this.authorId = authorId;
	}

	static VkMessageAttachment[] fromArray(JSONArray array) {
		VkMessageAttachment[] result = new VkMessageAttachment[array.length()];
		
		for (int i=0; i<result.length; i++) {
			result[i] = fromJSONObject(array.optJSONObject(i), i);
		}
		
		return result;
	}
	
	static VkMessageAttachment fromJSONObject(JSONObject jo, int i) {
		if (jo == null) return null;
		
		String typeString = jo.optString(String.format("attach%d_type", i));
		
		String id = jo.optString(String.format("attach%d", i));
		String fwd = jo.optString("fwd");
		long authorUid = jo.optLong("from");
		
		VkMessageAttachmentType type;
		if (TextUtils.isEmpty(typeString)) {
			if (authorUid != 0) {
				type = VkMessageAttachmentType.CHAT;
			} else {
				type = VkMessageAttachmentType.UNKNOWN;
			}
		} else {
			type = VkMessageAttachmentType.valueOf(typeString.toUpperCase());
		}
		
		return new VkMessageAttachment(type, id, fwd, authorUid);
	}

	public enum VkMessageAttachmentType {
		AUDIO,
		VIDEO,
		PHOTO,
		DOC,
		CHAT,
		UNKNOWN
	}

	/**
	 * @return the type
	 */
	public VkMessageAttachmentType getType() {
		return type;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the fwd
	 */
	public String getFwd() {
		return fwd;
	}

	/**
	 * @return the authorId
	 */
	public long getAuthorId() {
		return authorId;
	}	
}
