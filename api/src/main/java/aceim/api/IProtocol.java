package aceim.api;

import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Utils;

/**
 * An interface to receive requests from core.
 */
public interface IProtocol {

	/**
	 * Personal info request. See {@link ICoreService#personalInfo(aceim.api.dataentity.PersonalInfo, boolean)} for result callback.
	 * @param uid UID of buddy or account to request info for.
	 * @param shortInfo request only name (+ short description, optionally), is set to true.
	 */
	public void requestFullInfo(java.lang.String uid, boolean shortInfo);
	
	/**
	 * Buddy action (adding, editing or renaming, removing) request. 
	 * Despite of {@link ItemAction#JOINED} and {@link ItemAction#LEFT} actions, cannot be used for multi-user chat specific actions. Use {@link IProtocol#joinChatRoom(String, boolean)} and {@link IProtocol#leaveChatRoom(String)} methods for these cases.
	 * @param action an action to take
	 * @param buddy target buddy
	 */
	public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy buddy);
	
	/**
	 * Group action (adding, editing or renaming, removing) request. 
	 * @param action an action to take
	 * @param group target group
	 */
	public void buddyGroupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup group);
	
	/**
	 * {@link ProtocolServiceFeature} setting request. Predefined features IDs, such as status, extended status, can be found in {@link ApiConstants}
	 * @param featureId ID of the feature to be processed within info container
	 * @param info features container (contains target buddy/account UID as well).
	 */
	public void setFeature(java.lang.String featureId, aceim.api.dataentity.OnlineInfo info);
	
	/**
	 * Disconnection request.
	 */
	public void disconnect();
	
	/**
	 * Connection request.
	 * @param info a info to enter network with (if required by protocol).
	 */
	public void connect(aceim.api.dataentity.OnlineInfo info);
	
	/**
	 * Message sending request. Should be implemented synchronized, because of message ID definition.
	 * @param message a message to send
	 * @return message ID (managed by either protocol server or protocol plugin implementation)
	 */
	public long sendMessage(aceim.api.dataentity.Message message);
	
	/**
	 * Icon request. Asynchronized. See {@link ICoreService#iconBitmap(String, byte[], String)} for callback.
	 * @param ownerUid buddy/account UID to get icon for.
	 */
	public void requestIcon(java.lang.String ownerUid);
	
	/**
	 * Response for message (actually for message types that require response, like {@link ServiceMessage} or {@link FileMessage}).
	 * @param message message to respond
	 * @param accept answer
	 */
	public void messageResponse(aceim.api.dataentity.Message message, boolean accept);
	
	/**
	 * File transfer cancel request.
	 * @param messageId ID of file transfer to cancel
	 */
	public void cancelFileFransfer(long messageId);
	
	/**
	 * Typing notification sending request.
	 * @param ownerUid target buddy's UID to send notification to
	 */
	public void sendTypingNotification(java.lang.String ownerUid);
	
	/**
	 * Join chat room request. Makes sense only with protocols that support multi-user chats.
	 * @param chatId chat room UID to join
	 * @param loadOccupantsIcons should chat occupants' icons be loaded or not (aimed to reduce network usage)
	 */
	public void joinChatRoom(java.lang.String chatId, boolean loadOccupantsIcons);
	
	/**
	 * Leave chat room request. Makes sense only with protocols that support multi-user chats.
	 * @param chatId chat room UID to leave
	 */
	public void leaveChatRoom(java.lang.String chatId);
	
	/**
	 * Upload account icon request. Also {@link Utils#scaleAccountIcon(String, int)} may be used to resize icon.
	 * @param filePath local file system path to the photo that should be uploaded. 
	 */
	public void uploadAccountPhoto(java.lang.String filePath);
	
	/**
	 * Account icon removal request.
	 */
	public void removeAccountPhoto();
	
	//public void getChatRooms();
}
