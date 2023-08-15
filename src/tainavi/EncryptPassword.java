package tainavi;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptPassword {

	private static final String encKey = "TheWorldofNull-A";
	private static final SecretKey makeKey = new SecretKeySpec(encKey.getBytes(), "AES");
	
	// 暗号化
	public static byte[] enc(String src) {
		try {
		    Cipher cipher = Cipher.getInstance(makeKey.getAlgorithm()+"/ECB/PKCS5Padding");
		    cipher.init(Cipher.ENCRYPT_MODE, makeKey);
			return cipher.doFinal(src.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// 復号化
	public static String dec(byte[] src) {
		try {
		    Cipher cipher = Cipher.getInstance(makeKey.getAlgorithm()+"/ECB/PKCS5Padding");
		    cipher.init(Cipher.DECRYPT_MODE, makeKey);
			return new String(cipher.doFinal(src));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}	
}
