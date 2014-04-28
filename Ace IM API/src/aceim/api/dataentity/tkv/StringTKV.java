package aceim.api.dataentity.tkv;

import aceim.api.dataentity.ProtocolOption.ProtocolOptionType;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Text string {@link TKV}.
 */
public final class StringTKV extends TKV {
	
	private final ContentType contentType;

	public StringTKV(ContentType contentType, String key, boolean mandatory, String defaultValue) {
		super(key, mandatory, defaultValue);
		this.contentType = contentType;
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(contentType.name());
	}

	public static final Parcelable.Creator<StringTKV> CREATOR = new Parcelable.Creator<StringTKV>() {
		public StringTKV createFromParcel(Parcel in) {
			in.readString();
			return new StringTKV(in);
		}

		public StringTKV[] newArray(int size) {
			return new StringTKV[size];
		}
	};

	public StringTKV(Parcel in) {
		super(in);
		this.contentType = ContentType.valueOf(in.readString());
	}
	
	/**
	 * Content types for {@link StringTKV}
	 */
	public enum ContentType {
		/**
		 * Plain text.
		 */
		STRING,
		
		/**
		 * Password or data that should be hidden during input.
		 */
		PASSWORD,
		
		/**
		 * Time
		 */
		TIME,
		
		/**
		 * Integer number.
		 */
		INTEGER,
		
		/**
		 * Double number.
		 */
		DOUBLE,
		
		/**
		 * Date.
		 */
		DATE;
		
		public static ContentType fromProtocolOptionType(ProtocolOptionType type){
			switch(type) {
			case DATE:
				return DATE;
			case DOUBLE:
				return DOUBLE;
			case INTEGER:
				return INTEGER;
			case PASSWORD:
				return PASSWORD;
			case STRING:
				return STRING;
			case TIME:
				return TIME;
			default:
				return null;
			}
		}
	}

	/**
	 * @return the contentType
	 */
	public ContentType getContentType() {
		return contentType;
	}
}
