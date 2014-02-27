package aceim.protocol.snuk182.vkontakte.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.text.TextUtils;

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

	static VkMessageAttachment[] fromJSONObject(JSONObject jo) {
		if (jo == null) return null;
		
		List<VkMessageAttachment> list = new ArrayList<VkMessageAttachment>();
		
		String fwd = jo.optString("fwd");
		long authorUid = jo.optLong("from");
		if (authorUid != 0) {
			list.add(new VkMessageAttachment(VkMessageAttachmentType.CHAT, "", fwd, authorUid));
		}

		int i = 1;

		String typeString;

		while (!TextUtils.isEmpty(typeString = jo.optString(String.format("attach%d_type", i)))) {
			String id = jo.optString(String.format("attach%d", i));
			
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

			list.add(new VkMessageAttachment(type, id, fwd, authorUid));
			i++;
		}

		return list.toArray(new VkMessageAttachment[list.size()]);
	}

	public enum VkMessageAttachmentType {
		AUDIO, VIDEO, PHOTO, DOC, CHAT, UNKNOWN
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
