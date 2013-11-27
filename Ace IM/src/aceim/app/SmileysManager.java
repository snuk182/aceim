package aceim.app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.dataentity.SmileyResources;
import aceim.app.utils.LinqRules.PageWithSmileysLinqRule;
import aceim.app.utils.PluginsManager;
import aceim.app.view.page.Page;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

class SmileysManager extends PluginsManager {
	
	private final Map<String, SmileyResources> mResources = new LinkedHashMap<String, SmileyResources>(); 
	
	public SmileysManager(MainActivity activity) {
		super(activity, Constants.SMILEY_PLUGIN_PREFIX);
		mResources.put(getClass().getPackage().getName(), SmileyResources.mySmileys(activity));
		initSmileys();
	}
	
	private void initSmileys() {
		Logger.log("Init smileys", LoggerLevel.VERBOSE);
		
		PackageManager packageManager = mContext.getPackageManager();
		List<PackageInfo> list = packageManager.getInstalledPackages(PackageManager.GET_RESOLVED_FILTER);
		
		for (PackageInfo i : list) {
			if (i.packageName.startsWith(Constants.SMILEY_PLUGIN_PREFIX)) {
				Logger.log("Smiley pack info: " + i);
				addSmileyPackage(i.packageName);
			}
		}
	}	

	@Override
	protected void onPackageAdded(String packageName) {
		addSmileyPackage(packageName);
	}

	private void removeSmileyPackage(String packageName) {
		MainActivity a = getMainActivity();
		
		List<Page> pages = a.getScreen().findPagesByRule(new PageWithSmileysLinqRule());
		
		for (Page p : pages) {
			a.getScreen().removePage(p);
		}
		
		mResources.remove(packageName);
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
}
