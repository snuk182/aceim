package aceim.protocol.snuk182.xmpp;

import aceim.api.service.ICoreProtocolCallback;
import aceim.protocol.snuk182.xmpp.common.XMPPCommonService;
import android.content.Context;

public class XMPPService extends XMPPCommonService {

	public XMPPService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}	
}
