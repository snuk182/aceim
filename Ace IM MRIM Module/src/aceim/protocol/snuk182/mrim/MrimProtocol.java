package aceim.protocol.snuk182.mrim;

import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ProtocolService;
import aceim.protocol.snuk182.mrim.utils.ResourceUtils;

public class MrimProtocol extends ProtocolService<MrimService> {

	@Override
	protected MrimService createService(byte serviceId, String protocolUid) {
		MrimService service = new MrimService(serviceId, protocolUid, getCallback(), getBaseContext());
		return service;
	}

	@Override
	protected String getProtocolName() {
		return ResourceUtils.PROTOCOL_NAME;
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return ResourceUtils.getFeatures(getBaseContext());
	}

	@Override
	protected ProtocolOption[] getProtocolOptions() {
		return ResourceUtils.OPTIONS;
	}

}
