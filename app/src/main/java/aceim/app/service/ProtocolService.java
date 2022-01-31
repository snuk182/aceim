package aceim.app.service;

import java.util.concurrent.Executors;

import aceim.api.dataentity.ItemAction;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.ICoreProtocolCallback.Stub;
import aceim.api.service.IProtocolService;
import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.service.ProtocolServicesManager.ProtocolListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;


public class ProtocolService implements ServiceConnection {
	
	private Context mContext;

	private volatile boolean exiting = false;
	
	private final String packageName;
	private final String className;
	
	private ProtocolResources resources;
	
	private final ICoreProtocolCallback callback;
	private final ProtocolListener mProtocolListener;
	
	private IProtocolService protocol;
	
	private ProtocolService(Context context, String packageName, String className, ICoreProtocolCallback callback, ProtocolListener protocolListener) {
		this.packageName = packageName;
		this.className = className;
		this.callback = callback;
		this.mContext = context;
		this.mProtocolListener = protocolListener;
	}

	private void bind() {
		Intent intent = new Intent();
		intent.setClassName(packageName, className);
		
        Logger.log("binding: "+intent );
        
        mContext.startService(intent);
		boolean d = mContext.getApplicationContext().bindService(intent, this, 0);
		
		Logger.log(d ? "Binded" : "Not binded");
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		protocol = IProtocolService.Stub.asInterface(service);
		try {
			protocol.registerCallback(callback);			
			fillResources();
			mProtocolListener.onAction(this, ItemAction.JOINED);
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		protocol = null;
		if (!exiting) {
			Executors.defaultThreadFactory().newThread(mRebindProtocolsRunnable).start();
			mProtocolListener.onAction(this, ItemAction.LEFT);
		}
	}
	
	private void fillResources() throws RemoteException {
		this.resources = new ProtocolResources(this);
	}

	private ProtocolResources getFullProtocolResources() {
		ProtocolResources out = new ProtocolResources(this);
		
		out.setProtocolVersion(fillInfoField("version", packageName));
		out.setApiVersion(fillInfoField("api_version", packageName));
		
		return out;
	}
	
	private String fillInfoField(String idName, String packageName){
		Resources r;
		try {
			r = resources.getNativeResourcesForProtocol(null);
		} catch (AceImException e) {
			Logger.log(e);
			return null;
		}
		
		int id = r.getIdentifier(idName, "string", packageName);
		
		if (id != 0){
			return r.getString(id);
		} else {
			return null;
		}
	}
	
	public Context getContext() {
		return mContext;
	}

	public String getProtocolServicePackageName() {
		return packageName;
	}

	public ProtocolResources getResources(boolean getProtocolInfo) {
		if (getProtocolInfo) {
			return getFullProtocolResources();
		}
		return resources;
	}

	public ICoreProtocolCallback getCallback() {
		return callback;
	}

	public IProtocolService getProtocol() {
		return protocol;
	}

	public static ProtocolService create(Context context, String packageName, String className, Stub protocolCallback, ProtocolListener protocolListener) {
		ProtocolService ps = new ProtocolService(context, packageName, className, protocolCallback, protocolListener);
		ps.bind();
		
		while (ps.getProtocol() == null) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {}
		}
		
		return ps;
	}

	public ServiceInfo getServiceInfo() {
		try {
			return mContext.getPackageManager().getServiceInfo(new ComponentName(packageName, className), PackageManager.GET_RESOLVED_FILTER);
		} catch (NameNotFoundException e) {
			Logger.log(e);
			return null;
		}
	}

	/**
	 * @return the className
	 */
	public String getServiceClassName() {
		return className;
	}

	public void onExit() {
		exiting = true;
		try {
			getProtocol().shutdown();
		} catch (RemoteException e) {}
	}
	
	private final Runnable mRebindProtocolsRunnable = new Runnable() {
		
		@Override
		public void run() {
			bind();
		}
	};
}
