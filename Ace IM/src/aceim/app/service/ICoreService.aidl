package aceim.app.service;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.app.dataentity.Account;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.TextMessage;
import aceim.app.service.IUserInterface;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.ProtocolOption;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.dataentity.AccountOptionKeys;
import aceim.app.dataentity.GlobalOptionKeys;

interface ICoreService {

	void saveInstanceState(in Bundle bundle);
	Bundle restoreInstanceState();

	void registerCallback(IUserInterface callback);
	
	List<ProtocolOption> getProtocolOptions(String protocolServiceClassName, byte serviceId);
	
	Account createAccount(String protocolServiceClassName, in List<ProtocolOption> options);
	void deleteAccount(in Account account);
	void editAccount(in Account account, in List<ProtocolOption> options, String protocolServicePackageName);
	
	long sendMessage(in Message message);
	void sendLocation(in Buddy buddy);
	
	List<Account> getAccounts(boolean disabledToo);
	List<ProtocolResources> getAllProtocolResources(boolean getProtocolInfo);
	
	Buddy getBuddy(byte serviceId, String buddyProtocolUid);
	List<Buddy> getBuddies(byte serviceId, in List<String> buddyProtocolUid);
	Account getAccount(byte serviceId);
	
	void connect(byte serviceId);
	void disconnect(byte serviceId);
	void connectAll();
	void disconnectAll();
	
	void notifyUnread(in Message message, in Buddy buddy);
	void resetUnread(in Buddy buddy);
	//void setUIVisible(boolean visible);
	
	void addBuddy(in Buddy buddy);
	void removeBuddy(in Buddy buddy);
	void renameBuddy(in Buddy buddy);
	void moveBuddy(in Buddy buddy);
	
	void addGroup(in BuddyGroup group);
	void removeGroup(in BuddyGroup group);
	void renameGroup(in BuddyGroup group);
	void setGroupCollapsed(byte serviceId, String groupId, boolean collapsed);
	
	void requestBuddyShortInfo(byte serviceId, String uid);
	void requestBuddyFullInfo(byte serviceId, String uid);
	
	void respondMessage(in Message msg, boolean accept);
	void cancelFileTransfer(byte serviceId, long messageId);
	void uploadAccountPhoto(byte serviceId, String filename);
	void removeAccountPhoto(byte serviceId);
	
	List<Message> getLastMessages(in Buddy buddy);
	List<Message> getMessages(in Buddy buddy, int startFrom, int maxMessagesToRead);
	boolean deleteMessagesHistory(in Buddy buddy);
	
	void setFeature(String featureId, in OnlineInfo info);
	void sendTyping(byte serviceId, String buddyUid);
	void editBuddyVisibility(in Buddy buddy);
	void editBuddy(in Buddy buddy);
	void editMyVisibility(byte serviceId, byte visibility);
	
	void requestAvailableChatRooms(byte serviceId);
	
	void createChat(byte serviceId, String chatId, in List<Buddy> invitedBuddies);
	void leaveChat(byte serviceId, String chatId);
	void joinChat(byte serviceId, String chatId);
	
	void importAccounts(String password, in FileProgress progress); 
	void exportAccounts(String password, in FileProgress progress);
	void exit(boolean terminate);
}