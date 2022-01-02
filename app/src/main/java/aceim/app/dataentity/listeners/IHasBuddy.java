package aceim.app.dataentity.listeners;

import java.util.List;

import aceim.api.dataentity.Buddy;

public interface IHasBuddy extends IHasEntity {

	public void onBuddyStateChanged(List<Buddy> buddies);
	public Buddy getBuddy();
	public boolean hasThisBuddy(byte serviceId, String protocolUid);
	public void onBuddyIcon(byte serviceId, String protocolUid);
}
