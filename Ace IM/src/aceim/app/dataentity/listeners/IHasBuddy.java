package aceim.app.dataentity.listeners;

import aceim.api.dataentity.Buddy;

public interface IHasBuddy extends IHasEntity {

	public void onBuddyStateChanged(Buddy buddy);
	public Buddy getBuddy();
	public boolean hasThisBuddy(byte serviceId, String protocolUid);
	public void onBuddyIcon(byte serviceId, String protocolUid);
}
