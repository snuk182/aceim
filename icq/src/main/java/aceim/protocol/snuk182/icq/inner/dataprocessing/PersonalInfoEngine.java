package aceim.protocol.snuk182.icq.inner.dataprocessing;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.protocol.snuk182.icq.inner.ICQConstants;
import aceim.protocol.snuk182.icq.inner.ICQServiceInternal;
import aceim.protocol.snuk182.icq.inner.ICQServiceResponse;
import aceim.protocol.snuk182.icq.inner.dataentity.Flap;
import aceim.protocol.snuk182.icq.inner.dataentity.ICQPersonalInfo;
import aceim.protocol.snuk182.icq.inner.dataentity.Snac;
import aceim.protocol.snuk182.icq.inner.dataentity.TLV;
import aceim.protocol.snuk182.icq.utils.ProtocolUtils;


public class PersonalInfoEngine {
	private static final String CODEPAGE_WIN1251 = "Cp1251";
	private static final DateFormat dateformat = DateFormat.getDateInstance();

	public final Map<Byte, String> metaInfoRequestMap = Collections.synchronizedMap(new HashMap<Byte, String>());
	private final Map<String, Boolean> fullRequestMap = Collections.synchronizedMap(new HashMap<String, Boolean>());
	
	private ICQServiceInternal service;
	
	private ICQPersonalInfo recentInfo = new ICQPersonalInfo();
	
	public PersonalInfoEngine(ICQServiceInternal icqServiceInternal){
		this.service = icqServiceInternal;
	}
	
	public void parsePersonalInfoResponse(String uin, byte[] response, boolean last){
		short subtype = ProtocolUtils.bytes2ShortLE(response, 0);
		
		switch(subtype){
		case 0x104:
			parseShortPersonalInfo(uin, response, recentInfo);
			break;
		case 0x1ae:
			parseLastUserFound(uin, response, true);
			return;
		case 0xc8:
			parseBasicPersonalInfo(uin, response, recentInfo);
			break;
		case 0xeb:
			parseEmailsInfo(uin, response, recentInfo);
			break;
		case 0xd2:
			parseWorkInfo(uin, response, recentInfo);
			break;
		case 0xf0:
			parseInterestsInfo(uin, response, recentInfo);
			break;
		case 0xdc:
			parseMoreUserinfo(uin, response, recentInfo);
			break;
		case 0x10e:
			break;
		case 0xe6:
			parseNotesInfo(uin, response, recentInfo);
			break;
		case 0xfa:
			parseAffiliations(uin, response, recentInfo);
			break;
		}
		if (last){
			boolean isFull = fullRequestMap.remove(recentInfo.uin);
			service.getServiceResponse().respond(ICQServiceResponse.RES_USERINFO, recentInfo, isFull);
		}
	}

	private void parseAffiliations(String uin, byte[] response, ICQPersonalInfo info) {
		int pos = 3;
		
		int pastsCount = response[pos];
		pos++;
		
		while(pastsCount > 0){
			int past = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
			pos+=2;
			
			int noteLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
			pos+=2;
			
			String note;
			try {
				note = new String(response, pos, noteLength-1, CODEPAGE_WIN1251);
			} catch (UnsupportedEncodingException e) {
				note = new String(response, pos, noteLength-1);
			}
			pos += noteLength;
			
			if (past > 0){
				info.params.put("past "+past, note);
			}
			pastsCount--;
		}
		
		int affCount = response[pos];
		pos++;
		
		while(affCount > 0){
			int aff = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
			pos+=2;
			
			int noteLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
			pos+=2;
			
			String note;
			try {
				note = new String(response, pos, noteLength-1, CODEPAGE_WIN1251);
			} catch (UnsupportedEncodingException e) {
				note = new String(response, pos, noteLength-1);
			}
			pos += noteLength;
			
			if (aff > 0){
				info.params.put("affiliation "+aff, note);
			}
			affCount--;
		}
		
		service.log(info.params.toString());
	}

	private void parseMoreUserinfo(String uin, byte[] response, ICQPersonalInfo info) {
		int pos = 3;
		
		int age = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		byte gender = response[pos];
		pos++;
		
		int homepageLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String homepage;
		try {
			homepage = new String(response, pos, homepageLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			homepage = new String(response, pos, homepageLength-1);
		}
		
		pos+=homepageLength;
		
		int year = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos))-1900;
		pos+=2;
		byte month = response[pos];
		pos++;
		byte day = response[pos];
		pos++;
		
		int lang1 = response[pos];
		pos++;
		int lang2 = response[pos];
		pos++;
		int lang3 = response[pos];
		pos++;
		
		short maritalStatus = ProtocolUtils.bytes2ShortLE(response, pos);
		pos+=2;
		
		int fromCityLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String fromCity;
		try {
			fromCity = new String(response, pos, fromCityLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			fromCity = new String(response, pos, fromCityLength-1);
		}
		
		pos+=fromCityLength;
		
		int fromStateLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String fromState;
		try {
			fromState = new String(response, pos, fromStateLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			fromState = new String(response, pos, fromStateLength-1);
		}
		
		pos+=fromStateLength;
		
		int countryCode = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		
		int timezone = response[pos];
		
		if (age > 0){
			info.age = (short) age;
		}
		
		if (countryCode > 0){
			info.params.put("From country", countryCode);
		}
		
		if (fromState.length() > 0){
			info.params.put("From state", fromState);
		}
		
		if (fromCity.length() > 0){
			info.params.put("From city", fromCity);
		}
		
		if (gender > 0){
			info.gender = gender;
		}
		
		info.params.put("From GMT", timezone);
		
		if (homepage.length() > 0){
			info.params.put("Homepage", homepage);
		}
		
		if (year >0 && month>0 && day > 0){
			Calendar birth = Calendar.getInstance();
			birth.set(year, month, day);
			info.params.put("Birthday", dateformat.format(birth.getTime()));
		}
		
		if (lang1 > 0){
			info.params.put("Language #1", lang1);
		}
		if (lang2 > 0){
			info.params.put("Language #2", lang2);
		}
		if (lang3 > 0){
			info.params.put("Language #3", lang3);
		}
		
		if (maritalStatus > 0){
			info.params.put("Family status", maritalStatus);
		}
		
		service.log(info.params.toString());
	}

	@SuppressWarnings("unused")
	private void parseInterestsInfo(String uin, byte[] response, ICQPersonalInfo info) {
		int pos = 3;
		int interestsCount = response[pos];
		pos++;
		
		StringBuffer interests = new StringBuffer();
		while (interestsCount > 0){
			byte[] occupationCodeBytes = new byte[2];
			System.arraycopy(response, pos, occupationCodeBytes, 0, 2);
			String occupationCode = new String(occupationCodeBytes);
			pos+=2;
			
			int noteLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
			pos+=2;
			
			String note;
			try {
				note = new String(response, pos, noteLength-1, CODEPAGE_WIN1251);
			} catch (UnsupportedEncodingException e) {
				note = new String(response, pos, noteLength-1);
			}
			interests.append(note);
			pos += noteLength;
			
			if (interestsCount > 1){
				interests.append(", ");
			}
			interestsCount--;
		}
		
		if (interests.length() > 0){
			info.params.put("Interests", interests.toString());
		}
		
		service.log(info.params.toString());
	}

	private void parseWorkInfo(String uin, byte[] response, ICQPersonalInfo info) {
		int pos = 3;
		
		int workCityLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workCity;
		try {
			workCity = new String(response, pos, workCityLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workCity = new String(response, pos, workCityLength-1);
		}
		
		pos+=workCityLength;
		
		int workStateLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workState;
		try {
			workState = new String(response, pos, workStateLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workState = new String(response, pos, workStateLength-1);
		}
		
		pos+=workStateLength;
		
		int workPhoneLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workPhone;
		try {
			workPhone = new String(response, pos, workPhoneLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workPhone = new String(response, pos, workPhoneLength-1);
		}
		
		pos+=workPhoneLength;
		
		int workFaxLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workFax;
		try {
			workFax = new String(response, pos, workFaxLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workFax = new String(response, pos, workFaxLength-1);
		}
		
		pos+=workFaxLength;
		
		int workAddLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workAdd;
		try {
			workAdd = new String(response, pos, workAddLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workAdd = new String(response, pos, workAddLength-1);
		}
		
		pos+=workAddLength;
		
		int workZipLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workZip;
		try {
			workZip = new String(response, pos, workZipLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workZip = new String(response, pos, workZipLength-1);
		}
		
		pos+=workZipLength;
		
		int countryCode = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		
		int workCompanyLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workCompany;
		try {
			workCompany = new String(response, pos, workCompanyLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workCompany = new String(response, pos, workCompanyLength-1);
		}
		
		pos+=workCompanyLength;
		
		int workDepartmentLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workDepartment;
		try {
			workDepartment = new String(response, pos, workDepartmentLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workDepartment = new String(response, pos, workDepartmentLength-1);
		}
		
		pos+=workDepartmentLength;
		
		int workPositionLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workPosition;
		try {
			workPosition = new String(response, pos, workPositionLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workPosition = new String(response, pos, workPositionLength-1);
		}
		
		pos+=workPositionLength;
		
		int occupationCode = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		
		int workWebpageLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String workWebpage;
		try {
			workWebpage = new String(response, pos, workWebpageLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			workWebpage = new String(response, pos, workWebpageLength-1);
		}
		
		pos+=workWebpageLength;
		
		if (workAdd.length() > 0){
			info.params.put("Work address", workAdd);
		}
		if (workCity.length() > 0){
			info.params.put("Work city", workCity);
		}
		if (workCompany.length() > 0){
			info.params.put("Work company", workCompany);
		}
		if (workDepartment.length() > 0){
			info.params.put("Work department", workDepartment);
		}
		if (workFax.length() > 0){
			info.params.put("Work fax", workFax);
		}
		if (workPhone.length() > 0){
			info.params.put("Work phone", workPhone);
		}
		if (workPosition.length() > 0){
			info.params.put("Work position", workPosition);
		}
		if (workState.length() > 0){
			info.params.put("Work state", workState);
		}
		if (workWebpage.length() > 0){
			info.params.put("Work webpage", workWebpage);
		}
		if (workZip.length() > 0){
			info.params.put("Work ZIP/post code", workZip);
		}
		if (occupationCode > 0){
			info.params.put("Work occupation", occupationCode);
		}
		if (countryCode > 0){
			info.params.put("Work country", countryCode);
		}
		
		service.log(info.params.toString());
	}

	private void parseEmailsInfo(String uin, byte[] response, ICQPersonalInfo info) {
		int pos = 3;
		int emailsCount = response[pos];
		pos++;
		
		StringBuffer otherEmails = new StringBuffer();
		while (emailsCount > 0){
			int noteLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
			pos+=2;
			String note;
			try {
				note = new String(response, pos, noteLength-1, CODEPAGE_WIN1251);
			} catch (UnsupportedEncodingException e) {
				note = new String(response, pos, noteLength-1);
			}
			otherEmails.append(note);
			pos += noteLength;
			
			if (emailsCount > 1){
				otherEmails.append(", ");
			}
			emailsCount--;
		}
		
		if (otherEmails.length() > 0){
			info.params.put("Emails", otherEmails.toString());
		}
		
		service.log(info.params.toString());
	}

	private void parseNotesInfo(String uin, byte[] response, ICQPersonalInfo info) {
		int pos = 3;
		int noteLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String note;
		try {
			note = new String(response, pos, noteLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			note = new String(response, pos, noteLength-1);
		}
		
		if (note.length() > 0){
			info.params.put("Note", note);
		}
		
		service.log(info.params.toString());
	}

	private void parseBasicPersonalInfo(String uin, byte[] response, ICQPersonalInfo info) {
		
		int pos = 3;
		int nickLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String nick;
		try {
			nick = new String(response, pos, nickLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			nick = new String(response, pos, nickLength-1);
		}
		
		pos+=nickLength;
		
		int fnameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String fname;
		try {
			fname = new String(response, pos, fnameLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			fname = new String(response, pos, fnameLength-1);
		}
		
		pos+=fnameLength;
		
		int lnameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String lname;
		try {
			lname = new String(response, pos, lnameLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			lname = new String(response, pos, lnameLength-1);
		}
		
		pos+=lnameLength;
		
		int emailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String email;
		try {
			email = new String(response, pos, emailLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			email = new String(response, pos, emailLength-1);
		}
		
		pos+=emailLength;
		
		int cityLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String city;
		try {
			city = new String(response, pos, cityLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			city = new String(response, pos, cityLength-1);
		}
		
		pos+=cityLength;
		
		int stateLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String state;
		try {
			state = new String(response, pos, stateLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			state = new String(response, pos, stateLength-1);
		}
		
		pos+=stateLength;
		
		int phoneLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String phone;
		try {
			phone = new String(response, pos, phoneLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			phone = new String(response, pos, phoneLength-1);
		}
		
		pos+=phoneLength;
		
		int faxLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String fax;
		try {
			fax = new String(response, pos, faxLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			fax = new String(response, pos, faxLength-1);
		}
		
		pos+=faxLength;
		
		int addrLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String addr;
		try {
			addr = new String(response, pos, addrLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			addr = new String(response, pos, addrLength-1);
		}
		
		pos+=addrLength;
		
		int mobileLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String mobile;
		try {
			mobile = new String(response, pos, mobileLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			mobile = new String(response, pos, mobileLength-1);
		}
		
		pos+=mobileLength;
		
		int zipLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String zip;
		try {
			zip = new String(response, pos, zipLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			zip = new String(response, pos, zipLength-1);
		}
		
		pos+=zipLength;
		
		int countryCode = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		
		int gmt = response[pos];	
		
		info.uin = uin;
		info.nickname = nick;
		info.email = email;
		info.firstName = fname;
		info.lastName = lname;
		
		if (city.length() > 0){
			info.params.put("City", city);
		}
		if (phone.length() > 0){
			info.params.put("Home phone", phone);
		}
		if (mobile.length() > 0){
			info.params.put("Mobile phone", mobile);			
		}
		if (state.length() > 0){
			info.params.put("State", state);			
		}
		if (fax.length() > 0){
			info.params.put("Fax", fax);
		}
		if (addr.length() > 0 ){
			info.params.put("Address", addr);
		}
		if (countryCode > 0){
			info.params.put("Country", countryCode);
		}
		if (zip.length() > 0){
			info.params.put("ZIP code", zip);
		}
		info.params.put("GMT", gmt);
		
		service.log(info.params.toString());
	}

	private void parseLastUserFound(String uin, byte[] response, boolean isLast) {
		List<ICQPersonalInfo> infos = new ArrayList<ICQPersonalInfo>(1);
		if (response[2]!=0xa){
			service.getServiceResponse().respond(ICQServiceResponse.RES_SEARCHRESULT, infos);
		}
		int pos = 5; //omit 2 bytes of length
		uin = ProtocolUtils.unsignedInt2Long(ProtocolUtils.bytes2IntLE(response, pos))+"";
		pos+=4;
		
		int nickLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String nick;
		try {
			nick = new String(response, pos, nickLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			nick = new String(response, pos, nickLength-1);
		}
		pos+=nickLength;
		
		int fnameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String fname;
		try {
			fname = new String(response, pos, fnameLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			fname = new String(response, pos, fnameLength-1);
		}
		pos+=fnameLength;
		
		int lnameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String lname;
		try {
			lname = new String(response, pos, lnameLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			lname = new String(response, pos, lnameLength-1);
		}		
		pos+=lnameLength;
		
		int emailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String email;
		try {
			email = new String(response, pos, emailLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			email = new String(response, pos, emailLength-1);
		}
		pos+=emailLength;
		
		byte authRequired = response[pos];
		pos++;
		
		short onlineStatus = ProtocolUtils.bytes2ShortLE(response, pos);
		pos+=2;
		
		byte gender = response[pos];
		pos++;
		
		short age = ProtocolUtils.bytes2ShortLE(response, pos);
		
		ICQPersonalInfo info = new ICQPersonalInfo();
		info.uin = uin;
		info.nickname = nick;
		info.email = email;
		info.firstName = fname;
		info.lastName = lname;
		info.age = age;
		info.authRequired = authRequired;
		info.gender = gender;
		info.status = onlineStatus;
		
		infos.add(info);
		service.getServiceResponse().respond(ICQServiceResponse.RES_SEARCHRESULT, infos);
	}

	private void parseShortPersonalInfo(String uin, byte[] response, ICQPersonalInfo info) {
		if (response[2]!=0xa){
			return;
		}
		int pos = 3;
		int nickLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String nick;
		try {
			nick = new String(response, pos, nickLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			nick = new String(response, pos, nickLength-1);
		}
		pos+=nickLength;
		
		int fnameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String fname;
		try {
			fname = new String(response, pos, fnameLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			fname = new String(response, pos, fnameLength-1);
		}
		pos+=fnameLength;
		
		int lnameLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String lname;
		try {
			lname = new String(response, pos, lnameLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			lname = new String(response, pos, lnameLength-1);
		}
		pos+=lnameLength;
		
		int emailLength = ProtocolUtils.unsignedShort2Int(ProtocolUtils.bytes2ShortLE(response, pos));
		pos+=2;
		String email;
		try {
			email = new String(response, pos, emailLength-1, CODEPAGE_WIN1251);
		} catch (UnsupportedEncodingException e) {
			email = new String(response, pos, emailLength-1);
		}
		pos+=emailLength;
		
		info.uin = uin;
		info.nickname = nick;
		info.email = email;
		info.firstName = fname;
		info.lastName = lname;
		
	}
	
	public void getShortPersonalMetainfo(String uin) {
		getPersonalMetaInfoInternal(uin, false);
	}
	
	private void getPersonalMetaInfoInternal(String uin, boolean isFull) {
		recentInfo = new ICQPersonalInfo();
		fullRequestMap.put(uin, isFull);
		service.getRunnableService().sendToSocket(getMetaInfoRequestFlap(uin, isFull));
	}
	
	private Flap getMetaInfoRequestFlap(String uin, boolean isFull){
		Flap flap1 = new Flap();
		flap1.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data1 = new Snac();
		data1.serviceId = ICQConstants.SNAC_FAMILY_ICQEXTENSION;
		data1.subtypeId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		data1.requestId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		
		TLV tlv1 = new TLV();
		tlv1.type = 0x1;
		
		byte[] tlvBytes = new byte[16];
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 14), 0, tlvBytes, 0, 2);
		
		String ownerUin = service.getUn();
		int ownerUinNumber = Integer.parseInt(ownerUin);
		System.arraycopy(ProtocolUtils.int2ByteLE(ownerUinNumber), 0, tlvBytes, 2, 4);
		
		byte counter = service.getIntCounter().byteValue();
		metaInfoRequestMap.put(new Byte(counter), uin);
		
		System.arraycopy(new byte[]{(byte) 0xd0, 0x07, 0x02, counter, isFull ? (byte) 0xb2 : (byte) 0xba, 0x4}, 0, tlvBytes, 6, 6);
		
		int uinNumber = Integer.parseInt(uin);
		System.arraycopy(ProtocolUtils.int2ByteLE(uinNumber), 0, tlvBytes, 12, 4);
		
		tlv1.value = tlvBytes;
		data1.data = new TLV[]{tlv1};
		
		flap1.data = data1;
		
		return flap1;
	}
	
	public void sendSearchByUinRequest(String uin){
		
		int numUin;
		try {
			numUin = Integer.parseInt(uin);
		} catch (NumberFormatException e) {
			service.getServiceResponse().respond(ICQServiceResponse.RES_NOTIFICATION, "UIN cannot be parsed");
			service.getServiceResponse().respond(ICQServiceResponse.RES_SEARCHRESULT, new ArrayList<ICQPersonalInfo>(0));
			return;
		}
		
		Flap flap = new Flap();
		flap.channel = ICQConstants.FLAP_CHANNELL_DATA;
		
		Snac data = new Snac();
		data.serviceId = ICQConstants.SNAC_FAMILY_ICQEXTENSION;
		data.subtypeId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		data.requestId = ICQConstants.SNAC_ICQEXTENSION_METAINFOREQ;
		
		TLV tlv = new TLV();
		tlv.type = ICQConstants.TLV_ICQEXTENSION_METADATA;
		byte[] value = new byte[20];
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 18), 0, value, 0, 2);
		System.arraycopy(ProtocolUtils.int2ByteLE(Integer.parseInt(service.getUn())), 0, value, 2, 4);
		System.arraycopy(ProtocolUtils.short2ByteLE(ICQConstants.ICQEXTENSION_COMMAND_METADATA_REQ), 0, value, 6, 2);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 2), 0, value, 8, 2);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) ICQConstants.ICQEXTENSION_SUBCOMMAND_SEARCH_BY_UIN_WITH_TLV), 0, value, 10, 2);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0x136), 0, value, 12, 2);
		System.arraycopy(ProtocolUtils.short2ByteLE((short) 0x9), 0, value, 14, 2);
		System.arraycopy(ProtocolUtils.int2ByteLE(numUin), 0, value, 16, 4);
		
		tlv.value = value;
		data.data = new TLV[]{tlv};
		flap.data = data;
		
		service.getRunnableService().sendToSocket(flap);
	}

	public void getFullPersonalMetainfo(String uin) {
		getPersonalMetaInfoInternal(uin, true);
	}
}
