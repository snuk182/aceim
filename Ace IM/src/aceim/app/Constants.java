package aceim.app;

import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;


public final class Constants {

	public static final String ICON_FILE_SUFIX = ".ico";
	
	public static final String DISABLED_SUFFIX = "_Disabled";
	
	public static final String VIEWGROUP_ONLINE = "AceImOnline";
	public static final String VIEWGROUP_OFFLINE = "AceImOffline";
	public static final String VIEWGROUP_UNREAD = "AceImUnread";
	public static final String VIEWGROUP_CHATS = "AceImChats";
	public static final String VIEWGROUP_NOGROUP = "AceImNoGroup";
	public static final String VIEWGROUP_NOT_IN_LIST = "AceImNotInList";
	
	public static final String MARKET_SEARCH_PROTOCOL_URI = "market://search?q=aceim.app.protocol";
	
	public static final int PICKFILE_OPTION = 0xf;
	public static final int PICKFILE_TRANSFER = 0xff;

	public static final ColorFilter WALLPAPER_COLOR_FILTER = new LightingColorFilter(0xff888888, 0x0);

	public static final String INTENT_EXTRA_ACCOUNT = "Account";
	public static final String INTENT_EXTRA_SERVICE_ID = "ServiceId";
	public static final String INTENT_EXTRA_OPTION_KEY = "OptionKey";
	public static final String INTENT_EXTRA_OPTION_VALUE = "OptionValue";
	public static final String INTENT_EXTRA_ACCOUNT_LIST = "AccountList";
	
	public static final String INTENT_ACTION_OPTION = "AceImOption";
	
	public interface OptionKey {
		String getStringKey();
		OptionKey fromStringKey(String key);
	}

	public static final String SHARED_PREFERENCES_GLOBAL = "AceImTotalParams";

	public static final String SAVED_STATE_PAGES = "OpenedPages";
	public static final String SAVED_STATE_SELECTED_PAGE = "SelectedPage";
	//public static final String SAVED_STATE_SERVICE_INTENT = "ServiceIntent";

	public static final String INTENT_EXTRA_BUDDY = "Buddy";
	public static final String INTENT_EXTRA_PROTOCOL_RESOURCES = "ProtocolResources";
	public static final String INTENT_EXTRA_CLASS_NAME = "ClassName";
	public static final String INTENT_EXTRA_MESSAGE = "Message";

	public static final String SMILEY_PLUGIN_PREFIX = "aceim.smileys.";
	public static final String THEME_PLUGIN_PREFIX = "aceim.themes.";
}
