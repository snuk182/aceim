package aceim.protocol.snuk182.xmpp;

import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.protocol.snuk182.xmpp.common.XMPPCommonProtocol;
import aceim.protocol.snuk182.xmpp.common.utils.ResourceUtils;

public class XMPPProtocol extends XMPPCommonProtocol {

	@Override
	protected XMPPService createService(byte serviceId, String protocolUid) {
		return new XMPPService(serviceId, protocolUid, getCallback(), getBaseContext());
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return ResourceUtils.getFeatures(getBaseContext());
	}
}
