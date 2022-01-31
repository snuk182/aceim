package aceim.protocol.snuk182.mrim.inner.dataentity;

import java.io.Serializable;

import aceim.protocol.snuk182.mrim.inner.MrimConstants;


public class MrimOnlineInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4689140462799047731L;

	public String uin;
	public int status = MrimConstants.STATUS_OFFLINE;
	public String xstatusId;
	public String xstatusName;
	public String xstatusText;
	
}
