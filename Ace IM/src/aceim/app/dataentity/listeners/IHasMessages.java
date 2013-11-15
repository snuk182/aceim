package aceim.app.dataentity.listeners;

import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;

public interface IHasMessages {
	
	public void onMessageReceived(Message message);
	public void onMessageAckReceived(long messageId, MessageAckState ack);
	public boolean hasMessagesOfBuddy(byte serviceId, String buddyProtocolUid);
}
