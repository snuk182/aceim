package aceim.protocol.snuk182.vkontakte.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;
import aceim.protocol.snuk182.vkontakte.VkConstants;
import aceim.protocol.snuk182.vkontakte.VkEntityAdapter;
import aceim.protocol.snuk182.vkontakte.model.AccessToken;
import aceim.protocol.snuk182.vkontakte.model.ApiObject;
import aceim.protocol.snuk182.vkontakte.model.LongPollResponse;
import aceim.protocol.snuk182.vkontakte.model.LongPollResponse.LongPollResponseUpdate;
import aceim.protocol.snuk182.vkontakte.model.LongPollServer;
import aceim.protocol.snuk182.vkontakte.model.VkBuddy;
import aceim.protocol.snuk182.vkontakte.model.VkBuddyGroup;
import aceim.protocol.snuk182.vkontakte.model.VkChat;
import aceim.protocol.snuk182.vkontakte.model.VkMessage;
import aceim.protocol.snuk182.vkontakte.model.VkOnlineInfo;
import android.net.Uri;
import android.text.TextUtils;

final class VkEngine {
	
	private static final String API_URL = "https://api.vk.com/method/";
	private static final String TOKEN_URL = "https://oauth.vk.com/access_token";

	private static final String PARAM_ACCESS_TOKEN = "access_token";

	private final String accessToken;
	private final String internalUserId;
	
	private PollListenerThread listener;
	
	private final VkEngineConnector connector;
	
	static {
		System.setProperty("networkaddress.cache.ttl", "0");
		System.setProperty("networkaddress.cache.negative.ttl", "0");
	}
	
	VkEngine(String accessToken, String internalUserId) {
		this.accessToken = accessToken;
		this.internalUserId = internalUserId;
		this.connector = new VkEngineConnector();
	}
	

	void sendTypingNotifications(String uid, boolean isChat) throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put(isChat ? "chat_id" : "uid", uid);
		params.put("type", "typing");

		doGetRequest("messages.setActivity", accessToken, params);		
	}
	
	void setStatus(String statusString) throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("text", statusString);
		
		doGetRequest("status.set", accessToken, params);	
	}
	
	List<VkChat> getGroupChats() throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("count", "200");

		String result = doGetRequest("messages.getDialogs", accessToken, params);
		
		try {
			JSONArray array = new JSONObject(result).getJSONArray("response");
			
			List<String> chats = new ArrayList<String>();
			for (int i=0; i<array.length(); i++) {
				JSONObject jo = array.optJSONObject(i);
				
				String chatId = null;
				if (jo != null && !TextUtils.isEmpty((chatId = jo.optString("chat_id")))) {
					chats.add(chatId);
				}
			}
			
			List<VkChat> vkchats = new ArrayList<VkChat>(chats.size());
			for (String chatId : chats) {
				params.clear();
				params.put("chat_id", chatId);
				
				result = doGetRequest("messages.getChat", accessToken, params);
				
				try {
					VkChat chat = new VkChat(new JSONObject(result));
					
					if (chat.getUsers().length > 2) {
						vkchats.add(chat);
					}
				} catch (JSONException e) {
					Logger.log(e);
				}
			}
			
			return vkchats;
		} catch (JSONException e) {
			throw new RequestFailedException(e);
		}
	}

	List<VkOnlineInfo> getOnlineBuddies() throws RequestFailedException {
		String result = doGetRequest("friends.getOnline", accessToken, null);
		return ApiObject.parseArray(result, VkOnlineInfo.class);
	}

	List<VkBuddyGroup> getBuddyGroupList() throws RequestFailedException {
		String result = doGetRequest("friends.getLists", accessToken, null);
		
		return ApiObject.parseArray(result, VkBuddyGroup.class);
	}

	List<VkBuddy> getBuddyList() throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("fields", "first_name,last_name,nickname,photo_big,lid");

		String result = doGetRequest("friends.get", accessToken, params);

		return ApiObject.parseArray(result, VkBuddy.class);
	}
	
	VkBuddy getMyInfo() throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("uids", internalUserId);
		params.put("fields", "first_name,last_name,nickname,photo_big");

		String result = doGetRequest("users.get", accessToken, params);
		
		return ApiObject.parseArray(result, VkBuddy.class).get(0);
	}
	
	String requestStatus(long uid) throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("uid", Long.toString(uid));
		String result = doGetRequest("status.get", accessToken, params);
		
		try {
			return Utils.unescapeXMLString(new JSONObject(result).getJSONObject("response").getString("text"));
		} catch (JSONException e) {
			Logger.log(e);
			throw new RequestFailedException(e);
		}
	}

	void connectLongPoll(int pollWaitTime, LongPollCallback callback) throws RequestFailedException {
		Logger.log("Get new longpoll server connection", LoggerLevel.VERBOSE);
		try {
			LongPollServer lpServer = getLongPollServer();

			startLongPollConnection(pollWaitTime, lpServer, callback);
		} catch (JSONException e) {
			Logger.log(e);
		}
	}
	

	List<VkBuddy> getUsersByIdList(long[] users) throws RequestFailedException {
		if (users == null) return null;
		
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<users.length; i++) {
			long userId = users[i];
			sb.append(userId);
			if (i < (users.length - 1)) {
				sb.append(",");
			}
		}
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("uids", sb.toString());
		params.put("fields", "first_name,last_name,nickname,photo_big");

		String result = doGetRequest("users.get", accessToken, params);
		
		return ApiObject.parseArray(result, VkBuddy.class);
	}
	
	VkChat getChatById(String chatId) throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("chat_id", chatId);
		
		String result = doGetRequest("messages.getChat", accessToken, params);
		return new VkChat(result);
	}	

	List<VkMessage> getLastChatMessages(String id, boolean isChat) throws RequestFailedException {
		Map<String, String> params = new HashMap<String, String>();
		params.put(isChat ? "chat_id" : "uid", id);
		params.put("count", "3");
		params.put("rev", "1");		
		
		String result = doGetRequest("messages.getHistory", accessToken, params);
		return ApiObject.parseArray(result, VkMessage.class);
	}

	private LongPollServer getLongPollServer() throws JSONException, RequestFailedException {
		String result = doGetRequest("messages.getLongPollServer", accessToken, null);
		return new LongPollServer(result);
	}

	private String doGetRequest(String apiMethodName, String accessToken, Map<String, String> params) throws RequestFailedException {
		if (accessToken == null) {
			throw new IllegalStateException("Empty access token");
		}

		if (params == null) {
			params = new HashMap<String, String>(1);
		}
		params.put(PARAM_ACCESS_TOKEN, accessToken);
		return connector.request(Method.GET, API_URL + apiMethodName, VkEntityAdapter.map2NameValuePairs(params), null, null);
	}

	private void startLongPollConnection(int pollWaitTime, LongPollServer lpServer, LongPollCallback callback) {
		disconnect(null);
		listener = new PollListenerThread(pollWaitTime, lpServer, callback);
		listener.start();
	}
	
	public long sendMessage(VkMessage message) throws RequestFailedException {
		String result = doGetRequest("messages.send", accessToken, message.toParamsMap());
				
		try {
			return new JSONObject(result).getLong("response");
		} catch (JSONException e) {
			Logger.log(e);
			return 0;
		}
	}
	
	public byte[] getIcon(String url) throws RequestFailedException {
		try {
			return connector.requestRawStream(Method.GET, url, null, null, null);
		} catch (Exception e) {
			disconnect(e.getLocalizedMessage());
			throw new RequestFailedException(e);
		}
	}
	
	static AccessToken getAccessToken(String code) throws RequestFailedException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);

		params.add(new BasicNameValuePair("client_id", VkApiConstants.API_ID));
		params.add(new BasicNameValuePair("client_secret", VkApiConstants.API_SECRET));
		params.add(new BasicNameValuePair("code", code));
		params.add(new BasicNameValuePair("redirect_uri", VkConstants.OAUTH_REDIRECT_URL));

		String json = new VkEngineConnector().request(Method.GET, TOKEN_URL, params, null, null);
		
		try {
			AccessToken token = new AccessToken(json);

			return token;
		} catch (JSONException e) {
			Logger.log(e);
			return null;
		}
	}

	private final class PollListenerThread extends Thread {

		private static final String LONGPOLL_MODE_GET_ATTACHMENTS = "2";

		private final int pollWaitSeconds;

		private final String url;
		private final String key;
		private String ts;
		private final LongPollCallback callback;
		
		private String lastUpdate = "";

		private volatile boolean isClosed = false;

		private PollListenerThread(int pollWaitTime, LongPollServer lpServer, LongPollCallback callback) {
			this.url = lpServer.getServer();
			this.key = lpServer.getKey();
			this.ts = lpServer.getTs();
			this.callback = callback;
			this.pollWaitSeconds = pollWaitTime > 0 ? pollWaitTime : Integer.parseInt(VkConstants.POLL_WAIT_TIME);
		}
		
		private void disconnect(String reason) {
			if (!isInterrupted() && !isClosed) {
				isClosed = true;
				interrupt();
				if (callback != null) {
					callback.disconnected(reason);
				}
			}
		}

		@Override
		public void run() {
			List<NameValuePair> params = new ArrayList<NameValuePair>(5);

			params.add(new BasicNameValuePair("act", "a_check"));
			params.add(new BasicNameValuePair("key", key));
			params.add(new BasicNameValuePair("ts", ts));
			params.add(new BasicNameValuePair("wait", Integer.toString(pollWaitSeconds)));
			params.add(new BasicNameValuePair("mode", LONGPOLL_MODE_GET_ATTACHMENTS));

			while (!isInterrupted() && !isClosed) {
				try {
					String responseString = connector.request(Method.GET, "http://" + url, params, null, null);
					
					LongPollResponse response = new LongPollResponse(responseString);

					ts = response.getTs();
					
					String currentUpdate = response.getUpdatesJSON();
					if (!lastUpdate.equals(currentUpdate)) {
						lastUpdate = currentUpdate;
						for (LongPollResponseUpdate u : response.getUpdates()) {
							processUpdate(u, callback);
						}
					} else {
						//Despite of the "wait" parameter in request, server does not hold the connection and returns previous value immediately. It's not a local cache issue.
						Thread.sleep(1000);
					}

					if (response.isConnectionDead()) {
						Logger.log("Dead longpoll connection", LoggerLevel.VERBOSE);
						interrupt();
					}
				} catch (Exception e) {
					Logger.log(e);
					interrupt();
				} 
			}

			if (!isClosed) {
				try {
					connectLongPoll(pollWaitSeconds, callback);
				} catch (RequestFailedException e) {
					Logger.log(e);
				}
			}
		}
		
		private void processUpdate(LongPollResponseUpdate update, LongPollCallback callback) {
			switch (update.getType()) {
			case BUDDY_ONLINE:
			case BUDDY_OFFLINE_AWAY:
				VkOnlineInfo vi = VkOnlineInfo.fromLongPollUpdate(update);
				callback.onlineInfo(vi);
				break;
			case MSG_NEW:
				VkMessage vkm = VkMessage.fromLongPollUpdate(update);
				if (vkm.isOutgoing() && vkm.isUnread()) {
					callback.messageAck(vkm);
				} else {
					callback.message(vkm);
				}
				break;
			case BUDDY_TYPING:
				callback.typingNotification(update.getId(), 0);
				break;
			case BUDDY_TYPING_CHAT:
				callback.typingNotification(Long.parseLong(update.getParams()[0]), update.getId());
				break;
			default:
				Logger.log("LongPoll update: " + update.getType() + "/" + update.getId() + "/" + update.getParams(), LoggerLevel.INFO);
				break;
			}
		}
	}
	
	public void disconnect(String reason) {
		if (listener != null) {
			listener.disconnect(reason);
		}
	}
	
	public static class RequestFailedException extends Exception {
		
		private static final long serialVersionUID = -7412616341196774522L;

		private RequestFailedException(Exception inner) {
			super(inner);
		}
		
		private RequestFailedException(String reason) {
			super(reason);
		}
	}

	private enum Method {
		POST, GET
	}

	public interface LongPollCallback {

		void onlineInfo(VkOnlineInfo vi);
		void messageAck(VkMessage vkm);
		void message(VkMessage vkm);
		void typingNotification(long contactId, long chatParticipantId);
		void disconnected(String reason);
	}
	
	private static class VkEngineConnector {
		private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
		private volatile boolean requestAllowed = true;
		
		private final Runnable requestAllowedResetter = new Runnable() {

			@Override
			public void run() {
				requestAllowed = true;
			}
			
		};

		private String request(Method method, String url, List<NameValuePair> parameters, String content, List<? extends NameValuePair> nameValuePairs) throws RequestFailedException {
			try {
				byte[] resultBytes = requestRawStream(method, url, parameters, content, nameValuePairs);
				String result = new String(resultBytes, "UTF-8");
				
				Logger.log("Got " + result, LoggerLevel.VERBOSE);
				return result;
			} catch (Exception e) {
				throw new RequestFailedException(e);
			}
		}

		private byte[] requestRawStream(Method method, String url, List<NameValuePair> parameters, String content, List<? extends NameValuePair> nameValuePairs) throws RequestFailedException, URISyntaxException, ClientProtocolException, IOException {
			while (!requestAllowed) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
			
			Uri.Builder b = new Uri.Builder();
			b.encodedPath(url);

			if (parameters != null) {
				for (NameValuePair p : parameters) {
					b.appendQueryParameter(p.getName(), p.getValue());
				}
			}

			url = b.build().toString();

			HttpRequestBase request;
			switch (method) {
			case GET:
				request = new HttpGet(new URI(url));
				break;
			case POST:
				request = new HttpPost(new URI(url));
				if (nameValuePairs != null) {
					((HttpPost) request).setEntity(new UrlEncodedFormEntity(nameValuePairs));
				}

				if (content != null) {
					((HttpPost) request).setEntity(new StringEntity(content));
				}
				break;
			default:
				Logger.log("Unknown request method " + method, LoggerLevel.WTF);
				return null;
			}
			
			HttpEntity httpEntity = null;
			ByteArrayOutputStream ostream = null;
			try {
				HttpParams httpParameters = new BasicHttpParams();
				int timeoutConnection = 120000;
				int timeoutSocket = 120000;
				HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
				httpParameters.setParameter("http.useragent", "Android mobile");

				HttpClient httpclient = new DefaultHttpClient(httpParameters);
				
				httpclient.getParams().setParameter("http.useragent", "Android mobile");

				request.addHeader("Accept-Encoding", "gzip");
				
				Logger.log("Ask " + url, LoggerLevel.VERBOSE);
				
				requestAllowed = false;
				HttpResponse response = httpclient.execute(request);
				
				scheduledExecutor.schedule(requestAllowedResetter, 334, TimeUnit.MILLISECONDS);
				
				Logger.log("..." + response.getStatusLine().getStatusCode(), LoggerLevel.VERBOSE);
				
				httpEntity = response.getEntity();
				
				InputStream instream = httpEntity.getContent();
				Header contentEncoding = response.getFirstHeader("Content-Encoding");
				if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
					instream = new GZIPInputStream(instream);
				}
				
				ostream = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				int read = -1;
				
				while((read = instream.read(buffer, 0, buffer.length)) != -1) {
					ostream.write(buffer, 0, read);
				}
				
				ostream.flush();
				
				return ostream.toByteArray();
			} finally {
				if (httpEntity != null) {
					httpEntity.consumeContent();
				}
				if (ostream != null) {
					ostream.close();
				}
			}
		}
	}
}
