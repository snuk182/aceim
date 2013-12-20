package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.ServiceRedirect;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class MainProcessor extends AbstractFlapProcessor {
	
	@Override
	public void init(ICQServiceInternal service) throws ICQException{
		super.init(service);
		service.log("---------------main thread---------------");
		goOnline();
	}

	private void goOnline() throws ICQException{
		Flap[] flaps = new Flap[]{
				sendOnlineStatusAndDCInfo(), 
				sendOnlineReady(), 
				/*service.getMessagingEngine().getOfflineMessagesRequestFlap(), */
				requestOfflineMessages()};
		service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 9);
		service.getRunnableService().sendMultipleToSocket(flaps);
		service.getPersonalInfoEngine().getShortPersonalMetainfo(service.getUn());
		if (service.getCurrentState() != ICQServiceInternal.STATE_CONNECTED){
			service.setCurrentState(ICQServiceInternal.STATE_CONNECTED);
			service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTED);
		}
		
		ICQOnlineInfo onlineInfo = service.getOnlineInfo();
		if (onlineInfo.extendedStatusId > -1) {
			sendXStatusChange(onlineInfo.qipStatus, onlineInfo.extendedStatusId, onlineInfo.personalText, onlineInfo.extendedStatus);
		}
	}

	private Flap requestOfflineMessages() throws ICQException{
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_ICQEXTENSION;
		data.subtypeId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		data.requestId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		
		TLV tlv = new TLV();
		tlv.type = ICQConstants.TLV_ICQEXTENSION_METADATA;
		byte[] tlvData = new byte[10];
		tlvData[0] = 0x8;
		tlvData[1] = 0;
		byte[] uinIntBytes = ProtocolUtils.int2ByteLE(Integer.parseInt(service.getUn()));
		System.arraycopy(uinIntBytes, 0, tlvData, 2, 4);
		System.arraycopy(ProtocolUtils.short2ByteLE(ICQConstants.ICQEXTENSION_COMMAND_GETOFFLINEMESSAGES), 0, tlvData, 6, 2);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0x0), 0, tlvData, 8, 2);
		tlv.value = tlvData;
		
		data.data = new TLV[]{tlv};
		flap.data = data;
		return flap;
	}

	@Override
	protected void internalFlapMap(Flap flap)throws ICQException{
		if (flap == null) return;
		switch (flap.channel){
		case ICQConstants.FLAP_CHANNELL_START:
			break;
		case ICQConstants.FLAP_CHANNELL_DATA:
			internalSnacMap(flap.data);
			break;
		case ICQConstants.FLAP_CHANNELL_CLOSE:
			if (flap.tlvData!=null){
				for (int i=0; i<flap.tlvData.length; i++){
					TLV tlv = flap.tlvData[i];
					internalTLVMap(tlv);					
				}
			}
			service.lastConnectionError = "multiple login";
			service.getRunnableService().disconnect();
			break;
		}
	}

	@Override
	protected void internalTLVMap(TLV tlv) throws ICQException{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void internalSnacMap(Snac snac)throws ICQException{
		if (snac == null) return;
		
		switch(snac.getServiceId()){
		case ICQConstants.SNAC_FAMILY_GENERIC:
			switch(snac.subtypeId){
			case ICQConstants.SNAC_GENERIC_REDIRECT:
				service.log("redirect got");
				parseRedirectInfo(snac);
				break;
			case ICQConstants.SNAC_GENERIC_RATELIMITWARNING:
				service.log("rate limit warning!");
				break;
			case ICQConstants.SNAC_GENERIC_SERVERPAUSE:
				service.log("server pause!");
				break;
			case ICQConstants.SNAC_GENERIC_OWNINFORES:
				service.setOnlineInfo(service.getOnlineInfoEngine().parseOnlineInfo(snac, service.getOnlineInfo()).get(0));
				service.getServiceResponse().respond(ICQServiceResponse.RES_ACCOUNTUPDATED, service.getOnlineInfo());
			case ICQConstants.SNAC_GENERIC_EXTSTATUSRES:
				parseExternalStatus(snac);
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_LOCATION:
			switch(snac.subtypeId){
			case ICQConstants.SNAC_LOCATION_PARAMRES:
				try {
					snac.data = service.getDataParser().parseTLV(snac.plainData);
				} catch (ICQException e) {
					service.log(e);
				}
				break;
			case ICQConstants.SNAC_LOCATION_ERROR:
			case ICQConstants.SNAC_LOCATION_XZRES:
				service.getServiceResponse().respond(ICQServiceResponse.RES_KEEPALIVE);
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_BUDDYLISTMGMT:
			switch(snac.subtypeId){
			case ICQConstants.SNAC_BUDDYLISTMGMT_PARAMRES:
				try {
					snac.data = service.getDataParser().parseTLV(snac.plainData);
				} catch (ICQException e) {
					service.log(e);
				}
				break;
			case ICQConstants.SNAC_BUDDYLISTMGMT_USERONLINE:
			case ICQConstants.SNAC_BUDDYLISTMGMT_USEROFFLINE:
				parseBuddyInfo(snac);
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_MESSAGING:
			switch(snac.subtypeId){
			case ICQConstants.SNAC_MESSAGING_PARAMRES:
				break;
			case ICQConstants.SNAC_MESSAGING_MSGSENTTHROUGHSERVER:
				service.getMessagingEngine().parseMessage(snac);
				break;
			case ICQConstants.SNAC_MESSAGING_PLUGINMSG:
				service.getMessagingEngine().parsePluginMessage(snac, false);
				break;
			case ICQConstants.SNAC_MESSAGING_MSGSENT:
				service.getMessagingEngine().parsePluginMessage(snac, true);
				break;
			case ICQConstants.SNAC_MESSAGING_TYPINGNOTIFICATION:
				service.getMessagingEngine().parseTyping(snac);
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_SERVERSIDEINFO:
			switch(snac.subtypeId){
			case ICQConstants.SNAC_SERVERSIDEINFO_CLRES:
				parseBuddyList(snac.plainData, snac.lFlag==1);
				break;
			case ICQConstants.SNAC_SERVERSIDEINFO_SSIEDITRES:
				service.getSSIEngine().parseSSIResponse(snac.plainData);
				break;
			case ICQConstants.SNAC_SERVERSIDEINFO_AUTHRES:
				service.getSSIEngine().parseAuthResponse(snac.plainData);
				break;
			case ICQConstants.SNAC_SERVERSIDEINFO_AUTHREQ:
				service.getSSIEngine().parseAuthRequest(snac.plainData);
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_ICQEXTENSION:
			switch(snac.subtypeId){
			case ICQConstants.SNAC_ICQEXTENSION_METAINFORES:
				byte[] data = snac.plainData;
				try {
					TLV[] tlvs = service.getDataParser().parseTLV(data);
					for (TLV tlv:tlvs){
						switch(tlv.type){
						case ICQConstants.TLV_ICQEXTENSION_METADATA:
							parseICQExtensionMetaInfo(tlv.value, snac.lFlag < 1);
							break;
						}
					}
				} catch (ICQException e) {
					service.log(e);
				}
				break;
			}
			break;
		}
	}
	
	
	
	private void parseRedirectInfo(Snac snac) throws ICQException{
		ServiceRedirect redirect = null;
		byte[] tlvData = snac.plainData;
		
		try {
			redirect = new ServiceRedirect(service.getDataParser().parseTLV(tlvData));
		} catch (ICQException e) {
			service.log(e);
		}
		
		if (redirect == null) return;
		
		switch(redirect.family){
		case ICQConstants.SNAC_FAMILY_SERVERSTOREDBUDDYICON:
			service.getBuddyIconEngine().serviceServerURLResponse(redirect);
			break;
		}		
	}	

	private void parseBuddyInfo(Snac snac) throws ICQException{
		List<ICQOnlineInfo>  infos = service.getOnlineInfoEngine().parseOnlineInfo(snac, null);
		if (infos == null) return;
		for(ICQOnlineInfo info:infos){
			for (int i=service.getBuddyList().buddyInfos.size()-1; i>-1; i--){
				ICQOnlineInfo nfo = service.getBuddyList().buddyInfos.get(i);
				
				if (nfo.uin.equals(info.uin)){
					service.getBuddyList().buddyInfos.remove(i);
				}
			}
			
			if (snac.getServiceId()==ICQConstants.SNAC_FAMILY_BUDDYLISTMGMT
					&& snac.subtypeId==ICQConstants.SNAC_BUDDYLISTMGMT_USEROFFLINE){
				info.userStatus = ICQConstants.STATUS_OFFLINE;
			}
			
			if (snac.getServiceId()==ICQConstants.SNAC_FAMILY_BUDDYLISTMGMT
					&& snac.subtypeId==ICQConstants.SNAC_BUDDYLISTMGMT_USERONLINE && info.userStatus == ICQConstants.STATUS_OFFLINE){
				info.userStatus = ICQConstants.STATUS_ONLINE;
			}
			
			if (snac.getServiceId()==ICQConstants.SNAC_FAMILY_BUDDYLISTMGMT){
				service.getBuddyList().buddyInfos.add(info);
			}
			
			service.getServiceResponse().respond(ICQServiceResponse.RES_BUDDYSTATECHANGED, info);		
		}
	}	

	private void parseICQExtensionMetaInfo(byte[] data, boolean last) throws ICQException{
		//2 bytes - length-2 - useless
		long receiverUinInt = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntLE(data, 2));
		String receiverUin = new String(receiverUinInt+"");
		
		if (!receiverUin.equalsIgnoreCase(service.getUn())){
			// lol:)
			service.log("   hohoho, message sent to "+receiverUin);
		}
		
		short metaInfoType = ProtocolUtils.bytes2ShortLE(data, 6);
		@SuppressWarnings("unused")
		short seqNumber = ProtocolUtils.bytes2ShortLE(data, 8);
		switch(metaInfoType){
		case 0x41:
			service.log("       got offline message");
			byte[] tailData = new byte[data.length-10];
			System.arraycopy(data, 10, tailData, 0, tailData.length);
			service.getMessagingEngine().parseOfflineMessage(tailData);
			break;
		case 0x42:
			service.log("       no more offline messages");
			service.getRunnableService().sendToSocket(requestDeleteOfflineMessagesFromServer());
			break;
		case 0x7da:
			tailData = new byte[data.length-10];
			System.arraycopy(data, 10, tailData, 0, tailData.length);
			String uin = service.getPersonalInfoEngine().metaInfoRequestMap.remove(new Byte(data[9]));
			service.getPersonalInfoEngine().parsePersonalInfoResponse(uin, tailData, last);
			break;
		}
	}

	private Flap requestDeleteOfflineMessagesFromServer() throws ICQException{
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_ICQEXTENSION;
		data.subtypeId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		data.requestId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		
		TLV tlv = new TLV();
		tlv.type = ICQConstants.TLV_ICQEXTENSION_METADATA;
		byte[] tlvData = new byte[10];
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 8), 0, tlvData, 0, 2);
		System.arraycopy(ProtocolUtils.int2ByteLE(Integer.parseInt(service.getUn())), 0, tlvData, 2, 4);
		System.arraycopy(ProtocolUtils.short2ByteLE(ICQConstants.ICQEXTENSION_COMMAND_DELETEOFFLINEMESSAGES), 0, tlvData, 6, 2);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, tlvData, 8, 2);
		tlv.value = tlvData;
		
		data.data = new TLV[]{tlv};
		flap.data = data;
		
		return flap;
	}

	/*public void sendKeepalive(){
		if (keepaliveTimer.isAlive()){
			//keepaliveTimer.reset = true;
		}else {
			keepaliveTimer.start();
		}
	}*/

	private Flap sendOnlineReady() throws ICQException{
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_GENERIC;
		data.subtypeId = ICQConstants.SNAC_GENERIC_CLIENTONLINE;
		data.requestId = ICQConstants.SNAC_GENERIC_CLIENTONLINE;
		
		byte[] families = new byte[ICQConstants.LOCAL_FAMILIES_VERSIONS.length*2];
		for (int i=0; i<ICQConstants.LOCAL_FAMILIES_VERSIONS.length/4; i++){
			System.arraycopy(ICQConstants.LOCAL_FAMILIES_VERSIONS, i*4, families, i*8, 4);
			System.arraycopy(ProtocolUtils.short2ByteBE(ICQConstants.FAMILY_TOOL_ID), 0, families, (i*8)+4, 2);
			System.arraycopy(ProtocolUtils.short2ByteBE(ICQConstants.FAMILY_TOOL_VERSION), 0, families, (i*8)+6, 2);
		}
		data.plainData = families;
		
		flap.data = data;
		
		return flap;
	}

	private Flap sendOnlineStatusAndDCInfo() throws ICQException{
		
		TLV dcinfo = new TLV();
		dcinfo.type = 0xc;
		byte[] rawInfo = new byte[37];
		byte[] buffer;
		try {
			buffer = InetAddress.getLocalHost().getAddress();
		} catch (UnknownHostException e) {
			buffer = new byte[]{0,0,0,0};
		}
		System.arraycopy(buffer, 0, rawInfo, 0, 4);
		buffer = new byte[]{0,0,(byte) 0xab, (byte) 0xcd};
		System.arraycopy(buffer, 0, rawInfo, 4, 4);
		rawInfo[8] = ICQConstants.DC_NORMAL;
		buffer = ProtocolUtils.short2ByteBE(ICQConstants.DC_PROTO_VERSION);
		System.arraycopy(buffer, 0, rawInfo, 9, 2);
		buffer = ProtocolUtils.int2ByteLE((int) new Date().getTime()/1000);
		System.arraycopy(buffer, 0, rawInfo, 11, 4);
		buffer = ProtocolUtils.int2ByteBE(ICQConstants.WEB_FRONT_PORT);
		System.arraycopy(buffer, 0, rawInfo, 15, 4);
		System.arraycopy(new byte[]{0,0,0,3}, 0, rawInfo, 19, 4);
		buffer = ProtocolUtils.int2ByteBE((int) new Date().getTime()/1000);
		System.arraycopy(buffer, 0, rawInfo, 23, 4);
		buffer = ProtocolUtils.int2ByteBE((int) new Date().getTime()/1000);
		System.arraycopy(buffer, 0, rawInfo, 27, 4);
		System.arraycopy(new byte[]{0,0,0,0}, 0, rawInfo, 31, 4);
		System.arraycopy(new byte[]{0,0}, 0, rawInfo, 35, 2);
		dcinfo.value = rawInfo;
		
		if (service.getOnlineInfo().extendedStatusId>-1){
			return preparePlainStatusChange(service.getOnlineInfo().userStatus, new TLV[]{dcinfo, getICQMoodTLV()});			
		} else {
			return preparePlainStatusChange(service.getOnlineInfo().userStatus, new TLV[]{dcinfo});			
		}
	}

	/*class KeepaliveTimer extends Thread{
		
		public volatile boolean running = true;
		
		@Override
		public void run(){
			while(running){
				try {
					Thread.sleep(120000);
					if (running){
							bugogaAttacks();
						}
						//sendKeepalive();
						//checkServerReachable();
					
				} catch (InterruptedException e) {
					service.log(e);
				}				
			}
		}
		
		
	}*/
	
	public void sendStatusChange(int newStatus){
		
		service.getRunnableService().sendMultipleToSocket(new Flap[]{preparePlainStatusChange(newStatus, null)});
	}
	
	private Flap preparePlainStatusChange(int newStatus, TLV[] additionalTLVS){
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_GENERIC;
		data2.subtypeId = ICQConstants.SNAC_GENERIC_STATUSSET;
		data2.requestId = ICQConstants.SNAC_GENERIC_STATUSSET;
		
		if (additionalTLVS!=null && additionalTLVS.length>0){
			TLV[] tlvs = new TLV[additionalTLVS.length+1];
			tlvs[0] = getStatusChangeTLV(newStatus);			
			for (int i=0; i<additionalTLVS.length; i++){
				tlvs[i+1] = additionalTLVS[i];
			}
			data2.data = tlvs;
		} else {
			data2.data = new TLV[]{getStatusChangeTLV(newStatus)};
		}
		
		flap2.data = data2;
		
		return flap2;
	}
	
	private TLV getICQMoodTLV(){
		TLV statusText = new TLV();
		statusText.type = ICQConstants.TLV_ONLINE_ICONDATA;
		
		String fullText = service.getOnlineInfo().personalText+" "+service.getOnlineInfo().extendedStatus;
		if (fullText.length()>250){
			fullText = fullText.substring(0, 250);
		}
		byte[] textBytes;
		try {
			textBytes = fullText.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			textBytes = fullText.getBytes();
		}
		byte[] icqmood = new String("icqmood"+service.getOnlineInfo().extendedStatusId).getBytes();
		byte[] textData = new byte[12+textBytes.length+icqmood.length];
		int i=0;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 2), 0, textData, i, 2);
		i+=2;
		textData[i] = 4;
		i++;
		textData[i] = (byte) (textBytes.length+4);
		i++;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) textBytes.length), 0, textData, i, 2);
		i+=2;
		System.arraycopy(textBytes, 0, textData, i, textBytes.length);
		i+=textBytes.length;
		System.arraycopy(ProtocolUtils.int2ByteBE(0xe), 0, textData, i, 4);
		i+=4;
		System.arraycopy(ProtocolUtils.short2ByteBE((short) icqmood.length), 0, textData, i, 2);
		i+=2;
		System.arraycopy(icqmood, 0, textData, i, icqmood.length);
		i+=icqmood.length;
		statusText.value = textData;
		
		return statusText;
	}

	public void sendXStatusChange(byte[] status, Byte xstatus, String args, String args2){
		Flap flap1 = preparePlainStatusChange(service.getOnlineInfo().userStatus, new TLV[]{getICQMoodTLV()});
		
		Flap flap2 = new Flap();
		flap2.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data2 = new Snac();
		data2.serviceId = ICQConstants.SNAC_FAMILY_LOCATION;
		data2.subtypeId = ICQConstants.SNAC_LOCATION_USERINFOSET;
		data2.requestId = ICQConstants.SNAC_LOCATION_USERINFOSET;
		
		TLV clsids = new TLV();
		clsids.type = 0x5;
		
		byte[] rawData = new byte[(16*10) + (status!=null?16:0) + (xstatus>-1?16:0)];
		int i = 0;
		System.arraycopy(ICQConstants.CLSID_UTF, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_RTF, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_DIRECT, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_SRV_RELAY, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_CLIENTINFOPREFIX, 0, rawData, 64, 16);
		i+=16;
		/*System.arraycopy(new String("Asia 0.5.0.0.001").getBytes(), 0, rawData, i, 16);
		i+=16;*/
		System.arraycopy(ICQConstants.CLSID_TYPING, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_AIM_FILERECEIVE, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_AIM_FILESEND, 0, rawData, i, 16);		
		i+=16;
		System.arraycopy(ICQConstants.CLSID_ASIA, 0, rawData, i, 16);
		i+=16;
		System.arraycopy(ICQConstants.CLSID_XTRAZ, 0, rawData, i, 16);
		i+=16;
		if (xstatus>-1){
			System.arraycopy(ICQConstants.XSTATUS_CLSIDS[xstatus], 0, rawData, i, 16);
			i+=16;
		}
		if (status!=null){
			System.arraycopy(status, 0, rawData, i, 16);
			i+=16;
		}		
		clsids.value = rawData;
		
		data2.data = new TLV[]{clsids};
		flap2.data = data2;
		
		service.getRunnableService().sendMultipleToSocket(new Flap[]{flap1, flap2});
	}
	
	private TLV getStatusChangeTLV(int statusValue){
		TLV status = new TLV();
		status.type = 0x6;
		byte[] rawStatus = new byte[4];
		byte[] dcauth = ProtocolUtils.short2ByteBE((short) ICQConstants.STATUS_DCAUTH);
		byte[] online = ProtocolUtils.short2ByteBE((short) statusValue);
		System.arraycopy(dcauth, 0, rawStatus, 0, 2);
		System.arraycopy(online, 0, rawStatus, 2, 2);
		status.value = rawStatus;
		
		return status;
	}

	@Override
	public void onDisconnect() {}

	public void checkServerConnection() {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_LOCATION;
		data.subtypeId = ICQConstants.SNAC_LOCATION_XZREQ;
		data.requestId = ICQConstants.SNAC_LOCATION_XZREQ;
		
		byte[] uidBytes;
		try {
			uidBytes = service.getUn().getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uidBytes = service.getUn().getBytes();
		}
		
		byte[] allBytes = new byte[uidBytes.length + 1];
		
		allBytes[0] = (byte) uidBytes.length;
		System.arraycopy(uidBytes, 0, allBytes, 1, uidBytes.length);
		data.plainData = allBytes;
		
		flap.data = data;
		
		service.getRunnableService().sendToSocket(flap);
	}
	
	protected void parseExternalStatus(Snac snac){
		byte[] data = snac.plainData;
		
		short action = ProtocolUtils.bytes2ShortBE(data, 0);
		
		if (action == 0 || action == 1) {
			byte flags = data[2];
			
			if ((flags & 0x40) > 0) {
				service.log("Upload request");
				service.getBuddyIconEngine().requestIconUpload(service.getSSIEngine().newIcon);
			} else {
				//we've just deleted old icon
				if (data[3] == 0 && service.getOnlineInfo().iconData != null) {
					service.getOnlineInfo().iconData = null;
					service.getSSIEngine().requestIconUpload(service.getSSIEngine().newIcon);
				}
			}
		}		
	}	
}
