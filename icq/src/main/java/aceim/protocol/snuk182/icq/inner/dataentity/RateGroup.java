package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class RateGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7485307891970096169L;

	public short id = 0;
	public int[] familySubtypePairs = null;
	
	public RateGroup(short id, int[] familySubtypePairs) {
		super();
		this.id = id;
		this.familySubtypePairs = familySubtypePairs;
	}	
}
