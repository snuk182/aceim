package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class TLV implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4444375183666574282L;
	
	public int type;
	//private short length;
	public byte[] value;
	
	public TLV(){
		
	}
	
	public TLV(short type, byte[] value){
		this.type = type;
		this.value = value;
	}
}
