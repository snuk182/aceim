package aceim.protocol.snuk182.icq.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CheckedInputStream;

public final class Checksum {

	public static long getCRC32Checksum(File file) throws IOException {

		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new java.util.zip.CRC32());

		byte[] buf = new byte[128];
		while (cis.read(buf) >= 0) {
		}

		long val = cis.getChecksum().getValue();

		cis.close();

		return val;
	}

	public static long getMD5Checksum(File file) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		InputStream is = new FileInputStream(file);
		try {
			is = new DigestInputStream(is, md);
		} finally {
			is.close();
		}
		byte[] digest = md.digest();

		return new BigInteger(1, digest).longValue();
	}
}
