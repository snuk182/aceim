package aceim.protocol.snuk182.vkontakte.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import aceim.api.utils.Logger;

public abstract class ApiObject {
	
	private final JSONObject jsonObject;
	
	ApiObject() {
		jsonObject = null;
	}

	ApiObject(String json) {
		JSONObject tmp;
		try {
			tmp = new JSONObject(json);
		} catch (JSONException e) {
			tmp = null;
		}
		jsonObject = tmp;
	}
	
	ApiObject(JSONObject jo) {
		jsonObject = jo;
	}

	protected JSONObject getJSONObject() {
		JSONObject response;
		try {
			if (jsonObject != null && (response = jsonObject.getJSONObject("response")) != null) {
				return response;
			}
		} catch (JSONException e) {
			//Logger.log("No response in JSONObject", LoggerLevel.VERBOSE);
		}
		
		return jsonObject;
	}

	public String getString(String key) {
		JSONObject jsonObject = getJSONObject();
		if (jsonObject != null) {
			return jsonObject.optString(key);
		}
		
		return null;
	}
	
	public static <T extends ApiObject> List<T> parseArray(String json, Class<T> cls){
		try {
			JSONArray array = new JSONObject(json).getJSONArray("response");

			List<T> list = new ArrayList<T>(array.length());

			for (int i = 0; i < array.length(); i++) {
				JSONObject jo = array.optJSONObject(i);
				
				T object = null;
				
				try {
					if (jo != null) {
						object = cls.getConstructor(JSONObject.class).newInstance(jo);					
					} else {
						Object o = array.get(i);
						object = cls.getConstructor(o.getClass()).newInstance(o);					
					}
				} catch (Exception e) {
					Logger.log(e);
				}
				
				if (object != null) {
					list.add(object);
				}
			}

			return list;
		} catch (Exception e) {
			Logger.log(e);
		}

		return Collections.emptyList();
	}
}
