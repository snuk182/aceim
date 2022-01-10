package aceim.protocol.snuk182.yahoo;

import aceim.api.IProtocol;
import aceim.api.dataentity.ConnectionState;
import aceim.api.service.AccountService;
import aceim.api.service.ICoreProtocolCallback;
import android.content.Context;

public class YahooService extends AccountService {

	public YahooService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}

	@Override
	protected ConnectionState getCurrentState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProtocol getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void keepaliveRequest() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void timeoutReconnect() {
		// TODO Auto-generated method stub

	}

}
