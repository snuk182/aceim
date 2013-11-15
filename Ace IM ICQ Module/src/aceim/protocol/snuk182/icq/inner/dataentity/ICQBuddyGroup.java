package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class ICQBuddyGroup implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2206757783304146636L;
	
	public String name;
	public int groupId;
	public List<Integer> buddies = new ArrayList<Integer>();
	public List<TLV> additional = new ArrayList<TLV>();
	
	public void setAdditional(TLV[] tlvs) {
		if (tlvs==null) return;
		
		for (int i=0; i<tlvs.length; i++){
			TLV tlv = tlvs[i];
			switch(tlv.type){
			case 0xc8:
				if (tlv.value==null){
					continue;
				}
				int pos = 0;
				while (pos<tlv.value.length){
					buddies.add(new Integer(ProtocolUtils.bytes2ShortBE(tlv.value, pos)));
					pos+=2;
				}
				break;
			default:
				additional.add(tlv);
			}
		}		
	}
}
