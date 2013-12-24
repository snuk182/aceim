package aceim.protocol.snuk182.vkontakte.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import aceim.api.utils.Logger;

public class VkChat extends ApiObject {

	public VkChat(JSONObject jo){
		super(jo);
	}
	
	public VkChat(String json) {
		super(json);
	}

	public long getId() {
		return getJSONObject().optLong("chat_id");
	}
	
	public long getAdminId() {
		return getJSONObject().optLong("admin_id");
	}
	
	public String getTitle() {
		return getString("title");
	}
	
	public long[] getUsers() {
		JSONArray arr = getJSONObject().optJSONArray("users");
		
		long[] result = new long[arr.length()];
		
		for (int i=0; i<arr.length(); i++) {
			try {
				result[i] = arr.getLong(i);
			} catch (JSONException e) {
				Logger.log(e);
			}
		}
		
		return result;
	}
}
