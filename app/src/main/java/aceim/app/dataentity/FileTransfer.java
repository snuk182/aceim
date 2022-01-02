package aceim.app.dataentity;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.FileProgress;
import android.os.Parcel;
import android.os.Parcelable;

public class FileTransfer implements Parcelable {
	
	private final long messageId;
	private final Buddy participant;
	private FileProgress progress;

	public FileTransfer(long messageId, Buddy participant) {
		this.messageId = messageId;
		this.participant = participant;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(messageId);
		out.writeParcelable(participant, flags);
		out.writeParcelable(progress, flags);
	}

	public static final Parcelable.Creator<FileTransfer> CREATOR = new Parcelable.Creator<FileTransfer>() {
		public FileTransfer createFromParcel(Parcel in) {
			return new FileTransfer(in);
		}

		public FileTransfer[] newArray(int size) {
			return new FileTransfer[size];
		}
	};

	private FileTransfer(Parcel in) {
		messageId = in.readLong();
		participant = in.readParcelable(Buddy.class.getClassLoader());
		progress = in.readParcelable(FileProgress.class.getClassLoader());
	}

	/**
	 * @return the progress
	 */
	public FileProgress getProgress() {
		return progress;
	}

	/**
	 * @param progress the progress to set
	 */
	public void setProgress(FileProgress progress) {
		this.progress = progress;
	}

	/**
	 * @return the messageId
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * @return the participant
	 */
	public Buddy getParticipant() {
		return participant;
	}
}
