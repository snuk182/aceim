package aceim.api.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Transferring file information entity.
 * 
 * @author Sergiy Plygun
 *
 */
public class FileInfo extends Entity implements Parcelable {

	/**
	 * File size
	 */
	private long size;	
	
	/**
	 * File name (with absolute path, if outgoing, name only, if incoming)
	 */
	private String filename;
	
	public FileInfo(Parcel arg0) {
		super(arg0);
		readFromParcel(arg0);
	}

	public FileInfo(byte serviceId) {
		super(serviceId);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(filename);
		dest.writeLong(size);
	}
	
	private void readFromParcel(Parcel in){
		filename = in.readString();
		size = in.readLong();
	}

	public static final Parcelable.Creator<FileInfo> CREATOR = new Parcelable.Creator<FileInfo>(){

		@Override
		public FileInfo createFromParcel(Parcel in) {
			in.readString();
			
			return new FileInfo(in);
		}

		@Override
		public FileInfo[] newArray(int size) {
			return new FileInfo[size];
		}
	};

	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
