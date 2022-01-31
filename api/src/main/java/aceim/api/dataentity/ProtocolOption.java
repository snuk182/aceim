package aceim.api.dataentity;

import aceim.api.dataentity.tkv.FileTKV;
import aceim.api.dataentity.tkv.ListTKV;
import aceim.api.dataentity.tkv.StringTKV;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.dataentity.tkv.ToggleTKV;
import aceim.api.dataentity.tkv.StringTKV.ContentType;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Entity used for protocol accounts creation and management. A set of protocol options defines preferences required for account (username, password, login host etc etc).
 */
public class ProtocolOption implements Parcelable {

	/**
	 * Default option value.
	 */
	private final String defaultValue;
	
	/**
	 * Resource ID for option's label.
	 */
	private final int labelId;
	
	/**
	 * Option's type, ID (key) and current value.
	 */
	private final TKV tkv;

	protected ProtocolOption(Parcel src) {
		defaultValue = src.readString();
		labelId = src.readInt();
		tkv = src.readParcelable(TKV.class.getClassLoader());
	}

	public ProtocolOption(ProtocolOptionType type, String key, String defaultValue, int labelId, boolean isMandatory) {
		this(type, key, defaultValue, labelId, isMandatory, null, null);
	}

	public ProtocolOption(ProtocolOptionType type, String key, String defaultValue, int labelId, boolean isMandatory, String value) {
		this(type, key, defaultValue, labelId, isMandatory, value, null);
	}

	public ProtocolOption(ProtocolOptionType type, String key, String defaultValue, int labelId, boolean isMandatory, String value, Object parameter) {
		this.defaultValue = defaultValue;
		this.labelId = labelId;

		switch (type) {
		case FILE:
			this.tkv = new FileTKV(parameter.toString(), key, isMandatory, defaultValue);
			break;
		case LIST:
			this.tkv = new ListTKV((String[]) parameter, key, isMandatory, defaultValue);
			break;
		case CHECKBOX:
			this.tkv = new ToggleTKV(key, isMandatory, Boolean.getBoolean(defaultValue));
			break;
		default:
			this.tkv = new StringTKV(ContentType.fromProtocolOptionType(type), key, isMandatory, defaultValue);
			break;
		}
		
		this.tkv.setValue(value);
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(defaultValue);
		dest.writeInt(labelId);
		dest.writeParcelable(tkv, flags);
	}

	public static final Parcelable.Creator<ProtocolOption> CREATOR = new Parcelable.Creator<ProtocolOption>() {
		public ProtocolOption createFromParcel(Parcel in) {
			return new ProtocolOption(in);
		}

		public ProtocolOption[] newArray(int size) {
			return new ProtocolOption[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return tkv.getValue();
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(String value) {
		this.tkv.setValue(value);
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return tkv.getKey();
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return the labelId
	 */
	public int getLabelId() {
		return labelId;
	}

	/**
	 * @return the isMandatory
	 */
	public boolean isMandatory() {
		return tkv.isMandatory();
	}

	public enum ProtocolOptionType {
		STRING, PASSWORD, CHECKBOX, TIME, FILE, INTEGER, DOUBLE, DATE, LIST
	}

	/**
	 * @return the tkv
	 */
	public TKV getTkv() {
		return tkv;
	}
}
