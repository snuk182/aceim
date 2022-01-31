package aceim.protocol.snuk182.vkontakte.model;

import org.json.JSONArray;
import org.json.JSONException;

import aceim.api.utils.Logger;

public class LongPollResponse extends ApiObject {

	public LongPollResponse(String json) throws JSONException {
		super(json);
	}

	public String getTs() {
		return super.getString("ts");
	}
	
	public boolean isConnectionDead() {
		try {
			String failed = getJSONObject().getString("failed");
			return "2".equals(failed);
		} catch (JSONException e) {}
		
		return false;
	}
	
	public String getUpdatesJSON() {
		try {
			return getJSONObject().getJSONArray("updates").toString();
		} catch (JSONException e) {
			Logger.log(e);
			return "";
		}
	}
	
	public LongPollResponseUpdate[] getUpdates() {
		try {
			JSONArray array = getJSONObject().getJSONArray("updates");
			
			LongPollResponseUpdate[] updates = new LongPollResponseUpdate[array.length()];
			
			for (int i = 0; i < array.length(); i++) {
				LongPollResponseUpdate update = LongPollResponseUpdate.fromParamsArray((JSONArray) array.get(i));
				updates[i] = update;
			}
			
			return updates;
		} catch (JSONException e) {
			Logger.log(e);
		}
		
		return new LongPollResponseUpdate[0];
	}
	
	public static class LongPollResponseUpdate {
		private final LongPollResponseUpdateType type;
		private final long id;
		private final String[] params;
		
		private LongPollResponseUpdate(LongPollResponseUpdateType type, long l, String[] params) {
			this.type = type;
			this.id = l;
			this.params = params;
		}

		/**
		 * @return the type
		 */
		public LongPollResponseUpdateType getType() {
			return type;
		}

		/**
		 * @return the id
		 */
		public long getId() {
			return id;
		}

		/**
		 * @return the params
		 */
		public String[] getParams() {
			return params;
		}
		
		private static LongPollResponseUpdate fromParamsArray(JSONArray jsonArray) {
			if (jsonArray == null) return null;
			
			try {
				int typeId = jsonArray.getInt(0);
				
				LongPollResponseUpdateType type = LongPollResponseUpdateType.getByTypeId(typeId);
				
				if (type == null) {
					throw new JSONException("Unknown longpoll update #"+typeId);
				}
				
				LongPollResponseUpdate update;
				
				if (jsonArray.length() > 2) {
					String[] otherParams = new String[jsonArray.length()-2];
					
					for (int i=2; i<jsonArray.length(); i++) {
						otherParams[i-2] = jsonArray.getString(i);
					}
					
					update = new LongPollResponseUpdate(type, jsonArray.getLong(1), otherParams);
				} else {
					update = new LongPollResponseUpdate(type, jsonArray.getLong(1), new String[0]);
				}
				
				return update;
			} catch (JSONException e) {
				Logger.log(e);
			}
			
			return null;
		}
	}
	
	public enum LongPollResponseUpdateType {
		MSG_DELETED(0),
		MSG_FLAGS_CHANGE(1),
		MSG_FLAGS_SET(2),
		MSG_FLAGS_RESET(3),
		MSG_NEW(4),
		BUDDY_ONLINE(8),
		BUDDY_OFFLINE_AWAY(9),
		CHAT_PARAMETER(51),
		BUDDY_TYPING(61),
		BUDDY_TYPING_CHAT(62),
		BUDDY_CALL(70);
		
		private final byte id;
		
		private LongPollResponseUpdateType(int id){
			this.id = (byte) id;
		}
		
		public byte getId(){
			return id;
		}
		
		public static LongPollResponseUpdateType getByTypeId(int typeId) {
			for (LongPollResponseUpdateType type : LongPollResponseUpdateType.values()) {
				if (type.getId() == typeId) {
					return type;
				}
			}
			
			return null;
		}
	}
}
