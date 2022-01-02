package aceim.api.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A set of possible connection states
 */
public enum ConnectionState implements Parcelable {
	/**
	 * Disconnected from network
	 */
	DISCONNECTED,
	
	/**
	 * Connection is being established (both physical and logical)
	 */
	CONNECTING,
	
	/**
	 * Connected to network
	 */
	CONNECTED,
	
	/**
	 * Connection is about to be lost (does not really used, though)
	 */
	DISCONNECTING;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name());
	}
	
	public static final Creator<ConnectionState> CREATOR = new Creator<ConnectionState>() {
        @Override
        public ConnectionState createFromParcel(final Parcel source) {
            return ConnectionState.valueOf(source.readString());
        }

        @Override
        public ConnectionState[] newArray(final int size) {
            return new ConnectionState[size];
        }
    };
}