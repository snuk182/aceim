package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class ICQIconData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1152804935512145797L;

	public short ssiItemId;
	public short iconId;
	public byte flags;
	public byte[] hash;
	public byte[] jpegData;
}
