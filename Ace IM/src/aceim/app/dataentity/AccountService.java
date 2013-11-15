package aceim.app.dataentity;

public class AccountService {

	private final Account account;
	private final ProtocolService protocolService;
	
	public AccountService(Account account, ProtocolService protocolService) {
		this.account = account;
		this.protocolService = protocolService;
	}

	/**
	 * @return the account
	 */
	public Account getAccount() {
		return account;
	}

	/**
	 * @return the protocolService
	 */
	public ProtocolService getProtocolService() {
		return protocolService;
	}
}
