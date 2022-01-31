package aceim.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.dataentity.SmileyResources;
import aceim.app.utils.PluginsManager;
import aceim.app.view.page.chat.SmileysPopup;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.EditText;

public class SmileysManager extends PluginsManager {
	
	private SortedMap<String, Drawable> mManagedSmileysCache = null;
	private final Map<String, SmileyResources> mResources = new LinkedHashMap<String, SmileyResources>(); 
	
	private static SmileysPopup sSmileysPopup;
	
	public SmileysManager(MainActivity activity) {
		super(activity, ApiConstants.ACTION_PLUGIN_SMILEYS);
		mResources.put(activity.getPackageName(), SmileyResources.mySmileys(activity));
		initSmileys();
	}
	
	private void initSmileys() {
		Logger.log("Init smileys", LoggerLevel.VERBOSE);
		
		PackageManager packageManager = mContext.getPackageManager();
		List<PackageInfo> list = packageManager.getInstalledPackages(PackageManager.GET_RESOLVED_FILTER);
		
		for (PackageInfo i : list) {
			if (i.packageName.startsWith(ApiConstants.ACTION_PLUGIN_SMILEYS)) {
				Logger.log("Smiley pack info: " + i);
				addSmileyPackage(i.packageName);
			}
		}
	}	

	@Override
	protected void onPackageAdded(String packageName) {
		addSmileyPackage(packageName);
		resetSmileysPopup();
	}

	private void removeSmileyPackage(String packageName) {
		mResources.remove(packageName);
		resetSmileysPopup();
	}

	private void addSmileyPackage(String packageName) {
		if (mResources.containsKey(packageName)) {
			removeSmileyPackage(packageName);
		}
		
		SmileyResources r = SmileyResources.fromPackageName(packageName, mContext);
		mResources.put(packageName, r);
	}

	@Override
	protected void onPackageRemoved(String packageName) {
		removeSmileyPackage(packageName);
	}

	protected MainActivity getMainActivity() {
		return (MainActivity) mContext;
	}

	/**
	 * @return the mResources
	 */
	public Map<String, SmileyResources> getResources() {
		return mResources;
	}

	private SortedMap<String, Drawable> manageSmileys() {
		if (mManagedSmileysCache == null) {
			mManagedSmileysCache = new TreeMap<String, Drawable>(new StringLengthComparator());
			
			List<SmileyResources> smileys = new ArrayList<SmileyResources>(mResources.values());
			for (int k=smileys.size()-1; k>=0; k--) {
				SmileyResources smr = smileys.get(k);
				try {
					Resources res = smr.getNativeResourcesForProtocol(getMainActivity().getPackageManager());
					for (int i = 0; i < smr.getDrawableIDs().length; i++) {
						String smiley = smr.getNames()[i];
						
						if (!mManagedSmileysCache.containsKey(smiley)) {
							mManagedSmileysCache.put(smiley, res.getDrawable(smr.getDrawableIDs()[i]));
						}
					}
				} catch (Exception e) {
					Logger.log(e);
				}
			}
		}
		
		return mManagedSmileysCache;
	}
	
	/**
	 * @return the sSmileysPopup
	 */
	public SmileysPopup getSmileysPopup() {
		if (sSmileysPopup == null) {
			sSmileysPopup = new SmileysPopup((MainActivity) mContext);
		}
		
		return sSmileysPopup;
	}

	public void resetSmileysPopup() {
		if (sSmileysPopup != null) {
			sSmileysPopup.hide();
			sSmileysPopup = null;
		}
	}
	
	/**
	 * @return the mSmileysManager
	 */
	public Collection<SmileyResources> getUnmanagedSmileys() {
		return getResources().values();
	}
	
	public SortedMap<String, Drawable> getManagedSmileys() {
		return manageSmileys();
	}
	
	public Collection<SmileyResources> getThirdPartySmileys() {
		List<SmileyResources> unmanaged = new ArrayList<SmileyResources>(getUnmanagedSmileys());
		unmanaged.remove(0);
		return Collections.unmodifiableList(unmanaged);
	}

	private static final class StringLengthComparator implements Comparator<String> {

		@Override
		public int compare(String rhs, String lhs) {
			if (lhs != null && rhs != null) {
				if (lhs.length() != rhs.length()) {
					return lhs.length() - rhs.length();
				} else {
					return lhs.compareTo(rhs);
				}
			} else {
				return lhs != null ? 1 : -1;
			}
		}		
	}

	public boolean isPopupShown() {
		return sSmileysPopup != null && sSmileysPopup.isShown();
	}

	public void hidePopup() {
		if (isPopupShown()) {
			sSmileysPopup.hide(); 
		}
	}

	public void showPopup(EditText mEditor) {
		if (sSmileysPopup == null) {
			sSmileysPopup = new SmileysPopup(getMainActivity());
		}
		
		sSmileysPopup.show(mEditor);
	}
}
