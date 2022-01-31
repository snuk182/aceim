package aceim.protocol.snuk182.mrim.inner.dataentity;

import java.io.Serializable;

public class MrimMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7930939369686195063L;
	
	public String text;
	public String from;
	public String to;
	public int flags = 0;

	public int messageId = 0;

}
