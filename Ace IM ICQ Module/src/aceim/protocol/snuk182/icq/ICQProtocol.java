package aceim.protocol.snuk182.icq;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.ItemAction;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.AccountService;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.IProtocolService;
import aceim.api.service.ProtocolException;
import aceim.api.service.ProtocolService;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddy;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQBuddyGroup;
import aceim.protocol.snuk182.icq.utils.ResourceUtils;
import android.os.Parcelable;
import android.os.RemoteException;

public class ICQProtocol extends ProtocolService {

	public ICQProtocol() {
		super();
		setMainService(stub);
	}

	private final IProtocolService.Stub stub = new IProtocolService.Stub() {

		@Override
		public void registerCallback(ICoreProtocolCallback back) throws RemoteException {
			setCallback(back);
		}

		@Override
		public void requestFullInfo(byte serviceId, String uid, boolean shortInfo) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				if (!uid.equals(service.getProtocolUid())) {
					if (shortInfo) {
						service.internal.request(ICQServiceInternal.REQ_GETSHORTBUDDYINFO, uid);
					} else {
						service.internal.request(ICQServiceInternal.REQ_GETFULLBUDDYINFO, uid);
					}
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}

		@Override
		public void buddyAction(ItemAction action, Buddy buddy) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(buddy.getServiceId());

			ICQBuddy icqBuddy = ICQEntityAdapter.buddy2ICQBuddy(buddy);
			try {
				switch (action) {
				case ADDED:
					service.internal.request(ICQServiceInternal.REQ_ADDBUDDY, icqBuddy);
					break;
				case MODIFIED:
					service.internal.request(ICQServiceInternal.REQ_EDITBUDDY, icqBuddy);
					break;
				case DELETED:
					service.internal.request(ICQServiceInternal.REQ_REMOVEBUDDY, icqBuddy);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}

		@Override
		public void buddyGroupAction(ItemAction action, BuddyGroup group) throws RemoteException {
			try {
				ICQService service = (ICQService) findAccountServiceById(group.getServiceId());
				ICQBuddyGroup icqBuddyGroup = ICQEntityAdapter.buddyGroup2ICQBuddyGroup(group);

				switch (action) {
				case ADDED:
					service.internal.request(ICQServiceInternal.REQ_ADDGROUP, icqBuddyGroup);
					break;
				case DELETED:
					service.internal.request(ICQServiceInternal.REQ_REMOVEGROUP, icqBuddyGroup);
					break;
				case MODIFIED:
					service.internal.request(ICQServiceInternal.REQ_RENAMEGROUP, icqBuddyGroup);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}

		@Override
		public void disconnect(byte serviceId) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				service.internal.request(ICQServiceInternal.REQ_DISCONNECT);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void connect(OnlineInfo info) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(info.getServiceId());

			try {
				service.connect(info);
			} catch (ProtocolException e) {
				RemoteException ee = new RemoteException();
				ee.initCause(e);
				throw ee;
			}
		}

		@Override
		public long sendMessage(Message message) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(message.getServiceId());

			try {
				Object result;
				if (message instanceof TextMessage) {
					result = service.internal.request(ICQServiceInternal.REQ_SENDMESSAGE, ICQEntityAdapter.textMessage2ICBMMessage((TextMessage) message));					
				} else if (message instanceof FileMessage) {
					result = service.internal.request(ICQServiceInternal.REQ_SENDFILE, ICQEntityAdapter.fileMessage2IcbmMessage((FileMessage) message, service.getProtocolUid()));					
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
		public void requestIcon(byte serviceId, String ownerUid) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				service.internal.request(ICQServiceInternal.REQ_GETICON, ownerUid);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void messageResponse(Message message, boolean accept) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(message.getServiceId());

			try {
				if (message instanceof FileMessage) {
					service.internal.request(ICQServiceInternal.REQ_FILERESPOND, message.getMessageId(), accept);
				} else if (message instanceof ServiceMessage) {
					if (message.getContactDetail().equals(getBaseContext().getString(R.string.ask_authorization))) {
						service.internal.request(ICQServiceInternal.REQ_AUTHRESPONSE, message.getContactUid(), accept);
					}
				}
			} catch (Exception e) {
				Logger.log(e);
			}
			
		}

		@Override
		public void cancelFileFransfer(byte serviceId, long messageId) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				service.internal.request(ICQServiceInternal.REQ_FILECANCEL, messageId);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void sendTypingNotification(byte serviceId, String ownerUid) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				service.internal.request(ICQServiceInternal.REQ_SENDTYPING, ownerUid);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public void getChatRooms(byte serviceId) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public void addAccount(byte serviceId, String protocolUid) throws RemoteException {
			addAccountService(serviceId, protocolUid);
		}

		@Override
		public void removeAccount(byte serviceId) throws RemoteException {
			removeAccountService(serviceId);
		}

		@Override
		public void shutdown() throws RemoteException {
			ICQProtocol.this.shutdown();
		}

		@Override
		public List<ProtocolOption> getProtocolOptions() throws RemoteException {
			return Arrays.asList(ResourceUtils.OPTIONS);
		}

		@Override
		public String getProtocolName() throws RemoteException {
			return ICQProtocol.this.getProtocolName();
		}

		@Override
		public void uploadAccountPhoto(byte serviceId, String filePath) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				byte[] scaledImage = Utils.scaleAccountIcon(filePath, 60);

				service.internal.request(ICQServiceInternal.REQ_UPLOADICON, scaledImage);
			} catch (Exception e) {
				Logger.log(e);
				getCallback().notification(serviceId, e.getLocalizedMessage());
			}
		}

		@Override
		public void removeAccountPhoto(byte serviceId) throws RemoteException {
			ICQService service = (ICQService) findAccountServiceById(serviceId);

			try {
				service.internal.request(ICQServiceInternal.REQ_UPLOADICON, new byte[0]);
			} catch (ICQException e) {
				Logger.log(e);
			}
		}

		@Override
		public ProtocolServiceFeature[] getProtocolFeatures() throws RemoteException {
			return ResourceUtils.getFeatures(getBaseContext());
		}

		@Override
		public void setFeature(String featureId, OnlineInfo info) throws RemoteException {
			//This helps parsing Bundles
			info.getFeatures().setClassLoader(TKV.class.getClassLoader());
			
			ICQService service = (ICQService) findAccountServiceById(info.getServiceId());
			try {
				if (featureId.equals(ApiConstants.FEATURE_STATUS)) {
					byte status = info.getFeatures().getByte(featureId);

					int istatus = ICQEntityAdapter.userStatus2ICQUserStatus(status);
					if (istatus < 0) {
						byte[] qipStatus = ICQEntityAdapter.userQipStatus2ICQQipStatus(status);
						if (qipStatus != null) {
							service.internal.request(ICQServiceInternal.REQ_SETSTATUS, ICQConstants.STATUS_ONLINE, qipStatus);
						} else {
							Logger.log("Wrong status - " + status);
						}
					} else {
						service.internal.request(ICQServiceInternal.REQ_SETSTATUS, istatus);
					}
				} else if (featureId.equals(ApiConstants.FEATURE_XSTATUS)) {
					service.internal.request(ICQServiceInternal.REQ_SETEXTENDEDSTATUS, ICQEntityAdapter.onlineInfo2ICQOnlineInfo(info, info.getProtocolUid()));
				} else if (featureId.equals(IcqApiConstants.FEATURE_ACCOUNT_VISIBILITY)) {
					service.internal.request(ICQServiceInternal.REQ_ACCOUNTVISIBILITY, ICQEntityAdapter.ACCOUNT_VISIBILITY_MAPPING[info.getFeatures().getByte(featureId, (byte) 0)]);
				} else if (featureId.equals(IcqApiConstants.FEATURE_BUDDY_VISIBILITY)) {
					byte value = info.getFeatures().getByte(featureId, (byte) 0);
					service.internal.request(ICQServiceInternal.REQ_BUDDYVISIBILITY, info.getProtocolUid(), value < 0 ? ICQConstants.VIS_REGULAR : ICQEntityAdapter.BUDDY_VISIBILITY_MAPPING[value]);
				} else if (featureId.equals(IcqApiConstants.FEATURE_BUDDY_SEARCH)) {
					Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
					if (p == null || p.length < 1) {
						Logger.log("No Parcelable at ICQ Buddy search request", LoggerLevel.INFO);
						return;
					}
					Map<String, String> map = ICQEntityAdapter.searchTKVListToMap(p, getBaseContext());
					
					service.internal.request(ICQServiceInternal.REQ_SEARCHFORBUDDY, map);
				} else if (featureId.equals(IcqApiConstants.FEATURE_AUTHORIZATION)) {
					Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
					if (p == null || p.length < 1) {
						Logger.log("No Parcelable at ICQ Auth request", LoggerLevel.INFO);
						return;
					}
					
					String reasonKey = getBaseContext().getString(R.string.message);
					for (Parcelable pp : p) {
						TKV tkv = (TKV) pp;
						if (tkv.getKey().equals(reasonKey)) {
							service.internal.request(ICQServiceInternal.REQ_AUTHREQUEST, info.getProtocolUid(), tkv.getValue());
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
		public void joinChatRoom(byte serviceId, String chatId) throws RemoteException {
			
		}

		@Override
		public void leaveChatRoom(byte serviceId, String chatId) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void getChatRoomOccupants(byte serviceId, String chatId, boolean loadOccupantIcons) throws RemoteException {
			// TODO Auto-generated method stub
			
		}
	};

	@Override
	protected AccountService createService(byte serviceId, String protocolUid) {
		ICQService service = new ICQService(serviceId, protocolUid, this);
		return service;
	}

	@Override
	protected String getProtocolName() {
		return IcqApiConstants.PROTOCOL_NAME;
	}
}
