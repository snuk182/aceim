/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package aceim.api.service;
public interface IProtocolService extends android.os.IInterface
{
  /** Default implementation for IProtocolService. */
  public static class Default implements aceim.api.service.IProtocolService
  {
    @Override public void registerCallback(aceim.api.service.ICoreProtocolCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void addAccount(byte serviceId, java.lang.String protocolUid) throws android.os.RemoteException
    {
    }
    @Override public void removeAccount(byte serviceId) throws android.os.RemoteException
    {
    }
    @Override public void shutdown() throws android.os.RemoteException
    {
    }
    @Override public void logToFile(boolean enable) throws android.os.RemoteException
    {
    }
    @Override public void requestFullInfo(byte serviceId, java.lang.String uid, boolean shortInfo) throws android.os.RemoteException
    {
    }
    @Override public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy buddy) throws android.os.RemoteException
    {
    }
    @Override public void buddyGroupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup group) throws android.os.RemoteException
    {
    }
    @Override public void setFeature(java.lang.String featureId, aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException
    {
    }
    @Override public void disconnect(byte serviceId) throws android.os.RemoteException
    {
    }
    @Override public void connect(aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException
    {
    }
    @Override public long sendMessage(aceim.api.dataentity.Message message) throws android.os.RemoteException
    {
      return 0L;
    }
    @Override public void requestIcon(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException
    {
    }
    @Override public void messageResponse(aceim.api.dataentity.Message message, boolean accept) throws android.os.RemoteException
    {
    }
    @Override public void cancelFileFransfer(byte serviceId, long messageId) throws android.os.RemoteException
    {
    }
    @Override public void sendTypingNotification(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException
    {
    }
    @Override public void joinChatRoom(byte serviceId, java.lang.String chatId, boolean loadOccupantsIcons) throws android.os.RemoteException
    {
    }
    @Override public void leaveChatRoom(byte serviceId, java.lang.String chatId) throws android.os.RemoteException
    {
    }
    @Override public void uploadAccountPhoto(byte serviceId, java.lang.String filePath) throws android.os.RemoteException
    {
    }
    @Override public void removeAccountPhoto(byte serviceId) throws android.os.RemoteException
    {
    }
    @Override public aceim.api.dataentity.ProtocolServiceFeature[] getProtocolFeatures() throws android.os.RemoteException
    {
      return null;
    }
    @Override public aceim.api.dataentity.ProtocolOption[] getProtocolOptions() throws android.os.RemoteException
    {
      return null;
    }
    @Override public java.lang.String getProtocolName() throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements aceim.api.service.IProtocolService
  {
    private static final java.lang.String DESCRIPTOR = "aceim.api.service.IProtocolService";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an aceim.api.service.IProtocolService interface,
     * generating a proxy if needed.
     */
    public static aceim.api.service.IProtocolService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof aceim.api.service.IProtocolService))) {
        return ((aceim.api.service.IProtocolService)iin);
      }
      return new aceim.api.service.IProtocolService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_registerCallback:
        {
          data.enforceInterface(descriptor);
          aceim.api.service.ICoreProtocolCallback _arg0;
          _arg0 = aceim.api.service.ICoreProtocolCallback.Stub.asInterface(data.readStrongBinder());
          this.registerCallback(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_addAccount:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.addAccount(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_removeAccount:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          this.removeAccount(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_shutdown:
        {
          data.enforceInterface(descriptor);
          this.shutdown();
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_logToFile:
        {
          data.enforceInterface(descriptor);
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.logToFile(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_requestFullInfo:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          boolean _arg2;
          _arg2 = (0!=data.readInt());
          this.requestFullInfo(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_buddyAction:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.ItemAction _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.ItemAction.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          aceim.api.dataentity.Buddy _arg1;
          if ((0!=data.readInt())) {
            _arg1 = aceim.api.dataentity.Buddy.CREATOR.createFromParcel(data);
          }
          else {
            _arg1 = null;
          }
          this.buddyAction(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_buddyGroupAction:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.ItemAction _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.ItemAction.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          aceim.api.dataentity.BuddyGroup _arg1;
          if ((0!=data.readInt())) {
            _arg1 = aceim.api.dataentity.BuddyGroup.CREATOR.createFromParcel(data);
          }
          else {
            _arg1 = null;
          }
          this.buddyGroupAction(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_setFeature:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          aceim.api.dataentity.OnlineInfo _arg1;
          if ((0!=data.readInt())) {
            _arg1 = aceim.api.dataentity.OnlineInfo.CREATOR.createFromParcel(data);
          }
          else {
            _arg1 = null;
          }
          this.setFeature(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_disconnect:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          this.disconnect(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_connect:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.OnlineInfo _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.OnlineInfo.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          this.connect(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_sendMessage:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.Message _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.Message.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          long _result = this.sendMessage(_arg0);
          reply.writeNoException();
          reply.writeLong(_result);
          return true;
        }
        case TRANSACTION_requestIcon:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.requestIcon(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_messageResponse:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.Message _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.Message.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          this.messageResponse(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_cancelFileFransfer:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          long _arg1;
          _arg1 = data.readLong();
          this.cancelFileFransfer(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_sendTypingNotification:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.sendTypingNotification(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_joinChatRoom:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          boolean _arg2;
          _arg2 = (0!=data.readInt());
          this.joinChatRoom(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_leaveChatRoom:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.leaveChatRoom(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_uploadAccountPhoto:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.uploadAccountPhoto(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_removeAccountPhoto:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          this.removeAccountPhoto(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_getProtocolFeatures:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.ProtocolServiceFeature[] _result = this.getProtocolFeatures();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          return true;
        }
        case TRANSACTION_getProtocolOptions:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.ProtocolOption[] _result = this.getProtocolOptions();
          reply.writeNoException();
          reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          return true;
        }
        case TRANSACTION_getProtocolName:
        {
          data.enforceInterface(descriptor);
          java.lang.String _result = this.getProtocolName();
          reply.writeNoException();
          reply.writeString(_result);
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements aceim.api.service.IProtocolService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void registerCallback(aceim.api.service.ICoreProtocolCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().registerCallback(callback);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void addAccount(byte serviceId, java.lang.String protocolUid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(protocolUid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_addAccount, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().addAccount(serviceId, protocolUid);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeAccount(byte serviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeAccount, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().removeAccount(serviceId);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void shutdown() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_shutdown, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().shutdown();
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void logToFile(boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((enable)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_logToFile, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().logToFile(enable);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void requestFullInfo(byte serviceId, java.lang.String uid, boolean shortInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(uid);
          _data.writeInt(((shortInfo)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestFullInfo, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().requestFullInfo(serviceId, uid, shortInfo);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy buddy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((action!=null)) {
            _data.writeInt(1);
            action.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          if ((buddy!=null)) {
            _data.writeInt(1);
            buddy.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_buddyAction, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().buddyAction(action, buddy);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void buddyGroupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup group) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((action!=null)) {
            _data.writeInt(1);
            action.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          if ((group!=null)) {
            _data.writeInt(1);
            group.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_buddyGroupAction, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().buddyGroupAction(action, group);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setFeature(java.lang.String featureId, aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(featureId);
          if ((info!=null)) {
            _data.writeInt(1);
            info.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFeature, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().setFeature(featureId, info);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void disconnect(byte serviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_disconnect, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().disconnect(serviceId);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void connect(aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((info!=null)) {
            _data.writeInt(1);
            info.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_connect, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().connect(info);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public long sendMessage(aceim.api.dataentity.Message message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((message!=null)) {
            _data.writeInt(1);
            message.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendMessage, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().sendMessage(message);
          }
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void requestIcon(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(ownerUid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestIcon, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().requestIcon(serviceId, ownerUid);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void messageResponse(aceim.api.dataentity.Message message, boolean accept) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((message!=null)) {
            _data.writeInt(1);
            message.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          _data.writeInt(((accept)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_messageResponse, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().messageResponse(message, accept);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void cancelFileFransfer(byte serviceId, long messageId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeLong(messageId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelFileFransfer, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().cancelFileFransfer(serviceId, messageId);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void sendTypingNotification(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(ownerUid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendTypingNotification, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().sendTypingNotification(serviceId, ownerUid);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void joinChatRoom(byte serviceId, java.lang.String chatId, boolean loadOccupantsIcons) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(chatId);
          _data.writeInt(((loadOccupantsIcons)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_joinChatRoom, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().joinChatRoom(serviceId, chatId, loadOccupantsIcons);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void leaveChatRoom(byte serviceId, java.lang.String chatId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(chatId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_leaveChatRoom, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().leaveChatRoom(serviceId, chatId);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void uploadAccountPhoto(byte serviceId, java.lang.String filePath) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(filePath);
          boolean _status = mRemote.transact(Stub.TRANSACTION_uploadAccountPhoto, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().uploadAccountPhoto(serviceId, filePath);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void removeAccountPhoto(byte serviceId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeAccountPhoto, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().removeAccountPhoto(serviceId);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public aceim.api.dataentity.ProtocolServiceFeature[] getProtocolFeatures() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        aceim.api.dataentity.ProtocolServiceFeature[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getProtocolFeatures, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().getProtocolFeatures();
          }
          _reply.readException();
          _result = _reply.createTypedArray(aceim.api.dataentity.ProtocolServiceFeature.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public aceim.api.dataentity.ProtocolOption[] getProtocolOptions() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        aceim.api.dataentity.ProtocolOption[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getProtocolOptions, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().getProtocolOptions();
          }
          _reply.readException();
          _result = _reply.createTypedArray(aceim.api.dataentity.ProtocolOption.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String getProtocolName() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getProtocolName, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().getProtocolName();
          }
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      public static aceim.api.service.IProtocolService sDefaultImpl;
    }
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_addAccount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_removeAccount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_shutdown = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_logToFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_requestFullInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_buddyAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_buddyGroupAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setFeature = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_disconnect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_connect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_sendMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_requestIcon = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_messageResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_cancelFileFransfer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_sendTypingNotification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_joinChatRoom = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_leaveChatRoom = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_uploadAccountPhoto = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_removeAccountPhoto = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_getProtocolFeatures = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_getProtocolOptions = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_getProtocolName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    public static boolean setDefaultImpl(aceim.api.service.IProtocolService impl) {
      // Only one user of this interface can use this function
      // at a time. This is a heuristic to detect if two different
      // users in the same process use this function.
      if (Stub.Proxy.sDefaultImpl != null) {
        throw new IllegalStateException("setDefaultImpl() called twice");
      }
      if (impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static aceim.api.service.IProtocolService getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public void registerCallback(aceim.api.service.ICoreProtocolCallback callback) throws android.os.RemoteException;
  public void addAccount(byte serviceId, java.lang.String protocolUid) throws android.os.RemoteException;
  public void removeAccount(byte serviceId) throws android.os.RemoteException;
  public void shutdown() throws android.os.RemoteException;
  public void logToFile(boolean enable) throws android.os.RemoteException;
  public void requestFullInfo(byte serviceId, java.lang.String uid, boolean shortInfo) throws android.os.RemoteException;
  public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy buddy) throws android.os.RemoteException;
  public void buddyGroupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup group) throws android.os.RemoteException;
  public void setFeature(java.lang.String featureId, aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException;
  public void disconnect(byte serviceId) throws android.os.RemoteException;
  public void connect(aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException;
  public long sendMessage(aceim.api.dataentity.Message message) throws android.os.RemoteException;
  public void requestIcon(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException;
  public void messageResponse(aceim.api.dataentity.Message message, boolean accept) throws android.os.RemoteException;
  public void cancelFileFransfer(byte serviceId, long messageId) throws android.os.RemoteException;
  public void sendTypingNotification(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException;
  public void joinChatRoom(byte serviceId, java.lang.String chatId, boolean loadOccupantsIcons) throws android.os.RemoteException;
  public void leaveChatRoom(byte serviceId, java.lang.String chatId) throws android.os.RemoteException;
  public void uploadAccountPhoto(byte serviceId, java.lang.String filePath) throws android.os.RemoteException;
  public void removeAccountPhoto(byte serviceId) throws android.os.RemoteException;
  public aceim.api.dataentity.ProtocolServiceFeature[] getProtocolFeatures() throws android.os.RemoteException;
  public aceim.api.dataentity.ProtocolOption[] getProtocolOptions() throws android.os.RemoteException;
  public java.lang.String getProtocolName() throws android.os.RemoteException;
}
