package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class ICBMParams implements Serializable {

	private static final long serialVersionUID = -604108296710525346L;
	
	public short channel;
	public int flags;
	public int maxMessageSnacLength;
	public int maxSenderWarningLevel;
	public int maxReceiverWarningLevel;
	public int minimumMessageInterval;
	public short smth;
	
	@Override
	public String toString(){
		return "		ICBM params: channell "+channel+"/flags "+flags+"/max snac length "+maxMessageSnacLength+"/max sender warning "+maxSenderWarningLevel+"/max receiver warning "+maxReceiverWarningLevel+"/min message interval "+minimumMessageInterval+"/other "+smth;
	}
}
