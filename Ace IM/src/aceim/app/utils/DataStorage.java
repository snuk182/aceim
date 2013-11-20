package aceim.app.utils;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountOptionKeys;
import aceim.app.dataentity.AccountService;
import aceim.app.service.ServiceUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;

public final class DataStorage {

	private static final String ATTR_CONNECTION_STATE = "connection_state";
	//private static final String ATTR_LAST_UPDATE = "last_update";
	private static final String TAG_ACCOUNTS = "accounts";
	private static final String ATTR_COLLAPSED = "is_collapsed";
	private static final String TAG_GROUP = "group";
	private static final String TAG_GROUPS = "groups";
	private static final String ATTR_GROUPS = TAG_GROUPS;
	private static final String ATTR_UNREAD = "unread";
	private static final String ATTR_ID = "id";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_GROUP_ID = "group_id";
	private static final String TAG_NAME = "name";
	private static final String TAG_BUDDY_NAME = "buddy_name";
	private static final String TAG_GROUP_NAME = "group_name";
	private static final String TAG_BUDDY = "buddy";
	private static final String TAG_CHAT = "chat";
	private static final String ATTR_BUDDIES = "buddies";
	private static final String TAG_BUDDIES = "buddies";
	private static final String TAG_FEATURES = "features";
	private static final String TAG_FEATURE = "feature";
	private static final String TAG_XSTATUS_TEXT = "xstatus_text";
	private static final String TAG_XSTATUS_NAME = "xstatus_name";
	//private static final String ATTR_STATUS = "status";
	private static final String ATTR_PROTOCOL_UID = "protocol_uid";
	private static final String ATTR_PROTOCOL_NAME = "protocol_name";
	private static final String ATTR_PROTOCOL_SERVICE_CLASS_NAME = "protocol_service_class_name";
	private static final String TAG_ACCOUNT = "account";
	private static final String XML_ENCODING = "UTF-16LE";
	private static final String XMLPARAMS_TOTAL = "XmlTotalParams";
	static final String PREFERENCES_FILEEXT = ".preferences";

	private static final String XML_NAMESPACE = "aceim.app";
	//private static final String XML_NAMESPACE_OLD = "ua.snuk182.asia";

	private static final String SHARED_PREFERENCES = "ProtocolSharedPrefs";	
	
	private Context mContext;

	public DataStorage(Context context) {
		this.mContext = context;
	}

	public void saveProtocolOptions(final List<ProtocolOption> options, final String storageName) {
		Logger.log("Save protocol options for " + storageName, LoggerLevel.VERBOSE);
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				SharedPreferences.Editor preferences = mContext.getSharedPreferences(storageName, ServiceUtils.getAccessMode()).edit();

				for (ProtocolOption o : options) {
					preferences.putString(o.getKey(), o.getValue());
				}

				preferences.commit();
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}
	
	public void removeProtocolOptions(final Set<String> set, final String storageName) {		
		Logger.log("Remove protocol options " + storageName, LoggerLevel.VERBOSE);
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				SharedPreferences.Editor preferences = mContext.getSharedPreferences(storageName, ServiceUtils.getAccessMode()).edit();

				preferences.clear();
				preferences.commit();
			}
		};

		Executors.defaultThreadFactory().newThread(r).start();
	}
	
	public synchronized String getProtocolOptionValue(String key, Account account) {
		Logger.log("Value requested for protocol option " + key + " for " + account.getAccountId(), LoggerLevel.VERBOSE);
		
		if (key == null) {
			return null;
		}

		String storageName = account.getAccountId()+" "+SHARED_PREFERENCES;
		SharedPreferences preferences = mContext.getSharedPreferences(storageName, ServiceUtils.getAccessMode());

		return preferences.getString(key, null);
	}

	public synchronized List<Account> getAccounts() {
		Logger.log("Get all saved accounts", LoggerLevel.VERBOSE);
		try {
			//context.getFileStreamPath(XMLPARAMS_TOTAL).delete();
			
			List<Account> accounts = getAccountHeaders();
			for (Account account : accounts) {
				try {
					getAccount(account, true);
				} catch (Exception e) {
					Logger.log(e);
				}
			}

			return accounts;
		} catch (Exception e1) {
			Logger.log(e1);
			return new ArrayList<Account>();
		} 		
	}

	public synchronized void saveAccounts(List<AccountService> accounts) {
		Logger.log("Save accounts request", LoggerLevel.VERBOSE);
		for (AccountService account : accounts) {
			saveAccount(account.getAccount(), false);
		}
		
		saveServiceState(accounts);
	}

	public synchronized void saveServiceState(List<AccountService> accounts) {
		Logger.log("Save service state request", LoggerLevel.VERBOSE);
		List<Account> accountViews = new LinkedList<Account>();
		for (AccountService service : accounts) {
			accountViews.add(service.getAccount());
		}
		try {
			saveAccountHeaders(accountViews);
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public void removeAccount(Account account) {
		
		mContext.deleteFile(account.getAccountId());
		mContext.deleteFile(account.getFilename() + PREFERENCES_FILEEXT);

		try {
			List<Account> acco = getAccountHeaders();
			for (int i = acco.size() - 1; i >= 0; i--) {
				if (acco.get(i).getProtocolUid().equalsIgnoreCase(account.getProtocolUid())) {
					acco.remove(i);
					break;
				}
			}
			saveAccountHeaders(acco);
			Logger.log("Account removed #" + account, LoggerLevel.VERBOSE);
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public void saveAccount(final Account account) {
		saveAccount(account, false);
	}

	public void saveAccount(final Account account, final boolean saveHeaders) {
		saveAccount(account, null, saveHeaders);
	}
	
	public void saveAccount(final Account account, List<ProtocolOption> options, final boolean saveHeaders) {
		Logger.log("Save account " + account + (saveHeaders ? " with headers" : ", no headers"), LoggerLevel.VERBOSE);
		if (account == null) {
			return;
		}
		
		Runnable r = new Runnable() {

			@Override
			public void run() {
				saveAccountInternal(account, saveHeaders);
			}
			
		};

		Executors.defaultThreadFactory().newThread(r).start();
		
		if (options != null) {
			saveProtocolOptions(options, account.getAccountId() + " " + SHARED_PREFERENCES);
		}
	}
	
	private synchronized void saveAccountInternal(Account account, boolean saveHeaders) {
		boolean saveNotInList = mContext.getSharedPreferences(account.getAccountId(), 0).getBoolean(AccountOptionKeys.SAVE_NOT_IN_LIST.name(), false);		
		
		XmlSerializer serializer = Xml.newSerializer();
		try {
			serializer.setOutput(new BufferedOutputStream(mContext.openFileOutput(account.getFilename() + PREFERENCES_FILEEXT, ServiceUtils.getAccessMode())), XML_ENCODING);
			serializer.startDocument(XML_ENCODING, true);
			serializer.startTag(XML_NAMESPACE, TAG_ACCOUNT);
			serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_UID, account.getProtocolUid().trim());
			
			if (account.getProtocolName() != null) {
				serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_NAME, account.getProtocolName().trim());
			}
			
			if (account.getProtocolServicePackageName() != null) {
				serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_SERVICE_CLASS_NAME, account.getProtocolServicePackageName().trim());
			}
			
			if (account.getOwnName() != null) {
				serializer.startTag(XML_NAMESPACE, TAG_NAME);
				serializer.text(account.getOwnName());
				serializer.endTag(XML_NAMESPACE, TAG_NAME);
			}

			if (account.getOnlineInfo().getXstatusName() != null) {
				serializer.startTag(XML_NAMESPACE, TAG_XSTATUS_NAME);
				serializer.text(account.getOnlineInfo().getXstatusName());
				serializer.endTag(XML_NAMESPACE, TAG_XSTATUS_NAME);				
			}
			
			if (account.getOnlineInfo().getXstatusDescription() != null) {
				serializer.startTag(XML_NAMESPACE, TAG_XSTATUS_TEXT);
				serializer.text(account.getOnlineInfo().getXstatusDescription());
				serializer.endTag(XML_NAMESPACE, TAG_XSTATUS_TEXT);
			}
			
			serializer.startTag(XML_NAMESPACE, TAG_FEATURES);
			for (String feature: account.getOnlineInfo().getFeatures().keySet()) {
				Object value = account.getOnlineInfo().getFeatures().get(feature);
				
				serializer.startTag(XML_NAMESPACE, TAG_FEATURE);
				serializer.attribute(XML_NAMESPACE, ATTR_ID, feature);
				serializer.attribute(XML_NAMESPACE, ATTR_TYPE, value.getClass().getName());
				serializer.attribute(XML_NAMESPACE, ATTR_VALUE, value.toString());
				serializer.endTag(XML_NAMESPACE, TAG_FEATURE);
			}
			serializer.endTag(XML_NAMESPACE, TAG_FEATURES);

			serializer.startTag(XML_NAMESPACE, TAG_GROUPS);
			serializer.attribute(XML_NAMESPACE, ATTR_GROUPS, Integer.toString(account.getBuddyGroupList().size()));
			
			List<BuddyGroup> buddyGroups = new ArrayList<BuddyGroup>();
			buddyGroups.addAll(account.getBuddyGroupList());
			BuddyGroup noGroup = new BuddyGroup(ApiConstants.NO_GROUP_ID, account.getProtocolUid(), account.getServiceId());
			noGroup.getBuddyList().addAll(account.getNoGroupBuddies());
			buddyGroups.add(noGroup);
			
			for (BuddyGroup group : buddyGroups) {
				serializer.startTag(XML_NAMESPACE, TAG_GROUP);
				serializer.attribute(XML_NAMESPACE, ATTR_ID, group.getId());
				serializer.attribute(XML_NAMESPACE, ATTR_COLLAPSED, Boolean.toString(group.isCollapsed()));

				if (group.getName() != null) {
					serializer.startTag(XML_NAMESPACE, TAG_GROUP_NAME);
					serializer.text(group.getName());
					serializer.endTag(XML_NAMESPACE, TAG_GROUP_NAME);
				}
				
				serializer.startTag(XML_NAMESPACE, TAG_BUDDIES);
				serializer.attribute(XML_NAMESPACE, ATTR_BUDDIES, Integer.toString(account.getBuddyList().size()));
				
				for (Buddy buddy : group.getBuddyList()) {
					
					if (buddy.getGroupId().equals(ApiConstants.NOT_IN_LIST_GROUP_ID) && !saveNotInList) {
						continue;
					}
					
					if (buddy instanceof MultiChatRoom) {
						serializer.startTag(XML_NAMESPACE, TAG_CHAT);
					} else {
						serializer.startTag(XML_NAMESPACE, TAG_BUDDY);
					}
					serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_UID, buddy.getProtocolUid().trim());
					serializer.attribute(XML_NAMESPACE, ATTR_GROUP_ID, buddy.getGroupId());
					serializer.attribute(XML_NAMESPACE, ATTR_ID, Integer.toString(buddy.getId()));
					serializer.attribute(XML_NAMESPACE, ATTR_UNREAD, Byte.toString(buddy.getUnread()));
					
					/*serializer.startTag(XML_NAMESPACE, TAG_FEATURES);
					for (String feature: buddy.getOnlineInfo().getFeatures().keySet()) {
						if (feature.equals(ApiConstants.FEATURE_STATUS)) {
							continue;
						}
						
						Object value = buddy.getOnlineInfo().getFeatures().get(feature);
						
						serializer.startTag(XML_NAMESPACE, TAG_FEATURE);
						serializer.attribute(XML_NAMESPACE, ATTR_ID, feature);
						serializer.attribute(XML_NAMESPACE, ATTR_TYPE, value.getClass().getName());
						serializer.attribute(XML_NAMESPACE, ATTR_VALUE, value.toString());
						serializer.endTag(XML_NAMESPACE, TAG_FEATURE);
					}
					serializer.endTag(XML_NAMESPACE, TAG_FEATURES);*/
					
					if (buddy.getName() != null) {
						serializer.startTag(XML_NAMESPACE, TAG_BUDDY_NAME);
						serializer.text(buddy.getName());
						serializer.endTag(XML_NAMESPACE, TAG_BUDDY_NAME);
					}
					if (buddy instanceof MultiChatRoom) {
						serializer.endTag(XML_NAMESPACE, TAG_CHAT);
					} else {
						serializer.endTag(XML_NAMESPACE, TAG_BUDDY);
					}
				}
				serializer.endTag(XML_NAMESPACE, TAG_BUDDIES);

				serializer.endTag(XML_NAMESPACE, TAG_GROUP);
			}
			serializer.endTag(XML_NAMESPACE, TAG_GROUPS);
			serializer.endTag(XML_NAMESPACE, TAG_ACCOUNT);
			serializer.endDocument();

			if (saveHeaders) {
				List<Account> accounts = getAccountHeaders();

				boolean found = false;
				for (Account acco : accounts) {
					if (acco.getProtocolUid().equalsIgnoreCase(account.getProtocolUid())) {
						// acco.merge(account);
						found = true;
					}
				}

				if (!found) {
					accounts.add(account);
				}

				saveAccountHeaders(accounts);
			}
			
			Logger.log("Account saved " + account, LoggerLevel.VERBOSE);
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	private synchronized void saveAccountHeaders(List<Account> accounts) throws IllegalArgumentException, IllegalStateException, FileNotFoundException, IOException {
		XmlSerializer serializer = Xml.newSerializer();
		serializer.setOutput(new BufferedOutputStream(mContext.openFileOutput(XMLPARAMS_TOTAL, ServiceUtils.getAccessMode())), XML_ENCODING);
		serializer.startDocument(XML_ENCODING, true);

		serializer.startTag(XML_NAMESPACE, TAG_ACCOUNTS);

		for (Account account : accounts) {
			serializer.startTag(XML_NAMESPACE, TAG_ACCOUNT);
			serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_UID, account.getProtocolUid());
			
			if (account.getProtocolName() != null) {
				serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_NAME, account.getProtocolName());
			}
			
			if (account.getProtocolServicePackageName() != null) {
				serializer.attribute(XML_NAMESPACE, ATTR_PROTOCOL_SERVICE_CLASS_NAME, account.getProtocolServicePackageName());
			}
			serializer.attribute(XML_NAMESPACE, ATTR_CONNECTION_STATE, account.getConnectionState().name());
			serializer.endTag(XML_NAMESPACE, TAG_ACCOUNT);
		}
		serializer.endTag(XML_NAMESPACE, TAG_ACCOUNTS);
		serializer.endDocument();
	}
	
	static List<Account> readFileWithAccountList(InputStream stream, String encoding) throws XmlPullParserException, IOException {
		List<Account> accounts = new ArrayList<Account>();

		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(stream, encoding);

		int eventType = parser.getEventType();
		Account account = null;
		boolean done = false;
		while (eventType != XmlPullParser.END_DOCUMENT && !done) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_DOCUMENT:
				break;
			case XmlPullParser.START_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase(TAG_ACCOUNT)) {
					String protocolUid = parser.getAttributeValue(XML_NAMESPACE, ATTR_PROTOCOL_UID);
					String protocolName = parser.getAttributeValue(XML_NAMESPACE, ATTR_PROTOCOL_NAME);
					String protocolServiceClassName = parser.getAttributeValue(XML_NAMESPACE, ATTR_PROTOCOL_SERVICE_CLASS_NAME);
					
					if (protocolUid != null) {
						account = new Account((byte) accounts.size(), protocolUid, protocolName, protocolServiceClassName);
						
						String connectionState = parser.getAttributeValue(XML_NAMESPACE, ATTR_CONNECTION_STATE);					
						if (connectionState != null) {
							account.setConnectionState(ConnectionState.valueOf(connectionState));
						} else {
							account.setConnectionState(ConnectionState.DISCONNECTED);
						}
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase(TAG_ACCOUNT) && account != null) {
					accounts.add(account);
				} else if (name.equalsIgnoreCase(TAG_ACCOUNTS)) {
					done = true;
				}
				break;
			}
			eventType = parser.next();
		}

		return accounts;
	}

	private synchronized List<Account> getAccountHeaders() throws XmlPullParserException {
		try {
			return readFileWithAccountList(mContext.openFileInput(XMLPARAMS_TOTAL), XML_ENCODING);
		} catch (IOException e) {
			Logger.log(e);
		}
		
		return Collections.emptyList();
	}

	public synchronized Account getAccount(Account account, boolean getBuddies) throws XmlPullParserException, IOException {
		if (account == null) {
			return null;
		}

		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(mContext.openFileInput(account.getFilename() + PREFERENCES_FILEEXT), XML_ENCODING);
		
		int eventType = parser.getEventType();
		List<BuddyGroup> buddyGroupList = new ArrayList<BuddyGroup>();
		
		Buddy buddy = null;
		BuddyGroup group = null;
		boolean done = false;
		while (eventType != XmlPullParser.END_DOCUMENT && !done) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_DOCUMENT:
				break;
			case XmlPullParser.START_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase(TAG_FEATURE)) {
					String fid = parser.getAttributeValue(XML_NAMESPACE, ATTR_ID);
					String className = parser.getAttributeValue(XML_NAMESPACE, ATTR_TYPE);
					String value = parser.getAttributeValue(XML_NAMESPACE, ATTR_VALUE);
					
					OnlineInfo info;
					if (buddy != null) {
						info = buddy.getOnlineInfo();
					} else if (account != null) {
						info = account.getOnlineInfo();
					} else {
						parser.next();
						continue;
					}
					
					if (className.equals(Byte.class.getName())) {
						info.getFeatures().putByte(fid, Byte.parseByte(value));
					} else if (className.equals(Short.class.getName())) {
						info.getFeatures().putShort(fid, Short.parseShort(value));
					} else if (className.equals(Integer.class.getName())) {
						info.getFeatures().putInt(fid, Integer.parseInt(value));
					} else if (className.equals(Long.class.getName())) {
						info.getFeatures().putLong(fid, Long.parseLong(value));
					} else if (className.equals(Boolean.class.getName())) {
						info.getFeatures().putBoolean(fid, Boolean.parseBoolean(value));
					} else if (className.equals(Double.class.getName())) {
						info.getFeatures().putDouble(fid, Double.parseDouble(value));
					} else if (className.equals(Character.class.getName())) {
						info.getFeatures().putChar(fid, value.charAt(0));
					} else if (className.equals(Float.class.getName())) {
						info.getFeatures().putFloat(fid, Float.parseFloat(value));
					} else {
						info.getFeatures().putString(fid, value);
					}
				} else if (account != null) {
					if (name.equalsIgnoreCase(TAG_NAME)) {
						if (account != null) {
							account.setOwnName(parser.nextText());
						}
					} else if (name.equalsIgnoreCase(TAG_XSTATUS_NAME)) {
						if (account != null) {
							account.getOnlineInfo().setXstatusName(parser.nextText());
						}
					} else if (name.equalsIgnoreCase(TAG_XSTATUS_TEXT)) {
						if (account != null) {
							account.getOnlineInfo().setXstatusDescription(parser.nextText());
						}
					} else if (name.equalsIgnoreCase(TAG_BUDDY_NAME)) {
						if (buddy != null) {
							buddy.setName(parser.nextText());
						}
					} else if (name.equalsIgnoreCase(TAG_GROUP_NAME)) {
						if (group != null) {
							group.setName(parser.nextText());
						}
					} else if (name.equalsIgnoreCase(TAG_BUDDY) && getBuddies) {
						buddy = new Buddy(parser.getAttributeValue(XML_NAMESPACE, ATTR_PROTOCOL_UID), account.getProtocolUid(), account.getProtocolName(), account.getServiceId());

						fillBuddy(buddy, parser);
					} else if (name.equalsIgnoreCase(TAG_CHAT) && getBuddies) {
						buddy = new MultiChatRoom(parser.getAttributeValue(XML_NAMESPACE, ATTR_PROTOCOL_UID), account.getProtocolUid(), account.getProtocolName(), account.getServiceId());

						fillBuddy(buddy, parser);
					} else if (name.equalsIgnoreCase(TAG_GROUP) && getBuddies) {
						group = new BuddyGroup(parser.getAttributeValue(XML_NAMESPACE, ATTR_ID), account.getProtocolUid(), account.getServiceId());
						group.setCollapsed(Boolean.parseBoolean(parser.getAttributeValue(XML_NAMESPACE, ATTR_COLLAPSED)));
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase(TAG_BUDDY) || name.equalsIgnoreCase(TAG_CHAT)) {
					if (buddy != null && group != null) {
						group.getBuddyList().add(buddy);
						buddy.getOnlineInfo().getFeatures().remove(ApiConstants.FEATURE_STATUS);
						buddy = null;
					}
				} else if (name.equalsIgnoreCase(TAG_GROUP)) {
					if (group != null) {
						buddyGroupList.add(group);
						group = null;
					}
				} else if (name.equalsIgnoreCase(TAG_ACCOUNT)) {
					done = true;
				}
				break;
			}
			eventType = parser.next();
		}
		
		account.setBuddyList(buddyGroupList);
		
		return account;
	}

	private void fillBuddy(Buddy buddy, XmlPullParser parser) {
		buddy.setGroupId(parser.getAttributeValue(XML_NAMESPACE, ATTR_GROUP_ID));
		buddy.setId(Integer.parseInt(parser.getAttributeValue(XML_NAMESPACE, ATTR_ID)));
		buddy.setUnread(Byte.parseByte(parser.getAttributeValue(XML_NAMESPACE, ATTR_UNREAD)));
	}

	public synchronized void delete(String storageName) {
		Logger.log("Delete storage file " + storageName, LoggerLevel.VERBOSE);
		mContext.deleteFile(storageName);
	}
}
