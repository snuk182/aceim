package aceim.protocol.snuk182.icq.inner;

public interface ICQServiceResponse {

	public Object respond(short action, Object... args);

	public static final short RES_NOP = 0;
	public static final short RES_SAVEPARAMS = 1;
	public static final short RES_CONNECTED = 2;
	public static final short RES_DISCONNECTED = 3;
	public static final short RES_STATUSSET = 4;
	public static final short RES_EXTENDEDSTATUSSET = 5;
	public static final short RES_MESSAGE = 6;
	public static final short RES_FILEMESSAGE = 7;
	public static final short RES_USERINFO = 8;
	public static final short RES_OWNINFO = 9;
	public static final short RES_OWNINFOSET = 10;
	public static final short RES_BUDDYADDED = 11;
	public static final short RES_BUDDYDELETED = 12;
	public static final short RES_CLUPDATED = 13;
	public static final short RES_BUDDYSTATECHANGED = 14;
	public static final short RES_SAVEIMAGEFILE = 15;
	public static final short RES_NOTIFICATION = 16;
	public static final short RES_ACCOUNTUPDATED = 17;
	//public static final short RES_SAVETOSTORAGE = 18;
	//public static final short RES_GETFROMSTORAGE = 19;
	public static final short RES_LOG = 20;
	public static final short RES_AUTHREQUEST = 21;
	public static final short RES_SEARCHRESULT = 22;
	public static final short RES_BUDDYMODIFIED = 23;
	public static final short RES_GROUPMODIFIED = 24;
	public static final short RES_GROUPADDED = 25;
	public static final short RES_GROUPDELETED = 26;
	public static final short RES_CONNECTING = 27;
	public static final short RES_FILEPROGRESS = 28;
	public static final short RES_MESSAGEACK = 29;
	public static final short RES_TYPING = 30;
	
	public static final short RES_KEEPALIVE = 31;
	public static final short RES_ACCOUNT_ACTIVITY = 32;
	public static final short RES_GET_FILE_FOR_SAVING = 33;
}
