package aceim.app.utils;

/**
 * 
 * Native LED controller is created by apangin ( http://apangin.habrahabr.ru/ )
 */
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ConnectionState;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.FileProgress;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.service.ProtocolException;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.AceImException;
import aceim.app.Constants;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.AccountService;
import aceim.app.service.CoreService;
import aceim.app.view.page.Page;
import aceim.app.view.page.chat.Chat;
import aceim.app.view.page.contactlist.ContactList;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;

public class Notificator {

	private static final String URI_PROTOCOL = "aceim://";

	private float mVolumeLevel = 1f;

	private Context mContext;
	private final NotificationManager mNotificatorManager;
	private final Vibrator mVibrator;
	private final AudioManager mAudioManager;

	private static final int APP_ICON_ID = -100500;

	private LedBlinker mLedBlinker;

	private final Map<Long, NotificationCompat.Builder> mFileTransferViews = new HashMap<Long, NotificationCompat.Builder>();

	private SoundNotificationMode mSoundMode = SoundNotificationMode.PROFILE_DEPENDENT;
	private StatusBarNotificationMode mStatusBarMode = StatusBarNotificationMode.APP_ICON;
	private LEDNotificationMode mLEDNotificationMode = LEDNotificationMode.DEFAULT_BLINK;
	private boolean messageSoundOnly = false;

	public Notificator(Context context) {
		this.mContext = context;
		this.mNotificatorManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	public void onMessage(Message message, Account account) {
		Logger.log("Notification for message" + message, LoggerLevel.VERBOSE);
		Buddy buddy = account.getBuddyByProtocolUid(message.getContactUid());

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		builder.setAutoCancel(true);
		builder.setContentTitle(buddy.getSafeName());
		builder.setSubText(account.getSafeName());
		builder.setWhen(System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(mContext, MainActivity.class);
		fillMessageIntent(notificationIntent, account, buddy, message);
		
		String text; 
		
		if (message instanceof TextMessage) {
			notificationIntent.putExtra(Constants.INTENT_EXTRA_CLASS_NAME, Chat.class.getName());
			notificationIntent.setData(ViewUtils.stringAsIntentDataUri(MainActivity.class.getName()));
			
			text = message.getText();
			
			if (buddy.getUnread() > 1) {
				builder.setNumber(buddy.getUnread());
			}

			builder.setSmallIcon(R.drawable.ic_message);
		} else if (message instanceof FileMessage || ((message instanceof ServiceMessage) && ((ServiceMessage)message).isRequireAcceptDeclineAnswer())) {
			Intent acceptIntent = new Intent(mContext, CoreService.class);
			fillMessageIntent(acceptIntent, account, buddy, message);
			acceptIntent.setData(ViewUtils.stringAsIntentDataUri(mContext.getString(android.R.string.ok)));

			Intent declineIntent = new Intent(mContext, CoreService.class);
			fillMessageIntent(declineIntent, account, buddy, message);
			declineIntent.setData(ViewUtils.stringAsIntentDataUri(mContext.getString(android.R.string.cancel)));

			PendingIntent acceptPIntent = PendingIntent.getService(mContext, 0, acceptIntent, 0);
			PendingIntent declinePIntent = PendingIntent.getService(mContext, 0, declineIntent, 0);

			builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.decline), declinePIntent);
			builder.addAction(android.R.drawable.ic_menu_save, mContext.getString(R.string.accept), acceptPIntent);

			if (message instanceof FileMessage) {
				FileMessage fm = (FileMessage) message;
				StringBuilder sb = new StringBuilder();
				sb.append(mContext.getString(R.string.buddy_sends_files, buddy.getSafeName()));
				for (FileInfo fi : fm.getFiles()) {
					sb.append("\n");
					sb.append(mContext.getString(R.string.file_transfer_request_format, fi.getFilename(), ViewUtils.humanReadableByteCount(fi.getSize(), true)));
				}

				text = sb.toString();

				builder.setSmallIcon(R.drawable.ic_file_message);
			} else {
				builder.setSmallIcon(R.drawable.ic_service_message);
				
				text = message.getText();
			}			
		} else {
			text = message.getText();
			builder.setSmallIcon(R.drawable.ic_service_message);
		}
		
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
		
		builder.setContentIntent(contentIntent);
		builder.setContentText(text);
		builder.setTicker(mContext.getString(R.string.default_key_value_format, buddy.getSafeName(), text));

		builder.setLargeIcon(ViewUtils.getIcon(mContext, buddy.getFilename()));

		switch (mLEDNotificationMode) {
		case DEFAULT_BLINK:
			builder.setLights(0xff0000ff, 700, 300);
			break;
		case NATIVE_BLINK:
			if (!checkAndRunNativeLed()) {
				builder.setLights(0xff0000ff, 700, 300);
			}
			break;
		default:
			break;
		}

		if (!(message instanceof TextMessage)) {
			mNotificatorManager.notify((int) message.getMessageId(), builder.build());
		} else {
			switch (mStatusBarMode) {
			case OFF:
				break;
			case ACCOUNTS:
				mNotificatorManager.notify(account.getAccountId().hashCode(), builder.build());
				break;
			default:
				mNotificatorManager.notify(buddy.getFilename().hashCode(), builder.build());
			}
		}
		// mNotificatorManager.cancel(buddy.getFilename().hashCode());
		// mNotificatorManager.cancel(account.getAccountId().hashCode());
	}

	private void fillMessageIntent(Intent notificationIntent, Account account, Buddy buddy, Message message) {
		notificationIntent.putExtra(Constants.INTENT_EXTRA_CLASS_NAME, message.getClass().getName());
		notificationIntent.putExtra(Constants.INTENT_EXTRA_ACCOUNT, account);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_BUDDY, buddy);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_MESSAGE, message);
	}

	public void onAccountStateChanged(List<AccountService> accountServices) {
		switch (mStatusBarMode) {
		case ACCOUNTS:
			mNotificatorManager.cancel(APP_ICON_ID);
			
			// order of application bar icons is changed in Android 2.3
			if (Build.VERSION.SDK_INT < 9) {
				for (AccountService as : accountServices) {
					accountNotification(as);				
				}
			} else {
				for (int i=accountServices.size()-1; i>=0; i--) {
					accountNotification(accountServices.get(i));
				}
			}		
			break;
		case APP_ICON:
			NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
			
			int onlines = 0;
			int offlines = 0;
			for (AccountService as : accountServices) {
				if (as != null && as.getAccount().isEnabled()) {
					if (as.getAccount().getConnectionState() == ConnectionState.CONNECTED) {
						onlines++;
					} else {
						offlines++;
					}
				}
				mNotificatorManager.cancel(as.getAccount().getAccountId().hashCode());			
			}
			
			StringBuilder contentText = new StringBuilder();
			if (onlines > 0) {
				contentText.append(onlines);
				contentText.append(" ");
				contentText.append(mContext.getString(R.string.online));
			}			
			if (onlines > 0 && offlines > 0) {
				contentText.append(", ");
			}
			if (offlines > 0) {
				contentText.append(offlines);
				contentText.append(" ");
				contentText.append(mContext.getString(R.string.offline));
			}
			
			Intent notificationIntent = new Intent(mContext, MainActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
			builder.setOngoing(true);
			builder.setSmallIcon(R.drawable.ic_logo_notification);
			builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher));
			builder.setContentTitle(mContext.getString(R.string.app_name));
			builder.setContentText(contentText.toString());
			builder.setContentIntent(contentIntent);

			mNotificatorManager.notify(APP_ICON_ID, builder.build());
			break;
		default:
			mNotificatorManager.cancel(APP_ICON_ID);
			for (AccountService as : accountServices) {
				mNotificatorManager.cancel(as.getAccount().getAccountId().hashCode());			
			}
			break;
		}
	}
	
	@SuppressLint("InlinedApi")
	private void accountNotification(AccountService accountService) {
		if (accountService == null || !accountService.getAccount().isEnabled()) return;
			
		Account account = accountService.getAccount();

		Logger.log("Notification for account " + account, LoggerLevel.VERBOSE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

		int unreads = account.getUnreadMessages();
		
		int targetWidth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB 
				? mContext.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width) : 
					0;
		
		builder.setAutoCancel(false);
		builder.setOngoing(true);
		builder.setContentTitle(account.getSafeName());
		builder.setContentText(unreads > 0 ? mContext.getString(R.string.unread_messages) : ViewUtils.getAccountStatusName(mContext, account, accountService.getProtocolService().getResources(false)));
		builder.setLargeIcon(ViewUtils.getIcon(mContext, account.getFilename(), targetWidth, 0));

		builder.setSmallIcon(unreads > 0 ? R.drawable.ic_message : ViewUtils.getAccountStatusIcon(mContext, account));
		builder.setNumber(unreads);

		Intent notificationIntent = new Intent(mContext, MainActivity.class);
		String notificatorId = Page.getPageIdForEntityWithId(ContactList.class, account);
		notificationIntent.setData((Uri.parse(URI_PROTOCOL + notificatorId)));
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, account.getServiceId(), notificationIntent, 0);
		builder.setContentIntent(contentIntent);

		mNotificatorManager.notify(account.getAccountId().hashCode(), builder.build());
	}

	public void onFileTransferProgress(FileProgress progress) {
		Logger.log("Notification for file transfer " + progress.getFilePath() + ", id #" + progress.getMessageId() + " size " + ViewUtils.humanReadableByteCount(progress.getSentBytes(), true) + "/" + ViewUtils.humanReadableByteCount(progress.getTotalSizeBytes(), true), LoggerLevel.VERBOSE);

		NotificationCompat.Builder builder = mFileTransferViews.get(progress.getMessageId());

		if (builder == null) {
			//mNotificatorManager.cancel((int) progress.getMessageId());

			builder = new NotificationCompat.Builder(mContext);
			String path = progress.getFilePath();
			builder.setContentTitle(path.contains(File.separator) ? path.substring(path.lastIndexOf(File.separator) + 1) : path);
			builder.setSmallIcon(R.drawable.ic_file_message);
			
			mFileTransferViews.put(progress.getMessageId(), builder);
		}

		builder.setWhen(System.currentTimeMillis());
		
		if (TextUtils.isEmpty(progress.getError())) {
			if (progress.getSentBytes() >= progress.getTotalSizeBytes()) {
				PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, ViewUtils.getOpenFileInCorrespondingApplicationIntent(progress.getFilePath()), 0);
				builder.setContentIntent(contentIntent);
				builder.setAutoCancel(true);
				builder.setProgress(0, 0, false);
				builder.setContentText(mContext.getString(R.string.file_transfer_completed));

				mFileTransferViews.remove(progress.getMessageId());
			} else {
				builder.setAutoCancel(false);
				
				Intent intent = new Intent(mContext, MainActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
				builder.setContentIntent(contentIntent);
				
				if (progress.getTotalSizeBytes() > 0) {
					int percentDone = (int) ((progress.getSentBytes() * 100f) / progress.getTotalSizeBytes());
					builder.setProgress(100, percentDone, false);
					builder.setContentText(mContext.getString(R.string.file_transfer_bytes_completed, Long.toString(progress.getSentBytes()), Long.toString(progress.getTotalSizeBytes())));
				} else {
					builder.setProgress(100, 0, true);
					builder.setContentText(mContext.getString(R.string.file_transfer_init));
				}
			}
		} else {
			Intent intent = new Intent(mContext, MainActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
			builder.setContentIntent(contentIntent);
			
			builder.setContentText(progress.getError());

			builder.setAutoCancel(true);
			builder.setProgress(100, 100, false);

			mFileTransferViews.remove(progress.getMessageId());
		}

		mNotificatorManager.notify((int) progress.getMessageId(), builder.build());
	}

	public void removeAccountIcon(Account account) {
		Logger.log("Remove notification for account " + account, LoggerLevel.VERBOSE);
		mNotificatorManager.cancel(account.getAccountId().hashCode());
	}

	public void removeMessageNotification(Buddy buddy, List<AccountService> services) {
		Logger.log("Remove message notification for " + buddy, LoggerLevel.VERBOSE);
		switch (mStatusBarMode) {
		case ACCOUNTS:
			onAccountStateChanged(services);
			break;
		default:
			mNotificatorManager.cancel(buddy.getFilename().hashCode());
			break;
		}

		if (mLedBlinker != null) {
			mLedBlinker.stopBlinking();
		}
	}

	public void removeFileNotification(long messageId) {
		Logger.log("Remove file transfer notification for id #" + messageId, LoggerLevel.VERBOSE);
		Builder n = mFileTransferViews.get(messageId);

		if (n != null) {
			mNotificatorManager.cancel(n.hashCode());
			mFileTransferViews.remove(n);
		}
	}

	public void removeAppIcon() {
		Logger.log("Remove app icon notification", LoggerLevel.VERBOSE);
		mNotificatorManager.cancel(APP_ICON_ID);
	}

	private void play(final int res) {
		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				MediaPlayer mp = MediaPlayer.create(mContext, res);
				mp.setVolume(mVolumeLevel, mVolumeLevel);
				mp.start();
			}
		}).start();
	}

	public void messageSound() {
		switch (mSoundMode) {
		case OFF:
			break;
		case PROFILE_DEPENDENT:
			playMessageBasedOnProfile();
			break;
		default:
			playMessage(mSoundMode == SoundNotificationMode.SOUND || mSoundMode == SoundNotificationMode.SOUND_VIBRA, mSoundMode == SoundNotificationMode.VIBRA || mSoundMode == SoundNotificationMode.SOUND_VIBRA);
			break;
		}
	}

	private void playMessage(boolean playSound, boolean vibrate) {
		if (mAudioManager != null && playSound) {
			play(R.raw.message);
		}
		if (mVibrator != null && vibrate) {
			mVibrator.vibrate(800);
		}
	}

	private void playMessageBasedOnProfile() {
		int ringerMode = mAudioManager.getRingerMode();
		switch (ringerMode) {
		case AudioManager.RINGER_MODE_NORMAL:
			playMessage(true, true);
			break;
		case AudioManager.RINGER_MODE_VIBRATE:
			playMessage(false, true);
			break;
		case AudioManager.RINGER_MODE_SILENT:
			playMessage(false, false);
			break;
		}
	}

	public void processException(Exception ex, Account account) {
		Logger.log("Notification for exception " + ex, LoggerLevel.VERBOSE);
		Builder builder = new Builder(mContext);
		Notification n = null;
		if (ex instanceof AceImException) {
			switch (((AceImException) ex).reason) {
			case NO_PROTOCOL_FOUND:
				Intent i = ViewUtils.getSearchPluginsInPlayStoreIntent(account);
				PendingIntent pi = PendingIntent.getActivity(mContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

				n = builder.setAutoCancel(true).setSmallIcon(android.R.drawable.ic_dialog_alert).setWhen(System.currentTimeMillis()).setDefaults(Notification.DEFAULT_SOUND).setContentIntent(pi).build();
				break;
			default:
				break;
			}
		}
		if (ex instanceof ProtocolException) {
			switch (((ProtocolException) ex).cause) {
			case BROKEN_AUTH_DATA:
				break;
			case CONNECTION_ERROR:
				break;
			case DEFAULT:
				break;
			case GROUPCHAT_ALREADY_EXISTS:
				break;
			case NONE:
				break;
			case NO_GROUPCHAT_AVAILABLE:
				break;
			default:
				break;

			}
		}
		if (n != null) {
			mNotificatorManager.notify(n.hashCode(), n);
		}
	}

	public void setMessageSoundOnly(boolean messageSoundOnly) {
		this.messageSoundOnly = messageSoundOnly;
	}

	/**
	 * @return the messageSoundOnly
	 */
	public boolean isMessageSoundOnly() {
		return messageSoundOnly;
	}

	/**
	 * @return the mSoundMode
	 */
	public SoundNotificationMode getSoundMode() {
		return mSoundMode;
	}

	/**
	 * @param soundMode
	 *            the mSoundMode to set
	 */
	public void setSoundMode(SoundNotificationMode soundMode) {
		if (soundMode == null) {
			return;
		}
		this.mSoundMode = soundMode;
	}
	
	/**
	 * @return the mVolumeLevel
	 */
	public float getVolumeLevel() {
		return mVolumeLevel;
	}

	/**
	 * @param mVolumeLevel the mVolumeLevel to set
	 */
	public void setVolumeLevel(float mVolumeLevel) {
		this.mVolumeLevel = mVolumeLevel;
	}

	/**
	 * @return the mStatusBarMode
	 */
	public StatusBarNotificationMode getStatusBarMode() {
		return mStatusBarMode;
	}

	/**
	 * @param statusBarMode
	 *            the mStatusBarMode to set
	 */
	public void setStatusBarMode(StatusBarNotificationMode statusBarMode) {
		if (statusBarMode == null) {
			return;
		}
		this.mStatusBarMode = statusBarMode;
	}

	public LEDNotificationMode getLEDNotificationMode() {
		return mLEDNotificationMode;
	}

	public void setLEDNotificationMode(LEDNotificationMode ledNotificationMode) {
		if (ledNotificationMode == null) {
			return;
		}
		this.mLEDNotificationMode = ledNotificationMode;
	}

	private boolean checkAndRunNativeLed() {
		mLedBlinker = LedBlinker.getLedBlinker();

		return mLedBlinker != null;
	}

	public static enum SoundNotificationMode {
		OFF, SOUND, VIBRA, SOUND_VIBRA, PROFILE_DEPENDENT;
	}

	public static enum StatusBarNotificationMode {
		OFF, APP_ICON, MESSAGES, ACCOUNTS;
	}

	public static enum LEDNotificationMode {
		OFF, DEFAULT_BLINK, NATIVE_BLINK
	}

	private static class LedBlinker extends Thread {
		private final String red;
		private final String green;
		private final String blue;

		// volatile boolean stopped = false;

		LedBlinker(boolean isSE) {
			if (isSE) {
				red = "ledc:rgb1:red";
				green = "ledc:rgb1:green";
				blue = "ledc:rgb1:blue";
			} else {
				red = "amber";
				green = "green";
				blue = "blue";
			}
		}

		public static LedBlinker getLedBlinker() {
			Logger.log("Init LED blinker... ", LoggerLevel.VERBOSE);
			boolean isSE = true;

			// sony ericsson case
			try {
				nativeLedControl("ledc:rgb1:green", 0);
				Logger.log("...LED blinker for SE inited", LoggerLevel.VERBOSE);
			} catch (Exception e) {
				isSE = false;
				try {
					nativeLedControl("green", 0);
					Logger.log("...LED blinker inited", LoggerLevel.VERBOSE);
				} catch (Exception e1) {

					Logger.log("... no support available", LoggerLevel.VERBOSE);
					return null;
				}
			}

			LedBlinker blinker = new LedBlinker(isSE);

			return blinker;
		}

		public void stopBlinking() {
			Logger.log("Stop LED blinker", LoggerLevel.VERBOSE);
			// stopped = true;
			interrupt();
		}

		/**
		 * /sys/class/leds/ledc:rgb1:red/brightness - red
		 * /sys/class/leds/ledc:rgb1:green/brightness - green
		 * /sys/class/leds/ledc:rgb1:blue/brightness - blue
		 */
		private static void nativeLedControl(String name, int brightness) throws Exception {
			FileWriter fw = new FileWriter("/sys/class/leds/" + name + "/brightness");
			fw.write(Integer.toString(brightness));
			fw.close();
		}

		public void run() {
			for (int ledState = 0;; ledState = (ledState + 1) % 6) {
				switch (ledState) {
				case 0:
					try {
						nativeLedControl(red, 255);
					} catch (Exception e1) {
					}
					break;
				case 1:
					try {
						nativeLedControl(blue, 255);
					} catch (Exception e1) {
					}
					break;
				case 2:
					try {
						nativeLedControl(red, 0);
					} catch (Exception e1) {
					}
					break;
				case 3:
					try {
						nativeLedControl(green, 255);
					} catch (Exception e1) {
					}
					break;
				case 4:
					try {
						nativeLedControl(blue, 0);
					} catch (Exception e1) {
					}
					break;
				case 5:
					try {
						nativeLedControl(green, 0);
					} catch (Exception e1) {
					}
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
			try {
				nativeLedControl(red, 0);
			} catch (Exception e) {
			}
			try {
				nativeLedControl(green, 0);
			} catch (Exception e) {
			}
			try {
				nativeLedControl(blue, 0);
			} catch (Exception e) {
			}
		}
	}
}
