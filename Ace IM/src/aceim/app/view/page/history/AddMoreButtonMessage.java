package aceim.app.view.page.history;

import aceim.api.dataentity.ServiceMessage;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMoreButtonMessage extends ServiceMessage {

	public AddMoreButtonMessage(Parcel arg0) {
		super(arg0);
	}

	public AddMoreButtonMessage(byte serviceId, String contactUid, String message) {
		super(serviceId, contactUid, false);
		setText(message);
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
	}

	public static final Parcelable.Creator<AddMoreButtonMessage> CREATOR = new Parcelable.Creator<AddMoreButtonMessage>() {
		public AddMoreButtonMessage createFromParcel(Parcel in) {
			in.readString();
			return new AddMoreButtonMessage(in);
		}

		public AddMoreButtonMessage[] newArray(int size) {
			return new AddMoreButtonMessage[size];
		}
	};
}
