package aceim.app.service;

import aceim.app.dataentity.Account;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.Buddy;
import aceim.app.dataentity.ProtocolResources;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.InputFormFeature;

interface IUserInterface {
	void onProtocolUpdated(in ProtocolResources resources, in ItemAction action);
	void onConnectionStateChanged(byte serviceId, in ConnectionState connState, int extraParameter);
	void onAccountStateChanged(in OnlineInfo info);
	void onContactListUpdated(in Account account);
	void onBuddyStateChanged(in Buddy buddy);
	
	void onMessage(in Message message);
	void onMessageAck(byte serviceId, long messageId, String senderUid, in MessageAckState ackState);
	
	void onAccountIcon(byte serviceId);
	void onBuddyIcon(byte serviceId, String buddyProtocolUid);
	
	void onFileProgress(in FileProgress progress);
	void onSearchResult(byte serviceId, in List<PersonalInfo> infoList);
	void onPersonalInfo(in PersonalInfo info);
	void showFeatureInputForm(byte serviceId, String uid, in InputFormFeature feature);
}