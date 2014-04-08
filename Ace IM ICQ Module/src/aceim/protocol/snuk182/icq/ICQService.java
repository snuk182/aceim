package aceim.protocol.snuk182.icq;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import aceim.protocol.snuk182.icq.inner.ICQConstants;
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
import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;

public class ICQService extends AccountService {

	public ICQService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}

	@Override
	public IProtocol getProtocol() {
		return protocol;
	}

	@Override
	protected void timeoutReconnect() {
		internal.getRunnableService().disconnect();
		try {
			internal.connectInternal(true);
		} catch (ICQException e) {
			Logger.log(e);
		}
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
			Logger.log(e);
		}
	}

	private final IProtocol protocol = new IProtocol() {

		@Override
		public void requestFullInfo(String uid, boolean shortInfo) {
			try {
				if (!uid.equals(getProtocolUid())) {
					if (shortInfo) {
						internal.request(ICQServiceInternal.REQ_GETSHORTBUDDYINFO, uid);
					} else {
						internal.request(ICQServiceInternal.REQ_GETFULLBUDDYINFO, uid);
					}
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}

		@Override
		public void buddyAction(ItemAction action, Buddy buddy) {
			ICQBuddy icqBuddy = ICQEntityAdapter.buddy2ICQBuddy(buddy);
			try {
				switch (action) {
				case ADDED:
					internal.request(ICQServiceInternal.REQ_ADDBUDDY, icqBuddy);
					break;
				case MODIFIED:
					internal.request(ICQServiceInternal.REQ_EDITBUDDY, icqBuddy);
					break;
				case DELETED:
					internal.request(ICQServiceInternal.REQ_REMOVEBUDDY, icqBuddy);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}

		@Override
		public void buddyGroupAction(ItemAction action, BuddyGroup group) {
			try {
				ICQBuddyGroup icqBuddyGroup = ICQEntityAdapter.buddyGroup2ICQBuddyGroup(group);

				switch (action) {
				case ADDED:
					internal.request(ICQServiceInternal.REQ_ADDGROUP, icqBuddyGroup);
					break;
				case DELETED:
					internal.request(ICQServiceInternal.REQ_REMOVEGROUP, icqBuddyGroup);
					break;
				case MODIFIED:
					internal.request(ICQServiceInternal.REQ_RENAMEGROUP, icqBuddyGroup);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}

		@Override
		public void disconnect() {
			try {
				internal.request(ICQServiceInternal.REQ_DISCONNECT);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void connect(OnlineInfo info) {
			try {
				connectInternal(info);
			} catch (ProtocolException e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public long sendMessage(Message message) {
			try {
				Object result;
				if (message instanceof TextMessage) {
					result = internal.request(ICQServiceInternal.REQ_SENDMESSAGE, ICQEntityAdapter.textMessage2ICBMMessage((TextMessage) message));
				} else if (message instanceof FileMessage) {
					result = internal.request(ICQServiceInternal.REQ_SENDFILE, ICQEntityAdapter.fileMessage2IcbmMessage((FileMessage) message, getProtocolUid()));
				} else {
					result = null;
				}

				if (result instanceof Long) {
					return (Long) result;
				} else {
					return 0;
				}
			} catch (ICQException e) {
				Logger.log(e);
				return 0;
			}
		}

		@Override
		public void requestIcon(String ownerUid) {
			try {
				internal.request(ICQServiceInternal.REQ_GETICON, ownerUid);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void messageResponse(Message message, boolean accept) {
			try {
				if (message instanceof FileMessage) {
					internal.request(ICQServiceInternal.REQ_FILERESPOND, message.getMessageId(), accept);
				} else if (message instanceof ServiceMessage) {
					if (message.getContactDetail().equals(getContext().getString(R.string.ask_authorization))) {
						internal.request(ICQServiceInternal.REQ_AUTHRESPONSE, message.getContactUid(), accept);
					}
				}
			} catch (Exception e) {
				Logger.log(e);
			}

		}

		@Override
		public void cancelFileFransfer(long messageId) {
			try {
				internal.request(ICQServiceInternal.REQ_FILECANCEL, messageId);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void sendTypingNotification(String ownerUid) {
			try {
				internal.request(ICQServiceInternal.REQ_SENDTYPING, ownerUid);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void uploadAccountPhoto(String filePath) {
			try {
				byte[] scaledImage = Utils.scaleAccountIcon(filePath, 60);

				internal.request(ICQServiceInternal.REQ_UPLOADICON, scaledImage);
			} catch (Exception e) {
				Logger.log(e);
				getCoreService().notification(e.getLocalizedMessage());
			}
		}

		@Override
		public void removeAccountPhoto() {
			try {
				internal.request(ICQServiceInternal.REQ_UPLOADICON, new byte[0]);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void setFeature(String featureId, OnlineInfo info) {
			// This helps parsing Bundles
			info.getFeatures().setClassLoader(TKV.class.getClassLoader());

			try {
				if (featureId.equals(ApiConstants.FEATURE_STATUS)) {
					byte status = info.getFeatures().getByte(featureId);

					int istatus = ICQEntityAdapter.userStatus2ICQUserStatus(status);
					if (istatus < 0) {
						byte[] qipStatus = ICQEntityAdapter.userQipStatus2ICQQipStatus(status);
						if (qipStatus != null) {
							internal.request(ICQServiceInternal.REQ_SETSTATUS, ICQConstants.STATUS_ONLINE, qipStatus);
						} else {
							Logger.log("Wrong status - " + status);
						}
					} else {
						internal.request(ICQServiceInternal.REQ_SETSTATUS, istatus);
					}
				} else if (featureId.equals(ApiConstants.FEATURE_XSTATUS)) {
					internal.request(ICQServiceInternal.REQ_SETEXTENDEDSTATUS, ICQEntityAdapter.onlineInfo2ICQOnlineInfo(info, info.getProtocolUid()));
				} else if (featureId.equals(IcqApiConstants.FEATURE_ACCOUNT_VISIBILITY)) {
					internal.request(ICQServiceInternal.REQ_ACCOUNTVISIBILITY, ICQEntityAdapter.ACCOUNT_VISIBILITY_MAPPING[info.getFeatures().getByte(featureId, (byte) 0)]);
				} else if (featureId.equals(IcqApiConstants.FEATURE_BUDDY_VISIBILITY)) {
					byte value = info.getFeatures().getByte(featureId, (byte) 0);
					internal.request(ICQServiceInternal.REQ_BUDDYVISIBILITY, info.getProtocolUid(), value < 0 ? ICQConstants.VIS_REGULAR : ICQEntityAdapter.BUDDY_VISIBILITY_MAPPING[value]);
				} else if (featureId.equals(IcqApiConstants.FEATURE_BUDDY_SEARCH)) {
					Parcelable[] p = info.getFeatures().getParcelableArray(featureId);
					if (p == null || p.length < 1) {
						Logger.log("No Parcelable at ICQ Buddy search request", LoggerLevel.INFO);
						return;
					}
					Map<String, String> map = ICQEntityAdapter.searchTKVListToMap(p, getContext());

					internal.request(ICQServiceInternal.REQ_SEARCHFORBUDDY, map);
				} else if (featureId.equals(IcqApiConstants.FEATURE_AUTHORIZATION)) {
					Parcelable[] p = info.getFeatures().getParcelableArray(featureId);
					if (p == null || p.length < 1) {
						Logger.log("No Parcelable at ICQ Auth request", LoggerLevel.INFO);
						return;
					}

					String reasonKey = getContext().getString(R.string.message);
					for (Parcelable pp : p) {
						TKV tkv = (TKV) pp;
						if (tkv.getKey().equals(reasonKey)) {
							internal.request(ICQServiceInternal.REQ_AUTHREQUEST, info.getProtocolUid(), tkv.getValue());
							return;
						}
					}

					Logger.log("No reason message at ICQ Auth request", LoggerLevel.INFO);
				}
			} catch (ICQException e) {
				Logger.log(e);
			}

		}

		@Override
		public void joinChatRoom(String chatId, boolean loadOccupantsIcons) {
		}

		@Override
		public void leaveChatRoom(String chatId) {
		}
	};

	private final ICQServiceResponse icqResponse = new ICQServiceResponse() {
		
		@SuppressWarnings("unchecked")
		@Override
		public Object respond(short action, Object... args) {

			try {
				switch (action) {
				case ICQServiceResponse.RES_LOG:
					Logger.log((String) args[0]);
					break;
				case ICQServiceResponse.RES_CONNECTED:
					getCoreService().connectionStateChanged(ConnectionState.CONNECTED, 0);
					sendKeepalive();
					break;
				case ICQServiceResponse.RES_DISCONNECTED:
					closeKeepaliveThread();
					
					int errorCode = -1;
					
					if (args.length > 0 && !TextUtils.isEmpty((CharSequence) args[0])) {
						String error = args[0].toString();
						getCoreService().notification(error);
						
						if (ICQServiceInternal.ERROR_WRONG_PASSWORD.equals(error)) {
							errorCode = ProtocolException.Cause.CANNOT_AUTHORIZE.ordinal();
						} else if (ICQServiceInternal.ERROR_RATE_LIMIT_EXCEEDED.equals(error)) {
							errorCode = ProtocolException.Cause.CONNECTION_LIMIT_EXCEEDED.ordinal();
						}
					} 
					
					getCoreService().connectionStateChanged(ConnectionState.DISCONNECTED, errorCode);					
					break;
				case ICQServiceResponse.RES_SAVEIMAGEFILE:
					getCoreService().iconBitmap((String) args[1], (byte[]) args[0], Base64.encodeBytes((byte[]) args[2]));
					break;
				case ICQServiceResponse.RES_CLUPDATED:
					List<Buddy> buddyList = ICQEntityAdapter.ICQBuddyList2Buddylist((List<ICQBuddy>) args[0], getProtocolUid(), getServiceId());
					List<BuddyGroup> buddyGroupList = ICQEntityAdapter.ICQBuddyGroupList2BuddyGroupList((List<ICQBuddyGroup>) args[1], getProtocolUid(), getServiceId(), (List<ICQBuddy>) args[0], buddyList);
					getCoreService().buddyListUpdated(buddyGroupList);
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

					getCoreService().message(txtmessage);
					break;
				case ICQServiceResponse.RES_BUDDYSTATECHANGED:
					OnlineInfo buddiOnlineInfo = ICQEntityAdapter.icqOnlineInfo2OnlineInfo((ICQOnlineInfo) args[0], getProtocolUid(), getServiceId());
					
					getCoreService().buddyStateChanged(Arrays.asList(buddiOnlineInfo));
					
					if (buddiOnlineInfo.getFeatures().getByte(ApiConstants.FEATURE_XSTATUS, (byte) -1) > -1 && args.length < 2) {
						internal.getMessagingEngine().askForXStatus(buddiOnlineInfo.getProtocolUid());
					}
					
					break;
				case ICQServiceResponse.RES_CONNECTING:
					getCoreService().connectionStateChanged(ConnectionState.CONNECTING, (Integer) args[0]);
					break;
				case ICQServiceResponse.RES_FILEMESSAGE:
					getCoreService().message(ICQEntityAdapter.icbmMessage2FileMessage((ICBMMessage) args[0], getServiceId()));
					break;
				case ICQServiceResponse.RES_NOTIFICATION:
					getCoreService().notification((String) args[0]);
					break;
				case ICQServiceResponse.RES_ACCOUNTUPDATED:
					getCoreService().accountStateChanged(ICQEntityAdapter.icqOnlineInfo2OnlineInfo((ICQOnlineInfo) args[0], getProtocolUid(), getServiceId()));
					break;
				case ICQServiceResponse.RES_USERINFO:
					getCoreService().personalInfo(ICQEntityAdapter.icqPersonalInfo2PersonalInfo((ICQPersonalInfo) args[0], getContext(), getServiceId()), !((Boolean)args[1]));
					break;
				case ICQServiceResponse.RES_AUTHREQUEST:
					getCoreService().message(ICQEntityAdapter.authRequestToServiceMessage(getServiceId(), (String) args[0], (String) args[1], getContext()));
					break;
				case ICQServiceResponse.RES_SEARCHRESULT:
					getCoreService().searchResult(ICQEntityAdapter.icqPersonalInfos2PersonalInfos((List<ICQPersonalInfo>) args[0], getContext(), getServiceId()));
					break;
				case ICQServiceResponse.RES_GROUPADDED:
					getCoreService().groupAction(ItemAction.ADDED, ICQEntityAdapter.ICQBuddyGroup2BuddyGroup((ICQBuddyGroup) args[0], internal.getUn(), getServiceId(), internal.getBuddyList().buddyList, null));
					break;
				case ICQServiceResponse.RES_BUDDYADDED:
					getCoreService().buddyAction(ItemAction.ADDED, ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[0], internal.getUn(), getServiceId()));
					break;
				case ICQServiceResponse.RES_BUDDYDELETED:
					getCoreService().buddyAction(ItemAction.DELETED, ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[0], internal.getUn(), getServiceId()));
					break;
				case ICQServiceResponse.RES_GROUPDELETED:
					getCoreService().groupAction(ItemAction.DELETED, ICQEntityAdapter.ICQBuddyGroup2BuddyGroup((ICQBuddyGroup) args[0], internal.getUn(), getServiceId(), internal.getBuddyList().buddyList, null));
					break;
				case ICQServiceResponse.RES_BUDDYMODIFIED:
					getCoreService().buddyAction(ItemAction.MODIFIED, ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[0], internal.getUn(), getServiceId()));
					break;
				case ICQServiceResponse.RES_GROUPMODIFIED:
					getCoreService().groupAction(ItemAction.MODIFIED, ICQEntityAdapter.ICQBuddyGroup2BuddyGroup((ICQBuddyGroup) args[0], internal.getUn(), getServiceId(), internal.getBuddyList().buddyList, null));
					break;
				case ICQServiceResponse.RES_FILEPROGRESS:
					FileProgress fp = new FileProgress(getServiceId(), ProtocolUtils.bytes2LongBE((byte[]) args[0], 0), (String) args[1], (Long) args[2], (Long) args[3], (Boolean) args[4], (String) args[6], (String) args[5]);
					getCoreService().fileProgress(fp);
					break;
				case ICQServiceResponse.RES_MESSAGEACK:
					getCoreService().messageAck((String) args[0], (Long) args[1], ICQEntityAdapter.icqMessageAck2MessageAck((Byte) args[2]));
					break;
				case ICQServiceResponse.RES_TYPING:
					getCoreService().typingNotification((String) args[0]);
					break;
				case ICQServiceResponse.RES_ACCOUNT_ACTIVITY:
					getCoreService().accountActivity((String) args[0]);
					break;
				case ICQServiceResponse.RES_GET_FILE_FOR_SAVING:
					return Utils.createLocalFileForReceiving((String) args[0], ICQEntityAdapter.ICQBuddy2Buddy((ICQBuddy) args[1], getProtocolUid(), (byte) 0), (Long) args[2]);
				}
			} catch (Exception e) {
				Logger.log(e);
			}
			return null;
		}
	};
	
	private void connectInternal(OnlineInfo info) throws ProtocolException {
		try {
			String username = getCoreService().requestPreference(ResourceUtils.KEY_USERNAME);
			String password = getCoreService().requestPreference(ResourceUtils.KEY_PASSWORD);
			String host = getCoreService().requestPreference(ResourceUtils.KEY_LOGIN_HOST);
			String port = getCoreService().requestPreference(ResourceUtils.KEY_LOGIN_PORT);
			String ping = getCoreService().requestPreference(ResourceUtils.KEY_PING_TIMEOUT);
			String secureLogin = getCoreService().requestPreference(ResourceUtils.KEY_SECURE_LOGIN);
			
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

	private ICQServiceInternal internal = new ICQServiceInternal(icqResponse);
}
