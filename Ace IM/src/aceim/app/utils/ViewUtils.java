package aceim.app.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.ListFeature;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.ProtocolServiceFeature;
import aceim.api.service.ApiConstants;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.AceImException;
import aceim.app.Constants;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.preference.OptionsActivity;
import aceim.app.view.page.Page;
import aceim.app.view.page.chat.Chat;
import aceim.app.view.page.chat.ChatMessageHolder;
import aceim.app.view.page.history.History;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.BitmapAjaxCallback;

public final class ViewUtils {

	private ViewUtils() {}

	@SuppressWarnings("unchecked")
	private static final Class<? extends Page>[] ALLOWED_PAGES_FOR_STORING = new Class[] { Chat.class, History.class };

	static final String BUDDYICON_FILEEXT = ".ico";

	@SuppressLint("DefaultLocale")
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Checks if the device is a tablet or a phone - another version
	 * 
	 * @param activityContext
	 *            The Activity Context.
	 * @return Returns true if the device is a Tablet
	 */
	public static boolean isTablet(Context context) {
		Logger.log("isTablet checking", LoggerLevel.VERBOSE);
		if (Build.VERSION.SDK_INT < 11) {
			return false;
		} else if (Build.VERSION.SDK_INT < 14) {
			return true;
		} else {
			try {
				// Compute screen size
				DisplayMetrics dm = context.getResources().getDisplayMetrics();
				float screenWidth = dm.widthPixels / dm.densityDpi;
				float screenHeight = dm.heightPixels / dm.densityDpi;
				double size = Math.sqrt(Math.pow(screenWidth, 2) + Math.pow(screenHeight, 2));
				// Tablet devices should have a screen size greater than 6
				// inches
				return size > 6;
			} catch (Throwable t) {
				Logger.log(t);
				return false;
			}
		}
	}

	public static void showAlertToast(Context context, int iconId, int textId, String params) {
		Logger.log("Show alert toast", LoggerLevel.VERBOSE);
		View v = LayoutInflater.from(context).inflate(R.layout.alert_toast, null);
		
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		TextView text = (TextView) v.findViewById(R.id.text);
		
		icon.setImageResource(iconId);

		if (params != null) {
			String contentText = context.getString(textId, params);
			text.setText(contentText);
		} else {
			text.setText(textId);
		}

		Toast t = createToast(context, v);
		t.setDuration(Toast.LENGTH_LONG);
		t.show();
	}

	public static void showInformationToast(Context context, Object icon, int textId, String params) {
		Logger.log("Show info toast", LoggerLevel.VERBOSE);
		View v = LayoutInflater.from(context).inflate(R.layout.info_toast, null);
		ImageView iconView = (ImageView) v.findViewById(R.id.icon);
		TextView text = (TextView) v.findViewById(R.id.text);

		if (icon == null) {
			//iconView.getLayoutParams().width = 0;
		} else if (icon instanceof Integer) {
			iconView.setImageResource((Integer) icon);
		} else if (icon instanceof Drawable) {
			iconView.setImageDrawable((Drawable) icon);
		} else if (icon instanceof Bitmap) {
			iconView.setImageBitmap((Bitmap) icon);
		} 

		if (params != null) {
			String contentText = context.getString(textId, params);
			text.setText(contentText);
		} else {
			text.setText(textId);
		}

		Toast t = createToast(context, v);
		t.setDuration(Toast.LENGTH_LONG);

		int offset = context.getResources().getDimensionPixelSize(R.dimen.default_padding);

		t.setGravity(Gravity.LEFT | Gravity.TOP, offset, offset);
		t.show();
	}

	public static Toast createToast(Context context, View view) {
		Logger.log("Show toast", LoggerLevel.VERBOSE);
		Toast toast = new Toast(context);

		// RelativeLayout container = (RelativeLayout)
		// LayoutInflater.from(context).inflate(R.layout.toast_base, null);

		// container.addView(view, 0, new
		// RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
		// RelativeLayout.LayoutParams.WRAP_CONTENT));
		// container.setPadding(0, 0, 0, 0);
		toast.setView(view);
		return toast;
	}

	public static Drawable getAccountStatusIcon(Context context, Account account, ProtocolResources protocolResources) {
		try {
			Resources nRes = protocolResources.getNativeResourcesForProtocol(context.getPackageManager());
			int[] stIcons = ((ListFeature) protocolResources.getFeature(ApiConstants.FEATURE_STATUS)).getDrawables();

			int ic;
			switch (account.getConnectionState()) {
			case CONNECTED:
				ic = stIcons[account.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS)];
				break;
			case CONNECTING:
			case DISCONNECTING:
				ic = stIcons[stIcons.length - 1];
				break;
			default:
				ic = stIcons[stIcons.length - 2];
				break;
			}

			return nRes.getDrawable(ic);
		} catch (Exception e) {
			Logger.log(e);
			return null;
		}
	}

	public static String getAccountStatusName(Context context, Account account, ProtocolResources protocolResources) {
		try {
			Resources nRes = protocolResources.getNativeResourcesForProtocol(context.getPackageManager());
			int[] stNames = ((ListFeature) protocolResources.getFeature(ApiConstants.FEATURE_STATUS)).getNames();

			int ic;
			switch (account.getConnectionState()) {
			case CONNECTED:
				return nRes.getString(stNames[account.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS)]);
			case CONNECTING:
				ic = R.string.connecting;
				break;
			default:
				ic = R.string.disconnected;
				break;
			}

			return context.getString(ic);
		} catch (Exception e) {
			Logger.log(e);
			return null;
		}
	}

	public static int getAccountStatusIcon(Context mContext, Account account) {
		int ic;

		switch (account.getConnectionState()) {
		case CONNECTED:
			ic = android.R.drawable.presence_online;
			break;
		case CONNECTING:
		case DISCONNECTING:
			ic = android.R.drawable.presence_away;
			break;
		default:
			ic = android.R.drawable.presence_offline;
			break;
		}

		return ic;
	}

	public static Drawable getBuddyStatusIcon(Context context, Buddy buddy, ProtocolResources protocolResources) {
		try {
			Resources nRes = protocolResources.getNativeResourcesForProtocol(context.getPackageManager());
			int[] stIcons = ((ListFeature) protocolResources.getFeature(ApiConstants.FEATURE_STATUS)).getDrawables();

			byte status = buddy.getOnlineInfo().getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1);

			if (status > -1) {
				return nRes.getDrawable(stIcons[status]);
			} else {
				return nRes.getDrawable(stIcons[stIcons.length - 2]);
			}
		} catch (Exception e) {
			Logger.log(e);
			return null;
		}
	}

	public static Intent getSearchPluginsInPlayStoreIntent(Account account) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		if (account == null) {
			i.setData(Uri.parse("market://search?q=" + MainActivity.class.getPackage().getName()));
		} else {
			i.setData(Uri.parse("market://search?q=" + account.getProtocolServicePackageName()));
		}
		return i;
	}

	public static Intent getOpenFileInCorrespondingApplicationIntent(String filePath) {
		MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
		String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
		String mime = mimeMap.getMimeTypeFromExtension(extension);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + filePath), mime);
		return intent;
	}

	public static String getFormattedXStatus(OnlineInfo info, Context context, ProtocolResources resources) {
		Resources res;
		try {
			res = resources.getNativeResourcesForProtocol(context.getPackageManager());
		} catch (AceImException e) {
			Logger.log(e);
			return "";
		}

		byte value = -1;
		if (info.getFeatures().getByte(ApiConstants.FEATURE_STATUS, (byte) -1) < 0) {
			return context.getString(R.string.offline);
		} else if (!TextUtils.isEmpty(info.getXstatusName()) || !TextUtils.isEmpty(info.getXstatusDescription())) {
			if (!TextUtils.isEmpty(info.getXstatusName()) && !TextUtils.isEmpty(info.getXstatusDescription())) {
				return context.getResources().getString(R.string.xstatus_text_format, info.getXstatusName(), info.getXstatusDescription());
			} else {
				return TextUtils.isEmpty(info.getXstatusDescription()) ? info.getXstatusName() : info.getXstatusDescription();
			}
		} else if ((value = info.getFeatures().getByte(ApiConstants.FEATURE_XSTATUS, (byte) -1)) > -1) {
			return res.getString(((ListFeature) resources.getFeature(ApiConstants.FEATURE_XSTATUS)).getNames()[value]);
		} else {
			value = info.getFeatures().getByte(ApiConstants.FEATURE_STATUS);
			return res.getString(((ListFeature) resources.getFeature(ApiConstants.FEATURE_STATUS)).getNames()[value]);
		}
	}

	public static void storeImageFile(Context context, byte[] bytes, String filename, Runnable runOnFinish) {
		FileAsyncSaver saver = new FileAsyncSaver(context, filename + BUDDYICON_FILEEXT, bytes, runOnFinish);
		Executors.defaultThreadFactory().newThread(saver).start();
	}

	private static final class FileAsyncSaver implements Runnable {

		private final String fileName;
		private final byte[] contents;
		private final Runnable runOnFinish;
		private final Context context;

		public FileAsyncSaver(Context context, String fileName, byte[] contents, Runnable runOnFinish) {
			this.fileName = fileName;
			this.contents = contents;
			this.runOnFinish = runOnFinish;
			this.context = context;
		}

		@Override
		public void run() {
			if (contents == null) {
				Logger.log("No content to save", LoggerLevel.VERBOSE);
				return;
			}

			FileOutputStream fos = null;
			try {
				fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
				fos.write(contents);
				fos.close();

				if (runOnFinish != null) {
					runOnFinish.run();
				}
			} catch (FileNotFoundException e) {
				Logger.log(e.toString(), LoggerLevel.WTF);
			} catch (Exception e) {
				Logger.log(e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						Logger.log(e);
					}
				}
			}
		}
	}

	public static Intent getOpenOptionsIntent(MainActivity mainActivity, Account account) throws RemoteException {
		Intent i = new Intent(mainActivity, OptionsActivity.class);
		if (account != null) {
			i.putExtra(Constants.INTENT_EXTRA_ACCOUNT, account);
		}

		return i;
	}

	public static boolean allowPageStoring(Page page) {
		for (Class<? extends Page> pageClass : ALLOWED_PAGES_FOR_STORING) {
			if (pageClass == page.getClass()) {
				return true;
			}
		}
		return false;
	}

	public static Class<? extends Page> getPageClassByPageId(String pageId) {
		for (Class<? extends Page> cls : ALLOWED_PAGES_FOR_STORING) {
			if (pageId.equals(cls.getSimpleName())) {
				return cls;
			}
		}

		return null;
	}

	public static String getFileNameFromPath(String filePath) {
		return filePath.contains(File.separator) ? filePath.substring(filePath.lastIndexOf(File.separator) + File.separator.length(), filePath.length()) : filePath;
	}

	public static Uri stringAsIntentDataUri(String string) {
		return Uri.parse("aceim://" + string);
	}

	public static void removeIcon(Context context, String filename) {
		context.deleteFile(filename + BUDDYICON_FILEEXT);
	}
	
	public static void fillBuddyPlaceholder(Context context, Buddy buddy, View container, ProtocolResources protocolResources) {
		
		AQuery aq = new AQuery(container);
		
		int[] extraImageIDs = new int[]{R.id.image_extra_1, R.id.image_extra_2, R.id.image_extra_3, R.id.image_extra_4};
		
		fillIcon(R.id.image_icon, aq, buddy.getFilename(), context);
		aq.id(R.id.image_status).image(getBuddyStatusIcon(context, buddy, protocolResources));
		aq.id(R.id.label_xstatus).text(getFormattedXStatus(buddy.getOnlineInfo(), context, protocolResources));
		
		Resources res;
		try {
			res = protocolResources.getNativeResourcesForProtocol(context.getPackageManager());
		} catch (AceImException e) {
			Logger.log(e);
			return;
		}

		int imagesIndex = 0;

		for (String featureId : buddy.getOnlineInfo().getFeatures().keySet()) {
			ProtocolServiceFeature feature = protocolResources.getFeature(featureId);

			if (feature == null) {
				Logger.log("Unknown protocol feature: " + featureId, LoggerLevel.INFO);
				continue;
			}

			if (!feature.isShowInIconList()) {
				continue;
			}
			
			if (feature instanceof ListFeature && !feature.getFeatureId().equals(ApiConstants.FEATURE_STATUS)) {
				ListFeature lf = (ListFeature) feature;
				byte value = buddy.getOnlineInfo().getFeatures().getByte(featureId, (byte) -1);

				if (value > -1) {
					aq.id(extraImageIDs[imagesIndex]).visibility(View.VISIBLE).image(res.getDrawable(lf.getDrawables()[value]));
					imagesIndex++;
				}
			} else {
				if (feature.getIconId() != 0){
					aq.id(extraImageIDs[imagesIndex]).visibility(View.VISIBLE).image(res.getDrawable(feature.getIconId()));
					imagesIndex++;
				}
			}
		}

		for (int i = imagesIndex; i < extraImageIDs.length; i++) {
			aq.id(extraImageIDs[i]).visibility(View.GONE);
		}
	}
	
	public static void fillAccountPlaceholder(Context context, Account account, View container, ProtocolResources protocolResources) {
		AQuery aq = new AQuery(container);
		
		int[] extraImageIDs = new int[]{R.id.image_extra_1, R.id.image_extra_2, R.id.image_extra_3, R.id.image_extra_4};
		
		fillIcon(R.id.image_icon, aq, account.getFilename(), context);
		aq.id(R.id.image_status).image(getAccountStatusIcon(context, account, protocolResources));
		aq.id(R.id.label_xstatus).text(getFormattedXStatus(account.getOnlineInfo(), context, protocolResources));		

		Resources res;
		try {
			res = protocolResources.getNativeResourcesForProtocol(context.getPackageManager());
		} catch (AceImException e) {
			Logger.log(e);
			return;
		}

		int imagesIndex = 0;

		for (String featureId : account.getOnlineInfo().getFeatures().keySet()) {
			ProtocolServiceFeature feature = protocolResources.getFeature(featureId);

			if (feature == null) {
				Logger.log("Unknown protocol feature: " + featureId, LoggerLevel.INFO);
				continue;
			}

			if (!feature.isShowInIconList() ||  feature.getFeatureId().equals(ApiConstants.FEATURE_STATUS)) {
				continue;
			}
			
			if (feature instanceof ListFeature) {
				ListFeature lf = (ListFeature) feature;
				byte value = account.getOnlineInfo().getFeatures().getByte(featureId, (byte) -1);

				if (value > -1) {
					aq.id(extraImageIDs[imagesIndex]).visibility(View.VISIBLE).image(res.getDrawable(lf.getDrawables()[value]));
					imagesIndex++;
				}
			} else {
				if (feature.getIconId() != 0){
					aq.id(extraImageIDs[imagesIndex]).visibility(View.VISIBLE).image(res.getDrawable(feature.getIconId()));
					imagesIndex++;
				}
			}
		}
		
		for (int i = imagesIndex; i < extraImageIDs.length; i++) {
			aq.id(extraImageIDs[i]).visibility(View.GONE);
		}
	}

	public static void setWallpaperMode(Activity activity, View target) {
		boolean forceDrawWallpaper = activity.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getBoolean(GlobalOptionKeys.FORCE_DRAW_WALLPAPER.name(),
				Boolean.parseBoolean(activity.getString(R.string.default_force_draw_wallpaper)));

		if (forceDrawWallpaper) {
			BitmapDrawable wallpaper = (BitmapDrawable) activity.getWallpaper();

			if (wallpaper == null) {
				Logger.log("Unsupported wallpaper", LoggerLevel.DEBUG);
				return;
			}

			wallpaper.setDither(false);

			if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				wallpaper.setGravity(wallpaper.getBitmap().getHeight() <= wallpaper.getBitmap().getWidth() ? Gravity.CENTER_HORIZONTAL | Gravity.FILL_VERTICAL : Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
			} else {
				wallpaper.setGravity(wallpaper.getBitmap().getHeight() <= wallpaper.getBitmap().getWidth() ? Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL : Gravity.CENTER_HORIZONTAL | Gravity.FILL_VERTICAL);
			}

			target.setBackgroundDrawable(wallpaper);
		} else {
			target.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	public static void resetFeaturesForOffline(OnlineInfo info, ProtocolResources mProtocolResources, boolean resetStatus) {
		for (String featureKey : new ArrayList<String>(info.getFeatures().keySet())) {
			ProtocolServiceFeature feature = mProtocolResources.getFeature(featureKey);
			
			if (feature == null || !feature.isAvailableOffline()) {
				info.getFeatures().remove(featureKey);
			}
		}
		
		if (resetStatus) {
			info.getFeatures().remove(ApiConstants.FEATURE_STATUS);
		}
	}
	
	public static File getBitmapFile(Context context, String filename){
		return new File(context.getFilesDir().getAbsolutePath() + File.separator + filename + BUDDYICON_FILEEXT);
	}

	public static Bitmap getIcon(Context context, String filename) {
		FileInputStream fis = null;
		try {
			fis = context.openFileInput(filename + BUDDYICON_FILEEXT);
		} catch (Exception e) {
		}

		if (fis == null)
			return null;

		BitmapFactory.Options options = new BitmapFactory.Options();

		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(fis, null, options);

		if (!checkAvailableRamForBitmap(options.outHeight, options.outWidth))
			return null;

		try {
			fis = context.openFileInput(filename + BUDDYICON_FILEEXT);
		} catch (Exception e) {
		}

		options.inJustDecodeBounds = false;
		options.inDither = true;
		options.inScaled = false;
		options.inPurgeable = true;
		options.inPreferredConfig = Bitmap.Config.RGB_565;

		return BitmapFactory.decodeStream(fis, null, options);
	}
	
	private static synchronized final boolean checkAvailableRamForBitmap(int h, int w) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			if ((h * w * 2) > (Debug.getNativeHeapFreeSize() * 0.75)) {
				Logger.log("LOW MEMORY " + Runtime.getRuntime().freeMemory());
				return false;
			}
		} else {
			if ((h * w * 2) > (Runtime.getRuntime().freeMemory() * 0.75)) {
				Logger.log("LOW MEMORY " + Runtime.getRuntime().freeMemory());
				return false;
			}
		}

		return true;
	}

	public static boolean hasIcon(Context context, String filename) {
		FileInputStream fis = null;
		try {
			fis = context.openFileInput(filename + BUDDYICON_FILEEXT);
			fis.close();
		} catch (Exception e) {
		} 

		return fis != null;
	}

	public static void fillIcon(int imageIcon, View v, String filename, Context context) {
		fillIcon(imageIcon, new AQuery(v), filename, context);
	}

	public static void fillIcon(int imageIcon, AQuery aq, String filename, Context context) {
		BitmapAjaxCallback callback = new BitmapAjaxCallback();
		File file = getBitmapFile(context, filename);
		callback
				//.animation(android.R.anim.slide_in_left)
				.memCache(true)
				.fallback(R.drawable.dummy_icon)
				//.targetWidth(context.getResources().getDimensionPixelSize(R.dimen.contact_list_grid_item_size))
				.file(file)
				.url(file.getAbsolutePath());
		aq.id(imageIcon).image(callback);		
	}

	public static List<ChatMessageHolder> wrapMessages(Buddy buddy, Account account, List<Message> messages) {
		if (messages == null) {
			return Collections.emptyList();
		}
		
		List<ChatMessageHolder> messageHolders = new ArrayList<ChatMessageHolder>(messages.size());		
		
		for (Message m : messages) {
			String senderName = m.isIncoming() ? (m.getContactDetail() != null ? m.getContactDetail() : buddy.getSafeName()) : account.getSafeName();
			messageHolders.add(new ChatMessageHolder(m, senderName));
		}
		
		return messageHolders;
	}

	public static void removeAccountIcons(Account account, Context context) {
		if (account == null) return; 
		
		for (Buddy buddy : account.getBuddyList()) {
			context.deleteFile(buddy.getFilename() + BUDDYICON_FILEEXT);
		}
		
		context.deleteFile(account.getFilename() + BUDDYICON_FILEEXT);
	}
}
