package aceim.protocol.snuk182.vkontakte.model;

import org.json.JSONObject;

public class VkBuddy extends ApiObject {

	public VkBuddy(JSONObject jo) {
		super(jo);
	}
	
	public long getUid() {
		return super.getJSONObject().optLong("uid");
	}
	
	public String getFirstName() {
		return super.getString("first_name");
	}
	
	public String getLastName() {
		return super.getString("last_name");
	}
	
	public String getNickName() {
		return super.getString("nickname");
	}
	
	public String getPhotoPath() {
		return super.getString("photo_big");
	}
	
	public long getGroupId() {
		return getJSONObject().optLong("lid");
	}
}
