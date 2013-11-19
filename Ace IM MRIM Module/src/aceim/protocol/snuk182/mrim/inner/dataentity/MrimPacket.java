package aceim.protocol.snuk182.mrim.inner.dataentity;

import java.io.Serializable;

public class MrimPacket implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8289019089194121267L;

	public long seqNumber;
	public int type = -1;
	public byte[] rawData;
}
