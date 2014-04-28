package aceim.api.dataentity.tkv;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Message attachment {@link TKV}
 */
public class MessageAttachment extends TKV {
	
	/**
	 * Attachment type.
	 */
	private final MessageAttachmentType type;

	public MessageAttachment(MessageAttachmentType type, String title, String source) {
		super(source, false, title);
		this.type = type;
	}
	
	public static final Parcelable.Creator<MessageAttachment> CREATOR = new Parcelable.Creator<MessageAttachment>() {
		
		public MessageAttachment createFromParcel(Parcel in) {
			in.readString();
			return new MessageAttachment(in);
		}

		public MessageAttachment[] newArray(int size) {
			return new MessageAttachment[size];
		}
	};

	protected MessageAttachment(Parcel in) {
		super(in);
		this.type = MessageAttachmentType.valueOf(in.readString());
	}
	
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(type.name());
	}

	public String getTitle() {
		return getValue();
	}
	
	public String getSource() {
		return getKey();
	}
	
	/**
	 * @return the type
	 */
	public MessageAttachmentType getType() {
		return type;
	}

	public enum MessageAttachmentType {
		/**
		 * URL to picture.
		 */
		PHOTO, 
		
		/**
		 * URL to an audio file.
		 */
		AUDIO, 
		
		/**
		 * URL to a video file.
		 */
		VIDEO, 
		
		/**
		 * Url to a location (as of version 0.9.3 of core - not implemented).
		 */
		MAP, 
		
		/**
		 * General URL.
		 */
		OTHER
	}
}
