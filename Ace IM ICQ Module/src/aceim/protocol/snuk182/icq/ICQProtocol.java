package aceim.protocol.snuk182.icq;

import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ProtocolService;
import aceim.protocol.snuk182.icq.utils.ResourceUtils;

public class ICQProtocol extends ProtocolService<ICQService> {

	@Override
	protected ICQService createService(byte serviceId, String protocolUid) {
		return new ICQService(serviceId, protocolUid, getCallback(), getBaseContext());
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return ResourceUtils.getFeatures(getBaseContext());
	}

	@Override
	protected String getProtocolName() {
		return IcqApiConstants.PROTOCOL_NAME;
	}

	@Override
	protected ProtocolOption[] getProtocolOptions() {
		return ResourceUtils.OPTIONS;
	}

}
