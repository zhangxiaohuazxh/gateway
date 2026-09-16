package cn.hubbo.utils;

import lombok.Getter;

public class CipherUtils {

	@Getter
	public enum SignType {
		NONE("none"),
		RSA("rsa"),
		AES("aes");
		private final String value;

		SignType(String value) {
			this.value = value;
		}
	}

	public static byte[] decrypt(SignType signType, String key, String nonce, byte[] originalBytes) {
		return new byte[]{};
	}


}
