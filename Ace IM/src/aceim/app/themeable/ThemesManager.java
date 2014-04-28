package aceim.app.themeable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.Constants;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.utils.PluginsManager;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class ThemesManager extends PluginsManager {
	
	private final ThemeResources mViewResources;
	private final Context mCurrentThemeContext;

	public ThemesManager(Activity activity) {
		super(activity, ApiConstants.ACTION_PLUGIN_THEME);

		String themeId = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getString(GlobalOptionKeys.THEME.name(), null);
		Context themeContext = null;
		
		try {
			themeContext = getThemeContext(themeId);
		} catch (NameNotFoundException ne) {
			Logger.log("No installed theme found: " + themeId, LoggerLevel.INFO);
			mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0)
					.edit()
					.remove(GlobalOptionKeys.THEME.name())
					.commit();
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			if (themeContext == null) {
				themeContext = mContext;
			}
		}
		
		mCurrentThemeContext = themeContext;
		mViewResources = new ThemeResources.Builder().resourcesFromThemesManager(this).build();
	}

	@Override
	protected void onPackageAdded(String packageName) {}

	@Override
	protected void onPackageRemoved(String packageName) {
		((Activity)mContext).finish();
	}

	public Context getCurrentThemeContext() {
		return mCurrentThemeContext;
	}
	
	public Map<String, String> getInstalledThemes() {
		PackageManager packageManager = mContext.getPackageManager();
		List<PackageInfo> list = packageManager.getInstalledPackages(PackageManager.GET_RESOLVED_FILTER);
		
		Map<String, String> themes = new HashMap<String, String>(list.size());
		for (PackageInfo i : list) {
			if (i.packageName.startsWith(ApiConstants.ACTION_PLUGIN_THEME)) {
				Logger.log("Theme info: " + i);
				themes.put(i.packageName, i.applicationInfo.loadLabel(packageManager).toString());
			}
		}
		
		return themes;
	}
	
	private Context getThemeContext(String themeContainer) throws NameNotFoundException {
		if (mContext.getPackageName().equals(themeContainer)) {
			return mContext;
		} else {
			return mContext.getApplicationContext().createPackageContext(themeContainer, Context.CONTEXT_IGNORE_SECURITY);
		}
	}
	
	/**
	 * @return the mViewResources
	 */
	public ThemeResources getViewResources() {
		return mViewResources;
	}
}
