package aceim.protocol.snuk182.xmpp.common;

public final class XMPPApiConstants {

	private XMPPApiConstants() {}
	
	/*public static final byte STATUS_OFFLINE = -1;
	public static final byte STATUS_ONLINE = 0;
	public static final byte STATUS_AWAY = 1;
	public static final byte STATUS_NA = 2;
	public static final byte STATUS_DND = 3;	
	public static final byte STATUS_FREE4CHAT = 4;
	public static final byte STATUS_INVISIBLE = 5;*/
	
	public static final String DEFAULT_PORT = "5222";
	public static final String DEFAULT_HOST = "jabber.org";
	
	public static final String PROTOCOL_NAME = "XMPP";
	
	public static final String FEATURE_ADD_BUDDY = "AddBuddy";
	public static final String FEATURE_AUTHORIZATION = "Authorization";
	public static final String FEATURE_GROUPCHATS = "Groupchats";
	public static final String FEATURE_ADD_GROUPCHAT = "AddGroupchat";
	public static final String FEATURE_CONFIGURE_CHAT_ROOM_ACTION = "ConfigureChatRoomAction";
	public static final String FEATURE_CONFIGURE_CHAT_ROOM_RESULT = "ConfigureChatRoomResult";
	public static final String FEATURE_DESTROY_CHAT_ROOM = "DestroyChatRoom";
	
	public static final String FEATURE_ADD_PUBLIC_KEY = "AddPublicKey";
	public static final String FEATURE_REMOVE_PUBLIC_KEY = "RemovePublicKey";
	public static final String FEATURE_ENCRYPTION_ON = "EncryptionOn";
	public static final String FEATURE_ENCRYPTION_OFF = "EncryptionOff";
}
