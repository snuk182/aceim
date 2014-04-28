package aceim.api.dataentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aceim.api.dataentity.tkv.MessageAttachment;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Text message/
 */
public class TextMessage extends Message {
	
	/**
	 * List of message attachments. Thread-safe.
	 */
	private final List<MessageAttachment> attachments;
	
	@SuppressWarnings("unchecked")
	public TextMessage(Parcel arg0) {
		super(arg0);
		ArrayList<MessageAttachment> list = arg0.readArrayList(MessageAttachment.class.getClassLoader());
		attachments = Collections.synchronizedList(list);
	}
	
	public TextMessage(byte serviceId, String from){
		super(serviceId, from);
		attachments = Collections.synchronizedList(new ArrayList<MessageAttachment>());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeList(attachments);
	}
	
	public static final Parcelable.Creator<TextMessage> CREATOR = new Parcelable.Creator<TextMessage>(){

		@Override
		public TextMessage createFromParcel(Parcel arg0) {
			//Omitting classname variable used for class hierarchy parcelable support
			arg0.readString();
			return new TextMessage(arg0);
		}

		@Override
		public TextMessage[] newArray(int size) {
			return new TextMessage[size];
		}
		
	};
	
	/**
	 * @return the attachments
	 */
	public List<MessageAttachment> getAttachments() {
		return attachments;
	}
}
