package aceim.protocol.snuk182.xmppcrypto;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolException;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.VCard;

import android.os.RemoteException;

public class XMPPRosterListener extends XMPPListener implements RosterListener, PacketListener, PacketFilter {

	private volatile boolean isContactListReady = false;
	
	//private final List<OnlineInfo> infos = Collections.synchronizedList(new ArrayList<OnlineInfo>());
	private final Map<String, OnlineInfo> presenceCache = new ConcurrentHashMap<String, OnlineInfo>();
	
	public XMPPRosterListener(XMPPService service) {
		super(service);
	}

	@Override
	public void entriesUpdated(Collection<String> addresses) {
		try {
			clUpdated();
		} catch (ProtocolException e) {
			Logger.log(e);
		}
	}

	@Override
	public void entriesDeleted(Collection<String> addresses) {
		try {
			clUpdated();
		} catch (ProtocolException e) {
			Logger.log(e);
		}
	}

	@Override
	public void entriesAdded(Collection<String> addresses) {
		try {
			clUpdated();
		} catch (ProtocolException e) {
			Logger.log(e);
		}
	}
	
	void clUpdated() throws ProtocolException {
		List<BuddyGroup> groups = getContactList();
		
		try {
			getService().getProtocolService().getCallback().buddyListUpdated(getService().getServiceId(), groups);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}
	
	List<BuddyGroup> getContactList() {
		Roster roster = getService().getConnection().getRoster();
		
		Collection<RosterEntry> entries = roster.getEntries();
		for (RosterEntry entry : entries) {
			if (entry.getName() == null) {
				try {
					VCard vc = new VCard();
					vc.load(getService().getConnection(), entry.getUser());
					String nick = getNicknameFromVCard(vc);
					
					if (nick != null) {
						entry.setName(nick);
					}
				} catch (XMPPException e) {
					Logger.log(e);
				}
			}
		}
		
		List<BuddyGroup> groups = XMPPEntityAdapter.rosterGroupCollection2BuddyGroupList(roster.getGroups(), entries, getService().getOnlineInfo().getProtocolUid(), getService().getEdProvider(), getService().getProtocolService(), getService().getServiceId());
		return groups;
	}

	@Override
	public void presenceChanged(Presence presence) {
		Logger.log(" - presence " + presence.getFrom() + " " + presence.getMode(), LoggerLevel.VERBOSE);

		OnlineInfo info = XMPPEntityAdapter.presence2OnlineInfo(presence, getService().getEdProvider(), getService().getProtocolService(), getService().getServiceId(), getService().getProtocolUid(), presenceCache.get(XMPPEntityAdapter.normalizeJID(presence.getFrom())));
		
		presenceCache.put(info.getProtocolUid(), info);
		
		if (isContactListReady) {
			try {
				getService().getProtocolService().getCallback().buddyStateChanged(info);
			} catch (RemoteException e) {
				Logger.log(e);
			}
		} 
	}

	void checkCachedInfos() {
		while (presenceCache.size() > 0) {
			for (Iterator<String> i = presenceCache.keySet().iterator(); i.hasNext();) {
				OnlineInfo info = presenceCache.remove(i.next());
				i.remove();
				try {
					getService().getProtocolService().getCallback().buddyStateChanged(info);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}
	}
	
	@Override
	public boolean accept(Packet packet) {
		if (packet instanceof Presence) {
			Presence p = (Presence) packet;
			return ((Presence.Type.subscribe.equals(p.getType())) 
				|| (Presence.Type.unsubscribe.equals(p.getType()))
				|| (Presence.Type.subscribed.equals(p.getType()))
				|| (Presence.Type.unsubscribed.equals(p.getType())));
		}
		return false;
	}

	@Override
	public void processPacket(Packet packet) {
		if (packet instanceof Presence) {
			Presence p = (Presence) packet;
			if (Presence.Type.subscribe.equals(p.getType())) {	
				ServiceMessage message = new ServiceMessage(getService().getServiceId(), XMPPEntityAdapter.normalizeJID(p.getFrom()), true);
				message.setText(p.getFrom());
				message.setContactDetail(getService().getProtocolService().getString(R.string.ask_authorization));
				
				try {
					getService().getProtocolService().getCallback().message(message);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			
			} else if (Presence.Type.unsubscribe.equals(p.getType())) {					
				Presence ppacket = new Presence(Presence.Type.unsubscribed);
				ppacket.setTo(packet.getFrom());
				ppacket.setFrom(packet.getTo());
				getService().getConnection().sendPacket(ppacket);					
			} else if (Presence.Type.unsubscribed.equals(p.getType()) || Presence.Type.subscribed.equals(p.getType())) {
				try {
					clUpdated();
				} catch (ProtocolException e) {
					Logger.log(e);
				}
			} else {
				presenceChanged(p);
			}
		}
	}
	
	void renameBuddy(Buddy buddy) {
		Roster roster = getService().getConnection().getRoster();
		RosterEntry buddyEntry = roster.getEntry(buddy.getProtocolUid());
		buddyEntry.setName(buddy.getName());
		try {
			getService().getProtocolService().getCallback().buddyAction(ItemAction.MODIFIED, buddy);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	void renameGroup(final BuddyGroup buddyGroup) {
		new Thread() {

			@Override
			public void run() {
				try {
					RosterGroup rgroup = XMPPEntityAdapter.buddyGroup2RosterEntry(getService().getConnection(), buddyGroup);

					for (RosterEntry entry : rgroup.getEntries()) {
						getService().getConnection().getRoster().createEntry(entry.getUser(), entry.getName(), new String[] { buddyGroup.getName() });
					}

					getService().getProtocolService().getCallback().groupAction(ItemAction.MODIFIED, buddyGroup);
				} catch (Exception e) {
					Logger.log(e);
					try {
						getService().getProtocolService().getCallback().notification(getService().getServiceId(), e.getLocalizedMessage());
					} catch (RemoteException e1) {
						Logger.log(e1);
					}
				}
			}
		}.start();
	}

	void moveBuddy(final Buddy buddy) {
		new Thread() {
			@Override
			public void run() {
				try {
					Roster roster = getService().getConnection().getRoster();
					roster.createEntry(buddy.getProtocolUid(), buddy.getName(), (buddy.getGroupId() != null && buddy.getGroupId().equals(ApiConstants.NO_GROUP_ID)) ? new String[] { buddy.getGroupId() } : new String[0]);
					buddy.setGroupId(buddy.getGroupId());
					getService().getProtocolService().getCallback().buddyAction(ItemAction.MODIFIED, buddy);
				} catch (XMPPException e) {
					Logger.log(e);
					try {
						getService().getProtocolService().getCallback().notification(getService().getServiceId(), e.getLocalizedMessage());
					} catch (RemoteException e1) {
						Logger.log(e1);
					}
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}.start();
	}

	void removeGroup(final BuddyGroup buddyGroup) {
		new Thread() {

			@Override
			public void run() {
				try {
					RosterGroup rgroup = XMPPEntityAdapter.buddyGroup2RosterEntry(getService().getConnection(), buddyGroup);
					Roster roster = getService().getConnection().getRoster();
					
					for (RosterEntry entry : rgroup.getEntries()) {
						//getService().getConnection().getRoster().removeEntry(entry);
						
						roster.createEntry(entry.getUser(), entry.getName(), new String[0]);
						
						Buddy buddy = XMPPEntityAdapter.rosterEntry2Buddy(entry, buddyGroup.getOwnerUid(), getService().getEdProvider(), getService().getProtocolService(), buddyGroup.getServiceId());
						buddy.setGroupId(ApiConstants.NO_GROUP_ID);
						
						getService().getProtocolService().getCallback().buddyAction(ItemAction.MODIFIED, buddy);
					}

					getService().getProtocolService().getCallback().groupAction(ItemAction.DELETED, buddyGroup);
				} catch (Exception e) {
					Logger.log(e);
					try {
						getService().getProtocolService().getCallback().notification(getService().getServiceId(), e.getLocalizedMessage());
					} catch (RemoteException e1) {
						Logger.log(e1);
					}
				}
			}
		}.start();
	}

	void removeBuddy(final Buddy buddy) {
		new Thread() {

			@Override
			public void run() {
				try {
					getService().getConnection().getRoster().removeEntry(XMPPEntityAdapter.buddy2RosterEntry(getService().getConnection(), buddy));
					getService().getProtocolService().getCallback().buddyAction(ItemAction.DELETED, buddy);
				} catch (XMPPException e) {
					Logger.log(e);
					try {
						getService().getProtocolService().getCallback().notification(getService().getServiceId(), e.getLocalizedMessage());
					} catch (RemoteException e1) {
						Logger.log(e1);
					}
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}

		}.start();
	}

	void addGroup(final BuddyGroup buddyGroup) {
		new Thread() {

			@Override
			public void run() {
				try {
					BuddyGroup g = new BuddyGroup(buddyGroup.getName(), buddyGroup.getOwnerUid(), buddyGroup.getServiceId());
					g.setName(buddyGroup.getName());
					
					getService().getConnection().getRoster().createGroup(g.getName());
					getService().getProtocolService().getCallback().groupAction(ItemAction.ADDED, g);
				} catch (RemoteException e) {
					Logger.log(e);
					try {
						getService().getProtocolService().getCallback().notification(getService().getServiceId(), e.getLocalizedMessage());
					} catch (RemoteException e1) {
						Logger.log(e1);
					}
				}
			}
		}.start();
	}

	void addBuddy(final Buddy buddy) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				try {
					Roster roster = getService().getConnection().getRoster();
					roster.createEntry(buddy.getProtocolUid(), buddy.getName(), (buddy.getGroupId() != null && !buddy.getGroupId().equals(ApiConstants.NO_GROUP_ID)) ? new String[] { buddy.getGroupId() } : new String[0]);
					getService().getProtocolService().getCallback().buddyAction(ItemAction.ADDED, XMPPEntityAdapter.rosterEntry2Buddy(roster.getEntry(buddy.getProtocolUid()), getService().getProtocolUid(), getService().getEdProvider(), getService().getProtocolService(), getService().getServiceId()));
				} catch (XMPPException e) {
					Logger.log(e);
					try {
						getService().getProtocolService().getCallback().notification(getService().getServiceId(), e.getLocalizedMessage());
					} catch (RemoteException e1) {
						Logger.log(e1);
					}
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}

	/**
	 * @return the isContactListReady
	 */
	public boolean isContactListReady() {
		return isContactListReady;
	}

	/**
	 * @param isContactListReady the isContactListReady to set
	 */
	public void setContactListReady(boolean isContactListReady) {
		this.isContactListReady = isContactListReady;
	}
	
	public void loadCard(String jid) {
		Executors.defaultThreadFactory().newThread(new PersonalInfoRunnable(jid, PersonalInfoTarget.ICON, false)).start();
	}
	
	public void getBuddyInfo(String jid, boolean shortInfo, boolean isMultiUserChat) {
		PersonalInfoTarget target;
		
		if (shortInfo) {
			target = PersonalInfoTarget.SHORT;
		} else {
			target = PersonalInfoTarget.ALL;
		}
		
		Executors.defaultThreadFactory().newThread(new PersonalInfoRunnable(jid, target, isMultiUserChat)).start();
	}
	
	@Override
	void onDisconnect() {
		presenceCache.clear();
	}

	private String getNicknameFromVCard(VCard card) {
		String fn;
		if (card.getNickName() != null && card.getNickName().length() > 0) {
			fn = card.getNickName();
		} else {
			fn = card.getField("FN");
		}
		
		return fn;
	}
	
	/**
	 * @return the presenceCache
	 */
	Map<String, OnlineInfo> getPresenceCache() {
		return presenceCache;
	}

	private class PersonalInfoRunnable implements Runnable {

		private final String uid;
		private final PersonalInfoTarget target;
		private final boolean isMultiUserChat;

		private PersonalInfoRunnable(String uid, PersonalInfoTarget target, boolean isMultiUserChat) {
			this.uid = uid;
			this.target = target;
			this.isMultiUserChat = isMultiUserChat;
		}

		@Override
		public void run() {
			try {
				PersonalInfo info = new PersonalInfo(getService().getServiceId());
				info.setProtocolUid(uid);
				
				VCard card = new VCard();
				
				if (isMultiUserChat) {
					try {
						RoomInfo room = MultiUserChat.getRoomInfo(getService().getConnection(), uid);
						info.getProperties().putCharSequence(PersonalInfo.INFO_CHAT_DESCRIPTION, room.getDescription());
						info.getProperties().putCharSequence(PersonalInfo.INFO_CHAT_OCCUPANTS, room.getOccupantsCount() + "");
						info.getProperties().putCharSequence(PersonalInfo.INFO_CHAT_SUBJECT, room.getSubject());
						
						getService().getProtocolService().getCallback().personalInfo(info);
						return;
					} catch (XMPPException e) {
						Logger.log(e.getLocalizedMessage(), LoggerLevel.DEBUG);
					}
				}
				
				try {
					card.load(getService().getConnection(), uid);
					
					switch (target) {
					case ALL:
						for (String prop: getAllFieldsOfCard(card).keySet()){
							info.getProperties().putCharSequence(prop, card.getField(prop));							
						}
					case SHORT:
						String fn = getNicknameFromVCard(card);
						
						if (fn != null) {
							info.getProperties().putString(PersonalInfo.INFO_NICK, fn);
						}
						getService().getProtocolService().getCallback().personalInfo(info);
					case ICON:
						if (card.getAvatar() != null) {
							getService().getProtocolService().getCallback().iconBitmap(getService().getServiceId(), uid, card.getAvatar(), card.getAvatarHash());
						}
						break;
					
					}
				} catch (XMPPException e) {
					Logger.log(e);
				}
			} catch (RemoteException e) {
				Logger.log(e);
			}			
		}
		

		@SuppressWarnings("unchecked")
		private final Map<String, String> getAllFieldsOfCard(VCard card) {
			try {
				Field f = VCard.class.getDeclaredField("otherSimpleFields");
				f.setAccessible(true);
				return (Map<String, String>) f.get(card);
			} catch (Exception e) {
				Logger.log(e);
			}
			
			return Collections.emptyMap();
		}
	}
	
	private enum PersonalInfoTarget {
		ALL,
		SHORT,
		ICON
	}

	public void authorizationResponse(String contactUid, boolean accept) {
		Presence subscribe = new Presence(accept ? Presence.Type.subscribed : Presence.Type.unsubscribed);
	    subscribe.setTo(contactUid);
	    getService().getConnection().sendPacket(subscribe);
	}

	public void uploadIcon(final byte[] bytes) {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					VCard card = new VCard();
					card.load(getService().getConnection(),getService().getProtocolUid());
					card.setAvatar(bytes);
					card.save(getService().getConnection());
					getService().getProtocolService().getCallback().iconBitmap(getService().getServiceId(), getService().getProtocolUid(), bytes, card.getAvatarHash());
				} catch (XMPPException e) {
					Logger.log(e);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		}).start();
	}
}
