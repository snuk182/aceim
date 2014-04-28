package aceim.api.dataentity;

import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File transfer message entity.
 * 
 * @author Sergiy Plygun
 *
 */
public class FileMessage extends Message {
	
	/**
	 * List of files being transferred, read-only.
	 */
	private final List<FileInfo> files;
	
	@SuppressWarnings("unchecked")
	public FileMessage(Parcel arg0) {
		super(arg0);
		files = Collections.unmodifiableList(arg0.readArrayList(FileInfo.class.getClassLoader()));
	}
	
	public FileMessage(byte serviceId, String from, List<FileInfo> files){
		super(serviceId, from);
		this.files = Collections.unmodifiableList(files);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeList(files);
	}
	
	public static final Parcelable.Creator<FileMessage> CREATOR = new Parcelable.Creator<FileMessage>(){

		@Override
		public FileMessage createFromParcel(Parcel arg0) {
			//Omitting classname variable used for class hierarchy parcelable support
			arg0.readString();
			return new FileMessage(arg0);
		}

		@Override
		public FileMessage[] newArray(int size) {
			return new FileMessage[size];
		}

	};

	/**
	 * @return the files
	 */
	public List<FileInfo> getFiles() {
		return files;
	}
}
