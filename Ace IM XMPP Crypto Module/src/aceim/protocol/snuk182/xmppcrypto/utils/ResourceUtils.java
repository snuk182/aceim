package aceim.protocol.snuk182.xmppcrypto.utils;

import org.jivesoftware.smack.XMPPException;

import aceim.api.dataentity.ActionFeature;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.MarkerFeature;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ProtocolServiceFeatureTarget;
import aceim.api.dataentity.tkv.FileTKV;
import aceim.api.dataentity.tkv.ListTKV;
import aceim.api.dataentity.tkv.StringTKV;
import aceim.api.dataentity.tkv.StringTKV.ContentType;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Utils;
import aceim.protocol.snuk182.xmpp.common.XMPPApiConstants;
import aceim.protocol.snuk182.xmppcrypto.R;
import android.content.Context;

public final class ResourceUtils {
	
	public static final String KEY_JID = "key_jid";
	public static final String KEY_PASSWORD = "key_password";
	
	public static final String KEY_SERVER_HOST = "key_server_host";
	public static final String KEY_SERVER_PORT = "key_server_port";
	
	public static final String KEY_PING_TIMEOUT = "key_ping_timeout";
	
	public static final String KEY_PROXY_TYPE = "key_proxy_type";
	public static final String KEY_PROXY_HOST = "key_proxy_host";
	public static final String KEY_PROXY_PORT = "key_proxy_port";
	public static final String KEY_PROXY_USERNAME= "key_proxy_username";
	public static final String KEY_PROXY_PASSWORD = "key_proxy_password";
	
	public static final String KEY_PRIVATEKEY_FILE = "key_privatekey_file";
	public static final String KEY_PRIVATEKEY_PASSWORD = "key_privatekey_pw";
	
	public static final String KEY_SECURE_CONNECTION = "key_secure_connection";
	
	public static final String BUDDY_PUBLIC_KEY_PREFIX = "BuddyPublicKey_";
	
	private static ProtocolServiceFeature[] FEATURES = null;
	
	private static InputFormFeature sGroupChatFeature = null;
	
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
							Utils.fillResources(context.getResources(), R.array.xmpp_status_strings),
							Utils.fillResources(context.getResources(), R.array.xmpp_status_icons),
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new InputFormFeature(
							XMPPApiConstants.FEATURE_ADD_BUDDY, 
							context.getString(R.string.add_buddy), 
							android.R.drawable.ic_menu_add,
							false,
							false,
							getAddBuddyFormTKVs(context), 
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new InputFormFeature(XMPPApiConstants.FEATURE_AUTHORIZATION, 
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
					new MarkerFeature(ApiConstants.FEATURE_ACCOUNT_MANAGEMENT, 
							"Account info management", 
							android.R.drawable.ic_menu_info_details,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					new ActionFeature(XMPPApiConstants.FEATURE_GROUPCHATS,
							context.getString(R.string.groupchats),
							R.drawable.ic_menu_allfriends,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT}),
					getAddGroupchatFeature(context),
					new ActionFeature(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION,
							context.getString(R.string.room_configuration),
							android.R.drawable.ic_menu_edit,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),					
					new ActionFeature(XMPPApiConstants.FEATURE_DESTROY_CHAT_ROOM,
							context.getString(R.string.destroy_room),
							android.R.drawable.ic_menu_delete,
							false,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new InputFormFeature(XMPPApiConstants.FEATURE_ADD_PUBLIC_KEY,
							context.getString(R.string.add_public_key),
							R.drawable.ic_menu_login,
							false,
							true,
							getAddPublicKeyFields(context),
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new ActionFeature(XMPPApiConstants.FEATURE_REMOVE_PUBLIC_KEY,
							context.getString(R.string.remove_public_key),
							R.drawable.ic_menu_login,
							false,
							true,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new ActionFeature(XMPPApiConstants.FEATURE_ENCRYPTION_OFF,
							context.getString(R.string.encryption_off),
							R.drawable.ic_menu_unlock,
							true,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY}),
					new ActionFeature(XMPPApiConstants.FEATURE_ENCRYPTION_ON,
							context.getString(R.string.encryption_on),
							R.drawable.ic_menu_lock,
							true,
							false,
							new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.BUDDY})
					};
				}
				
				return FEATURES;
			}
			
	private static TKV[] getAddPublicKeyFields(Context context) {
		return new TKV[]{
				new FileTKV("file/*", context.getString(R.string.pgp_key_file), true, null),
		};
	}

	public static InputFormFeature getAddGroupchatFeature(Context context) {
		if (sGroupChatFeature == null) {
			sGroupChatFeature = new InputFormFeature(XMPPApiConstants.FEATURE_ADD_GROUPCHAT, 
					context.getString(R.string.add_connect_groupchat), 
					android.R.drawable.ic_menu_add, 
					false,
					false,
					getAddGroupchatFormTKVs(context), 
					new ProtocolServiceFeatureTarget[]{ProtocolServiceFeatureTarget.ACCOUNT});
		}
		
		return sGroupChatFeature;
	}

	private static TKV[] getAddGroupchatFormTKVs(Context context) {
		return new TKV[]{
				new StringTKV(ContentType.STRING, context.getString(R.string.host), true, XMPPApiConstants.DEFAULT_HOST),
				new StringTKV(ContentType.STRING, context.getString(R.string.chat_room_id), true, null),
				new ListTKV(new String[]{context.getString(R.string.join_existing_room), context.getString(R.string.create_new_chat)}, context.getString(R.string.chat_action), true, null),
				new StringTKV(ContentType.STRING, context.getString(R.string.nickname), false, null),
				new StringTKV(ContentType.PASSWORD, context.getString(R.string.password), false, null)
		};
	}
	
	private static final TKV[] getAuthorizationFields(Context context) {
		return new TKV[]{
				new StringTKV(ContentType.STRING, context.getString(R.string.message), true, context.getString(R.string.ask_authorization_message_default))
		};
	}
	
	private static final TKV[] getAddBuddyFormTKVs(Context context) {
		return new TKV[]{
				new StringTKV(ContentType.STRING, context.getString(R.string.jid), false, null),
				new StringTKV(ContentType.STRING, context.getString(R.string.nickname), false, null)
		};
	}

	public static String xmppExceptionToString(XMPPException e) {
		return e.getLocalizedMessage(); //(e.getXMPPError() != null ? e.getXMPPError().getMessage() + "(" + e.getXMPPError().getCode() + ")" : "") + (e.getStreamError() != null ? e.getStreamError().getText() + "(" + e.getStreamError().getCode() + ")" : "");
	}
}
