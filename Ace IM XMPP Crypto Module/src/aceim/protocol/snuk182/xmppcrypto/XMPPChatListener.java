package aceim.protocol.snuk182.xmppcrypto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolException;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.ChatStateListener;
import org.jivesoftware.smackx.DefaultMessageEventRequestListener;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.MessageEventNotificationListener;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;

import android.os.RemoteException;
import android.text.TextUtils;

public class XMPPChatListener extends XMPPListener implements ChatManagerListener, ChatStateListener, MessageEventNotificationListener {

	private static final Random RANDOMIZER = new Random();
	
	private final Map<String, Chat> chats = new HashMap<String, Chat>();	
	private final Map<String, MultiUserChat> multichats = new HashMap<String, MultiUserChat>();
	
	private final DefaultMessageEventRequestListener messageEventListener = new DefaultMessageEventRequestListener();
	
	private MessageEventManager messageEventManager;
	
	private final Runnable getGroupchatsRunnable = new Runnable() {
		
		@Override
		public void run() {
			try {
				getAvailableChatRooms();
			} catch (ProtocolException e) {
				Logger.log(e);
			}
		}
	};
	
	public XMPPChatListener(XMPPService service) {
		super(service);
	}

	@Override
	public void chatCreated(Chat chat, boolean isLocal) {
		Logger.log("chat " + chat.getParticipant(), LoggerLevel.VERBOSE);

		if (chats.get(XMPPEntityAdapter.normalizeJID(chat.getParticipant())) == null) {
			chat.addMessageListener(this);
			chats.put(XMPPEntityAdapter.normalizeJID(chat.getParticipant()), chat);
		}
	}

	@Override
	public void processMessage(Chat chat, Message message) {
		if (message.getBody() == null) {
			Logger.log("Empty message from " + chat.getParticipant(), LoggerLevel.WARNING);
		} else {
			Logger.log("Message from " + chat.getParticipant(), LoggerLevel.VERBOSE);			
			processMessageInternal(message, false);
		}
	}

	@Override
	public void stateChanged(Chat chat, ChatState state) {
		if (state == ChatState.composing) {
			try {
				getService().getProtocolService().getCallback().typingNotification(getService().getServiceId(), XMPPEntityAdapter.normalizeJID(chat.getParticipant()));
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}
	}

	@Override
	public void cancelledNotification(String from, String packetID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void composingNotification(String from, String packetID) {
		try {
			getService().getProtocolService().getCallback().typingNotification(getService().getServiceId(), XMPPEntityAdapter.normalizeJID(from));
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	@Override
	public void deliveredNotification(String from, String packetID) {
		long messageId = Long.parseLong(packetID);
		Logger.log(getService().getProtocolUid() + " - " + from + " delivered " + messageId, LoggerLevel.VERBOSE);
		try {
			getService().getProtocolService().getCallback().messageAck(getService().getServiceId(), XMPPEntityAdapter.normalizeJID(from), messageId, MessageAckState.RECIPIENT_ACK);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	@Override
	public void displayedNotification(String from, String packetID) {
		long messageId = Long.parseLong(packetID);
		Logger.log(getService().getProtocolUid() + " - " + from + " displayed " + messageId, LoggerLevel.VERBOSE);
		try {
			getService().getProtocolService().getCallback().messageAck(getService().getServiceId(), XMPPEntityAdapter.normalizeJID(from), messageId, MessageAckState.READ_ACK);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	@Override
	public void offlineNotification(String from, String packetID) {
		long messageId = Long.parseLong(packetID);
		Logger.log(getService().getProtocolUid() + " - " + from + " offline " + messageId, LoggerLevel.VERBOSE);
		try {
			getService().getProtocolService().getCallback().messageAck(getService().getServiceId(), XMPPEntityAdapter.normalizeJID(from), messageId, MessageAckState.SERVER_ACK);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}
	
	void processMessageInternal(final Message message, boolean resourceAsWriterId) {
		TextMessage txtmessage = XMPPEntityAdapter.xmppMessage2TextMessage(message, getService(), resourceAsWriterId);
		try {
			getService().getProtocolService().getCallback().message(txtmessage);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}
	
	void sendTyping(String jid) {
		messageEventManager.sendComposingNotification(jid, Integer.toHexString(RANDOMIZER.nextInt()));	
	}

	long sendMessage(TextMessage textMessage) throws Exception {
		MultiUserChat muc = multichats.get(textMessage.getContactUid());

		if (muc != null) {
			Message m = muc.createMessage();
			m.setBody(textMessage.getText());
			muc.sendMessage(m);
			return m.getPacketID().hashCode();
		}

		Chat chat = chats.get(textMessage.getContactUid());
		if (chat == null) {
			chat = getService().getConnection().getChatManager().createChat(textMessage.getContactUid(), this);
			chats.put(textMessage.getContactUid(), chat);
		}
		
		Message packet = XMPPEntityAdapter.textMessage2XMPPMessage(textMessage, chat.getThreadID(), chat.getParticipant(), Message.Type.chat, getService().getEdProvider());
		chat.sendMessage(packet);
		return packet.getPacketID().hashCode();
	}
	
	void chatDefaultAction(String chatId, String serviceMessage) {
		try {
			ServiceMessage message = new ServiceMessage(getService().getServiceId(), chatId, false);
			message.setText(serviceMessage);
			getService().getProtocolService().getCallback().message(message);
			
			getService().getProtocolService().getCallback().multiChatParticipants(getService().getServiceId(), chatId, getChatRoomOccupants(chatId, false));
		} catch (RemoteException e) {
			Logger.log(e);
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	List<BuddyGroup> getChatRoomOccupants(String chatId, boolean loadOccupantIcons) throws ProtocolException {
		MultiUserChat muc = multichats.get(chatId);
		if (muc == null) {
			throw new ProtocolException("No joined chat found");
		}

		return XMPPEntityAdapter.xmppMUCOccupants2mcrOccupants(getService(), muc, loadOccupantIcons);
	}

	

	/**
	 * @return the messageEventListener
	 */
	public DefaultMessageEventRequestListener getMessageEventListener() {
		return messageEventListener;
	}

	/**
	 * @param messageEventManager the messageEventManager to set
	 */
	public void setMessageEventManager(MessageEventManager messageEventManager) {
		this.messageEventManager = messageEventManager;
		
		if (messageEventManager == null){
			return;
		}		

		if (getService().getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) 0) !=  XMPPEntityAdapter.INVISIBLE_STATUS_ID) {
			messageEventManager.addMessageEventRequestListener(messageEventListener);
		}
		
		messageEventManager.addMessageEventNotificationListener(this);	
	}
	
	boolean amIOwner(MultiUserChat chat) throws XMPPException {
		for (Affiliate aff : chat.getOwners()) {
			if (XMPPEntityAdapter.normalizeJID(aff.getJid()).equals(getService().getProtocolUid())) {
				return true;
			}
		}
		return false;
	}
	
	void fillWithListeners(final MultiUserChat chat) {
		chat.addMessageListener(new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				Message message = (Message) packet;
				if (message.getFrom().split("/")[1].equals(chat.getNickname())) {
					Logger.log("Message from myself!");
					return;
				}
				processMessageInternal(message, true);
			}
		});

		chat.addParticipantListener(new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				try {
					getService().getProtocolService().getCallback().multiChatParticipants(getService().getServiceId(), chat.getRoom(), getChatRoomOccupants(chat.getRoom(), false));
				} catch (RemoteException e) {
					Logger.log(e);
				} catch (ProtocolException e) {
					Logger.log(e);
				}
			}
		});

		chat.addInvitationRejectionListener(new InvitationRejectionListener() {

			@Override
			public void invitationDeclined(String invitee, String reason) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(invitee) + " declined invitation: " + reason);
			}
		});

		chat.addParticipantStatusListener(new ParticipantStatusListener() {

			@Override
			public void voiceRevoked(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " voice revoked");
			}

			@Override
			public void voiceGranted(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " voice granted");
			}

			@Override
			public void ownershipRevoked(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " ownership revoked");
			}

			@Override
			public void ownershipGranted(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " ownership granted");
			}

			@Override
			public void nicknameChanged(String participant, String newNickname) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " changed nick to " + newNickname);
			}

			@Override
			public void moderatorRevoked(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " moderator revoked");
			}

			@Override
			public void moderatorGranted(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " moderator granted");
			}

			@Override
			public void membershipRevoked(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " membership revoked");
			}

			@Override
			public void membershipGranted(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " membership granted");
			}

			@Override
			public void left(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " left the chat");
			}

			@Override
			public void kicked(String participant, String actor, String reason) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " was kicked by " + actor + ": " + reason);
			}

			@Override
			public void joined(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " joined the chat");
			}

			@Override
			public void banned(String participant, String actor, String reason) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " was banned by " + actor + ": " + reason);
			}

			@Override
			public void adminRevoked(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " admin revoked");
			}

			@Override
			public void adminGranted(String participant) {
				chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " admin granted");
			}
		});

		chat.addSubjectUpdatedListener(new SubjectUpdatedListener() {

			@Override
			public void subjectUpdated(String subject, String from) {
				try {
					ServiceMessage sm = new ServiceMessage(getService().getServiceId(), chat.getRoom(), false);
					sm.setText(StringUtils.parseResource(from) + " set chat topic to \"" + subject + "\"");
					getService().getProtocolService().getCallback().message(sm);
					
					Buddy buddy = XMPPEntityAdapter.chatInfo2Buddy(chat.getRoom(), subject, getService().getProtocolUid(), getService().getServiceId(), true);
					getService().getProtocolService().getCallback().buddyAction(ItemAction.MODIFIED, buddy);
				} catch (RemoteException e) {
					Logger.log(e);
				}
			}
		});
	}
	
	List<MultiChatRoom> getJoinedChatRooms() {
		XMPPService service = getService();
		Iterator<String> joinedRooms = MultiUserChat.getJoinedRooms(service.getConnection(), service.getProtocolUid());
		List<MultiChatRoom> multiChatBuddies = new ArrayList<MultiChatRoom>();
		for (; joinedRooms.hasNext();) {
			String roomJid = joinedRooms.next();
			try {
				RoomInfo info = MultiUserChat.getRoomInfo(service.getConnection(), roomJid);
				multiChatBuddies.add(XMPPEntityAdapter.chatRoomInfo2Buddy(info, service.getProtocolUid(), service.getServiceId(), true));
			} catch (XMPPException e) {
				Logger.log(e);
			}
		}

		for (final Buddy room : multiChatBuddies) {
			MultiUserChat chat = new MultiUserChat(service.getConnection(), room.getProtocolUid());
			fillWithListeners(chat);
			multichats.put(room.getProtocolUid(), chat);
		}

		return multiChatBuddies;
	}

	@Override
	void onDisconnect() {
		chats.clear();
		multichats.clear();
	}

	public void requestAvailableGroupchats() {
		Executors.defaultThreadFactory().newThread(getGroupchatsRunnable).start();
	}
	
	private void getAvailableChatRooms() throws ProtocolException {
		try {
			Collection<String> mucServices = MultiUserChat.getServiceNames(getService().getConnection());
			if (mucServices.isEmpty()) {
				throw new ProtocolException(ProtocolException.Cause.NO_GROUPCHAT_AVAILABLE);
			}
			List<PersonalInfo> chats = new ArrayList<PersonalInfo>();
			for (String service : mucServices) {
				try {
					chats.addAll(XMPPEntityAdapter.xmppHostedRooms2MultiChatRooms(MultiUserChat.getHostedRooms(getService().getConnection(), service), getService().getProtocolUid(), getService().getServiceId()));
				} catch (XMPPException e) {
					Logger.log(e);
				}
			}
			getService().getProtocolService().getCallback().searchResult(getService().getServiceId(), chats);
		} catch (XMPPException e) {
			Logger.log(e);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	public void leaveChat(final String chatId) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				MultiUserChat muc = multichats.remove(chatId);
				if (muc != null) {
					muc.leave();
					OnlineInfo info = getService().getRosterListener().getPresenceCache().get(chatId);
					info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) -1);
					info.getFeatures().remove(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION);
					info.getFeatures().remove(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_RESULT);
					
					try {
						//getService().getProtocolService().getCallback().buddyAction(ItemAction.LEFT, buddy);
						getService().getProtocolService().getCallback().buddyStateChanged(info);
					} catch (RemoteException e) {
						Logger.log(e);
					}
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}

	public void joinChat(final String host, final String chat, final String nickname, final String password, final boolean createChat) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				try {
					Collection<String> mucServices = MultiUserChat.getServiceNames(getService().getConnection());
					
					MultiUserChat muc = null;
					
					String chatJid = chat + "@" + host;
					String myName = TextUtils.isEmpty(nickname) ? StringUtils.parseName(getService().getProtocolUid()) : nickname;
					
					label:
					for (String service : mucServices) {
						for (HostedRoom room : MultiUserChat.getHostedRooms(getService().getConnection(), service)) {
							if (room.getJid().equals(chatJid)) {
								muc = new MultiUserChat(getService().getConnection(), chatJid);
								continue label;
							}
						}
					}
					
					if (muc == null) {
						muc = new MultiUserChat(getService().getConnection(), chatJid);	
					} else {
						if (createChat) {
							try {
								getService().getProtocolService().getCallback().notification(getService().getServiceId(), getService().getProtocolService().getBaseContext().getString(R.string.chat_exists, chatJid));
							} catch (RemoteException e1) {
								Logger.log(e1);
							}
							return;
						}
					}
					
					if (createChat) {
						newMultiUserChat(muc, nickname, password);
					} else {
						muc.join(myName, password);
					}	
					
					multichats.put(chatJid, muc);	
					
					RoomInfo info = MultiUserChat.getRoomInfo(getService().getConnection(), chatJid);
					MultiChatRoom room = XMPPEntityAdapter.chatRoomInfo2Buddy(info, getService().getProtocolUid(), getService().getServiceId(), true);
					
					try {
						room.getOccupants().addAll(XMPPEntityAdapter.xmppMUCOccupants2mcrOccupants(getService(), muc, true));
						
						String myChatId = chatJid + "/" + myName;
						
						for (Affiliate aff: muc.getOwners()) {
							Logger.log("Owner: "+aff.getJid(), LoggerLevel.VERBOSE);
							if (aff.getJid().equals(myChatId) || aff.getJid().equals(getService().getProtocolUid())) {
								room.getOnlineInfo().getFeatures().putBoolean(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION, true);
								room.getOnlineInfo().getFeatures().putBoolean(XMPPApiConstants.FEATURE_DESTROY_CHAT_ROOM, true);
							}
						}
						for (Affiliate aff: muc.getAdmins()) {
							Logger.log("Admin: "+aff.getJid(), LoggerLevel.VERBOSE);
							if (aff.getJid().equals(myChatId) || aff.getJid().equals(getService().getProtocolUid())) {
								room.getOnlineInfo().getFeatures().putBoolean(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION, true);
								room.getOnlineInfo().getFeatures().putBoolean(XMPPApiConstants.FEATURE_DESTROY_CHAT_ROOM, true);
							}
						}
					} catch (Exception e) {
						Logger.log(e.getLocalizedMessage(), LoggerLevel.DEBUG);
					}
					
					room.setName(TextUtils.isEmpty(info.getDescription()) ? chat : info.getDescription());
					room.getOnlineInfo().setXstatusName(muc.getSubject());
					
					getService().getRosterListener().getPresenceCache().put(chatJid, room.getOnlineInfo());
					
					getService().getProtocolService().getCallback().buddyAction(ItemAction.JOINED, room);
					getService().getProtocolService().getCallback().buddyStateChanged(room.getOnlineInfo());
					
					fillWithListeners(muc);
				} catch (XMPPException e) {
					getService().onXmppException(e);
				} catch (RemoteException e) {
					Logger.log(e);
				} 
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}

	private void newMultiUserChat(MultiUserChat muc, String nickname, String password) throws XMPPException {
		muc.create(nickname != null ? nickname : getService().getOnlineInfo().getProtocolUid());
		
		Form form = muc.getConfigurationForm();
		Form submitForm = form.createAnswerForm();
		for (Iterator<FormField> fields = form.getFields(); fields.hasNext();) {
			FormField field = fields.next();
			Logger.log("Chat "+ muc.getRoom() + " form field "+field.getVariable());
			if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
				submitForm.setDefaultAnswer(field.getVariable());
			}
		}
		try {
			List<String> owners = new ArrayList<String>();
			owners.add(getService().getProtocolUid());
			submitForm.setAnswer("muc#roomconfig_roomowners", owners);
		} catch (Exception e) {
			Logger.log("Could not set XMPP muc room owners for " + muc.getRoom());
		}

		if (password != null) {
			submitForm.setAnswer("muc#roomconfig_roomsecret", password);
			submitForm.setAnswer("muc#roomconfig_publicroom", false);
			submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
		}

		muc.sendConfigurationForm(submitForm);
	}

	public boolean hasGroupchatSupport() throws XMPPException {
		return !MultiUserChat.getServiceNames(getService().getConnection()).isEmpty();
	}

	public void getChatConfigurationForm(final String chatJid) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				MultiUserChat muc = multichats.get(chatJid);
				
				if (muc != null) {
					try {
						Form form = muc.getConfigurationForm();
						InputFormFeature feature = XMPPEntityAdapter.chatRoomConfigurationForm2InputFormFeature(form, getService().getProtocolService());
						getService().getProtocolService().getCallback().showFeatureInputForm(getService().getServiceId(), chatJid, feature);
					} catch (XMPPException e) {
						getService().onXmppException(e);
					} catch (RemoteException e) {
						Logger.log(e);
					}
				} else {
					Logger.log("Could not find a room with jid #" + chatJid, LoggerLevel.INFO);
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}

	public void chatRoomConfiguration(final String chatJid, final Map<String, String> values) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				MultiUserChat muc = multichats.get(chatJid);
				
				if (muc != null) {
					try {
						Form form = muc.getConfigurationForm();						
						Form submitForm = fillMucForm(form, values);
						muc.sendConfigurationForm(submitForm);						
						
						RoomInfo info = MultiUserChat.getRoomInfo(getService().getConnection(), chatJid);
						MultiChatRoom room = XMPPEntityAdapter.chatRoomInfo2Buddy(info, getService().getProtocolUid(), getService().getServiceId(), true);
						
						try {
							room.getOccupants().addAll(XMPPEntityAdapter.xmppMUCOccupants2mcrOccupants(getService(), muc, true));
						} catch (Exception e) {
							Logger.log(e.getLocalizedMessage(), LoggerLevel.DEBUG);
						}
						
						room.setName(TextUtils.isEmpty(info.getDescription()) ? StringUtils.parseName(chatJid) : info.getDescription());
						room.getOnlineInfo().setXstatusName(info.getSubject());
						
						room.getOnlineInfo().getFeatures().putBoolean(XMPPApiConstants.FEATURE_CONFIGURE_CHAT_ROOM_ACTION, true);
						
						getService().getProtocolService().getCallback().buddyAction(ItemAction.JOINED, room);
						getService().getProtocolService().getCallback().buddyStateChanged(room.getOnlineInfo());
					} catch (XMPPException e) {
						getService().onXmppException(e);
					} catch (RemoteException e) {
						Logger.log(e);
					}
				} else {
					Logger.log("Could not find a room with jid #" + chatJid, LoggerLevel.INFO);
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}

	private Form fillMucForm(Form form, Map<String, String> values) {
		Form submitForm = form.createAnswerForm();
		for (Iterator<FormField> i = submitForm.getFields(); i.hasNext();) {
			FormField f = i.next();
			
			String value = values.get(f.getLabel());
			
			if (!TextUtils.isEmpty(value)) {
				
				String type = f.getType();
				
				if (type.equalsIgnoreCase(FormField.TYPE_TEXT_SINGLE) || type.equalsIgnoreCase(FormField.TYPE_JID_SINGLE) || type.equalsIgnoreCase(FormField.TYPE_TEXT_PRIVATE) || type.equalsIgnoreCase(FormField.TYPE_TEXT_MULTI)) {
					submitForm.setAnswer(f.getVariable(), value);			
				} else if (type.equalsIgnoreCase(FormField.TYPE_JID_MULTI) || type.equalsIgnoreCase(FormField.TYPE_LIST_MULTI) || type.equalsIgnoreCase(FormField.TYPE_LIST_SINGLE)) {
					submitForm.setAnswer(f.getVariable(), Arrays.asList(value));	
				} else if (type.equalsIgnoreCase(FormField.TYPE_BOOLEAN)) {
					submitForm.setAnswer(f.getVariable(), Boolean.parseBoolean(value));
				} else {
					submitForm.setAnswer(f.getVariable(), value);
				}
			} else {
				submitForm.setDefaultAnswer(f.getVariable());
			}
		}
		
		return submitForm;
	}

	public void destroyChatRoom(final String chatJid) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				MultiUserChat muc = multichats.get(chatJid);
				
				if (muc != null) {
					try {
						Buddy b = XMPPEntityAdapter.chatInfo2Buddy(chatJid, chatJid, getService().getProtocolUid(), getService().getServiceId(), false);
						muc.destroy(null, null);
						getService().getProtocolService().getCallback().buddyAction(ItemAction.DELETED, b);
					} catch (XMPPException e) {
						getService().onXmppException(e);
					} catch (RemoteException e) {
						getService().onRemoteException(e);
					}
				} else {
					Logger.log("Could not find a room with jid #" + chatJid, LoggerLevel.INFO);
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}
}
