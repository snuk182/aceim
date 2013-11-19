package aceim.protocol.snuk182.mrim.inner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import aceim.protocol.snuk182.mrim.MrimEntityAdapter;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimBosString;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimBuddy;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimGroup;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimMessage;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimOnlineInfo;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimPacket;
import aceim.protocol.snuk182.mrim.utils.Base64;
import aceim.protocol.snuk182.mrim.utils.MD5;
import aceim.protocol.snuk182.mrim.utils.ProtocolUtils;
import android.annotation.SuppressLint;
import android.os.Build;

public final class MrimProcessor {
	
	private Map<Long, MessageData> msgIDs = new HashMap<Long, MessageData>();

	private static final String ALIAS = "Asia IM Android";
	private static final String VERSION_INFO = "client=\"android\" version=\"2.0 (build 211)\" desc=\"Android Agent\"";
	//private static final String VERSION_INFO = "client=\"magent\" version=\"5.8\" build=\"4139\"";
	private static final String LANG = "ru";
	private static final String GEO_LIST = "geo-list";
	private static final String ICON_SERVER = "http://obraz.foto.mail.ru/%s/%s/_mrimavatar";
	
	private MrimServiceInternal service;

	public MrimProcessor(MrimServiceInternal service) {
		this.service = service;
	}

	public MrimPacket parsePacket(byte[] tail) throws MrimException {
		if (tail == null || tail.length < 4) {
			throw new MrimException("No packet data");
		}

		int start4bytes = ProtocolUtils.bytes2IntLE(tail, 0);

		if (start4bytes != MrimConstants.CS_MAGIC) {
			return parseBosString(tail);
		}

		if (tail.length < 44) {
			throw new MrimException("No packet data");
		}

		MrimPacket packet = new MrimPacket();

		// first 8 bytes are useless
		packet.seqNumber = ProtocolUtils.bytes2IntLE(tail, 8);
		packet.type = ProtocolUtils.bytes2IntLE(tail, 12);
		packet.rawData = tail;

		return packet;
	}

	public void parsePacketTail(MrimPacket packet) {
		// TODO remove
		//service.log("packet " + packet.type);

		if (packet.type == MrimConstants.MRIM_CS_HELLO) {
			service.log("server hello");
			reconnectBos((MrimBosString) packet);
			return;
		}

		switch (packet.type) {
		case MrimConstants.MRIM_CS_MPOP_SESSION:
			service.getServiceResponse().respond(MrimServiceResponse.RES_KEEPALIVE);
			break;
		case MrimConstants.MRIM_CS_HELLO_ACK:
			parsePingFreq(packet);
			//proceedLogin();
			try {
				proceedLogin3();
			} catch (IOException e) {
				service.log(e);
			}
			break;
		case MrimConstants.MRIM_CS_LOGIN_ACK:
			service.log("login ok");
			service.getServiceResponse().respond(MrimServiceResponse.RES_CONNECTING, 5);
			break;
		case MrimConstants.MRIM_CS_LOGIN_REJ:
			service.log("login failed");
			loginFailed(packet);
			break;
		case MrimConstants.MRIM_CS_LOGOUT:
			parseLogout(packet);
			break;
		case MrimConstants.MRIM_CS_CONTACT_LIST2:
			service.getServiceResponse().respond(MrimServiceResponse.RES_CONNECTING, 7);
			service.log("contact list");
			parseContacts(packet);
			
			setStatus(service.getOnlineInfo());
			break;
		case MrimConstants.MRIM_CS_MESSAGE_ACK:
			parseMessage(packet);
			break;
		case MrimConstants.MRIM_CS_MESSAGE_STATUS:
			parseMessageAck(packet);
			break;
		case MrimConstants.MRIM_CS_USER_STATUS:
			parseUserStatus(packet);
			break;
		case MrimConstants.MRIM_CS_CONNECTION_PARAMS:
			parsePingFreq(packet);
			break;
		case MrimConstants.MRIM_CS_NEW_EMAIL:
			parseNewEmail(packet);
			break;
		case MrimConstants.MRIM_CS_MAILBOX_STATUS:
			parseMailboxStatus(packet);
			break;
		case MrimConstants.MRIM_CS_OFFLINE_MESSAGE_ACK:
			parseOfflineMessage(packet);
			break;
		case MrimConstants.MRIM_CS_USER_INFO:
			service.log("my own info");
			break;
		case 0x1079:
			service.log("some 1079 received");
			break;
		case MrimConstants.MRIM_CS_FILE_TRANSFER:
			service.getFileTransferEngine().parseFTRequest(packet);
			break;
		case MrimConstants.MRIM_CS_FILE_TRANSFER_ACK:
			service.getFileTransferEngine().parseFTResponse(packet);
			break;
		case MrimConstants.MRIM_CS_PROXY:
			service.getFileTransferEngine().parseFTProxyConnectionRequest(packet);
			break;
		case MrimConstants.MRIM_CS_PROXY_ACK:
			service.getFileTransferEngine().parseFTProxyAck(packet);
			break;
		}
		return;
	}

	private void parseOfflineMessage(MrimPacket packet) {
		int pos = 44;
		
		byte[] idBytes = new byte[4];
		System.arraycopy(packet.rawData, pos, idBytes, 0, 4);
		
		int id = ProtocolUtils.bytes2IntLE(idBytes);
		pos+=4;
		try {
			String email = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
			
			String date = getMailValue(email, "Date");
			String from = getMailValue(email, "From");
			String msg = getMailMessage(email);
			long flags = 0;
			try {
			    flags = Integer.parseInt(getMailValue(email, "X-MRIM-Flags"), 16);
			} catch (Exception e) {
			}
			
			service.log("offline "+from+"/"+date+"/"+msg+"/"+flags);
			
			final MrimMessage message = new MrimMessage();
			message.text = msg+" \r\n"+date;
			message.from = from;
			message.messageId = id;
			
			new Thread(){
				@Override
				public void run(){
					service.getServiceResponse().respond(MrimServiceResponse.RES_MESSAGE, message);
				}
			}.start();
			
			//sendDeleteOfflineMsgRequest(idBytes);
			
			/*if ((service.getOnlineInfo().status & MrimConstants.STATUS_FLAG_INVISIBLE) == 0 && (flags & MrimConstants.MESSAGE_FLAG_NORECV) == 0){
				sendMsgAck(from, id);
			}*/
		} catch (Exception e) {
			service.log(e);
		}
	}
	
	@SuppressWarnings("unused")
	private void sendDeleteOfflineMsgRequest(byte[] idBytes) {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_DELETE_OFFLINE_MESSAGE;
		packet.rawData = idBytes;

		service.getRunnableService().sendToSocket(packet);
	}

	@SuppressLint("DefaultLocale")
	private String getMailValue(String header, String key) {
        int pos = header.toLowerCase().indexOf(key.toLowerCase());
        if (-1 == pos) {
            return "";
        }
        int end = header.indexOf('\n', pos);
        return header.substring(pos + key.length() + 1, end).trim();
    }
    private String getMailMessage(String mail) {
    	String body = "";
        if (-1 != getMailValue(mail, "Content-Type").indexOf("multipart/")) {
            String boundary = getMailValue(mail, "Boundary");
            int start = mail.indexOf("--" + boundary) + 2 + boundary.length();
            int end = mail.indexOf("--" + boundary, start)-1;
            body = getMailBody(mail.substring(start, end));
        }

        if (-1 != getMailValue(mail, "Content-Transfer-Encoding").indexOf("base64")) {
            byte[] data;
			try {
				data = Base64.decode(body);
				body = ProtocolUtils.getEncodedString(data, "UTF-16LE");
			} catch (IOException e) {
				service.log(e);
			}            
        }
        return body;
    }

	private String getMailBody(String string) {
		if (string.indexOf("\n\n")>-1){
			return string.split("\n\n")[1];
		} else {
			return string.split("\r\n\r\n")[1];
		}		
	}

	private void parseMailboxStatus(MrimPacket packet) {
		int pos = 44;
		
		long emailCount = MrimEntityAdapter.ul2Long(packet.rawData, pos);
		pos+=4;
		StringBuilder sb = new StringBuilder();
		sb.append(emailCount);
		sb.append(" emails");
		
		service.getServiceResponse().respond(MrimServiceResponse.RES_NOTIFICATION, sb.toString(), true);
	}

	private void parseNewEmail(MrimPacket packet) {
		int pos = 44;
		
		long emailCount = MrimEntityAdapter.ul2Long(packet.rawData, pos);
		pos+=4;
		String sender = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos+=(4+MrimEntityAdapter.ul2Long(packet.rawData, pos));
		String topic = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		
		StringBuilder sb = new StringBuilder();
		sb.append(emailCount);
		sb.append(" emails\n\n");
		sb.append(sender);
		sb.append("\n");
		sb.append(topic);
		service.getServiceResponse().respond(MrimServiceResponse.RES_NOTIFICATION, sb.toString(), true);
	}

	private void parseLogout(MrimPacket packet) {
		int pos = 44;
		int status = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos+=4;
		if ((status & MrimConstants.LOGOUT_NO_RELOGIN_FLAG)!=0){
			service.lastConnectionError = "multiple login";
		} else {
			service.lastConnectionError = "server disconnected - "+status;
		} 		
		service.getRunnableService().disconnect();
	}

	private void parseUserStatus(MrimPacket packet) {
		int pos = 44;
		int status = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos+=4;
		String statusName = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos+=4+statusName.length();
		
		String xstatusname = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
		pos+=4+(xstatusname.length()*2);
		String xstatustext = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
		pos+=4+(xstatustext.length()*2);
		
		String email = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos+=4+email.length();
		
		MrimOnlineInfo info = new MrimOnlineInfo();
		info.status = status;
		info.uin = email;
		info.xstatusId = statusName;
		info.xstatusName = xstatusname;
		info.xstatusText = xstatustext;
		
		service.log("uid "+email+" status "+status+" / "+statusName+" / "+xstatusname+" / "+xstatustext+" / "+new String(packet.rawData, pos, packet.rawData.length-pos-1));
		
		service.getServiceResponse().respond(MrimServiceResponse.RES_BUDDYSTATECHANGED, info);
	}

	private void parseMessageAck(MrimPacket packet) {
		int pos = 44;
		
		int status = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		
		MessageData data = msgIDs.remove(packet.seqNumber);
		if (data != null){
			if (status == MrimConstants.MESSAGE_DELIVERED){
				service.getServiceResponse().respond(MrimServiceResponse.RES_MESSAGEACK, data.email, (long)data.id, 2);
			} else {
				service.getServiceResponse().respond(MrimServiceResponse.RES_MESSAGEACK, data.email, (long)data.id, 1);
			}			
		}
	}

	@SuppressWarnings("unused")
	private void parseMessage(MrimPacket packet) {
		int pos = 44;
		
		int msgId = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos+=4;
		int flags = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos+=4;
		String from = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos+=4+from.length();
		
		if (((MrimConstants.MESSAGE_FLAG_NOTIFY & flags) != 0)){
			service.getServiceResponse().respond(MrimServiceResponse.RES_TYPING, from);
			return;
		}
		
		String text = null;
		if (((MrimConstants.MESSAGE_FLAG_OLD & flags) != 0) || ((MrimConstants.MESSAGE_FLAG_AUTHORIZE & flags) != 0)){
			text = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
			pos+=4+text.length();
		} else {
			text = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
			pos+=4+(text.length()*2);
		}
		
		String rtfText = null;
		
		if (((MrimConstants.MESSAGE_FLAG_OLD & flags) != 0) || ((MrimConstants.MESSAGE_FLAG_AUTHORIZE & flags) != 0)){
			rtfText = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
			pos+=4+text.length();
		} else {
			rtfText = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
			pos+=4+(text.length()*2);
		}
		
		String from2 = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos+=4+from2.length();
		int seq = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos+=4;
		service.log("message from "+from+" - "+text);
		
		MrimMessage message = new MrimMessage();
		message.text = text;
		message.from = from;
		message.messageId = msgId;
		
		service.getServiceResponse().respond(MrimServiceResponse.RES_MESSAGE, message);
		
		if ((service.getOnlineInfo().status & MrimConstants.STATUS_FLAG_INVISIBLE) == 0 && (flags & MrimConstants.MESSAGE_FLAG_NORECV) == 0){
			sendMsgAck(from, msgId);
		}
	}

	private void sendMsgAck(String to, int msgId) {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_MESSAGE_RECV;
		byte[] toBytes = MrimEntityAdapter.string2lpsa(to);
		byte[] msgIdBytes = ProtocolUtils.int2ByteLE(msgId);
		
		packet.rawData = new byte[toBytes.length+msgIdBytes.length];
		System.arraycopy(toBytes, 0, packet.rawData, 0, toBytes.length);
		System.arraycopy(msgIdBytes, 0, packet.rawData, toBytes.length, 4);
		
		service.getRunnableService().sendToSocket(packet);
	}

	private void parseContacts(MrimPacket packet) {
		int pos = 44;
		
		List<MrimGroup> groups = new ArrayList<MrimGroup>();
		List<MrimBuddy> buddies = new ArrayList<MrimBuddy>();
		
		int responseCode = (int) MrimEntityAdapter.ul2Long(packet.rawData, pos);
		pos += 4;
		if (responseCode != MrimConstants.GET_CONTACTS_OK){
			return;
		}
		
		int groupCount = (int) MrimEntityAdapter.ul2Long(packet.rawData, pos);
		pos += 4;
		
		String groupMask = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos += (4 + groupMask.length());
		String contactMask = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		pos += (4 + contactMask.length());
		
		service.log("group mask " + groupMask);
		service.log("contact mask " + contactMask);
		
		for (int i = 0; i< groupCount; i++){
			int flags = (int) MrimEntityAdapter.ul2Long(packet.rawData, pos);
			pos += 4;
			
			String name = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
			pos += (4 + name.length()*2);
			service.log("group name " + name+", group flags " + flags);
			pos += MrimEntityAdapter.skipFormatted(packet.rawData, groupMask, pos, 2);
			
			if ((flags & MrimConstants.CONTACT_FLAG_REMOVED) != 0){
				continue;
			}
			
			MrimGroup group = new MrimGroup();
			group.groupId = i;
			group.name = name;
			group.flags = flags;
			
			//TODO use more flags
			groups.add(group);
		}
		
		int i = 0;
		while(pos < packet.rawData.length){
			service.log("----- buddy -----");
			int flags = (int)MrimEntityAdapter.ul2Long(packet.rawData, pos);
			service.log("buddy flags " + flags);
			pos+=4;
			
            int groupId = (int)MrimEntityAdapter.ul2Long(packet.rawData, pos);
            service.log("buddy group " + groupId);
            pos+=4;
            
            String uin = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));            
            service.log("buddy uin " + uin);
            
            String name = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));
            service.log("buddy name " + name);
            
            int serverFlags = (int)MrimEntityAdapter.ul2Long(packet.rawData, pos);
            service.log("buddy server flags " + serverFlags);
            pos+=4;
            
            int status = (int)MrimEntityAdapter.ul2Long(packet.rawData, pos);
            service.log("buddy status " + status);
            pos+=4;
            
            String phone = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));
            service.log("buddy phone " + phone);
            
            String statusText = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));
            service.log("buddy xstatus id " + statusText);
            
            String xStatusTitle = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));
            service.log("buddy xstatus title " + xStatusTitle);
            
            String xStatusText = MrimEntityAdapter.lpsw2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));
            service.log("buddy xstatus text " + xStatusText);
            
            long unknown = MrimEntityAdapter.ul2Long(packet.rawData, pos);
            service.log("buddy unk " + unknown);
            pos+=4;
            
            String client = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
            pos += (4 + MrimEntityAdapter.ul2Long(packet.rawData, pos));
            service.log("buddy uin " + uin + ", buddy name "+name);
            
            pos += MrimEntityAdapter.skipFormatted(packet.rawData, contactMask, pos, 12);
            
            if ((flags & MrimConstants.CONTACT_FLAG_REMOVED) != 0){
            	continue;
			}
            
            MrimBuddy buddy = new MrimBuddy();
            buddy.id = i;
            buddy.uin = uin;
            buddy.groupId = groupId;
            buddy.onlineInfo.xstatusName = xStatusTitle;
            buddy.onlineInfo.xstatusText = xStatusText;
            buddy.onlineInfo.status = status;
            buddy.onlineInfo.xstatusId = statusText;
            buddy.serverFlags = serverFlags;
            buddy.flags = flags;
            buddy.clientId = client;
            buddy.name = name;
            
            buddies.add(buddy);
            
            i++;
        }
		service.getServiceResponse().respond(MrimServiceResponse.RES_CONNECTING, 9);
		service.getServiceResponse().respond(MrimServiceResponse.RES_CLUPDATED, buddies, groups);
		service.setCurrentState(MrimServiceInternal.STATE_CONNECTED);
		service.startKeepalive();
		service.getServiceResponse().respond(MrimServiceResponse.RES_CONNECTED);
		getIcon(service.getMrid());
	}

	private void parsePingFreq(MrimPacket packet) {
		long pingFreq = MrimEntityAdapter.ul2Long(packet.rawData, 44);
		service.setPingFrequency(pingFreq);
	}

	private void loginFailed(MrimPacket packet) {
		service.lastConnectionError = MrimEntityAdapter.lpsa2String(packet.rawData, 44);
		service.getRunnableService().disconnect();
	}

	private void reconnectBos(MrimBosString bos) {
		service.setCurrentState(MrimServiceInternal.STATE_CONNECTING_BOS);
		service.log("reconnect to " + bos.bosAddress + ":" + bos.bosPort);
		service.getRunnableService().disconnect();

		service.getServiceResponse().respond(MrimServiceResponse.RES_CONNECTING, 3);
		service.setCurrentState(MrimServiceInternal.STATE_AUTHENTICATING);
		service.runService(bos.bosAddress, bos.bosPort);
	}
	
	private void proceedLogin3() throws IOException {
		
		service.log("logging in");
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_LOGIN3;
		
		MD5 hash = new MD5();
		hash.init();
		hash.updateASCII(service.getPw());
		hash.finish();
		byte[] md5pw = hash.getDigestBits();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(MrimEntityAdapter.string2lpsa(service.getMrid()));
		stream.write(ProtocolUtils.int2ByteLE(md5pw.length));
		stream.write(md5pw);
		stream.write(ProtocolUtils.int2ByteLE(0x56));
		stream.write(MrimEntityAdapter.string2lpsa(VERSION_INFO));
		stream.write(MrimEntityAdapter.string2lpsa(LANG));
		stream.write(ProtocolUtils.int2ByteLE(0x10));
		stream.write(ProtocolUtils.int2ByteLE(0x1));
		stream.write(MrimEntityAdapter.string2lpsa(GEO_LIST));
		stream.write(MrimEntityAdapter.string2lpsa(ALIAS));
		
		WeirdLoginEntity e2c = new WeirdLoginEntity();
		e2c.type = 0x2c;
		e2c.putBlob("00000134d148f68fab1ccbf13b4a2a05", "UTF-8");
		stream.write(e2c.toByteArray());
		
		WeirdLoginEntity e1 = new WeirdLoginEntity();
		e1.type = 0x1;
		e1.putBlob(new byte[]{0x4f, 0, 0, 0, 1});
		e1.putBlob("176x208", "UTF-8");
		//e1.putBlob("x", "UTF-8");
		stream.write(e1.toByteArray());
		
		WeirdLoginEntity e43 = new WeirdLoginEntity();
		e43.type = 0x43;
		e43.putBlob("RELEASE: "+Build.VERSION.RELEASE+";SDK_INT: "+Build.VERSION.SDK_INT, "UTF-16LE");
		stream.write(e43.toByteArray());
		
		WeirdLoginEntity e44 = new WeirdLoginEntity();
		e44.type = 0x44;
		e44.putBlob("MANUFACTURER: BEREZKA;MODEL: 61tc-311d;PRODUCT: tv;", "UTF-16LE");
		stream.write(e44.toByteArray());
		
		WeirdLoginEntity e45 = new WeirdLoginEntity();
		e45.type = 0x45;
		e45.putBlob("en", "UTF-16LE");
		stream.write(e45.toByteArray());
		
		packet.rawData = stream.toByteArray();
		
		service.getRunnableService().sendToSocket(packet);
	}

	
	/*private void proceedLogin3() throws IOException {
		service.log("logging in");
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_LOGIN3;
		
		MD5 hash = new MD5();
		hash.init();
		hash.updateASCII(service.getPw());
		hash.finish();

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(MrimEntityAdapter.string2lpsa(service.getMrid()));
		stream.write(hash.getDigestBits());
		stream.write(ProtocolUtils.int2ByteLE(0xffffffff));
		stream.write(MrimEntityAdapter.string2lpsa(VERSION_INFO));
		stream.write(MrimEntityAdapter.string2lpsa(LANG));
		stream.write(ProtocolUtils.int2ByteLE(0x10));
		stream.write(ProtocolUtils.int2ByteLE(0x1));
		stream.write(MrimEntityAdapter.string2lpsa(GEO_LIST));
		stream.write(MrimEntityAdapter.string2lpsa(ALIAS));
		
		for (int i = 0; i <= 0x7a; )
        {
			stream.write(i);   
				if (i == 9 || i == 0x1e || i == 0x2c || i == 0x42 || i == 0x43 || i == 0x44 || i == 0x45 ||
                 i == 0x4b || i == 0x4c || i == 0x68)
					stream.write(ProtocolUtils.int2ByteLE(0x01000000));
                else
                	stream.write(ProtocolUtils.int2ByteLE(0x02000000));

                if (i == 0)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000e66)); //revision of MAgent
                else if (i == 1)
                        stream.write(ProtocolUtils.int2ByteLE(7)); //seems to be minor version of MAgent
                else if (i == 2)
                        stream.write(ProtocolUtils.int2ByteLE(19));
                else if (i == 3)
                        stream.write(ProtocolUtils.int2ByteLE(0x51ec0ee9));
                else if (i == 4 || i == 6 || i == 7 || i == 0x2d || i == 0x2f || i == 0x3f || i == 0x40 || i == 0x47 ||
                 i == 0x4e)
                        stream.write(ProtocolUtils.int2ByteLE(1));
                else if (i == 8)
                        stream.write(ProtocolUtils.int2ByteLE(2));
                else if (i == 9)
                	stream.write(new byte[]{
                				0x16, 0x00, 0x00, 0x00, 0x59, 0x50, 0x43, 0x6a, 0x5c, 0x59, 0x58, 0x55, 0x78, 0x54, 0x5f, 0x40, 
                				0x46, 0x18, 0x46, 0x40, 0x54, 0x5a, 0x44, 0x66, 0x59, 0x57});
                else if (i == 0x14)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000501));
                else if (i == 0x2c)
                        stream.write(new byte[]{
                        		0x30, 0x39, 0x64, 0x65, 0x63, 0x31, 0x61, 0x63, 0x65, 0x33, 0x61, 0x30, 0x36, 0x34, 0x62, 0x64, 
                        		0x34, 0x31, 0x30, 0x34, 0x36, 0x64, 0x35, 0x61, 0x39, 0x37, 0x61, 0x39, 0x34, 0x62, 0x37, 0x36});
                else if (i == 0x41)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000c6e));
                else if (i == 0x42)
                        //Hardware characteristics
                        stream.write(new byte[]{
                        		0x41, 0x00, 0x4d, 0x00, 0x44, 0x00, 0x20, 0x00, 0x50, 0x00, 0x68, 0x00, 0x65, 0x00, 0x6e, 0x00, 
                        		0x6f, 0x00, 0x6d, 0x00, 0x28, 0x00, 0x74, 0x00, 0x6d, 0x00, 0x29, 0x00, 0x20, 0x00, 0x49, 0x00, 
                        		0x49, 0x00, 0x20, 0x00, 0x58, 0x00, 0x32, 0x00, 0x20, 0x00, 0x35, 0x00, 0x35, 0x00, 0x35, 0x00, 
                        		0x20, 0x00, 0x50, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x63, 0x00, 0x65, 0x00, 0x73, 0x00, 0x73, 0x00, 0x6f, 0x00, 0x72, 0x00});
                else if (i == 0x43)
                        //System characteristics
                        stream.write(new byte[]{
                        		0x4d, 0x00, 0x69, 0x00, 0x63, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x73, 0x00, 0x6f, 0x00, 0x66, 0x00, 
                        		0x74, 0x00, 0x20, 0x00, 0x57, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x64, 0x00, 0x6f, 0x00, 0x77, 0x00, 
                        		0x73, 0x00, 0x20, 0x00, 0x58, 0x00, 0x50, 0x00, 0x20, 0x00, 0x50, 0x00, 0x72, 0x00, 0x6f, 0x00, 
                        		0x66, 0x00, 0x65, 0x00, 0x73, 0x00, 0x73, 0x00, 0x69, 0x00, 0x6f, 0x00, 0x6e, 0x00, 0x61, 0x00, 0x6c, 0x00});
                else if (i == 0x45)
                        stream.write(new byte[]{0x08, 0x00, 0x00, 0x00, 0x30, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00});
                else if (i == 0x46)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000027));
                else if (i == 0x48)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000500));
                else if (i == 0x49)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000400));
                else if (i == 0x4a)
                        stream.write(ProtocolUtils.int2ByteLE(0x00000020));
                else if (i == 0x4b)
                        //Graphic card characteristics
                        stream.write(new byte[]{
                        		0x56, 0x00, 0x69, 0x00, 0x72, 0x00, 0x74, 0x00, 0x75, 0x00, 0x61, 0x00, 0x6c, 0x00, 0x42, 0x00, 
                        		0x6f, 0x00, 0x78, 0x00, 0x20, 0x00, 0x47, 0x00, 0x72, 0x00, 0x61, 0x00, 0x70, 0x00, 0x68, 0x00, 
                        		0x69, 0x00, 0x63, 0x00, 0x73, 0x00, 0x20, 0x00, 0x41, 0x00, 0x64, 0x00, 0x61, 0x00, 0x70, 0x00, 
                        		0x74, 0x00, 0x65, 0x00, 0x72, 0x00});
                else if (i == 0x4c)
                        stream.write(new byte[]{0x69, 0x00, 0x6e, 0x00, 0x6e, 0x00, 0x6f, 0x00, 0x74, 0x00, 0x65, 0x00, 0x6b, 0x00, 0x20, 0x00, 0x47, 0x00, 0x6d, 0x00, 0x62, 0x00, 0x48, 0x00});
                else if (i == 0x4d)
                        stream.write(ProtocolUtils.int2ByteLE(0x000001ff));
                else
                        stream.write(ProtocolUtils.int2ByteLE(0));

                if (i == 3 || i == 4 || i == 0x1a || i == 0x13 || i == 0x14 || i == 0x54 || i == 0x66)
                        i += 2;
                else if (i == 5)
                        i--;
                else if (i == 0x15)
                        i = 0x67;
                else if (i == 0x67)
                        i = 0x14;
                else if (i == 0x2f)
                        i = 0x3f;
                else
                        i++;
        }

		packet.rawData = stream.toByteArray();
		
		service.getRunnableService().sendToSocket(packet);
	}*/

	@SuppressWarnings("unused")
	private void proceedLogin() {
		service.log("logging in");
		MrimPacket packet = new MrimPacket();

		MD5 hash = new MD5();
		hash.init();
		hash.updateASCII(service.getPw());
		hash.finish();

		byte[] md5pass = hash.getDigestBits();
		packet.type = MrimConstants.MRIM_CS_LOGIN2;
		byte[] mrid = MrimEntityAdapter.string2lpsa(service.getMrid());
		//byte[] pw = MrimEntityAdapter.string2lpsa(service.getPw());
		byte[] alias = MrimEntityAdapter.string2lpsa(ALIAS);
		byte[] verInfo = MrimEntityAdapter.string2lpsa(VERSION_INFO);
		byte[] lang = MrimEntityAdapter.string2lpsa(LANG);
		byte[] statusDataBlob = getStatusSetByteBlob(service.getOnlineInfo().status, service.getOnlineInfo().xstatusId, service.getOnlineInfo().xstatusName, service.getOnlineInfo().xstatusText);
		byte[] raw = new byte[12 + mrid.length + md5pass.length + statusDataBlob.length + verInfo.length + lang.length + alias.length];

		Arrays.fill(raw, (byte) 0);
		int pos = 0;

		System.arraycopy(mrid, 0, raw, pos, mrid.length);
		pos += mrid.length;
		System.arraycopy(ProtocolUtils.int2ByteLE(md5pass.length), 0, raw, pos, 4);
		pos += 4;
		System.arraycopy(md5pass, 0, raw, pos, md5pass.length);
		pos += md5pass.length;
		
		/*System.arraycopy(ProtocolUtils.int2ByteLE(service.getCurrentStatus()), 0, raw, pos, 4);
		pos += 4;
		System.arraycopy(txtStatus, 0, raw, pos, txtStatus.length);
		pos += txtStatus.length;
		System.arraycopy(xstatusName, 0, raw, pos, xstatusName.length);
		pos += xstatusName.length;
		System.arraycopy(xstatusText, 0, raw, pos, xstatusText.length);
		pos += xstatusText.length;
		System.arraycopy(ProtocolUtils.int2ByteLE(0x12), 0, raw, pos, 4);
		pos += 4;*/
		
		System.arraycopy(statusDataBlob, 0, raw, pos, statusDataBlob.length);
		pos+=statusDataBlob.length;
		
		System.arraycopy(verInfo, 0, raw, pos, verInfo.length);
		pos += verInfo.length;
		System.arraycopy(lang, 0, raw, pos, lang.length);
		pos += lang.length;
		//skip two integers = 0
		pos += 8;
		
		System.arraycopy(alias, 0, raw, pos, alias.length);
		pos += alias.length;

		packet.rawData = raw;

		service.getRunnableService().sendToSocket(packet);
	}

	private MrimPacket parseBosString(byte[] tail) throws MrimException {
		MrimBosString bos = new MrimBosString();

		String fullBos = new String(tail);

		if (fullBos.length() < 1) {
			throw new MrimException("Corrupted BOS address");
		}

		if (fullBos.indexOf(":") > -1) {
			String[] boss = fullBos.split(":");
			bos.bosAddress = boss[0];
			bos.bosPort = Integer.parseInt(boss[1]);
		} else {
			bos.bosAddress = fullBos;
		}
		bos.type = MrimConstants.MRIM_CS_HELLO;

		return bos;
	}

	public byte[] packet2Bytes(MrimPacket packet) throws MrimException {

		if (packet == null || packet.type == -1) {
			throw new MrimException("Nothing to convert");
		}

		if (packet.rawData == null) {
			packet.rawData = new byte[0];
		}

		byte[] out = new byte[44 + packet.rawData.length];
		Arrays.fill(out, (byte) 0);
		System.arraycopy(ProtocolUtils.int2ByteLE(MrimConstants.CS_MAGIC), 0, out, 0, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(MrimConstants.PROTO_VERSION), 0, out, 4, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE((int) packet.seqNumber), 0, out, 8, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE((int) packet.type), 0, out, 12, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(packet.rawData.length), 0, out, 16, 4);

		// here IP data may be added
		System.arraycopy(packet.rawData, 0, out, 44, packet.rawData.length);
		return out;
	}

	public byte[] packets2Bytes(MrimPacket[] packets) throws MrimException {
		if (packets == null) {
			throw new MrimException("Error - packets is null");
		}

		if (packets.length == 1) {
			return packet2Bytes(packets[0]);
		}

		int length = 0;
		List<byte[]> bytes = new ArrayList<byte[]>();
		for (int i = 0; i < packets.length; i++) {
			if (packets[i] == null) {
				continue;
			}
			byte[] out = packet2Bytes(packets[i]);
			length += out.length;
			bytes.add(out);
		}

		byte[] out = new byte[length];
		int pos = 0;
		for (int i = 0; i < bytes.size(); i++) {
			System.arraycopy(bytes.get(i), 0, out, pos, bytes.get(i).length);
			pos += bytes.get(i).length;
		}

		return out;
	}

	public void sendHello() {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_HELLO;
		packet.rawData = new byte[0];

		service.getRunnableService().sendToSocket(packet);
	}

	public void sendKeepalive() {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_PING;
		packet.rawData = new byte[0];

		service.getRunnableService().sendToSocket(packet);
	}

	public void sendMessage(MrimMessage message) {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_MESSAGE;
		
		byte[] toBytes = MrimEntityAdapter.string2lpsa(message.to);
		byte[] textBytes = MrimEntityAdapter.string2lpsw(message.text);
		byte[] rtfBytes = MrimEntityAdapter.string2lpsa("");
		
		byte[] data = new byte[toBytes.length+textBytes.length+rtfBytes.length+4];
		int pos = 0;
		System.arraycopy(ProtocolUtils.int2ByteLE(message.flags), 0, data, pos, 4);
		pos+=4;
		System.arraycopy(toBytes, 0, data, pos, toBytes.length);
		pos+=toBytes.length;
		System.arraycopy(textBytes, 0, data, pos, textBytes.length);
		pos+=textBytes.length;
		System.arraycopy(rtfBytes, 0, data, pos, rtfBytes.length);
		pos+=rtfBytes.length;
		
		packet.rawData = data;
		
		long messageId = service.getRunnableService().sendToSocket(packet);
		MessageData mdata = new MessageData();
		mdata.email = message.to;
		mdata.id = message.messageId;
		msgIDs.put(messageId, mdata);
	}
	
	public void getIcon(final String email) {
		new Thread(){
			@Override
			public void run(){
				String[] items = email.split("@");
				String[] domains = items[1].split("\\.");
				String url = String.format(ICON_SERVER, domains[0], items[0]);
				try {
					HttpClient hc = new DefaultHttpClient();
					HttpHead head = new HttpHead(url);
				
					HttpResponse response = hc.execute(head);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND){
						return;
					}
					HttpGet get = new HttpGet(url);
					response = hc.execute(get);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					//int len = (int) response.getEntity().getContentLength();
					//if (len > 0){
						byte[] bytes = new byte[1024];
						int read = 0;
						InputStream content = response.getEntity().getContent();
						while (read > -1){
							read = content.read(bytes, 0, 1024);
							if (read > 0){
								bos.write(bytes, 0, read);
							}
						};
					//}					
					
					service.getServiceResponse().respond(MrimServiceResponse.RES_SAVEIMAGEFILE, bos.toByteArray(), email, new String(email.hashCode()+""));
				} catch (Exception e) {
					service.log(url+"\n");
					service.log(e);
				}
			}
		}.start();
	}

	private class MessageData implements Serializable {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -5512618917441817714L;

		public int id = 0;
		private String email;
		
	}

	public void setStatus(MrimOnlineInfo info) {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_CHANGE_STATUS;
		
		packet.rawData = getStatusSetByteBlob(info.status, info.xstatusId, info.xstatusName, info.xstatusText);
		
		service.getRunnableService().sendToSocket(packet);
	}
	
	public byte[] getStatusSetByteBlob(int status, String xstatusName, String xstatusTitle, String xstatusText){
		byte[] xstNameBytes = MrimEntityAdapter.string2lpsa(xstatusName);
		byte[] xstTitleBytes = MrimEntityAdapter.string2lpsw(xstatusTitle);
		byte[] xstTextBytes = MrimEntityAdapter.string2lpsw(xstatusText);
		
		byte[] bytes = new byte[8+xstNameBytes.length+xstTextBytes.length+xstTitleBytes.length];
		int pos=0;
		System.arraycopy(ProtocolUtils.int2ByteLE(status), 0, bytes, pos, 4);
		pos+=4;
		
		System.arraycopy(xstNameBytes, 0, bytes, pos, xstNameBytes.length);
		pos+=xstNameBytes.length;
		System.arraycopy(xstTitleBytes, 0, bytes, pos, xstTitleBytes.length);
		pos+=xstTitleBytes.length;
		System.arraycopy(xstTextBytes, 0, bytes, pos, xstTextBytes.length);
		pos+=xstTextBytes.length;
		
		System.arraycopy(ProtocolUtils.int2ByteLE(0x56), 0, bytes, pos, 4);
		pos += 4;
		
		return bytes;
	}

	public void sendTyping(String uid) {
		MrimMessage message = new MrimMessage();
		message.to = uid;
		message.text = " ";
		message.flags = MrimConstants.MESSAGE_FLAG_NOTIFY;
		//lazy filling in;
		message.messageId = VERSION_INFO.hashCode();
		
		sendMessage(message);
	}

	public void askForWebAuthKey() {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_GET_MPOP_SESSION;
		
		service.getRunnableService().sendToSocket(packet);
	}

	
	private class WeirdLoginEntity implements Serializable {
		private static final long serialVersionUID = -5484794243398170724L;
		
		public int type;
		public final List<byte[]> blobs = new LinkedList<byte[]>();
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			os.write(ProtocolUtils.int2ByteLE(type));
			os.write(blobs.size());
			for (byte[] blob : blobs){
				os.write(blob);
			}
			
			return os.toByteArray();
		}
		
		public void putBlob(byte[] bt){
			byte[] blob = new byte[4+bt.length];
			System.arraycopy(ProtocolUtils.int2ByteLE(bt.length), 0, blob, 0, 4);
			System.arraycopy(bt, 0, blob, 4, bt.length);
			
			blobs.add(blob);
		}
		
		public void putBlob(String data, String encoding) {
			byte[] strData;
			try {
				strData = data.getBytes(encoding);
			} catch (UnsupportedEncodingException e) {
				strData = data.getBytes();
			}
			putBlob(strData);
		}
	}
}
