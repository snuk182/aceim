package aceim.api.dataentity;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Personal info entity
 */
public class PersonalInfo extends Entity implements Parcelable{
	
	/**
	 * Info owner's protocol UID, either buddy or account
	 */
	private String protocolUid;
	
	/**
	 * Does this info belong to Multi-User chat? (No by default)
	 */
	private boolean isMultichat = false;
	
	/**
	 * Info bundle.
	 */
	private Bundle properties;
	
	//A list of common info keys
	public static final String INFO_CHAT_DESCRIPTION = "Description";
	public static final String INFO_CHAT_OCCUPANTS = "Occupants";
	public static final String INFO_CHAT_SUBJECT = "Subject";
	
	public static final String INFO_NICK = "nick";	
	public static final String INFO_FIRST_NAME = "first-name";
	public static final String INFO_LAST_NAME = "last-name";
	public static final String INFO_EMAIL = "email";
	public static final String INFO_ICON = "icon";
	public static final String INFO_GENDER = "gender";
	public static final String INFO_AGE = "age";
	public static final String INFO_STATUS = "status";
	public static final String INFO_REQUIRES_AUTH = "auth-required";

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(protocolUid);
		dest.writeBundle(properties);
		dest.writeByte((byte) (isMultichat ? 1 : 0));
	}
	
	private PersonalInfo(Parcel in){
		super(in);
		readFromParcel(in);
	}

	public PersonalInfo(byte serviceId) {
		super(serviceId);
		properties = new Bundle();
	}

	private void readFromParcel(Parcel in) {
		protocolUid = in.readString();
		properties = in.readBundle();	
		isMultichat = in.readByte() > 0;
	}

	public static final Parcelable.Creator<PersonalInfo> CREATOR = new Parcelable.Creator<PersonalInfo>(){

		@Override
		public PersonalInfo createFromParcel(Parcel source) {
			//Omitting classname variable used for class hierarchy parcelable support
			source.readString();
			return new PersonalInfo(source);
		}

		@Override
		public PersonalInfo[] newArray(int size) {
			return new PersonalInfo[size];
		}
		
	};

	/**
	 * @return the protocolUid
	 */
	public String getProtocolUid() {
		return protocolUid;
	}

	/**
	 * @param protocolUid the protocolUid to set
	 */
	public void setProtocolUid(String protocolUid) {
		this.protocolUid = protocolUid;
	}

	/**
	 * @return the properties
	 */
	public Bundle getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Bundle properties) {
		this.properties = properties;
	}

	/**
	 * @return the isMultichat
	 */
	public boolean isMultichat() {
		return isMultichat;
	}

	/**
	 * @param isMultichat the isMultichat to set
	 */
	public void setMultichat(boolean isMultichat) {
		this.isMultichat = isMultichat;
	}
}
