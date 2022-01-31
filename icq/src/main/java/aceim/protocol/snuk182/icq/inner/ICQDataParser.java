package aceim.protocol.snuk182.icq.inner;

import java.util.ArrayList;
import java.util.List;

import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public final class ICQDataParser {
	
	public List<Flap> parseFlaps(byte[] in) throws ICQException{
		List<Flap> flaps = new ArrayList<Flap>();
		
		if (in == null) {
			return flaps;
		}
		
		if (in.length <6){
			throw new ICQException("Error - broken FLAP");
		}
		
		int pos = 0;
		while (pos<in.length){
			int tailLength = 6+ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(in, pos+4));
			//System.out.println("1 flap length:"+tailLength);
			if (tailLength != 0){
				byte[] tailData = new byte[tailLength];
				System.arraycopy(in, pos+0, tailData, 0, tailLength);
				Flap flap = parseFlap(tailData);
				if (flap!=null){
					flaps.add(flap);
				}
			}
			pos+=tailLength;
		}
		
		return flaps;
	}
	
	public Flap parseFlap(byte[] in) throws ICQException{
		
		if (in == null) {
			throw new ICQException("Error - no FLAP data!");
		}		
		if (in[0] != 0x2a){
			throw new ICQException("Error - not a FLAP:"+in);
		}
		
		Flap flap = new Flap();
		flap.channel = in[1];
		flap.sequenceNumber = (short) (ProtocolUtils.bytes2ShortBE(in, 2));
		int tailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(in, 4));
		//System.out.println("snac length:"+tailLength);
		if (tailLength > 0 && !(in[1]==0x1 && tailLength == 4)){	
			byte[] tailData;
			if (in[1] == 0x01){
				tailData = new byte[tailLength-4];
				System.arraycopy(in, 10, tailData, 0, tailLength-4);				
			} else {
				tailData = new byte[tailLength];
				System.arraycopy(in, 6, tailData, 0, tailLength);				
			}
			
			if (in[1] == 0x02) {
				flap.data = parseSnac(tailData);			
			} else {
				flap.tlvData = parseTLV(tailData);
			}
		}
		return flap;
	}	
	public Snac parseSnac(byte[] in) throws ICQException{
		//System.out.println("      parse snac -> "+ProtocolUtils.getSpacedHexString(in));	
		
		if (in == null){
			throw new ICQException("Error - no SNAC data!");
		}
		if (in.length<10){
			throw new ICQException("Error - SNAC data corrupted"+in);
		}
		
		Snac snac = new Snac();
		snac.serviceId = (short) ((in[0]<<8)+in[1]);
		snac.subtypeId = (short) ((in[2]<<8)+in[3]);
		snac.hFlag = in[4];
		snac.lFlag = in[5];
		snac.requestId = (in[6]<<24)+(in[7]<<16)+(in[8]<<8)+(in[9]);
		if (in.length > 10){
			
			int govno = snac.hFlag&0x80;
			short dataToSkip = 0;
			if (govno>0){  
				dataToSkip = (short) (ProtocolUtils.bytes2ShortBE(in, 10)+2);		
			}		
			
			int tailLength = in.length - 10 - dataToSkip;
			snac.plainData = new byte[tailLength];
			System.arraycopy(in, 10+dataToSkip, snac.plainData, 0, tailLength);			
			//snac.setData(parseTLV(snac.getPlainData()));
		}
		
		/*if (incompleteSnac !=null && incompleteSnac.getServiceId()==snac.getServiceId() && incompleteSnac.getSubtypeId()==incompleteSnac.getSubtypeId()){
			byte[] completeSnac = new byte[incompleteSnac.getPlainData().length+snac.getPlainData().length];
			System.arraycopy(incompleteSnac.getPlainData(), 0, completeSnac, 0, incompleteSnac.getPlainData().length);
			System.arraycopy(snac.getPlainData(), 0, completeSnac, incompleteSnac.getPlainData().length, snac.getPlainData().length);
			snac.setPlainData(completeSnac);
			System.out.println("      full snac -> "+ProtocolUtils.getSpacedHexString(completeSnac));	
			
		}
		
		if (snac.getlFlag()>0){
			incompleteSnac = snac;
		} else {
			incompleteSnac = null;
		}*/
		
		return snac;
	}
	
	public TLV[] parseTLV(byte[] in) throws ICQException{
		return parseTLV(in, -1);
	}
	
	public TLV[] parseTLV(byte[] in, int tlvCount) throws ICQException{
		//System.out.println("       parse tlv -> "+ProtocolUtils.getSpacedHexString(in));		
		
		if (in == null){
			throw new ICQException("Error - no TLV data");
		}
		if (in.length < 1){
			return new TLV[0];
		}
		if (in.length<4){
			throw new ICQException("Error - TLV data corrupted");
		}
		List<TLV> tlvs = new ArrayList<TLV>();
		for (int i=0; i<in.length; i++){
			if (tlvCount>-1 && i==tlvCount){
				break;
			}
			
			TLV tlv = new TLV();
			int type = (in[i]<<8)+(in[++i]&0xff);
			//tlv.setType(Utils.unsignedShort2Int((short) ());
			tlv.type = type;
			//tlv.setLength((short) ((in[++i]<<8)+in[++i]));
			int length = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(in, ++i));
			i++;
			if (length>0 && in.length>4){
				byte[] tailData = new byte[length];
				System.arraycopy(in, ++i, tailData, 0, length);
				tlv.value = tailData;
				i+=length-1;
			}
			//System.out.println("		parsed "+type+"-"+length+"bytes");
			tlvs.add(tlv);			
		}
		
		TLV[] array = new TLV[tlvs.size()];
		for (int i=0;i<tlvs.size();i++){
			array[i] = tlvs.get(i);
		}
		
		return array;//(TLV[]) tlvs.toArray();
	}
	
	public byte[] flaps2Bytes(Flap[] flaps) throws ICQException{
		if (flaps == null){
			throw new ICQException("Error - flaps is null");
		}
		
		if (flaps.length == 1){
			return flap2Bytes(flaps[0]);
		}
		
		int length = 0;
		List<byte[]> bytes = new ArrayList<byte[]>();
		for (int i=0; i<flaps.length; i++){
			if (flaps[i]==null){
				continue;
			}
			byte[] out = flap2Bytes(flaps[i]);
			length += out.length;
			bytes.add(out);
		}
		
		byte[] out = new byte[length];
		int pos = 0;
		for (int i=0; i<bytes.size(); i++){
			System.arraycopy(bytes.get(i), 0, out, pos, bytes.get(i).length);
			pos+=bytes.get(i).length;
		}
		
		return out;
	}
	
	public byte[] flap2Bytes(Flap flap) throws ICQException{
		if (flap == null){
			throw new ICQException("Error - flap is null");
		}
		byte[] snacData = null;
		byte[] tlvData = null;
		if (flap.data != null){
			snacData = snac2Bytes(flap.data);
		}
		if (flap.tlvData != null){
			tlvData = tlvs2Bytes(flap.tlvData);
		}
		short dataLength = (short) ((snacData!=null?snacData.length:0)+(tlvData!=null?tlvData.length:0));
		byte[] out = new byte[1+1+2+2+dataLength];
		out[0] = 0x2a;
		out[1] = flap.channel;
		byte[] buf = ProtocolUtils.short2ByteBE(flap.sequenceNumber);
		System.arraycopy(buf, 0, out, 2, buf.length);
		buf = ProtocolUtils.short2ByteBE(dataLength);
		System.arraycopy(buf, 0, out, 4, buf.length);
		if (snacData!=null){
			System.arraycopy(snacData, 0, out, 6, snacData.length);
		}
		if (tlvData!=null){
			if (snacData==null){
				System.arraycopy(tlvData, 0, out, 6, tlvData.length);
			} else {
				System.arraycopy(snacData, 0, out, 6+snacData.length, snacData.length);
			}
		}
		
		return out;
	}
	public byte[] snac2Bytes(Snac snac) throws ICQException{
		if (snac == null){
			throw new ICQException("Error - snac is null");
		}
		int length = 2+2+2+4;
		byte[] tlvData = null;
		if (snac.data!=null){
			tlvData = tlvs2Bytes(snac.data);
			length += tlvData.length;
		}
		if (snac.plainData!=null){
			length += snac.plainData.length;
		}
		byte[] out = new byte[length];
		byte[] buf = ProtocolUtils.short2ByteBE(snac.getServiceId());
		System.arraycopy(buf, 0, out, 0, buf.length);
		buf = ProtocolUtils.short2ByteBE(snac.subtypeId);
		System.arraycopy(buf, 0, out, 2, buf.length);
		buf = ProtocolUtils.int2ByteBE(snac.requestId);
		out[4] = snac.hFlag;
		out[5] = snac.lFlag;
		System.arraycopy(buf, 0, out, 6, buf.length);
		int pos = 10;
		if (snac.plainData!=null){
			System.arraycopy(snac.plainData, 0, out, pos, snac.plainData.length);
			pos+=snac.plainData.length;
		}
		if (tlvData!=null){
			System.arraycopy(tlvData, 0, out, pos, tlvData.length);
			pos+=tlvData.length;
		}
		return out;
	}
	
	public byte[] tlvs2Bytes(TLV[] tlvs){
		if (tlvs == null){
			return new byte[0];
		}
		List<byte[]> list = new ArrayList<byte[]>(tlvs.length);
		int lengthAll = 0;
		for (int i=0; i<tlvs.length; i++){
			
			if (tlvs[i].value==null){
				tlvs[i].value = new byte[0];
			}
			
			int length = 2+tlvs[i].value.length;	
			if (tlvs[i].type != 0x0){//authrequest
				length += 2;					
			} 
			byte[] bytes = new byte[length];
			byte[] buf = ProtocolUtils.short2ByteBE((short) tlvs[i].type);
			System.arraycopy(buf, 0, bytes, 0, buf.length);
			int dataOffset = 2;
			if (tlvs[i].type != 0x0){//authrequest
				buf = ProtocolUtils.short2ByteBE((short) tlvs[i].value.length);			
				System.arraycopy(buf, 0, bytes, 2, buf.length);
				dataOffset += 2;
			}
			if (tlvs[i].value.length>0){
				System.arraycopy(tlvs[i].value, 0, bytes, dataOffset, tlvs[i].value.length);
			}			
			lengthAll+=length;
			list.add(bytes);
		} 
		byte[] out = new byte[lengthAll];
		int bytepos = 0;
		for (byte[] item:list){
			System.arraycopy(item, 0, out, bytepos, item.length);
			bytepos += item.length;
		}
		return out;
	}
}
