package aceim.protocol.snuk182.mrim.inner.dataentity;

import java.io.Serializable;

public class MrimBuddy implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4501558349899921773L;

	public String name;
	public int id = 0;
	
	public String uin;
	public final MrimOnlineInfo onlineInfo = new MrimOnlineInfo();
	public int groupId;
	public int flags = 0;
	public int serverFlags = 0;
	public String clientId;
}
