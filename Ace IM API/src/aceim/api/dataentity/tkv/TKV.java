package aceim.api.dataentity.tkv;

import java.lang.reflect.Constructor;

import aceim.api.utils.Logger;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * General item entity for input forms. 
 * 
 */
public abstract class TKV implements Parcelable {
	
	/**
	 * Is item mandatory?
	 */
	private final boolean mandatory;
	
	/**
	 * Item key.
	 */
	private final String key;
	
	/**
	 * Item value.
	 */
	private String value;
	
	protected TKV(String key){
		this(key, false, null);
	}
	
	protected TKV(String key, boolean mandatory){
		this(key, mandatory, null);
	}
	
	protected TKV(String key, boolean mandatory, String defaultValue) {
		this.key = key;
		this.mandatory = mandatory;
		this.value = defaultValue;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		// The trick to support inheritance.
		out.writeString(getClass().getName());
		
		out.writeByte((byte) (mandatory ? 1 : 0));
		out.writeString(key);
		out.writeString(value);
	}

	public static final Parcelable.Creator<TKV> CREATOR = new Parcelable.Creator<TKV>() {
		@SuppressWarnings("unchecked")
		public TKV createFromParcel(Parcel in) {
			String className = in.readString();
			try {
				Class<? extends TKV> cls = (Class<? extends TKV>) Class.forName(className);
				Class<?>[] paramTypes = { Parcel.class }; 
				Constructor<? extends TKV> constructor = cls.getConstructor(paramTypes);
				return constructor.newInstance(in);
			} catch (Exception e) {
				Logger.log(e);
			}
			
			return null;
		}

		public TKV[] newArray(int size) {
			return new TKV[size];
		}
	};

	protected TKV(Parcel in) {
		mandatory = in.readByte() > 0;
		key = in.readString();
		value = in.readString();
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the mandatory
	 */
	public boolean isMandatory() {
		return mandatory;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	
	@Override
	public String toString() {
		return getValue();
	}
}
