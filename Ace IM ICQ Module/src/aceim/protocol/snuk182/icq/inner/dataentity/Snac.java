package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class Snac implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5655214423701253519L;
	
	public short serviceId;
	public short subtypeId;
	public byte hFlag = 0;
	public byte lFlag = 0;
	public int requestId;
	public TLV[] data;
	public byte[] plainData;
	
	public short getServiceId() {
		return serviceId;
	}
	public Snac(short serviceId, short subtypeId) {
		super();
		this.serviceId = serviceId;
		this.subtypeId = subtypeId;
	}
	
	public Snac(){}
}
