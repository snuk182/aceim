package aceim.app.dataentity.listeners;

import aceim.app.dataentity.Account;


public interface IHasAccountList extends IHasEntity {
	public void onAccountAdded(Account account);
	public void onAccountModified(Account account);
	public void onAccountRemoved(Account account);
}
