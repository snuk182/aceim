package aceim.app.service;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.BuddyGroup;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.RemoteException;

public final class ServiceUtils {
	
	private ServiceUtils(){}

	public static AccountService makeAccount(Account account, ProtocolServicesManager protocolsManager) {
		Logger.log("Make account service for " + account.getAccountId(), LoggerLevel.VERBOSE);
		ProtocolService s = protocolsManager.getProtocolServiceByName(account.getProtocolServicePackageName());
		
		if (s != null) {
			try {
				s.getProtocol().addAccount(account.getServiceId(), account.getProtocolUid());
			} catch (RemoteException e) {
				Logger.log(e);
			} catch (Exception e) {
				Logger.log(e);
			}
		} else {
			Logger.log("No protocol service found for " + account.getAccountId() + " with name " + account.getProtocolServicePackageName() , LoggerLevel.INFO);
		}
		
		return new AccountService(account, s);
	}

	@SuppressLint("InlinedApi")
	public static final int getAccessMode(){
		int mode = Context.MODE_PRIVATE;
		if (Build.VERSION.SDK_INT > 10){
			mode = Context.MODE_MULTI_PROCESS;
		}
		
		return mode;
	}

	public static final int getRequestCodeForActivity(int code) {
		if (code < 0) {
			code = -code;
		}
		return code < Short.MAX_VALUE ? code : code >> 16;
	}
	
	public static Buddy cloneBuddy(Buddy origin){
		Buddy clone = new Buddy(origin.getProtocolUid(), origin.getOwnerUid(), origin.getServiceName(), origin.getServiceId());
		clone.merge(origin);
		return clone;
	}
	
	public static BuddyGroup cloneBuddyGroup(BuddyGroup origin){
		BuddyGroup clone = new BuddyGroup(origin.getId(), origin.getOwnerUid(), origin.getServiceId());
		clone.setCollapsed(origin.isCollapsed());
		clone.getBuddyList().addAll(origin.getBuddyList());
		return clone;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static void toClipboard(Context context, String text, String title) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipMan = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			clipMan.setText(text);
		} else {
			android.content.ClipboardManager clipMan = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText(title!=null ? title : context.getString(R.string.app_name), text);
			clipMan.setPrimaryClip(clip);
		}
	}
}
