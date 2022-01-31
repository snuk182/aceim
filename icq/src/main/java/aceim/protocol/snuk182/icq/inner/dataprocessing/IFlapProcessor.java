package aceim.protocol.snuk182.icq.inner.dataprocessing;

import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;

public interface IFlapProcessor {
	public void process(Flap flap) throws ICQException;
	public void init(ICQServiceInternal icqServiceInternal) throws ICQException;
	
	public void onDisconnect();	
}
