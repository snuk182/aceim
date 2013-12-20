package aceim.protocol.snuk182.vkontakte.internal;

import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.protocol.snuk182.vkontakte.R;
import aceim.protocol.snuk182.vkontakte.VkConstants;
import aceim.protocol.snuk182.vkontakte.VkProtocol;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginActivity extends Activity {

	/*private static final int MASK_EXTENDED_MESSAGE_METHODS = 4096;
	private static final int MASK_BUDDIES = 1024;
	private static final int MASK_BUDDY_STATUSES = 2;
	
	private static final String VK_API_VERSION = "5.5";*/
	
	private String authServiceUrl = VkConstants.OAUTH_SERVER;
	private String uid = "";
	private String password = "";
	private boolean autoSubmitDialog = false;
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.login);
		login();
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void login() {
		uid = getIntent().getExtras().getString(VkConstants.KEY_PROTOCOL_ID);
		password = getIntent().getExtras().getString(VkConstants.KEY_PASSWORD);
		autoSubmitDialog = getIntent().getExtras().getBoolean(VkConstants.KEY_AUTO_SUBMIT_AUTH_DIALOG, false);
		
		WebView webView = (WebView) findViewById(R.id.loginWebView);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.clearCache(true);		
		
		if (Build.VERSION.SDK_INT < 19) {
			webView.getSettings().setSavePassword(false);
		}
		
		CookieSyncManager.createInstance(this);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		
		webView.setWebViewClient(new WebViewClient(){
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				setProgressBarIndeterminateVisibility(true);
				parseResult(url);
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				setProgressBarIndeterminateVisibility(false);
				if (url.startsWith(VkConstants.OAUTH_SERVER)) {
					view.loadUrl("javascript:"
							+ "document.getElementsByName('email')[0].value = '" + uid + "';"
							+ "document.getElementsByName('pass')[0].value = '" + password + "';");
					if (autoSubmitDialog) {
						view.loadUrl("javascript:document.addEventListener('DOMContentLoaded', function(){"
								+ "document.getElementById('install_allow').click();"
								+ "});");
					}
				}				
			}
		});
		
		String url = 
				"?client_id=" + VkApiConstants.API_ID
				+ "&scope=friends,status,messages,offline" 
				+ "&redirect_uri=" + VkConstants.OAUTH_REDIRECT_URL
				+ "&display=popup"
				+ "&response_type=token";
		
		if (authServiceUrl == null) {
			authServiceUrl = VkConstants.OAUTH_SERVER;
		}
		
		webView.loadUrl(authServiceUrl + url);
	}
	
	private void parseResult(String url) {
		if (url == null || !url.startsWith(VkConstants.OAUTH_REDIRECT_URL) || url.contains("error=")) {
			Logger.log("Webview result: " + url, LoggerLevel.VERBOSE);
			return;
		}
		
		Uri parsed = Uri.parse(url.replace("#", "?"));
		String code = parsed.getQueryParameter("code");
		String error = parsed.getQueryParameter("error");
		String errorDescription = parsed.getQueryParameter("error_description");
		
		String accessToken = parsed.getQueryParameter("access_token");
		String expirationTime = parsed.getQueryParameter("expires_in");
		String internalUserId = parsed.getQueryParameter("user_id");
		
		Bundle results = new Bundle();
		results.putString(VkConstants.KEY_PROTOCOL_ID, uid);
		
		if (code != null) {
			results.putString(VkConstants.KEY_CODE, code);
		}
		if (error != null) {
			results.putString(VkConstants.EXTRA_ERROR, error);
		}
		if (errorDescription != null) {
			results.putString(VkConstants.EXTRA_ERROR_DESCRIPTION, errorDescription);
		}
		
		if (accessToken != null) {
			results.putString(VkConstants.KEY_TOKEN, accessToken);
		}
		if (expirationTime != null) {
			long expTimeSeconds =  Long.parseLong(expirationTime);
			results.putLong(VkConstants.KEY_EXP_TIME_SECONDS, expTimeSeconds * 1000 + System.currentTimeMillis());
			results.putBoolean(VkConstants.KEY_UNEXPIRABLE_TOKEN, expTimeSeconds == 0);
		}
		if (internalUserId != null) {
			results.putLong(VkConstants.KEY_USER_ID, Long.parseLong(internalUserId));
		}
		
		Intent serviceIntent = new Intent(getApplicationContext(), VkProtocol.class);
		serviceIntent.putExtras(results);
		
		startService(serviceIntent);
		finish();
	}
}
