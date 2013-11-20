package aceim.app.view.page.transfers;

import java.util.ArrayList;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.OnlineInfo;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.FileTransfer;
import aceim.app.dataentity.listeners.IHasFileTransfer;
import aceim.app.utils.DialogUtils;
import aceim.app.view.page.Page;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

public class FileTransfers extends Page implements IHasFileTransfer {
	
	private static final String SAVE_PARAM_TRANSFERS = "FileTransfers";
	
	private final Account mAccount;
	private final ArrayList<FileTransfer> mTransfers = new ArrayList<FileTransfer>();
	private FileTransfersAdapter mAdapter;
	
	private ListView mList;
	
	@SuppressLint("DefaultLocale")
	private final OnClickListener mCancelAllClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			AlertDialog.Builder newBuilder = new AlertDialog.Builder(getMainActivity());
			newBuilder.setMessage(getMainActivity().getString(R.string.are_you_sure_you_want_to_cancel, getMainActivity().getString(R.string.all_transfers).toLowerCase())).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (FileTransfer t : mTransfers) {
						try {
							getMainActivity().getCoreService().cancelFileTransfer(t.getProgress().getServiceId(), t.getMessageId());
						} catch (RemoteException e) {
							getMainActivity().onRemoteException(e);
						}
					}
					
					mAdapter.clear();
				}

			}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}

			});
			DialogUtils.showBrandedDialog(newBuilder.create());
		}
	};

	public FileTransfers(Account account) {
		this.mAccount = account;
	}
	
	@Override
	public View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.file_transfers, null);
		
		view.findViewById(R.id.cancel_all).setOnClickListener(mCancelAllClickListener);
		view.findViewById(R.id.close).setOnClickListener(mRemoveMeClickListener);
		
		mList = (ListView) view.findViewById(R.id.list);
		
		mAdapter = new FileTransfersAdapter(group.getContext(), mTransfers);
		mList.setAdapter(mAdapter);
		
		return view;
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_save);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.file_transfers, mAccount.getSafeName());
	}

	@Override
	public void onBuddyStateChanged(Buddy buddy) {}

	@Override
	public Buddy getBuddy() {
		return null;
	}

	@Override
	public boolean hasThisBuddy(byte serviceId, String protocolUid) {
		for (FileTransfer t : mTransfers) {
			Buddy b = t.getParticipant();
			if (b.getServiceId() == serviceId && b.getProtocolUid().equals(protocolUid)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void onBuddyIcon(byte serviceId, String protocolUid) {
		if (mAccount.getServiceId() != serviceId) {
			return;
		}
		
		if (mAdapter != null) {
			for (FileTransfer t : mTransfers) {
				if (t.getParticipant().getProtocolUid().equals(protocolUid)) {
					mAdapter.populate(mList.findViewWithTag(t), t);
				}
			}
		}
	}

	@Override
	public Account getAccount() {
		return mAccount;
	}

	@Override
	public void onOnlineInfoChanged(OnlineInfo info) {}

	@Override
	public Buddy getBuddyWithParameters(byte serviceId, String protocolUid) {
		for (FileTransfer t : mTransfers) {
			Buddy b = t.getParticipant();
			if (b.getServiceId() == serviceId && b.getProtocolUid().equals(protocolUid)) {
				return b;
			}
		}
		
		return null;
	}

	@Override
	public void onConnectionStateChanged(ConnectionState connState, int extraParameter) {}

	@Override
	public void onContactListUpdated(Account account) {}

	@Override
	public void onAccountIcon(byte serviceId) {}

	@Override
	public void onFileProgress(FileProgress progress) {
		if (progress.getServiceId() != mAccount.getServiceId()) {
			return;
		}
		
		for (FileTransfer t : mTransfers) {
			if (t.getMessageId() == progress.getMessageId()) {
				t.setProgress(progress);
				if (mAdapter != null) {
					mAdapter.populate(mList.findViewWithTag(t), t);
				}
				return;
			}
		}
		
		FileTransfer newTransfer = new FileTransfer(progress.getMessageId(), mAccount.getBuddyByProtocolUid(progress.getOwnerUid()));
		newTransfer.setProgress(progress);
		
		if (mAdapter != null) {
			mAdapter.add(newTransfer);
		} else {
			mTransfers.add(newTransfer);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle saver){
		saver.putParcelableArrayList(SAVE_PARAM_TRANSFERS, mTransfers);
	}
	
	@Override
	public Bundle getPageDataForStorage(){
		Bundle saver = new Bundle();
		
		onSaveInstanceState(saver);
		
		return saver;
	}
	
	@Override
	public void recoverFromStoredData(Bundle bundle){
		if (bundle == null) {
			return;
		}
		
		ArrayList<FileTransfer> transfers = bundle.getParcelableArrayList(SAVE_PARAM_TRANSFERS);
		mTransfers.addAll(transfers);
	}
}
