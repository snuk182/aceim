package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import aceim.api.utils.Logger;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMMessage;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;
import android.os.Environment;

/*
 * it's strongly recommended not even breathe here, unless you're absolutely absolutely sure about what you gonna do...
 */
public class FileTransferEngine {
	
	private static final int SERVER_SOCKET_TIMEOUT = 600000;

	private static final String proxyUrl = "ars.oscar.aol.com";

	private final Map<Long, FileRunnableService> activeTransfers = new HashMap<Long, FileRunnableService>();

	private final ICQServiceInternal service;
	private final List<ICBMMessage> ftMessages = new ArrayList<ICBMMessage>();
	
	private List<NotificationData> notifications = new LinkedList<NotificationData>();

	public FileTransferEngine(ICQServiceInternal service) {
		this.service = service;
	}

	public List<ICBMMessage> getMessages() {
		return ftMessages;
	}

	public void fileReceiveResponse(long messageId, Boolean accept) {
		ICBMMessage message = findMessageByMessageId(messageId);
		if (message == null) {
			service.log("ft: no message");
			return;
		}
		if (!message.senderId.equals(service.getUn())){
			message.receiverId = message.senderId;
			message.senderId = service.getUn();
		}
		message.rvMessageType = (short) (accept ? 2 : 1);
		if (!accept) {
			service.log("ft: reject from "+message.receiverId);
			service.getMessagingEngine().sendFileMessageReject(message);
		} else {
			service.log("ft: accept from "+message.receiverId);
			acceptFile(message);
		}
	}

	private void acceptFile(ICBMMessage message) {
		connectPeer(message, null, true);
	}

	private void createPeer(ICBMMessage message, List<File> files) throws IOException {
		service.log("ft: creating own peer");
		FileRunnableService frs = activeTransfers.get(ProtocolUtils.bytes2LongBE(message.messageId, 0));
		
		if (frs == null){
			service.log("ft: new runnable for "+message.receiverId);
			frs = new FileRunnableService(FileRunnableService.TARGET_PEER, message, files);
			frs.connectionState = FileRunnableService.CONNSTATE_FILE_HEADER;
			activeTransfers.put(ProtocolUtils.bytes2LongBE(message.messageId, 0), frs);
		} else {
			service.log("ft: existing runnable for "+message.receiverId);
			frs.message = message;
			frs.target = FileRunnableService.TARGET_PEER;
		}
		frs.connectionState = FileRunnableService.CONNSTATE_FILE_HEADER;
		
		frs.server = createLocalSocket(frs);
		message.externalPort = frs.server.getLocalPort();
		message.rvIp = ProtocolUtils.getIPString(service.getInternalIp());
		message.rvMessageType = 0;
		sendFileTransferRequest(message, files);
	}

	private void connectPeer(ICBMMessage message, FileRunnableService runnable, boolean incoming) {
		service.log("connecting peer "+message.rvIp+":"+message.externalPort+"//sender "+message.senderId+"//receiver "+message.receiverId);
		
		Socket socket;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(InetAddress.getByAddress(ProtocolUtils.ipString2ByteBE(message.rvIp)), message.externalPort), 7000);
		} catch (UnknownHostException e) {
			service.log(e);
			socket = null;
		} catch (IOException e) {
			service.log(e);
			socket = null;
		}

		if (socket != null && socket.isConnected()) {
			service.log("ft: direct socket connected for "+message.receiverId);
			if (runnable == null){
				service.log("ft: new runnable for "+message.receiverId);
				runnable = new FileRunnableService(socket, FileRunnableService.TARGET_PEER, message);
				runnable.connectionState = FileRunnableService.CONNSTATE_FILE_HEADER;
				activeTransfers.put(ProtocolUtils.bytes2LongBE(message.messageId, 0), runnable);
			} else {
				service.log("ft: existing runnable for "+message.receiverId);
				if (runnable.server != null){
					try {
						runnable.server.close();
						runnable.server = null;
					} catch (IOException e) {
						service.log(e);
					}
				}
				runnable.socket = socket;
				runnable.connectionState = FileRunnableService.CONNSTATE_FILE_HEADER;
				runnable.target = FileRunnableService.TARGET_PEER;
			}
			runnable.start();
			if (!message.senderId.equals(service.getUn())){
				message.receiverId = message.senderId;
			}
			service.getRunnableService().sendToSocket(getAcceptMessage(message));
		} else {
			service.log("ft: no direct connection");
			if (incoming/* && checkForClientsDCCapability(message.receiverId)*/){
				service.log("ft: creating socket for "+message.receiverId);
				try {
					createPeer(message, null);
				} catch (IOException e) {
					service.log(e);
					connectProxy(message, runnable);
				}
			} else {
				connectProxy(message, runnable);
			}
		}
	}

	@SuppressWarnings("unused")
	private boolean checkForClientsDCCapability(String receiverId) {
		String dcCap = ProtocolUtils.getHexString(ICQConstants.CLSID_DIRECT);
		for (ICQOnlineInfo info: service.getBuddyList().buddyInfos){
			if (info.uin.equals(receiverId) && info.capabilities!=null){
				for (String cap: info.capabilities){
					if (dcCap.equals(cap)){
						return true;
					}
				}
			}
		}
		return false;
	}

	private void connectProxy(ICBMMessage message, FileRunnableService runnable) {
		Socket socket;
		try {
			if (message.connectFTProxy){
				service.log("connecting proxy "+message.rvIp+":"+service.getLoginPort());				
				socket = new Socket(message.rvIp, service.getLoginPort());
			} else {
				service.log("creating proxy call");				
				socket = new Socket(proxyUrl, service.getLoginPort());
			}
		} catch (UnknownHostException e) {
			service.log(e);
			socket = null;
		} catch (IOException e) {
			service.log(e);
			socket = null;
		}

		if (socket != null && socket.isConnected()) {
			if (runnable == null) {
				runnable = new FileRunnableService(socket, FileRunnableService.TARGET_PROXY, message);
				activeTransfers.put(ProtocolUtils.bytes2LongBE(message.messageId, 0), runnable);
			} else {
				if (runnable.server != null){
					try {
						runnable.server.close();
						runnable.server = null;
					} catch (IOException e) {
						service.log(e);
					}
				}
				runnable.socket = socket;
				runnable.connectionState = FileRunnableService.CONNSTATE_HANDSHAKE;
				runnable.target = FileRunnableService.TARGET_PROXY;
				runnable.message = message;
			}
			runnable.start();
		} else {
			transferFailed(new IOException("Cannot connect"), "", message, message.senderId);
		}
	}

	public ICBMMessage findMessageByMessageId(long messageId) {
		for (ICBMMessage msg : ftMessages) {
			if (ProtocolUtils.bytes2LongBE(msg.messageId, 0) == messageId) {
				return msg;
			}
		}
		return null;
	}
	
	public ICBMMessage findMessageByMessageId(byte[] messageId) {
		for (ICBMMessage msg : ftMessages) {
			if (Arrays.equals(messageId, msg.messageId)) {
				return msg;
			}
		}
		return null;
	}
	
	public void removeMessageByMessageId(long messageId) {
		for (int i=ftMessages.size()-1; i>=0; i--) {
			ICBMMessage msg = ftMessages.get(i);
			if (ProtocolUtils.bytes2LongBE(msg.messageId, 0) == messageId) {
				ftMessages.remove(i);
				break;
			}
		}
	}

	class FileRunnableService extends Thread {
		public static final int CONNSTATE_CONNECTED = 0;
		public static final int CONNSTATE_HANDSHAKE = 1;
		public static final int CONNSTATE_FILE_HEADER = 2;
		public static final int CONNSTATE_FILE_BODY = 3;
		public static final int CONNSTATE_FILE_SENT = 4;
		public static final int CONNSTATE_DISCONNECTED = 5;

		public static final int TARGET_PEER = 0;
		public static final int TARGET_PROXY = 1;

		ServerSocket server = null;
		Socket socket;
		int connectionState = CONNSTATE_CONNECTED;
		int target;
		ICBMMessage message;
		String participantUid = null;
		List<byte[]> blobs = new LinkedList<byte[]>();

		List<File> files = null;

		long currentFileSizeLeft = 0;
		long currentFileSize = 0;
		byte[] currentFileInfo = null;
		int totalFiles = 1;
		private ExtendedBufferedOutputStream currentFileStream;
		private String currentFileName;

		byte[] buffer = null;

		public FileRunnableService(Socket socket, int target, ICBMMessage message) {
			this(socket, target, message, null);
		}

		public FileRunnableService(int target, ICBMMessage message, List<File> files) {
			this(null, target, message, files);
		}

		public FileRunnableService(Socket socket, int target, ICBMMessage message, List<File> files) {
			this.socket = socket;
			this.target = target;
			this.message = message;
			this.files = files;
			if (files != null) {
				totalFiles = files.size();
				currentFileSize = 0;
				for (File f : files) {
					currentFileSize += f.length();
				}
			}
			
			if (message.senderId !=null && !message.senderId.equals(service.getUn())){
				participantUid = message.senderId;
			} else {
				participantUid = message.receiverId;
			}
			
			setName("File transfer " + message.senderId);
		}

		@Override
		public void run() {
			if (socket == null) {
				return;
			}
			
			if (message.receiverId.equals(service.getUn())){
				message.receiverId = message.senderId;
				message.senderId = service.getUn();
			}
			if (target == TARGET_PROXY) {				
				sendHandshake();
			} else if (files != null && files.size()>0 && message.connectFTPeer) {
                connectionState = CONNSTATE_FILE_HEADER;
                fireTransfer();
            } 
			/*else {
				if (files != null) {
					sendFileInfo(files.get(0));
				} 
			}*/

			getDataFromSocket();
		}

		private void sendFileInfo(File file) {
			byte[] infoBlob;
			
			byte[] filenameBytes = file.getName().getBytes();
			String encoding = null;/*"UTF-16BE";
			try {
				filenameBytes = file.getName().getBytes(encoding);				
			} catch (UnsupportedEncodingException e) {
				filenameBytes = file.getName().getBytes();
				encoding = "UTF-8";
			}*/
			
			if (filenameBytes.length + 194 > 256){
				infoBlob = new byte[filenameBytes.length + 194];
			} else {
				infoBlob = new byte[256];
			}
			
			Arrays.fill(infoBlob, (byte) 0);
			
			infoBlob[0] = 0x4f; // file header
			infoBlob[1] = 0x46;
			infoBlob[2] = 0x54;
			infoBlob[3] = 0x32;
			
			int pos = 4;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) infoBlob.length), 0, infoBlob, pos, 2);
			
			pos+=2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) 0x101), 0, infoBlob, pos, 2); //stage
			pos+=2;
			System.arraycopy(message.messageId, 0, infoBlob, pos, 8); //cookie;
			pos+=8;
			System.arraycopy(new byte[]{0,0}, 0, infoBlob, pos, 2); //encryption
			pos+=2;
			System.arraycopy(new byte[]{0,0}, 0, infoBlob, pos, 2); //compression
			pos+=2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) totalFiles), 0, infoBlob, pos, 2); //total files
			pos+=2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) files.size()), 0, infoBlob, pos, 2); // files left
			pos+=2;
			System.arraycopy(new byte[]{0,1}, 0, infoBlob, pos, 2); //dunno
			pos+=2;
			System.arraycopy(new byte[]{0,1}, 0, infoBlob, pos, 2); //dunno
			pos+=2;
			
			System.arraycopy(ProtocolUtils.int2ByteBE((int) currentFileSize), 0, infoBlob, pos, 4);
			pos+=4;
			System.arraycopy(ProtocolUtils.int2ByteBE((int) file.length()), 0, infoBlob, pos, 4);
			pos+=4;
			System.arraycopy(ProtocolUtils.int2ByteBE((int) file.lastModified()/1000), 0, infoBlob, pos, 4);
			pos+=4;
			try {
				System.arraycopy(ProtocolUtils.int2ByteBE((int) getChecksum(file)), 0, infoBlob, pos, 4);
			} catch (IOException e) {
				service.log(e);
				System.arraycopy(ProtocolUtils.int2ByteBE( 0), 0, infoBlob, pos, 4);
			} 
			pos+=4;
			
			System.arraycopy(new byte[]{(byte) 0xff,(byte) 0xff,0,0}, 0, infoBlob, pos, 4); //dunno what's that
			pos+=4;
			System.arraycopy(new byte[]{0,0,0,0}, 0, infoBlob, pos, 4);
			pos+=4;
			System.arraycopy(new byte[]{0,0,0,0}, 0, infoBlob, pos, 4);
			pos+=4;
			System.arraycopy(new byte[]{(byte) 0xff,(byte) 0xff,0,0}, 0, infoBlob, pos, 4);
			pos+=4;
			System.arraycopy(new byte[]{0,0,0,0}, 0, infoBlob, pos, 4);
			pos+=4;
			System.arraycopy(new byte[]{(byte) 0xff,(byte) 0xff,0,0}, 0, infoBlob, pos, 4);
			pos+=4;
			
			byte[] id = new String("Cool FileXfer").getBytes();
			System.arraycopy(id, 0, infoBlob, pos, id.length);			
			pos+=32;
			
			infoBlob[pos] = 0x20;
			pos++;
			infoBlob[pos] = 0x1c;
			pos++;
			infoBlob[pos] = 0x11;
			pos++;
			
			pos+=69; //dummy?
			
			pos+=16; //mac file info?
			
			//if (encoding.equals("UTF-16BE")) {
			//	System.arraycopy(new byte[]{0,2}, 0, infoBlob, pos, 2); //encoding
			//} else {
				System.arraycopy(new byte[]{0,0}, 0, infoBlob, pos, 2); //encoding
			//}
			pos+=2;
			System.arraycopy(new byte[]{0,0}, 0, infoBlob, pos, 2); //encoding subcode
			pos+=2;
			System.arraycopy(filenameBytes, 0, infoBlob, pos, filenameBytes.length);
			
			sendToSocket(infoBlob);
		}

		private void sendHandshake() {
			sendToSocket(getHandshakeData(message));
		}

		private boolean getDataFromSocket() {
			byte[] tail = null;
			int read = 0;
			int tailLength = 0;

			while (connectionState != CONNSTATE_DISCONNECTED && socket != null && socket.isConnected() && !socket.isClosed()) {
				InputStream is;
				try {
					is = socket.getInputStream();

					if (is.available() < 1) {
						Thread.sleep(300);
					} else {
						Thread.sleep(500);

						if (tail == null) {

							byte[] lengthBytes;

							switch (connectionState) {
							case CONNSTATE_CONNECTED:
							case CONNSTATE_HANDSHAKE:
								lengthBytes = new byte[2];
								is.read(lengthBytes, 0, 2);
								tailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(lengthBytes));
								break;
							case CONNSTATE_FILE_HEADER:
								byte[] fileHdrMark = new byte[4];
								is.read(fileHdrMark, 0, 4);
								if (fileHdrMark[0] == 0x4f // file header
										&& fileHdrMark[1] == 0x46
										&& fileHdrMark[2] == 0x54
										&& fileHdrMark[3] == 0x32) {
									lengthBytes = new byte[2];
									is.read(lengthBytes, 0, 2);
									tailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(lengthBytes)) - 6;
								}
								break;
							case CONNSTATE_FILE_BODY:
								if (buffer == null) {
									buffer = new byte[88000];
								}

								read = is.read(buffer, 0, buffer.length);
								service.log("read " + read+"| bytes left " + currentFileSizeLeft);
								currentFileSizeLeft -= read;
								fileData(buffer, read, currentFileSizeLeft);

								if (currentFileSizeLeft < 1) {
									sendFileAck();
									connectionState = CONNSTATE_FILE_HEADER;
									totalFiles--;
									buffer = null;
									if (totalFiles < 1) {
										cleanup();
									}
								}
								/*
								 * if (buffer.length >= currentFileSizeLeft){
								 * //read = is.read(buffer, 0, buffer.length);
								 * //buffer = new byte[(int)
								 * currentFileSizeLeft]; read = is.read(buffer,
								 * 0, buffer.length); service.log("read "+read);
								 * 
								 * currentFileSizeLeft-=read; fileData(buffer,
								 * read, currentFileSizeLeft);
								 * 
								 * if (currentFileSizeLeft < 1){ sendFileAck();
								 * connectionState = CONNSTATE_FILE_HEADER;
								 * totalFiles--; buffer = null; if (totalFiles <
								 * 1){ ftMessages.remove(message);
								 * activeTransfers.remove(message.messageId);
								 * socket.close(); } }
								 * 
								 * } else { read = is.read(buffer, 0,
								 * buffer.length); service.log("read "+read);
								 * currentFileSizeLeft-=read; fileData(buffer,
								 * read, currentFileSizeLeft); }
								 */

								continue;
							}

							read = 0;
							tail = new byte[tailLength];
							read += is.read(tail, 0, tailLength);
							service.log("-- FT Got " + ProtocolUtils.getSpacedHexString(tail));
							if (read < tailLength) {
								continue;
							}
						} else {
							read += is.read(tail, 6 + read, tailLength - read);
							if (read < tailLength) {
								continue;
							}
						}

						try {
							blobs.add(tail);
						} catch (Exception e) {
							service.log(e);
						}
						new Thread("File transfer processor") {
							@Override
							public void run() {
								try {
									forceBlobProcess();
								} catch (Exception e) {
									service.log(e);
								}
							}
						}.start();
						tail = null;
					}
				} catch (IOException e) {
					service.log(e);
				} catch (InterruptedException e) {
					service.log(e);
				}
			}
			cleanup();
			return false;
		}

		private void sendFileAck() {
			if (currentFileInfo == null) {
				return;
			}

			byte[] out = getFileInfoByteBlock(currentFileInfo, (short) 0x204);
			sendToSocket(out);
		}

		public synchronized boolean sendToSocket(byte[] out) {
			try {
				OutputStream os = socket.getOutputStream();

				service.log("-- FT To be sent " + ProtocolUtils.getSpacedHexString(out));
				os.write(out);

			} catch (IOException e) {
				connectionState = CONNSTATE_DISCONNECTED;
				service.log(e);
			}
			return true;
		}

		protected void forceBlobProcess() throws Exception {
			synchronized (blobs) {
				while (blobs.size() > 0) {
					byte[] blob = blobs.remove(0);
					process(blob);
				}
			}
		}

		private void process(byte[] blob) {
			switch (connectionState) {
			case CONNSTATE_CONNECTED:
			case CONNSTATE_HANDSHAKE:
				parseRendezvous(blob);
				break;
			case CONNSTATE_FILE_HEADER:
				service.log("got header");
				currentFileInfo = blob;
				parseFileInfoBlob(blob);
				break;
			case CONNSTATE_FILE_BODY:
				// fileData(blob);
				break;
			}
		}

		public void parseRendezvous(byte[] blob) {
			switch (blob[3]) {
			case 0x3:
				connectionState = CONNSTATE_HANDSHAKE;
				int portId = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(blob, 10));
				String rvIp = ProtocolUtils.getIPString(blob, 12);
				message.rvMessageType = 0;
				message.receiverId = participantUid;
				//if (files == null){
					service.getRunnableService().sendToSocket(getRedirectToProxyMessage(rvIp, portId, message, 2));
				/*} else {
					service.getRunnableService().sendToSocket(getAcceptMessage(message));
				}*/
				break;
			case 0x4:
				//sendFileInfo(files.get(0));
				break;
			case 0x5:
				connectionState = CONNSTATE_FILE_HEADER;
				if (files != null){
					message.receiverId = participantUid;
					service.getRunnableService().sendToSocket(getAcceptMessage(message));
					fireTransfer();
				}
				break;
			}
		}

		private void parseFileInfoBlob(byte[] blob) {
			// assume we have no 6 bytes of header in a blob
			int pos = 0;
			short stage = ProtocolUtils.bytes2ShortBE(blob, pos);
			pos += 2;
			pos += 8; // skip msg cookie
			//short encryption = ProtocolUtils.bytes2ShortBE(blob, pos);
			pos += 2;
			//short compression = ProtocolUtils.bytes2ShortBE(blob, pos);
			pos += 2;

			int filesCount = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(blob, pos));
			pos += 2;
			//int filesLeft = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(blob, pos));
			pos += 2;
			//int partsCount = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(blob, pos));
			pos += 2;

			pos += 2; // dunno what's there

			//long totalFilesize = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(blob, pos));
			pos += 4;
			long thisFileSize = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(blob, pos));
			pos += 4;
			long modTime = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(blob, pos))*1000;
			pos += 4;
			//long checksum = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(blob, pos));
			pos += 4;

			pos += 16; // dunno what's there ^^

			long thisFileSizeSent = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(blob, pos));
			pos += 4;
			@SuppressWarnings("unused")
			long checksumSent = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(blob, pos));
			pos += 4;
			
			pos += 32; // skip ID

			//byte flags = blob[pos];
			pos++;

			pos += 2; // skip some offsets

			pos += 69; // skip dummy
			pos += 16; // skip mac file info

			short charsetType = ProtocolUtils.bytes2ShortBE(blob, pos);
			pos += 2;
			short charsetSubcode = ProtocolUtils.bytes2ShortBE(blob, pos);
			pos += 2;
			
			service.log("char code "+charsetType+"|char subcode "+charsetSubcode);

			int filenamePos = pos;
			
			for (int i=blob.length-2; i>pos; i--){
				
				if (blob[i] != 0){
					pos = i+1;
					break;
				}
			}

			int filemaneSize = pos - filenamePos;
			byte[] filenameBytes = new byte[filemaneSize];
			System.arraycopy(blob, filenamePos, filenameBytes, 0, filemaneSize);

			String encoding;

			switch (charsetType) {
			case 0:
				encoding = "UTF-8";
				break;
			case 2:
				encoding = "UTF-16";
				break;
			default:
				encoding = "windows-1251";
				break;
			}

			String filename;
			try {
				filename = new String(filenameBytes, encoding);
			} catch (UnsupportedEncodingException e) {
				filename = new String(filenameBytes);
			}

			if (files == null){
				totalFiles = filesCount;
				service.log("file info " + filename + " sized " + thisFileSize);
				currentFileSizeLeft = thisFileSize;
				currentFileSize = thisFileSize;

				currentFileStream = createFile(filename, thisFileSize, modTime, message, participantUid);
				currentFileName = filename;

				byte[] out = getFileInfoByteBlock(blob, (short) 0x202);

				sendToSocket(out);
				connectionState = CONNSTATE_FILE_BODY;
			} else {
				switch(stage){
				case 0x202:
					connectionState = CONNSTATE_FILE_BODY;
					sendFileToSocket(files.get(0));
					break;
				case 0x204:
					files.remove(0);
					if (files.size()<1){
						cleanup();
					} else {
						sendFileInfo(files.get(0));
					}
					break;
				case 0x205:
					byte[] out2 = getFileInfoByteBlock(blob, (short) 0x106);
					sendToSocket(out2);
					break;
				case 0x106:
					break;
				case 0x207:
					connectionState = CONNSTATE_FILE_BODY;
					sendFileToSocket(files.get(0), thisFileSizeSent);
					break;
				}
			}			
		}
		
		private void sendFileToSocket(final File file){
			sendFileToSocket(file, 0);
		}

		private void sendFileToSocket(final File file, long startFrom) {
			OutputStream os;
			try {
				os = socket.getOutputStream();
			} catch (IOException e) {
				service.log(e);
				transferFailed(e, file.getAbsolutePath(), message, participantUid);
				cleanup();
				return;
			}
			long length = file.length();
			if (length > 8000){
				buffer = new byte[8000];
			} else {
				buffer = new byte[(int) length];
			}
			
			currentFileSizeLeft = 0;
			int read = 0;
			service.log("sending "+file.getName()+" to "+participantUid);
			
			BufferedInputStream bis = null;
			try {
				FileInputStream fis = new FileInputStream(file);
				if (startFrom > 0){
					fis.skip(startFrom);
					currentFileSizeLeft += startFrom;
				}
				bis = new BufferedInputStream(fis, 8000);
				while(currentFileSizeLeft < length){
					read = bis.read(buffer, 0, buffer.length);
					if (read < 0){
						break;
					}
					os.write(buffer, 0, read);
				    os.flush();
				    currentFileSizeLeft += read;
				    service.log("sent "+currentFileSizeLeft+" bytes");
				    
				    sendNotification(message.messageId, file.getAbsolutePath(), length, currentFileSizeLeft, false, null, participantUid);	
				    
				    try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						service.log(e);
					}
				}
			} catch (IOException e) {
				service.log(e);
				transferFailed(e, file.getAbsolutePath(), message, participantUid);
				cleanup();
				return;
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						service.log(e);
					}
				}
			}
			connectionState = CONNSTATE_FILE_HEADER;
			
			service.log(file.getName()+" sent");
		}

		private void cleanup() {
			try {
				socket.close();
				ftMessages.remove(message);
				activeTransfers.remove(ProtocolUtils.bytes2LongBE(message.messageId, 0));
				if (server != null){
					server.close();
					server = null;					
				}
			} catch (IOException e) {
			}
		}

		private synchronized void fileData(byte[] blob, int read, final long bytesLeft) {
			if (currentFileStream != null) {
				try {
					/*
					 * if (connectionState == CONNSTATE_FILE_HEADER){
					 * currentFileStream.close(); currentFileStream = null; }
					 * else if (connectionState == CONNSTATE_FILE_BODY){
					 * currentFileStream.write(blob); }
					 */

					if (connectionState == CONNSTATE_FILE_BODY) {
						currentFileStream.write(blob, 0, read);						
						currentFileStream.flush();
						
						if (bytesLeft < 1) {
							connectionState = CONNSTATE_FILE_HEADER;
							final String filename = currentFileStream.file.getAbsolutePath();
							currentFileStream.close();
							currentFileStream = null;
							service.log(currentFileName + " got");
							
							sendNotification( message.messageId, filename, currentFileSize, currentFileSize - bytesLeft, true, null, participantUid);
						} else {
							sendNotification( message.messageId, currentFileStream.getFile().getAbsolutePath(), currentFileSize, currentFileSize - bytesLeft, true, null, participantUid);
						}
					}
					// messageId, filename, totalSize, sizeTransferred,
					// isReceive, error
					

				} catch (IOException e) {
					Logger.log(e);
					try {
						currentFileStream.close();
					} catch (IOException e1) {
						Logger.log(e);
					}
					currentFileStream = null;
				}
			}
		}

		public void fireTransfer() {
			service.log("client ready, proceed FT");
			if (message.receiverId.equals(service.getUn())){
				message.receiverId = message.senderId;
				message.senderId = service.getUn();
			}
			if (files != null){
				if (currentFileSizeLeft < 1){
					sendFileInfo(files.get(0));
				}
			}			
		}
	}
	
	private synchronized void sendNotification(byte[] messageId, String filename, long totalSize, long sizeSent, boolean incoming, String error, String participantUid){
		NotificationData data = new NotificationData(messageId, filename, totalSize, sizeSent, incoming, error, participantUid);
		notifications.add(data);
		
		new Thread("Notification"){
			
			@Override
			public void run(){
				sendNotifications();
			}
			
		}.start();
	}

	public long getChecksum(File file) throws IOException, IllegalStateException {
		long sum = 0;
		    long end = file.length();
		    RandomAccessFile aFile = null;
		    try {
		      FileTransferChecksum summer = new FileTransferChecksum();
		      ByteBuffer buffer = ByteBuffer.allocate(1024);
		      long remaining = end;
		      aFile = new RandomAccessFile(file, "r");
		      FileChannel channel = aFile.getChannel();
		      while (remaining > 0) {
		        buffer.rewind();
		        buffer.limit((int) Math.min(remaining, buffer.capacity()));
		        int count = channel.read(buffer);
		        if (count == -1) break;
		        buffer.flip();
		        remaining -= buffer.limit();
		        summer.update(buffer.array(), buffer.arrayOffset(), buffer.limit());
		      }
		      if (remaining > 0) {
		        throw new IOException("could not get checksum for entire file; "
		            + remaining + " failed of " + end);
		      }

		      sum = summer.getValue();

		    } finally {
		      if (aFile != null) {
		    	  aFile.close();
		      }
		    }
		    return sum;
		  
	}

	private void sendNotifications() {
		synchronized (notifications) {
			while (notifications.size() > 0){
				NotificationData data = notifications.remove(0);
				service.getServiceResponse().respond(ICQServiceResponse.RES_FILEPROGRESS, data.messageId, data.filePath, data.totalSize, data.sent, data.incoming, data.error, data.participantUid);
			}
		}		
	}

	private byte[] getHandshakeData(ICBMMessage message) {
		service.log("get handshake for "+message.externalPort+" id "+ProtocolUtils.getHexString(message.messageId));
		
		byte[] header = new byte[12];

		Arrays.fill(header, (byte) 0);
		header[2] = 0x04;
		header[3] = 0x4a;

		if (message.connectFTProxy){
			header[5] = 0x4;
		} else {
			header[5] = 0x2;
		}

		byte[] uinBytes;
		try {
			uinBytes = message.senderId.getBytes("ASCII");
		} catch (UnsupportedEncodingException e1) {
			uinBytes = message.senderId.getBytes();
		}
		
		TLV clsidTlv = new TLV();
		clsidTlv.type = 1;
		clsidTlv.value = ICQConstants.CLSID_AIM_FILESEND;

		byte[] tlvBytes = service.getDataParser().tlvs2Bytes(new TLV[] { clsidTlv });

		byte[] out = new byte[21 + uinBytes.length + tlvBytes.length + ((message.connectFTProxy) ? 2 : 0)];

		byte pos = 0;
		System.arraycopy(header, 0, out, pos, header.length);
		pos += header.length;
		out[pos] = (byte) uinBytes.length;
		pos++;
		System.arraycopy(uinBytes, 0, out, pos, uinBytes.length);
		pos += uinBytes.length;
		
		if (message.connectFTProxy){
			System.arraycopy(ProtocolUtils.short2ByteBE((short) message.externalPort), 0, out, pos, 2);
			pos+=2;
		}
		
		System.arraycopy(message.messageId, 0, out, pos, 8);
		pos += 8;
		
		System.arraycopy(tlvBytes, 0, out, pos, tlvBytes.length);

		System.arraycopy(ProtocolUtils.short2ByteBE((short) (out.length - 2)), 0, out, 0, 2);

		return out;
	}

	public void transferFailed(Exception e, String filename, ICBMMessage message, String participantUid) {
		if (!message.senderId.equals(service.getUn())){
			message.receiverId = participantUid;
			message.senderId = service.getUn();
		}
		message.rvMessageType = 1;
		service.getMessagingEngine().sendFileMessageReject(message);
		sendNotification(message.messageId, filename, 100, 0, false, e.getLocalizedMessage(), participantUid);
	}

	private ExtendedBufferedOutputStream createFile(String filename, long filesize, long modTime, ICBMMessage message, String participantUid) {
		// Dummy
		String storageState = Environment.getExternalStorageState();
		if (storageState.equals(Environment.MEDIA_MOUNTED)) {
			try {
				File file = (File) service.getServiceResponse().respond(ICQServiceResponse.RES_GET_FILE_FOR_SAVING, filename, service.getBuddyList().findBuddyByUin(participantUid), modTime);
				FileOutputStream fos = new FileOutputStream(file, true);
				ExtendedBufferedOutputStream os = new ExtendedBufferedOutputStream(file, fos);
				return os;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			transferFailed(new IOException("No storage mounted"), filename, message, participantUid);
		}
		return null;
	}

	private byte[] getFileInfoByteBlock(byte[] blob, short state) {
		byte[] out = new byte[blob.length + 6];

		out[0] = 0x4f; // file header
		out[1] = 0x46;
		out[2] = 0x54;
		out[3] = 0x32;

		System.arraycopy(ProtocolUtils.short2ByteBE((short) (blob.length + 6)), 0, out, 4, 2);

		System.arraycopy(ProtocolUtils.short2ByteBE(state), 0, blob, 0, 2);
		System.arraycopy(blob, 0, out, 6, blob.length);

		return out;
	}
	
	private Flap getAcceptMessage(ICBMMessage message){
		message.rvMessageType = 2;
		
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_SENDTHROUGHSERVER;
		data.requestId = ICQConstants.SNAC_MESSAGING_SENDTHROUGHSERVER;
		
		TLV tlv2711 = new TLV();
		tlv2711.type = 0x2711;
		
		byte[] tlv5data = service.getDataParser().tlvs2Bytes(new TLV[]{tlv2711});
		byte[] tlv5fullData = new byte[26 + tlv5data.length];
		System.arraycopy(ProtocolUtils.short2ByteBE(message.rvMessageType), 0, tlv5fullData, 0, 2);
		System.arraycopy(message.messageId, 0, tlv5fullData, 2, 8);
		System.arraycopy(ICQConstants.CLSID_AIM_FILESEND, 0, tlv5fullData, 10, 16);
		System.arraycopy(tlv5data, 0, tlv5fullData, 26, tlv5data.length);

		TLV ch2messageTLV = new TLV();
		ch2messageTLV.type = 0x5;
		ch2messageTLV.value = tlv5fullData;

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

		data.data = new TLV[] { ch2messageTLV };
		data.plainData = snacRawData;

		flap.data = data;
		
		return flap;
	}

	private Flap getRedirectToProxyMessage(String rvIp, int portId, ICBMMessage message, int seqNum) {
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
		unknownA.value = new byte[] { 0, (byte) seqNum };

		TLV rvIPtlv = new TLV();
		rvIPtlv.type = 2;
		rvIPtlv.value = ProtocolUtils.ipString2ByteBE(rvIp);

		TLV xoredRvIpTlv = new TLV();
		xoredRvIpTlv.type = 0x16;
		xoredRvIpTlv.value = ProtocolUtils.unxorByteArray(rvIPtlv.value);

		TLV extPortTlv = new TLV();
		extPortTlv.type = 5;
		extPortTlv.value = ProtocolUtils.short2ByteBE((short) portId);

		TLV xoredPortTlv = new TLV();
		xoredPortTlv.type = 0x17;
		xoredPortTlv.value = ProtocolUtils.unxorByteArray(extPortTlv.value);

		TLV redirectTlv = new TLV();
		redirectTlv.type = 0x10;

		TLV[] tlv5content = new TLV[] { unknownA, rvIPtlv, xoredRvIpTlv, extPortTlv, xoredPortTlv, redirectTlv };

		byte[] tlv5data = service.getDataParser().tlvs2Bytes(tlv5content);
		byte[] tlv5fullData = new byte[26 + tlv5data.length];
		System.arraycopy(ProtocolUtils.short2ByteBE(message.rvMessageType), 0, tlv5fullData, 0, 2);
		System.arraycopy(message.messageId, 0, tlv5fullData, 2, 8);
		System.arraycopy(ICQConstants.CLSID_AIM_FILESEND, 0, tlv5fullData, 10, 16);
		System.arraycopy(tlv5data, 0, tlv5fullData, 26, tlv5data.length);

		TLV ch2messageTLV = new TLV();
		ch2messageTLV.type = 0x5;
		ch2messageTLV.value = tlv5fullData;

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

		data.data = new TLV[] { ch2messageTLV };
		data.plainData = snacRawData;

		flap.data = data;

		return flap;
	}

	public long sendFiles(final ICBMMessage message) {
		
		message.rvMessageType = 0;
		if (message.messageId == null) {
			message.messageId = new byte[8];
			ICBMMessagingEngine.RANDOM.nextBytes(message.messageId);
		}
		message.externalPort = service.getLoginPort();
		message.rvIp = ProtocolUtils.getIPString(service.getInternalIp());
		
		ftMessages.add(message);
		
		new Thread("File sender "+message.receiverId){
			
			@Override
			public void run(){
				try {
					List<File> files = message.getFileList();
					createPeer(message, files);
				} catch (IOException e) {
					connectProxy(message, activeTransfers.get(ProtocolUtils.bytes2LongBE(message.messageId, 0)));
				}
			}
			
		}.start();
		return ProtocolUtils.bytes2LongBE(message.messageId);
	}
	
	private void sendFileTransferRequest(ICBMMessage message, List<File> files){
		short type = 1;
		
		TLV tlv2711 = null;
		
		TLV unknownA = new TLV();
		unknownA.type = 0xa;
		
		if (files != null){
			int count = files.size();
			long filesize = 0;
			for (File f : files) {
				filesize += f.length();
			}
			byte[] filename;
			if (files.size() == 1) {
				try {
					filename = files.get(0).getName().getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					filename = files.get(0).getName().getBytes();
				}
			} else {
				filename = new byte[0];
			}
			byte[] tlv2711data = new byte[9 + filename.length];
			int pos = 0;
			System.arraycopy(ProtocolUtils.short2ByteBE(type), 0, tlv2711data, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.short2ByteBE((short) count), 0, tlv2711data, pos, 2);
			pos += 2;
			System.arraycopy(ProtocolUtils.int2ByteBE((int) filesize), 0, tlv2711data, pos, 4);
			pos += 4;
			System.arraycopy(filename, 0, tlv2711data, pos, filename.length);
			
			tlv2711 = new TLV();
			tlv2711.type = 0x2711;
			tlv2711.value = tlv2711data;
			
			unknownA.value = new byte[] { 0, 1 };
		} else {
			unknownA.value = new byte[] { 0, 2 };
		}

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

		TLV unknownF = new TLV();
		unknownF.type = 0xf;

		TLV rvIPtlv = new TLV();
		rvIPtlv.type = 2;
		rvIPtlv.value = ProtocolUtils.ipString2ByteBE(message.rvIp);

		TLV xoredRvIpTlv = new TLV();
		xoredRvIpTlv.type = 0x16;
		xoredRvIpTlv.value = ProtocolUtils.unxorByteArray(rvIPtlv.value);

		TLV extPortTlv = new TLV();
		extPortTlv.type = 5;
		extPortTlv.value = ProtocolUtils.short2ByteBE((short) message.externalPort);

		TLV xoredPortTlv = new TLV();
		xoredPortTlv.type = 0x17;
		xoredPortTlv.value = ProtocolUtils.unxorByteArray(extPortTlv.value);

		TLV internalIPTlv = new TLV();
		internalIPTlv.type = 0x3;
		internalIPTlv.value = ProtocolUtils.ipString2ByteBE(message.rvIp);		

		TLV tlv2712 = new TLV();
		tlv2712.type = 0x2712;
		tlv2712.value = new String("utf-8").getBytes();

		/*
		 * TLV tlv2713 = new TLV(); tlv2713.type = 0x2713; tlv2713.value = new
		 * byte[8]; Arrays.fill(tlv2713.value, (byte) 0);
		 */
		
		TLV[] tlv5content;
		if (files != null){
			tlv5content = new TLV[] { unknownA, unknownF, rvIPtlv, xoredRvIpTlv, internalIPTlv, extPortTlv, xoredPortTlv, tlv2711, tlv2712 };			
		} else {
			tlv5content = new TLV[] { unknownA, unknownF, rvIPtlv, xoredRvIpTlv, internalIPTlv, extPortTlv, xoredPortTlv };			
		}
		
		byte[] tlv5data = service.getDataParser().tlvs2Bytes(tlv5content);
		byte[] tlv5fullData = new byte[26 + tlv5data.length];
		System.arraycopy(ProtocolUtils.short2ByteBE(message.rvMessageType), 0, tlv5fullData, 0, 2);
		System.arraycopy(message.messageId, 0, tlv5fullData, 2, 8);
		System.arraycopy(ICQConstants.CLSID_AIM_FILESEND, 0, tlv5fullData, 10, 16);
		System.arraycopy(tlv5data, 0, tlv5fullData, 26, tlv5data.length);

		TLV ch2messageTLV = new TLV();
		ch2messageTLV.type = 0x5;
		ch2messageTLV.value = tlv5fullData;

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

		data.data = new TLV[] { ch2messageTLV };
		data.plainData = snacRawData;

		flap.data = data;

		service.getRunnableService().sendToSocket(flap);
	}

	private ServerSocket createLocalSocket(final FileRunnableService frs) throws IOException {
		final ServerSocket server = new ServerSocket(0);
		
		new Thread("FT Server socket listener") {

			@Override
			public void run() {
				try {
					server.setSoTimeout(SERVER_SOCKET_TIMEOUT);
					Socket socket = server.accept();

					frs.socket = socket;
					service.log("client connected");
					frs.start();
				} catch (Exception e) {
					service.log(e);
				}
			}

		}.start();
		
		return server;
	}

	public void redirectRequest(ICBMMessage message) {
		FileRunnableService runnable = activeTransfers.get(ProtocolUtils.bytes2LongBE(message.messageId, 0));
		if (runnable == null) {
			return;
		}
		runnable.message = message;
		if (message.connectFTProxy){
			connectProxy(message, runnable);
		} else if (message.connectFTPeer){
			connectPeer(message, runnable, false);
		}
	}

	public void fireTransfer(ICBMMessage message) {		
		FileRunnableService runnable = activeTransfers.get(ProtocolUtils.bytes2LongBE(message.messageId, 0));
		if (runnable == null) {
			return;
		}
		runnable.fireTransfer();
	}
	
	private class ExtendedBufferedOutputStream extends BufferedOutputStream {
		
		private final File file;
		
		public ExtendedBufferedOutputStream(File file, OutputStream os) {
			super(os, 88000);
			this.file = file;
		}

		public File getFile() {
			return file;
		}
	}

	public void cancel(Long messageId) {
		
		if (findMessageByMessageId(messageId) == null) {
			return;
		};
		
		fileReceiveResponse(messageId, false);
		
		FileRunnableService runnable = activeTransfers.get(messageId);
		if (runnable == null) {
			return;
		}	
		
		if (runnable.socket!=null && !runnable.socket.isClosed()){
			try {
				runnable.socket.close();
			} catch (IOException e) {
				service.log(e);
			}
		}
		
		activeTransfers.remove(messageId);
		removeMessageByMessageId(messageId);		
	}
	
	private class NotificationData {
		
		public byte[] messageId;
		public String filePath;
		public long totalSize;
		public long sent;
		public boolean incoming;
		public String error;
		public String participantUid;
		
		public NotificationData( byte[] messageId, String filePath, long totalSize, long sent, boolean incoming, String error, String participantUid){
			this.messageId = messageId;
			this.filePath = filePath;
			this.totalSize = totalSize;
			this.sent = sent;
			this.incoming = incoming;
			this.error = error;
			this.participantUid = participantUid;
		}
	}

	public void cancelAll() {
		for (FileRunnableService runnable: activeTransfers.values()){
			if (runnable.socket != null && !runnable.socket.isClosed()){
				try {
					runnable.socket.close();
				} catch (IOException e) {
					service.log(e);
				}
			}
		}
	}
	
	/**
	 * An implementation of the checksumming method used by AOL Instant Messenger's
	 * file transfer protocol.
	 */
	public final class FileTransferChecksum {
	    /** The checksum of an empty set of data. */
	    public static final long CHECKSUM_EMPTY = 0xffff0000L;

	    /** The checksum value. */
	    private long checksum;

	    { // init
	        reset();
	    }

	    /**
	     * Creates a new file transfer checksum computer object.
	     */
	    public FileTransferChecksum() { }

	    public void update(int value) {
	        update(new byte[] { (byte) value }, 0, 1);
	    }

	    public void update(final byte[] input, final int offset, final int len) {
	        if (input == null){
	        	return;
	        }

	        assert checksum >= 0;

	        long check = (checksum >> 16) & 0xffffL;

	        for (int i = 0; i < len; i++) {
	            final long oldcheck = check;

	            final int byteVal = input[offset + i] & 0xff;

	            final int val;
	            if ((i & 1) != 0) val = byteVal;
	            else val = byteVal << 8;

	            check -= val;

	            if (check > oldcheck) check--;
	        }

	        check = ((check & 0x0000ffff) + (check >> 16));
	        check = ((check & 0x0000ffff) + (check >> 16));

	        checksum = check << 16 & 0xffffffffL;
	        assert checksum >= 0;
	    }

	    public long getValue() {
	        assert checksum >= 0;
	        return checksum;
	    }

	    public void reset() {
	        checksum = CHECKSUM_EMPTY;
	        assert checksum >= 0;
	    }

	    public String toString() {
	        return "FileTransferChecksum: " + checksum;
	    }
	}
}
