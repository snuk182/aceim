package aceim.api.dataentity;

import aceim.api.utils.Utils;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Base API entity
 * @author Sergiy P
 *
 */
public abstract class Entity implements Parcelable {

	/**
	 * Owner account's service ID, mandatory for all entities.
	 */
	private final byte serviceId;

	protected Entity(Parcel source) {
		serviceId = source.readByte();
	}

	/**
	 * @param serviceId owner account's service ID
	 */
	protected Entity(byte serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	/**
	 * Please call super.writeToParcel(Parcel, int) during overriding this method.
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// The trick to support inheritance.
		dest.writeString(getClass().getName());

		dest.writeByte(serviceId);
	}

	/**
	 * @return the serviceId
	 */
	public byte getServiceId() {
		return serviceId;
	}

	public static final Parcelable.Creator<Entity> CREATOR = new Parcelable.Creator<Entity>() {
		public Entity createFromParcel(Parcel in) {
			String className = in.readString();
			
			return Utils.unparcelUnknownEntity(in, className);
		}

		public Entity[] newArray(int size) {
			return new Entity[size];
		}
	};
}
