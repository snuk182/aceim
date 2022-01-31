package aceim.protocol.snuk182.mrim.utils;

import java.util.Random;

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
import aceim.protocol.snuk182.mrim.MrimApiConstants;
import aceim.protocol.snuk182.mrim.R;
import android.content.Context;

public final class ResourceUtils {
	
	public static final Random RANDOM = new Random();

	private ResourceUtils() {}
	
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
							Utils.fillResources(context.getResources(), R.array.mrim_status_strings),
							Utils.fillResources(context.getResources(), R.array.mrim_status_icons),
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new ListFeature(
							ApiConstants.FEATURE_XSTATUS, 
							context.getString(R.string.xstatus), 
							0,
							true,
							true,
							true,
							Utils.fillResources(context.getResources(), R.array.mrim_xstatus_descr),
							Utils.fillResources(context.getResources(), R.array.mrim_xstatus_names_32),
							true,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new InputFormFeature(MrimApiConstants.FEATURE_AUTHORIZATION, 
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
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY})					
			};
		}
		
		return FEATURES;
	}
	
	public static final String KEY_USERNAME = "key_username";
	public static final String KEY_PASSWORD = "key_password";
	public static final String KEY_LOGIN_HOST = "key_login_host";
	public static final String KEY_LOGIN_PORT = "key_login_port";
	public static final String KEY_PING_TIMEOUT = "key_ping_timeout";
	
	public static final ProtocolOption[] OPTIONS = { 
			new ProtocolOption(ProtocolOptionType.STRING, KEY_USERNAME, null, R.string.uin, true),
			new ProtocolOption(ProtocolOptionType.PASSWORD, KEY_PASSWORD, null, R.string.password, true), 
			new ProtocolOption(ProtocolOptionType.STRING, KEY_LOGIN_HOST, "mrim.mail.ru", R.string.host, false),
			new ProtocolOption(ProtocolOptionType.INTEGER, KEY_LOGIN_PORT, "2042", R.string.port, false), 
			new ProtocolOption(ProtocolOptionType.INTEGER, KEY_PING_TIMEOUT, "200", R.string.ping, false) 
	};
	
	public static final String PROTOCOL_NAME = "MRIM";
	
	private static final TKV[] getAuthorizationFields(Context context) {
		return new TKV[]{
				new StringTKV(ContentType.STRING, context.getString(R.string.message), true, context.getString(R.string.ask_authorization_message_default))
		};
	}
}
