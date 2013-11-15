package org.jivesoftware.smackx.packet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Iterator;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.EncryptionUtils;
import org.jivesoftware.smackx.provider.EncryptedDataProvider;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.spongycastle.util.io.Streams;

public class EncryptedMessage implements PacketExtension {
	
	private String encryptedText;

	@Override
	public String getElementName() {
		return "x";
	}

	@Override
	public String getNamespace() {
		return "jabber:x:encrypted";
	}

	@Override
	public String toXML() {
		StringBuilder buf = new StringBuilder();
		buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");
		buf.append(encryptedText);
		buf.append("</").append(getElementName()).append(">");
		return buf.toString();
	}

	public String getEncryptedText() {
		return encryptedText;
	}	
	
	public String decryptAndGet(String keyPath, String keyPwd) throws Exception {
		InputStream in = new ByteArrayInputStream(encryptedText.getBytes());
		InputStream keyIn = new BufferedInputStream(new FileInputStream(keyPath));
		String out = decrypt(in, keyIn, keyPwd.toCharArray());
		keyIn.close();
		in.close();
		
		return out;
	}

	private static String decrypt(InputStream in, InputStream keyIn, char[] passwd) throws IOException, NoSuchProviderException, PGPException {
		in = PGPUtil.getDecoderStream(in);

		try {
			PGPObjectFactory pgpF = new PGPObjectFactory(in);
			PGPEncryptedDataList enc;

			Object o = pgpF.nextObject();
			//
			// the first object might be a PGP marker packet.
			//
			if (o instanceof PGPEncryptedDataList) {
				enc = (PGPEncryptedDataList) o;
			} else {
				enc = (PGPEncryptedDataList) pgpF.nextObject();
			}

			//
			// find the secret key
			//
			@SuppressWarnings("rawtypes")
			Iterator it = enc.getEncryptedDataObjects();
			PGPPrivateKey sKey = null;
			PGPPublicKeyEncryptedData pbe = null;
			PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));

			while (sKey == null && it.hasNext()) {
				pbe = (PGPPublicKeyEncryptedData) it.next();

				sKey = EncryptionUtils.findSecretKey(pgpSec, pbe.getKeyID(), passwd);
			}

			if (sKey == null) {
				throw new IllegalArgumentException("secret key for message not found.");
			}

			InputStream clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("SC").build(sKey));

			PGPObjectFactory plainFact = new PGPObjectFactory(clear);

			Object message = plainFact.nextObject();

			if (message instanceof PGPCompressedData) {
				PGPCompressedData cData = (PGPCompressedData) message;
				PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream());

				message = pgpFact.nextObject();
			}

			if (message instanceof PGPLiteralData) {
				PGPLiteralData ld = (PGPLiteralData) message;

				
				InputStream unc = ld.getInputStream();
				ByteArrayOutputStream fOut = new ByteArrayOutputStream();

				Streams.pipeAll(unc, fOut);
				
				byte[] bytes = fOut.toByteArray();
				fOut.close();
				
				return new String(bytes);				
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("encrypted message contains a signed message - not literal data.");
			} else {
				throw new PGPException("message is not a simple encrypted file - type unknown.");
			}

			/*if (pbe.isIntegrityProtected()) {
				if (!pbe.verify()) {
					System.err.println("message failed integrity check");
				} else {
					System.err.println("message integrity check passed");
				}
			} else {
				System.err.println("no message integrity check");
			}*/
		} catch (PGPException e) {
			if (e.getUnderlyingException() != null) {
				e.getUnderlyingException().printStackTrace();
			}
			
			throw e;
		}
	}
	
	public void setAndEncrypt(String message, String buddyKey) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PGPPublicKey encKey = EncryptionUtils.readPublicKey(buddyKey);
		encrypt(out, message, encKey, true, false);
		
		encryptedText = EncryptedDataProvider.removeHeaderFooter(new String(out.toByteArray()));
		
		out.close();
	}

	private static void encrypt(OutputStream out, String message, PGPPublicKey encKey, boolean armor, boolean withIntegrityCheck) throws IOException, NoSuchProviderException {
		out = new ArmoredOutputStream(out);
		
		try {
			byte[] bytes = compressFile(message.getBytes(), CompressionAlgorithmTags.ZLIB);

			PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("SC"));

			encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("SC"));

			OutputStream cOut = encGen.open(out, bytes.length);

			cOut.write(bytes);
			cOut.close();

			if (armor) {
				out.close();
			}
		} catch (PGPException e) {
			System.err.println(e);
			if (e.getUnderlyingException() != null) {
				e.getUnderlyingException().printStackTrace();
			}
		}
	}

	static byte[] compressFile(byte[] data, int algorithm) throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        /*PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
        PGPUtil.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY,
            new File(fileName));*/
        
        EncryptionUtils.writeToLiteralData(new ByteArrayInputStream(data), bOut, PGPLiteralData.BINARY, "");
        
        byte[] bbytes = bOut.toByteArray();
        
        bOut.close();
        return bbytes;
    }

	public void setEncryptedText(String encryptedText) {
		this.encryptedText = encryptedText;
	}

    
}
