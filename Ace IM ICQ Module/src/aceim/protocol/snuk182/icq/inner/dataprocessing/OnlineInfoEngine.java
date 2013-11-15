package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQDCInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQIconData;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class OnlineInfoEngine {
	
	private ICQServiceInternal service;
	
	public OnlineInfoEngine(ICQServiceInternal icqServiceInternal){
		this.service = icqServiceInternal;
	}
	
	public List<ICQOnlineInfo> parseOnlineInfo(Snac snac, ICQOnlineInfo info) throws ICQException{
		service.log("	online info:");
		if (snac.plainData == null || snac.plainData.length<1) {
			return null;
		}
		List<ICQOnlineInfo> infos = new ArrayList<ICQOnlineInfo>();
		
		int pos =0;
		
		/*if (snac.plainData[pos]!=service.getUn().length()){
			pos+=8;
		}*/
		
		while (pos<snac.plainData.length){
			ICQOnlineInfo nfo;
			if (info != null && pos==0){
				nfo = info;
			} else {
				nfo = new ICQOnlineInfo();
			}
			
			if (snac.plainData[pos] == 0){
				pos+=ProtocolUtils.bytes2ShortBE(snac.plainData, pos)+2;
			}
			
			int ln = snac.plainData[pos];
			pos++;
			
			String uin;
			try {
				uin = new String(snac.plainData, pos, ln, "ASCII");
			} catch (UnsupportedEncodingException e) {
				uin = new String(snac.plainData, pos, ln);
			}
			
			nfo.uin = uin;
			service.log("		uin: "+uin);
			pos+= ln;
			
			short warnLevel = ProtocolUtils.bytes2ShortBE(snac.plainData, pos);
			nfo.warnLevel = warnLevel;
			service.log("		warn level: "+warnLevel);
			pos+=2;
			
			int tlvCount = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(snac.plainData, pos));
			pos+=2;
			
			for (int i=0; i<tlvCount; i++){
				
				try {
					TLV tlv = new TLV();
					tlv.type = (snac.plainData[pos]<<8)+snac.plainData[++pos];
					int length = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(snac.plainData, ++pos));
					++pos;
					//service.log("		tlv length="+length);
					if (length>0 && snac.plainData.length>4){
						byte[] tailData = new byte[length];
						System.arraycopy(snac.plainData, ++pos, tailData, 0, length);
						tlv.value = tailData;
						pos+=length;
					} else {
						++pos;
					}
					onlineInfoTLVMap(tlv, nfo);
				} catch (Exception e) {
					service.log(e);
					throw new ICQException(e.getMessage());			
				}
			}
			
			if (!uin.equals(service.getUn())){
				nfo.visibility = service.getBuddyList().denyList.get(uin) !=null ? ICQConstants.VIS_DENIED : (service.getBuddyList().permitList.get(uin) !=null ? ICQConstants.VIS_PERMITTED : ICQConstants.VIS_REGULAR);
				/*if (nfo.userStatus != ICQConstants.STATUS_OFFLINE){
					service.getBuddyList().removeFromNotAuthListByUin(uin);
				}*/
				
				for (ICQBuddy buddy : service.getBuddyList().notAuthList) {
					if (buddy.uin.equals(uin)) {
						nfo.visibility = ICQConstants.VIS_NOT_AUTHORIZED;
						break;
					}
				}
			}
			
			infos.add(nfo);
		}
		
		return infos;
		
	}
	
	ICQOnlineInfo onlineInfoTLVMap(TLV tlv, ICQOnlineInfo info) throws ICQException{
		if (tlv == null){
			return info;
		}
		
		ICQOnlineInfo internalInfo;
		if (info == null){
			internalInfo = new ICQOnlineInfo();
		} else {
			internalInfo = info;
		}
		
		switch (tlv.type){
		case ICQConstants.TLV_ONLINE_USERCLASS:
			internalInfo.userClass = ProtocolUtils.bytes2ShortBE(tlv.value);
			service.log("    user class: "+internalInfo.userClass);				
			break;
		case ICQConstants.TLV_ONLINE_ACCOUNTCREATIONTIME:
			internalInfo.memberSinceTime = ProtocolUtils.bytes2Date(tlv.value);
			service.log("    member since: "+internalInfo.memberSinceTime);
			break;
		case ICQConstants.TLV_ONLINE_CREATETIME:
			internalInfo.createTime = ProtocolUtils.bytes2Date(tlv.value);
			service.log("    create time: "+internalInfo.createTime);
			break;
		case ICQConstants.TLV_ONLINE_IDLETIME:
			internalInfo.idleTime = ProtocolUtils.bytes2IntBE(tlv.value);
			service.log("    idle: "+internalInfo.idleTime);
			break;
		case ICQConstants.TLV_ONLINE_SIGNONTIME:
			internalInfo.signonTime = ProtocolUtils.bytes2Date(tlv.value);
			service.log("    signon: "+internalInfo.signonTime);
			break;
		case ICQConstants.TLV_ONLINE_USERSTATUS:
			internalInfo.userStatus = ProtocolUtils.bytes2IntBE(tlv.value);
			service.log("    status: "+ProtocolUtils.getSpacedHexString(tlv.value));
			break;
		case ICQConstants.TLV_ONLINE_OWNNAME:
			internalInfo.name = ProtocolUtils.getEncodedString(tlv.value);
			service.log("    name: "+internalInfo.name);
			//service.getServiceResponse().respond(IProtocolServiceResponse.RES_OWNINFO, service.getServiceId(), args);
			break;
		case ICQConstants.TLV_ONLINE_DCINFO:
			try {
				internalInfo.dcInfo = parseDCInfo(tlv.value);
				service.log("    dc info: "+internalInfo.dcInfo.internalIP);
			} catch (ICQException e) {
				//to do
			}			
			break;
		case ICQConstants.TLV_ONLINE_CAPABILITIES:
			internalInfo.capabilities = parseCapabilities(tlv.value);
			service.log("    capabilities: "+internalInfo.capabilities.size());
			break;
		case ICQConstants.TLV_ONLINE_EXTERNALIP:
			try {
					internalInfo.extIP = ProtocolUtils.unsignedByte2Short(tlv.value[0])+"."+ProtocolUtils.unsignedByte2Short(tlv.value[1])+"."+ProtocolUtils.unsignedByte2Short(tlv.value[2])+"."+ProtocolUtils.unsignedByte2Short(tlv.value[3]);
				} catch (Exception e) {
					internalInfo.extIP = "";
				}
				service.log("    external ip: "+internalInfo.extIP);
			break;
		case ICQConstants.TLV_ONLINE_ONLINETIME:
			internalInfo.onlineTime = ProtocolUtils.bytes2IntBE(tlv.value);
			service.log("    online since: "+internalInfo.onlineTime);
			break;
		case ICQConstants.TLV_DISTRIBUTIONNUMBER:
			internalInfo.distribNumber = tlv.value[0];
			service.log("    distribution: "+internalInfo.distribNumber);
			break;
		case ICQConstants.TLV_ONLINE_ICONDATA:
			ICQIconData[] data = parseIconData(tlv.value);
			if (data !=null){
				for (ICQIconData item:data){
					if (item != null){
						switch(item.iconId){
						case 0x1:
							if (internalInfo.iconData != null && internalInfo.iconData.ssiItemId != 0) {
								item.ssiItemId = internalInfo.iconData.ssiItemId;
							}
							internalInfo.iconData = item;
							service.log("    icon id "+ProtocolUtils.getHexString(item.hash) + " flags "+item.flags);
							break;
						case 0x2:
							if (item.hash.length>3 && internalInfo.uin!=null && !internalInfo.uin.equals(service.getUn())){
								int textLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(item.hash, 0));
								try {
									internalInfo.personalText = new String(item.hash, 2, textLength, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									internalInfo.personalText = new String(item.hash, 2, textLength);
								}
							}
							service.log("    extra text "+internalInfo.personalText);
							break;
						case 0xd:
							/*if (internalInfo.iconData==null){
								internalInfo.iconData = item;
							}*/
							service.log("    HEX "+ProtocolUtils.getHexString(item.hash));
							break;
						}
					}
				}
			}
			break;
		default:
			internalInfo.tlvs.add(tlv);
			service.log("    unknown tlv: "+ProtocolUtils.getSpacedHexString(ProtocolUtils.int2ByteBE(tlv.type)));
			break;	
		}
		return internalInfo;
	}
	
	ICQDCInfo parseDCInfo(byte[] in) throws ICQException{
		if (in.length<37){
			throw new ICQException("Error - not a ICQDCInfo");
		}
		ICQDCInfo info = new ICQDCInfo();		
		byte[] buffer = new byte[4];
		
		System.arraycopy(in, 0, buffer, 0, 4);		
		info.internalIP = ProtocolUtils.getSpacedHexString(buffer).replace(' ', '.');
		
		System.arraycopy(in, 4, buffer, 0, 4);
		info.tcpPort = ProtocolUtils.bytes2IntBE(buffer)+"";
		
		info.type = in[8];
		
		System.arraycopy(in, 9, buffer, 0, 2);
		info.protocolVersion = ProtocolUtils.bytes2ShortBE(buffer);
		
		System.arraycopy(in, 11, buffer, 0, 4);
		info.authCookie = ProtocolUtils.bytes2ShortBE(buffer);
		
		System.arraycopy(in, 15, buffer, 0, 4);
		info.webFrontPort = ProtocolUtils.getSpacedHexString(buffer).replace(' ', '.');
		
		System.arraycopy(in, 23, buffer, 0, 4);
		info.lastInfoUpdateTime = ProtocolUtils.bytes2Date(buffer);
		
		System.arraycopy(in, 27, buffer, 0, 4);
		info.lastExtInfoUpdateTime = ProtocolUtils.bytes2Date(buffer);
		
		System.arraycopy(in, 31, buffer, 0, 4);
		info.lastExtStatusUpdateTime = ProtocolUtils.bytes2Date(buffer);
		
		System.arraycopy(in, 35, buffer, 0, 2);
		info.smth = ProtocolUtils.bytes2ShortBE(buffer);
		
		return info;
	}
	
	ICQIconData[] parseIconData(byte[] value){
		if (value==null) return null;
		
		List<ICQIconData> dataList = new ArrayList<ICQIconData>();
		
		int pos = 0;
		
		while(pos<value.length){
			ICQIconData iconData = new ICQIconData();
			short iconId = ProtocolUtils.bytes2ShortBE(value, pos);
			pos +=2;
			byte flags = value[pos];
			pos++;
			byte[] hash = new byte[ProtocolUtils.unsignedByte2Short(value[pos])];
			pos++;
			System.arraycopy(value, pos, hash, 0, hash.length);
			pos+=hash.length;
			iconData.iconId = iconId;
			iconData.flags = flags;
			iconData.hash = hash;
			
			dataList.add(iconData);
		}
		
		ICQIconData[] array = new ICQIconData[dataList.size()];
		for (int i=0; i<dataList.size(); i++){
			array[i] = dataList.get(i);
		}
		return array;
	}

	List<String> parseCapabilities(byte[] in){
		List<String> caps = new ArrayList<String>();
		if (in == null){
			return caps;
		}
		int pos = 0;
		while (pos<in.length){
			byte[] cap = new byte[16];
			System.arraycopy(in, pos, cap, 0, 16);
			String strCap = ProtocolUtils.getHexString(cap);
			//service.log("cap "+strCap);
			caps.add(strCap);
			pos+=16;
		}
		return caps;
	}
}
