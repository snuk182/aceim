package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class ICQBuddy implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4381611554924297253L;
	
	public String uin;
	public String screenName;
	public int groupId;
	public int itemId;
	public short flagType;
	public byte visibility = ICQConstants.VIS_REGULAR;
	public String email;
	public String phone;
	public String comment;
	public List<TLV> additional = new ArrayList<TLV>();
	public ICQOnlineInfo onlineInfo = new ICQOnlineInfo();
	
	
	public void setAdditional(TLV[] tlvs, List<ICQBuddy> notAuthList) {
		if (tlvs==null) return;
		
		for (int i=0; i<tlvs.length; i++){
			TLV tlv = tlvs[i];
			switch(tlv.type){
			case 0x0066:
				visibility = ICQConstants.VIS_NOT_AUTHORIZED;
				notAuthList.add(this);
				break;
			case 0x0137:
				email = ProtocolUtils.getEncodedString(tlv.value);
				break;
			case 0x013a:
				phone = ProtocolUtils.getEncodedString(tlv.value);
				break;
			case 0x0131:
				screenName = ProtocolUtils.getEncodedString(tlv.value);
				break;
			case 0x013c:
				comment = ProtocolUtils.getEncodedString(tlv.value);
				break;
			/*case 0x013d:
				//alert tunes
				break;
			case 0x013e:
				//alert id
				break;*/
			case 0x0145:
				onlineInfo.memberSinceTime = ProtocolUtils.bytes2Date(tlv.value);
				break;
			default:
				additional.add(tlv);
				//System.out.println("    unknown tlv: "+Utils.getSpacedHexString(Utils.int2ByteBE(tlv.getType())));
			}
		}
	}

	@Override
	public String toString(){
		return "Buddy: "+uin;
	}
}
