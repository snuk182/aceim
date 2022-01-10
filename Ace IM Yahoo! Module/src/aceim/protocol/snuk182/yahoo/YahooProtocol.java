package aceim.protocol.snuk182.yahoo;

import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ProtocolService;

public class YahooProtocol extends ProtocolService<YahooService> {

	public YahooProtocol() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected YahooService createService(byte arg0, String arg1) {
		return new YahooService(arg0, arg1, getCallback(), getBaseContext());
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getProtocolName() {
		return YahooProtocolConstants.PROTOCOL_NAME;
	}

	@Override
	protected ProtocolOption[] getProtocolOptions() {
		// TODO Auto-generated method stub
		return null;
	}

}
