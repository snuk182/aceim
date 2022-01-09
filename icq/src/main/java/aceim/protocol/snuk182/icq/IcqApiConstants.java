package aceim.protocol.snuk182.icq;

public final class IcqApiConstants {
	
	private IcqApiConstants(){}
	
	public static final byte STATUS_OFFLINE = -1;//<item>@drawable/icq_offline_big</item>
	public static final byte STATUS_ONLINE = 0;//<item>@drawable/icq_online_big</item>
	public static final byte STATUS_AWAY = 1;//<item>@drawable/icq_away_big</item>
	public static final byte STATUS_NA = 2;//<item>@drawable/icq_na_big</item>
	public static final byte STATUS_BUSY = 3;//<item>@drawable/icq_busy_big</item>
	public static final byte STATUS_DND = 4;//<item>@drawable/icq_dnd_big</item>
	public static final byte STATUS_FREE4CHAT = 5;//<item>@drawable/icq_free4chat_big</item>
	public static final byte STATUS_DEPRESS = 6;//<item>@drawable/icq_depress_big</item>
	public static final byte STATUS_ANGRY = 7;//<item>@drawable/icq_angry_big</item>
	public static final byte STATUS_DINNER = 8;//<item>@drawable/icq_dinner_big</item>
	public static final byte STATUS_WORK = 9;//<item>@drawable/icq_work_big</item>
	public static final byte STATUS_HOME = 10;
	public static final byte STATUS_INVISIBLE = 11;
	
	protected static final String PROTOCOL_NAME = "ICQ";
	
	public static final String FEATURE_BUDDY_VISIBILITY = "BuddyVisibility";
	public static final String FEATURE_BUDDY_SEARCH = "BuddySearch";
	
	public static final String FEATURE_ACCOUNT_VISIBILITY = "AccountVisibility";
	public static final String FEATURE_AUTHORIZATION = "Authorization";
}
