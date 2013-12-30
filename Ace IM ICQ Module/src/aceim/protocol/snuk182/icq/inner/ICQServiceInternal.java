package aceim.protocol.snuk182.icq.inner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMMessage;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMParams;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyGroup;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyList;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.RateLimit;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.inner.dataprocessing.AuthenticationProcessor;
import aceim.protocol.snuk182.icq.inner.dataprocessing.BuddyIconEngine;
import aceim.protocol.snuk182.icq.inner.dataprocessing.FileTransferEngine;
import aceim.protocol.snuk182.icq.inner.dataprocessing.ICBMMessagingEngine;
import aceim.protocol.snuk182.icq.inner.dataprocessing.IFlapProcessor;
import aceim.protocol.snuk182.icq.inner.dataprocessing.MainProcessor;
import aceim.protocol.snuk182.icq.inner.dataprocessing.OnlineInfoEngine;
import aceim.protocol.snuk182.icq.inner.dataprocessing.PersonalInfoEngine;
import aceim.protocol.snuk182.icq.inner.dataprocessing.SSIProcessor;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;

public class ICQServiceInternal {

	public static final byte STATE_DISCONNECTED = 0;
	public static final byte STATE_CONNECTING_LOGIN = 1;
	public static final byte STATE_CONNECTING_BOS = 2;
	public static final byte STATE_AUTHENTICATING = 3;
	public static final byte STATE_CONNECTED = 4;

	public static final short REQ_NOP = 0;
	public static final short REQ_CONNECT = 1;
	public static final short REQ_DISCONNECT = 2;
	public static final short REQ_SETSTATUS = 3;
	public static final short REQ_SETEXTENDEDSTATUS = 4;
	public static final short REQ_GETSTATUS = 5;
	public static final short REQ_GETEXTENDEDSTATUS = 6;
	public static final short REQ_SENDMESSAGE = 7;
	public static final short REQ_SENDFILE = 8;
	public static final short REQ_GETSHORTBUDDYINFO = 9;
	public static final short REQ_GETOWNINFO = 10;
	public static final short REQ_SETOWNINFO = 11;
	public static final short REQ_ADDBUDDY = 12;
	public static final short REQ_REMOVEBUDDY = 13;
	public static final short REQ_EDITCONTACTLIST = 14;
	public static final short REQ_SEARCHFORBUDDY = 15;
	public static final short REQ_SAVEPARAMS = 16;
	public static final short REQ_GETCONTACTLIST = 17;
	public static final short REQ_GETGROUPLIST = 18;
	public static final short REQ_GETICON = 19;
	public static final short REQ_AUTHREQUEST = 20;
	public static final short REQ_AUTHRESPONSE = 21;
	public static final short REQ_RENAMEBUDDY = 22;
	public static final short REQ_MOVEBUDDY = 23;
	public static final short REQ_RENAMEGROUP = 24;
	public static final short REQ_ADDGROUP = 25;
	public static final short REQ_REMOVEGROUP = 26;
	public static final short REQ_MOVEBUDDIES = 27;
	public static final short REQ_REMOVEBUDDIES = 28;
	public static final short REQ_FILERESPOND = 29;
	public static final short REQ_FILECANCEL = 30;
	public static final short REQ_GETFULLBUDDYINFO = 31;
	public static final short REQ_SENDTYPING = 32;
	public static final short REQ_BUDDYVISIBILITY = 33;
	public static final short REQ_ACCOUNTVISIBILITY = 34;
	public static final short REQ_EDITBUDDY = 35;
	public static final short REQ_SHUTDOWN = 36;
	public static final short REQ_UPLOADICON = 37;

	public static final short REQ_KEEPALIVE_CHECK = 0xff;

	private String loginHost = "login.icq.com";
	private int loginPort = 800;
	// private int pingTimeout = 10;// seconds

	private String un;
	private String pw;

	private ICQServiceResponse serviceResponse;

	private ICQRunnableService runnableService;
	private final List<Flap> packets = Collections.synchronizedList(new ArrayList<Flap>());
	private ICQDataParser dataParser = new ICQDataParser();
	private IFlapProcessor processor = null;
	private ICBMMessagingEngine messagingEngine = new ICBMMessagingEngine(this);
	private OnlineInfoEngine onlineInfoEngine = new OnlineInfoEngine(this);
	private BuddyIconEngine buddyIconEngine = new BuddyIconEngine(this);
	private PersonalInfoEngine personalInfoEngine = null;
	private FileTransferEngine fileTransferEngine = null;
	private SSIProcessor ssiEngine = null;

	private final AtomicInteger intCounter = new AtomicInteger();

	final ICQBuddyList buddyList = new ICQBuddyList();

	private ICQOnlineInfo onlineInfo;
	private ICBMParams messageParams;
	private int maxVisibleListLength;
	private int maxInvisibleListLength;
	private TLV[] ssiLimits;
	private short[] serverSupportedFamilies = null;
	private RateLimit[] rateLimits = null;
	private volatile byte currentState = STATE_DISCONNECTED;
	public String lastConnectionError = null;

	public ICQServiceInternal(ICQServiceResponse icqResponse) {
		serviceResponse = icqResponse;
	}

	public void startMainProcessor() throws ICQException {
		processor = new MainProcessor();
		try {
			processor.init(this);
		} catch (Exception e) {
			log(e);
			throw new ICQException("Error starting main processor");
		}
	}

	public void runService(String host, int port) {
		if (runnableService != null) {
			runnableService.interrupt();
		}
		runnableService = new ICQRunnableService(host, port);
		runnableService.start();
	}

	protected void forceFlapProcess() throws Exception {
		while (packets.size() > 0) {
			synchronized (packets) {
				Flap flap = packets.remove(0);
				processor.process(flap);
			}
		}
	}

	public class ICQRunnableService extends Thread {
		private Socket socket = new Socket();
		private String host;
		private int port;

		private short flapSeqNumber = 0;

		public void setFlapSeqNumber(short number) {
			this.flapSeqNumber = number;
		}

		public short getFlapSeqNumber() {
			if (flapSeqNumber >= 0x8000) {
				flapSeqNumber = 0;
			}
			;
			return flapSeqNumber++;
		}

		public ICQRunnableService(String host, int port) {
			this.host = host;
			this.port = port;
			setName("ICQ runnable " + un);
		}

		@Override
		public void run() {
			flapSeqNumber = ProtocolUtils.getAtomicShort();
			try {
				if (socket.isConnected()) {
					socket.close();
				}
				// socket = new Socket();
				// socket.setSoTimeout(300000);
				socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
				getDataFromSocket();
			} catch (UnknownHostException e) {
				serviceResponse.respond(ICQServiceResponse.RES_NOTIFICATION, "host not found!");
				new Timer().schedule(new ErrorTimer(), 5000);
				log(e);
			} catch (IOException e) {
				serviceResponse.respond(ICQServiceResponse.RES_NOTIFICATION, "connection error!");
				new Timer().schedule(new ErrorTimer(), 5000);
				log(e);
			}
		}

		private void getDataFromSocket() {
			byte[] tail = null;
			int read = 0;
			int tailLength = 0;

			while (!isInterrupted() && socket != null && socket.isConnected() && !socket.isClosed()) {
				try {
					InputStream is = socket.getInputStream();
					if (is.available() > 0) {
						Thread.sleep(200);

						byte[] head = new byte[6];

						is.read(head, 0, 6);

						tailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(head, 4));

						tail = new byte[6 + tailLength];
						System.arraycopy(head, 0, tail, 0, 6);
						read = 0;
						while (read < tailLength) {
							read += is.read(tail, 6 + read, tailLength - read);
						}
						log("Got " + ProtocolUtils.getSpacedHexString(tail));

						try {
							Flap flap = dataParser.parseFlap(tail);

							synchronized (packets) {
								packets.add(flap);
							}
						} catch (Exception e) {
							log(e);
						}
						new Thread("ICQ packet processor") {
							@Override
							public void run() {
								try {
									forceFlapProcess();
								} catch (Exception e) {
									log(e);
								}
							}
						}.start();

						tail = null;
					} else {
						Thread.sleep(1000);
					}
				} catch (IOException e) {
					log(e);
					new Thread("icq disconnection") {
						@Override
						public void run() {
							disconnect();
						}
					}.start();
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					log(e);
				}
			}
			log("disconnected");
		}

		public synchronized boolean sendToSocket(Flap flap) {
			try {
				if (flap == null) {
					throw new ICQException("Flap to send is Null!");
				}

				OutputStream os = socket.getOutputStream();
				flap.sequenceNumber = getFlapSeqNumber();
				byte[] out = dataParser.flap2Bytes(flap);

				if (flap.channel != 1) {
					log("To be sent " + out.length + " -> " + ProtocolUtils.getSpacedHexString(out));
				} else {
					log("smth secret to be sent");
				}

				os.write(out);

				// checkForKeepaliveTimer();
			} catch (NullPointerException e) {
				log(e);
				return false;
			} catch (IOException e) {
				log(e);
				disconnect();
				return false;
			} catch (ICQException e) {
				log(e);
				return false;
			}
			return true;
		}

		public synchronized boolean sendMultipleToSocket(Flap[] flaps) {

			try {
				OutputStream os = socket.getOutputStream();
				for (Flap p : flaps) {
					if (p == null) {
						continue;
					}
					p.sequenceNumber = getFlapSeqNumber();
				}
				byte[] out = dataParser.flaps2Bytes(flaps);
				log("To be sent " + out.length + " -> " + ProtocolUtils.getSpacedHexString(out));

				os.write(out);

				// checkForKeepaliveTimer();
			} catch (IOException e) {
				interrupt();
				log(e);
			} catch (ICQException e) {
				log(e);
			}
			return true;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public void disconnect() {
			log("attempt disconnect " + currentState + ((lastConnectionError != null) ? lastConnectionError : ""));
			// closeKeepaliveTimerThread();
			if (processor != null) {
				processor.onDisconnect();
			}
			if (socket != null && !socket.isClosed()) {
				try {
					socket.close();
					runnableService.interrupt();
				} catch (IOException e) {
					log(e);
				}
			}
			if (fileTransferEngine != null) {
				fileTransferEngine.cancelAll();
			}
			if (buddyIconEngine != null) {
				buddyIconEngine.disconnect();
			}
			if (currentState != STATE_CONNECTING_BOS) {
				if (lastConnectionError != null) {
					serviceResponse.respond(ICQServiceResponse.RES_DISCONNECTED, lastConnectionError);
					lastConnectionError = null;
				} else {
					serviceResponse.respond(ICQServiceResponse.RES_DISCONNECTED);
				}
				setCurrentState(STATE_DISCONNECTED);
			}
		}

		class ErrorTimer extends TimerTask {

			@Override
			public void run() {
				disconnect();
			}
		}
	}

	public ICQRunnableService getRunnableService() {
		return runnableService;
	}

	public void log(String string) {
		serviceResponse.respond(ICQServiceResponse.RES_LOG, string);
	}

	public void log(Exception e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.toString());
		for (StackTraceElement el : e.getStackTrace()) {
			sb.append("\n" + el);
		}
		log(sb.toString());
	}

	public void setServerSupportedFamilies(short[] serverSupportedFamilies) {
		this.serverSupportedFamilies = serverSupportedFamilies;
	}

	public short[] getServerSupportedFamilies() {
		return serverSupportedFamilies;
	}

	public void setRunnableService(ICQRunnableService runnableService) {
		this.runnableService = runnableService;
	}

	public void setRateLimits(RateLimit[] rateLimits) {
		this.rateLimits = rateLimits;
	}

	public RateLimit[] getRateLimits() {
		return rateLimits;
	}

	public ICQDataParser getDataParser() {
		return dataParser;
	}

	public void setDataParser(ICQDataParser dataParser) {
		this.dataParser = dataParser;
	}

	public void setOnlineInfo(ICQOnlineInfo onlineInfo) {
		this.onlineInfo = onlineInfo;
	}

	public ICQOnlineInfo getOnlineInfo() {
		return onlineInfo;
	}

	public void setMessageParams(ICBMParams messageParams) {
		this.messageParams = messageParams;
	}

	public ICBMParams getMessageParams() {
		return messageParams;
	}

	public void setMaxVisibleListLength(int maxVisibleListLength) {
		this.maxVisibleListLength = maxVisibleListLength;
	}

	public int getMaxVisibleListLength() {
		return maxVisibleListLength;
	}

	public void setMaxInvisibleListLength(int maxInvisibleListLength) {
		this.maxInvisibleListLength = maxInvisibleListLength;
	}

	public int getMaxInvisibleListLength() {
		return maxInvisibleListLength;
	}

	public void setSSILimits(TLV[] ssiLimits) {
		this.ssiLimits = ssiLimits;
	}

	public TLV[] getSSILimits() {
		return ssiLimits;
	}

	public void setBuddyList(List<ICQBuddy> buddies, List<ICQBuddyGroup> buddyGroups, Map<String, Short> permitList, Map<String, Short> denyList, List<ICQBuddy> notAuthList, List<Short> existingIDs) {
		buddyList.buddyList.clear();
		buddyList.buddyList.addAll(buddies);

		buddyList.buddyGroupList.clear();
		buddyList.buddyGroupList.addAll(buddyGroups);

		buddyList.permitList = permitList;
		buddyList.denyList = denyList;
		buddyList.notAuthList = notAuthList;

		buddyList.existingIDs.clear();
		buddyList.existingIDs.addAll(existingIDs);

		serviceResponse.respond(ICQServiceResponse.RES_CLUPDATED, buddies, buddyGroups);
	}

	public ICQBuddyList getBuddyList() {
		return buddyList;
	}

	public PersonalInfoEngine getPersonalInfoEngine() {
		if (personalInfoEngine == null) {
			personalInfoEngine = new PersonalInfoEngine(this);
		}
		return personalInfoEngine;
	}

	public boolean checkFileTransferEngineCreated() {
		return fileTransferEngine != null;
	}

	public FileTransferEngine getFileTransferEngine() {
		if (fileTransferEngine == null) {
			fileTransferEngine = new FileTransferEngine(this);
		}
		return fileTransferEngine;
	}

	public AtomicInteger getIntCounter() {
		return intCounter;
	}

	public SSIProcessor getSSIEngine() {
		if (ssiEngine == null) {
			ssiEngine = new SSIProcessor(this);
		}
		return ssiEngine;
	}

	public void setMessagingEngine(ICBMMessagingEngine messagingEngine) {
		this.messagingEngine = messagingEngine;
	}

	public ICBMMessagingEngine getMessagingEngine() {
		if (messagingEngine == null) {
			messagingEngine = new ICBMMessagingEngine(this);
		}
		return messagingEngine;
	}

	public void setOnlineInfoEngine(OnlineInfoEngine onlineInfoEngine) {
		this.onlineInfoEngine = onlineInfoEngine;
	}

	public OnlineInfoEngine getOnlineInfoEngine() {
		if (onlineInfoEngine == null) {
			onlineInfoEngine = new OnlineInfoEngine(this);
		}
		return onlineInfoEngine;
	}

	public void setBuddyIconEngine(BuddyIconEngine buddyIconEngine) {
		this.buddyIconEngine = buddyIconEngine;
	}

	public BuddyIconEngine getBuddyIconEngine() {
		if (buddyIconEngine == null) {
			buddyIconEngine = new BuddyIconEngine(this);
		}
		return buddyIconEngine;
	}

	@SuppressWarnings("unchecked")
	public Object request(short action, final Object... args) throws ICQException {
		switch (action) {
		case REQ_KEEPALIVE_CHECK:
			if (getCurrentState() == STATE_CONNECTED) {
				((MainProcessor) processor).checkServerConnection();
			}
			break;
		case REQ_GETSHORTBUDDYINFO:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				getPersonalInfoEngine().getShortPersonalMetainfo((String) args[0]);
			}
			break;
		case REQ_GETFULLBUDDYINFO:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				try {
					getPersonalInfoEngine().getFullPersonalMetainfo((String) args[0]);
				} catch (Exception e) {
					serviceResponse.respond(ICQServiceResponse.RES_NOTIFICATION, "Error getting info");
				}
			}
			break;
		case REQ_ADDGROUP:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				ICQBuddyGroup group = (ICQBuddyGroup) args[0];
				group.groupId = generateNewItemId();
				getSSIEngine().addGroup((ICQBuddyGroup) args[0]);
			}
			break;
		case REQ_ADDBUDDY:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				ICQBuddy buddy = (ICQBuddy) args[0];
				buddy.itemId = generateNewItemId();
				ICQBuddyGroup group = buddyList.findGroupById(buddy.groupId);
				getSSIEngine().addBuddyToContactList(buddy, group, false);
			}
			break;
		case REQ_REMOVEBUDDY:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				getSSIEngine().removeBuddyFromContactList((ICQBuddy) args[0]);
			}
			break;
		case REQ_MOVEBUDDIES:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				getSSIEngine().moveBuddies((List<ICQBuddy>) args[0], (ICQBuddyGroup) args[1], (ICQBuddyGroup) args[2]);
			}
			break;
		case REQ_REMOVEBUDDIES:
			if (getCurrentState() == STATE_CONNECTED && args.length > 0) {
				getSSIEngine().removeBuddies((List<ICQBuddy>) args[0]);
			}
			break;
		case REQ_SETSTATUS:
			if (getCurrentState() == STATE_CONNECTED) {
				int status = (Integer) args[0];
				if (args.length < 2) {
					onlineInfo.userStatus = status;
					((MainProcessor) processor).sendXStatusChange(null, onlineInfo.extendedStatusId, onlineInfo.personalText, onlineInfo.extendedStatus);
				} else {
					byte[] qipStatus = (byte[]) args[1];
					if (qipStatus != null) {
						onlineInfo.userStatus = ICQConstants.STATUS_ONLINE;
						onlineInfo.qipStatus = qipStatus;
						((MainProcessor) processor).sendXStatusChange(qipStatus, onlineInfo.extendedStatusId, onlineInfo.personalText, onlineInfo.extendedStatus);
					}
				}
			}
			break;
		case REQ_SETEXTENDEDSTATUS:
			if (getCurrentState() == STATE_CONNECTED) {
				ICQOnlineInfo nfo = (ICQOnlineInfo) args[0];
				onlineInfo.extendedStatusId = nfo.extendedStatusId;
				onlineInfo.extendedStatus = nfo.extendedStatus;
				onlineInfo.personalText = nfo.personalText;
				onlineInfo.qipStatus = nfo.qipStatus;

				((MainProcessor) processor).sendXStatusChange(onlineInfo.qipStatus, onlineInfo.extendedStatusId, onlineInfo.personalText, onlineInfo.extendedStatus);
			}
			break;
		case REQ_AUTHREQUEST:
			if (getCurrentState() != STATE_CONNECTED) {
				throw new ICQException("Enter the network first");
			}
			getSSIEngine().sendAuthorizationRequest((String) args[0], (String) args[1]);
			break;
		case REQ_AUTHRESPONSE:
			if (getCurrentState() != STATE_CONNECTED) {
				throw new ICQException("Enter the network first");
			}
			getSSIEngine().sendAuthorizationReply((String) args[0], (Boolean) args[1]);
			break;
		case REQ_SEARCHFORBUDDY:
			if (getCurrentState() != STATE_CONNECTED) {
				throw new ICQException("Enter the network first");
			}
			
			Map<String, String> map = (Map<String, String>) args[0];
			
			if (map.containsKey(ICQConstants.SEARCHPARAM_UIN)) {
				getPersonalInfoEngine().sendSearchByUinRequest(map.get(ICQConstants.SEARCHPARAM_UIN));
			}
			break;
		case REQ_DISCONNECT:
			log("disconnect direct request");
			if (buddyIconEngine != null) {
				buddyIconEngine.disconnect();
			}
			if (runnableService != null) {
				runnableService.disconnect();
			}
			break;
		case REQ_CONNECT:
			un = (String) args[0];
			pw = (String) args[1];
			
			// ICQ does not support passwords longer than 8 symbols
			if (pw != null && pw.length() > 8) {
				pw = pw.substring(0, 8);
			}

			if (args[2] != null) {
				loginHost = (String) args[2];
			}

			if (args[3] != null) {
				loginPort = Integer.parseInt(((String) args[3]).trim().replace("\n", ""));
			}

			onlineInfo = (ICQOnlineInfo) args[4];

			connectInternal((Boolean)args[5]);
			break;
		case REQ_GETCONTACTLIST:
			break;
		case REQ_GETEXTENDEDSTATUS:
			getMessagingEngine().askForXStatus((String) args[0]);
			break;
		case REQ_RENAMEBUDDY:
			getSSIEngine().modifyBuddy((ICQBuddy) args[0]);
			break;
		case REQ_RENAMEGROUP:
			getSSIEngine().modifyGroup((ICQBuddyGroup) args[0]);
			break;
		case REQ_MOVEBUDDY:
			moveBuddy((ICQBuddy) args[0]);
			break;
		case REQ_EDITBUDDY:
			ICQBuddy newBuddy = (ICQBuddy) args[0];
			ICQBuddy oldBuddy = buddyList.findBuddyByUin(newBuddy.uin);

			if (!newBuddy.screenName.equals(oldBuddy.screenName)) {
				getSSIEngine().modifyBuddy(newBuddy);
			}

			if (newBuddy.groupId != oldBuddy.groupId) {
				moveBuddy(newBuddy);
			}

			break;
		case REQ_GETGROUPLIST:
			break;
		case REQ_SENDMESSAGE:
			if (getCurrentState() == STATE_CONNECTED) {
				try {
					return getMessagingEngine().sendMessage((ICBMMessage) args[0]);
				} catch (Exception e) {
					serviceResponse.respond(ICQServiceResponse.RES_NOTIFICATION, "Error sending message");
				}
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_GETICON:
			if (getCurrentState() != STATE_DISCONNECTED) {
				Executors.defaultThreadFactory().newThread(new Runnable() {

					@Override
					public void run() {
						getBuddyIconEngine().requestIcon((String) args[0]);
					}
				}).start();
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_REMOVEGROUP:
			if (getCurrentState() == STATE_CONNECTED) {
				getSSIEngine().removeGroup((ICQBuddyGroup) args[0]);
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_FILERESPOND:
			if (getCurrentState() == STATE_CONNECTED) {
				new Thread("File accept response") {
					@Override
					public void run() {
						getFileTransferEngine().fileReceiveResponse((Long) args[0], (Boolean) args[1]);
					}
				}.start();
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_SENDFILE:
			if (getCurrentState() == STATE_CONNECTED) {
				try {
					return getFileTransferEngine().sendFiles((ICBMMessage) args[0]);
				} catch (Exception e) {
					serviceResponse.respond(ICQServiceResponse.RES_NOTIFICATION, "Error sending message");
				}
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_FILECANCEL:
			if (getCurrentState() == STATE_CONNECTED) {
				getFileTransferEngine().cancel((Long) args[0]);
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_SENDTYPING:
			if (getCurrentState() == STATE_CONNECTED) {
				getMessagingEngine().sendTyping((String) args[0]);
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_BUDDYVISIBILITY:
			if (getCurrentState() == STATE_CONNECTED) {
				ICQBuddy buddy = buddyList.findBuddyByUin((String) args[0]);
				buddy.visibility = (Byte) args[1];
				getSSIEngine().modifyVisibility(buddy);
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_ACCOUNTVISIBILITY:
			if (getCurrentState() == STATE_CONNECTED) {
				byte vis = (Byte) args[0];
				onlineInfo.visibility = vis;
				getSSIEngine().modifyMyVisibility(onlineInfo);
			} else {
				throw new ICQException("You should enter the network first");
			}
			break;
		case REQ_UPLOADICON:
			if (getCurrentState() == STATE_CONNECTED) {
				getSSIEngine().requestIconUpload((byte[]) args[0]);
			} else {
				throw new ICQException("You should enter the network first");
			}
			// getBuddyIconEngine().requestIconUpload((byte[]) args[0]);
			break;
		}
		return null;
	}

	public void connectInternal(boolean isSecureLogin) throws ICQException {
		processor = new AuthenticationProcessor();
		processor.init(this);
		((AuthenticationProcessor) processor).isSecureLogin = isSecureLogin;

		setCurrentState(STATE_CONNECTING_LOGIN);
		serviceResponse.respond(ICQServiceResponse.RES_CONNECTING, 1);
		runService(loginHost, loginPort);
	}

	private void moveBuddy(ICQBuddy newBuddy) {
		ICQBuddy oldBuddy = buddyList.findBuddyByUin(newBuddy.uin);
		ICQBuddyGroup oldGroup = buddyList.findGroupById(oldBuddy.groupId);
		ICQBuddyGroup newGroup = buddyList.findGroupById(newBuddy.groupId);

		getSSIEngine().moveBuddy(newBuddy, oldGroup, newGroup);
	}

	public ICQServiceResponse getServiceResponse() {
		return serviceResponse;
	}

	public String getLoginHost() {
		return loginHost;
	}

	public void setLoginHost(String loginHost) {
		this.loginHost = loginHost;
	}

	public int getLoginPort() {
		return loginPort;
	}

	public void setLoginPort(int loginPort) {
		this.loginPort = loginPort;
	}

	/*
	 * public int getPingTimeout() { return pingTimeout; }
	 * 
	 * public void setPingTimeout(int pingTimeout) { this.pingTimeout =
	 * pingTimeout; }
	 */

	public String getUn() {
		return un;
	}

	public void setUn(String un) {
		this.un = un;
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

	public void setCurrentState(byte currentState) {
		this.currentState = currentState;
	}

	public byte getCurrentState() {
		return currentState;
	}

	public IFlapProcessor getProcessor() {
		return processor;
	}

	public byte[] getInternalIp() {
		return runnableService.socket.getLocalAddress().getAddress();
	}

	public short generateNewItemId() {
		short ssiItemId = 0;
		Random r = new Random();
		do {
			ssiItemId = (short) r.nextInt(0x8000);
		} while (buddyList.existingIDs.contains(ssiItemId));

		return ssiItemId;
	}
}
