package aceim.protocol.snuk182.icq.utils;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public final class ProtocolUtils {
	static AtomicInteger atomicInt = new AtomicInteger();

	public static byte[] short2ByteLE(short in) {
		byte[] out = new byte[2];
		byte hexBase = (byte) 0xff;
		out[0] = (byte) (hexBase & in);
		out[1] = (byte) (((hexBase << 8) & in) >> 8);

		return out;
	}

	public static byte[] int2ByteLE(int in) {
		byte[] out = new byte[4];
		byte hexBase = (byte) 0xff;
		out[0] = (byte) (hexBase & in);
		out[1] = (byte) (((hexBase << 8) & in) >> 8);
		out[2] = (byte) (((hexBase << 16) & in) >> 16);
		out[3] = (byte) (((hexBase << 24) & in) >> 24);

		return out;
	}

	public static byte[] short2ByteBE(short in) {
		byte[] out = new byte[2];
		byte hexBase = (byte) 0xff;
		out[1] = (byte) (hexBase & in);
		out[0] = (byte) (((hexBase << 8) & in) >> 8);

		return out;
	}

	public static byte[] int2ByteBE(int in) {
		byte[] out = new byte[4];
		byte hexBase = (byte) 0xff;
		out[3] = (byte) (hexBase & in);
		out[2] = (byte) (((hexBase << 8) & in) >> 8);
		out[1] = (byte) (((hexBase << 16) & in) >> 16);
		out[0] = (byte) (((hexBase << 24) & in) >> 24);

		return out;
	}

	public static byte[] long2ByteBE(long in) {
		byte[] out = new byte[8];
		byte hexBase = (byte) 0xff;
		out[7] = (byte) (hexBase & in);
		out[6] = (byte) (((hexBase << 8) & in) >> 8);
		out[5] = (byte) (((hexBase << 16) & in) >> 16);
		out[4] = (byte) (((hexBase << 24) & in) >> 24);
		out[3] = (byte) (((hexBase << 32) & in) >> 32);
		out[2] = (byte) (((hexBase << 40) & in) >> 40);
		out[1] = (byte) (((hexBase << 48) & in) >> 48);
		out[0] = (byte) (((hexBase << 56) & in) >> 56);
		return out;
	}

	public static byte[] long2ByteLE(long in) {
		byte[] out = new byte[8];
		byte hexBase = (byte) 0xff;
		out[0] = (byte) (hexBase & in);
		out[1] = (byte) (((hexBase << 8) & in) >> 8);
		out[2] = (byte) (((hexBase << 16) & in) >> 16);
		out[3] = (byte) (((hexBase << 24) & in) >> 24);
		out[4] = (byte) (((hexBase << 32) & in) >> 32);
		out[5] = (byte) (((hexBase << 40) & in) >> 40);
		out[6] = (byte) (((hexBase << 48) & in) >> 48);
		out[7] = (byte) (((hexBase << 56) & in) >> 56);
		return out;
	}

	public static long unsignedInt2Long(int in) {
		long out = 0xffffffffL;
		out &= in;
		return out;
	}

	public static short unsignedByte2Short(byte in) {
		short out = 0xff;
		out &= in;
		return out;
	}

	public static int unsignedShort2Int(short in) {
		int out = 0x0000ffff;
		out &= in;
		return out;
	}

	public static short bytes2ShortBE(byte[] in, int pos) {
		if (in == null || in.length < 2 + pos)
			return 0;

		short out = 0;
		out += (in[pos + 1]) & 0x00ff;
		out += (in[pos] << 8) & 0xff00;
		return out;
		// return (short) ((in[pos]<<8)+in[pos+1]);
	}

	public static short bytes2ShortLE(byte[] in, int pos) {
		if (in == null || in.length < 2 + pos)
			return 0;

		short out = 0;
		out += (in[pos]) & 0x00ff;
		out += (in[pos + 1] << 8) & 0xff00;
		return out;
		// return (short) ((in[pos+1]<<8)+in[pos]);
	}

	public static short bytes2ShortBE(byte[] in) {
		return bytes2ShortBE(in, 0);
	}

	public static short bytes2ShortLE(byte[] in) {
		return bytes2ShortLE(in, 0);
	}

	public static int bytes2IntBE(byte[] in) {
		return bytes2IntBE(in, 0);
	}

	public static int bytes2IntLE(byte[] in) {
		return bytes2IntLE(in, 0);
	}
	
	public static long bytes2LongBE(byte[] in) {
		return bytes2LongBE(in, 0);
	}

	public static long bytes2LongLE(byte[] in) {
		return bytes2LongLE(in, 0);
	}

	public static int bytes2IntBE(byte[] in, int pos) {
		if (in == null || in.length < 4 + pos)
			return 0;

		int out = 0;
		out += in[pos + 3] & 0x00ff;
		out += (in[pos + 2] << 8) & 0xff00;
		out += (in[pos + 1] << 16) & 0xff0000;
		out += (in[pos] << 24) & 0xff000000;
		return out;
		// return (in[pos]<<24)+(in[pos+1]<<16)+(in[pos+2]<<8)+(in[pos+3]);
	}

	public static int bytes2IntLE(byte[] in, int pos) {
		if (in == null || in.length < 4 + pos)
			return 0;

		int out = 0;
		out += in[pos] & 0xff;
		out += (in[pos + 1] << 8) & 0xff00;
		out += (in[pos + 2] << 16) & 0xff0000;
		out += (in[pos + 3] << 24) & 0xff000000;
		return out;
		// return (in[pos+3]<<24)+(in[pos+2]<<16)+(in[pos+1]<<8)+(in[pos]);
	}

	public static long bytes2LongBE(byte[] in, int pos) {
		if (in == null || in.length < 8 + pos)
			return 0;

		long out = 0L;
		/*
		 * out+=((in[pos+7])&0xff); out+=((in[pos+6]<<8)&0xff00);
		 * out+=((in[pos+5]<<16)&0xff0000); out+=((in[pos+4]<<24)&0xff000000);
		 * out+=((in[pos+3]<<32)&0xff00000000L);
		 * out+=((in[pos+2]<<40)&0xff0000000000L);
		 * out+=((in[pos+1]<<48)&0xff000000000000L);
		 * out+=((in[pos]<<56)&0xff00000000000000L);
		 */

		for (int i = 0; i < 8; i++) {
			out <<= 8;
			out += (in[pos + i] & 0x00000000000000ffL);
		}

		return out;
		// return
		// (in[pos]<<56)+(in[pos+1]<<48)+(in[pos+2]<<40)+(in[pos+3]<<32)+(in[pos+4]<<24)+(in[pos+5]<<16)+(in[pos+6]<<8)+(in[pos+7]);
	}

	public static long bytes2LongLE(byte[] in, int pos) {
		if (in == null || in.length < 8 + pos)
			return 0;

		long out = 0;
		/*
		 * out+=(in[pos])&0xff; out+=(in[pos+1]<<8)&0xff00;
		 * out+=(in[pos+2]<<16)&0xff0000; out+=(in[pos+3]<<24)&0xff000000;
		 * out+=(in[pos+4]<<32)&0xff00000000L;
		 * out+=(in[pos+5]<<40)&0xff0000000000L;
		 * out+=(in[pos+6]<<48)&0xff000000000000L;
		 * out+=(in[pos+7]<<56)&0xff00000000000000L;
		 */

		for (int i = 7; i >= 0; i--) {
			out <<= 8;
			out += (in[pos + i] & 0x00000000000000ffL);
		}

		return out;
		// return
		// (in[pos+7]<<56)+(in[pos+6]<<48)+(in[pos+5]<<40)+(in[pos+4]<<32)+(in[pos+3]<<24)+(in[pos+2]<<16)+(in[pos+1]<<8)+(in[pos]);
	}

	static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

	public static String getSpacedHexString(byte[] raw) {
		if (raw == null){
			return null;
		}
		byte[] hex = new byte[3 * raw.length];
		int index = 0;

		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
			hex[index++] = (byte) ' ';
		}
		return new String(hex);
	}
	
	public static String getIPString(byte[] ipBytes){
		return getIPString(ipBytes, 0);
	}
	
	public static String getIPString(byte[] ipBytes, int pos){
		if (ipBytes == null || ipBytes.length <= (pos)){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i=pos; i<ipBytes.length; i++){
			sb.append(unsignedByte2Short(ipBytes[i]));
			if (i < ipBytes.length-1){
				sb.append('.');
			}
		}
		return sb.toString();
	}

	public static String getHexString(byte[] raw) {
		if (raw == null) {
			return "";
		}
		byte[] hex = new byte[2 * raw.length];
		int index = 0;

		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		return new String(hex);
	}

	public static int getAtomicInt() {
		// return atomicInt.intValue();
		return new Random().nextInt();
	}

	public static short getAtomicShort() {
		return (short) new Random().nextInt(0x8000);
	}

	public static short[] byteArrayToShortArrayBE(byte[] in) {
		if (in == null)
			return null;
		short[] out = new short[in.length / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = (short) ((in[2 * i + 1] << 8) + in[2 * i]);
		}
		return out;
	}

	public static short[] byteArrayToShortArrayLE(byte[] in) {
		if (in == null)
			return null;
		short[] out = new short[in.length / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = (short) ((in[2 * i] << 8) + in[2 * i + 1]);
		}
		return out;
	}
	
	public static String getEncodedString(byte[] in, int offset, int len){
		return getEncodedString(in, offset, len, "UTF-8");
	}

	public static String getEncodedString(byte[] in, int offset, int len, String encoding) {
		if (in == null)
			return "";
		String out;
		try {
			out = new String(in, offset, len, encoding);
		} catch (UnsupportedEncodingException e) {
			out = new String(in, offset, len);
		}
		return out;
	}
	
	public static String getEncodedString(byte[] in){
		return getEncodedString(in, "UTF-8");
	}

	public static String getEncodedString(byte[] in, String encoding) {
		if (in == null)
			return "";
		String out;
		try {
			out = new String(in, encoding);
		} catch (UnsupportedEncodingException e) {
			out = new String(in);
		}
		return out;
	}

	public static Date bytes2Date(byte[] in) {
		return new Date(ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntBE(in)) * 1000);
	}

	public static String xmlToParameter(String xml) {
		return xml.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	public static String xmlFromParameter(String par) {
		return par.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
	}

	/*public static BuddyGroup getBuddyGroupByGroupId(short id, List<BuddyGroup> groups) {
		for (BuddyGroup group : groups) {
			if (group.id == id) {
				return group;
			}
		}
		return null;
	}*/
	
	public static final byte[] unxorByteArray(byte[] in){
		if (in == null){
			return null;
		}
		
		byte[] out = new byte[in.length];
		
		for (int i=0; i<in.length; i++){
			out[i] = (byte) (in[i]^0xff);
		}
		
		return out;
	}

	public static byte[] ipString2ByteBE(String rvIp) {
		if (rvIp == null){
			return new byte[0];
		}
		String[] bytes = rvIp.split("\\.");
		byte[] ip = new byte[bytes.length];
		
		for (int i=0; i<bytes.length; i++){
			try {
				ip[i] = (byte) Integer.parseInt(bytes[i]);
			} catch (NumberFormatException e) {
				ip[i] = 0;
			}
		}
		
		return ip;
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T[] concatObjectArrays(T[] a, T[] b) {
	    final int alen = a.length;
	    final int blen = b.length;
	    if (alen == 0) {
	        return b;
	    }
	    if (blen == 0) {
	        return a;
	    }
	    final T[] result = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), alen + blen);
	    System.arraycopy(a, 0, result, 0, alen);
	    System.arraycopy(b, 0, result, alen, blen);
	    return result;
	}

	public static final byte[] concatByteArrays(byte[] a, byte[] b) {
	    final int alen = a.length;
	    final int blen = b.length;
	    if (alen == 0) {
	        return b;
	    }
	    if (blen == 0) {
	        return a;
	    }
	    final byte[] result = new byte[alen + blen];
	    System.arraycopy(a, 0, result, 0, alen);
	    System.arraycopy(b, 0, result, alen, blen);
	    return result;
	}
}
