package aceim.app.themeable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import android.content.res.Resources;
import android.content.res.Resources.Theme;

public class ThemesManager extends PluginsManager {
	
	private final Theme mCurrentTheme;
	private final ThemeResources mViewResources;

	public ThemesManager(Activity activity) {
		super(activity, Constants.THEME_PLUGIN_PREFIX);

		String themeId = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getString(GlobalOptionKeys.THEME.name(), null);
		Theme theme = null;
		
		try {
			Context themeContext = getThemeContext(themeId);
			if (themeContext != null) {
				Resources r = themeContext.getResources();
				themeContext.setTheme(r.getIdentifier("Ace.IM.Theme", "style", themeId));
				
				theme = themeContext.getTheme();
			} 
		} catch (NameNotFoundException ne) {
			Logger.log("No installed theme found: " + themeId, LoggerLevel.INFO);
			mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0)
					.edit()
					.remove(GlobalOptionKeys.THEME.name())
					.commit();
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			if (theme == null) {
				theme = mContext.getTheme();
			}
		}
		
		mCurrentTheme = theme;
		mViewResources = new ThemeResources.Builder().resourcesFromThemesManager(this).build();
	}

	@Override
	protected void onPackageAdded(String packageName) {}

	@Override
	protected void onPackageRemoved(String packageName) {
		((Activity)mContext).finish();
	}

	public Theme getCurrentTheme() {
		return mCurrentTheme;
	}
	
	public Context getCurrentThemeContext() {
		Context themeContext = null;
		
		String theme = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getString(GlobalOptionKeys.THEME.name(), null);
		try {			
			themeContext = getThemeContext(theme);
		} catch (NameNotFoundException ne) {
			Logger.log("No installed theme found: " + theme, LoggerLevel.INFO);
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
		
		return themeContext;
	}
	
	public Map<String, String> getInstalledThemes() {
		PackageManager packageManager = mContext.getPackageManager();
		List<PackageInfo> list = packageManager.getInstalledPackages(PackageManager.GET_RESOLVED_FILTER);
		
		Map<String, String> themes = new HashMap<String, String>(list.size());
		for (PackageInfo i : list) {
			if (i.packageName.startsWith(Constants.THEME_PLUGIN_PREFIX)) {
				Logger.log("Theme info: " + i);
				themes.put(i.packageName, i.applicationInfo.loadLabel(packageManager).toString());
			}
		}
		
		return themes;
	}
	
	private Context getThemeContext(String themeContainer) throws NameNotFoundException {
		return mContext.getApplicationContext().createPackageContext(themeContainer, Context.CONTEXT_IGNORE_SECURITY);
	}
	
	/**
	 * @return the mViewResources
	 */
	public ThemeResources getViewResources() {
		return mViewResources;
	}
}
