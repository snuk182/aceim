package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQException;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICBMParams;
import aceim.protocol.snuk182.icq.inner.dataentity.RateGroup;
import aceim.protocol.snuk182.icq.inner.dataentity.RateLimit;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.MD5;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class AuthenticationProcessor extends AbstractFlapProcessor {

	private final byte[] pwRoastArray = { (byte) 0xF3, 0x26, (byte) 0x81, (byte) 0xC4, 0x39, (byte) 0x86, (byte) 0xDB, (byte) 0x92, 0x71, (byte) 0xA3, (byte) 0xB9, (byte) 0xE6, 0x53, 0x7A, (byte) 0x95, 0x7C };
	private byte[] authCookie = null;
	private String reconnectAddress = null;
	private int reconnectPort = 0;

	public boolean isSecureLogin = false;
	
	public static final int MAX_PASSWORD_LEN = 16;

	private static final String AIM_MD5_STRING = "AOL Instant Messenger (SM)";

	private boolean endLogin = false;
	private ScheduledFuture<?> task;
	private ScheduledThreadPoolExecutor startupTimer = new ScheduledThreadPoolExecutor(1);

	private AuthExpireTimerTask expireTimerTask = new AuthExpireTimerTask();

	@Override
	public void init(ICQServiceInternal service) throws ICQException {
		super.init(service);
		service.log("---------------auth thread---------------");
	}

	private Flap sendCookie() throws ICQException {
		service.log("--- send cookie");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_START;

		TLV[] tlvs = new TLV[2];

		tlvs[0] = new TLV((short) 0x0, new byte[] { 0x00, 0x01 });

		tlvs[1] = new TLV(ICQConstants.TLV_AUTHCOOKIE, authCookie);

		flap.tlvData = tlvs;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap createPlainAuthRequest(boolean sendAuthData) throws ICQException {
		service.log("--- plain auth request");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_START;

		TLV[] tlvs;

		if (sendAuthData) {
			tlvs = new TLV[5];

			tlvs[0] = new TLV((short) 0x0, new byte[] { 0x00, 0x01 });

			TLV screenNameTlv = new TLV();
			screenNameTlv.type = ICQConstants.TLV_SCREENNAME;
			screenNameTlv.value = service.getUn().getBytes();
			tlvs[1] = screenNameTlv;

			tlvs[2] = new TLV(ICQConstants.TLV_NEWPASSWORD, roastPw(service.getPw()));

			tlvs[3] = new TLV(ICQConstants.TLV_CLIENTIDENTITY, new String("Ace IM").getBytes());

			byte[] clientid = { (byte) 0x88, (byte) 0x88 };
			tlvs[4] = new TLV(ICQConstants.TLV_CLIENTID, clientid);
		} else {
			tlvs = new TLV[1];
			tlvs[0] = new TLV((short) 0x0, new byte[] { 0x00, 0x01 });
		}

		flap.tlvData = tlvs;

		return flap;
	}

	private Flap createMD5Request() throws ICQException {
		service.log("--- md5 auth request");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_AUTHENTICATE;
		data.subtypeId = ICQConstants.SNAC_AUTHENTICATE_MD5AUTHKEYREQ;
		data.hFlag = (byte) 0x0;
		data.lFlag = (byte) 0x0;
		data.requestId = 0;

		byte[] name;
		try {
			name = service.getUn().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			name = service.getUn().getBytes();
		}
		TLV screenname = new TLV();
		screenname.type = 0x01;
		screenname.value = name;
		TLV tlv1 = new TLV();
		tlv1.type = (short) 0x004b;

		TLV[] tlvs = new TLV[] { screenname, tlv1 };
		data.data = tlvs;
		flap.data = data;
		return flap;
	}

	private byte[] roastPw(String pws) throws ICQException {
		byte[] pw = pws.getBytes();
		byte[] pwhash = new byte[pw.length];
		
		int ln;
		
		if (pw.length > MAX_PASSWORD_LEN) {
			ln = MAX_PASSWORD_LEN;
		} else {
			ln = pw.length;
		}
		for (int i = 0; i < ln; i++) {
			if (i + 1 <= pwRoastArray.length) {
				pwhash[i] = (byte) (pw[i] ^ pwRoastArray[i]);
			} else {
				pwhash[i] = pw[i];
			}
		}
		return pwhash;
	}

	@Override
	protected void internalFlapMap(Flap flap) throws ICQException {
		if (flap == null)
			return;
		switch (flap.channel) {
		case ICQConstants.FLAP_CHANNELL_START:
			if (authCookie != null) {
				service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 4);
				service.getRunnableService().sendToSocket(sendCookie());
			} else {
				task = startupTimer.schedule(expireTimerTask, 60, TimeUnit.SECONDS);
				service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 2);

				service.getRunnableService().sendToSocket(createPlainAuthRequest(!isSecureLogin));

				if (isSecureLogin) {
					service.getRunnableService().sendToSocket(createMD5Request());
				}
			}

			break;
		case ICQConstants.FLAP_CHANNELL_DATA:
			internalSnacMap(flap.data);
			break;
		case ICQConstants.FLAP_CHANNELL_CLOSE:
			/*
			 * if (isSecureLogin){ return; }
			 */
			if (flap.tlvData != null) {
				for (int i = 0; i < flap.tlvData.length; i++) {
					TLV tlv = flap.tlvData[i];
					internalTLVMap(tlv);
				}
			}
			tryConnectingBOS();
			break;
		}
	}

	private void tryConnectingBOS() {
		if (reconnectAddress != null) {
			service.log("reconnect to " + reconnectAddress);
			service.setCurrentState(ICQServiceInternal.STATE_CONNECTING_BOS);
		} else {
			deactivateTimer();
		}
		service.getRunnableService().disconnect();
		if (reconnectAddress != null) {
			service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 3);
			service.setCurrentState(ICQServiceInternal.STATE_AUTHENTICATING);
			service.runService(reconnectAddress, reconnectPort);

			permitList.clear();
			denyList.clear();
			ignoreList.clear();
		} else {
			deactivateTimer();
		}
	}

	@Override
	protected void internalTLVMap(TLV tlv) {
		if (tlv == null) {
			return;
		}
		switch (tlv.type) {
		case ICQConstants.TLV_EVIL:
			break;
		case ICQConstants.TLV_SCREENNAME:
			if (service.getOnlineInfo() != null) {
				service.getOnlineInfo().userClass = ProtocolUtils.bytes2ShortBE(tlv.value);
				service.log("    user class: " + service.getOnlineInfo().userClass);
			}
			break;
		case ICQConstants.TLV_ERRORDESCRIPTIONURL:
			String errorUrl = ProtocolUtils.getEncodedString(tlv.value);
			service.log(errorUrl);
			if (errorUrl.indexOf(ICQConstants.ERROR_BADCREDENTIALS) > -1) {
				service.lastConnectionError = ICQServiceInternal.ERROR_WRONG_PASSWORD;
			}
			break;
		case ICQConstants.TLV_RECONNECTADDRESS:
			if (reconnectAddress != null) {
				service.getOnlineInfo().memberSinceTime = ProtocolUtils.bytes2Date(tlv.value);
				service.log("    member since: " + service.getOnlineInfo().memberSinceTime);
			} else {
				reconnectAddress = new String(tlv.value).split(":")[0];
				reconnectPort = Integer.parseInt(new String(tlv.value).split(":")[1]);
				service.log("    reconnect: " + reconnectAddress + ":" + reconnectPort);
			}
			break;
		case ICQConstants.TLV_AUTHCOOKIE:
			authCookie = tlv.value;
			break;
		case ICQConstants.TLV_CLIENTIDENTITY:
			if (service.getOnlineInfo() != null) {
				service.getOnlineInfo().signonTime = ProtocolUtils.bytes2Date(tlv.value);
				service.log("    signon time: " + service.getOnlineInfo().signonTime);
			}
			break;
		case ICQConstants.TLV_PERSONALTEXT:
			/*
			 * if (service.getOnlineInfo()!=null){
			 * service.getOnlineInfo().setPersonalText
			 * (ProtocolUtils.bytes2ShortBE(tlv.value));
			 * service.log("    personal text: "
			 * +service.getOnlineInfo().getPersonalText()); }
			 */
			break;
		case ICQConstants.TLV_DCINFO:
			if (service.getOnlineInfo() != null) {
				try {
					service.getOnlineInfo().dcInfo = service.getOnlineInfoEngine().parseDCInfo(tlv.value);
					service.log("--- online info");
				} catch (ICQException e) {
					// to do
				}
			}
			break;
		case ICQConstants.TLV_RECONNECTHOST:
			if (service.getOnlineInfo() != null) {
				try {
					service.getOnlineInfo().extIP = ProtocolUtils.unsignedByte2Short(tlv.value[0]) + "." + ProtocolUtils.unsignedByte2Short(tlv.value[1]) + "." + ProtocolUtils.unsignedByte2Short(tlv.value[2]) + "."
							+ ProtocolUtils.unsignedByte2Short(tlv.value[3]);
				} catch (Exception e) {
					service.getOnlineInfo().extIP = "";
				}
				service.log("    external ip: " + service.getOnlineInfo().extIP);
			}
			break;
		case ICQConstants.TLV_CLIENTIDLE:
			if (service.getOnlineInfo() != null) {
				service.getOnlineInfo().onlineTime = ProtocolUtils.bytes2IntBE(tlv.value);
				service.log("    online since: " + service.getOnlineInfo().onlineTime);
			}
			break;
		case ICQConstants.TLV_DISTRIBUTIONNUMBER:
			if (service.getOnlineInfo() != null) {
				service.getOnlineInfo().distribNumber = tlv.value[0];
				service.log("    distribution: " + service.getOnlineInfo().distribNumber);
			}
			break;
		case ICQConstants.TLV_ERRORSUBCODE:
			if (tlv.value != null && tlv.value.length > 1 && tlv.value[1] == 0x18) {
				service.lastConnectionError = ICQServiceInternal.ERROR_RATE_LIMIT_EXCEEDED;
			}
			break;
		default:
			if (service.getOnlineInfo() != null) {
				service.getOnlineInfo().tlvs.add(tlv);
				service.log("    unknown tlv: " + ProtocolUtils.getSpacedHexString(ProtocolUtils.int2ByteBE(tlv.type)));
			}
			break;
		}

	}

	@Override
	protected void internalSnacMap(Snac snac) throws ICQException {
		if (snac == null)
			return;

		switch (snac.getServiceId()) {
		case ICQConstants.SNAC_FAMILY_GENERIC:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_GENERIC_SERVERSUPPORTEDFAMILIES:
				service.setServerSupportedFamilies(ProtocolUtils.byteArrayToShortArrayBE(snac.plainData));
				service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 5);
				service.getRunnableService().sendToSocket(sendClientFamiliesVersions());
				break;
			case ICQConstants.SNAC_GENERIC_RATELIMITINFORES:
				parseRateLimits(snac);

				Flap clRequest;
				if (service.getBuddyList().itemNumber > 0 && service.getBuddyList().lastUpdateTime != null) {
					clRequest = requestContactListUpdate(service.getBuddyList().lastUpdateTime, service.getBuddyList().itemNumber);
				} else {
					clRequest = requestContactList();
				}

				Flap[] flaps = new Flap[] { clRequest, sendRateLimitsAck(), requestOnlineInfo(), requestSSIServiceLimits(), requestLocationServiceLimits(), requestBuddylistServiceLimits(), requestMessagingServiceLimits(),
						requestPrivacyServiceLimits() };
				service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 7);
				service.getRunnableService().sendMultipleToSocket(flaps);
				break;
			case ICQConstants.SNAC_GENERIC_OWNINFORES:
				service.getOnlineInfoEngine().parseOnlineInfo(snac, service.getOnlineInfo());

				service.getServiceResponse().respond(ICQServiceResponse.RES_ACCOUNTUPDATED, service.getOnlineInfo());
				if (service.getCurrentState() == ICQServiceInternal.STATE_AUTHENTICATING && endLogin) {
					service.getServiceResponse().respond(ICQServiceResponse.RES_KEEPALIVE);
					service.startMainProcessor();
					deactivateTimer();
				}
				break;
			case ICQConstants.SNAC_GENERIC_SERVERSERVICESVERSIONRES:
				service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 6);
				service.getRunnableService().sendToSocket(requestRateLimits());
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_LOCATION:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_LOCATION_PARAMRES:
				try {
					snac.data = service.getDataParser().parseTLV(snac.plainData);
				} catch (ICQException e) {
					service.log(e);
				}
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_BUDDYLISTMGMT:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_BUDDYLISTMGMT_PARAMRES:
				try {
					snac.data = service.getDataParser().parseTLV(snac.plainData);
				} catch (ICQException e) {
					service.log(e);
				}
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_MESSAGING:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_MESSAGING_PARAMRES:
				service.setMessageParams(parseICBMParams(snac.plainData));
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_PRIVACYMGMT:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_PRIVACYMGMT_PARAMRES:
				parsePrivacyParams(snac.plainData);
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_SERVERSIDEINFO:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_SERVERSIDEINFO_PARAMRES:
				parseSSIParams(snac);
				break;
			case ICQConstants.SNAC_SERVERSIDEINFO_CLRES:
				parseBuddyList(snac.plainData, snac.lFlag == 1);
			case ICQConstants.SNAC_SERVERSIDEINFO_LOCALSSIUPTODATE:
				Flap[] flaps = new Flap[] { sendCapabilities(), sendICBMParameters(), sendSSIActivate() };
				service.getServiceResponse().respond(ICQServiceResponse.RES_CONNECTING, 9);
				service.getRunnableService().sendMultipleToSocket(flaps);
				endLogin = true;
				break;
			}
			break;
		case ICQConstants.SNAC_FAMILY_AUTHENTICATE:
			switch (snac.subtypeId) {
			case ICQConstants.SNAC_AUTHENTICATE_MD5AUTHKEYRES:
				parseMD5Key(snac.plainData);
				break;
			case ICQConstants.SNAC_AUTHENTICATE_LOGINRES:
				snac.data = service.getDataParser().parseTLV(snac.plainData);
				parseMD5LoginResult(snac);
				break;
			}
			break;
		}
	}

	private void parseMD5LoginResult(Snac snac) {
		if (snac == null || snac.data == null) {
			service.lastConnectionError = ICQServiceInternal.ERROR_WRONG_PASSWORD;
			service.getRunnableService().disconnect();
			return;
		}

		for (TLV tlv : snac.data) {
			internalTLVMap(tlv);
		}

		// tryConnectingBOS();
	}

	private void parseMD5Key(byte[] data) {
		int keyLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(data, 0));
		byte[] key = new byte[keyLength];
		System.arraycopy(data, 2, key, 0, (int) keyLength);

		proceedMD5Login(key);
	}

	private void proceedMD5Login(byte[] key) {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_AUTHENTICATE;
		data.subtypeId = ICQConstants.SNAC_AUTHENTICATE_LOGINREQ;
		data.requestId = ICQConstants.SNAC_AUTHENTICATE_LOGINREQ;

		TLV uinTlv = new TLV();
		uinTlv.type = 0x1;
		try {
			uinTlv.value = service.getUn().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			uinTlv.value = service.getUn().getBytes();
		}

		TLV pwTlv = new TLV();
		pwTlv.type = 0x25;

		byte[] pwHash = new MD5().calculate(service.getPw().getBytes());

		byte[] aimHash = AIM_MD5_STRING.getBytes();

		byte[] md5buf = new byte[key.length + pwHash.length + aimHash.length];
		int pos = 0;
		System.arraycopy(key, 0, md5buf, pos, key.length);
		pos += key.length;
		System.arraycopy(pwHash, 0, md5buf, pos, pwHash.length);
		pos += pwHash.length;
		System.arraycopy(aimHash, 0, md5buf, pos, aimHash.length);

		pwTlv.value = new MD5().calculate(md5buf);

		TLV unk = new TLV((short) 0x4c, new byte[0]);

		data.data = new TLV[] { uinTlv, pwTlv, unk };
		flap.data = data;

		service.getRunnableService().sendToSocket(flap);
	}

	private Flap sendSSIActivate() throws ICQException {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_SERVERCLLOAD;
		data.requestId = ICQConstants.SNAC_SERVERSIDEINFO_SERVERCLLOAD;
		flap.data = data;

		return flap;
	}

	private Flap sendICBMParameters() throws ICQException {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_PARAMSET;
		data.requestId = ICQConstants.SNAC_MESSAGING_PARAMSET;

		byte[] plainData = new byte[] { 0, 0, 0, 0, 0, 0xb, 0x1f, 0x40, 3, (byte) 0xe7, 3, (byte) 0xe7, 0, 0, 0, 0 };// TODO
																														// fix!
		data.plainData = plainData;
		flap.data = data;

		return flap;
	}

	private Flap sendCapabilities() throws ICQException {
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_LOCATION;
		data.subtypeId = ICQConstants.SNAC_LOCATION_USERINFOSET;
		data.requestId = ICQConstants.SNAC_LOCATION_USERINFOSET;

		TLV clsids = new TLV();
		clsids.type = 0x5;

		byte[] rawData = new byte[16 * 10 + ((service.getOnlineInfo().qipStatus != null) ? 16 : 0)];
		int pos = 0;
		System.arraycopy(ICQConstants.CLSID_UTF, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_DIRECT, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_ICQUTF, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_SRV_RELAY, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_CLIENTINFOPREFIX, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_TYPING, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_AIM_FILERECEIVE, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_AIM_FILESEND, 0, rawData, pos, 16);
		pos += 16;
		System.arraycopy(ICQConstants.CLSID_ASIA, 0, rawData, pos, 16);
		pos += 16;
		/*
		 * System.arraycopy(new String("Asia 0.5.0.4.001").getBytes(), 0,
		 * rawData, pos, 16); pos+=16;
		 */
		System.arraycopy(ICQConstants.CLSID_XTRAZ, 0, rawData, pos, 16);
		pos += 16;
		if (service.getOnlineInfo().qipStatus != null) {
			System.arraycopy(service.getOnlineInfo().qipStatus, 0, rawData, pos, 16);
			pos += 16;
		}

		clsids.value = rawData;
		data.data = new TLV[] { clsids };
		flap.data = data;

		return flap;
	}

	private void parseSSIParams(Snac snac) throws ICQException {
		try {
			service.setSSILimits(service.getDataParser().parseTLV(snac.plainData));
		} catch (ICQException e) {
			service.log(e);
		}

	}

	private void parsePrivacyParams(byte[] in) throws ICQException {
		try {
			TLV[] tlvs = service.getDataParser().parseTLV(in);
			for (int i = 0; i < tlvs.length; i++) {
				if (tlvs[i].type == 0x01) {
					service.setMaxVisibleListLength(ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(tlvs[i].value)));
					service.log("   max visible items:" + service.getMaxVisibleListLength());
				}
				if (tlvs[i].type == 0x02) {
					service.setMaxInvisibleListLength(ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(tlvs[i].value)));
					service.log("   max invisible items:" + service.getMaxInvisibleListLength());
				}
			}
		} catch (ICQException e) {
			service.log(e);
		}
	}

	private ICBMParams parseICBMParams(byte[] plainData) throws ICQException {
		if (plainData == null || plainData.length < 16)
			return null;

		ICBMParams params = new ICBMParams();
		params.channel = ProtocolUtils.bytes2ShortBE(plainData, 0);
		params.flags = ProtocolUtils.bytes2IntBE(plainData, 2);
		params.maxMessageSnacLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(plainData, 6));
		params.maxSenderWarningLevel = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, 8));
		params.maxReceiverWarningLevel = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, 10));
		params.minimumMessageInterval = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(plainData, 12));
		params.smth = ProtocolUtils.bytes2ShortBE(plainData, 14);

		service.log(params + "");

		return params;
	}

	private Flap requestOnlineInfo() throws ICQException {
		service.log("--- request online info");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_GENERIC;
		data.subtypeId = ICQConstants.SNAC_GENERIC_OWNINFOREQ;
		data.requestId = ICQConstants.SNAC_GENERIC_OWNINFOREQ;

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestSSIServiceLimits() throws ICQException {
		service.log("--- request SSI limits");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_PARAMREQ;
		data.requestId = ICQConstants.SNAC_SERVERSIDEINFO_PARAMREQ;

		TLV tlv = new TLV();
		tlv.type = (short) 0x0b;
		tlv.value = new byte[] { 0x00, 0x0f };
		data.data = new TLV[] { tlv };

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestContactListUpdate(Date lastUpdate, int itemNumber) {
		service.log("--- request contact list update");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_CLUPD;
		data.requestId = ICQConstants.SNAC_SERVERSIDEINFO_CLUPD;

		byte[] cldata = new byte[6];
		int updateSeconds = (int) (lastUpdate.getTime() / 1000);

		byte[] buffer = new byte[4];
		buffer = ProtocolUtils.int2ByteBE(updateSeconds);
		System.arraycopy(buffer, 0, cldata, 0, 4);
		buffer = ProtocolUtils.short2ByteBE((short) itemNumber);
		System.arraycopy(buffer, 0, cldata, 4, 2);
		data.plainData = cldata;

		flap.data = data;
		return flap;
	}

	private Flap requestContactList() throws ICQException {
		service.log("--- request contact list");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_SERVERSIDEINFO;
		data.subtypeId = ICQConstants.SNAC_SERVERSIDEINFO_CLREQ;
		data.requestId = ICQConstants.SNAC_SERVERSIDEINFO_CLREQ;

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestLocationServiceLimits() throws ICQException {
		service.log("--- request location service limits");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_LOCATION;
		data.subtypeId = ICQConstants.SNAC_LOCATION_PARAMREQ;
		data.requestId = ICQConstants.SNAC_LOCATION_PARAMREQ;

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestBuddylistServiceLimits() throws ICQException {
		service.log("--- request buddylist service limits");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_BUDDYLISTMGMT;
		data.subtypeId = ICQConstants.SNAC_BUDDYLISTMGMT_PARAMREQ;
		data.requestId = ICQConstants.SNAC_BUDDYLISTMGMT_PARAMREQ;

		TLV tlv = new TLV();
		tlv.type = (short) 0x05;
		tlv.value = new byte[] { 0x00, 0x03 };
		data.data = new TLV[] { tlv };

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestMessagingServiceLimits() throws ICQException {
		service.log("--- request messaging service limits");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_MESSAGING;
		data.subtypeId = ICQConstants.SNAC_MESSAGING_PARAMREQ;
		data.requestId = ICQConstants.SNAC_MESSAGING_PARAMREQ;

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestPrivacyServiceLimits() throws ICQException {
		service.log("--- request privacy service limits");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_PRIVACYMGMT;
		data.subtypeId = ICQConstants.SNAC_PRIVACYMGMT_PARAMREQ;
		data.requestId = ICQConstants.SNAC_PRIVACYMGMT_PARAMREQ;

		flap.data = data;
		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap sendClientFamiliesVersions() throws ICQException {
		service.log("--- send client family versions");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_GENERIC;
		data.subtypeId = ICQConstants.SNAC_GENERIC_SERVERSERVICESVERSIONREQ;
		data.requestId = ICQConstants.SNAC_GENERIC_SERVERSERVICESVERSIONREQ;
		data.plainData = ICQConstants.LOCAL_FAMILIES_VERSIONS;

		flap.data = data;

		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private Flap requestRateLimits() throws ICQException {
		service.log("--- request rate limits");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_GENERIC;
		data.subtypeId = ICQConstants.SNAC_GENERIC_RATELIMITINFOREQ;
		data.requestId = ICQConstants.SNAC_GENERIC_RATELIMITINFOREQ;

		flap.data = data;

		return flap;
		// service.getRunnableService().sendToSocket(flap);
	}

	private void parseRateLimits(Snac snac) throws ICQException {
		int rateClassCount = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(snac.plainData));
		RateLimit[] limits = new RateLimit[rateClassCount];
		int pos = 2;
		for (int i = 0; i < rateClassCount; i++) {
			byte[] buffer = new byte[4];
			System.arraycopy(snac.plainData, pos, buffer, 0, 2);
			short classId = ProtocolUtils.bytes2ShortBE(buffer);

			pos += 2;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int windowSize = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int clearLevel = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int alertLevel = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int limitLevel = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int disconnectLevel = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int currentLevel = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int maxLevel = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			System.arraycopy(snac.plainData, pos, buffer, 0, 4);
			int lastTime = ProtocolUtils.bytes2IntBE(buffer);

			pos += 4;
			byte currentState = snac.plainData[pos];

			pos++;

			RateLimit limit = new RateLimit(classId, windowSize, clearLevel, alertLevel, limitLevel, disconnectLevel, currentLevel, maxLevel, lastTime, currentState);
			limits[i] = limit;
		}

		// List<RateGroup> groups = new LinkedList<RateGroup>();
		// while(pos<snac.plainData.length){
		for (int i = 0; i < rateClassCount; i++) {
			byte[] buffer = new byte[4];

			System.arraycopy(snac.plainData, pos, buffer, 0, 2);
			short groupId = ProtocolUtils.bytes2ShortBE(buffer);

			pos += 2;
			System.arraycopy(snac.plainData, pos, buffer, 0, 2);
			int familyCount = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortBE(buffer));

			pos += 2;
			int[] families = new int[familyCount];
			for (int j = 0; j < familyCount; j++) {
				System.arraycopy(snac.plainData, pos, buffer, 0, 4);
				families[j] = ProtocolUtils.bytes2IntBE(buffer);
				pos += 4;
			}

			RateGroup rateGroup = new RateGroup(groupId, families);
			limits[i].rateGroup = rateGroup;
			// groups.add(rateGroup);
		}

		service.setRateLimits(limits);
	}

	private Flap sendRateLimitsAck() throws ICQException {
		service.log("--- send rate limits ack");

		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;

		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_GENERIC;
		data.subtypeId = ICQConstants.SNAC_GENERIC_RATELIMITGROUPADD;
		data.requestId = ICQConstants.SNAC_GENERIC_RATELIMITGROUPADD;

		byte[] rateIds = new byte[service.getRateLimits().length * 2];
		for (int i = 0; i < service.getRateLimits().length; i++) {
			byte[] id = ProtocolUtils.short2ByteBE(service.getRateLimits()[i].rateGroup.id);
			System.arraycopy(id, 0, rateIds, i * 2, 2);
		}
		data.plainData = rateIds;

		flap.data = data;

		return flap;
	}

	public void deactivateTimer() {

		service.log("deactivate auth timer");
		expireTimerTask.disconnect = false;
		if (startupTimer != null && task != null) {
			task.cancel(false);
		} else {
			service.log("deactivate auth timer failed");
		}
	}

	class AuthExpireTimerTask implements Runnable {

		public volatile boolean disconnect = true;

		@Override
		public void run() {
			if (disconnect && (service.getProcessor() == null || service.getProcessor() instanceof AuthenticationProcessor)) {
				service.log("auth timeout");
				authCookie = null;
				service.getRunnableService().disconnect();
			}
		}
	}

	@Override
	public void onDisconnect() {
		deactivateTimer();
	}
}
