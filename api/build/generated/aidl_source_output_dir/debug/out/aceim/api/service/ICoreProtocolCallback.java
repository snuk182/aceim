/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package aceim.api.service;
public interface ICoreProtocolCallback extends android.os.IInterface
{
  /** Default implementation for ICoreProtocolCallback. */
  public static class Default implements aceim.api.service.ICoreProtocolCallback
  {
    @Override public void connectionStateChanged(byte serviceId, aceim.api.dataentity.ConnectionState connState, int extraParameter) throws android.os.RemoteException
    {
    }
    @Override public void iconBitmap(byte serviceId, java.lang.String ownerUid, byte[] data, java.lang.String hash) throws android.os.RemoteException
    {
    }
    @Override public void buddyListUpdated(byte serviceId, java.util.List<aceim.api.dataentity.BuddyGroup> buddyList) throws android.os.RemoteException
    {
    }
    @Override public void message(aceim.api.dataentity.Message message) throws android.os.RemoteException
    {
    }
    @Override public void buddyStateChanged(java.util.List<aceim.api.dataentity.OnlineInfo> infos) throws android.os.RemoteException
    {
    }
    @Override public void notification(byte serviceId, java.lang.String message) throws android.os.RemoteException
    {
    }
    @Override public void accountStateChanged(aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException
    {
    }
    @Override public void personalInfo(aceim.api.dataentity.PersonalInfo info, boolean isShortInfo) throws android.os.RemoteException
    {
    }
    @Override public void searchResult(byte serviceId, java.util.List<aceim.api.dataentity.PersonalInfo> infoList) throws android.os.RemoteException
    {
    }
    @Override public void groupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup newGroup) throws android.os.RemoteException
    {
    }
    @Override public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy newBuddy) throws android.os.RemoteException
    {
    }
    @Override public void fileProgress(aceim.api.dataentity.FileProgress progress) throws android.os.RemoteException
    {
    }
    @Override public void messageAck(byte serviceId, java.lang.String ownerUid, long messageId, aceim.api.dataentity.MessageAckState state) throws android.os.RemoteException
    {
    }
    @Override public void typingNotification(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String requestPreference(byte serviceId, java.lang.String preferenceName) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void accountActivity(byte serviceId, java.lang.String text) throws android.os.RemoteException
    {
    }
    @Override public void multiChatParticipants(byte serviceId, java.lang.String chatUid, java.util.List<aceim.api.dataentity.BuddyGroup> participantList) throws android.os.RemoteException
    {
    }
    @Override public void showFeatureInputForm(byte serviceId, java.lang.String uid, aceim.api.dataentity.InputFormFeature feature) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements aceim.api.service.ICoreProtocolCallback
  {
    private static final java.lang.String DESCRIPTOR = "aceim.api.service.ICoreProtocolCallback";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an aceim.api.service.ICoreProtocolCallback interface,
     * generating a proxy if needed.
     */
    public static aceim.api.service.ICoreProtocolCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof aceim.api.service.ICoreProtocolCallback))) {
        return ((aceim.api.service.ICoreProtocolCallback)iin);
      }
      return new aceim.api.service.ICoreProtocolCallback.Stub.Proxy(obj);
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
        case TRANSACTION_connectionStateChanged:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          aceim.api.dataentity.ConnectionState _arg1;
          if ((0!=data.readInt())) {
            _arg1 = aceim.api.dataentity.ConnectionState.CREATOR.createFromParcel(data);
          }
          else {
            _arg1 = null;
          }
          int _arg2;
          _arg2 = data.readInt();
          this.connectionStateChanged(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_iconBitmap:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          java.lang.String _arg3;
          _arg3 = data.readString();
          this.iconBitmap(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_buddyListUpdated:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.util.List<aceim.api.dataentity.BuddyGroup> _arg1;
          _arg1 = data.createTypedArrayList(aceim.api.dataentity.BuddyGroup.CREATOR);
          this.buddyListUpdated(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_message:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.Message _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.Message.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          this.message(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_buddyStateChanged:
        {
          data.enforceInterface(descriptor);
          java.util.List<aceim.api.dataentity.OnlineInfo> _arg0;
          _arg0 = data.createTypedArrayList(aceim.api.dataentity.OnlineInfo.CREATOR);
          this.buddyStateChanged(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_notification:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.notification(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_accountStateChanged:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.OnlineInfo _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.OnlineInfo.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          this.accountStateChanged(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_personalInfo:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.PersonalInfo _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.PersonalInfo.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          this.personalInfo(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_searchResult:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.util.List<aceim.api.dataentity.PersonalInfo> _arg1;
          _arg1 = data.createTypedArrayList(aceim.api.dataentity.PersonalInfo.CREATOR);
          this.searchResult(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_groupAction:
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
          this.groupAction(_arg0, _arg1);
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
        case TRANSACTION_fileProgress:
        {
          data.enforceInterface(descriptor);
          aceim.api.dataentity.FileProgress _arg0;
          if ((0!=data.readInt())) {
            _arg0 = aceim.api.dataentity.FileProgress.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          this.fileProgress(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_messageAck:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          long _arg2;
          _arg2 = data.readLong();
          aceim.api.dataentity.MessageAckState _arg3;
          if ((0!=data.readInt())) {
            _arg3 = aceim.api.dataentity.MessageAckState.CREATOR.createFromParcel(data);
          }
          else {
            _arg3 = null;
          }
          this.messageAck(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_typingNotification:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.typingNotification(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_requestPreference:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _result = this.requestPreference(_arg0, _arg1);
          reply.writeNoException();
          reply.writeString(_result);
          return true;
        }
        case TRANSACTION_accountActivity:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.accountActivity(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_multiChatParticipants:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.util.List<aceim.api.dataentity.BuddyGroup> _arg2;
          _arg2 = data.createTypedArrayList(aceim.api.dataentity.BuddyGroup.CREATOR);
          this.multiChatParticipants(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_showFeatureInputForm:
        {
          data.enforceInterface(descriptor);
          byte _arg0;
          _arg0 = data.readByte();
          java.lang.String _arg1;
          _arg1 = data.readString();
          aceim.api.dataentity.InputFormFeature _arg2;
          if ((0!=data.readInt())) {
            _arg2 = aceim.api.dataentity.InputFormFeature.CREATOR.createFromParcel(data);
          }
          else {
            _arg2 = null;
          }
          this.showFeatureInputForm(_arg0, _arg1, _arg2);
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements aceim.api.service.ICoreProtocolCallback
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
      @Override public void connectionStateChanged(byte serviceId, aceim.api.dataentity.ConnectionState connState, int extraParameter) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          if ((connState!=null)) {
            _data.writeInt(1);
            connState.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          _data.writeInt(extraParameter);
          boolean _status = mRemote.transact(Stub.TRANSACTION_connectionStateChanged, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().connectionStateChanged(serviceId, connState, extraParameter);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void iconBitmap(byte serviceId, java.lang.String ownerUid, byte[] data, java.lang.String hash) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(ownerUid);
          _data.writeByteArray(data);
          _data.writeString(hash);
          boolean _status = mRemote.transact(Stub.TRANSACTION_iconBitmap, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().iconBitmap(serviceId, ownerUid, data, hash);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void buddyListUpdated(byte serviceId, java.util.List<aceim.api.dataentity.BuddyGroup> buddyList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeTypedList(buddyList);
          boolean _status = mRemote.transact(Stub.TRANSACTION_buddyListUpdated, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().buddyListUpdated(serviceId, buddyList);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void message(aceim.api.dataentity.Message message) throws android.os.RemoteException
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
          boolean _status = mRemote.transact(Stub.TRANSACTION_message, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().message(message);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void buddyStateChanged(java.util.List<aceim.api.dataentity.OnlineInfo> infos) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeTypedList(infos);
          boolean _status = mRemote.transact(Stub.TRANSACTION_buddyStateChanged, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().buddyStateChanged(infos);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void notification(byte serviceId, java.lang.String message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(message);
          boolean _status = mRemote.transact(Stub.TRANSACTION_notification, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().notification(serviceId, message);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void accountStateChanged(aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException
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
          boolean _status = mRemote.transact(Stub.TRANSACTION_accountStateChanged, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().accountStateChanged(info);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void personalInfo(aceim.api.dataentity.PersonalInfo info, boolean isShortInfo) throws android.os.RemoteException
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
          _data.writeInt(((isShortInfo)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_personalInfo, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().personalInfo(info, isShortInfo);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void searchResult(byte serviceId, java.util.List<aceim.api.dataentity.PersonalInfo> infoList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeTypedList(infoList);
          boolean _status = mRemote.transact(Stub.TRANSACTION_searchResult, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().searchResult(serviceId, infoList);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void groupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup newGroup) throws android.os.RemoteException
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
          if ((newGroup!=null)) {
            _data.writeInt(1);
            newGroup.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_groupAction, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().groupAction(action, newGroup);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy newBuddy) throws android.os.RemoteException
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
          if ((newBuddy!=null)) {
            _data.writeInt(1);
            newBuddy.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_buddyAction, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().buddyAction(action, newBuddy);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void fileProgress(aceim.api.dataentity.FileProgress progress) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((progress!=null)) {
            _data.writeInt(1);
            progress.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_fileProgress, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().fileProgress(progress);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void messageAck(byte serviceId, java.lang.String ownerUid, long messageId, aceim.api.dataentity.MessageAckState state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(ownerUid);
          _data.writeLong(messageId);
          if ((state!=null)) {
            _data.writeInt(1);
            state.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_messageAck, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().messageAck(serviceId, ownerUid, messageId, state);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void typingNotification(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(ownerUid);
          boolean _status = mRemote.transact(Stub.TRANSACTION_typingNotification, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().typingNotification(serviceId, ownerUid);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String requestPreference(byte serviceId, java.lang.String preferenceName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(preferenceName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestPreference, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().requestPreference(serviceId, preferenceName);
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
      @Override public void accountActivity(byte serviceId, java.lang.String text) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(text);
          boolean _status = mRemote.transact(Stub.TRANSACTION_accountActivity, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().accountActivity(serviceId, text);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void multiChatParticipants(byte serviceId, java.lang.String chatUid, java.util.List<aceim.api.dataentity.BuddyGroup> participantList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(chatUid);
          _data.writeTypedList(participantList);
          boolean _status = mRemote.transact(Stub.TRANSACTION_multiChatParticipants, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().multiChatParticipants(serviceId, chatUid, participantList);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void showFeatureInputForm(byte serviceId, java.lang.String uid, aceim.api.dataentity.InputFormFeature feature) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeByte(serviceId);
          _data.writeString(uid);
          if ((feature!=null)) {
            _data.writeInt(1);
            feature.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_showFeatureInputForm, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().showFeatureInputForm(serviceId, uid, feature);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static aceim.api.service.ICoreProtocolCallback sDefaultImpl;
    }
    static final int TRANSACTION_connectionStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_iconBitmap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_buddyListUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_message = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_buddyStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_notification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_accountStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_personalInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_searchResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_groupAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_buddyAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_fileProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_messageAck = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_typingNotification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_requestPreference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_accountActivity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_multiChatParticipants = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_showFeatureInputForm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    public static boolean setDefaultImpl(aceim.api.service.ICoreProtocolCallback impl) {
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
    public static aceim.api.service.ICoreProtocolCallback getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public void connectionStateChanged(byte serviceId, aceim.api.dataentity.ConnectionState connState, int extraParameter) throws android.os.RemoteException;
  public void iconBitmap(byte serviceId, java.lang.String ownerUid, byte[] data, java.lang.String hash) throws android.os.RemoteException;
  public void buddyListUpdated(byte serviceId, java.util.List<aceim.api.dataentity.BuddyGroup> buddyList) throws android.os.RemoteException;
  public void message(aceim.api.dataentity.Message message) throws android.os.RemoteException;
  public void buddyStateChanged(java.util.List<aceim.api.dataentity.OnlineInfo> infos) throws android.os.RemoteException;
  public void notification(byte serviceId, java.lang.String message) throws android.os.RemoteException;
  public void accountStateChanged(aceim.api.dataentity.OnlineInfo info) throws android.os.RemoteException;
  public void personalInfo(aceim.api.dataentity.PersonalInfo info, boolean isShortInfo) throws android.os.RemoteException;
  public void searchResult(byte serviceId, java.util.List<aceim.api.dataentity.PersonalInfo> infoList) throws android.os.RemoteException;
  public void groupAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.BuddyGroup newGroup) throws android.os.RemoteException;
  public void buddyAction(aceim.api.dataentity.ItemAction action, aceim.api.dataentity.Buddy newBuddy) throws android.os.RemoteException;
  public void fileProgress(aceim.api.dataentity.FileProgress progress) throws android.os.RemoteException;
  public void messageAck(byte serviceId, java.lang.String ownerUid, long messageId, aceim.api.dataentity.MessageAckState state) throws android.os.RemoteException;
  public void typingNotification(byte serviceId, java.lang.String ownerUid) throws android.os.RemoteException;
  public java.lang.String requestPreference(byte serviceId, java.lang.String preferenceName) throws android.os.RemoteException;
  public void accountActivity(byte serviceId, java.lang.String text) throws android.os.RemoteException;
  public void multiChatParticipants(byte serviceId, java.lang.String chatUid, java.util.List<aceim.api.dataentity.BuddyGroup> participantList) throws android.os.RemoteException;
  public void showFeatureInputForm(byte serviceId, java.lang.String uid, aceim.api.dataentity.InputFormFeature feature) throws android.os.RemoteException;
}
