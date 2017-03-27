package com.bytegriffin.get4j.util;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * MD5生成器
 */
public class MD5Util {

	private static String[] chars = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
			"o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8",
			"9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z" };

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n = 256 + n;
		int d1 = n / 16;
		int d2 = n % 16;
		return chars[d1] + chars[d2];
	}

	private static String byteArrayToHexString(byte[] b) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			resultSb.append(byteToHexString(b[i]));
		}
		return resultSb.toString().substring(0, 8);
	}

	/**
	 * 8位短uuid
	 * @param salt
	 * @return
	 */
	public synchronized static String generateID(String salt) {
		java.security.MessageDigest md;
		String pwd = null;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] b = salt.getBytes("UTF-8");
			byte[] hash = md.digest(b);
			String str = byteArrayToHexString(hash);
			return str;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return pwd;
	}

	/**
	 * 8位短uuid
	 * @return
	 */
	public synchronized static String generateID() {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(chars[x % 0x3E]);
		}
		return shortBuffer.toString();
	}
	
	
	public synchronized static String uuid() {
		return UUID.randomUUID().toString();
	}
	

}
