package aceim.app.dataentity;

import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.AceImException.AceImExceptionReason;
import aceim.app.Constants;
import aceim.app.MainActivity;
import aceim.app.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;

public class SmileyResources extends PluginResources {
	
	private final String[] names;
	private final int[] drawableIDs;

	private SmileyResources(String[] names, int[] drawableIDs, Resources resources) {
		super(Constants.SMILEY_PLUGIN_PREFIX, resources);
		this.names = names;
		this.drawableIDs = drawableIDs;
	}

	public SmileyResources(Parcel in) {
		super(in);
		
		names = in.createStringArray();
		drawableIDs = in.createIntArray();
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		
		out.writeStringArray(names);
		out.writeIntArray(drawableIDs);
	}

	public static final Parcelable.Creator<SmileyResources> CREATOR = new Parcelable.Creator<SmileyResources>() {
		public SmileyResources createFromParcel(Parcel in) {
			in.readString();
			return new SmileyResources(in);
		}

		public SmileyResources[] newArray(int size) {
			return new SmileyResources[size];
		}
	};
	
	public static SmileyResources mySmileys(MainActivity activity) {
		String[] names = activity.getResources().getStringArray(R.array.smiley_names);		
		int[] values = getValuesInternal(activity.getResources(), R.array.smiley_values);
		
		return new SmileyResources(names, values, activity.getResources());
	}
	
	public static SmileyResources fromPackageName(String packageName, Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			Resources r = pm.getResourcesForApplication(packageName);
			
			int namesId = r.getIdentifier("smiley_names", "array", packageName);
			int valuesId = r.getIdentifier("smiley_values", "array", packageName);
			
			if (namesId == 0 || valuesId == 0) {
				throw new AceImException(packageName, AceImExceptionReason.RESOURCE_NOT_INITIALIZED);
			}
			
			String[] names = r.getStringArray(namesId);			
			int[] values = getValuesInternal(r, valuesId);
			
			return new SmileyResources(names, values, r);
		} catch (Exception e) {
			Logger.log(e);
		}
		
		return null;
	}
	
	private static int[] getValuesInternal(Resources r, int arrayId){
		TypedArray a = r.obtainTypedArray(arrayId);
		
		int[] values = new int[a.length()];
		for (int i=0; i<a.length(); i++) {
			int id = a.getResourceId(i, R.drawable.logo_corner_small);
			values[i] = id;
		}
		
		a.recycle();
		
		return values;
	}

	/**
	 * @return the names
	 */
	public String[] getNames() {
		return names;
	}

	/**
	 * @return the drawableIDs
	 */
	public int[] getDrawableIDs() {
		return drawableIDs;
	}
}
