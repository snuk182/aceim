package aceim.protocol.snuk182.xmppcrypto;

abstract class XMPPListener {
	
	private final XMPPService mService;

	XMPPListener(XMPPService service) {
		this.mService = service;
	}

	/**
	 * @return the mService
	 */
	public XMPPService getService() {
		return mService;
	}

	abstract void onDisconnect();
}
