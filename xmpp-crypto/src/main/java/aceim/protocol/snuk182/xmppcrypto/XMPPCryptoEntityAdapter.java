package aceim.protocol.snuk182.xmppcrypto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.packet.EncryptedMessage;
import org.jivesoftware.smackx.packet.SignedPresence;
import org.jivesoftware.smackx.provider.EncryptedDataProvider;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.protocol.snuk182.xmpp.common.XMPPApiConstants;
import aceim.protocol.snuk182.xmpp.common.XMPPEntityAdapter;
import aceim.protocol.snuk182.xmpp.common.XMPPServiceInternal;
import aceim.protocol.snuk182.xmppcrypto.utils.ResourceUtils;
import android.content.Context;
import android.text.TextUtils;

public class XMPPCryptoEntityAdapter extends XMPPEntityAdapter {
	
	static final byte INVISIBLE_STATUS_ID = 5;
	private static final Mode[] presenceModes = {Mode.available, Mode.away, Mode.xa, Mode.dnd, Mode.chat};
	
	private EncryptedDataProvider edProvider = null;
	
	@Override
	public TextMessage xmppMessage2TextMessage(Message message, XMPPServiceInternal service, boolean resourceAsWriterId){
		if (message == null){
			return null;
		}
		
		TextMessage txtMessage;
		if (resourceAsWriterId){
			txtMessage = new TextMessage(service.getOnlineInfo().getServiceId(), StringUtils.parseBareAddress(message.getFrom()));
			txtMessage.setContactDetail(StringUtils.parseResource(message.getFrom()));
		} else {
			String from	= normalizeJID(message.getFrom());
			txtMessage = new TextMessage(service.getOnlineInfo().getServiceId(), from);
		}
		txtMessage.setMessageId(message.getPacketID() != null ? message.getPacketID().hashCode() : message.hashCode());
		txtMessage.setTime(System.currentTimeMillis());
		txtMessage.setText(message.getBody());
		txtMessage.setIncoming(true);
		
		if (edProvider != null){
			PacketExtension ext = message.getExtension("x", "jabber:x:encrypted");
			if (ext != null){
				EncryptedMessage ems = (EncryptedMessage) ext;
				try {
					txtMessage.setText(ems.decryptAndGet(edProvider.getMyKey(), edProvider.getMyKeyPw()));	
				} catch (Exception e) {
					Logger.log(e);
				}
				
				if (edProvider.getKeyStorage().get(txtMessage.getContactUid()) != null){
					OnlineInfo info = service.getRosterListener().getPresenceCache().get(txtMessage.getContactUid());
					if (info != null && !info.getFeatures().containsKey(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_ON)) {
						info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_ON, true);
						info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF);
						
						service.getService().getCoreService().buddyStateChanged(Arrays.asList(info));
					}
				}
			}
		}
		
		return txtMessage;
	}

	@Override
	public Presence userStatus2XMPPPresence(Byte status) {
		Presence presence;
		
		if (status < 0 || status >= presenceModes.length) {
			presence = new Presence(Type.unavailable);
		} else {
			presence = new Presence(Type.available);
			presence.setMode(presenceModes[status]);
		}
		
		if (edProvider != null){
			SignedPresence spr = new SignedPresence();
			try {
				spr.signAndSet(presence.getStatus(), edProvider.getMyKey(), edProvider.getMyKeyPw());
				presence.addExtension(spr);
			} catch (XMPPException e) {
				Logger.log(e);
			}
		}
		
		return presence;
	}
	
	@Override
	public OnlineInfo presence2OnlineInfo(Presence presence, Context context, byte serviceId, String ownerJid, OnlineInfo onlineInfo){
		if (presence == null){
			return null;
		}
		
		String jid = normalizeJID(presence.getFrom());

		OnlineInfo info;		
		if (onlineInfo != null && jid.equals(onlineInfo.getProtocolUid())) {
			info = onlineInfo;
		} else {
			info = new OnlineInfo(serviceId, jid);
		}
		
		String resource = StringUtils.parseResource(presence.getFrom());
		if (!TextUtils.isEmpty(resource)) {
			info.getFeatures().putString(ApiConstants.FEATURE_BUDDY_RESOURCE, resource);
		}
		
		info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, xmppPresence2UserStatus(presence));
		info.setXstatusName(presence.getStatus());

		info.getFeatures().putBoolean(ApiConstants.FEATURE_FILE_TRANSFER, true);
		
		if (edProvider != null){
			
			String buddyPGPKey = context.getSharedPreferences(ownerJid, 0).getString(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid(), null);
			if (buddyPGPKey != null) {
				info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF, true);
				info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_REMOVE_PUBLIC_KEY, true);		
				
				PacketExtension ext = presence.getExtension("x", "jabber:x:signed");
				if (ext != null){
					SignedPresence spr = (SignedPresence) ext;
					try {
						info.setXstatusName(spr.verifyAndGet(buddyPGPKey));
					} catch (Exception e) {
						Logger.log(e);
					}
				} 
			} else {
				info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ADD_PUBLIC_KEY, true);
			}
		} 
		
		return info;
	}
	
	@Override
	public Buddy rosterEntry2Buddy(RosterEntry entry, String ownerJid, Context context, byte serviceId){
		if (entry == null){
			return null;
		}
		Buddy buddy = new Buddy(normalizeJID(entry.getUser()), ownerJid, XMPPApiConstants.PROTOCOL_NAME, serviceId);
		buddy.setName(entry.getName());
		buddy.setId(entry.getUser().hashCode());
		buddy.setGroupId((entry.getGroups() != null && entry.getGroups().size() > 0) ? entry.getGroups().iterator().next().getName() : ApiConstants.NO_GROUP_ID);
		//buddy.clientId = getClientId(entry.getUser());
		
		if (entry.getStatus()!=null && entry.getStatus().equals(RosterPacket.ItemStatus.SUBSCRIPTION_PENDING)){
			//buddy.visibility = Buddy.VIS_NOT_AUTHORIZED;
		}
		
		OnlineInfo info = buddy.getOnlineInfo();
		
		if (edProvider != null){
			String buddyPGPKey = context.getSharedPreferences(ownerJid, 0).getString(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid(), null);
			
			if (buddyPGPKey != null) {
				//info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ENCRYPTION_OFF, true);
				info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_REMOVE_PUBLIC_KEY, true);
				edProvider.getKeyStorage().put(info.getProtocolUid(), null);
			} else {
				info.getFeatures().putBoolean(XMPPApiConstants.FEATURE_ADD_PUBLIC_KEY, true);
			}
		}
		
		return buddy;
	}
	
	@Override
	public BuddyGroup rosterGroup2BuddyGroup(RosterGroup entry, Collection<RosterEntry> buddies, String ownerUid, Context context, byte serviceId){
		if (entry == null){
			return null;
		}
		BuddyGroup group = new BuddyGroup(entry.getName(), ownerUid, serviceId);
		group.setName(entry.getName());
		
		for (RosterEntry buddy: entry.getEntries()){
			buddies.remove(buddy);
			group.getBuddyList().add(rosterEntry2Buddy(buddy, ownerUid, context, serviceId));
		}
		return group;
	}
	
	@Override
	public List<BuddyGroup> rosterGroupCollection2BuddyGroupList(Collection<RosterGroup> entries, Collection<RosterEntry> buddies, String ownerUid, Context context, byte serviceId){
		if (entries == null){
			return null;
		}
		
		List<RosterEntry> list = new ArrayList<RosterEntry>(buddies);
		
		List<BuddyGroup> groups = new ArrayList<BuddyGroup>(entries.size());
		for (RosterGroup entry: entries){
			groups.add(rosterGroup2BuddyGroup(entry, list, ownerUid, context, serviceId));
		}
		
		BuddyGroup noGroup = new BuddyGroup(ApiConstants.NO_GROUP_ID, ownerUid, serviceId);
		for (RosterEntry buddyWithNoGroup : list) {
			noGroup.getBuddyList().add(rosterEntry2Buddy(buddyWithNoGroup, ownerUid, context, serviceId));
		}
		groups.add(noGroup);
		
		return groups;
	}

	@Override
	public Message textMessage2XMPPMessage(TextMessage textMessage, String thread, String to, Message.Type messageType) throws Exception {
		Message message = new Message(to, messageType);
		message.setThread(thread);
		message.setPacketID(textMessage.getMessageId() + "");
		MessageEventManager.addNotificationsRequests(message, true, true, true, true);
		
		String buddyKey = null;
		if (edProvider != null && (buddyKey = edProvider.getKeyStorage().get(textMessage.getContactUid())) != null){
			EncryptedMessage ems = new EncryptedMessage();
			ems.setAndEncrypt(textMessage.getText(), buddyKey);
			//TODO
			message.setBody("Encrypted message");			
			message.addExtension(ems);
		} else {
			message.setBody(textMessage.getText());			
		}
		
		return message;
	}
	
	/**
	 * @return the edProvider
	 */
	public EncryptedDataProvider getEdProvider() {
		return edProvider;
	}
	
	public void setEdProvider(EncryptedDataProvider edProvider) {
		this.edProvider = edProvider;
	}
}
