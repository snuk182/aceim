package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class Flap implements Serializable {
	
	
	private static final long serialVersionUID = -3159833850745440628L;
	public byte channel;
	public short sequenceNumber;
	public Snac data;
	public TLV[] tlvData;

	public Flap(){}
}
