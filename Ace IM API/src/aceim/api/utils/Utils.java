package aceim.api.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import aceim.api.IProtocol;
import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.Entity;
import aceim.api.dataentity.ListFeature;
import aceim.api.service.ApiConstants;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A utility class with useful methods.
 */
public final class Utils {
	
	private Utils(){}

	/**
	 * Get IP address I'm currently online with.
	 * 
	 * @return IP address in dot-separated form
	 * @throws SocketException if something bad happened during IP address recovering
	 */
	public static String getMyIp() throws SocketException {
		List<String> ips = new LinkedList<String>();
		for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
			NetworkInterface intf = en.nextElement();
			for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
				InetAddress inetAddress = enumIpAddr.nextElement();
				if (!inetAddress.isLoopbackAddress()) {
					ips.add(inetAddress.getHostAddress());						
				}
			}
		}
		return ips.get(0);
	}
	
	/**
	 * Create SD card file instance for incoming file transfer.
	 * 
	 * @param filename file name, without path
	 * @param buddy file sender
	 * @param modTime modification time (optional, set if greater than zero)
	 * @return
	 */
	public static File createLocalFileForReceiving(String filename, Buddy buddy, long modTime) {
		File root = Environment.getExternalStorageDirectory();
		File downloads;
		
		if (buddy != null) {
			downloads = new File(root, "AceIM" + File.separator + buddy.getOwnerUid() + File.separator + buddy.getProtocolUid() + "_" + buddy.getName());
		} else {
			downloads = new File(root, "AceIM");
		}
		
		downloads.mkdirs();
		
		File file = new File(downloads, filename);

		if (file.exists()) {
			boolean deleted = file.delete();
			if (!deleted) {
				file = new File(downloads, new Random().nextInt() + "_" + filename);
			}
		}
		if (modTime > 0) {
			file.setLastModified(modTime);
		}

		return file;
	}
	
	/**
	 * Escape {@link ApiConstants#GENERAL_DIVIDER} instances with underline signs.
	 * @param input input string
	 * @return result
	 */
	public static String escapeGeneralDividers(String input) {
		return input.replace(ApiConstants.GENERAL_DIVIDER, '_');
	}
	
	/**
	 * Get typed array from resources and return it as integer aray. Used, for example, in {@link ListFeature} entities.
	 * @param r resources
	 * @param resourceId typed array's resource ID
	 * @return integer array of resource IDs, or null if input resourceId is not valid
	 */
	public static int[] fillResources(Resources r, int resourceId) {
		if (resourceId < 0) {
			return null;
		}
		
		TypedArray ta = r.obtainTypedArray(resourceId);
		int[] targetArray = new int[ta.length()];

		for (int i = 0; i < targetArray.length; i++) {
			targetArray[i] = ta.getResourceId(i, 0);
		}

		ta.recycle();
		return targetArray;
	}
	
	/**
	 * Convert map of parcelables to bundle.
	 * @param map
	 * @return
	 */
	public static final Bundle map2Bundle(Map<String, Parcelable> map) {
		if (map == null) {
			return null;
		}
		
		Bundle b = new Bundle();
		for (String k : map.keySet()) {
			b.putParcelable(k, map.get(k));
		}	
		
		return b;
	}
	
	/**
	 * Convers bundle with parcelables to map.
	 * @param bundle
	 * @return
	 */
	public static final Map<String, Parcelable> bundle2Map(Bundle bundle){
		if (bundle == null) {
			return null;
		}
		
		Map<String, Parcelable> map = new HashMap<String, Parcelable>();
		for (String k : bundle.keySet()){
			map.put(k, bundle.getParcelable(k));
		}
		
		return map;
	}
	
	/**
	 * Scale and crop to square image, for usage with {@link IProtocol#uploadAccountPhoto(String)}. If input file is already less than required size, no additional processing is applied.
	 * @param filePath filesystem path to image file, should be accessible
	 * @param targetSize target side length in pixels
	 * @return resized and cropped image, in a form of byte array
	 */
	public static byte[] scaleAccountIcon(String filePath, int targetSize) {
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, o);
		
		if (o.outWidth > o.outHeight) {
			o.inSampleSize = o.outWidth / targetSize;
		} else {
			o.inSampleSize = o.outHeight / targetSize;
		}
		
		o.inJustDecodeBounds = false;
		
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		try {
			Bitmap b = BitmapFactory.decodeFile(filePath, o);
			
			if (o.outHeight > targetSize || o.outWidth > targetSize) {
				Bitmap newb;
				
				if (o.outHeight < o.outWidth) {
					newb = Bitmap.createScaledBitmap(b, (int) (((targetSize + 0f)*o.outWidth)/o.outHeight), targetSize, false);
				} else {
					newb = Bitmap.createScaledBitmap(b, targetSize, (int) (((targetSize + 0f)*o.outHeight)/o.outWidth), false);
				}
				
				if (b != newb) {
					b.recycle();
					b = newb;
				}
				
				if (b.getWidth() >= b.getHeight()){
					newb = Bitmap.createBitmap(b, (int) ((b.getWidth() - b.getHeight()) / 2f), 0, b.getHeight(), b.getHeight());
				} else {
					newb = Bitmap.createBitmap(b, 0, (int) ((b.getHeight() - b.getWidth()) / 2f), b.getWidth(), b.getWidth());
				}
				
				b.recycle();
				b = newb;				
			}
			
			b.compress(Bitmap.CompressFormat.JPEG, 100, bo);
			b.recycle();
			return bo.toByteArray();
		} finally {
			try {
				bo.close();
			} catch (IOException e) {
				Logger.log(e);
			}
		}
	}
	
	/**
	 * Read {@link Entity} from a parcel, if entity class is known.
	 * @param in parcel container
	 * @param cls entity class
	 * @return unparcelled entity
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Parcelable> T unparcelEntity(Parcel in, Class<T> cls) {
		String className = in.readString();
		
		try {
			if (className.equals(cls.getName())) {
				return cls.getConstructor(Parcel.class).newInstance(in);
			} 
		} catch (Exception e) {
			Logger.log(e);
		}
		
		return (T) unparcelUnknownEntity(in, className);
	}

	/**
	 * Read {@link Entity} from a parcel by class name
	 * @param in parcel container
	 * @param className entity class name
	 * @return recovered entity, or null if class name is invalid, or the class entity cannot be created 
	 */
	@SuppressWarnings("unchecked")
	public static Entity unparcelUnknownEntity(Parcel in, String className) {
		try {
			Class<? extends Entity> cls = (Class<? extends Entity>) Class.forName(className);
			Class<?>[] paramTypes = { Parcel.class }; 
			Constructor<? extends Entity> constructor = cls.getConstructor(paramTypes);
			return constructor.newInstance(in);
		} catch (Exception e) {
			Logger.log(e);
		}
		
		return null;
	}
	
	/**
	 * Escape XML characters in a string.
	 * @param input
	 * @return
	 */
	public static String escapeXMLString(String input) {
		if (input == null) return null; 
		
		return input.replaceAll("&", "&amp;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	
	/**
	 * Unescape XML characters in a string.
	 * @param input
	 * @return
	 */
	public static String unescapeXMLString(String input) {
		if (input == null) return null; 
		
		return input.replaceAll("&amp;", "&").replaceAll("&apos;", "'").replaceAll("&quot;", "\"").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
	}
}
