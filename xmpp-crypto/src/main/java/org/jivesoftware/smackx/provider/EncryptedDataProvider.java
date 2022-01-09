package org.jivesoftware.smackx.provider;

import java.security.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.EncryptedMessage;
import org.jivesoftware.smackx.packet.SignedPresence;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.xmlpull.v1.XmlPullParser;

public class EncryptedDataProvider implements PacketExtensionProvider {
	
	private static BouncyCastleProvider bcProvider = null;

	private final Map<String, String> keyStorage = Collections.synchronizedMap(new HashMap<String, String>());
	
	private String myKey = null;
	private String myKeyPw = null;

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		PacketExtension ext = null;
		if (parser.getNamespace().equals("jabber:x:encrypted")) {
			EncryptedMessage msg = new EncryptedMessage();
			msg.setEncryptedText(parser.nextText());
			ext = msg;
		} else if (parser.getNamespace().equals("jabber:x:signed")) {
			SignedPresence pr = new SignedPresence();
			pr.setSignedStanza(parser.nextText());
			ext = pr;
		}

		return ext;
	}

	public static String removeHeaderFooter(String signed) {
		if (signed.indexOf(" ") < 0) {
			return signed;
		}
	
		int begin = -1;
		if ((begin = signed.lastIndexOf("\r\n\r\n")) < 0) {
			if ((begin = signed.lastIndexOf("\n\n")) < 0) {
				return signed;
			} else {
				begin += 2;
			}
		} else {
			begin += 4;
		}
	
		int end = -1;
		if ((end = signed.indexOf("\r\n-----", begin)) < 0) {
			if ((end = signed.indexOf("\n-----", begin)) < 0) {
				return signed;
			}
		}
	
		if (end <= begin) {
			return signed;
		}
	
		return signed.substring(begin, end);
	}
	
	public Provider getProvider() {
		if (bcProvider == null) {
			bcProvider = new BouncyCastleProvider();
		}
		
		return bcProvider;
	}

	/**
	 * @return the myKey
	 */
	public String getMyKey() {
		return myKey;
	}

	/**
	 * @param myKey the myKey to set
	 */
	public void setMyKey(String myKey) {
		this.myKey = myKey;
	}

	/**
	 * @return the myKeyPw
	 */
	public String getMyKeyPw() {
		return myKeyPw;
	}

	/**
	 * @param myKeyPw the myKeyPw to set
	 */
	public void setMyKeyPw(String myKeyPw) {
		this.myKeyPw = myKeyPw;
	}

	/**
	 * @return the keyStorage
	 */
	public Map<String, String> getKeyStorage() {
		return keyStorage;
	}
}
