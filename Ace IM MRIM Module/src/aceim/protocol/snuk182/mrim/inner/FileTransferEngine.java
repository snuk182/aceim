package aceim.protocol.snuk182.mrim.inner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import aceim.protocol.snuk182.mrim.MrimEntityAdapter;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimFileTransfer;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimIncomingFile;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimPacket;
import aceim.protocol.snuk182.mrim.utils.ProtocolUtils;
import android.os.Environment;

public class FileTransferEngine {

	private static final String MRA_FT_HELLO = "MRA_FT_HELLO ";
	private static final String MRA_GET_FILE = "MRA_FT_GET_FILE ";
	private static final String LIST_DATA_DIVIDER = ";";
	private static final String IN_DATA_DIVIDER = ":";

	private static final int SERVER_SOCKET_TIMEOUT = 600000;

	private static final String MrimProxyName = "mrim10-3.mail.ru";

	private byte[] localIp = new byte[] { 0, 0, 0, 0 };

	private final MrimServiceInternal service;
	private final List<MrimFileTransfer> transfers = new ArrayList<MrimFileTransfer>();
	private final Map<Long, FileRunnableService> activeTransfers = new HashMap<Long, FileRunnableService>();

	private List<NotificationData> notifications = new LinkedList<NotificationData>();

	public FileTransferEngine(MrimServiceInternal service) {
		this.service = service;
	}

	public long sendFiles(String buddyMrid, final List<File> files, byte[] localIp, int messageId) {
		final MrimFileTransfer transfer = getFileTransferRequest(buddyMrid, files, messageId);
		this.localIp = localIp;

		transfers.add(transfer);
		new Thread("File sender " + buddyMrid) {

			@Override
			public void run() {
				try {
					sendFileTransferRequest(createPeer(transfer));
				} catch (IOException e) {
					// todo ask mirror?
				}
			}

		}.start();
		return transfer.messageId;
	}

	private FileRunnableService createPeer(MrimFileTransfer transfer) throws IOException {
		service.log("ft: creating own peer");
		FileRunnableService frs = activeTransfers.get(transfer.messageId);

		if (frs == null) {
			service.log("ft: new runnable for " + transfer.buddyMrid);
			frs = new FileRunnableService(transfer);
			frs.connectionState = FileRunnableService.CONNSTATE_HANDSHAKE;
			activeTransfers.put((long) transfer.messageId, frs);
		} else {
			service.log("ft: existing runnable for " + transfer.buddyMrid);
			frs.transfer = transfer;
		}
		frs.connectionState = FileRunnableService.CONNSTATE_HANDSHAKE;

		frs.server = createLocalSocket(frs);
		/*
		 * message.externalPort = frs.server.getLocalPort(); message.rvIp =
		 * ProtocolUtils.getIPString(service.getInternalIp());
		 * message.rvMessageType = 0;
		 */
		return frs;
	}

	private void sendFileTransferRequest(FileRunnableService frs) {

		String myIpPortStr = ProtocolUtils.getIPString(localIp) + IN_DATA_DIVIDER + frs.server.getLocalPort() + LIST_DATA_DIVIDER;

		service.log("--- my ipport " + myIpPortStr);

		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_FILE_TRANSFER;
		byte[] to = MrimEntityAdapter.string2lpsa(frs.transfer.buddyMrid);
		byte[] sessionId = ProtocolUtils.int2ByteLE(frs.transfer.messageId);

		long ln = 0;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < frs.transfer.files.size(); i++) {
			File file = frs.transfer.files.get(i);
			ln += file.length();

			sb.append(file.getName().replaceAll(IN_DATA_DIVIDER, "_").replaceAll(LIST_DATA_DIVIDER, "_"));
			// sb.append(IN_DATA_DIVIDER);
			sb.append(LIST_DATA_DIVIDER);
			sb.append(file.length());
			sb.append(LIST_DATA_DIVIDER);
		}
		byte[] lengthSum = ProtocolUtils.long2ByteLE(ln);
		byte[] filenames = MrimEntityAdapter.string2lpsa(sb.toString());
		byte[] unk1 = MrimEntityAdapter.string2lpsa("");
		byte[] myIpPort = MrimEntityAdapter.string2lpsa(myIpPortStr);

		byte[] internalData = new byte[to.length + sessionId.length + 4 + 4 + filenames.length + unk1.length + myIpPort.length];

		int i = 0;
		System.arraycopy(to, 0, internalData, i, to.length);
		i += to.length;
		System.arraycopy(sessionId, 0, internalData, i, sessionId.length);
		i += sessionId.length;
		System.arraycopy(lengthSum, 0, internalData, i, 4);
		i += 4;
		System.arraycopy(ProtocolUtils.int2ByteLE(filenames.length + unk1.length + myIpPort.length), 0, internalData, i, 4);
		i += 4;
		System.arraycopy(filenames, 0, internalData, i, filenames.length);
		i += filenames.length;
		System.arraycopy(unk1, 0, internalData, i, unk1.length);
		i += unk1.length;
		System.arraycopy(myIpPort, 0, internalData, i, myIpPort.length);
		i += myIpPort.length;

		packet.rawData = internalData;

		service.getRunnableService().sendToSocket(packet);
	}

	private byte[] getHandshakeData(MrimFileTransfer transfer) {
		service.log("get handshake for " + transfer.host + " id " + transfer.messageId);

		String str = new String(MRA_FT_HELLO + service.getMrid());
		byte[] boo = new byte[str.length() + 1];
		System.arraycopy(str.getBytes(), 0, boo, 0, str.length());
		boo[boo.length - 1] = 0;
		return boo;
	}

	private ServerSocket createLocalSocket(final FileRunnableService frs) throws IOException {
		final ServerSocket server = new ServerSocket(0);

		new Thread() {

			@Override
			public void run() {
				try {
					setName("FT Server socket listener " + server.getLocalPort());
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

	private MrimFileTransfer getFileTransferRequest(String buddyMrid, List<File> files, int messageId) {
		MrimFileTransfer transfer = new MrimFileTransfer();
		transfer.buddyMrid = buddyMrid;
		transfer.files = files;
		transfer.messageId = messageId;
		return transfer;
	}

	class FileRunnableService extends Thread {
		public static final int CONNSTATE_CONNECTED = 0;
		public static final int CONNSTATE_HANDSHAKE = 1;
		public static final int CONNSTATE_FILE_HEADER = 2;
		public static final int CONNSTATE_FILE_BODY = 3;
		public static final int CONNSTATE_FILE_SENT = 4;
		public static final int CONNSTATE_DISCONNECTED = 5;

		ServerSocket server = null;
		Socket socket;
		int connectionState = CONNSTATE_CONNECTED;
		MrimFileTransfer transfer;
		String participantUid = null;
		List<byte[]> blobs = new LinkedList<byte[]>();

		long currentFileSizeLeft = 0;
		long currentFileSize = 0;
		byte[] currentFileInfo = null;
		private ExtendedBufferedOutputStream currentFileStream;

		byte[] buffer = null;

		public FileRunnableService(MrimFileTransfer transfer) {
			this(null, transfer);
		}

		public FileRunnableService(Socket socket, MrimFileTransfer transfer) {
			this.socket = socket;
			this.transfer = transfer;
			if (transfer.files != null) {
				currentFileSize = 0;
				for (File f : transfer.files) {
					currentFileSize += f.length();
				}
			}

			participantUid = transfer.buddyMrid;

			setName("File transfer " + transfer.buddyMrid + " " + transfer.messageId);
		}

		@Override
		public void run() {
			if (socket == null) {
				return;
			}

			getDataFromSocket();
		}

		private ExtendedBufferedOutputStream createFile(String filename, long filesize, long modTime, MrimFileTransfer transfer, String participantUid) {
			// Dummy
			String storageState = Environment.getExternalStorageState();
			if (storageState.equals(Environment.MEDIA_MOUNTED)) {
				try {
					File file = (File) service.getServiceResponse().respond(MrimServiceResponse.RES_GET_FILE_FOR_SAVING, filename, participantUid, modTime);
					FileOutputStream fos = new FileOutputStream(file, true);
					ExtendedBufferedOutputStream os = new ExtendedBufferedOutputStream(file, fos);
					return os;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				transferFailed(transfer, "No storage mounted");
			}
			return null;
		}

		private void sendFileRequest(MrimIncomingFile file) {
			currentFileStream = createFile(file.filename, file.filesize, new Date().getTime(), transfer, participantUid);

			byte[] infoBlob;

			String str = MRA_GET_FILE + file.filename;
			infoBlob = new byte[str.length() + 1];
			System.arraycopy(str.getBytes(), 0, infoBlob, 0, infoBlob.length - 1);
			infoBlob[infoBlob.length - 1] = 0;

			currentFileSize = file.filesize;
			currentFileSizeLeft = file.filesize;
			connectionState = CONNSTATE_FILE_BODY;
			sendToSocket(infoBlob);
		}

		private void sendProxyHandshake() {
			connectionState = CONNSTATE_CONNECTED;
			try {
				sendToSocket(getProxyHandshakeData(transfer));
			} catch (MrimException e) {
				service.log(e);
				transferFailed(transfer, e.getLocalizedMessage());
				notifyFail(transfer);
				cleanup();
			}
		}

		private void sendHandshake() {
			connectionState = CONNSTATE_HANDSHAKE;
			sendToSocket(getHandshakeData(transfer));
		}

		public byte[] getProxyHandshakeData(MrimFileTransfer transfer) throws MrimException {
			MrimPacket packet = new MrimPacket();
			packet.type = MrimConstants.MRIM_CS_PROXY_HELLO;
			packet.rawData = transfer.proxySessionId;
			return service.processor.packet2Bytes(packet);
		}

		private void getDataFromSocket() {
			int read = 0;
			boolean fullPacket = true;
			final List<byte[]> tmpBlobs = new LinkedList<byte[]>();

			while (connectionState != CONNSTATE_DISCONNECTED && socket != null && socket.isConnected() && !socket.isClosed()) {
				InputStream is;
				try {
					is = socket.getInputStream();

					if (is.available() < 1) {
						Thread.sleep(300);
					} else {
						Thread.sleep(500);

						switch (connectionState) {
						case CONNSTATE_CONNECTED:
							byte[] head = new byte[44];

							is.read(head, 0, 44);

							service.log("-- FT got " + ProtocolUtils.getSpacedHexString(head));

							int ack = ProtocolUtils.bytes2IntLE(head, 12);

							if (ack == MrimConstants.MRIM_CS_PROXY_HELLO_ACK) {
								connectionState = CONNSTATE_HANDSHAKE;
								if (transfer.files == null) {
									sendHandshake();
								}
							}

							break;
						case CONNSTATE_HANDSHAKE:
						case CONNSTATE_FILE_HEADER:
							read = 0;
							byte[] blob = new byte[is.available()];
							is.read(blob, 0, blob.length);

							String str = new String(blob);
							service.log("-- FT got " + ProtocolUtils.getSpacedHexString(blob) + " (" + str + ")");

							connectionState = CONNSTATE_FILE_HEADER;
							if (str.contains(MRA_FT_HELLO)) {
								if (transfer.files != null) { // i am sender
									if (transfer.connection != MrimFileTransfer.CONN_MIRROR) {
										sendHandshake();
									}
								} else {
									if (transfer.connection == MrimFileTransfer.CONN_MIRROR) {
										sendHandshake();
									}
									// service.getRunnableService().sendToSocket(getAcceptMessage(transfer));
									sendFileRequest(transfer.incomingFiles.remove(0));
								}
								break;
							}

							if (str.contains(MRA_GET_FILE)) {
								processHeader(blob);
							}
							break;
						case CONNSTATE_FILE_BODY:
							if (buffer == null) {
								buffer = new byte[88000];
							}

							read = is.read(buffer, 0, buffer.length);
							service.log("read " + read + "| bytes left " + currentFileSizeLeft);
							currentFileSizeLeft -= read;
							fileData(buffer, read, currentFileSizeLeft);

							if (currentFileSizeLeft < 1) {
								// sendFileAck();
								connectionState = CONNSTATE_FILE_HEADER;
								buffer = null;

								if (transfer.incomingFiles != null) {
									if (transfer.incomingFiles.size() > 0) {
										sendFileRequest(transfer.incomingFiles.remove(0));
									} else {
										cleanup();
									}
								}

								if (transfer.files != null && transfer.files.size() < 1) {
									cleanup();
								}
							}
							continue;
						}

						if (fullPacket) {
							byte[] boo = new byte[0];
							for (byte[] i : tmpBlobs) {
								boo = ProtocolUtils.concatByteArrays(boo, i);
							}

							tmpBlobs.clear();
							blobs.add(boo);

						}
					}
				} catch (IOException e) {
					service.log(e);
				} catch (InterruptedException e) {
					service.log(e);
				}
			}
			cleanup();
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
							service.log(filename + " got");

							sendNotification(transfer.messageId, filename, currentFileSize, currentFileSize - bytesLeft, true, null, participantUid);
						} else {
							sendNotification(transfer.messageId, currentFileStream.getFile().getAbsolutePath(), currentFileSize, currentFileSize - bytesLeft, true, null, participantUid);
						}
					}
					// messageId, filename, totalSize, sizeTransferred,
					// isReceive, error

				} catch (IOException e) {
					service.log(e);
					try {
						currentFileStream.close();
					} catch (IOException e1) {
						service.log(e);
					}
					currentFileStream = null;
				}
			}
		}

		private void cleanup() {
			try {
				transfers.remove(transfer);
				activeTransfers.remove(transfer.messageId);
				if (server != null) {
					server.close();
					server = null;
				}
				socket.close();
			} catch (IOException e) {
				service.log(e);
			}
		}

		public synchronized boolean sendToSocket(byte[] out) {
			try {
				OutputStream os = socket.getOutputStream();

				service.log("-- FT To be sent " + ProtocolUtils.getSpacedHexString(out) + "/" + new String(out));
				os.write(out);

			} catch (IOException e) {
				connectionState = CONNSTATE_DISCONNECTED;
				service.log(e);
			}
			return true;
		}

		private void processHeader(byte[] blob) {
			if (blob.length < 1) {
				return;
			}
			String str = ProtocolUtils.getEncodedString(blob, 0, blob.length - 1);

			int index = str.indexOf(MRA_GET_FILE);
			if (index < 0) {
				return;
			}

			service.log("got header");
			currentFileInfo = blob;

			try {
				String fileInfo = str.substring(index + MRA_GET_FILE.length());
				service.log(transfer.buddyMrid + " asks for file " + fileInfo);

				for (int i = 0; i < transfer.files.size(); i++) {
					File fi = transfer.files.get(i);
					if (fi.getName().replaceAll(IN_DATA_DIVIDER, "_").replaceAll(LIST_DATA_DIVIDER, "_").equals(fileInfo)) {
						sendFileToSocket(fi);
						transfer.files.remove(fi);
						if (transfer.files.size() < 1) {
							cleanup();
						}
						return;
					}
				}
				transferFailed(transfer, "unknown file");

			} catch (Exception e) {
				service.log(e);
				transferFailed(transfer, e.getLocalizedMessage());
			}
			cleanup();
			notifyFail(transfer);
		}

		private void sendFileToSocket(final File file) {
			OutputStream os;
			try {
				os = socket.getOutputStream();
			} catch (IOException e) {
				service.log(e);
				transferFailed(transfer, e.getLocalizedMessage());
				notifyFail(transfer);
				cleanup();
				return;
			}
			long length = file.length();
			if (length > 8000) {
				buffer = new byte[8000];
			} else {
				buffer = new byte[(int) length];
			}

			currentFileSizeLeft = 0;
			int read = 0;
			service.log("sending " + file.getName() + " to " + participantUid);

			FileInputStream fis;
			try {
				fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis, 8000);
				while (currentFileSizeLeft < length) {
					read = bis.read(buffer, 0, buffer.length);
					if (read < 0) {
						break;
					}
					os.write(buffer, 0, read);
					os.flush();
					currentFileSizeLeft += read;
					service.log("sent " + currentFileSizeLeft + " bytes");

					sendNotification(transfer.messageId, file.getAbsolutePath(), length, currentFileSizeLeft, false, null, participantUid);

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						service.log(e);
					}
				}
			} catch (IOException e) {
				service.log(e);
				transferFailed(transfer, e.getLocalizedMessage());
				notifyFail(transfer);
				cleanup();
				return;
			}
			connectionState = CONNSTATE_FILE_HEADER;

			service.log(file.getName() + " sent");
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
	}

	public void parseFTRequest(MrimPacket packet) {
		MrimFileTransfer transfer = new MrimFileTransfer();

		int version = ProtocolUtils.bytes2IntLE(packet.rawData, 4);

		if (version > MrimConstants.PROTO_VERSION) {
			int i = 44;
			transfer.buddyMrid = MrimEntityAdapter.lpsa2String(packet.rawData, i);
			i += 4 + transfer.buddyMrid.length();
			transfer.messageId = ProtocolUtils.bytes2IntLE(packet.rawData, i);
			i += 4;
			i += 4; // total filesize - useless
			i += 4; // total file info size - useless

			String fileStr = MrimEntityAdapter.lpsa2String(packet.rawData, i);

			parseFiles(fileStr, transfer);

			i += 4 + fileStr.length();

			int nextPacket = ProtocolUtils.bytes2IntLE(packet.rawData, i);
			i += 4;
			if (nextPacket != 0) {
				int count = ProtocolUtils.bytes2IntLE(packet.rawData, i);
				i += 4;
				for (int ii = 0; ii < count; ii++) {
					fileStr = MrimEntityAdapter.lpsw2String(packet.rawData, i);
					i += 4 + 2 * fileStr.length();

					parseFiles(fileStr, transfer);
				}
			}

			String ipData = MrimEntityAdapter.lpsa2String(packet.rawData, i);
			parseIPString(transfer, ipData);

			transfers.add(transfer);
		} else {
			int i = 44;
			transfer.buddyMrid = MrimEntityAdapter.lpsa2String(packet.rawData, i);
			i += 4 + transfer.buddyMrid.length();
			transfer.messageId = ProtocolUtils.bytes2IntLE(packet.rawData, i);
			i += 4;
			i += 4; // total filesize - useless
			i += 4; // total file info size - useless

			String fileStr = MrimEntityAdapter.lpsa2String(packet.rawData, i);

			String[] strFiles = fileStr.split(LIST_DATA_DIVIDER);
			transfer.incomingFiles = new LinkedList<MrimIncomingFile>();
			for (String file : strFiles) {
				if (file.length() < 1) {
					continue;
				}

				String[] attrs = file.split(IN_DATA_DIVIDER);
				MrimIncomingFile ifile = new MrimIncomingFile();
				ifile.filename = attrs[0];
				ifile.filesize = Long.parseLong(attrs[1]);
				transfer.incomingFiles.add(ifile);
			}

			i += 4 + fileStr.length();

			i += 4; // data divider

			String ipData = MrimEntityAdapter.lpsa2String(packet.rawData, i);
			parseIPString(transfer, ipData);

			transfers.add(transfer);
		}

		service.getServiceResponse().respond(MrimServiceResponse.RES_FILEMESSAGE, transfer);
	}

	private void parseFiles(String fileStr, MrimFileTransfer transfer) {
		String[] strFiles = fileStr.split(LIST_DATA_DIVIDER);
		transfer.incomingFiles = new LinkedList<MrimIncomingFile>();

		for (int ii = 0; ii < strFiles.length;) {
			MrimIncomingFile ifile = new MrimIncomingFile();
			ifile.filename = strFiles[ii];
			ii++;
			ifile.filesize = Long.parseLong(strFiles[ii]);
			transfer.incomingFiles.add(ifile);
			ii++;
		}
	}

	public void parseFTResponse(MrimPacket packet) {
		int i = 44;
		int status = ProtocolUtils.bytes2IntLE(packet.rawData, i);
		i += 4;
		String from = MrimEntityAdapter.lpsa2String(packet.rawData, i);
		i += from.length() + 4;
		int msgId = ProtocolUtils.bytes2IntLE(packet.rawData, i);
		i += 4;

		service.log("Ft response " + status + " from " + from + " for " + msgId);
		MrimFileTransfer transfer = findTransfer(msgId);

		if (transfer == null)
			return;

		switch (status) {
		case MrimConstants.FILE_TRANSFER_STATUS_OK:
			break;
		case MrimConstants.FILE_TRANSFER_STATUS_DECLINE: // here so far
			transferFailed(transfer, "Cancelled");
			break;
		case MrimConstants.FILE_TRANSFER_STATUS_INCOMPATIBLE_VERS:
		case MrimConstants.FILE_TRANSFER_STATUS_ERROR:
			transferFailed(transfer, "remote error");
			break;
		case MrimConstants.FILE_TRANSFER_MIRROR:
			String ipData = MrimEntityAdapter.lpsa2String(packet.rawData, i);
			transfer.connection = MrimFileTransfer.CONN_MIRROR;
			parseIPString(transfer, ipData);
			connectPeer(transfer, null);
			break;
		}
	}

	private void parseIPString(MrimFileTransfer transfer, String ipData) {
		String[] ipDataParts = ipData.split(LIST_DATA_DIVIDER);
		for (String data : ipDataParts) {
			if (data.length() < 1) {
				continue;
			}

			String[] connection = data.split(IN_DATA_DIVIDER);

			transfer.host = connection[0];
			transfer.port = Integer.parseInt(connection[1]);

			if (transfer.port != 443) {// TODO do we need ssl? do we need more
										// ips?
				break;
			}
		}
	}

	private MrimFileTransfer findTransfer(int msgId) {
		for (MrimFileTransfer tr : transfers) {
			if (tr.messageId == msgId) {
				return tr;
			}
		}
		return null;
	}

	private void connectPeer(MrimFileTransfer transfer, FileRunnableService runnable) {
		service.log("connecting peer " + transfer.host + ":" + transfer.port + " for " + transfer.messageId + "//receiver ");

		Socket socket;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(InetAddress.getByAddress(ProtocolUtils.ipString2ByteBE(transfer.host)), transfer.port), 10000);
		} catch (Exception e) {
			service.log(e);
			socket = null;
		}

		if (socket != null && socket.isConnected()) {
			service.log("ft: direct socket connected for " + transfer.messageId);
			if (runnable == null) {
				service.log("ft: new runnable for " + transfer.messageId);
				runnable = new FileRunnableService(socket, transfer);
				activeTransfers.put((long) transfer.messageId, runnable);
			} else {
				service.log("ft: existing runnable for " + transfer.messageId);
				if (runnable.server != null) {
					try {
						runnable.server.close();
						runnable.server = null;
					} catch (IOException e) {
						service.log(e);
					}
				}

				if (runnable.socket != null) {
					try {
						runnable.socket.close();
					} catch (IOException e) {
						service.log(e);
					}
				}
				runnable.socket = socket;

			}
			runnable.start();

			if (transfer.connection == MrimFileTransfer.CONN_MIRROR) {
				runnable.sendHandshake();
			}

			if (transfer.connection == MrimFileTransfer.CONN_PROXY) {
				runnable.sendProxyHandshake();
			}

		} else {
			if (runnable == null) {
				runnable = new FileRunnableService(socket, transfer);
			}

			try {
				if (runnable.server != null) {
					runnable.server.close();
					runnable.server = null;
				}
			} catch (IOException e) {
				service.log(e);
			}

			runnable.socket = socket;

			service.log("ft: no direct connection");
			if (transfer.connection == MrimFileTransfer.CONN_PEER && transfer.files == null) {
				createMirror(transfer);
			} else if (transfer.connection == MrimFileTransfer.CONN_MIRROR && transfer.files != null) {
				createProxyCall(transfer);
			} else {
				transferFailed(transfer, "no route to host");
				notifyFail(transfer);
			}
		}
	}

	private void createProxyCall(MrimFileTransfer transfer) {
		try {
			transfer.connection = MrimFileTransfer.CONN_PROXY;
			InetAddress inetAdd = InetAddress.getByName(MrimProxyName);
			transfer.host = inetAdd.getHostAddress();
			transfer.port = 2041;

			service.getRunnableService().sendToSocket(getFTProxyRedirectCall(transfer));
		} catch (Exception uhe) {
			service.log(uhe);
		}
	}

	private MrimPacket getFTProxyRedirectCall(MrimFileTransfer transfer) throws IOException {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_PROXY;

		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		stream.write(MrimEntityAdapter.string2lpsa(transfer.buddyMrid));
		stream.write(ProtocolUtils.int2ByteLE(transfer.messageId));
		stream.write(ProtocolUtils.int2ByteLE(MrimConstants.MRIM_PROXY_TYPE_FILES));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < transfer.files.size(); i++) {
			File file = transfer.files.get(i);

			sb.append(file.getName().replaceAll(IN_DATA_DIVIDER, "_").replaceAll(LIST_DATA_DIVIDER, "_"));
			// sb.append(IN_DATA_DIVIDER);
			sb.append(LIST_DATA_DIVIDER);
			sb.append(file.length());
			sb.append(LIST_DATA_DIVIDER);
		}

		stream.write(MrimEntityAdapter.string2lpsa(sb.toString()));
		stream.write(MrimEntityAdapter.string2lpsa(transfer.host + IN_DATA_DIVIDER + transfer.port + LIST_DATA_DIVIDER));
		/*
		 * transfer.proxySessionId = new byte[16];
		 * 
		 * Random random = new Random();
		 * System.arraycopy(ProtocolUtils.long2ByteLE(random.nextLong()), 0,
		 * transfer.proxySessionId, 0, 8);
		 * System.arraycopy(ProtocolUtils.long2ByteLE(random.nextLong()), 0,
		 * transfer.proxySessionId, 8, 8);
		 * stream.write(transfer.proxySessionId);
		 */

		stream.write(new byte[16]);

		packet.rawData = stream.toByteArray();

		return packet;
	}

	private void createMirror(MrimFileTransfer transfer) {
		try {
			MrimPacket packet = getAnswerMessage(transfer, MrimConstants.FILE_TRANSFER_MIRROR);

			FileRunnableService frs = createPeer(transfer);

			String ipData = ProtocolUtils.getIPString(localIp) + IN_DATA_DIVIDER + frs.server.getLocalPort();

			packet.rawData = ProtocolUtils.concatByteArrays(packet.rawData, MrimEntityAdapter.string2lpsa(ipData));

			transfer.connection = MrimFileTransfer.CONN_MIRROR;

			service.getRunnableService().sendToSocket(packet);

		} catch (IOException e) {
			transferFailed(transfer, e.toString());
			notifyFail(transfer);
		}
	}

	private void notifyFail(MrimFileTransfer transfer) {
		service.getRunnableService().sendToSocket(getAnswerMessage(transfer, MrimConstants.FILE_TRANSFER_STATUS_ERROR));
	}

	private void transferFailed(MrimFileTransfer transfer, String error) {
		service.log(error);
		transfers.remove(transfer);
		activeTransfers.remove(transfer.messageId);
		sendNotification(transfer.messageId, transfer.buddyMrid, 0L, 0L, false, error, transfer.buddyMrid);
	}

	@SuppressWarnings("unused")
	private MrimPacket getAcceptMessage(MrimFileTransfer transfer) {
		return getAnswerMessage(transfer, MrimConstants.FILE_TRANSFER_STATUS_OK);
	}

	private MrimPacket getAnswerMessage(MrimFileTransfer transfer, int ftStatus) {
		MrimPacket packet = new MrimPacket();
		packet.type = MrimConstants.MRIM_CS_FILE_TRANSFER_ACK;

		byte[] mrid = MrimEntityAdapter.string2lpsa(transfer.buddyMrid);
		byte[] blob = new byte[8 + mrid.length];

		int i = 0;
		System.arraycopy(ProtocolUtils.int2ByteLE(ftStatus), 0, blob, i, 4);
		i += 4;

		System.arraycopy(mrid, 0, blob, i, mrid.length);
		i += mrid.length;
		System.arraycopy(ProtocolUtils.int2ByteLE(transfer.messageId), 0, blob, i, 4);

		packet.rawData = blob;
		return packet;
	}

	private synchronized void sendNotification(int messageId, String filename, long totalSize, long sizeSent, boolean incoming, String error, String participantUid) {
		NotificationData data = new NotificationData(messageId, filename, totalSize, sizeSent, incoming, error, participantUid);
		notifications.add(data);

		new Thread("Notification") {

			@Override
			public void run() {
				sendNotifications();
			}

		}.start();
	}

	private void sendNotifications() {
		synchronized (notifications) {
			while (notifications.size() > 0) {
				NotificationData data = notifications.remove(0);
				service.getServiceResponse().respond(MrimServiceResponse.RES_FILEPROGRESS, (long) data.messageId, data.filePath, data.totalSize, data.sent, data.incoming, data.error, data.participantUid);
			}
		}
	}

	private class NotificationData {

		public int messageId;
		public String filePath;
		public long totalSize;
		public long sent;
		public boolean incoming;
		public String error;
		public String participantUid;

		public NotificationData(int messageId, String filePath, long totalSize, long sent, boolean incoming, String error, String participantUid) {
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
		for (FileRunnableService runnable : activeTransfers.values()) {
			if (runnable.server != null && !runnable.server.isClosed()) {
				try {
					runnable.server.close();
				} catch (IOException e) {
					service.log(e);
				}
			}
			if (runnable.socket != null && !runnable.socket.isClosed()) {
				try {
					runnable.socket.close();
				} catch (IOException e) {
					service.log(e);
				}
			}
		}
	}

	public void fileReceiveResponse(Long msgId, Boolean accept, byte[] internalIp) {
		MrimFileTransfer transfer = findTransfer(msgId.intValue());

		if (transfer == null) {
			service.log("ft: no message");
			return;
		}

		this.localIp = internalIp;
		if (!accept) {
			service.log("ft: reject " + transfer.messageId);
			service.getRunnableService().sendToSocket(getAnswerMessage(transfer, MrimConstants.FILE_TRANSFER_STATUS_DECLINE));
		} else {
			service.log("ft: accept " + transfer.messageId);
			connectPeer(transfer, null);
		}
	}

	public void parseFTProxyConnectionRequest(MrimPacket packet) {
		int pos = 44;

		String email = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		service.log("proxy " + email);

		pos += email.length() + 4;

		int id = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos += 4;

		service.log("proxy id " + id);

		int dataType = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos += 4;

		service.log("proxy type " + dataType);

		if (dataType != MrimConstants.MRIM_PROXY_TYPE_FILES) {
			return;
		}

		MrimFileTransfer transfer = findTransfer(id);
		if (transfer == null) {
			return;
		}

		String rawdata = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		service.log("proxy data " + rawdata);

		parseFiles(rawdata, transfer);

		pos += rawdata.length() + 4;

		String iplist = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		service.log("proxy iplist " + iplist);

		pos += iplist.length() + 4;

		transfer.proxySessionId = new byte[16];
		System.arraycopy(packet.rawData, pos, transfer.proxySessionId, 0, 16);
		pos += 16;

		byte[] answerData = new byte[packet.rawData.length - 40];
		System.arraycopy(ProtocolUtils.int2ByteLE(MrimConstants.PROXY_STATUS_OK), 0, answerData, 0, 4);
		System.arraycopy(packet.rawData, 44, answerData, 4, packet.rawData.length - 44);

		MrimPacket ack = new MrimPacket();
		ack.type = MrimConstants.MRIM_CS_PROXY_ACK;
		ack.rawData = answerData;

		service.getRunnableService().sendToSocket(ack);

		transfer.connection = MrimFileTransfer.CONN_PROXY;
		parseIPString(transfer, iplist);
		connectPeer(transfer, activeTransfers.get(id));
	}

	public void parseFTProxyAck(MrimPacket packet) {
		int pos = 44;

		int result = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos += 4;

		String email = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		service.log("proxy " + email);

		pos += email.length() + 4;

		int id = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos += 4;

		service.log("proxy id " + id);

		int dataType = ProtocolUtils.bytes2IntLE(packet.rawData, pos);
		pos += 4;

		service.log("proxy type " + dataType);

		if (dataType != MrimConstants.MRIM_PROXY_TYPE_FILES) {
			return;
		}

		MrimFileTransfer transfer = findTransfer(id);
		if (transfer == null) {
			return;
		}

		FileRunnableService frs = activeTransfers.get(id);

		service.log("proxy id " + id);
		if (result != MrimConstants.PROXY_STATUS_OK) {
			service.log("Proxy error " + result);
			transferFailed(transfer, "Proxy error");
			notifyFail(transfer);
			if (frs != null) {
				frs.cleanup();
			}
			return;
		}

		if (transfer.proxySessionId != null) {
			service.log("proceed file send " + frs.connectionState);
			return;
		}

		String rawdata = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		service.log("proxy data " + rawdata);

		parseFiles(rawdata, transfer);

		pos += rawdata.length() + 4;

		String iplist = MrimEntityAdapter.lpsa2String(packet.rawData, pos);
		service.log("proxy iplist " + iplist);

		pos += iplist.length() + 4;

		transfer.proxySessionId = new byte[16];
		System.arraycopy(packet.rawData, pos, transfer.proxySessionId, 0, 16);
		pos += 16;

		parseIPString(transfer, iplist);

		connectPeer(transfer, frs);
	}
}
