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
import aceim.app.utils.PluginsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

class ProtocolServicesManager extends PluginsManager {

	private final List<ProtocolListener> mListeners = new LinkedList<ProtocolListener>();
	private final Map<String, ProtocolService> mProtocols = new HashMap<String, ProtocolService>();
	private final ICoreProtocolCallback.Stub mProtocolCallback;
	
	ProtocolServicesManager(Context context, ICoreProtocolCallback.Stub protocolCallback) {
		super(context, ApiConstants.ACTION_PLUGIN);
		this.mProtocolCallback = protocolCallback;	
	}
	
	void initProtocolServices() {
		Logger.log("Init protocol services", LoggerLevel.VERBOSE);
		
		PackageManager packageManager = mContext.getPackageManager();
		Intent baseIntent = new Intent(ApiConstants.ACTION_PLUGIN);
		baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
		List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);
		
		for (int i = 0; i < list.size(); ++i) {
			ResolveInfo info = list.get(i);
			ServiceInfo sinfo = info.serviceInfo;
			
			Logger.log("Plugin info: " + sinfo);
			
			initProtocolService(sinfo);
		}
	}	
	
	void addProtocolListener(ProtocolListener listener){
		this.mListeners.add(listener);
	}
	
	void removeProtocolListener(ProtocolListener listener){
		this.mListeners.remove(listener);
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
		} else {
			for (ProtocolListener l : mListeners) {
				l.onAction(ps, ItemAction.DELETED);
			}
		}		
	}
	
	interface ProtocolListener {
		void onAction(ProtocolService protocol, ItemAction action);
	}

	@Override
	protected void onPackageAdded(String packageName) {
		if (mProtocols.containsKey(packageName)) {
			removeProtocolService(packageName);
		}	
		
		PackageManager packageManager = mContext.getPackageManager();
		
		ServiceInfo sinfo = null;
		
		try {
			PackageInfo info = packageManager.getPackageInfo(packageName, PackageManager.GET_RESOLVED_FILTER);
			
			for (ServiceInfo i : info.services) {
				if (i.name.equals(packageName)) {
					sinfo = i;
					break;
				}
			}
		} catch (Exception e) {
			Logger.log(e);
		}
		
		if (sinfo == null) {
			Logger.log("No protocol service found within a package "+packageName, LoggerLevel.WARNING);
			return;
		}
		
		initProtocolService(sinfo);
	}

	@Override
	protected void onPackageRemoved(String packageName) {
		removeProtocolService(packageName);
	}
	
	private void initProtocolService(ServiceInfo sinfo) {
		boolean newPackage = mProtocols.containsKey(sinfo.packageName);
		
		ProtocolService ps = ProtocolService.create(mContext, sinfo.packageName, sinfo.name, mProtocolCallback);
		mProtocols.put(sinfo.packageName, ps);
		
		for (ProtocolListener l : mListeners){
			l.onAction(ps, newPackage ? ItemAction.ADDED : ItemAction.MODIFIED);
		}	
	}
}
