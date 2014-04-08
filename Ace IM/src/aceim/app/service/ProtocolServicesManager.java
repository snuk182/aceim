package aceim.app.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.ItemAction;
import aceim.api.service.ApiConstants;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.utils.PluginsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

class ProtocolServicesManager extends PluginsManager {

	private final ProtocolListener mListener;
	private final Map<String, ProtocolService> mProtocols = new HashMap<String, ProtocolService>();
	private final ICoreProtocolCallback.Stub mProtocolCallback;
	
	ProtocolServicesManager(Context context, ICoreProtocolCallback.Stub protocolCallback, ProtocolListener listener) {
		super(context, ApiConstants.ACTION_PLUGIN_PROTOCOL);
		this.mProtocolCallback = protocolCallback;	
		this.mListener = listener;
	}
	
	void initProtocolServices() {
		Logger.log("Init protocol services", LoggerLevel.VERBOSE);
		
		PackageManager packageManager = mContext.getPackageManager();
		Intent baseIntent = new Intent(ApiConstants.ACTION_PLUGIN_PROTOCOL);
		baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
		List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);
		
		for (int i = 0; i < list.size(); ++i) {
			ResolveInfo info = list.get(i);
			ServiceInfo sinfo = info.serviceInfo;
			
			Logger.log("Plugin info: " + sinfo);
			
			initProtocolService(sinfo);
		}
	}	
	
	List<ProtocolService> getProtocolsList() {
		return Collections.unmodifiableList(new ArrayList<ProtocolService>(mProtocols.values()));
	}
	
	ProtocolService getProtocolServiceByName(String protocolServiceClassName) {
		return mProtocols.get(protocolServiceClassName);
	}

	private void removeProtocolService(String intentSource) {
		Logger.log("Remove protocol " + intentSource, LoggerLevel.VERBOSE);
		ProtocolService ps = mProtocols.remove(intentSource);
		
		if (ps == null) {
			Logger.log("No protocol to fire 'onRemove' for "+intentSource, LoggerLevel.INFO);
			return;
		} else {
			mListener.onAction(ps, ItemAction.DELETED);
		}		
	}
	
	@Override
	public void onExit(){
		super.onExit();
		for (ProtocolService ps : mProtocols.values()) {
			ps.onExit();
		}
	}
	
	@Override
	protected void onPackageAdded(String packageName) {
		Logger.log("Package added: " + packageName, LoggerLevel.VERBOSE);
		
		/*if (mProtocols.containsKey(packageName)) {
			removeProtocolService(packageName);
		}	*/
		
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
		
		boolean newPackage = !mProtocols.containsKey(sinfo.packageName);
		
		mListener.onAction(initProtocolService(sinfo), newPackage ? ItemAction.ADDED : ItemAction.MODIFIED);
	}

	@Override
	protected void onPackageRemoved(String packageName) {
		Logger.log("Package removed: " + packageName, LoggerLevel.VERBOSE);
		removeProtocolService(packageName);
	}
	
	private ProtocolService initProtocolService(ServiceInfo sinfo) {
		
		ProtocolService ps = ProtocolService.create(mContext, sinfo.packageName, sinfo.name, mProtocolCallback, mListener);
		mProtocols.put(sinfo.packageName, ps);
		return ps;
	}
	
	interface ProtocolListener {
		void onAction(ProtocolService protocol, ItemAction action);
	}
}
