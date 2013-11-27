package aceim.app.dataentity;

import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.ICoreProtocolCallback.Stub;
import aceim.api.service.IProtocolService;
import aceim.api.utils.Logger;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;


public class ProtocolService implements ServiceConnection {
	
	private Context mContext;

	//private final List<Account> mAccounts = new ArrayList<Account>();
	
	private final String packageName;
	private final String className;
	private ProtocolResources resources;
	private final ICoreProtocolCallback callback;
	
	private IProtocolService protocol;
	
	private ProtocolService(Context context, String packageName, String className, ICoreProtocolCallback callback) {
		this.packageName = packageName;
		this.className = className;
		this.callback = callback;
		this.mContext = context;
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
		} catch (RemoteException e) {
			Logger.log(e);
		}
	}

	private void fillResources() throws RemoteException {
		this.resources = new ProtocolResources(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		
	}
	
	public Context getContext() {
		return mContext;
	}

	public String getProtocolServicePackageName() {
		return packageName;
	}

	public ProtocolResources getResources() {
		return resources;
	}

	public ICoreProtocolCallback getCallback() {
		return callback;
	}

	public IProtocolService getProtocol() {
		return protocol;
	}

	public static ProtocolService create(Context context, String packageName, String className, Stub protocolCallback) {
		ProtocolService ps = new ProtocolService(context, packageName, className, protocolCallback);
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
}
