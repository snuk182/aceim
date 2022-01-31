package aceim.protocol.snuk182.vkontakte.model;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;

public class AccessToken extends ApiObject {
	
	private final String token;
	private final long userId;
	private final Date expirationTime;
	private final boolean unexpirable;

	public AccessToken(String token, long userId, long expirationTime, boolean unexpirableToken) {
		this.token = token;
		this.userId = userId;
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(expirationTime);
		
		this.expirationTime = c.getTime();
		this.unexpirable = unexpirableToken;
	}

	public AccessToken(String json) throws JSONException {
		super(json);
		token = super.getString("access_token");
		userId = super.getJSONObject().getLong("user_id");
		
		long seconds = getJSONObject().getLong("expires_in");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.SECOND, (seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)seconds));
		expirationTime = c.getTime();
		unexpirable = seconds == 0;
	}
	
	public String getToken() {
		return token;
	}
	
	public long getUserID() {
		return userId;
	}
	
	public Date getExpirationTime() {
		return expirationTime;
	}

	/**
	 * @return the unexpirable
	 */
	public boolean isUnexpirable() {
		return unexpirable;
	}
}
