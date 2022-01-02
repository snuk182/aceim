package aceim.api.service;

import aceim.api.dataentity.ListFeature;

/**
 * Ace IM API constants.
 */
public final class ApiConstants {
	
	private ApiConstants(){}
	
	/**
	 * Application ID prefix for protocol implementations. 
	 */
	public static final String ACTION_PLUGIN_PROTOCOL = "aceim.protocol";
	
	/**
	 * Application ID prefix for smiley packs.
	 */
	public static final String ACTION_PLUGIN_SMILEYS = "aceim.smileys";
	
	/**
	 * Application ID prefix for themes.
	 */
	public static final String ACTION_PLUGIN_THEME = "aceim.theme";
	
	/**
	 * Use this for buddy entities, that do not belong to list of account buddies (for example, has just sent authorization request).
	 */
	public static final String NOT_IN_LIST_GROUP_ID = "#not-in-list#";
	
	/**
	 * Use this for buddy entities, that do not belong to a group.
	 */
	public static final String NO_GROUP_ID = "#no-group#";
	
	/**
	 * Divider for use in complex IDs.
	 */
	public static final char GENERAL_DIVIDER = ' ';
	
	/**
	 * Use this for status feature ({@link ListFeature})
	 */
	public static final String FEATURE_STATUS = "Status";
	
	/**
	 * Use this for extended status feature ({@link ListFeature})
	 */
	public static final String FEATURE_XSTATUS = "XStatus";
	
	/**
	 * Use this to light file transfer ability of buddy or account.
	 */
	public static final String FEATURE_FILE_TRANSFER = "FileTransfer";
	
	/**
	 * Use this to show buddy group management ability (adding, modifying, deleting)
	 */
	public static final String FEATURE_GROUP_MANAGEMENT = "GroupManagement";
	
	/**
	 * Use this to show buddy management ability (adding, modifying, deleting)
	 */
	public static final String FEATURE_BUDDY_MANAGEMENT = "BuddyManagement";
	
	
	/**
	 * Use this to account info management ability
	 */
	public static final String FEATURE_ACCOUNT_MANAGEMENT = "AccountManagement";
	
	/**
	 * Use this to show buddy sharing (sending info to other buddies) ability. Not implemented in core version 0.9.3.
	 */
	public static final String FEATURE_BUDDY_SHARING = "BuddySharing";
	
	/**
	 * 
	 */
	public static final String FEATURE_BUDDY_RESOURCE = "BuddyResource";
	
	/**
	 * Offline status drawable ID.
	 */
	public static final String RESOURCE_DRAWABLE_OFFLINE = "ic_offline";
	
	/**
	 * Online status drawable ID.
	 */
	public static final String RESOURCE_DRAWABLE_ONLINE = "ic_online";
	
	/**
	 * Connecting status drawable ID.
	 */
	public static final String RESOURCE_DRAWABLE_CONNECTING = "ic_connecting";
}
