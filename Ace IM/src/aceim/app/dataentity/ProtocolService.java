package aceim.app.dataentity;

import aceim.api.service.ICoreProtocolCallback;
import aceim.api.service.ICoreProtocolCallback.Stub;
import aceim.api.service.IProtocolService;
import aceim.api.utils.Logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;


public class ProtocolService implements ServiceConnection {
	
	private Context mContext;

	//private final List<Account> mAccounts = new ArrayList<Account>();
	
	private final ServiceInfo serviceInfo;
	private ProtocolResources resources;
	private final ICoreProtocolCallback callback;
	
	private IProtocolService protocol;
	
	public ProtocolService(Context context, ServiceInfo serviceInfo, ICoreProtocolCallback callback) {
		this.serviceInfo = serviceInfo;
		this.callback = callback;
		this.mContext = context;
		
		bind();
	}

	private void bind() {
		Intent intent = new Intent();
		intent.setClassName(serviceInfo.packageName, serviceInfo.name);
        Logger.log("binding: "+intent );
        mContext.startService(intent);
		boolean d = mContext.bindService(intent, this, 0);
		Logger.log(Boolean.toString(d));
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		protocol = IProtocolService.Stub.asInterface(service);
		try {
			protocol.registerCallback(callback);
			
			fillResources();
			
			/*for (Account account: mAccounts) {
				protocol.addAccount(account.serviceId, account.protocolUid);
			}*/
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
	
	/*public void addAccount(Account account) {
		mAccounts.add(account);
		
		if (protocol == null) {
			bind();
		} else {
			try {
				protocol.addAccount(account.serviceId, account.protocolUid);
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}		
	}
	
	public void removeAccount(Account account) {
		mAccounts.remove(account);
		
		if (mAccounts.size() < 1 && protocol != null) {
			try {
				protocol.shutdown();
			} catch (RemoteException e) {
				Logger.log(e);
			}
		}
	}

	*/public Context getContext() {
		return mContext;
	}

	public String getProtocolServicePackageName() {
		return serviceInfo.packageName;
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

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public static ProtocolService create(Context context, ServiceInfo sinfo, Stub protocolCallback) {
		ProtocolService ps = new ProtocolService(context, sinfo, protocolCallback);
		while (ps.getProtocol() == null) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {}
		}
		
		return ps;
	}
}
