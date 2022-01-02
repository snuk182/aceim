package aceim.api.dataentity;

import aceim.api.IProtocol;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Action being applied to item. See {@link IProtocol#buddyAction(ItemAction, Buddy)} for usage example.
 * 
 */
public enum ItemAction implements Parcelable {
	/**
	 * Item being added.
	 */
	ADDED,
	
	/**
	 * Item being edited (renamed).
	 */
	MODIFIED,
	
	/**
	 * Item being removed.
	 */
	DELETED,
	
	/**
	 * Item being joined (used for multi user chats only).
	 */
	JOINED,
	
	/**
	 * Item being left (used for multi user chats only).
	 */
	LEFT;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name());
	}
	
	public static final Creator<ItemAction> CREATOR = new Creator<ItemAction>() {
        @Override
        public ItemAction createFromParcel(final Parcel source) {
            return ItemAction.valueOf(source.readString());
        }

        @Override
        public ItemAction[] newArray(final int size) {
            return new ItemAction[size];
        }
    };
}
