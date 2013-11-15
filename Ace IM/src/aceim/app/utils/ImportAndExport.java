package aceim.app.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import aceim.api.dataentity.FileProgress;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.api.utils.Utils;
import aceim.app.service.IUserInterface;
import aceim.app.service.ServiceUtils;
import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;

public final class ImportAndExport {

	private static String root = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "AceImTmp" + File.separator;
	private static final int TOTAL_PROGRESS = 10000;

	private ImportAndExport() {
	}

	public static final void exportData(final FileProgress progress, final String password, final IUserInterface ui, final Context context) {
		Logger.log("Export data request", LoggerLevel.VERBOSE);

		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				File[] files = context.getApplicationContext().getFilesDir().listFiles();

				FileProgress p = new FileProgress(progress.getServiceId(), progress.getMessageId(), "", TOTAL_PROGRESS, 10, progress.isIncoming(), progress.getOwnerUid(), null);
				try {
					ui.onFileProgress(p);
				} catch (Exception e1) {
					Logger.log(e1);
				}

				zipAndEncode(progress, files, password, ui, context);
			}
		}).start();
	}

	private static void zipAndEncode(FileProgress progress, File[] files, String password, IUserInterface ui, Context context) {
		Logger.log("Going to zip: " + Arrays.toString(files), LoggerLevel.VERBOSE);
		
		File target = Utils.createLocalFileForReceiving("Export " + DateFormat.getLongDateFormat(context).format(Calendar.getInstance().getTime()), null, 0);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(target);
			CipherOutputStream cos = new CipherOutputStream(new BufferedOutputStream(fos), generateKey(password, Cipher.ENCRYPT_MODE));
			ZipOutputStream zos = new ZipOutputStream(cos);

			byte[] buffer = new byte[2048];

			FileProgress p = new FileProgress(progress.getServiceId(), progress.getMessageId(), target.getAbsolutePath(), TOTAL_PROGRESS, progress.getSentBytes() + 5, progress.isIncoming(), progress.getOwnerUid(), null);
			try {
				ui.onFileProgress(p);
			} catch (Exception e1) {
				Logger.log(e1);
			}

			for (File file : files) {
				FileInputStream in = new FileInputStream(file);
				zos.putNextEntry(new ZipEntry(file.getName()));

				Logger.log("Zipping: " + file, LoggerLevel.VERBOSE);

				p = new FileProgress(p.getServiceId(), p.getMessageId(), target.getAbsolutePath(), TOTAL_PROGRESS, p.getSentBytes() + 5, p.isIncoming(), p.getOwnerUid(), null);
				try {
					ui.onFileProgress(p);
				} catch (Exception e1) {
					Logger.log(e1);
				}

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
				in.close();
			}
			
			zos.close();
			
			p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, TOTAL_PROGRESS, progress.isIncoming(), progress.getOwnerUid(), null);
			try {
				ui.onFileProgress(p);
			} catch (Exception e1) {
				Logger.log(e1);
			}

			Logger.log("Export succeded to: " + target, LoggerLevel.VERBOSE);
		} catch (Exception e) {
			Logger.log(e);
			
			FileProgress p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, TOTAL_PROGRESS, progress.isIncoming(), progress.getOwnerUid(), e.toString());
			try {
				ui.onFileProgress(p);
			} catch (Exception e1) {
				Logger.log(e1);
			}

			try {
				if (fos != null) {
					fos.close();
				}
				
				target.delete();
			} catch (IOException e1) {
				Logger.log(e1);
			}
			
		}
	}

	public static final void importData(final FileProgress progress, final String password, final IUserInterface ui, final Context context) {
		Logger.log("Import data request from " + progress.getFilePath(), LoggerLevel.VERBOSE);

		Executors.defaultThreadFactory().newThread(new Runnable() {

			@Override
			public void run() {
				try {
					File tmpFolder = new File(root);
					tmpFolder.mkdirs();
					
					File[] files = decodeAndUnzip(progress, password, ui, context);

					for (File file : files) {
						importFile(file, progress, ui, context);
					}

					FileProgress p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, TOTAL_PROGRESS, progress.isIncoming(), progress.getOwnerUid(), null);
					try {
						ui.onFileProgress(p);
					} catch (Exception e1) {
						Logger.log(e1);
					}

					for (File file : tmpFolder.listFiles()) {
						file.delete();
					}
					tmpFolder.delete();
				} catch (Exception e) {
					Logger.log(e);
					FileProgress p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, 10, progress.isIncoming(), progress.getOwnerUid(), e.getMessage());
					try {
						ui.onFileProgress(p);
					} catch (Exception e1) {
						Logger.log(e1);
					}
				}
			}
		}).start();
	}

	private static void importFile(File file, FileProgress progress, IUserInterface ui, Context context) {
		Logger.log("Importing: " + file, LoggerLevel.VERBOSE);

		try {
			FileInputStream fis = new FileInputStream(file);
			FileOutputStream fos = context.openFileOutput(file.getName(), ServiceUtils.getAccessMode());

			copyFile(fis, fos);

			fos.close();
			fis.close();
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	private static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	private static final File[] decodeAndUnzip(FileProgress progress, String password, IUserInterface ui, Context context) {
		Logger.log("Unzipping...", LoggerLevel.VERBOSE);

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(progress.getFilePath());
			CipherInputStream cis = new CipherInputStream(new BufferedInputStream(fis), generateKey(password, Cipher.DECRYPT_MODE));
			ZipInputStream zis = new ZipInputStream(cis);

			String filename;
			ZipEntry ze;
			int count;
			byte[] buffer = new byte[2048];

			FileProgress p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, 10, progress.isIncoming(), progress.getOwnerUid(), progress.getError());
			try {
				ui.onFileProgress(p);
			} catch (Exception e) {
				Logger.log(e);
			}

			while ((ze = zis.getNextEntry()) != null) {
				filename = ze.getName();

				p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, p.getSentBytes() + 1, progress.isIncoming(), progress.getOwnerUid(), progress.getError());
				try {
					ui.onFileProgress(p);
				} catch (Exception e) {
					Logger.log(e);
				}

				if (ze.isDirectory()) {
					Logger.log("Dir found: " + filename, LoggerLevel.VERBOSE);

					File fmd = new File(root + filename);
					fmd.mkdirs();

					continue;
				}

				Logger.log("File found: " + filename, LoggerLevel.VERBOSE);

				FileOutputStream fout = new FileOutputStream(root + filename);

				while ((count = zis.read(buffer)) != -1) {
					fout.write(buffer, 0, count);
				}

				Logger.log("File unzipped: " + filename, LoggerLevel.VERBOSE);

				fout.close();
				zis.closeEntry();
			}

			zis.close();

			p = new FileProgress(progress.getServiceId(), progress.getMessageId(), progress.getFilePath(), TOTAL_PROGRESS, TOTAL_PROGRESS / 2, progress.isIncoming(), progress.getOwnerUid(), progress.getError());
			try {
				ui.onFileProgress(p);
			} catch (Exception e) {
				Logger.log(e);
			}

			File[] files = new File(root).listFiles();

			Logger.log("Successfully unzipped: " + Arrays.toString(files), LoggerLevel.VERBOSE);

			return files;
		} catch (Exception e) {
			Logger.log(e);
			
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e1) {
					Logger.log(e1);
				}
			}
			
			return new File[0];
		}
	}

	private static Cipher generateKey(String password, int cipherMode) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {
		byte[] keyStart = password.getBytes("UTF-8");

	    KeyGenerator kgen = KeyGenerator.getInstance("AES");
	    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
	    sr.setSeed(keyStart);
	    kgen.init(128, sr);
	    SecretKey skey = kgen.generateKey();

	    byte[] key = skey.getEncoded();

		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(cipherMode, skeySpec);

		return cipher;
	}
}
