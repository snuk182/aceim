package aceim.protocol.snuk182.icq.utils;

import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.MarkerFeature;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolOption.ProtocolOptionType;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ProtocolServiceFeatureTarget;
import aceim.api.dataentity.tkv.StringTKV;
import aceim.api.dataentity.tkv.StringTKV.ContentType;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Utils;

import aceim.protocol.snuk182.icq.IcqApiConstants;
import aceim.protocol.snuk182.icq.R;
import android.content.Context;

public final class ResourceUtils {
	
	private static ProtocolServiceFeature[] FEATURES = null;
	
	public static final ProtocolServiceFeature[] getFeatures(Context context) {
		if (FEATURES == null) {
			FEATURES = new ProtocolServiceFeature[] {
					new ListFeature(
							ApiConstants.FEATURE_STATUS, 
							context.getString(R.string.status), 
							0,
							true,
							true,
							true,
							Utils.fillResources(context.getResources(), R.array.array_status_strings),
							Utils.fillResources(context.getResources(), R.array.array_status_icons),
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new ListFeature(
							ApiConstants.FEATURE_XSTATUS, 
							context.getString(R.string.xstatus), 
							0,
							true,
							true,
							true,
							Utils.fillResources(context.getResources(), R.array.icq_xstatus_descr),
							Utils.fillResources(context.getResources(), R.array.icq_xstatus_names_32),
							true,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new ListFeature(
							IcqApiConstants.FEATURE_ACCOUNT_VISIBILITY, 
							context.getString(R.string.visibility), 
							0,
							true,
							true,
							true,
							Utils.fillResources(context.getResources(), R.array.array_account_visibility_names), 
							Utils.fillResources(context.getResources(), R.array.array_account_visibility_icons), 
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new ListFeature(IcqApiConstants.FEATURE_BUDDY_VISIBILITY, 
							context.getString(R.string.visibility), 
							android.R.drawable.ic_menu_view,
							true,
							true,
							false,
							Utils.fillResources(context.getResources(), R.array.array_buddy_visibility_names),
							Utils.fillResources(context.getResources(), R.array.array_buddy_visibility_icons),
							true,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new InputFormFeature(
							IcqApiConstants.FEATURE_BUDDY_SEARCH, 
							context.getString(R.string.search), 
							android.R.drawable.ic_menu_search,
							false,
							false,
							getSearchFormTKVs(context), 
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new InputFormFeature(IcqApiConstants.FEATURE_AUTHORIZATION, 
							context.getString(R.string.ask_authorization), 
							android.R.drawable.ic_dialog_alert, 
							true,
							false,
							getAuthorizationFields(context), 
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new MarkerFeature(ApiConstants.FEATURE_FILE_TRANSFER, 
							"File Transfer", 
							android.R.drawable.ic_menu_save,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new MarkerFeature(ApiConstants.FEATURE_BUDDY_MANAGEMENT, 
							"Buddy management", 
							android.R.drawable.ic_menu_info_details,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new MarkerFeature(ApiConstants.FEATURE_GROUP_MANAGEMENT, 
							"Group management", 
							android.R.drawable.ic_menu_info_details,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.GROUP}),
					new MarkerFeature(ApiConstants.FEATURE_GROUP_MANAGEMENT, 
							"Account info management", 
							android.R.drawable.ic_menu_info_details,
							false, 
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT})
			};
		}
		
		return FEATURES;
	}
	
	private static final TKV[] getAuthorizationFields(Context context) {
		return new TKV[]{
				new StringTKV(ContentType.STRING, context.getString(R.string.message), true, context.getString(R.string.ask_authorization_message_default))
		};
	}
	
	private static final TKV[] getSearchFormTKVs(Context context) {
		return new TKV[]{
				new StringTKV(ContentType.STRING, context.getString(R.string.uin), false, null),
				new StringTKV(ContentType.STRING, context.getString(R.string.screenname), false, null),
		};
	}

	public static final String KEY_USERNAME = "key_username";
	public static final String KEY_PASSWORD = "key_password";
	public static final String KEY_LOGIN_HOST = "key_login_host";
	public static final String KEY_LOGIN_PORT = "key_login_port";
	public static final String KEY_PING_TIMEOUT = "key_ping_timeout";
	public static final String KEY_SECURE_LOGIN = "key_secure_login";
	
	public static final ProtocolOption[] OPTIONS = { 
			new ProtocolOption(ProtocolOptionType.DOUBLE, KEY_USERNAME, null, R.string.uin, true),
			new ProtocolOption(ProtocolOptionType.PASSWORD, KEY_PASSWORD, null, R.string.password, true), 
			new ProtocolOption(ProtocolOptionType.STRING, KEY_LOGIN_HOST, "login.icq.com", R.string.host, false),
			new ProtocolOption(ProtocolOptionType.INTEGER, KEY_LOGIN_PORT, "5190", R.string.port, false), 
			new ProtocolOption(ProtocolOptionType.INTEGER, KEY_PING_TIMEOUT, "200", R.string.ping, false),
			new ProtocolOption(ProtocolOptionType.CHECKBOX, KEY_SECURE_LOGIN, "true", R.string.secure, false) 
	};
}
