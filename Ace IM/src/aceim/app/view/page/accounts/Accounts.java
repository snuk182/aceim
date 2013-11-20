package aceim.app.view.page.accounts;

import java.util.List;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.listeners.IHasAccountList;
import aceim.app.utils.ViewUtils;
import aceim.app.utils.linq.KindaLinq;
import aceim.app.utils.linq.KindaLinqRule;
import aceim.app.view.page.Page;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.androidquery.AQuery;

public class Accounts extends Page implements IHasAccountList {
	
	private final List<Account> mAccounts;	
	private AccountsAdapter mAdapter;
	
	public Accounts(List<Account> accounts) {
		this.mAccounts = accounts;
		this.setHasOptionsMenu(true);
	}
	
	private final OnClickListener mGetAccountEditorClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Page.addAccountEditorPage(((MainActivity)getMainActivity()).getScreen(), null);
		}
	};
	
	private final OnClickListener mSearchInPlayClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent i = ViewUtils.getSearchPluginsInPlayStoreIntent(null);
			getMainActivity().startActivity(i);
		}
	};
	
	private final OnClickListener mUtilsListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Page.getUtilsPage(getMainActivity().getScreen());
		}
	};
	
	@Override
	public View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.accounts, group, false);
		
		mAdapter = new AccountsAdapter((MainActivity) getMainActivity(), mAccounts);
		
		AQuery aq = new AQuery(view);
		
		aq.id(R.id.list).adapter(mAdapter);
		aq.id(R.id.add).clicked(mGetAccountEditorClickListener);
		aq.id(R.id.search_play_store).clicked(mSearchInPlayClickListener);
		aq.id(R.id.utils).clicked(mUtilsListener);
		
		return view;
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(R.drawable.ic_menu_allfriends);
	}

	@Override
	public String getTitle(Context context) {
		return context.getResources().getString(R.string.accounts);
	}

	@Override
	public void onAccountAdded(Account account) {
		mAdapter.add(account);
	}

	@Override
	public void onAccountModified(final Account account) {
		Account acc = KindaLinq.from(mAccounts).where(new KindaLinqRule<Account>() {
			
			@Override
			public boolean match(Account t) {
				return t.getServiceId() == account.getServiceId();
			}
		}).first();
		
		acc.merge(account);
		mAdapter.notifyDataSetChanged();	
	}

	@Override
	public void onAccountRemoved(Account account) {
		mAdapter.remove(account);
	}
}
