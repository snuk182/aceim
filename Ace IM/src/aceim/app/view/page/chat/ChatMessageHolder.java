package aceim.app.view.page.chat;

import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.utils.Logger;
import android.os.Parcel;
import android.os.Parcelable;

public final class ChatMessageHolder implements Parcelable{

	private final Message mMessage;
	private final String mSenderName;
	private MessageAckState mAckState = null;

	public ChatMessageHolder(Message message, String senderName) {
		super();
		this.mMessage = message;
		this.mSenderName = senderName;
	}

	/**
	 * @return the mMessage
	 */
	public Message getMessage() {
		return mMessage;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mSenderName);
		out.writeString(mMessage.getClass().getName());
		out.writeParcelable(mMessage, flags);
	}

	public static final Parcelable.Creator<ChatMessageHolder> CREATOR = new Parcelable.Creator<ChatMessageHolder>() {
		public ChatMessageHolder createFromParcel(Parcel in) {
			return new ChatMessageHolder(in);
		}

		public ChatMessageHolder[] newArray(int size) {
			return new ChatMessageHolder[size];
		}
	};

	private ChatMessageHolder(Parcel in) {
		mSenderName = in.readString();
		String className = in.readString();
		Class<?> cls;
		try {
			cls = (Class<?>) Class.forName(className);			
		} catch (ClassNotFoundException e) {
			Logger.log(e);
			cls = null;
		}
		
		mMessage = (Message) (cls!=null ? in.readParcelable(cls.getClassLoader()) : null);
	}

	/**
	 * @return the mAckState
	 */
	public MessageAckState getAckState() {
		return mAckState;
	}

	/**
	 * @param mAckState the mAckState to set
	 */
	public void setAckState(MessageAckState ackState) {
		this.mAckState = ackState;
	}

	/**
	 * @return the mSenderName
	 */
	public String getSenderName() {
		return mSenderName;
	}
}
