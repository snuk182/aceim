package aceim.app.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.ItemAction;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.dataentity.ProtocolService;
import aceim.app.utils.LinqRules.StringCompareLinqRule;
import aceim.app.utils.linq.KindaLinq;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

class ProtocolServicesManager {

	private final List<ProtocolListener> mListeners = new LinkedList<ProtocolListener>();
	private final Map<String, ProtocolService> mProtocols = new HashMap<String, ProtocolService>();
	private final ICoreProtocolCallback.Stub mProtocolCallback;
	
	private final Context mContext;
	private PackageBroadcastReceiver packageBroadcastReceiver;
	private IntentFilter packageFilter;
	
	ProtocolServicesManager(Context mContext, ICoreProtocolCallback.Stub protocolCallback) {
		this.mContext = mContext;
		this.mProtocolCallback = protocolCallback;
		
		packageBroadcastReceiver = new PackageBroadcastReceiver();
	    packageFilter = new IntentFilter();
	    packageFilter.addAction( Intent.ACTION_PACKAGE_ADDED  );  
	    packageFilter.addAction( Intent.ACTION_PACKAGE_REPLACED );
	    packageFilter.addAction( Intent.ACTION_PACKAGE_REMOVED );
	    packageFilter.addCategory( Intent.CATEGORY_DEFAULT );
	    packageFilter.addDataScheme( "package" );
	    
	    mContext.registerReceiver( packageBroadcastReceiver, packageFilter );
	}
	
	void initProtocolServices(String packageName) {
		Logger.log("Init protocol services", LoggerLevel.VERBOSE);
		
		PackageManager packageManager = mContext.getPackageManager();
		Intent baseIntent = new Intent(ApiConstants.ACTION_PLUGIN);
		baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
		List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);
		Logger.log("Plugins found: " + list);
		
		for (int i = 0; i < list.size(); ++i) {
			ResolveInfo info = list.get(i);
			ServiceInfo sinfo = info.serviceInfo;
			
			if (packageName != null && !sinfo.packageName.equals(packageName)) {
				continue;
			}
			
			Logger.log("Plugin info: " + sinfo);
			if (sinfo != null) {
				boolean newPackage = mProtocols.get(sinfo.packageName) == null;
				
				ProtocolService ps = ProtocolService.create(mContext, sinfo, mProtocolCallback);
				mProtocols.put(sinfo.packageName, ps);
				
				for (ProtocolListener l : mListeners){
					l.onAction(ps, newPackage ? ItemAction.ADDED : ItemAction.MODIFIED);
				}	
			}
		}
	}	
	
	void addProtocolListener(ProtocolListener listener){
		this.mListeners.add(listener);
	}
	
	void removeProtocolListener(ProtocolListener listener){
		this.mListeners.remove(listener);
	}
	
	void onExit(){
		mContext.unregisterReceiver(packageBroadcastReceiver);
	}
	
	Map<String, ProtocolService> getProtocols() {
		return mProtocols;
	}

	private void removeProtocolService(String intentSource) {
		Logger.log("Remove protocol " + intentSource, LoggerLevel.VERBOSE);
		ProtocolService ps = mProtocols.remove(intentSource);
		
		if (ps == null) {
			Logger.log("No protocol to fire 'onRemove' for "+intentSource, LoggerLevel.INFO);
			return;
		}
		
		for (ProtocolListener l : mListeners) {
			l.onAction(ps, ItemAction.DELETED);
		}
	}
	
	private class PackageBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String intentSource = intent.getData().getSchemeSpecificPart();
			if (intentSource.startsWith(ApiConstants.ACTION_PLUGIN) || isProtocolAlreadyInstalled(intentSource)) {
				Logger.log("Protocol broadcast receiver caught: " + intent.getAction()+" "+intent.getDataString());
				if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) || intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
					initProtocolServices(intentSource);
				} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
					removeProtocolService(intentSource);
				}
			} 		
		}

		private boolean isProtocolAlreadyInstalled(String intentSource) {
			return KindaLinq.from(mProtocols.keySet()).where(new StringCompareLinqRule(intentSource)).first() != null;
		}
	}
	
	interface ProtocolListener {
		void onAction(ProtocolService protocol, ItemAction action);
	}
}
