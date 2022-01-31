package aceim.protocol.snuk182.vkontakte.model;

public class LongPollServer extends ApiObject {

	public LongPollServer(String json) {
		super(json);
	}
	/**
	 * @return the key
	 */
	public String getKey() {
		return getString("key");
	}
	/**
	 * @return the server
	 */
	public String getServer() {
		return getString("server");
	}
	/**
	 * @return the ts
	 */
	public String getTs() {
		return getString("ts");
	}
}
