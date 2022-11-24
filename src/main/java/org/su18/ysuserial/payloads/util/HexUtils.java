package org.su18.ysuserial.payloads.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author su18
 */
public class HexUtils {

	public static byte[] toByteArray(InputStream in) throws IOException {
		byte[] classBytes;
		classBytes = new byte[in.available()];
		in.read(classBytes);
		in.close();
		return classBytes;
	}

	public static String bytesToHexString(byte[] bArray, int length) {
		StringBuffer sb = new StringBuffer(length);

		for (int i = 0; i < length; ++i) {
			String sTemp = Integer.toHexString(255 & bArray[i]);
			if (sTemp.length() < 2) {
				sb.append(0);
			}

			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}

	public static String generatePassword(String password) {
		String md5Str = md5(password);
		if (md5Str != null) {
			return md5Str.substring(0, 16).toLowerCase();
		}

		// 如果生成出错，则使用 p@ssw0rd
		return "f359740bd1cda994";
	}

	public static String md5(String s) {
		String ret = null;
		try {
			java.security.MessageDigest m;
			m = java.security.MessageDigest.getInstance("MD5");
			m.update(s.getBytes(), 0, s.length());
			ret = new java.math.BigInteger(1, m.digest()).toString(16).toUpperCase();
		} catch (Exception ignored) {
		}
		return ret;
	}
}
