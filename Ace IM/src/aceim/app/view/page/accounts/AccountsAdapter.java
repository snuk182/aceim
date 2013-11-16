package aceim.app.view.page.accounts;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.ListFeature;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.app.AceImException;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountOptionKeys;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.DialogUtils;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.androidquery.AQuery;

public class AccountsAdapter extends ArrayAdapter<Account> {

	private final Map<Account, AccountClickListener> mAccounts = new HashMap<Account, AccountClickListener>();

	public AccountsAdapter(MainActivity activity, List<Account> objects) {
		super(activity, R.layout.account_item, R.id.label_username, objects);

		fillClickListeners(objects);
	}

	private void fillClickListeners(List<Account> objects) {
		if (objects == null) {
			return;
		}
		
		MainActivity activity = (MainActivity) getContext();

		for (Account acc : objects) {
			
			AccountClickListener l = new AccountClickListener(acc, activity.getProtocolResourcesForAccount(acc));
			mAccounts.put(acc, l);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		
		AQuery aq = new AQuery(v);

		ToggleButton disableBtn = (ToggleButton) v.findViewById(R.id.btn_disable);
		ImageButton deleteBtn = (ImageButton) v.findViewById(R.id.btn_delete);
		
		final Account a = getItem(position);
		AccountClickListener l = mAccounts.get(a);
		Resources r;
		if (l.mResources != null) {
			try {
				r = l.mResources.getNativeResourcesForProtocol(getContext().getPackageManager());
				ListFeature status = (ListFeature) l.mResources.getFeature(ApiConstants.FEATURE_STATUS);
				if (status != null) {
					aq.id(R.id.image_protocol).image(r.getDrawable(status.getDrawables()[0]));
				}

				aq.id(R.id.btn_search_in_play).visibility(View.INVISIBLE);
				disableBtn.setVisibility(View.VISIBLE);			
			} catch (AceImException e) {
				Logger.log(e);
				return new FrameLayout(getContext());
			}
		} else {
			disableBtn.setVisibility(View.INVISIBLE);	
			aq.id(R.id.btn_search_in_play).visibility(View.VISIBLE).clicked(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = ViewUtils.getSearchPluginsInPlayStoreIntent(a);
					getContext().startActivity(i);
				}
			});
		}
		
		ViewUtils.fillIcon(R.id.image_icon, aq, a.getFilename(), getContext());
		
		disableBtn.setChecked(a.isEnabled());
		disableBtn.setOnCheckedChangeListener(l);
		deleteBtn.setOnClickListener(l);
		v.setOnClickListener(l);

		return v;
	}

	private class AccountClickListener implements OnCheckedChangeListener, OnClickListener {

		final Account mAccount;
		final ProtocolResources mResources;

		public AccountClickListener(Account acc, ProtocolResources resources) {
			this.mAccount = acc;
			this.mResources = resources;
		}

		@Override
		public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

			int askId = isChecked ? R.string.this_account_to_be_enabled : R.string.this_account_to_be_disabled;
			builder.setMessage(String.format(getContext().getResources().getString(askId), mAccount.getSafeName())).setCancelable(false)
					.setPositiveButton(getContext().getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mAccount.setEnabled(isChecked);
							getContext()
								.getSharedPreferences(mAccount.getAccountId(), 0)
								.edit()
								.putBoolean(AccountOptionKeys.DISABLED.name(), !mAccount.isEnabled())
								.commit();
						}
					}).setNegativeButton(getContext().getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							buttonView.setChecked(!isChecked);
						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();
		}

		@Override
		public void onClick(View v) {
			final MainActivity activity = (MainActivity) getContext();
			if (v instanceof ImageButton) {
				if (v.getId() == R.id.btn_delete) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

					builder.setMessage(String.format(getContext().getResources().getString(R.string.are_you_sure_you_want_to_remove), mAccount.getSafeName())).setCancelable(false)
							.setPositiveButton(getContext().getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									activity.accountRemoved(mAccount);
								}
							}).setNegativeButton(getContext().getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
								}
							});
					AlertDialog dialog = builder.create();
					DialogUtils.showBrandedDialog(dialog);		
				}
			} else {
				Page.addAccountEditorPage(activity.getScreen(), mAccount);
			}
		}
	}
	
	@Override
	public void add(Account acc){
		super.add(acc);
		AccountClickListener l = new AccountClickListener(acc, ((MainActivity)getContext()).getProtocolResourcesForAccount(acc));
		mAccounts.put(acc, l);
	}
	
	@Override
	public void addAll(Collection<? extends Account> collection){
		for (Account acc : collection) {
			add(acc);
			AccountClickListener l = new AccountClickListener(acc, ((MainActivity)getContext()).getProtocolResourcesForAccount(acc));
			mAccounts.put(acc, l);
		}
	}
	
	@Override
	public void addAll(Account ... items){
		for (Account acc : items) {
			add(acc);
			AccountClickListener l = new AccountClickListener(acc, ((MainActivity)getContext()).getProtocolResourcesForAccount(acc));
			mAccounts.put(acc, l);
		}
	}
	
	@Override
	public void insert(Account acc, int index) {
		super.insert(acc, index);
		AccountClickListener l = new AccountClickListener(acc, ((MainActivity)getContext()).getProtocolResourcesForAccount(acc));
		mAccounts.put(acc, l);
	}
	
	@Override
	public void remove(Account object) {
		super.remove(object);
		mAccounts.remove(object);
	}
}
