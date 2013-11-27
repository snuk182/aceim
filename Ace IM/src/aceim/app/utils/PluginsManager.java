package aceim.app.utils;

import aceim.api.utils.Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class PluginsManager {
	
	protected final String packagePrefix;
	protected final Context mContext;
	
	private final PackageBroadcastReceiver packageBroadcastReceiver;
	private final IntentFilter packageFilter;
	
	protected PluginsManager(Context context, String packagePrefix) {
		this.mContext = context;
		this.packagePrefix = packagePrefix;
		
		packageBroadcastReceiver = new PackageBroadcastReceiver();
	    packageFilter = new IntentFilter();
	    packageFilter.addAction( Intent.ACTION_PACKAGE_ADDED  );  
	    packageFilter.addAction( Intent.ACTION_PACKAGE_REPLACED );
	    packageFilter.addAction( Intent.ACTION_PACKAGE_REMOVED );
	    packageFilter.addCategory( Intent.CATEGORY_DEFAULT );
	    packageFilter.addDataScheme( "package" );
	    
	    mContext.registerReceiver( packageBroadcastReceiver, packageFilter );
	}

	private class PackageBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String intentSource = intent.getData().getSchemeSpecificPart();
			if (intentSource.startsWith(packagePrefix)) {
				Logger.log("Protocol broadcast receiver caught: " + intent.getAction()+" "+intent.getDataString());
				if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) || intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
					onPackageAdded(intentSource);
				} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
					onPackageRemoved(intentSource);
				}
			} 		
		}
	}
	
	public void onExit(){
		mContext.unregisterReceiver(packageBroadcastReceiver);
	}
	
	protected abstract void onPackageAdded(String packageName);
	protected abstract void onPackageRemoved(String packageName);
}
