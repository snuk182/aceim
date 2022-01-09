package aceim.protocol.snuk182.mrim;

import java.util.Arrays;
import java.util.List;

import aceim.api.IProtocol;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolException.Cause;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;
import aceim.protocol.snuk182.mrim.inner.MrimConstants;
import aceim.protocol.snuk182.mrim.inner.MrimException;
import aceim.protocol.snuk182.mrim.inner.MrimServiceInternal;
import aceim.protocol.snuk182.mrim.inner.MrimServiceResponse;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimBuddy;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimFileTransfer;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimGroup;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimMessage;
import aceim.protocol.snuk182.mrim.inner.dataentity.MrimOnlineInfo;
import aceim.protocol.snuk182.mrim.utils.ProtocolUtils;
import aceim.protocol.snuk182.mrim.utils.ResourceUtils;
import android.content.Context;

public class MrimService extends AccountService {

	public MrimService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}

	@Override
	protected ConnectionState getCurrentState() {
		return MrimEntityAdapter.mrimConnectionState2ConnectionState(internal.getCurrentState());
	}

	@Override
	protected void keepaliveRequest() {
		internal.askForWebAuthKey();
	}

	@Override
	protected void timeoutReconnect() {
		internal.getRunnableService().disconnect();	
		internal.connectInternal();
	}

	@Override
	public IProtocol getProtocol() {
		return protocol;
	}

	private final IProtocol protocol = new IProtocol(){

		@Override
		public void buddyAction(ItemAction arg0, Buddy arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void buddyGroupAction(ItemAction arg0, BuddyGroup arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void cancelFileFransfer(long messageId) {
			try {
				internal.request(MrimServiceInternal.REQ_FILECANCEL, messageId);
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}			
		}

		@Override
		public void connect(OnlineInfo arg0) {
			sendKeepalive();
			try {
				connectInternal(arg0);
			} catch (ProtocolException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public void disconnect() {
			try {
				internal.request(MrimServiceInternal.REQ_DISCONNECT);
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public void joinChatRoom(String arg0, boolean arg1) {}

		@Override
		public void leaveChatRoom(String arg0) {}

		@Override
		public void messageResponse(Message message, boolean accept) {
			try {
				if (message instanceof FileMessage) {
					internal.request(MrimServiceInternal.REQ_FILERESPOND, message.getMessageId(), accept);
				} else if (message instanceof ServiceMessage) {
					if (message.getContactDetail().equals(getContext().getString(R.string.ask_authorization))) {
						internal.request(MrimServiceInternal.REQ_AUTHRESPONSE, message.getContactUid(), accept);
					}
				}
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public void removeAccountPhoto() {}

		@Override
		public void requestFullInfo(String arg0, boolean shortInfo) {
			try {
				if (shortInfo){
					internal.request(MrimServiceInternal.REQ_GETSHORTBUDDYINFO, arg0);
				} else {
					internal.request(MrimServiceInternal.REQ_GETFULLBUDDYINFO, arg0);
				}
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public void requestIcon(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public long sendMessage(Message message) {
			try {
				Object result;
				if (message instanceof TextMessage) {
					MrimMessage mrimMessage = MrimEntityAdapter.textMessage2MrimMessage(getProtocolUid(), (TextMessage) message);
					result = mrimMessage.messageId = ResourceUtils.RANDOM.nextInt();
					internal.request(MrimServiceInternal.REQ_SENDMESSAGE, mrimMessage);
				} else if (message instanceof FileMessage) {
					result = ResourceUtils.RANDOM.nextInt();
					internal.request(MrimServiceInternal.REQ_SENDFILE, message.getContactUid(), MrimEntityAdapter.getFilesFromFileMessage((FileMessage)message), (Integer)result);
				} else {
					result = null;
				}

				if (result instanceof Long) {
					return (Long) result;
				} else {
					return 0;
				}
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
				return 0;
			}
		}

		@Override
		public void sendTypingNotification(String recipient) {
			try {
				internal.request(MrimServiceInternal.REQ_SENDTYPING, recipient);
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
			
		}

		@Override
		public void setFeature(String featureId, OnlineInfo info) {
			// This helps parsing Bundles
			info.getFeatures().setClassLoader(TKV.class.getClassLoader());

			try {
				if (featureId.equals(ApiConstants.FEATURE_STATUS) || featureId.equals(ApiConstants.FEATURE_XSTATUS)) {
					byte status = info.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1);
					byte xstatus = info.getFeatures().getByte(ApiConstants.FEATURE_XSTATUS, (byte) -1);
					
					boolean isChat = MrimApiConstants.STATUS_FREE4CHAT == status;
					int mstatus = (isChat || (xstatus > -1)) ? MrimConstants.STATUS_OTHER : MrimEntityAdapter.userStatus2MrimUserStatus(status);
					String mxstatus = isChat ? "STATUS_CHAT" : MrimEntityAdapter.userXStatus2MrimXStatus(xstatus);
					internal.request(MrimServiceInternal.REQ_SETSTATUS, mstatus, mxstatus, info.getXstatusName(), info.getXstatusDescription());
				} /*else if (featureId.equals(IcqApiConstants.FEATURE_BUDDY_VISIBILITY)) {
					byte value = info.getFeatures().getByte(featureId, (byte) 0);
					internal.request(ICQServiceInternal.REQ_BUDDYVISIBILITY, info.getProtocolUid(), value < 0 ? ICQConstants.VIS_REGULAR : ICQEntityAdapter.BUDDY_VISIBILITY_MAPPING[value]);
				} */
			} catch (MrimException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public void uploadAccountPhoto(String arg0) {}
		
	};
	
	private MrimServiceResponse mrimResponse = new MrimServiceResponse(){

		@SuppressWarnings("unchecked")
		@Override
		public Object respond(short action, Object... args) {
			switch (action) {
			case MrimServiceResponse.RES_GET_FILE_FOR_SAVING:
				return Utils.createLocalFileForReceiving((String) args[0], new Buddy((String) args[1], getProtocolUid(), ResourceUtils.PROTOCOL_NAME, getServiceId()), (Long) args[2]);
			case MrimServiceResponse.RES_KEEPALIVE:
				resetHeartbeat();
				break;
			case MrimServiceResponse.RES_LOG:
				Logger.log((String)args[0], LoggerLevel.INFO);
				break;
			case MrimServiceResponse.RES_CONNECTED:
				getCoreService().connectionStateChanged(ConnectionState.CONNECTED, -1);
				break;
			case MrimServiceResponse.RES_DISCONNECTED:
				closeKeepaliveThread();
				System.gc();
				if (args.length > 0){
					Logger.log((String) args[0], LoggerLevel.INFO);
					getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, Cause.CONNECTION_ERROR.ordinal());
				} else {
					getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, -1);
				}
				break;
			case MrimServiceResponse.RES_SAVEIMAGEFILE:
				getCoreService().iconBitmap((String) args[1], (byte[]) args[0], (String) args[2]);
				break;
			case MrimServiceResponse.RES_CLUPDATED:
				List<MrimBuddy> mrimBuddies = (List<MrimBuddy>) args[0];
				List<MrimGroup> mrimGroups = (List<MrimGroup>) args[1];
				getCoreService().buddyListUpdated(MrimEntityAdapter.mrimBuddyGroupList2BuddyGroupList(ResourceUtils.PROTOCOL_NAME, mrimGroups, internal.getMrid(), getServiceId(), mrimBuddies));
				break;
			case MrimServiceResponse.RES_MESSAGE:
				TextMessage txtmessage = MrimEntityAdapter.mrimMessage2TextMessage((MrimMessage) args[0], getServiceId());
				
				getCoreService().message(txtmessage);
				break;
			case MrimServiceResponse.RES_BUDDYSTATECHANGED:	
				OnlineInfo oi = MrimEntityAdapter.mrimOnlineInfo2OnlineInfo((MrimOnlineInfo) args[0], getServiceId());
				getCoreService().buddyStateChanged(Arrays.asList(oi));
				break;
			case MrimServiceResponse.RES_CONNECTING:
				getCoreService().connectionStateChanged(ConnectionState.CONNECTING, (Integer) args[0]);
				break;
			case MrimServiceResponse.RES_FILEMESSAGE:
				getCoreService().message(MrimEntityAdapter.mrimFileTransferMessage2FileMessage((MrimFileTransfer) args[0], getServiceId()));
				break;
			case MrimServiceResponse.RES_NOTIFICATION:
				getCoreService().notification((String) args[0]);
				break;
			case MrimServiceResponse.RES_ACCOUNTUPDATED:
				getCoreService().accountStateChanged(MrimEntityAdapter.mrimOnlineInfo2OnlineInfo((MrimOnlineInfo) args[0], getServiceId()));
				break;
			case MrimServiceResponse.RES_AUTHREQUEST:
				getCoreService().message(MrimEntityAdapter.authRequestToServiceMessage(getServiceId(), (String) args[0], (String) args[1], getContext()));
				break;
			case MrimServiceResponse.RES_FILEPROGRESS:
				FileProgress fp = new FileProgress(getServiceId(), ProtocolUtils.bytes2LongBE((byte[]) args[0], 0), (String) args[1], (Long) args[2], (Long) args[3], (Boolean) args[4], (String) args[6], (String) args[5]);
				getCoreService().fileProgress(fp);
				break;
			case MrimServiceResponse.RES_MESSAGEACK:
				getCoreService().messageAck((String) args[0], (Long) args[1], MrimEntityAdapter.mrimMessageAck2MessageAck((Byte) args[2]));
				break;
			case MrimServiceResponse.RES_TYPING:
				getCoreService().typingNotification((String) args[0]);
				break;
			}	
			return null;
		}
		
	};
	
	private MrimServiceInternal internal = new MrimServiceInternal(mrimResponse);

	private void connectInternal(OnlineInfo info) throws ProtocolException {
		String username = getCoreService().requestPreference(ResourceUtils.KEY_USERNAME);
		String password = getCoreService().requestPreference(ResourceUtils.KEY_PASSWORD);
		String host = getCoreService().requestPreference(ResourceUtils.KEY_LOGIN_HOST);
		String port = getCoreService().requestPreference(ResourceUtils.KEY_LOGIN_PORT);
		String ping = getCoreService().requestPreference(ResourceUtils.KEY_PING_TIMEOUT);
		
		if (ping != null){
			try {
				pingTimeout = Integer.parseInt(ping);
			} catch (Exception e) {}
		}
		
		if (username == null || password == null){
			throw new ProtocolException(Cause.BROKEN_AUTH_DATA);
		}	
		
		int status = MrimEntityAdapter.userStatus2MrimUserStatus(info.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1));
		int xstatus = MrimEntityAdapter.userStatus2MrimUserStatus(info.getFeatures().getByte(ApiConstants.FEATURE_XSTATUS, (byte) -1));		
		
		try {
			internal.request(MrimServiceInternal.REQ_CONNECT, username, password, host, port, status, xstatus, info.getXstatusName(), info.getXstatusDescription());
		} catch (MrimException e) {
			Logger.log(e);
			getCoreService().notification(e.getLocalizedMessage());
		}	
	}
}
