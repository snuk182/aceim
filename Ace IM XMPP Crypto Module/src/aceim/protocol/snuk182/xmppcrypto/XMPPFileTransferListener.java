package aceim.protocol.snuk182.xmppcrypto;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.FileProgress;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;

public class XMPPFileTransferListener extends XMPPListener implements FileTransferListener {

	private FileTransferManager mFileTransferManager;
	
	private final Map<Long, FileTransferRequest> fileTransfers = Collections.synchronizedMap(new HashMap<Long, FileTransferRequest>());

	public XMPPFileTransferListener(XMPPServiceInternal service) {
		super(service);
	}

	@Override
	public void fileTransferRequest(FileTransferRequest request) {
		Logger.log("incoming file " + request.getFileName() + " from " + request.getRequestor(), LoggerLevel.VERBOSE);
		fileTransfers.put(Long.valueOf(request.getStreamID().hashCode()) , request);
		FileMessage fm = new FileMessage(getInternalService().getService().getServiceId(), XMPPEntityAdapter.normalizeJID(request.getRequestor()));

		FileInfo fi = new FileInfo(getInternalService().getService().getServiceId());
		fi.setFilename(request.getFileName());
		fi.setSize(request.getFileSize());
		fm.getFiles().add(fi);
		fm.setMessageId(request.getStreamID().hashCode());

		getInternalService().getService().getCoreService().message(fm);
	}

	void fileRespond(final FileMessage fileMessage, final Boolean accept) {
		FileTransferRequest request = fileTransfers.remove(fileMessage.getMessageId());
		
		if (request != null) {
			if (accept) {
				Runnable r = new IncomingFileRunnable(fileMessage.getContactUid(), request);
				Executors.defaultThreadFactory().newThread(r).start();
			} else {
				request.reject();
			}
		} else {
			Logger.log("No file message to respond " + fileMessage.getMessageId(), LoggerLevel.INFO);
		}
	}

	long sendFile(final String receiverJid, final List<File> files) {
		
		final OutgoingFileTransfer transfer = mFileTransferManager.createOutgoingFileTransfer(receiverJid);

		Runnable r = new OutgoingFileRunnable(files, transfer, receiverJid);
		Executors.defaultThreadFactory().newThread(r).start();
		
		return transfer.getStreamID().hashCode();
	}

	/**
	 * @return the mFileTransferManager
	 */
	public FileTransferManager getFileTransferManager() {
		return mFileTransferManager;
	}

	/**
	 * @param mFileTransferManager the mFileTransferManager to set
	 */
	public void setFileTransferManager(FileTransferManager mFileTransferManager) {
		this.mFileTransferManager = mFileTransferManager;
	}
	
	private class OutgoingFileRunnable implements Runnable {
		
		private final List<File> files;
		private final OutgoingFileTransfer transfer;
		private final String receiverJid;

		private OutgoingFileRunnable(List<File> files, OutgoingFileTransfer transfer, String receiverJid) {
			this.files = files;
			this.transfer = transfer;
			this.receiverJid = receiverJid;
		}

		@Override
		public void run() {
			for (File file : files) {
				try {
					transfer.sendFile(file, file.getName());
				} catch (XMPPException e) {
					Logger.log(e);
					return;
				}
				
				try {
					while (!transfer.isDone()) {
						String error;
						if (transfer.getStatus() == Status.error) {
							error = transfer.getError().getMessage();
						} else {
							error = null;
						}
						FileProgress fp = new FileProgress(getInternalService().getService().getServiceId(), transfer.getStreamID().hashCode(), file.getAbsolutePath(), transfer.getFileSize(), transfer.getAmountWritten(), false, receiverJid, error);
						getInternalService().getService().getCoreService().fileProgress(fp);
						Thread.sleep(1000);
					}
					
					FileProgress fp = new FileProgress(getInternalService().getService().getServiceId(), transfer.getStreamID().hashCode(), file.getAbsolutePath(), transfer.getFileSize(), transfer.getFileSize(), false, receiverJid, transfer.getError() != null ? transfer.getError().getMessage() : null);
					getInternalService().getService().getCoreService().fileProgress(fp);
				} catch (InterruptedException e) {
					Logger.log(e);
				}				
			}
		}
	}
	
	private class IncomingFileRunnable implements Runnable {
		
		private final String senderJid;
		private final FileTransferRequest request;
		
		private IncomingFileRunnable(String senderJid, FileTransferRequest request) {
			this.senderJid = senderJid;
			this.request = request;
		}

		@Override
		public void run() {
			Buddy buddy = XMPPEntityAdapter.rosterEntry2Buddy(getInternalService().getConnection().getRoster().getEntry(senderJid), getInternalService().getService().getProtocolUid(), getInternalService().getEdProvider(), getInternalService().getService().getContext(), getInternalService().getService().getServiceId());
			File file = Utils.createLocalFileForReceiving(request.getFileName(), buddy, request.getFileSize());
			IncomingFileTransfer transfer = request.accept();
			
			try {
				transfer.recieveFile(file);
			} catch (XMPPException e1) {
				Logger.log(e1);
			}
			
			
			try {
				while (!transfer.isDone()) {
					String error;
					if (transfer.getStatus() == Status.error) {
						error = transfer.getError().getMessage();
					} else {
						error = null;
					}
					FileProgress fp = new FileProgress(getInternalService().getService().getServiceId(), transfer.getStreamID().hashCode(), request.getFileName(), transfer.getFileSize(), transfer.getAmountWritten(), true, senderJid, error);
					getInternalService().getService().getCoreService().fileProgress(fp);
					Thread.sleep(1000);
				}
				
				FileProgress fp = new FileProgress(getInternalService().getService().getServiceId(), transfer.getStreamID().hashCode(), request.getFileName(), transfer.getFileSize(), transfer.getAmountWritten(), true, senderJid, transfer.getError() != null ? transfer.getError().getMessage() : null);
				getInternalService().getService().getCoreService().fileProgress(fp);
			} catch (InterruptedException e) {
				Logger.log(e);
			}
		}
	}

	public void cancel(long messageId) {
		FileTransferRequest request = fileTransfers.get(messageId);
		
		if (request != null) {
			request.reject();
			fileTransfers.remove(messageId);
		}
	}

	@Override
	void onDisconnect() {
		fileTransfers.clear();
	}
}
