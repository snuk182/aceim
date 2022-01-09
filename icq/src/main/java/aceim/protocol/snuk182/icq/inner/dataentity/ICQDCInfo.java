package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;
import java.util.Date;

public class ICQDCInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1932270582735486309L;
	
	public String internalIP;
	public String tcpPort;
	public byte type;
	public int protocolVersion;
	public short authCookie;
	public String webFrontPort;
	public Date lastInfoUpdateTime;
	public Date lastExtInfoUpdateTime;
	public Date lastExtStatusUpdateTime;
	public short smth;
	
	@Override
	public String toString(){
		return "dc info";
	}
}
