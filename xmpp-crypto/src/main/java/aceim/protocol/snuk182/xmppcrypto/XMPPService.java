package aceim.protocol.snuk182.xmppcrypto;

import java.security.Security;
import java.util.Arrays;

import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.provider.EncryptedDataProvider;

import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.service.ICoreProtocolCallback;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.protocol.snuk182.xmpp.common.XMPPCommonService;
import aceim.protocol.snuk182.xmpp.common.XMPPEntityAdapter;
import aceim.protocol.snuk182.xmppcrypto.utils.ResourceUtils;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;

public class XMPPService extends XMPPCommonService {
	
	@Override
	protected XMPPEntityAdapter initEntityAdapter() {
		return new XMPPCryptoEntityAdapter();
	}
	
	public XMPPService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}
	
	@Override
	protected void doSetFeature(String featureId, OnlineInfo info){
		if (featureId.equals(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_ON)) {
			encryptionOff(info);
		} else if (featureId.equals(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF)) {
			encryptionOn(info);
		} else if (featureId.equals(XMPPCryptoApiConstants.FEATURE_ADD_PUBLIC_KEY)) {
			processAddPublicKeyFeature(featureId, info);
		} else if (featureId.equals(XMPPCryptoApiConstants.FEATURE_REMOVE_PUBLIC_KEY)) {
			removeBuddyPGPKey(info);
		} else {
			super.doSetFeature(featureId, info);
		}
	}

	public void addBuddyPGPKey(OnlineInfo info, String pgp) {
		Editor e = getContext().getSharedPreferences(getProtocolUid(), 0).edit();
		e.putString(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid(), pgp);
		e.commit();
		
		info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_ADD_PUBLIC_KEY);
		info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF, true);
		info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_REMOVE_PUBLIC_KEY, true);
		
		//mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		getCoreService().buddyStateChanged(Arrays.asList(info));
	}

	public void removeBuddyPGPKey(OnlineInfo info) {
		Editor e = getContext().getSharedPreferences(getProtocolUid(), 0).edit();
		e.remove(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid());
		e.commit();
		
		info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ADD_PUBLIC_KEY, true);
		info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF);
		info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_ON);
		info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_REMOVE_PUBLIC_KEY);
		
		//mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		getCoreService().buddyStateChanged(Arrays.asList(info));
	}

	private void processAddPublicKeyFeature(String featureId, OnlineInfo info) {
		Parcelable[] p = info.getFeatures().getParcelableArray(featureId);					
		if (p == null || p.length < 1) {
			Logger.log("No Parcelable at XMPP Add Buddy request", LoggerLevel.INFO);
			return;
		}
		
		String pgpKey = getContext().getString(R.string.pgp_key_file);
		
		String pgp = null;
		
		for (Parcelable pp : p) {
			TKV tkv = (TKV) pp;
			if (tkv.getKey().equals(pgpKey)) {
				pgp = tkv.getValue();
			} 
		}
		
		if (TextUtils.isEmpty(pgp)) {
			Logger.log("Empty PGP value in request", LoggerLevel.INFO);
		} else {
			addBuddyPGPKey(info, pgp);
		}
	}
	
	public void encryptionOn(OnlineInfo info) {
		String buddyPGPKey;
		EncryptedDataProvider edProvider = ((XMPPCryptoEntityAdapter)getEntityAdapter()).getEdProvider();
		
		if (edProvider != null && (buddyPGPKey = getContext().getSharedPreferences(getProtocolUid(), 0).getString(ResourceUtils.BUDDY_PUBLIC_KEY_PREFIX + info.getProtocolUid(), null)) != null) {
			edProvider.getKeyStorage().put(info.getProtocolUid(), buddyPGPKey);
		}
		
		info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF);
		info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_ON, true);
		
		//mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		getCoreService().buddyStateChanged(Arrays.asList(info));
	}

	public void encryptionOff(OnlineInfo info) {
		EncryptedDataProvider edProvider = ((XMPPCryptoEntityAdapter)getEntityAdapter()).getEdProvider();
		
		if (edProvider != null){
			edProvider.getKeyStorage().put(info.getProtocolUid(), null);
		}
		
		info.getFeatures().remove(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_ON);
		info.getFeatures().putBoolean(XMPPCryptoApiConstants.FEATURE_ENCRYPTION_OFF, true);
		
		//mRosterListener.getPresenceCache().put(info.getProtocolUid(), info);
		
		getCoreService().buddyStateChanged(Arrays.asList(info));
	}

	@Override
	public void configure(ProviderManager pm) throws RemoteException {
		super.configure(pm);
		
		String pgpKey = getCoreService().requestPreference(ResourceUtils.KEY_PRIVATEKEY_FILE);
		String pgpKeyPassword = getCoreService().requestPreference(ResourceUtils.KEY_PRIVATEKEY_PASSWORD);
		
		EncryptedDataProvider edProvider;
		if (pgpKey != null && pgpKeyPassword != null){
			edProvider = new EncryptedDataProvider();
			edProvider.setMyKey(pgpKey);
			edProvider.setMyKeyPw(pgpKeyPassword);
			pm.addExtensionProvider("x", "jabber:x:signed", edProvider);
			pm.addExtensionProvider("x", "jabber:x:encrypted", edProvider);		
			
			Security.addProvider(edProvider.getProvider());			
		} else {
			edProvider = null;
			pm.removeExtensionProvider("x", "jabber:x:signed");
			pm.removeExtensionProvider("x", "jabber:x:encrypted");
		}
		
		((XMPPCryptoEntityAdapter)getEntityAdapter()).setEdProvider(edProvider);
	}
}
