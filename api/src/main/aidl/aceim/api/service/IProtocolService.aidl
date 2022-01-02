package aceim.api.service;

import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ICoreProtocolCallback;

interface IProtocolService {

	void registerCallback(ICoreProtocolCallback callback);
	void addAccount(byte serviceId, String protocolUid);
	void removeAccount(byte serviceId);
	void shutdown();
	void logToFile(boolean enable);
	
	void requestFullInfo(byte serviceId, String uid, boolean shortInfo);
	void buddyAction(in ItemAction action, in Buddy buddy);
	void buddyGroupAction(in ItemAction action, in BuddyGroup group);
	void setFeature(String featureId, in OnlineInfo info);
	void disconnect(byte serviceId);
	void connect(in OnlineInfo info);
	long sendMessage(in Message message);
	void requestIcon(byte serviceId, String ownerUid);
	void messageResponse(in Message message, boolean accept);
	void cancelFileFransfer(byte serviceId, long messageId);
	void sendTypingNotification(byte serviceId, String ownerUid);
	void joinChatRoom(byte serviceId, String chatId, boolean loadOccupantsIcons);
	void leaveChatRoom(byte serviceId, String chatId);
	void uploadAccountPhoto(byte serviceId, String filePath);
	void removeAccountPhoto(byte serviceId);
	
	ProtocolServiceFeature[] getProtocolFeatures();
	ProtocolOption[] getProtocolOptions();
	String getProtocolName();
}
