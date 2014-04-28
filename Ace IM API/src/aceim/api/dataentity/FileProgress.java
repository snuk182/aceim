package aceim.api.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File transfer progress entity.
 *
 */
public class FileProgress extends Entity implements Parcelable {
	
	private final long messageId;
	private final String filePath;
	private final long totalSizeBytes;
	private final long sentBytes;
	private final boolean isIncoming;
	private final String ownerUid;
	private final String error;

	/**
	 * @param serviceId owner account's service ID
	 * @param messageId file transfer message ID
	 * @param filePath path of file being transferred
	 * @param totalSizeBytes total file size
	 * @param sentBytes bytes sent
	 * @param isIncoming is transfer incoming?
	 * @param ownerUid owner account's UID
	 * @param error error message, if any occurs, null otherwise. Non-null value breaks transfer.
	 */
	public FileProgress(byte serviceId, long messageId, String filePath, long totalSizeBytes, long sentBytes, boolean isIncoming, String ownerUid, String error) {
		super(serviceId);		
		this.messageId = messageId;
		this.filePath = filePath;
		this.totalSizeBytes = totalSizeBytes;
		this.sentBytes = sentBytes;
		this.isIncoming = isIncoming;
		this.ownerUid = ownerUid;
		this.error = error;
	}
	
	public FileProgress(Parcel in) {
		super(in);
		this.messageId = in.readLong();
		this.filePath = in.readString();
		this.totalSizeBytes = in.readLong();
		this.sentBytes = in.readLong();
		this.isIncoming = in.readByte() > 0;
		this.ownerUid = in.readString();
		this.error = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeLong(messageId);
		dest.writeString(filePath);
		dest.writeLong(totalSizeBytes);
		dest.writeLong(sentBytes);
		dest.writeByte((byte) (isIncoming ? 1 : 0));
		dest.writeString(ownerUid);
		dest.writeString(error);
	}

	public static final Parcelable.Creator<FileProgress> CREATOR = new Parcelable.Creator<FileProgress>(){

		@Override
		public FileProgress createFromParcel(Parcel arg0) {
			//Omitting classname variable used for class hierarchy parcelable support
			arg0.readString();
			return new FileProgress(arg0);
		}

		@Override
		public FileProgress[] newArray(int size) {
			return new FileProgress[size];
		}
	};

	/**
	 * @return the messageId
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * @return the totalSizeBytes
	 */
	public long getTotalSizeBytes() {
		return totalSizeBytes;
	}

	/**
	 * @return the sentBytes
	 */
	public long getSentBytes() {
		return sentBytes;
	}

	/**
	 * @return the isIncoming
	 */
	public boolean isIncoming() {
		return isIncoming;
	}

	/**
	 * @return the ownerUid
	 */
	public String getOwnerUid() {
		return ownerUid;
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
}
