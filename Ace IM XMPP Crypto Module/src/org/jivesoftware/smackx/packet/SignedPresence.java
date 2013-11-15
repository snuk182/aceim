package org.jivesoftware.smackx.packet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Iterator;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.EncryptionUtils;
import org.jivesoftware.smackx.provider.EncryptedDataProvider;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

public class SignedPresence implements PacketExtension {

	private String signedStanza;

	@Override
	public String getNamespace() {
		return "jabber:x:signed";
	}

	@Override
	public String getElementName() {
		return "x";
	}

	@Override
	public String toXML() {
		StringBuilder buf = new StringBuilder();
		buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");
		buf.append(signedStanza);
		buf.append("</").append(getElementName()).append(">");
		return buf.toString();
	}

	public void signAndSet(String stanza, String keyPath, String keyPwd) throws XMPPException {
		try {
			signedStanza = sign(stanza, keyPath, keyPwd.toCharArray());
		} catch (Exception e) {
			throw new XMPPException(e);
		}
	}

	private static String sign(String stanza, String keyPath, char[] pass) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, PGPException, SignatureException {
		if (stanza == null) {
			stanza = "";
		}

		PGPSecretKey pgpSecKey = EncryptionUtils.readSecretKey(keyPath);
		PGPPrivateKey pgpPrivKey = pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("SC").build(pass));
		PGPSignatureGenerator sGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(pgpSecKey.getPublicKey().getAlgorithm(), PGPUtil.SHA1).setProvider("SC"));
		PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();

		sGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, pgpPrivKey);

		Iterator it = pgpSecKey.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			spGen.setSignerUserID(false, (String) it.next());
			sGen.setHashedSubpackets(spGen.generate());
		}

		InputStream fIn = new BufferedInputStream(new ByteArrayInputStream(stanza.getBytes()));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArmoredOutputStream aOut = new ArmoredOutputStream(out);

		aOut.beginClearText(PGPUtil.SHA1);

		//
		// note the last \n/\r/\r\n in the file is ignored
		//
		ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
		int lookAhead = readInputLine(lineOut, fIn);

		processLine(aOut, sGen, lineOut.toByteArray());

		if (lookAhead != -1) {
			do {
				lookAhead = readInputLine(lineOut, lookAhead, fIn);

				sGen.update((byte) '\r');
				sGen.update((byte) '\n');

				processLine(aOut, sGen, lineOut.toByteArray());
			} while (lookAhead != -1);
		}

		fIn.close();

		aOut.endClearText();

		BCPGOutputStream bOut = new BCPGOutputStream(aOut);

		sGen.generate().encode(bOut);

		aOut.close();

		String signed = new String(out.toByteArray());

		bOut.close();

		return EncryptedDataProvider.removeHeaderFooter(signed);

	}

	public String verifyAndGet(String buddyKey) throws XMPPException {
		try {
			return verify(signedStanza, buddyKey);
		} catch (Exception e) {
			throw new XMPPException(e);
		}
	}

	private static String verify(String signedStanza, String keyIn) throws Exception {
		InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(signedStanza.getBytes()));

		PGPPublicKeyRingCollection pgpRing = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(new FileInputStream(keyIn)));

		PGPPublicKey key;
		PGPObjectFactory pgpFact = new PGPObjectFactory(in);
		Object obj = pgpFact.nextObject();

		if (obj instanceof PGPCompressedData) {
			PGPCompressedData c1 = (PGPCompressedData) obj;
			pgpFact = new PGPObjectFactory(c1.getDataStream());
			obj = pgpFact.nextObject();
		}

		if (obj instanceof PGPOnePassSignatureList) {
			PGPOnePassSignatureList p1 = (PGPOnePassSignatureList) obj;
			PGPOnePassSignature ops = p1.get(0);
			key = pgpRing.getPublicKey(ops.getKeyID());

			PGPLiteralData p2 = (PGPLiteralData) pgpFact.nextObject();

			InputStream dIn = p2.getInputStream();
			int ch;

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			ops.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), key);

			while ((ch = dIn.read()) >= 0) {
				ops.update((byte) ch);
				out.write(ch);
			}

			String sout = new String(out.toByteArray());

			out.close();

			PGPSignatureList p3 = (PGPSignatureList) pgpFact.nextObject();

			if (ops.verify(p3.get(0))) {
				return sout;
			} else {
				return null;
			}
		}

		if (obj instanceof PGPSignatureList) {
			PGPSignatureList p1 = (PGPSignatureList) obj;
			PGPSignature s1 = p1.get(0);
			key = pgpRing.getPublicKey(s1.getKeyID());

			s1.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), key);

			if (s1.verify()) {
				return "ok";
			} else {
				return null;
			}
		}

		return null;
	}

	private static int readInputLine(ByteArrayOutputStream bOut, InputStream fIn) throws IOException {
		bOut.reset();

		int lookAhead = -1;
		int ch;

		while ((ch = fIn.read()) >= 0) {
			bOut.write(ch);
			if (ch == '\r' || ch == '\n') {
				lookAhead = readPassedEOL(bOut, ch, fIn);
				break;
			}
		}

		return lookAhead;
	}

	private static int readInputLine(ByteArrayOutputStream bOut, int lookAhead, InputStream fIn) throws IOException {
		bOut.reset();

		int ch = lookAhead;

		do {
			bOut.write(ch);
			if (ch == '\r' || ch == '\n') {
				lookAhead = readPassedEOL(bOut, ch, fIn);
				break;
			}
		} while ((ch = fIn.read()) >= 0);

		if (ch < 0) {
			lookAhead = -1;
		}

		return lookAhead;
	}

	private static int readPassedEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn) throws IOException {
		int lookAhead = fIn.read();

		if (lastCh == '\r' && lookAhead == '\n') {
			bOut.write(lookAhead);
			lookAhead = fIn.read();
		}

		return lookAhead;
	}

	private static void processLine(PGPSignature sig, byte[] line) throws SignatureException, IOException {
		int length = getLengthWithoutWhiteSpace(line);
		if (length > 0) {
			sig.update(line, 0, length);
		}
	}

	private static void processLine(OutputStream aOut, PGPSignatureGenerator sGen, byte[] line) throws SignatureException, IOException {
		// note: trailing white space needs to be removed from the end of
		// each line for signature calculation RFC 4880 Section 7.1
		int length = getLengthWithoutWhiteSpace(line);
		if (length > 0) {
			sGen.update(line, 0, length);
		}

		aOut.write(line, 0, line.length);
	}

	private static int getLengthWithoutSeparatorOrTrailingWhitespace(byte[] line) {
		int end = line.length - 1;

		while (end >= 0 && isWhiteSpace(line[end])) {
			end--;
		}

		return end + 1;
	}

	private static boolean isLineEnding(byte b) {
		return b == '\r' || b == '\n';
	}

	private static int getLengthWithoutWhiteSpace(byte[] line) {
		int end = line.length - 1;

		while (end >= 0 && isWhiteSpace(line[end])) {
			end--;
		}

		return end + 1;
	}

	private static boolean isWhiteSpace(byte b) {
		return isLineEnding(b) || b == '\t' || b == ' ';
	}

	public String getSignedStanza() {
		return signedStanza;
	}

	public void setSignedStanza(String signedStanza) {
		this.signedStanza = signedStanza;
	}
}
