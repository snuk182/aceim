package aceim.protocol.snuk182.xmppcrypto;

import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.ProtocolOption.ProtocolOptionType;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.protocol.snuk182.xmpp.common.XMPPApiConstants;
import aceim.protocol.snuk182.xmpp.common.XMPPCommonProtocol;
import aceim.protocol.snuk182.xmppcrypto.utils.ResourceUtils;

public class XMPPProtocol extends XMPPCommonProtocol {

	@Override
	protected XMPPService createService(byte serviceId, String protocolUid) {
		return new XMPPService(serviceId, protocolUid, getCallback(), getBaseContext());
	}

	@Override
	protected ProtocolServiceFeature[] getProtocolFeatures() {
		return ResourceUtils.getFeatures(getBaseContext());
	}

	@Override
	protected ProtocolOption[] getProtocolOptions() {
		return new ProtocolOption[]{ 
				new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_JID, null, R.string.jid, true),
				new ProtocolOption(ProtocolOptionType.PASSWORD, ResourceUtils.KEY_PASSWORD, null, R.string.password, true), 
				new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_SERVER_HOST, XMPPApiConstants.DEFAULT_HOST, R.string.server_host, true),
				new ProtocolOption(ProtocolOptionType.INTEGER, ResourceUtils.KEY_SERVER_PORT, XMPPApiConstants.DEFAULT_PORT, R.string.server_port, true), 
				new ProtocolOption(ProtocolOptionType.CHECKBOX, ResourceUtils.KEY_SECURE_CONNECTION, "true", R.string.label_secure_connection, false),
				new ProtocolOption(ProtocolOptionType.LIST, ResourceUtils.KEY_PROXY_TYPE, null, R.string.proxy_type, false, null, getBaseContext().getResources().getStringArray(R.array.xmpp_proxy_names)),
				new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_PROXY_HOST, null, R.string.proxy_host, false),
				new ProtocolOption(ProtocolOptionType.INTEGER, ResourceUtils.KEY_PROXY_PORT, null, R.string.proxy_port, false),
				new ProtocolOption(ProtocolOptionType.STRING, ResourceUtils.KEY_PROXY_USERNAME, null, R.string.proxy_username, false),
				new ProtocolOption(ProtocolOptionType.PASSWORD, ResourceUtils.KEY_PROXY_PASSWORD, null, R.string.proxy_password, false),
				new ProtocolOption(ProtocolOptionType.FILE, ResourceUtils.KEY_PRIVATEKEY_FILE, null, R.string.pgp_key_file, false, null, "*.asc"),
				new ProtocolOption(ProtocolOptionType.PASSWORD, ResourceUtils.KEY_PRIVATEKEY_PASSWORD, null, R.string.pgp_key_password, false),						
				new ProtocolOption(ProtocolOptionType.INTEGER, ResourceUtils.KEY_PING_TIMEOUT, "200", R.string.ping_timeout, false),
		};
	}
}
