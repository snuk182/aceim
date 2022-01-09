package aceim.protocol.snuk182.mrim;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ApiConstants;
import aceim.protocol.snuk182.mrim.inner.MrimConstants;
import aceim.protocol.snuk182.mrim.inner.MrimServiceInternal;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimBuddy;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimFileTransfer;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimGroup;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimIncomingFile;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimMessage;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimOnlineInfo;
import aceim.protocol.snuk182.mrim.utils.ProtocolUtils;
import android.annotation.SuppressLint;
import android.content.Context;


public final class MrimEntityAdapter {

	public static final String[] xstatuses = { "status_4", "status_5", "status_6", "status_7", "status_8", "status_9", "status_10", "status_11", "status_12", "status_13", "status_14", "status_15", "status_16", "status_18", "status_19", "status_20", "status_21", "status_22", "status_23", "status_24", "status_26", "status_27", "status_28", "status_51", "status_52", "status_46", "status_48", "status_47" };

	static ConnectionState mrimConnectionState2ConnectionState(short currentState) {
		switch (currentState) {
		case MrimServiceInternal.STATE_DISCONNECTED:
			return ConnectionState.DISCONNECTED;
		case MrimServiceInternal.STATE_CONNECTED:
			return ConnectionState.CONNECTED;
		default:
			return ConnectionState.CONNECTING;
		}
	}
	
	public static final int userStatus2MrimUserStatus(Byte status) {
		switch (status) {
		case MrimApiConstants.STATUS_OFFLINE:
			return MrimConstants.STATUS_OFFLINE;
		case MrimApiConstants.STATUS_AWAY:
			return MrimConstants.STATUS_AWAY;
		case MrimApiConstants.STATUS_INVISIBLE:
			return MrimConstants.STATUS_OFFLINE | MrimConstants.STATUS_FLAG_INVISIBLE;
		case MrimApiConstants.STATUS_OTHER:
		case MrimApiConstants.STATUS_FREE4CHAT:
			return MrimConstants.STATUS_OTHER;
		default:
			return MrimConstants.STATUS_ONLINE;
		}
	}
	
	public static final String userXStatus2MrimXStatus(byte xstatus){
		try {
			return xstatuses[xstatus];
		} catch (Exception e) {
			return "";
		}
	}

	public static final int skipFormatted(byte[] dump, String format, int pos, int processed) {
		int i = pos;
		while (processed < format.length()) {
			if (format.charAt(processed) == 's') {
				int strLen = (int) ul2Long(dump, pos);
				pos += 4 + strLen;
			} else if (format.charAt(processed) == 'z') {
				while (dump[pos] != 0) {
					pos++;
				}
			} else {
				pos += 4;
			}
			processed++;
		}

		return pos - i;
	}

	public static final byte[] string2lpsa(String string) {
		if (string == null || string.length() < 1) {
			return ProtocolUtils.int2ByteLE(0);
		}

		byte[] strBytes;
		try {
			strBytes = string.getBytes("windows-1251");
		} catch (UnsupportedEncodingException e) {
			strBytes = string.getBytes();
		}

		byte[] out = new byte[4 + strBytes.length];
		System.arraycopy(ProtocolUtils.int2ByteLE(strBytes.length), 0, out, 0, 4);
		System.arraycopy(strBytes, 0, out, 4, strBytes.length);

		return out;
	}

	public static final byte[] string2lpsw(String string) {
		if (string == null || string.length() < 1) {
			return ProtocolUtils.int2ByteLE(0);
		}

		byte[] strBytes;
		try {
			strBytes = string.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			strBytes = string.getBytes();
		}

		byte[] out = new byte[4 + strBytes.length];
		System.arraycopy(ProtocolUtils.int2ByteLE(strBytes.length), 0, out, 0, 4);
		System.arraycopy(strBytes, 0, out, 4, strBytes.length);

		return out;
	}

	public static final String lpsa2String(byte[] dump, int pos) {
		int len = ProtocolUtils.bytes2IntLE(dump, pos);
		pos += 4;
		if (len < 1) {
			return "";
		}
		String str;
		try {
			str = new String(dump, pos, len, "windows-1251");
		} catch (UnsupportedEncodingException e) {
			str = new String(dump, pos, len);
		} catch (Exception ee){
			byte[] dummy = new byte[len];
			Arrays.fill(dummy, (byte) ' ');
			try {
				str = new String(dummy, "windows-1251");
			} catch (UnsupportedEncodingException e) {
				str = new String(dummy);
			}
		}
		return str;
	}

	public static final long ul2Long(byte[] dump, int pos) {
		return ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntLE(dump, pos));
	}
	
	public static final byte[] spacedHexString2Bytes(String str){
		String[] strings = str.split(" ");
		byte[] array = new byte[strings.length];
		Arrays.fill(array, (byte) 0);
		for (int i=0; i<strings.length; i++){
			String hex = strings[i];
			array[i] = (byte) Integer.parseInt(hex, 16);
		}
		
		return array;
	}

	public static final String lpsw2String(byte[] dump, int pos) {
		int len = ProtocolUtils.bytes2IntLE(dump, pos);
		pos += 4;
		if (len < 1) {
			return "";
		}
		if ((len % 2) > 0){
			len--;
		}
		String str;
		try {
			str = new String(dump, pos, len, "UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			str = new String(dump, pos, len);
		} catch (Exception ee){
			byte[] dummy = new byte[len];
			Arrays.fill(dummy, (byte) ' ');
			try {
				str = new String(dummy, "UTF-16LE");
			} catch (UnsupportedEncodingException e) {
				str = new String(dummy);
			}
		}
		return str;
	}

	private static String long2hex(long val) {
		String result = Long.toString(val, 16);
		while (result.length() < 8) {
			result = "0" + result;
		}
		return result;
	}

	public static final Buddy mrimBuddy2Buddy(String serviceName, MrimBuddy in, String ownerUid, byte serviceId) {
		Buddy out = new Buddy(in.uin, ownerUid, serviceName, serviceId);
		out.setId(in.id);
		out.setName(in.name);
		out.setGroupId(Integer.toString(in.groupId));
		out.getOnlineInfo().setXstatusName(in.onlineInfo.xstatusName);
		out.getOnlineInfo().setXstatusDescription(in.onlineInfo.xstatusText);
		//out.visibility = mrimVisibility2Visibility(in);
		
		out.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_STATUS, mrimUserStatus2UserStatus(in.onlineInfo.status, in.onlineInfo.xstatusId));
		out.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_XSTATUS, mrimXStatus2XStatus(in.onlineInfo.xstatusId));
		
		if (in.onlineInfo.status != MrimConstants.STATUS_OFFLINE) {
			out.getOnlineInfo().getFeatures().putBoolean(ApiConstants.FEATURE_FILE_TRANSFER, true);
		}

		return out;
	}

	public static final BuddyGroup mrimBuddyGroup2BuddyGroup(String serviceName, MrimGroup in, String ownerUid, byte serviceId, List<MrimBuddy> buddies) {
		BuddyGroup group = new BuddyGroup(Integer.toString(in.groupId), ownerUid, serviceId);
		group.setName(in.name);

		for (MrimBuddy buddy : buddies) {
			if (buddy.groupId == in.groupId) {
				group.getBuddyList().add(mrimBuddy2Buddy(serviceName, buddy, ownerUid, serviceId));
			}
		}

		return group;
	}

	public static final List<BuddyGroup> mrimBuddyGroupList2BuddyGroupList(String serviceName, List<MrimGroup> groupList, String ownerId, byte serviceId, List<MrimBuddy> buddies) {
		List<BuddyGroup> groups = new ArrayList<BuddyGroup>(groupList.size());
		for (MrimGroup group : groupList) {
			groups.add(mrimBuddyGroup2BuddyGroup(serviceName, group, ownerId, serviceId, buddies));
		}

		return groups;
	}

	public static final List<Buddy> mrimBuddyList2Buddylist(String serviceName, List<MrimBuddy> buddyList, String ownerUid, byte serviceId) {
		List<Buddy> buddies = new ArrayList<Buddy>();
		for (MrimBuddy buddy : buddyList) {
			buddies.add(mrimBuddy2Buddy(serviceName, buddy, ownerUid, serviceId));
		}
		return buddies;
	}

	/*private static final byte mrimVisibility2Visibility(MrimBuddy in) {
		if ((in.flags & MrimConstants.CONTACT_INTFLAG_NOT_AUTHORIZED) != 0) {
			return Buddy.VIS_NOT_AUTHORIZED;
		}

		return Buddy.VIS_REGULAR;
	}*/

	private static final byte mrimXStatus2XStatus(String xstatusId) {
		for (byte i=0; i<xstatuses.length; i++){
			if (xstatuses[i].equalsIgnoreCase(xstatusId)){
				return i;
			}
		}
		return -1;
	}

	private static final byte mrimUserStatus2UserStatus(int status, String xstatusName) {
		if ((status & MrimConstants.STATUS_FLAG_INVISIBLE) != 0) {
			return MrimApiConstants.STATUS_INVISIBLE;
		}
		
		if (xstatusName.equalsIgnoreCase("status_chat")){
			return MrimApiConstants.STATUS_FREE4CHAT;
		}

		switch (status) {
		case MrimConstants.STATUS_OFFLINE:
			return MrimApiConstants.STATUS_OFFLINE;
		case MrimConstants.STATUS_AWAY:
			return MrimApiConstants.STATUS_AWAY;
		case MrimConstants.STATUS_UNDETERMINATED:
			return MrimApiConstants.STATUS_OTHER;
		default:
			return MrimApiConstants.STATUS_ONLINE;
		}
	}

	@SuppressLint("DefaultLocale")
	public static final String getHexLong(byte[] dump, int pos) {
		long x1 = ul2Long(dump, pos) & 0xFFFFFFFFL;
		long x2 = ul2Long(dump, pos) & 0xFFFFFFFFL;
		return (long2hex(x2) + long2hex(x1)).toUpperCase();
	}

	public static TextMessage mrimMessage2TextMessage(MrimMessage msg, byte serviceId) {
		if (msg == null)
			return null;
		TextMessage txtMsg = new TextMessage(serviceId, msg.from);
		txtMsg.setText(msg.text);
		txtMsg.setTime(System.currentTimeMillis());
		txtMsg.setMessageId(msg.messageId);
		txtMsg.setIncoming(true);
		return txtMsg;
	}

	public static MrimMessage textMessage2MrimMessage(String myId, TextMessage textMessage) {
		if (textMessage == null) {
			return null;
		}
		MrimMessage msg = new MrimMessage();
		msg.from = myId;
		msg.text = textMessage.getText();
		msg.messageId = (int) textMessage.getMessageId();
		msg.to = textMessage.getContactUid();

		return msg;
	}

	public static OnlineInfo mrimOnlineInfo2OnlineInfo(MrimOnlineInfo in, byte serviceId) {
		if (in == null) {
			return null;
		}
		OnlineInfo out = new OnlineInfo(serviceId, in.uin);
		out.setXstatusName(in.xstatusName);
		out.setXstatusDescription(in.xstatusText);
		
		out.getFeatures().putByte(ApiConstants.FEATURE_STATUS, mrimUserStatus2UserStatus(in.status, in.xstatusId));
		out.getFeatures().putByte(ApiConstants.FEATURE_XSTATUS, mrimXStatus2XStatus(in.xstatusId));
		
		
		if (in.status != MrimConstants.STATUS_OFFLINE) {
			out.getFeatures().putBoolean(ApiConstants.FEATURE_FILE_TRANSFER, true);
		}
		
		return out;
	}

	public static final FileMessage mrimFileTransferMessage2FileMessage(MrimFileTransfer mrimFileTransfer, byte serviceId) {
		List<FileInfo> files = new ArrayList<FileInfo>(mrimFileTransfer.incomingFiles.size());
		for (MrimIncomingFile file: mrimFileTransfer.incomingFiles){
			FileInfo info = new FileInfo(serviceId);
			info.setFilename(file.filename);
			info.setSize(file.filesize);
			files.add(info);
		}
		
		FileMessage fm = new FileMessage(serviceId, mrimFileTransfer.buddyMrid, files);
		fm.setMessageId(mrimFileTransfer.messageId);
		
		return fm;
	}

	public static Message authRequestToServiceMessage(byte serviceId, String from, String reasonText, Context context) {
		ServiceMessage message = new ServiceMessage(serviceId, from, true);
		message.setText(reasonText);
		message.setContactDetail(context.getString(R.string.ask_authorization));
		return message;
	}

	public static MessageAckState mrimMessageAck2MessageAck(Byte ack) {
		switch (ack) {
		case 2:
			return MessageAckState.RECIPIENT_ACK;
		default:
			return MessageAckState.SERVER_ACK;
		}
	}

	public static List<File> getFilesFromFileMessage(FileMessage message) {
		if (message == null) {
			return Collections.emptyList();
		}
		
		List<File> files = new ArrayList<File>(message.getFiles().size());
		
		for (FileInfo i : message.getFiles()) {
			files.add(new File(i.getFilename()));
		}
		
		return files;
	}
}
