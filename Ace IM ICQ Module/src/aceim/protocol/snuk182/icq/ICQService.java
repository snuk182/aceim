package aceim.protocol.snuk182.icq;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolException.Cause;
import aceim.api.service.ProtocolService;
import aceim.api.utils.Logger;
import aceim.api.utils.Utils;

import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMMessage;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyGroup;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQOnlineInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQPersonalInfo;
import aceim.protocol.snuk182.icq.utils.Base64;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;
import aceim.protocol.snuk182.icq.utils.ResourceUtils;
import android.os.DeadObjectException;

public class ICQService extends AccountService {
	
	static final String SERVICE_NAME = "ICQ";
	
	private ICQServiceResponse icqResponse = new ICQServiceResponse(){

		@SuppressWarnings("unchecked")
		@Override
		public Object respond(short action, Object... args) {
			
			try{
				switch (action) {
				case ICQServiceResponse.RES_LOG:
					Logger.log((String)args[0]);
					break;
				case ICQServiceResponse.RES_CONNECTED:
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTED, 0);
					sendKeepalive();
					break;
				case ICQServiceResponse.RES_DISCONNECTED:
					closeKeepaliveThread();
					if (args.length > 0){
						//TODO fix reasons
						getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.DISCONNECTED, Cause.CONNECTION_ERROR.ordinal());						
					} else {
						getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.DISCONNECTED, -1);						
					}
					break;
				case ICQServiceResponse.RES_SAVEIMAGEFILE:
					getProtocolService().getCallback().iconBitmap(getServiceId(), (String)args[1], (byte[])args[0], Base64.encodeBytes((byte[]) args[2]));
					break;
				case ICQServiceResponse.RES_CLUPDATED:
					List<Buddy> buddyList = ICQEntityAdapter.ICQBuddyList2Buddylist((List<ICQBuddy>) args[0], getProtocolUid(), getServiceId());
					List<BuddyGroup> buddyGroupList = ICQEntityAdapter.ICQBuddyGroupList2BuddyGroupList((List<ICQBuddyGroup>) args[1], getProtocolUid(), getServiceId(), (List<ICQBuddy>) args[0], buddyList);
					getProtocolService().getCallback().buddyListUpdated(getServiceId(), buddyGroupList);
					break;
				case ICQServiceResponse.RES_KEEPALIVE:
					resetHeartbeat();
					break;
				case ICQServiceResponse.RES_MESSAGE:
					ICBMMessage msg = (ICBMMessage) args[0];
					
					if (msg.senderId.equals(getProtocolUid())) {
						resetHeartbeat();
						return null;
					}
					
					TextMessage txtmessage = ICQEntityAdapter.icbmMessage2TextMessage(msg, getServiceId());
					
					getProtocolService().getCallback().message(txtmessage);
					break;
				case ICQServiceResponse.RES_BUDDYSTATECHANGED:	
					OnlineInfo buddiOnlineInfo = ICQEntityAdapter.icqOnlineInfo2OnlineInfo((ICQOnlineInfo) args[0], getProtocolUid(), getServiceId());
					getProtocolService().getCallback().buddyStateChanged(buddiOnlineInfo);
					
					if (buddiOnlineInfo.getFeatures().getByte(ApiConstants.FEATURE_XSTATUS, (byte) -1) > -1 && args.length < 2) {
						internal.getMessagingEngine().askForXStatus(buddiOnlineInfo.getProtocolUid());
					}
					
					break;
				case ICQServiceResponse.RES_CONNECTING:
					getProtocolService().getCallback().connectionStateChanged(getServiceId(), ConnectionState.CONNECTING, (Integer) args[0]);
					break;					
				case ICQServiceResponse.RES_FILEMESSAGE:
					getProtocolService().getCallback().message(ICQEntityAdapter.icbmMessage2FileMessage((ICBMMessage)args[0], getServiceId()));
					break;
				case ICQServiceResponse.RES_NOTIFICATION:
					getProtocolService().getCallback().notification(getServiceId(), (String) args[0]);
					/*if (args.length > 1){
						return serviceResponse.respond(IAccountServiceResponse.RES_NOTIFICATION, getServiceId(), args[0], args[1]);
					} else {
						return serviceResponse.respond(IAccountServiceResponse.RES_NOTIFICATION, getServiceId(), args[0]);
					}*/
					break;
				case ICQServiceResponse.RES_ACCOUNTUPDATED:
					getProtocolService().getCallback().accountStateChanged(ICQEntityAdapter.icqOnlineInfo2OnlineInfo((ICQOnlineInfo) args[0], getProtocolUid(), getServiceId()));
					break;
				case ICQServiceResponse.RES_USERINFO:
					getProtocolService().getCallback().personalInfo(ICQEntityAdapter.icqPersonalInfo2PersonalInfo((ICQPersonalInfo) args[0], getProtocolService().getBaseContext(), getServiceId()));
					break;
				case ICQServiceResponse.RES_AUTHREQUEST:
					getProtocolService().getCallback().message(ICQEntityAdapter.authRequestToServiceMessage(getServiceId(), (String)args[0], (String)args[1], getProtocolService().getBaseContext()));
					break;
				case ICQServiceResponse.RES_SEARCHRESULT:
					getProtocolService().getCallback().searchResult(getServiceId(), ICQEntityAdapter.icqPersonalInfos2PersonalInfos((List<ICQPersonalInfo>) args[0], getProtocolService().getBaseContext(), getServiceId()));
					break;
				case ICQServiceResponse.RES_GROUPADDED:
					getProtocolService().getCallback().groupAction(ItemAction.ADDED, ICQEntityAdapter.ICQBuddyGroup2BuddyGroup((ICQBuddyGroup) args[0], internal.getUn(), getServiceId(), internal.getBuddyList().buddyList, null));
					break;
				case ICQServiceResponse.RES_BUDDYADDED:
					getProtocolService().getCallback().buddyAction(ItemAction.ADDED, ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[0], internal.getUn(), getServiceId()));
					break;
				case ICQServiceResponse.RES_BUDDYDELETED:
					getProtocolService().getCallback().buddyAction(ItemAction.DELETED, ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[0], internal.getUn(), getServiceId()));
					break;
				case ICQServiceResponse.RES_GROUPDELETED:
					getProtocolService().getCallback().groupAction(ItemAction.DELETED, ICQEntityAdapter.ICQBuddyGroup2BuddyGroup((ICQBuddyGroup) args[0], internal.getUn(), getServiceId(), internal.getBuddyList().buddyList, null));
					break;
				case ICQServiceResponse.RES_BUDDYMODIFIED:
					getProtocolService().getCallback().buddyAction(ItemAction.MODIFIED, ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[0], internal.getUn(), getServiceId()));
					break;
				case ICQServiceResponse.RES_GROUPMODIFIED:
					getProtocolService().getCallback().groupAction(ItemAction.MODIFIED, ICQEntityAdapter.ICQBuddyGroup2BuddyGroup((ICQBuddyGroup) args[0], internal.getUn(), getServiceId(), internal.getBuddyList().buddyList, null));
					break;
				case ICQServiceResponse.RES_FILEPROGRESS:
					FileProgress fp = new FileProgress(getServiceId(), ProtocolUtils.bytes2LongBE((byte[]) args[0], 0), (String)args[1], (Long)args[2], (Long)args[3], (Boolean)args[4], (String)args[6], (String)args[5]);
					getProtocolService().getCallback().fileProgress(fp);
					break;
				case ICQServiceResponse.RES_MESSAGEACK:
					getProtocolService().getCallback().messageAck(getServiceId(), (String)args[0], (Long)args[1], ICQEntityAdapter.icqMessageAck2MessageAck((Byte)args[2]));
					break;
				case ICQServiceResponse.RES_TYPING:
					getProtocolService().getCallback().typingNotification(getServiceId(), (String) args[0]);
					break;
				case ICQServiceResponse.RES_ACCOUNT_ACTIVITY:
					getProtocolService().getCallback().accountActivity(getServiceId(), (String) args[0]);
					break;
				case ICQServiceResponse.RES_GET_FILE_FOR_SAVING:
					return Utils.createLocalFileForReceiving((String)args[0], ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[1], getProtocolUid(), (byte)0), (Long)args[2]);
				}		
			} catch(DeadObjectException e){
				//TODO support for on-the-fly core service connections
				log("Callback is dead - shutting down");
				/*try {
					getProtocolService().getMainService().shutdown();
				} catch (RemoteException e1) {
					log("This cannot happen (attempting service shutdown due to dead object)");
				}*/
			} catch(Exception e){
				log(e);
			}
			return null;
		}
		
	};
	
	ICQServiceInternal internal = new ICQServiceInternal(icqResponse);
	
	public ICQService(byte serviceId, String protocolUid, ProtocolService protocolService) {
		super(serviceId, protocolUid, protocolService);
		
	}

	@Override
	protected void timeoutDisconnect() {
		internal.getRunnableService().disconnect();		
	}

	@Override
	protected ConnectionState getCurrentState() {
		return ICQEntityAdapter.icqConnectionState2ConnectionState(internal.getCurrentState());
	}

	@Override
	protected void keepaliveRequest() {
		try {
			internal.request(ICQServiceInternal.REQ_KEEPALIVE_CHECK);
		} catch (ICQException e) {
			log(e);
		}
	}

	void connect(OnlineInfo info) throws ProtocolException {
		try {
			String username = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_USERNAME);
			String password = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PASSWORD);
			String host = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_LOGIN_HOST);
			String port = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_LOGIN_PORT);
			String ping = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_PING_TIMEOUT);
			String secureLogin = getProtocolService().getCallback().requestPreference(getServiceId(), ResourceUtils.KEY_SECURE_LOGIN);
			
			if (ping != null){
				try {
					pingTimeout = Integer.parseInt(ping);
				} catch (Exception e) {}
			}
			
			if (username == null || password == null){
				throw new ProtocolException(Cause.BROKEN_AUTH_DATA);
			}			
			
			internal.request(ICQServiceInternal.REQ_CONNECT, username, password, host, port, ICQEntityAdapter.onlineInfo2ICQOnlineInfo(info, info.getProtocolUid()), Boolean.parseBoolean(secureLogin));
		} catch (Exception e) {
			throw new ProtocolException(e.getMessage());
		}		
	}
}
