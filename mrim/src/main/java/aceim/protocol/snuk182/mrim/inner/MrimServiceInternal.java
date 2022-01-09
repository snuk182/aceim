package aceim.protocol.snuk182.mrim.inner;

import java.io.File;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import aceim.protocol.snuk182.mrim.inner.dataentity.MrimMessage;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimOnlineInfo;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimPacket;
import aceim.protocol.snuk182.mrim.utils.ProtocolUtils;


public final class MrimServiceInternal {
	
	public static final short STATE_DISCONNECTED = 0;
	public static final short STATE_CONNECTING_LOGIN = 1;
	public static final short STATE_CONNECTING_BOS = 2;
	public static final short STATE_AUTHENTICATING = 3;
	public static final short STATE_CONNECTED = 4;
	
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
	public static final short REQ_SEARCHFORBUDDY_BY_UID = 15;
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
	
	private String loginHost = "mrim.mail.ru";
	private int loginPort = 2042;
	
	private String mrid;
	private String pw;
	//private byte[] internalIp = new byte[]{0,0,0,0};
	
	private MrimServiceResponse serviceResponse;
	
	private MrimRunnableService runnableService;
	private final List<MrimPacket> packets = Collections.synchronizedList(new ArrayList<MrimPacket>());
	MrimProcessor processor = new MrimProcessor(this);
	
	private short currentState = STATE_DISCONNECTED;
	//private int currentStatus = MrimConstants.STATUS_ONLINE;
	private final MrimOnlineInfo onlineInfo = new MrimOnlineInfo(); 
	public String lastConnectionError = null;
	private long pingFrequency = 120;
	private FileTransferEngine fileTransferEngine;
	
	public MrimServiceInternal(){
		onlineInfo.status = MrimConstants.STATUS_ONLINE;
		onlineInfo.xstatusId = "STATUS_ONLINE";
		onlineInfo.xstatusName = "x-name";
		onlineInfo.xstatusText = "x-text";
	}
	
	public MrimServiceInternal(MrimServiceResponse response){
		this.serviceResponse = response;
	}
	
	public void runService(String host, int port){
		if (runnableService!=null){
			runnableService.connected = false;
		}
		runnableService = new MrimRunnableService(host, port);
		runnableService.start();
	}
	
	public class MrimRunnableService extends Thread{
		private Socket socket;
		private String host;
		private int port;
		
		private volatile boolean connected = true;
		private int flapSeqNumber = 0;
		
		public int getFlapSeqNumber() {
			if (flapSeqNumber >=0xffffffff){
				flapSeqNumber = 0;
			};
			return flapSeqNumber++;
		}

		public MrimRunnableService(String host, int port){
			this.host = host;
			this.port = port;
			setName("MRIM runnable "+mrid+" "+host);
		}
		
		public void runService(String host, int port){
			if (runnableService!=null){
				runnableService.connected = false;
			}
			runnableService = new MrimRunnableService(host, port);
			runnableService.start();
		}
		
		protected void forcePacketProcess() throws Exception{
			while(packets.size()>0){
				synchronized (packets) {
					MrimPacket packet = packets.remove(0);
					processor.parsePacketTail(packet);
				}
			}
		}

		@Override
		public void run() {
			flapSeqNumber = ProtocolUtils.getAtomicShort();
			try {
				socket = new Socket();
				//socket.setSoTimeout(300000);
				socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
				connected = true;
				if (!host.equals(loginHost)){
					processor.sendHello();
				}
				getDataFromSocket();
			} catch (UnknownHostException e) {
				serviceResponse.respond(MrimServiceResponse.RES_NOTIFICATION, "host not found!");
				new Timer().schedule(new ErrorTimer(), 5000);				
				log(e);
			} catch (IOException e) {
				serviceResponse.respond(MrimServiceResponse.RES_NOTIFICATION, "connection error!");
				new Timer().schedule(new ErrorTimer(), 5000);				
				log(e);
			} 
		}
		
		private void getDataFromSocket(){
			byte[] tail = null;
			int read = 0;
			int tailLength = 0;
			
			while (connected && socket!=null && socket.isConnected() && !socket.isClosed()){
				try {
					InputStream is = socket.getInputStream();
					if (is.available()>0){
						Thread.sleep(200);
					
						byte[] head = new byte[44];
						
						read = is.read(head, 0, 44);
						
						if (currentState != STATE_CONNECTING_LOGIN){
							tailLength = (int) ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntLE(head, 16));					
							
							tail = new byte[44+tailLength];
							System.arraycopy(head, 0, tail, 0, 44);
							read = 0;
							while (read < tailLength){
								read = is.read(tail, 44+read, tailLength-read);
							}
							log("Got "+ProtocolUtils.getSpacedHexString(tail));
						} else {
							// omit newline char at the end
							tail = new byte[read-1];
							log("Got "+ProtocolUtils.getSpacedHexString(tail));
							System.arraycopy(head, 0, tail, 0, read-1);
						} 						
						try {
							MrimPacket packet = processor.parsePacket(tail);
							
							synchronized(packets){
								packets.add(packet);
							}
						} catch (Exception e) {
							log(e);
						}		
						new Thread("MRIM packet processor"){
							@Override
							public void run(){
								try {
									forcePacketProcess();
								} catch (Exception e) {
									log(e);
								}
							}
						}.start();
						tail = null;
					} else {
						Thread.sleep(1000);
					}				
				}catch(IOException e){
					log(e);
					new Thread("mrim disconnection"){
						@Override
						public void run(){
							disconnect();	
						}
					}.start();				
				}catch (Exception e) {
					log(e);
				} 
			}
			log(getName()+" disconnected");			
			connected = false;						
		}
		
		public synchronized long sendToSocket(MrimPacket packet){
			try {
				OutputStream os = socket.getOutputStream();
				packet.seqNumber = getFlapSeqNumber();
				byte[] out = processor.packet2Bytes(packet);
				
				if (currentState != STATE_AUTHENTICATING){
					log("To be sent "+ProtocolUtils.getSpacedHexString(out));
				} else {
					log("smth secret to be sent");
				}
				
				//log("To be sent "+ProtocolUtils.getSpacedHexString(out));
				
				os.write(out);
				
				return packet.seqNumber;
				//checkForKeepaliveTimer();
			} catch (NullPointerException e) {
				log(e);
			} catch (IOException e) {
				log(e);
				disconnect();
			} catch (MrimException e) {
				log(e);
			} 
			return 0;
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

		public void disconnect(){
			log("attempt disconnect "+currentState+ ((lastConnectionError!= null) ? lastConnectionError : ""));
			//closeKeepaliveTimerThread();
			if (socket!=null && !socket.isClosed()){
				try {
					socket.close();
					connected = false;
				} catch (IOException e) {
					log(e);
				}
			}
			/*if (fileTransferEngine != null){
				fileTransferEngine.cancelAll();
			}*/
			if (currentState != STATE_CONNECTING_BOS){
				if (lastConnectionError != null){
					serviceResponse.respond(MrimServiceResponse.RES_DISCONNECTED, lastConnectionError);
					lastConnectionError = null;
				} else {
					serviceResponse.respond(MrimServiceResponse.RES_DISCONNECTED);
				}
				setCurrentState(STATE_DISCONNECTED);
			}
		}	
		
		class ErrorTimer extends TimerTask{

			@Override
			public void run() {
				disconnect();			
			}		
		}
	}
	
	public void log(String string) {
		serviceResponse.respond(MrimServiceResponse.RES_LOG, string);				
	}
	
	public void log(Exception e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.toString());
		for (StackTraceElement el:e.getStackTrace()){
			sb.append("\n"+el);
		}
		log(sb.toString());		
	}

	

	public short getCurrentState() {
		return currentState;
	}

	public void setCurrentState(short currentState) {
		this.currentState = currentState;
	}

	public MrimRunnableService getRunnableService() {
		return runnableService;
	}

	public MrimServiceResponse getServiceResponse() {
		return serviceResponse;
	}
	
	@SuppressWarnings("unchecked")
	public Object request(short action, final Object... args) throws MrimException {
		switch(action){
		case REQ_CONNECT:
			mrid = (String) args[0];
			pw = (String) args[1];
			
			if (args[2] !=null){
				loginHost = (String) args[2];
			}
			
			if (args[3] != null){
				loginPort = Integer.parseInt(((String) args[3]).trim().replace("\n", ""));
			}
			
			onlineInfo.status = (Integer) args[4];
			onlineInfo.xstatusId = "STATUS_ONLINE"; //TODO edit
			onlineInfo.xstatusName = (String) args[6]!=null ? (String) args[6] : "";
			onlineInfo.xstatusText = (String) args[7]!=null ? (String) args[7] : "";
			
			connectInternal();		
			break;
		case REQ_DISCONNECT:
			log("disconnect direct request");
			closeKeepalive();
			if (runnableService != null){
				runnableService.disconnect();
			}
			break;
		case REQ_SENDMESSAGE:
			processor.sendMessage((MrimMessage)args[0]);
			break;
		case REQ_GETICON:
			processor.getIcon((String)args[0]);
			break;
		case REQ_SETSTATUS:
			onlineInfo.status = (Integer) args[0];
			onlineInfo.xstatusId = (String) args[1];
			onlineInfo.xstatusName = (String) args[2];
			onlineInfo.xstatusText = (String) args[3];
			processor.setStatus(onlineInfo);
			break;
		case REQ_SENDTYPING:
			processor.sendTyping((String)args[0]);
			break;
		case REQ_SENDFILE:
			final String buddyMrid = (String) args[0];
			final List<File> files = (List<File>) args[1];
			
			return getFileTransferEngine().sendFiles(buddyMrid, files, runnableService.socket.getLocalAddress().getAddress(), (Integer)args[2]);
		case REQ_FILERESPOND:
			
			final byte[] intIp;
			if (args.length > 2){
				intIp = (byte[]) args[2];
			} else {
				intIp = new byte[]{127,0,0,1};
			}
			
			new Thread("File accept response"){
				@Override
				public void run(){
					getFileTransferEngine().fileReceiveResponse((Long)args[0], (Boolean)args[1], intIp);
				}
			}.start();
			break;
		}
		return null;
	}

	public void connectInternal() {
		setCurrentState(STATE_CONNECTING_LOGIN);
		serviceResponse.respond(MrimServiceResponse.RES_CONNECTING, 1);
		runService(loginHost, loginPort);	
	}

	public FileTransferEngine getFileTransferEngine() {
		if (fileTransferEngine == null){
			fileTransferEngine = new FileTransferEngine(this);
		}
		
		return fileTransferEngine;
	}

	public String getLoginHost() {
		return loginHost;
	}

	public int getLoginPort() {
		return loginPort;
	}

	public String getMrid() {
		return mrid;
	}

	public String getPw() {
		return pw;
	}

	public void setPingFrequency(long pingFreq) {
		this.pingFrequency  = pingFreq;		
	}

	public long getPingFrequency() {
		return pingFrequency;
	}
	
	//keepalive timer, required for mrim :(:(
	
	private ScheduledFuture<?> task;
	
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
	
	private Runnable keepaliveRunnable = new Runnable(){

		@Override
		public void run() {
			processor.sendKeepalive();
			startKeepalive();
		}
		
	};
	
	public void closeKeepalive(){
		if (task != null){
			task.cancel(false);
		}
	}
	
	public void startKeepalive(){
		task = executor.schedule(keepaliveRunnable, pingFrequency , TimeUnit.SECONDS);
	}

	public MrimOnlineInfo getOnlineInfo() {
		return onlineInfo;
	}

	public void askForWebAuthKey() {
		processor.askForWebAuthKey();
	}
}
