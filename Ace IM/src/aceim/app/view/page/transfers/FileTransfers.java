package aceim.app.view.page.transfers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.utils.Logger;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.FileTransfer;
import aceim.app.dataentity.listeners.IHasFileTransfer;
import aceim.app.utils.DialogUtils;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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

	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final FileTransfer ft = (FileTransfer) parent.getItemAtPosition(position);
			
			if (ft == null || ft.getProgress() == null) {
				return;
			}
			
			if (TextUtils.isEmpty(ft.getProgress().getError())) {
				
				if (ft.getProgress().getTotalSizeBytes() > 0 && ft.getProgress().getSentBytes() >= ft.getProgress().getTotalSizeBytes()) {
					if (ft.getProgress().isIncoming()) {
						MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
						//String extension = filePath.substring(filePath.lastIndexOf(".")+1);
						String extension = MimeTypeMap.getFileExtensionFromUrl(ft.getProgress().getFilePath());
						String mime = mimeMap.getMimeTypeFromExtension(extension.toLowerCase(Locale.ENGLISH));
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse("file://" + ft.getProgress().getFilePath()), mime);
						try {
							getActivity().startActivity(intent);
							mAdapter.remove(ft);
						} catch (Exception e) {
							Logger.log(e);
						}
					}
				} else {
					AlertDialog.Builder newBuilder = new AlertDialog.Builder(getMainActivity());
					newBuilder.setMessage(getMainActivity().getString(R.string.are_you_sure_you_want_to_cancel, ViewUtils.getFileNameFromPath(ft.getProgress().getFilePath()))).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							try {
								getMainActivity().getCoreService().cancelFileTransfer(ft.getProgress().getServiceId(), ft.getProgress().getMessageId());
								mAdapter.remove(ft);
							} catch (RemoteException e) {
								getMainActivity().onRemoteException(e);
							}
						}

					}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}

					});
					DialogUtils.showBrandedDialog(newBuilder.create());
				}
			}
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
		mList.setOnItemClickListener(mItemClickListener);
		
		recoverFromStoredData(saved);
		
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
	public void onBuddyStateChanged(List<Buddy> buddy) {}

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
	
	private void recoverFromStoredData(Bundle bundle){
		if (bundle == null) {
			return;
		}
		
		ArrayList<FileTransfer> transfers = bundle.getParcelableArrayList(SAVE_PARAM_TRANSFERS);
		mTransfers.addAll(transfers);
	}
}
