package aceim.api;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolService;

/**
 * Protocol-to-Application interface. Protocol implementations use it to send data to main application.
 */
public interface ICoreService {

	/**
	 * A notification for core service about connection state changes.
	 * @param connState new connection state
	 * @param extraParameter use 1-10 range in a case of {@link ConnectionState#CONNECTING} for connection progress, 
	 * error code of correspondent {@link ProtocolException} (if any, -1 otherwise) in a case of {@link ConnectionState#DISCONNECTED},
	 * any integer in all other cases (not used either).
	 */
	void connectionStateChanged(ConnectionState connState, int extraParameter);
	
	/**
	 * Send new icon bitmap to core service.
	 * @param ownerUid a protocol UID of entity, that owns this icon, either {@link AccountService} or {@link Buddy}
	 * @param data bitmap data
	 * @param hash bitmap data hash
	 */
	void iconBitmap(String ownerUid, byte[] data, String hash);
	
	/**
	 * Send new buddy list to core service, which will overwrite existing one. Recommended to be used during connection flow ({@link ConnectionState#CONNECTING}).
	 * @param buddyList new buddylist (groups of buddies, may contain anonymous groups, marked with {@link ApiConstants#NO_GROUP_ID})
	 */
	void buddyListUpdated(List<BuddyGroup> buddyList);
	
	/**
	 * A notification about new message to the core.
	 * @param message a message
	 */
	void message(Message message);
	
	/**
	 * A notification about buddies' online state changing (online status, status text etc). Should be sent only after connection is established ({@link ConnectionState#CONNECTED})
	 * @param infos list of new buddies' online info's. Account's online infos not allowed.
	 */
	void buddyStateChanged(List<OnlineInfo> infos);
	
	/**
	 * An arbitrary text notification, that will be shown as popup in core context.
	 * @param message a message to show.
	 */
	void notification(String message);
	
	/**
	 * A notification about account's state changing (online status, status text etc). Recommended to be sent after connection is established ({@link ConnectionState#CONNECTED}).
	 * @param info new account info
	 */
	void accountStateChanged(OnlineInfo info);
	
	/**
	 * Send requested personal info to the core, for buddy or account.
	 * @param info info to send
	 * @param isShortInfo declares if info contains username only, or the full data.
	 */
	void personalInfo(PersonalInfo info, boolean isShortInfo);
	
	/**
	 * Send result of buddy/multi-user chat search. The search itself may be implemented via {@link IProtocol#setFeature(String, OnlineInfo)}.
	 * @param infoList list of personal infos, either short (uid + name) or full.
	 */
	void searchResult(List<PersonalInfo> infoList);
	
	/**
	 * Send changes of particular buddy group to the core. Group should have valid ID. 
	 * @param action an action applied to group (added/modified (renamed)/deleted)
	 * @param newGroup target group
	 */
	void groupAction(ItemAction action, BuddyGroup newGroup);
	
	/**
	 * Send changes of particular buddy to the core. 
	 * @param action an action applied to group (added/modified (renamed)/deleted)
	 * @param newBuddy target buddy
	 */
	void buddyAction(ItemAction action, Buddy newBuddy);
	
	/**
	 * Notify core about progress of file transfer. See {@link FileProgress} for details.
	 * @param progress the progress.
	 */
	void fileProgress(FileProgress progress);
	
	/**
	 * Send message ACK state to the core. See {@link MessageAckState} for details.
	 * @param ownerUid message's recipient UID
	 * @param messageId ack'ed message's ID
	 * @param state ACK state.
	 */
	void messageAck(String ownerUid, long messageId, MessageAckState state);
	
	/**
	 * Send typing notification to the core.
	 * @param ownerUid UID of buddy, who's typing
	 */
	void typingNotification(String ownerUid);
	
	/**
	 * Request account's preference, used in account editor, from the core. For storing preferences, see {@link ProtocolService#getProtocolFeatures()} and {@link ProtocolOption}.
	 * @param preferenceName preference name (key)
	 * @return value of preference (may be null)
	 */
	String requestPreference(String preferenceName);
	
	/**
	 * Notify core about activity in account. This can be any information, (e.g. your status request, connection status change), which is not necessarily sent to the core for notification.
	 * As of version 0.9.3, core support for account activities is not yet implemented.
	 * @param text activity text
	 */
	void accountActivity(String text);	
	
	/**
	 * Notify core about changes in multi-user chat's participant list (gone online/offline, renaming etc).
	 * @param chatUid owner chat UID
	 * @param participantList new list of participant, will completely replace old list in {@link MultiChatRoom} in core.
	 */
	void multiChatParticipants(String chatUid, List<BuddyGroup> participantList);
	
	/**
	 * Request core and UI to show arbitrary input form
	 * @param uid account/buddy UID, which context the form should be processed with. See also {@link IProtocol#setFeature(String, OnlineInfo)} for the cases of callback.
	 * @param feature
	 */
	void showFeatureInputForm(String uid, InputFormFeature feature);
}
