package aceim.protocol.snuk182.mrim.inner.dataentity;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class MrimFileTransfer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 633403985617127426L;
	
	public List<File> files;
	public List<MrimIncomingFile> incomingFiles;
	public String buddyMrid;

	public int messageId = 0;
	
	public String host;
	public int port;
	
	public byte connection = CONN_PEER;

	public byte[] proxySessionId = null;
	
	public static final byte CONN_PEER = 0;
	public static final byte CONN_MIRROR = 1;
	public static final byte CONN_PROXY = 2;
}
