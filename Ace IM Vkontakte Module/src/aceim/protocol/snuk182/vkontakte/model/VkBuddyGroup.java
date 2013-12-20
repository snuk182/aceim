package aceim.protocol.snuk182.vkontakte.model;

import org.json.JSONException;
import org.json.JSONObject;

import aceim.api.utils.Logger;

public class VkBuddyGroup extends ApiObject {

	public VkBuddyGroup(JSONObject jo){
		super(jo);
	}
	
	public String getName() {
		return super.getString("name");
	}
	
	public long getId() {
		try {
			return getJSONObject().getLong("lid");
		} catch (JSONException e) {
			Logger.log(e);
		}
		
		return 0;
	}
}
