package aceim.api.dataentity;

import aceim.api.utils.Utils;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * List item picker feature. Used, for instance, for enumerated network status changes (online, away, busy etc). 
 * 
 */
public class ListFeature extends ProtocolServiceFeature {
	
	/**
	 * Array of resource IDs for human readable list item names, corresponding to drawables array.
	 */
	private final int[] names;
	
	/**
	 * Array of resource IDs for list item drawables, corresponding to names array.
	 */
	private final int[] drawables;
	
	/**
	 * May the value of this feature be null (no item picked, even by default) ?
	 */
	private final boolean isNullable;
	
	public ListFeature(String featureId, String featureName, int iconId, boolean showInIconList, boolean editable, boolean availableOffline, int[] names, int[] drawables, boolean isNullable, ProtocolServiceFeatureTarget[] targets) {
		super(featureId, featureName, iconId, showInIconList, editable, availableOffline, targets);
		this.names = names;
		this.drawables = drawables;
		this.isNullable = isNullable;
	}

	public ListFeature(Parcel in) {
		super(in);
		names = in.createIntArray();
		drawables = in.createIntArray();
		isNullable = in.readByte() > 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeIntArray(names);
		out.writeIntArray(drawables);
		out.writeByte((byte) (isNullable ? 1 : 0));
	}

	public static final Parcelable.Creator<ListFeature> CREATOR = new Parcelable.Creator<ListFeature>() {
		public ListFeature createFromParcel(Parcel in) {
			return Utils.unparcelEntity(in, ListFeature.class);
		}

		public ListFeature[] newArray(int size) {
			return new ListFeature[size];
		}
	};

	/**
	 * @return the names
	 */
	public int[] getNames() {
		return names;
	}

	/**
	 * @return the drawables
	 */
	public int[] getDrawables() {
		return drawables;
	}

	/**
	 * @return the isNullable
	 */
	public boolean isNullable() {
		return isNullable;
	}
}
