package aceim.api.dataentity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Service message entity. 
 */
public class ServiceMessage extends Message {
	
	/**
	 * Does this message require accept or decline answer (like authorization request) ?
	 */
	private final boolean requiresAcceptDeclineAnswer;
	
	public ServiceMessage(Parcel arg0) {
		super(arg0);
		requiresAcceptDeclineAnswer = arg0.readByte() > 0;
	}
	
	/**
	 * @param serviceId Owner's service ID.
	 * @param contactUid Owner's protocol UID.
	 * @param requiresAcceptDeclineAnswer Does this message require accept or decline answer (like authorization request) ?
	 */
	public ServiceMessage(byte serviceId, String contactUid, boolean requiresAcceptDeclineAnswer){
		super(serviceId, contactUid);
		this.requiresAcceptDeclineAnswer = requiresAcceptDeclineAnswer;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeByte((byte) (requiresAcceptDeclineAnswer ? 1 : 0));
	}
	
	public static final Parcelable.Creator<ServiceMessage> CREATOR = new Parcelable.Creator<ServiceMessage>(){

		@Override
		public ServiceMessage createFromParcel(Parcel arg0) {
			//Omitting classname variable used for class hierarchy parcelable support
			arg0.readString();
			return new ServiceMessage(arg0);
		}

		@Override
		public ServiceMessage[] newArray(int size) {
			return new ServiceMessage[size];
		}
		
	};

	/**
	 * @return the requiresAcceptDeclineAnswer
	 */
	public boolean isRequireAcceptDeclineAnswer() {
		return requiresAcceptDeclineAnswer;
	}
}
