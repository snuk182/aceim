package aceim.app.dataentity;

import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.AceImException.AceImExceptionReason;
import aceim.app.MainActivity;
import aceim.app.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;

public class SmileyResources extends PluginResources {
	
	private final String smileyPackShortName;
	/**
	 * @return the smileyPackShortName
	 */
	public String getSmileyPackShortName() {
		return smileyPackShortName;
	}

	private final String[] names;
	private final int[] drawableIDs;

	private SmileyResources(String packageName, String[] names, int[] drawableIDs, String pluginName, String smileyPackShortName, Resources resources) {
		super(packageName, resources);
		this.names = names;
		this.drawableIDs = drawableIDs;
		this.smileyPackShortName = smileyPackShortName;
		
		setPluginName(pluginName);
	}

	public SmileyResources(Parcel in) {
		super(in);
		
		names = in.createStringArray();
		drawableIDs = in.createIntArray();
		smileyPackShortName = in.readString();
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		
		out.writeStringArray(names);
		out.writeIntArray(drawableIDs);
		out.writeString(smileyPackShortName);
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
		Resources r = activity.getResources();
		String[] names = r.getStringArray(R.array.smiley_names);		
		int[] values = getValuesInternal(r, R.array.smiley_values);
		String smileyPackShortName = r.getString(R.string.smileys);
		
		String pluginName = r.getString(activity.getApplicationInfo().labelRes);
		
		return new SmileyResources(activity.getPackageName(), names, values, pluginName, smileyPackShortName, r);
	}
	
	public static SmileyResources fromPackageName(String packageName, Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			Resources r = pm.getResourcesForApplication(packageName);
			
			int namesId = r.getIdentifier("smiley_names", "array", packageName);
			int valuesId = r.getIdentifier("smiley_values", "array", packageName);
			int shortNameId = r.getIdentifier("short_name", "string", packageName);
			
			if (namesId == 0 || valuesId == 0) {
				throw new AceImException(packageName, AceImExceptionReason.RESOURCE_NOT_INITIALIZED);
			}
			
			String[] names = r.getStringArray(namesId);			
			int[] values = getValuesInternal(r, valuesId);
			
			String pluginName = r.getString(context.getPackageManager().getApplicationInfo(packageName, 0).labelRes); 
			String smileyShortName = r.getString(shortNameId);
			
			return new SmileyResources(packageName, names, values, pluginName, smileyShortName, r);
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
