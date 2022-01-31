/**
 * 
 */
package aceim.protocol.snuk182.icq.inner;

/**
 * @author Sergiy P
 *
 */
public final class ICQConstants {
	
	/** flap */
	
	public static final byte FLAP_ID = 0x2a;
	public static final byte FLAP_CHANNELL_START = 0x01;
	public static final byte FLAP_CHANNELL_DATA = 0x02;
	public static final byte FLAP_CHANNELL_ERROR = 0x03;
	public static final byte FLAP_CHANNELL_CLOSE = 0x04;
	public static final byte FLAP_CHANNELL_KEEPALIVE = 0x05;
	
	/** snac families
	 * 
 0x0001   	 Generic service controls 	Used in ICQ 	Used in AIM
 0x0002   	 Location services 	Used in ICQ 	Used in AIM
 0x0003   	 Buddy List management service 	Used in ICQ 	Used in AIM
 0x0004   	 ICBM (messages) service 	Used in ICQ 	Used in AIM
 0x0005   	 Advertisements service 	  	Used in AIM
 0x0006   	 Invitation service 	  	Used in AIM
 0x0007   	 Administrative service 	  	Used in AIM
 0x0008   	 Popup notices service 	  	Used in AIM
 0x0009   	 Privacy management service 	Used in ICQ 	Used in AIM
 0x000a   	 User lookup service (not used any more) 	  	
 0x000b   	 Usage stats service 	Used in ICQ 	Used in AIM
 0x000c   	 Translation service 	  	Used in AIM
 0x000d   	 Chat navigation service 	  	Used in AIM
 0x000e   	 Chat service 	  	Used in AIM
 0x000f   	 Directory user search 	  	Used in AIM
 0x0010   	 Server-stored buddy icons (SSBI) service 	Used in ICQ 	Used in AIM
 0x0013   	 Server Side Information (SSI) service 	Used in ICQ 	Used in AIM
 0x0015   	 ICQ specific extensions service 	Used in ICQ 	 
 0x0017   	 Authorization/registration service 	Used in ICQ 	Used in AIM
 0x0085   	 Broadcast service - IServerd extension 	Used in ICQ 	Used in AIM
	 */
	public static final short SNAC_FAMILY_GENERIC = 0x0001;
	public static final short SNAC_FAMILY_LOCATION = 0x0002;
	public static final short SNAC_FAMILY_BUDDYLISTMGMT = 0x0003;
	public static final short SNAC_FAMILY_MESSAGING = 0x0004;
	public static final short SNAC_FAMILY_ADVIRTISEMENT = 0x0005;
	public static final short SNAC_FAMILY_INVITATION = 0x0006;
	public static final short SNAC_FAMILY_ADMINISTRATION = 0x0007;
	public static final short SNAC_FAMILY_POPUP = 0x0008;
	public static final short SNAC_FAMILY_PRIVACYMGMT = 0x0009;
	public static final short SNAC_FAMILY_USERLOOKUP = 0x000a;
	public static final short SNAC_FAMILY_USAGESTAT = 0x000b;
	public static final short SNAC_FAMILY_TRANSLATION = 0x000c;
	public static final short SNAC_FAMILY_CHATNAVIGATE = 0x000d;
	public static final short SNAC_FAMILY_CHAT = 0x000e;
	public static final short SNAC_FAMILY_DIRUSERSEARCH = 0x000f;
	public static final short SNAC_FAMILY_SERVERSTOREDBUDDYICON = 0x0010;
	public static final short SNAC_FAMILY_SERVERSIDEINFO = 0x0013;
	public static final short SNAC_FAMILY_ICQEXTENSION = 0x0015;
	public static final short SNAC_FAMILY_AUTHENTICATE = 0x0017;
	public static final short SNAC_FAMILY_BROADCAST = 0x0085;
	
	/** snac GENERIC family
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Client is now online and ready for normal function
 0x0003   	 server  	 Server supported snac families list
 0x0004   	 client  	 Request for new service
 0x0005   	 server  	 Redirect (for 0x0004 subtype)
 0x0006   	 client  	 Request rate limits information
 0x0007   	 server  	 Rate limits information response
 0x0008   	 client  	 Add rate limits group
 0x0009   	 client  	 Delete rate limits group
 0x000a   	 server  	 Rate information changed / rate limit warning
 0x000b   	 server  	 Server pause command
 0x000c   	 client  	 Client pause ack
 0x000d   	 server  	 Server resume command
 0x000e   	 client  	 Request own online information
 0x000f   	 server  	 Requested online info response
 0x0010   	 server  	 Evil notification
 0x0011   	 client  	 Set idle time
 0x0012  	 server  	 Migration notice and info
 0x0013   	 server  	 Message of the day (MOTD)
 0x0014   	 client  	 Set privacy flags
 0x0015   	 server  	 Well known urls
 0x0016   	 server  	 No operation (NOP)
 0x0017   	 client  	 Request server services versions
 0x0018   	 server  	 Server services versions
 0x001e   	 client  	 Set status (set location info)
 0x001f   	 server  	 Client verification request
 0x0020   	 client  	 Client verification reply
 0x0021   	 server  	 Client's extended status from server
	 */
	public static final short SNAC_GENERIC_ERROR = 0x0001;
	public static final short SNAC_GENERIC_CLIENTONLINE = 0x0002;
	public static final short SNAC_GENERIC_SERVERSUPPORTEDFAMILIES = 0x0003;
	public static final short SNAC_GENERIC_NEWSERVICEREQUEST = 0x0004;
	public static final short SNAC_GENERIC_REDIRECT = 0x0005;
	public static final short SNAC_GENERIC_RATELIMITINFOREQ = 0x0006;
	public static final short SNAC_GENERIC_RATELIMITINFORES = 0x0007;
	public static final short SNAC_GENERIC_RATELIMITGROUPADD = 0x0008;
	public static final short SNAC_GENERIC_RATELIMITGROUPDELETE = 0x0009;
	public static final short SNAC_GENERIC_RATELIMITWARNING = 0x000a;
	public static final short SNAC_GENERIC_SERVERPAUSE = 0x000b;
	public static final short SNAC_GENERIC_SERVERPAUSERES = 0x000c;
	public static final short SNAC_GENERIC_SERVERRESUME = 0x000d;
	public static final short SNAC_GENERIC_OWNINFOREQ = 0x000e;
	public static final short SNAC_GENERIC_OWNINFORES = 0x000f;
	public static final short SNAC_GENERIC_BUGOGA = 0x0010;
	public static final short SNAC_GENERIC_IDLETIMESET = 0x0011;
	public static final short SNAC_GENERIC_MIGRATIONINFO = 0x0012;
	public static final short SNAC_GENERIC_MESSAGEOFTHEDAY = 0x0013;
	public static final short SNAC_GENERIC_PRIVACYFLAGSET = 0x0014;
	public static final short SNAC_GENERIC_WELLKNOWNURLS = 0x0015;
	public static final short SNAC_GENERIC_NOP = 0x0016;
	public static final short SNAC_GENERIC_SERVERSERVICESVERSIONREQ = 0x0017;
	public static final short SNAC_GENERIC_SERVERSERVICESVERSIONRES = 0x0018;
	public static final short SNAC_GENERIC_STATUSSET = 0x001e;
	public static final short SNAC_GENERIC_CLIENTVERIFYREQ = 0x001f;
	public static final short SNAC_GENERIC_CLIENTVERIFYRES = 0x0020;
	public static final short SNAC_GENERIC_EXTSTATUSRES = 0x0021;
	
	/** snac LOCATION family 
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Request limitations/params
 0x0003   	 server  	 Limitations/params response
 0x0004   	 client  	 Set user information
 0x0005   	 client  	 Request user info
 0x0006   	 server  	 User information response
 0x0007   	 client  	 Watcher sub request
 0x0008   	 server  	 Watcher notification
 0x0009   	 client  	 Update directory info request
 0x000A   	 server  	 Update directory info reply
 0x000B   	 client  	 Query for SNAC(02,0C)
 0x000C   	 server  	 Reply to SNAC(02,0B)
 0x000F   	 client  	 Update user directory interests
 0x0010   	 server  	 Update user directory interests reply
 0x0015   	 client  	 User info query
	 */
	
	public static final short SNAC_LOCATION_ERROR = 0x0001;
	public static final short SNAC_LOCATION_PARAMREQ = 0x0002;
	public static final short SNAC_LOCATION_PARAMRES = 0x0003;
	public static final short SNAC_LOCATION_USERINFOSET = 0x0004;
	public static final short SNAC_LOCATION_USERINFOREQ = 0x0005;
	public static final short SNAC_LOCATION_USERINFORES = 0x0006;
	public static final short SNAC_LOCATION_WATCHERREQ = 0x0007;
	public static final short SNAC_LOCATION_WATCHERRES = 0x0008;
	public static final short SNAC_LOCATION_UPDATEDIRINFOREQ = 0x0009;
	public static final short SNAC_LOCATION_UPDATEDIRINFORES = 0x000a;
	public static final short SNAC_LOCATION_XZREQ = 0x000b;
	public static final short SNAC_LOCATION_XZRES = 0x000c;
	public static final short SNAC_LOCATION_USERDIRINTERESTSET = 0x000f;
	public static final short SNAC_LOCATION_USERDIRINTERESTSETRES = 0x0010;
	public static final short SNAC_LOCATION_USERINFOQUERY = 0x0015;
	
	/** snac BUDDYLISTMGMT
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Request limitations/params
 0x0003   	 server  	 Limitations/params response
 0x0004   	 client  	 Add buddy(s) to contact list
 0x0005   	 client  	 Remove buddy(ies) from contact
 0x0006   	 client  	 Query for list of watchers
 0x0007   	 server  	 Requested watchers list
 0x0008   	 client  	 Watcher sub request
 0x0009   	 server  	 Watcher notification
 0x000a   	 server  	 Notification rejected
 0x000b   	 server  	 User online notification
 0x000c   	 server  	 User offline notification
	 */
	
	public static final short SNAC_BUDDYLISTMGMT_ERROR = 0x0001;
	public static final short SNAC_BUDDYLISTMGMT_PARAMREQ = 0x0002;
	public static final short SNAC_BUDDYLISTMGMT_PARAMRES = 0x0003;
	public static final short SNAC_BUDDYLISTMGMT_ADDBUDDY = 0x0004;
	public static final short SNAC_BUDDYLISTMGMT_REMOVEBUDDY = 0x0005;
	public static final short SNAC_BUDDYLISTMGMT_WATCHERSLISTREQ = 0x0006;
	public static final short SNAC_BUDDYLISTMGMT_WATCHERSLISTRES = 0x0007;
	public static final short SNAC_BUDDYLISTMGMT_WATCHERREQ = 0x0008;
	public static final short SNAC_BUDDYLISTMGMT_WATCHERRES = 0x0009;
	public static final short SNAC_BUDDYLISTMGMT_NOTIFICATIONREJECTED = 0x000a;
	public static final short SNAC_BUDDYLISTMGMT_USERONLINE = 0x000b;
	public static final short SNAC_BUDDYLISTMGMT_USEROFFLINE = 0x000c;
	
	/** snac MESSAGING family
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Set ICBM parameters
 0x0003   	 client  	 Reset ICBM parameters
 0x0004   	 client  	 Request parameters info
 0x0005   	 server  	 Requested parameters info response
 0x0006   	 client  	 Send message thru server
 0x0007   	 server  	 Message for client from server
 0x0008   	 client  	 Evil request
 0x0009   	 server  	 Server evil ack
 0x000a   	 server  	 Missed call (msg not delivered)
 0x000b   	 client/server  	 Client/server message error or data
 0x000c   	 server  	 Server message ack
 0x0014   	 client/server  	 Mini typing notifications (MTN)
	 */
	
	public static final short SNAC_MESSAGING_ERROR = 0x0001;
	public static final short SNAC_MESSAGING_PARAMSET = 0x0002;
	public static final short SNAC_MESSAGING_PARAMRESET = 0x0003;
	public static final short SNAC_MESSAGING_PARAMREQ = 0x0004;
	public static final short SNAC_MESSAGING_PARAMRES = 0x0005;
	public static final short SNAC_MESSAGING_SENDTHROUGHSERVER = 0x0006;
	public static final short SNAC_MESSAGING_MSGSENTTHROUGHSERVER = 0x0007;
	public static final short SNAC_MESSAGING_BUGOGA = 0x0008;
	public static final short SNAC_MESSAGING_BUGOGARES = 0x0009;
	public static final short SNAC_MESSAGING_MSGNOTDELIVERED = 0x000a;
	public static final short SNAC_MESSAGING_PLUGINMSG = 0x000b;
	public static final short SNAC_MESSAGING_MSGSENT = 0x000c;
	public static final short SNAC_MESSAGING_OFFLINE = 0x0010;
	public static final short SNAC_MESSAGING_TYPINGNOTIFICATION = 0x0014;
	
	/** snac PRIVACYMGMT family
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Request service parameters
 0x0003   	 server  	 Requested service parameters
 0x0004   	 client  	 Set group permissions mask
 0x0005   	 client  	 Add to visible list
 0x0006   	 client  	 Delete from visible list
 0x0007   	 client  	 Add to invisible list
 0x0008   	 client  	 Delete from invisible list
 0x0009   	 server  	 Service error
 0x000A   	 client  	 Add to visible list (?)
 0x000B   	 client  	 Delete from visible list (?)
	 */
	
	public static final short SNAC_PRIVACYMGMT_ERROR = 0x0001;
	public static final short SNAC_PRIVACYMGMT_PARAMREQ = 0x0002;
	public static final short SNAC_PRIVACYMGMT_PARAMRES = 0x0003;
	public static final short SNAC_PRIVACYMGMT_GROUPPERMISSIONSMASKSET = 0x0004;
	public static final short SNAC_PRIVACYMGMT_ADDTOVISIBLE = 0x0005;
	public static final short SNAC_PRIVACYMGMT_REMOVEFROMVISIBLE = 0x0006;
	public static final short SNAC_PRIVACYMGMT_ADDTOINVISIBLE = 0x0007;
	public static final short SNAC_PRIVACYMGMT_REMOVEFROMINVISIBLE = 0x0008;
	public static final short SNAC_PRIVACYMGMT_ADDTOVISIBLE2 = 0x000a;
	public static final short SNAC_PRIVACYMGMT_REMOVEFROMVISIBLE2 = 0x000b;
	
	/** snac USAGESTAT family
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 server  	 Set minimum report interval
 0x0003   	 client  	 Usage stats report
 0x0004   	 server  	 Usage stats report ack
	 */
	
	public static final short SNAC_USAGESTAT_ERROR = 0x0001;
	public static final short SNAC_USAGESTAT_REPORTINTERVALSET = 0x0002;
	public static final short SNAC_USAGESTAT_USAGESTATREPORT = 0x0003;
	public static final short SNAC_USAGESTAT_USAGESTATREPORTRES = 0x0004;
	
	/** snac SERVERSTOREDBUDDYICON family
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Upload your icon to server
 0x0003   	 server  	 Server ack for icon upload
 0x0004   	 client  	 Request buddy icon from server (AIM only)
 0x0005   	 server  	 Server response to a buddy icon request (AIM only)
 0x0006   	 client  	 Request buddy icon from server (ICQ)
 0x0007   	 server  	 Server response to a buddy icon request (ICQ)
	 */
	
	public static final short SNAC_SERVERSTOREDBUDDYICON_ERROR = 0x0001;
	public static final short SNAC_SERVERSTOREDBUDDYICON_ICONUPLOAD = 0x0002;
	public static final short SNAC_SERVERSTOREDBUDDYICON_ICONUPLOADRES = 0x0003;
	public static final short SNAC_SERVERSTOREDBUDDYICON_BUDDYICONREQ1 = 0x0004;
	public static final short SNAC_SERVERSTOREDBUDDYICON_BUDDYICONRES1 = 0x0005;
	public static final short SNAC_SERVERSTOREDBUDDYICON_BUDDYICONREQ2 = 0x0006;
	public static final short SNAC_SERVERSTOREDBUDDYICON_BUDDYICONRES2 = 0x0007;
	
	/** snac SERVERSIDEINFO family
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Request service parameters
 0x0003   	 server  	 Service parameters reply
 0x0004   	 client  	 Request contact list (first time)
 0x0005   	 client  	 Contact list checkout
 0x0006   	 server  	 Server contact list reply
 0x0007   	 client  	 Load server contact list (after login)
 0x0008   	 client/server  	 SSI edit: add item(s)
 0x0009   	 client/server  	 SSI edit: update group header
 0x000a   	 client/server  	 SSI edit: remove item
 0x000e   	 server  	 SSI edit server ack
 0x000f   	 server  	 client local SSI is up-to-date
 0x0011   	 client/server  	 Contacts edit start (begin transaction)
 0x0012   	 client/server  	 Contacts edit end (finish transaction)
 0x0014   	 client  	 Grant future authorization to client
 0x0015   	 server  	 Future authorization granted
 0x0016   	 client  	 Delete yourself from another client server contact
 0x0018   	 client  	 Send authorization request
 0x0019   	 server  	 Authorization request
 0x001a   	 client  	 Send authorization reply
 0x001b   	 server  	 Authorization reply
 0x001c   	 server  	 "You were added" message
	 */
	
	public static final short SNAC_SERVERSIDEINFO_ERROR = 0x0001;
	public static final short SNAC_SERVERSIDEINFO_PARAMREQ = 0x0002;
	public static final short SNAC_SERVERSIDEINFO_PARAMRES = 0x0003;
	public static final short SNAC_SERVERSIDEINFO_CLREQ = 0x0004;
	public static final short SNAC_SERVERSIDEINFO_CLUPD = 0x0005;
	public static final short SNAC_SERVERSIDEINFO_CLRES = 0x0006;
	public static final short SNAC_SERVERSIDEINFO_SERVERCLLOAD = 0x0007;
	public static final short SNAC_SERVERSIDEINFO_ITEMADD = 0x0008;
	public static final short SNAC_SERVERSIDEINFO_GROUPHEADERUPD = 0x0009;
	public static final short SNAC_SERVERSIDEINFO_ITEMREMOVE = 0x000a;
	public static final short SNAC_SERVERSIDEINFO_SSIEDITRES = 0x000e;
	public static final short SNAC_SERVERSIDEINFO_LOCALSSIUPTODATE = 0x000f;
	public static final short SNAC_SERVERSIDEINFO_CONTACTSEDITSTART = 0x0011;
	public static final short SNAC_SERVERSIDEINFO_CONTACTSEDITEND = 0x0012;
	public static final short SNAC_SERVERSIDEINFO_CLIENTAUTHGRANT = 0x0014;
	public static final short SNAC_SERVERSIDEINFO_AUTHGRANTED = 0x0015;
	public static final short SNAC_SERVERSIDEINFO_DELETEME = 0x0016;
	public static final short SNAC_SERVERSIDEINFO_AUTHREQSEND = 0x0018;
	public static final short SNAC_SERVERSIDEINFO_AUTHREQ = 0x0019;
	public static final short SNAC_SERVERSIDEINFO_AUTHRESSEND = 0x001a;
	public static final short SNAC_SERVERSIDEINFO_AUTHRES = 0x001b;
	public static final short SNAC_SERVERSIDEINFO_YOUWEREADDED = 0x001c;
	
	/** snac ICQEXTENSION family 
	 * 
 0x0001   	 client/server  	 Client / server error
 0x0002   	 client  	 Meta information request
 0x0003   	 server  	 Meta information response
	 */
	
	public static final short SNAC_ICQEXTENSION_ERROR = 0x0001;
	public static final short SNAC_ICQEXTENSION_METAINFOREQ = 0x0002;
	public static final short SNAC_ICQEXTENSION_METAINFORES = 0x0003;
	
	/** snac AUTHENTICATE family 
	 * 
 0x0001   	 client/server  	 Server error (registration refused)
 0x0002   	 client  	 Client login request (md5 login sequence)
 0x0003   	 server  	 Server login reply / error reply
 0x0004   	 client  	 Request new uin
 0x0005   	 server  	 New uin response
 0x0006   	 client  	 Request md5 authkey
 0x0007   	 server  	 Server md5 authkey response
 0x000a   	 server  	 Server SecureID request
 0x000b   	 client  	 Client SecureID reply
	 */
	
	public static final short SNAC_AUTHENTICATE_ERROR = 0x0001;
	public static final short SNAC_AUTHENTICATE_LOGINREQ = 0x0002;
	public static final short SNAC_AUTHENTICATE_LOGINRES = 0x0003;
	public static final short SNAC_AUTHENTICATE_NEWUINREQ = 0x0004;
	public static final short SNAC_AUTHENTICATE_NEWUINRES = 0x0005;
	public static final short SNAC_AUTHENTICATE_MD5AUTHKEYREQ = 0x0006;
	public static final short SNAC_AUTHENTICATE_MD5AUTHKEYRES = 0x0007;
	public static final short SNAC_AUTHENTICATE_SECUREIDREQ = 0x000a;
	public static final short SNAC_AUTHENTICATE_SECUREIDRES = 0x000b;
	
	/**
	 * common set of widely used tlvs:

	
 00 01 	  	word 	  	TLV.Type(0x01) - screen name (uin)
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	screen name (uin)
	
	
 00 02 	  	word 	  	TLV.Type(0x02) - new password
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	array 	  	new password string
	
	
 00 03 	  	word 	  	TLV.Type(0x03) - client identity string
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	client identity string (name, version)
	
	
 00 04 	  	word 	  	TLV.Type(0x04) - error description url
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	error description url string
	
	
 00 05 	  	word 	  	TLV.Type(0x05) - reconnect here
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	server address & port string
	
	
 00 06 	  	word 	  	TLV.Type(0x06) - authorization cookie
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	array 	  	authorization cookie
	
	
 00 07 	  	word 	  	TLV.Type(0x07) - snac version
 xx xx 	  	word 	  	TLV.Length
  	  	  	  	?
	
	
 00 08 	  	word 	  	TLV.Type(0x08) - error subcode
 xx xx 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	error subcode (family specific)
	
	
 00 09 	  	word 	  	TLV.Type(0x09) - disconnect reason
 xx xx 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	disconnect reason (see table below)
	
	
 00 0A 	  	word 	  	TLV.Type(0x0A) - reconnect hostname
 xx xx 	  	word 	  	TLV.Length
  	  	  	  	?
	
	
 00 0B 	  	word 	  	TLV.Type(0x0B) - url
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	url
	
	
 00 0C 	  	word 	  	TLV.Type(0x0C) - debug data
 xx xx 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	debug data
	
	
 00 0D 	  	word 	  	TLV.Type(0x0D) - service (family) id
 xx xx 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	service (family) id
	
	
 00 0E 	  	word 	  	TLV.Type(0x0E) - client country (2 symbols)
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	client country
	
	
 00 0F 	  	word 	  	TLV.Type(0x0F) - client language (2 symbols)
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	client language
	
	
 00 10 	  	word 	  	TLV.Type(0x10) - script
 xx xx 	  	word 	  	TLV.Length
  	  	  	  	?
	
	
 00 11 	  	word 	  	TLV.Type(0x11) - user email
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	user email string
	
	
 00 12 	  	word 	  	TLV.Type(0x12) - old password
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	old password string
	
	
 00 13 	  	word 	  	TLV.Type(0x13) - registration status
 xx xx 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	registration status (1 - no disclosure, 2 - limit disclosure, 3 - full disclosure (?))
	
	
 00 14 	  	word 	  	TLV.Type(0x14) - distribution number
 00 04 	  	word 	  	TLV.Length
 xx xx xx xx 	  	dword 	  	distribution number
	
	
 00 15 	  	word 	  	TLV.Type(0x15) - personal text
 xx xx 	  	word 	  	TLV.Length
  	  	  	  	?
	
	
 00 16 	  	word 	  	TLV.Type(0x16) - client id
 00 02 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	client id number
	
	
 00 17 	  	word 	  	TLV.Type(0x17) - client major version
 00 02 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	client major version
	
	
 00 18 	  	word 	  	TLV.Type(0x18) - client minor version
 00 02 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	client minor version
	
	
 00 19 	  	word 	  	TLV.Type(0x19) - client lesser version
 00 02 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	client lesser version
	
	
 00 1A 	  	word 	  	TLV.Type(0x1A) - client build number
 00 02 	  	word 	  	TLV.Length
 xx xx 	  	word 	  	client build number
	
	
 00 25 	  	word 	  	TLV.Type(0x25) - password hash (MD5)
 00 10 	  	word 	  	TLV.Length
 xx xx xx xx
 xx xx xx xx
 xx xx xx xx
 xx xx xx xx
	  	array 	  	password hash (MD5)
	
	
 00 40 	  	word 	  	TLV.Type(0x40) - latest beta build number
 xx xx 	  	word 	  	TLV.Length
 xx xx xx xx 	  	dword 	  	latest beta build number
	
	
 00 41 	  	word 	  	TLV.Type(0x41) - latest beta install url
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	latest beta install url
	
	
 00 42 	  	word 	  	TLV.Type(0x42) - latest beta info url
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	latest beta info url
	
	
 00 43 	  	word 	  	TLV.Type(0x43) - latest beta version
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	latest beta version
	
	
 00 44 	  	word 	  	TLV.Type(0x44) - latest release build number
 xx xx 	  	word 	  	TLV.Length
 xx xx xx xx 	  	dword 	  	latest release build number
	
	
 00 45 	  	word 	  	TLV.Type(0x45) - latest release install url
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	latest release install url
	
	
 00 46 	  	word 	  	TLV.Type(0x46) - latest release info url
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	latest release info url
	
	
 00 47 	  	word 	  	TLV.Type(0x47) - latest release version
 xx xx 	  	word 	  	TLV.Length
 xx .. 	  	string 	  	latest release version
	
	
 00 48 	  	word 	  	TLV.Type(0x48) - beta digest signature (MD5)
 00 20 	  	word 	  	TLV.Length
 xx ... 	string 	  	hexadecimal string for beta digest signature (MD5)
	
	
 00 49 	  	word 	  	TLV.Type(0x49) - release digest signature (MD5)
 00 20 	  	word 	  	TLV.Length
 xx ... 	string 	  	hexadecimal string for release digest signature (MD5)
	
	
 00 54 	  	word 	  	TLV.Type(0x54) - change password url
 xx xx 	  	word 	  	TLV.Length
 xx ... 	string 	  	change password url
	 */
	
	public static final short TLV_SCREENNAME = 0x0001;
	public static final short TLV_NEWPASSWORD = 0x0002;
	public static final short TLV_CLIENTIDENTITY = 0x0003;
	public static final short TLV_ERRORDESCRIPTIONURL = 0x0004;
	public static final short TLV_RECONNECTADDRESS = 0x0005;
	public static final short TLV_AUTHCOOKIE = 0x0006;
	public static final short TLV_SNACVERSION = 0x0007;
	public static final short TLV_ERRORSUBCODE = 0x0008;
	public static final short TLV_DISCONNECTREASON = 0x0009;
	public static final short TLV_RECONNECTHOST = 0x000a;
	public static final short TLV_URL = 0x000b;
	public static final short TLV_DEBUGDATA = 0x000c;
	public static final short TLV_DCINFO = 0x000c;
	public static final short TLV_SERVICEID = 0x000d;
	public static final short TLV_CLIENTCOUNTRY = 0x000e;
	public static final short TLV_CLIENTLANGUAGE = 0x000f;
	public static final short TLV_CLIENTIDLE = 0x000f;
	public static final short TLV_SCRIPT = 0x0010;
	public static final short TLV_USEREMAIL = 0x0011;
	public static final short TLV_OLDPASSWORD = 0x0012;
	public static final short TLV_REGISTRATIONSTATUS = 0x0013;
	public static final short TLV_DISTRIBUTIONNUMBER = 0x0014;
	public static final short TLV_PERSONALTEXT = 0x0015;
	public static final short TLV_CLIENTID = 0x0016;
	public static final short TLV_CLIENTMAJORVERSION = 0x0017;
	public static final short TLV_CLIENTMINORVERSION = 0x0018;
	public static final short TLV_CLIENTLESSERVERSION = 0x0019;
	public static final short TLV_CLIENTBUILDNUMBER = 0x001a;
	public static final short TLV_PASSWORDHASH = 0x0025;
	public static final short TLV_LATESTBETABUILDNUMBER = 0x0040;
	public static final short TLV_LATESTBETAINSTALLURL = 0x0041;
	public static final short TLV_LATESTBETAINFOURL = 0x0042;
	public static final short TLV_LATESTBETAVERSION = 0x0043;
	public static final short TLV_LATESTRELEASEBUILDNUMBER = 0x0044;
	public static final short TLV_LATESTRELEASEINSTALLURL = 0x0045;
	public static final short TLV_LATESTRELEASEINFOURL = 0x0046;
	public static final short TLV_LATESTRELEASEVERSION = 0x0047;
	public static final short TLV_BETADIGESTSIGNATURE = 0x0048;
	public static final short TLV_RELEASEDIGESTSIGNATURE = 0x0049;
	public static final short TLV_CHANGEPASSWORD = 0x0054;
	public static final short TLV_EVIL = 0x008e;
	
	public static final short TLV_ONLINE_USERCLASS = 0x1;
	public static final short TLV_ONLINE_CREATETIME = 0x2;
	public static final short TLV_ONLINE_SIGNONTIME = 0x3;
	public static final short TLV_ONLINE_IDLETIME = 0x4;
	public static final short TLV_ONLINE_ACCOUNTCREATIONTIME = 0x5;
	public static final short TLV_ONLINE_USERSTATUS = 0x6;
	public static final short TLV_ONLINE_EXTERNALIP = 0xa;
	public static final short TLV_ONLINE_DCINFO = 0xc;
	public static final short TLV_ONLINE_CAPABILITIES = 0xd;
	public static final short TLV_ONLINE_ONLINETIME = 0xf;
	public static final short TLV_ONLINE_ICONDATA = 0x1d;
	public static final short TLV_ONLINE_OWNNAME = 0x18;
	
	public static final short USERCLASS_UNCONFIRMED = 0x0001;
	public static final short USERCLASS_ADMIN = 0x0002;
	public static final short USERCLASS_AOL = 0x0004;
	public static final short USERCLASS_COMMERCIAL = 0x0008;
	public static final short USERCLASS_FREE = 0x0010;
	public static final short USERCLASS_AWAY = 0x0020;
	public static final short USERCLASS_ICQ = 0x0040;
	public static final short USERCLASS_WIRELESS = 0x0080;
	public static final short USERCLASS_UNK100 = 0x0100;
	public static final short USERCLASS_UNK200 = 0x0200;
	public static final short USERCLASS_UNK400 = 0x0400;
	public static final short USERCLASS_UNK800 = 0x0800;
	
	public static final byte[] CLSID_SRV_RELAY = {0x09, 0x46, 0x13, 0x49, 0x4c, 0x7f, 0x11, (byte) 0xd1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_UTF = {0x09, 0x46, 0x13, 0x4e, 0x4c, 0x7f, 0x11, (byte) 0xd1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_CLIENTINFOPREFIX = {0x09, 0x46, 0x13, 0x4c, 0x4c, 0x7f, 0x11, (byte) 0xd1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_ICQUTF = {0x09, 0x46, 0x13, 0x49, 0x4c, 0x7f, 0x11, (byte) 0xd1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_RTF = {(byte) 0x97, (byte) 0xb1, 0x27, 0x51, 0x24, 0x3c, 0x43, 0x34, (byte) 0xad, 0x22, (byte) 0xd6, (byte) 0xab, (byte) 0xf7, 0x3f, 0x14, (byte) 0x92};
	public static final byte[] CLSID_DIRECT = {0x09, 0x46, 0x13, 0x44, 0x4C, 0x7F, 0x11, (byte) 0xD1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_TYPING = {0x56, 0x3f, (byte) 0xc8, 0x09, 0x0b, 0x6f, 0x41, (byte) 0xbd, (byte) 0x9f, 0x79, 0x42, 0x26, 0x09, (byte) 0xdf, (byte) 0xa2, (byte) 0xf3};
	public static final byte[] CLSID_XTRAZ = {0x1A, 0x09, 0x3C, 0x6C, (byte) 0xD7, (byte) 0xFD, 0x4E, (byte) 0xC5, (byte) 0x9D, 0x51, (byte) 0xA6, 0x47, 0x4E, 0x34, (byte) 0xF5, (byte) 0xA0};
	public static final byte[] CLSID_AIM_FILESEND = {0x09, 0x46, 0x13, 0x43, 0x4C, 0x7F, 0x11, (byte) 0xD1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_SHORT_CAPS = {0x09, 0x46, 0x00, 0x00, 0x4C, 0x7F, 0x11, (byte) 0xD1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_AIM_FILERECEIVE = {0x09, 0x46, 0x13, 0x48, 0x4C, 0x7F, 0x11, (byte) 0xD1, (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
	public static final byte[] CLSID_ASIA = {(byte) 0x88,(byte) 0x88,(byte) 0x88,(byte) 0x88,(byte) 0x88,(byte) 0x88,(byte) 0x88,(byte) 0x88,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
	
	public static final byte[] LOCAL_FAMILIES_VERSIONS = {
		0x00, 0x22, 0x00, 0x01, 
		0x00, 0x01, 0x00, 0x04, 
		0x00, 0x13, 0x00, 0x04, 
		0x00, 0x02, 0x00, 0x01, 
		0x00, 0x03, 0x00, 0x01, 
		0x00, 0x15, 0x00, 0x01, 
		0x00, 0x04, 0x00, 0x01, 
		0x00, 0x06, 0x00, 0x01, 
		0x00, 0x09, 0x00, 0x01, 
		0x00, 0x0a, 0x00, 0x01,
		
		/*0x00, 0x24, 0x00, 0x01,
		0x00, 0x25, 0x00, 0x01,*/
		
		0x00, 0x0b, 0x00, 0x01};
	
	public static final String GUID_RTF_TEXT = "00000000000000000000000000000000";
	public static final String GUID_ICQUTF = "{09461349-4C7F-11D1-8222-444553540000}";
	public static final String GUID_UTF8 = "{0946134E-4C7F-11D1-8222-444553540000}";
	
	public static final byte[] GUID_XSTATUSMSG = {0x3b, 0x60, (byte) 0xb3, (byte) 0xef, (byte) 0xd8, 0x2a, 0x6c, 0x45, (byte) 0xa4, (byte) 0xe0, (byte) 0x9c, 0x5a, 0x5e, 0x67, (byte) 0xe8, 0x65};
	
	public static final int STATUS_WEBAWARE = 0x0001;
	public static final int STATUS_SHOWIP = 0x0002;
	public static final int STATUS_BIRTHDAY = 0x0008;
	public static final int STATUS_WEBFRONT = 0x0020;
	public static final int STATUS_DCDISABLED = 0x0100;
	public static final int STATUS_DCAUTH = 0x1000;
	public static final int STATUS_DCCONT = 0x2000;
		
	public static final int STATUS_ONLINE  = 0x0000;
	public static final int STATUS_AWAY = 0x0001;
	public static final int STATUS_DND = 0x0002;
	public static final int STATUS_NA = 0x0004;
	public static final int STATUS_OCCUPIED = 0x0010;
	public static final int STATUS_FREE4CHAT = 0x0020;
	public static final int STATUS_INVISIBLE = 0x0100;
	public static final int STATUS_OFFLINE = 0x8888;
	
	public static final byte[] CLSID_STATUS_DEPRESSION = {(byte) 0xB7,0x07,0x43,0x78,(byte) 0xF5,0x0C,0x77,0x77,(byte) 0x97,0x77,0x57,0x78,0x50,0x2D,0x05,0x70};
	public static final byte[] CLSID_STATUS_FREE4CHAT = {(byte) 0xB7,0x07,0x43,0x78,(byte) 0xF5,0x0C,0x77,0x77,(byte) 0x97,0x77,0x57,0x78,0x50,0x2D,0x05,0x75};
	public static final byte[] CLSID_STATUS_HOME = {(byte) 0xB7,0x07,0x43,0x78,(byte) 0xF5,0x0C,0x77,0x77,(byte) 0x97,0x77,0x57,0x78,0x50,0x2D,0x05,0x76};
	public static final byte[] CLSID_STATUS_WORK = {(byte) 0xB7,0x07,0x43,0x78,(byte) 0xF5,0x0C,0x77,0x77,(byte) 0x97,0x77,0x57,0x78,0x50,0x2D,0x05,0x77};
	public static final byte[] CLSID_STATUS_LUNCH = {(byte) 0xB7,0x07,0x43,0x78,(byte) 0xF5,0x0C,0x77,0x77,(byte) 0x97,0x77,0x57,0x78,0x50,0x2D,0x05,0x78};
	public static final byte[] CLSID_STATUS_ANGRY = {(byte) 0xB7,0x07,0x43,0x78,(byte) 0xF5,0x0C,0x77,0x77,(byte) 0x97,0x77,0x57,0x78,0x50,0x2D,0x05,0x79};
	
	public static final byte DC_DISABLED = 0x0;
	public static final byte DC_HTTPS = 0x1;
	public static final byte DC_SOCKS = 0x2;
	public static final byte DC_NORMAL = 0x4;
	public static final byte DC_WEB = 0x6;
	
	public static final short DC_PROTO_VERSION = 0x0009;
	public static final int WEB_FRONT_PORT = 0x50;
	
	public static final short FAMILY_TOOL_ID = 0x0110;
	public static final short FAMILY_TOOL_VERSION = 0x164f;
	
	public static final String ERROR_BADCREDENTIALS = "errors/MISMATCH_PASSWD";
	
	public static final short ICBM_MESSAGETYPE_REQUEST = 0;
	public static final short ICBM_MESSAGETYPE_CANCEL = 1;
	public static final short ICBM_MESSAGETYPE_ACCEPT = 2;
	
	public static final byte MTYPE_ACK = 0;
	public static final byte MTYPE_PLAIN = 0x1;
	public static final byte MTYPE_CHAT = 0x2;
	public static final byte MTYPE_FILEREQ = 0x3;
	public static final byte MTYPE_URL = 0x4;
	public static final byte MTYPE_AUTHREQ = 0x6;
	public static final byte MTYPE_AUTHDENY = 0x7;
	public static final byte MTYPE_AUTHOK = 0x8;
	public static final byte MTYPE_SERVER = 0x9;
	public static final byte MTYPE_YOUWEREADDED = 0xc;
	public static final byte MTYPE_WEBPAGER = 0xd;
	public static final byte MTYPE_EMAILEXPRESS = 0xe;
	public static final byte MTYPE_CONTACTS = 0x13;
	public static final byte MTYPE_PLUGIN = 0x1a;
	public static final byte MTYPE_AUTOAWAY = (byte) 0xe8;
	public static final byte MTYPE_AUTOBUSY = (byte) 0xe9;
	public static final byte MTYPE_AUTONA = (byte) 0xea;
	public static final byte MTYPE_AUTODND = (byte) 0xeb;
	public static final byte MTYPE_AUTOFFC = (byte) 0xec;
	
	public static final byte MFLAG_NORMAL = 0x1;
	public static final byte MFLAG_AUTO = 0x3;
	public static final byte MFLAG_MULTI = (byte) 0x80;
	
	public static final byte TLV_ICQEXTENSION_METADATA = 0x1;
	
	public static final short ICQEXTENSION_COMMAND_METADATA_REQ = 0x7d0;	
	public static final short ICQEXTENSION_COMMAND_GETOFFLINEMESSAGES = 0x003c;
	public static final short ICQEXTENSION_COMMAND_OFFLINEMESSAGE = 0x0041;
	public static final short ICQEXTENSION_COMMAND_DELETEOFFLINEMESSAGES = 0x003e;
	public static final short ICQEXTENSION_SUBCOMMAND_SEARCH_BY_UIN_WITH_TLV = 0x0569;
	
	public static final short ICBM_PORT = 0x4455;
	
	public static final String SAVEDPREFERENCES_NAME = "ICQSavedParams";
	public static final String SAVEDPREFERENCES_SSI_UPDATE_DATE = "SSIUpdateDate";
	public static final String SAVEDPREFERENCES_SSI_ITEM_COUNT = "SSIItemCount";
	
	public static final int XSTATUS_NONE = 37;

	public static final byte[][] XSTATUS_CLSIDS =
	{
		new byte[]{0x63,0x62,0x73,0x37,(byte) 0xA0,0x3F,0x49,(byte) 0xFF,(byte) 0x80,(byte) 0xE5,(byte) 0xF7,0x09,(byte) 0xCD,(byte) 0xE0,(byte) 0xA4,(byte) 0xEE}, // SHOPPING
		new byte[]{0x5A,0x58,0x1E,(byte) 0xA1,(byte) 0xE5,(byte) 0x80,0x43,0x0C,(byte) 0xA0,0x6F,0x61,0x22,(byte) 0x98,(byte) 0xB7,(byte) 0xE4,(byte) 0xC7}, // DUCK
		new byte[]{(byte) 0x83,(byte) 0xC9,(byte) 0xB7,(byte) 0x8E,0x77,(byte) 0xE7,0x43,0x78,(byte) 0xB2,(byte) 0xC5,(byte) 0xFB,0x6C,(byte) 0xFC,(byte) 0xC3,0x5B,(byte) 0xEC}, // TIRED
		new byte[]{(byte) 0xE6,0x01,(byte) 0xE4,0x1C,0x33,0x73,0x4B,(byte) 0xD1,(byte) 0xBC,0x06,(byte) 0x81,0x1D,0x6C,0x32,0x3D,(byte) 0x81}, // PARTY
		new byte[]{(byte) 0x8C,0x50,(byte) 0xDB,(byte) 0xAE,(byte) 0x81,(byte) 0xED,0x47,(byte) 0x86,(byte) 0xAC,(byte) 0xCA,0x16,(byte) 0xCC,0x32,0x13,(byte) 0xC7,(byte) 0xB7}, // BEER
		new byte[]{0x3F,(byte) 0xB0,(byte) 0xBD,0x36,(byte) 0xAF,0x3B,0x4A,0x60,(byte) 0x9E,(byte) 0xEF,(byte) 0xCF,0x19,0x0F,0x6A,0x5A,0x7F}, // THINKING
		new byte[]{(byte) 0xF8,(byte) 0xE8,(byte) 0xD7,(byte) 0xB2,(byte) 0x82,(byte) 0xC4,0x41,0x42,(byte) 0x90,(byte) 0xF8,0x10,(byte) 0xC6,(byte) 0xCE,0x0A,(byte) 0x89,(byte) 0xA6}, // EATING
		new byte[]{(byte) 0x80,0x53,0x7D,(byte) 0xE2,(byte) 0xA4,0x67,0x4A,0x76,(byte) 0xB3,0x54,0x6D,(byte) 0xFD,0x07,0x5F,0x5E,(byte) 0xC6}, // TV
		new byte[]{(byte) 0xF1,(byte) 0x8A,(byte) 0xB5,0x2E,(byte) 0xDC,0x57,0x49,0x1D,(byte) 0x99,(byte) 0xDC,0x64,0x44,0x50,0x24,0x57,(byte) 0xAF}, // FRIENDS
		new byte[]{0x1B,0x78,(byte) 0xAE,0x31,(byte) 0xFA,0x0B,0x4D,0x38,(byte) 0x93,(byte) 0xD1,(byte) 0x99,0x7E,(byte) 0xEE,(byte) 0xAF,(byte) 0xB2,0x18}, // COFFEE
		new byte[]{0x61,(byte) 0xBE,(byte) 0xE0,(byte) 0xDD,(byte) 0x8B,(byte) 0xDD,0x47,0x5D,(byte) 0x8D,(byte) 0xEE,0x5F,0x4B,(byte) 0xAA,(byte) 0xCF,0x19,(byte) 0xA7}, // MUSIC
		new byte[]{0x48,(byte) 0x8E,0x14,(byte) 0x89,(byte) 0x8A,(byte) 0xCA,0x4A,0x08,(byte) 0x82,(byte) 0xAA,0x77,(byte) 0xCE,0x7A,0x16,0x52,0x08}, // BUSINESS
		new byte[]{0x10,0x7A,(byte) 0x9A,0x18,0x12,0x32,0x4D,(byte) 0xA4,(byte) 0xB6,(byte) 0xCD,0x08,0x79,(byte) 0xDB,0x78,0x0F,0x09}, // CAMERA
		new byte[]{0x6F,0x49,0x30,(byte) 0x98,0x4F,0x7C,0x4A,(byte) 0xFF,(byte) 0xA2,0x76,0x34,(byte) 0xA0,0x3B,(byte) 0xCE,(byte) 0xAE,(byte) 0xA7}, // FUNNY
		new byte[]{0x12,(byte) 0x92,(byte) 0xE5,0x50,0x1B,0x64,0x4F,0x66,(byte) 0xB2,0x06,(byte) 0xB2,(byte) 0x9A,(byte) 0xF3,0x78,(byte) 0xE4,(byte) 0x8D}, // PHONE
		new byte[]{(byte) 0xD4,(byte) 0xA6,0x11,(byte) 0xD0,(byte) 0x8F,0x01,0x4E,(byte) 0xC0,(byte) 0x92,0x23,(byte) 0xC5,(byte) 0xB6,(byte) 0xBE,(byte) 0xC6,(byte) 0xCC,(byte) 0xF0}, // GAMES
		new byte[]{0x60,(byte) 0x9D,0x52,(byte) 0xF8,(byte) 0xA2,(byte) 0x9A,0x49,(byte) 0xA6,(byte) 0xB2,(byte) 0xA0,0x25,0x24,(byte) 0xC5,(byte) 0xE9,(byte) 0xD2,0x60}, // COLLEGE
		new byte[]{0x1F,0x7A,0x40,0x71,(byte) 0xBF,0x3B,0x4E,0x60,(byte) 0xBC,0x32,0x4C,0x57,(byte) 0x87,(byte) 0xB0,0x4C,(byte) 0xF1}, // SICK
		new byte[]{0x78,0x5E,(byte) 0x8C,0x48,0x40,(byte) 0xD3,0x4C,0x65,(byte) 0x88,0x6F,0x04,(byte) 0xCF,0x3F,0x3F,0x43,(byte) 0xDF}, // SLEEPING
		new byte[]{(byte) 0xA6,(byte) 0xED,0x55,0x7E,0x6B,(byte) 0xF7,0x44,(byte) 0xD4,(byte) 0xA5,(byte) 0xD4,(byte) 0xD2,(byte) 0xE7,(byte) 0xD9,0x5C,(byte) 0xE8,0x1F}, // SURFING
		new byte[]{0x12,(byte) 0xD0,0x7E,0x3E,(byte) 0xF8,(byte) 0x85,0x48,(byte) 0x9E,(byte) 0x8E,(byte) 0x97,(byte) 0xA7,0x2A,0x65,0x51,(byte) 0xE5,(byte) 0x8D}, // INTERNET
		new byte[]{(byte) 0xBA,0x74,(byte) 0xDB,0x3E,(byte) 0x9E,0x24,0x43,0x4B,(byte) 0x87,(byte) 0xB6,0x2F,0x6B,(byte) 0x8D,(byte) 0xFE,(byte) 0xE5,0x0F}, // ENGINEERING
		new byte[]{0x63,0x4F,0x6B,(byte) 0xD8,(byte) 0xAD,(byte) 0xD2,0x4A,(byte) 0xA1,(byte) 0xAA,(byte) 0xB9,0x11,0x5B,(byte) 0xC2,0x6D,0x05,(byte) 0xA1}, // TYPING
		new byte[]{0x01,(byte) 0xD8,(byte) 0xD7,(byte) 0xEE,(byte) 0xAC,0x3B,0x49,0x2A,(byte) 0xA5,(byte) 0x8D,(byte) 0xD3,(byte) 0xD8,0x77,(byte) 0xE6,0x6B,(byte) 0x92}, // ANGRY
		new byte[]{0x2C,(byte) 0xE0,(byte) 0xE4,(byte) 0xE5,0x7C,0x64,0x43,0x70,(byte) 0x9C,0x3A,0x7A,0x1C,(byte) 0xE8,0x78,(byte) 0xA7,(byte) 0xDC}, // UNK
		new byte[]{0x10,0x11,0x17,(byte) 0xC9,(byte) 0xA3,(byte) 0xB0,0x40,(byte) 0xF9,(byte) 0x81,(byte) 0xAC,0x49,(byte) 0xE1,0x59,(byte) 0xFB,(byte) 0xD5,(byte) 0xD4}, // PPC
		new byte[]{0x16,0x0C,0x60,(byte) 0xBB,(byte) 0xDD,0x44,0x43,(byte) 0xF3,(byte) 0x91,0x40,0x05,0x0F,0x00,(byte) 0xE6,(byte) 0xC0,0x09}, // MOBILE
		new byte[]{0x64,0x43,(byte) 0xC6,(byte) 0xAF,0x22,0x60,0x45,0x17,(byte) 0xB5,(byte) 0x8C,(byte) 0xD7,(byte) 0xDF,(byte) 0x8E,0x29,0x03,0x52}, // MAN
		new byte[]{0x16,(byte) 0xF5,(byte) 0xB7,0x6F,(byte) 0xA9,(byte) 0xD2,0x40,0x35,(byte) 0x8C,(byte) 0xC5,(byte) 0xC0,(byte) 0x84,0x70,0x3C,(byte) 0x98,(byte) 0xFA}, // WC
		new byte[]{0x63,0x14,0x36,(byte) 0xFF,0x3F,(byte) 0x8A,0x40,(byte) 0xD0,(byte) 0xA5,(byte) 0xCB,0x7B,0x66,(byte) 0xE0,0x51,(byte) 0xB3,0x64}, // QUESTION
		new byte[]{(byte) 0xB7,0x08,0x67,(byte) 0xF5,0x38,0x25,0x43,0x27,(byte) 0xA1,(byte) 0xFF,(byte) 0xCF,0x4C,(byte) 0xC1,(byte) 0x93,(byte) 0x97,(byte) 0x97}, // WAY
		new byte[]{(byte) 0xDD,(byte) 0xCF,0x0E,(byte) 0xA9,0x71,(byte) 0x95,0x40,0x48,(byte) 0xA9,(byte) 0xC6,0x41,0x32,0x06,(byte) 0xD6,(byte) 0xF2,(byte) 0x80}, // HEART
		new byte[]{(byte) 0xCD,0x56,0x43,(byte) 0xA2,(byte) 0xC9,0x4C,0x47,0x24,(byte) 0xB5,0x2C,(byte) 0xDC,0x01,0x24,(byte) 0xA1,(byte) 0xD0,(byte) 0xCD},
		new byte[]{0x3F,(byte) 0xB0,(byte) 0xBD,0x36,(byte) 0xAF,0x3B,0x4A,0x60,(byte) 0x9E,(byte) 0xEF,(byte) 0xCF,0x19,0x0F,0x6A,0x5A,0x7E}, // CIGARETTE
		new byte[]{(byte) 0xE6,0x01,(byte) 0xE4,0x1C,0x33,0x73,0x4B,(byte) 0xD1,(byte) 0xBC,0x06,(byte) 0x81,0x1D,0x6C,0x32,0x3D,(byte) 0x82}, // SEX
		new byte[]{(byte) 0xD4,(byte) 0xE2,(byte) 0xB0,(byte) 0xBA,0x33,0x4E,0x4F,(byte) 0xA5,(byte) 0x98,(byte) 0xD0,0x11,0x7D,(byte) 0xBF,0x4D,0x3C,(byte) 0xC8}, // SEARCH
		new byte[]{0x00,0x72,(byte) 0xD9,0x08,0x4A,(byte) 0xD1,0x43,(byte) 0xDD,(byte) 0x91,(byte) 0x99,0x6F,0x02,0x69,0x66,0x02,0x6F}  // DIARY
	};

	/*public static final String[] XSTATUS_CLSIDS = {
		"63627337A03F49FF80E5F709CDE0A4EE", // 	XStatus(Shopping)
		"5A581EA1E580430CA06F612298B7E4C7", // 	XStatus(Duck)
		"83C9B78E77E74378B2C5FB6CFCC35BEC", //	XStatus(Tired) 
		"E601E41C33734BD1BC06811D6C323D81", // 	XStatus(Party)
		"8C50DBAE81ED4786ACCA16CC3213C7B7", // 	XStatus(Beer)
		"3FB0BD36AF3B4A609EEFCF190F6A5A7F", // 	XStatus(Thinking)
		"F8E8D7B282C4414290F810C6CE0A89A6", // 	XStatus(Eating)
		"80537DE2A4674A76B3546DFD075F5EC6", // 	XStatus(TV)
		"F18AB52EDC57491D99DC6444502457AF", // 	XStatus(Friends)
		"1B78AE31FA0B4D3893D1997EEEAFB218", // 	XStatus(Coffee)
		"61BEE0DD8BDD475D8DEE5F4BAACF19A7", // 	XStatus(Music)
		"488E14898ACA4A0882AA77CE7A165208", // 	XStatus(Business)
		"107A9A1812324DA4B6CD0879DB780F09", // 	XStatus(Camera)
		"6F4930984F7C4AFFA27634A03BCEAEA7", // 	XStatus(Funny)
		"1292E5501B644F66B206B29AF378E48D", // 	XStatus(Phone)
		"D4A611D08F014EC09223C5B6BEC6CCF0", // 	XStatus(Games)
		"609D52F8A29A49A6B2A02524C5E9D260", // 	XStatus(College)
		"1F7A4071BF3B4E60BC324C5787B04CF1", // 	XStatus(Sick)
		"785E8C4840D34C65886F04CF3F3F43DF", // 	XStatus(Sleeping)
		"A6ED557E6BF744D4A5D4D2E7D95CE81F", // 	XStatus(Surfing)
		"12D07E3EF885489E8E97A72A6551E58D", // 	XStatus(@)
		"BA74DB3E9E24434B87B62F6B8DFEE50F", // 	XStatus(Engineering)
		"634F6BD8ADD24AA1AAB9115BC26D05A1", // 	XStatus(Typing)
		"01D8D7EEAC3B492AA58DD3D877E66B92", // 	XStatus(Angry)
		"2CE0E4E57C6443709C3A7A1CE878A7DC", // 	XStatus(China1)
		"101117C9A3B040F981AC49E159FBD5D4", // 	XStatus(China2)
		"160C60BBDD4443F39140050F00E6C009", // 	XStatus(China3)
		"6443C6AF22604517B58CD7DF8E290352", // 	XStatus(China4)
		"16F5B76FA9D240358CC5C084703C98FA", // 	XStatus(China5)
		"631436FF3F8A40D0A5CB7B66E051B364", // 	XStatus(De1)
		"B70867F538254327A1FFCF4CC1939797", // 	XStatus(De2)
		"DDCF0EA971954048A9C6413206D6F280", // 	XStatus(De3)
		"CD5643A2C94C4724B52CDC0124A1D0CD", // 	XStatus(RuLove)
		"3FB0BD36AF3B4A609EEFCF190F6A5A7E", // 	XStatus(Smoking)
		"E601E41C33734BD1BC06811D6C323D82", // 	XStatus(Sex)
		"D4E2B0BA334E4FA598D0117DBF4D3CC8", // 	XStatus(RuSearch)
		"0072D9084AD143DD91996F026966026F"  // 	XStatus(RuJournal)
	};*/
	
	public static final byte VIS_TO_PERMITTED = 3;
	public static final byte VIS_EXCEPT_DENIED = 4;
	public static final byte VIS_TO_BUDDIES = 5;
	public static final byte VIS_TO_ALL = 1;
	public static final byte VIS_INVISIBLE = 2;
	public static final byte VIS_PERMITTED = 1;
	public static final byte VIS_DENIED = 2;
	public static final byte VIS_IGNORED = 3;
	public static final byte VIS_REGULAR = 0;
	public static final byte VIS_NOT_AUTHORIZED = 4;
	
	public static final String SEARCHPARAM_UIN = "uin";
}
