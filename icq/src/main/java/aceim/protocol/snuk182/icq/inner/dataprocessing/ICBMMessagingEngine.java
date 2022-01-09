package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMMessage;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQFileInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class ICBMMessagingEngine {
	private ICQServiceInternal service;
	private MessageParser parser;
	
	private static final DateFormat OFFLINE_DATE_FORMATTER = new SimpleDateFormat("dd MMMM yyyy, HH:mm:ss");

	static final Random RANDOM = new Random();

	private ScheduledFuture<?> task;
	public ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);

	public ICBMMessagingEngine(ICQServiceInternal icqServiceInternal) {
		parser = new MessageParser();
		this.service = icqServiceInternal;
	}

	public void parseMessage(Snac snac) {
		if (snac.plainData == null) {
			return;
		}

		parser.parseMessage(snac.plainData);
	}

	public void parsePluginMessage(Snac snac, boolean isAck) {
		if (snac.plainData == null) {
			return;
		}

		parser.parsePluginMessage(snac.plainData, isAck);
	}

	public void parseOfflineMessage(byte[] tailData) {
		parser.parseOfflineMessage(tailData);
	}

	private byte[] getAnswerXStatusSuffix(String title, String value) {
		byte[] header = new byte[81];
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 79), 0, header, 0, 2);
		System.arraycopy(ICQConstants.GUID_XSTATUSMSG, 0, header, 2, 16);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0x8), 0, header, 18, 2);
		System.arraycopy(ProtocolUtils.int2ByteLE(42), 0, header, 20, 4);
		byte[] headerStrBytes = new String("Script Plug-in: Remote Notification Arrive").getBytes();
		System.arraycopy(headerStrBytes, 0, header, 24, 42);
		System.arraycopy(ProtocolUtils.int2ByteLE(0x10000), 0, header, 66, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(0), 0, header, 70, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(0), 0, header, 74, 4);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, header, 78, 2);
		header[80] = 0;

		StringBuilder reqStrBu = new StringBuilder();
		reqStrBu.append("<NR><RES>");

		reqStrBu.append(ProtocolUtils.xmlToParameter("<ret event='OnRemoteNotification'><srv><id>cAwaySrv</id><val srv_id='cAwaySrv'><Root><CASXtraSetAwayMessage></CASXtraSetAwayMessage><uin>"));
		reqStrBu.append(service.getUn());
		reqStrBu.append(ProtocolUtils.xmlToParameter("</uin><index>1</index><title>"));
		reqStrBu.append(title != null ? title : "");
		reqStrBu.append(ProtocolUtils.xmlToParameter("</title><desc>"));
		reqStrBu.append(value != null ? value : "");
		reqStrBu.append(ProtocolUtils.xmlToParameter("</desc></Root></val></srv></ret>"));
		reqStrBu.append("</RES></NR>");

		String reqStr = reqStrBu.toString();

		byte[] reqBytes = reqStr.getBytes();
		byte[] body = new byte[8 + reqBytes.length];
		System.arraycopy(ProtocolUtils.int2ByteLE(reqBytes.length + 4), 0, body, 0, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(reqBytes.length), 0, body, 4, 4);
		System.arraycopy(reqBytes, 0, body, 8, reqBytes.length);

		byte[] total = new byte[header.length + body.length];
		System.arraycopy(header, 0, total, 0, header.length);
		System.arraycopy(body, 0, total, header.length, body.length);

		return total;
	}

	private byte[] getAskXStatusSuffix(String uin) {
		byte[] header = new byte[81];
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 79), 0, header, 0, 2);
		System.arraycopy(ICQConstants.GUID_XSTATUSMSG, 0, header, 2, 16);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0x8), 0, header, 18, 2);
		System.arraycopy(ProtocolUtils.int2ByteLE(42), 0, header, 20, 4);
		byte[] headerStrBytes = new String("Script Plug-in: Remote Notification Arrive").getBytes();
		System.arraycopy(headerStrBytes, 0, header, 24, 42);
		System.arraycopy(ProtocolUtils.int2ByteLE(0x100), 0, header, 66, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(0), 0, header, 70, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(0), 0, header, 74, 4);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, header, 78, 2);
		header[80] = 0;

		String reqStr = "<N><QUERY>" + ProtocolUtils.xmlToParameter("<Q><PluginID>srvMng</PluginID></Q>") + "</QUERY><NOTIFY>" + ProtocolUtils.xmlToParameter("<srv><id>cAwaySrv</id><req><id>AwayStat</id><trans>") + 0 + ProtocolUtils.xmlToParameter("</trans><senderId>" + uin + "</senderId></req></srv>") + "</NOTIFY></N>";
		byte[] reqBytes = reqStr.getBytes();
		byte[] body = new byte[8 + reqBytes.length];
		System.arraycopy(ProtocolUtils.int2ByteLE(reqBytes.length + 4), 0, body, 0, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(reqBytes.length), 0, body, 4, 4);
		System.arraycopy(reqBytes, 0, body, 8, reqBytes.length);

		byte[] total = new byte[header.length + body.length];
		System.arraycopy(header, 0, total, 0, header.length);
		System.arraycopy(body, 0, total, header.length, body.length);

		return total;
	}

	public void askForXStatus(String uin) {
		ICBMMessage message = new ICBMMessage();
		message.pluginSpecificData = getAskXStatusSuffix(message.senderId);
		message.receiverId = uin;
		message.text = "";
		message.messageType = ICQConstants.MTYPE_PLUGIN;

		sendChannel2Message(message);
	}

	private byte[] getPlainMessageSuffix() {
		byte[] guidBytes;
		try {
			guidBytes = new String(ICQConstants.GUID_UTF8).getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			guidBytes = new String(ICQConstants.GUID_UTF8).getBytes();
		}
		byte[] suffix = new byte[12 + guidBytes.length];
		System.arraycopy(new byte[] { 0, 0, 0, 0 }, 0, suffix, 0, 4);
		System.arraycopy(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, 0 }, 0, suffix, 4, 4);
		System.arraycopy(ProtocolUtils.int2ByteLE(guidBytes.length), 0, suffix, 8, 4);
		System.arraycopy(guidBytes, 0, suffix, 12, guidBytes.length);

		return suffix;
	}

	private void sendChannel2PluginMessage(ICBMMessage message) {
		if (message.messageId == null) {
			message.messageId = ProtocolUtils.long2ByteBE(new Random().nextLong());
		}

		short msgSeq = (short) 0xfffe;

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_PLUGINMSG;
		data.requestId = ICQConstants.SNAC_MESSAGING_PLUGINMSG;

		byte[] uidBytes;
		try {
			uidBytes = message.receiverId.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uidBytes = message.receiverId.getBytes();
		}

		byte[] block1 = new byte[13 + uidBytes.length];
		System.arraycopy(message.messageId, 0, block1, 0, 8);
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 2), 0, block1, 8, 2);
		block1[10] = (byte) uidBytes.length;
		System.arraycopy(uidBytes, 0, block1, 11, uidBytes.length);
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 3), 0, block1, 11 + uidBytes.length, 2);

		byte[] tlv2711data;
		if (message.messageType == ICQConstants.MTYPE_FILEREQ) {
			tlv2711data = new byte[] { 0, 2, 0, 1 };
		} else {
			byte[] textBytes = new byte[0]; // dummy
			byte[] suffix = message.pluginSpecificData != null ? message.pluginSpecificData : getPlainMessageSuffix();
			byte[] msgByteBlock = new byte[8 + textBytes.length + 1 + suffix.length];
			msgByteBlock[0] = message.messageType; // !!!
			msgByteBlock[1] = 0;
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, msgByteBlock, 2, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, msgByteBlock, 4, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) (textBytes.length + 1)), 0, msgByteBlock, 6, 2);
			System.arraycopy(textBytes, 0, msgByteBlock, 8, textBytes.length);
			msgByteBlock[8 + textBytes.length] = 0;
			System.arraycopy(suffix, 0, msgByteBlock, 9 + textBytes.length, suffix.length);

			tlv2711data = new byte[2 + 2 + 16 + 2 + 4 + 1 + 2 + 2 + 2 + 12 + msgByteBlock.length];
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 27), 0, tlv2711data, 0, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 9), 0, tlv2711data, 2, 2);
			byte[] zeros = new byte[16];
			Arrays.fill(zeros, (byte) 0);
			System.arraycopy(zeros, 0, tlv2711data, 4, 16);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, tlv2711data, 20, 2);
			System.arraycopy(ProtocolUtils.int2ByteLE(1), 0, tlv2711data, 22, 4);
			tlv2711data[26] = 0;
			System.arraycopy(ProtocolUtils.short2ByteLE(msgSeq), 0, tlv2711data, 27, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 14), 0, tlv2711data, 29, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE(msgSeq), 0, tlv2711data, 31, 2);
			zeros = new byte[12];
			Arrays.fill(zeros, (byte) 0);
			System.arraycopy(zeros, 0, tlv2711data, 33, 12);
			System.arraycopy(msgByteBlock, 0, tlv2711data, 45, msgByteBlock.length);
		}

		byte[] total = new byte[block1.length + tlv2711data.length];
		System.arraycopy(block1, 0, total, 0, block1.length);
		System.arraycopy(tlv2711data, 0, total, block1.length, tlv2711data.length);

		data.plainData = total;
		flap.data = data;

		service.getRunnableService().sendToSocket(flap);
	}

	private void sendChannel2Message(ICBMMessage message) {
		if (message.messageId == null) {
			message.messageId = ProtocolUtils.long2ByteBE(new Random().nextLong());
		}

		short msgSeq = (short) 0xfffe;

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_SENDTHROUGHSERVER;
		data.requestId = ICQConstants.SNAC_MESSAGING_SENDTHROUGHSERVER;

		/*
		 * TLV internalIpTLV = new TLV(); internalIpTLV.setType(0x03); byte[]
		 * buffer; try { buffer = InetAddress.getLocalHost().getAddress(); }
		 * catch (UnknownHostException e) { buffer = new byte[]{0,0,0,0}; }
		 * internalIpTLV.setValue(buffer);
		 * 
		 * TLV portTLV = new TLV(); portTLV.setType(0x05);
		 * portTLV.setValue(Utils.short2ByteBE(ICQConstants.ICBM_PORT));
		 */

		TLV unknownA = new TLV();
		unknownA.type = 0xa;
		unknownA.value = new byte[] { 0, 1 };

		TLV unknownF = new TLV();
		unknownF.type = 0xf;

		TLV msgTLV = new TLV();
		msgTLV.type = 0x2711;

		TLV[] tlv5content;
		byte[] clsid;

		if (message.messageType != ICQConstants.MTYPE_FILEREQ) {
			tlv5content = new TLV[] { unknownA, unknownF, msgTLV };
			clsid = ICQConstants.CLSID_ICQUTF;

			byte[] textBytes;
			try {
				textBytes = message.text.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				textBytes = message.text.getBytes();
			}

			byte[] suffix = null;
			switch (message.messageType) {
			case ICQConstants.MTYPE_PLAIN:
				suffix = getPlainMessageSuffix();
				break;
			case ICQConstants.MTYPE_PLUGIN:
				suffix = message.pluginSpecificData;
				break;
			default:
				suffix = new byte[0];
				break;
			}

			byte[] msgByteBlock = new byte[8 + textBytes.length + 1 + suffix.length];
			msgByteBlock[0] = message.messageType; // !!!
			msgByteBlock[1] = 0;
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, msgByteBlock, 2, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 1), 0, msgByteBlock, 4, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) (textBytes.length + 1)), 0, msgByteBlock, 6, 2);
			System.arraycopy(textBytes, 0, msgByteBlock, 8, textBytes.length);
			msgByteBlock[8 + textBytes.length] = 0;
			System.arraycopy(suffix, 0, msgByteBlock, 9 + textBytes.length, suffix.length);

			byte[] tlv2711data = new byte[2 + 2 + 16 + 2 + 4 + 1 + 2 + 2 + 2 + 12 + msgByteBlock.length];
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 27), 0, tlv2711data, 0, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 9), 0, tlv2711data, 2, 2);
			byte[] zeros = new byte[16];
			Arrays.fill(zeros, (byte) 0);
			System.arraycopy(zeros, 0, tlv2711data, 4, 16);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 0), 0, tlv2711data, 20, 2);
			System.arraycopy(ProtocolUtils.int2ByteLE(3), 0, tlv2711data, 22, 4);
			tlv2711data[26] = 0;
			System.arraycopy(ProtocolUtils.short2ByteLE(msgSeq), 0, tlv2711data, 27, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE((short) 14), 0, tlv2711data, 29, 2);
			System.arraycopy(ProtocolUtils.short2ByteLE(msgSeq), 0, tlv2711data, 31, 2);
			zeros = new byte[12];
			Arrays.fill(zeros, (byte) 0);
			System.arraycopy(zeros, 0, tlv2711data, 33, 12);
			System.arraycopy(msgByteBlock, 0, tlv2711data, 45, msgByteBlock.length);

			msgTLV.value = tlv2711data;
		} else {
			clsid = ICQConstants.CLSID_AIM_FILESEND;
			if (message.rvMessageType != 0) {
				tlv5content = new TLV[] { unknownA, unknownF, msgTLV };

				msgTLV.value = new byte[0];
			} else {
				// dummy
				tlv5content = new TLV[] { unknownA, unknownF, msgTLV };

				msgTLV.value = new byte[0];
			}
		}

		byte[] tlv5data = service.getDataParser().tlvs2Bytes(tlv5content);
		byte[] tlv5fullData = new byte[26 + tlv5data.length];
		System.arraycopy(ProtocolUtils.short2ByteBE(message.rvMessageType), 0, tlv5fullData, 0, 2);
		System.arraycopy(message.messageId, 0, tlv5fullData, 2, 8);
		System.arraycopy(clsid, 0, tlv5fullData, 10, 16);
		System.arraycopy(tlv5data, 0, tlv5fullData, 26, tlv5data.length);

		TLV ch2messageTLV = new TLV();
		ch2messageTLV.type = 0x5;
		ch2messageTLV.value = tlv5fullData;

		TLV confirmAckTLV = new TLV();
		confirmAckTLV.type = 0x3;

		byte[] uidBytes;
		try {
			uidBytes = message.receiverId.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uidBytes = message.receiverId.getBytes();
		}
		byte[] snacRawData = new byte[11 + uidBytes.length];
		System.arraycopy(message.messageId, 0, snacRawData, 0, 8);
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 2), 0, snacRawData, 8, 2);
		snacRawData[10] = (byte) message.receiverId.length();

		System.arraycopy(uidBytes, 0, snacRawData, 11, uidBytes.length);

		data.data = new TLV[] { ch2messageTLV, confirmAckTLV };
		data.plainData = snacRawData;

		flap.data = data;

		service.getRunnableService().sendToSocket(flap);
	}

	public long sendMessage(ICBMMessage message) {
		if (message == null) {
			service.log("msg to send is null " + new Date());
		}
		
		if (message.messageId == null) {
			message.messageId = new byte[8];
			RANDOM.nextBytes(message.messageId);
		} 

		for (ICQOnlineInfo info : service.getBuddyList().buddyInfos) {
			if (info != null && info.uin.equals(message.receiverId) && info.capabilities != null) {
				for (String cap : info.capabilities) {
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_ICQUTF))) {
						sendChannel2Message(message);
						return ProtocolUtils.bytes2LongBE(message.messageId);
					}
				}
				break;
			}
		}
		sendChannel1Message(message);
		
		return ProtocolUtils.bytes2LongBE(message.messageId);
	}

	public void sendFileMessage(ICBMMessage message) {
		for (ICQOnlineInfo info : service.getBuddyList().buddyInfos) {
			if (info != null && info.uin.equals(message.receiverId)) {
				for (String cap : info.capabilities) {
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_AIM_FILESEND))) {
						sendChannel2Message(message);
						return;
					}
				}
				break;
			}
		}
	}

	private void sendChannel1Message(ICBMMessage message) {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_SENDTHROUGHSERVER;
		data.requestId = 0;

		byte[] textBytes;
		try {
			textBytes = message.text.getBytes("windows-1251");
		} catch (UnsupportedEncodingException e) {
			textBytes = message.text.getBytes();
		}

		byte[] caps = new byte[] { 5, 1, 0, 1, 1 };
		byte[] text = new byte[8 + textBytes.length];
		text[1] = text[0] = 1;
		byte[] textLength = ProtocolUtils.short2ByteBE((short) (textBytes.length + 4));
		System.arraycopy(textLength, 0, text, 2, 2);
		text[5] = text[4] = 0;
		text[7] = text[6] = (byte) 0xff;
		System.arraycopy(textBytes, 0, text, 8, textBytes.length);

		TLV msgTLV = new TLV();
		msgTLV.type = 2;
		byte[] msgTLVBytes = new byte[caps.length + text.length];
		System.arraycopy(caps, 0, msgTLVBytes, 0, caps.length);
		System.arraycopy(text, 0, msgTLVBytes, caps.length, text.length);
		msgTLV.value = msgTLVBytes;

		TLV confirmAckTLV = new TLV();
		confirmAckTLV.type = 0x3;

		TLV storeMsgTLV = new TLV();
		storeMsgTLV.type = 0x6;

		byte[] uidBytes;
		try {
			uidBytes = message.receiverId.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			uidBytes = message.receiverId.getBytes();
		}
		byte[] snacRawData = new byte[11 + uidBytes.length];
		System.arraycopy(message.messageId, 0, snacRawData, 0, 8);
		System.arraycopy(ProtocolUtils.short2ByteBE((short) 1), 0, snacRawData, 8, 2);
		snacRawData[10] = (byte) message.receiverId.length();
		System.arraycopy(uidBytes, 0, snacRawData, 11, uidBytes.length);

		data.data = new TLV[] { msgTLV, confirmAckTLV, storeMsgTLV };
		data.plainData = snacRawData;

		flap.data = data;

		service.getRunnableService().sendToSocket(flap);
	}

	class MessageParser {

		String encoding = "windows-1251";

		void parseMessage(byte[] data) {
			ICBMMessage message = new ICBMMessage();
			System.arraycopy(data, 0, message.messageId, 0, 8);
			message.channel = ProtocolUtils.bytes2ShortBE(data, 8);
			message.receivingTime = new Date();

			String uin = new String(data, 11, data[10]);
			message.senderId = uin;

			@SuppressWarnings("unused")
			short warningLevel = ProtocolUtils.bytes2ShortBE(data, 11 + data[10]);
			int fixedPartTlvCount = ProtocolUtils.bytes2ShortBE(data, 13 + data[10]);

			int tlvDataLength = data.length - 15 - data[10];
			byte[] tlvData = new byte[tlvDataLength];
			System.arraycopy(data, 15 + data[10], tlvData, 0, tlvDataLength);

			service.log("message from " + message.senderId + " id " + ProtocolUtils.getHexString(message.messageId));

			ICQOnlineInfo info = new ICQOnlineInfo();
			try {
				TLV[] tlvs = service.getDataParser().parseTLV(tlvData);
				for (int i = 0; i < fixedPartTlvCount; i++) {
					service.getOnlineInfoEngine().onlineInfoTLVMap(tlvs[i], info);
				}

				for (int i = fixedPartTlvCount; i < tlvs.length; i++) {
					messageTLVMap(tlvs[i], message);
				}

				service.log("message type " + message.messageType);
				
				
				//if (service.checkFileTransferEngineCreated() && service.getFileTransferEngine().findMessageByMessageId(message.messageId)!=null){
				if (!message.senderId.equals(service.getUn()) && message.capability != null && message.capability.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_AIM_FILESEND))) {
					switch (message.messageType) {
					case 0:						
						if (!message.connectFTPeer && !message.connectFTProxy) {
							service.getFileTransferEngine().getMessages().add(message);
							service.getServiceResponse().respond(ICQServiceResponse.RES_FILEMESSAGE, message);
						} else {
							message.receiverId = message.senderId;
							message.senderId = service.getUn();
							service.getFileTransferEngine().redirectRequest(message);
						}
						break;
					case 1:
						service.getFileTransferEngine().transferFailed(new ICQException("Cancelled"), "", message, uin);
						break;
					case 2:
						service.getFileTransferEngine().fireTransfer(message);
						break;
					}
				}
				
			} catch (ICQException e) {
				service.log(e);
			}
		}

		void parsePluginMessage(byte[] plainData, boolean isAck) {
			ICBMMessage message = new ICBMMessage();

			int pos = 0;

			System.arraycopy(plainData, 0, message.messageId, 0, 8);
			pos += 8;

			short channel = ProtocolUtils.bytes2ShortBE(plainData, pos);
			message.channel = channel;
			pos += 2;
			int uinBytes = plainData[pos];
			pos += 1;
			message.senderId = new String(plainData, pos, uinBytes);
			pos += uinBytes;

			if (isAck) {
				service.getServiceResponse().respond(ICQServiceResponse.RES_MESSAGEACK, message.senderId, ProtocolUtils.bytes2LongBE(message.messageId, 0), (byte)1);
				return;
			}

			short reasonId = ProtocolUtils.bytes2ShortBE(plainData, pos);
			pos += 2;
			switch (reasonId) {
			case 3:
				byte[] data = new byte[plainData.length - pos];
				System.arraycopy(plainData, pos, data, 0, data.length);

				if (data.length < 5) {
					service.getFileTransferEngine().cancel(ProtocolUtils.bytes2LongBE(message.messageId, 0));
					return;
				} else {
					channel2TextMessage(data, message, true);
				}

				break;
			}
		}

		void parseOfflineMessage(byte[] tailData) {
			ICBMMessage message = new ICBMMessage();

			int senderUinInt = ProtocolUtils.bytes2IntLE(tailData, 0);
			message.senderId = senderUinInt + "";

			short year = ProtocolUtils.bytes2ShortLE(tailData, 4);
			byte month = tailData[6];
			byte day = tailData[7];
			byte hour = tailData[8];
			byte minute = tailData[9];
			Calendar sendingDate = Calendar.getInstance();
			sendingDate.set(year, month, day, hour, minute);
			message.sendingTime = sendingDate.getTime();
			message.receivingTime = new Date();

			byte mType = tailData[10];
			@SuppressWarnings("unused")
			byte mFlag = tailData[11];

			switch (mType) {
			case ICQConstants.MTYPE_PLAIN:
				int textLength = ProtocolUtils.bytes2ShortLE(tailData, 12) - 1;

				String text;
				try {
					text = new String(tailData, 14, textLength, "windows-1251");
				} catch (UnsupportedEncodingException e) {
					text = new String(tailData, 14, textLength);
				}

				message.text = "(Sent at "+OFFLINE_DATE_FORMATTER.format(message.sendingTime)+") "+text;
				service.log(message.senderId + " says: " + text);
				
				if (text.length() > 0)
					notifyMessageReceived(message);
				break;
			}
		}

		private void messageTLVMap(TLV tlv, ICBMMessage message) {

			switch (tlv.type) {
			case 0x5:
				switch (message.channel) { // message channel 2
				case 0x2:
					if (tlv.value == null) {
						return;
					}
					message.messageType = (byte) ProtocolUtils.bytes2ShortBE(tlv.value, 0);
					// eliminate message id - 8 bytes
					byte[] capability = new byte[16];
					System.arraycopy(tlv.value, 10, capability, 0, 16);
					String strCap = ProtocolUtils.getHexString(capability);
					message.capability = strCap;

					byte[] tailData = new byte[tlv.value.length - 26];
					System.arraycopy(tlv.value, 26, tailData, 0, tailData.length);
					try {
						TLV[] channel2Tlvs = service.getDataParser().parseTLV(tailData);
						for (TLV ch2tlv : channel2Tlvs) {
							channel2MessageTLVMap(ch2tlv, message);
						}
						if (service.getFileTransferEngine().findMessageByMessageId(ProtocolUtils.bytes2LongBE(message.messageId, 0)) != null) {
							message.connectFTPeer = true;
						}
					} catch (ICQException e) {
						service.log(e);
					}
					break;
				case 0x4: // message channel 4
					channel4MessageTLVMap(tlv, message);
					break;
				}
				break;
			case 0x2:
				switch (message.channel) { // message channel 1
				case 0x1:
					try {
						TLV[] channel1Tlvs = service.getDataParser().parseTLV(tlv.value);
						for (TLV ch1Tlv : channel1Tlvs) {
							channel1MessageTLVMap(ch1Tlv, message);
						}
					} catch (ICQException e) {
						service.log(e);
					}
					break;
				}
				break;
			case 0x24:
				service.log("0x24 unk cap - " + new String(tlv.value));
				break;
			case 0x13:
				service.log("0x13 unk cap - " + ProtocolUtils.getHexString(tlv.value));
				break;
			}
		}

		private void channel4MessageTLVMap(TLV tlv, ICBMMessage message) {
			int uinBytes = ProtocolUtils.bytes2IntLE(tlv.value, 0);
			String uin = new String(uinBytes + "");
			message.senderId = uin;

			byte mType = tlv.value[4];
			@SuppressWarnings("unused")
			byte mFlag = tlv.value[5];

			switch (mType) {
			case ICQConstants.MTYPE_PLAIN:
				int textLength = ProtocolUtils.bytes2ShortLE(tlv.value, 6) - 1;
				String text;
				try {
					text = new String(tlv.value, 8, textLength, "ASCII");
				} catch (UnsupportedEncodingException e) {
					text = new String(tlv.value, 8, textLength);
				}

				message.text = text;
				service.log(message.senderId + " says " + text);

				if (text.length() > 0)
					notifyMessageReceived(message);
				break;
			}
		}

		private void channel1MessageTLVMap(TLV tlv, ICBMMessage message) {
			byte[] bytes = ProtocolUtils.int2ByteBE(tlv.type);
			switch (bytes[2]) {
			case 0x5:
				byte[] capsArray = tlv.value;
				service.log(ProtocolUtils.getSpacedHexString(capsArray));

				/*
				 * if (capsArray.length > 0 && capsArray[capsArray.length - 1]
				 * == 6) { encoding = "UTF-16"; } else { encoding =
				 * "windows-1251"; }
				 * service.log("		channel 1 message require caps: "+encoding);
				 */

				break;
			case 0x1:
				int charsetType = ProtocolUtils.bytes2ShortBE(tlv.value, 0);
				int charsetSubtype = ProtocolUtils.bytes2ShortBE(tlv.value, 2);
				switch (charsetType) {
				case 0:
					encoding = "windows-1251";
					break;
				case 2:
					encoding = "UTF-16";
					break;
				default:
					encoding = "UTF-8";
					break;
				}

				service.log("charset type " + charsetType + "| charset subtype " + charsetSubtype);

				int textBytes = tlv.value.length - 4;
				String text;// = Utils.getEncodedString(textBytes);
				try {
					// text = new String(textBytes, "UTF-16");
					text = new String(tlv.value, 4, textBytes, encoding);
				} catch (UnsupportedEncodingException e) {
					text = new String(tlv.value, 4, textBytes);
				}

				message.text = text;
				service.log(message.senderId + " says " + text);

				if ((message.senderId.equals(service.getUn())) || text.length() > 0)
					notifyMessageReceived(message);
				break;
			}
		}

		private void channel2MessageTLVMap(TLV tlv, ICBMMessage message) {
			switch (tlv.type) {
			case 0x2711:
				if (message.capability.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_SRV_RELAY))) {
					channel2TextMessage(tlv.value, message, false);
				}
				if (message.capability.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_AIM_FILESEND))) {
					channel2FileMessage(tlv.value, message);
				}
				break;
			case 0xa:
				service.log("seq number " + tlv.value);
				break;
			case 0xf:
				service.log("host check " + ProtocolUtils.getHexString(tlv.value));
				break;
			case 0xd:
				service.log("mime " + new String(tlv.value));
				break;
			case 0xc:
				String text;
				try {
					text = new String(tlv.value, "UTF-16");
				} catch (UnsupportedEncodingException e) {
					text = new String(tlv.value);
				}
				message.invitation = ProtocolUtils.xmlFromParameter(text);

				service.log("invitation " + message.invitation);
				break;
			case 0x2:
				message.rvIp = ProtocolUtils.getIPString(tlv.value);
				service.log("rendezvouz ip " + message.rvIp);
				break;
			case 0x3:
				message.internalIp = ProtocolUtils.getIPString(tlv.value);
				service.log("internal ip " + message.internalIp);
				break;
			case 0x4:
				message.externalIp = ProtocolUtils.getIPString(tlv.value);
				service.log("external ip " + message.externalIp);
				break;
			case 0x5:
				message.externalPort = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(tlv.value));
				service.log("external port " + message.externalPort);
				break;
			case 0x10:
				service.log("connect FT proxy request");
				message.connectFTProxy = true;
				break;
			case 0x2712:
				service.log("kinda encoding " + new String(tlv.value));
				break;
			}
			if ((message.senderId.equals(service.getUn())) || (message.text != null && message.text.length() > 0 && message.capability.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_SRV_RELAY)))) {
				notifyMessageReceived(message);

				message.receiverId = message.senderId;
				// message.text = "";
				message.senderId = service.getUn();
				if ((service.getOnlineInfo().userStatus & ICQConstants.STATUS_INVISIBLE) < 1) {
					message.messageType = ICQConstants.MTYPE_PLAIN;
					sendChannel2PluginMessage(message);
				}
			}

		}

		@SuppressWarnings("unused")
		private void channel2FileMessage(byte[] in, ICBMMessage message) {

			if (in == null) {
				return;
			}

			int pos = 0;

			int type = ProtocolUtils.bytes2ShortBE(in, pos);
			pos += 2;
			int count = ProtocolUtils.bytes2ShortBE(in, pos);
			pos += 2;
			long filesize = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(in, pos));
			pos += 4;

			// message.messageType = ICQConstants.MTYPE_FILEREQ;

			int filenameBytes = in.length - 9;

			for (int i = 0; i < count; i++) {
				ICQFileInfo file = new ICQFileInfo();
				file.size = filesize;
				try {
					file.filename = new String(in, pos, filenameBytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					service.log(e);
					file.filename = new String(in, pos, filenameBytes);
				}
				message.files.add(file);
			}

		}

		@SuppressWarnings("unused")
		private void channel2TextMessage(byte[] data, ICBMMessage message, boolean isAck) {
			short block1Length = ProtocolUtils.bytes2ShortLE(data, 0);

			byte[] pluginGUID = new byte[16];
			System.arraycopy(data, 4, pluginGUID, 0, 16);
			String strGUID = ProtocolUtils.getHexString(pluginGUID);
			if (strGUID.equals(ICQConstants.GUID_RTF_TEXT)) {
				int pos = 2 + block1Length;
				short block2Length = ProtocolUtils.bytes2ShortLE(data, pos);
				pos = 4 + block1Length + block2Length;

				byte mType = data[pos];
				byte mFlag = data[pos + 1];

				short statusCode = ProtocolUtils.bytes2ShortLE(data, pos + 2);
				short priorityCode = ProtocolUtils.bytes2ShortLE(data, pos + 4);

				int textLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(data, pos + 6));
				byte[] textBytes = null;
				if (textLength > 1) {
					textBytes = new byte[textLength - 1];
					System.arraycopy(data, pos + 8, textBytes, 0, textLength - 1);
				}

				pos += (8 + textLength);

				String encoding = "windows-1251";
				switch (mType) {
				case ICQConstants.MTYPE_ACK:
				case ICQConstants.MTYPE_PLAIN:
					if (!message.senderId.equals(service.getUn())) {
						service.getServiceResponse().respond(ICQServiceResponse.RES_MESSAGEACK, message.senderId, ProtocolUtils.bytes2LongBE(message.messageId, 0), (byte)2);
					}

					int color = ProtocolUtils.bytes2IntLE(data, pos);
					pos += 4;
					int bgColor = ProtocolUtils.bytes2IntLE(data, pos);
					pos += 4;

					if (data.length >= pos + 4) {
						int guidLen = ProtocolUtils.bytes2IntLE(data, pos);
						pos += 4;
						if (guidLen == 38) {
							String guid = ProtocolUtils.getEncodedString(data, pos, guidLen);
							if (guid.equals(ICQConstants.GUID_UTF8)) {
								encoding = "UTF-8";
							}
							pos += guidLen;
						}
					}

					break;
				case ICQConstants.MTYPE_PLUGIN:
					parsePluginData(message, data, pos, message.senderId);
					break;
				}

				if (textBytes != null) {
					String text = ProtocolUtils.getEncodedString(textBytes, encoding);
					service.log(message.senderId + " says something " + text);
					message.text = text;
				} else {
					message.text = "";
				}

			}
		}

		private void parsePluginData(ICBMMessage message, byte[] data, int pos, String uid) {
			int headerLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(data, pos));
			byte[] pluginGuid = new byte[16];
			pos += 2;
			System.arraycopy(data, pos, pluginGuid, 0, 16);
			if (new String(pluginGuid).equals(new String(ICQConstants.GUID_XSTATUSMSG))) {
				pos += headerLength + 4;
				int dataLength = ProtocolUtils.bytes2IntLE(data, pos);
				pos += 4;
				byte[] xmlDataBytes = new byte[dataLength];
				System.arraycopy(data, pos, xmlDataBytes, 0, dataLength);

				String xmlData = ProtocolUtils.getEncodedString(xmlDataBytes);

				int i;
				if ((i = xmlData.indexOf("<NR><RES>")) < 0) {
					//service.log(message.senderId + " asks xstatus ");
					service.getServiceResponse().respond(ICQServiceResponse.RES_ACCOUNT_ACTIVITY, message.senderId + " asks xstatus ");
					if ((service.getOnlineInfo().userStatus & ICQConstants.STATUS_INVISIBLE) < 1){
						answerOwnXStatus(message, uid);
					}
					return;
				}
				;
				int j;
				if ((j = xmlData.indexOf("</RES></NR>")) < 0)
					return;
				String s2;
				if (((s2 = ProtocolUtils.xmlFromParameter(xmlData.substring(i + 9, j))).indexOf("<val srv_id='")) < 0)
					return;
				int j2;
				int k2;
				String xstatusName = "";
				if ((j2 = s2.indexOf("<title>")) > 0 && (k2 = s2.indexOf("/title>")) > 0) {
					xstatusName = s2.substring(j2 + 7, k2 - 1);
				}
				;
				int l2;
				int i3;
				String xstatusValue = "";
				if ((l2 = s2.indexOf("<desc>")) > 0 && (i3 = s2.indexOf("</desc>")) > 0) {
					xstatusValue = s2.substring(l2 + 6, i3);
				}
				;

				service.log(uid + " has xstatus: " + xstatusName + ": " + xstatusValue);

				ICQOnlineInfo info = service.getBuddyList().getByUin(uid);
				if (info != null) {
					info.personalText = xstatusName;
					info.extendedStatus = xstatusValue;

					service.getServiceResponse().respond(ICQServiceResponse.RES_BUDDYSTATECHANGED, info, true);
				}
			}
		}
	}

	public Flap getOfflineMessagesRequestFlap() {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_OFFLINE;
		data.requestId = ICQConstants.SNAC_MESSAGING_OFFLINE;

		flap.data = data;

		return flap;
	}

	private void answerOwnXStatus(ICBMMessage askMessage, String receiverUid) {
		ICBMMessage message = new ICBMMessage();
		message.messageId = askMessage.messageId;
		message.pluginSpecificData = getAnswerXStatusSuffix(service.getOnlineInfo().personalText, service.getOnlineInfo().extendedStatus);
		message.receiverId = receiverUid;
		message.text = "";
		message.messageType = ICQConstants.MTYPE_PLUGIN;

		sendChannel2PluginMessage(message);
	}

	protected void notifyMessageReceived(ICBMMessage message) {
		service.getServiceResponse().respond(ICQServiceResponse.RES_MESSAGE, message);
	}

	public void sendFileMessageReject(ICBMMessage message) {
		sendChannel2PluginMessage(message);
	}

	public void parseTyping(Snac snac) {
		if (snac.plainData == null || snac.plainData.length == 0) {
			return;
		}
		int pos = 10;
		byte screennameLength = snac.plainData[pos];
		pos++;
		String screenname = new String(snac.plainData, pos, screennameLength);
		pos += screennameLength;
		short type = ProtocolUtils.bytes2ShortBE(snac.plainData, pos);

		if (type != 0) {
			service.getServiceResponse().respond(ICQServiceResponse.RES_TYPING, screenname);
		}
	}

	public void sendTyping(String uin) {
		if (task != null && !task.isCancelled()) {
			task.cancel(false);
		}
		sendTyping(uin, false);
	}

	private void sendTyping(final String uin, boolean typingEnds) {
		short channel = 1;

		for (ICQOnlineInfo info : service.getBuddyList().buddyInfos) {
			if (info != null && info.capabilities != null && info.uin.equals(uin)) {
				for (String cap : info.capabilities) {
					if (cap.equals(ProtocolUtils.getHexString(ICQConstants.CLSID_ICQUTF))) {
						channel = 2;
						break;
					}
				}
				break;
			}
		}

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_TYPINGNOTIFICATION;
		data.requestId = ICQConstants.SNAC_MESSAGING_TYPINGNOTIFICATION;

		byte[] uinBytes = uin.getBytes();
		byte[] raw = new byte[13 + uinBytes.length];

		int pos = 0;

		System.arraycopy(ProtocolUtils.long2ByteBE(RANDOM.nextLong()), 0, raw, pos, 8);
		pos += 8;
		System.arraycopy(ProtocolUtils.short2ByteBE(channel), 0, raw, pos, 2);
		pos += 2;
		raw[pos] = (byte) uinBytes.length;
		pos++;
		System.arraycopy(uinBytes, 0, raw, pos, uinBytes.length);
		pos += uinBytes.length;

		raw[pos] = 0;
		pos++;
		if (typingEnds) {
			raw[pos] = 0;
		} else {
			raw[pos] = 2;
		}
		pos++;

		data.plainData = raw;
		flap.data = data;

		if (service.getRunnableService().sendToSocket(flap) && !typingEnds){
			task = executor.schedule(new Runnable() {

				@Override
				public void run() {
					sendTyping(uin, true);
				}

			}, 4, TimeUnit.SECONDS);
		}
		
	}
}
