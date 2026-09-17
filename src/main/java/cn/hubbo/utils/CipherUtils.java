package cn.hubbo.utils;

import cn.hubbo.vo.SecurityVO;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.XECPublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.Base64;
import java.util.Map;

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



    @SneakyThrows
    public static SecurityVO computeSharedSecret(String clientPublicKeyStrContent) {
        val decodeClientPubKeyBytes = Base64.getDecoder().decode(clientPublicKeyStrContent);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("XDH");
        keyPairGenerator.initialize(new NamedParameterSpec("X25519"));
        KeyPair serverKeyPair = keyPairGenerator.generateKeyPair();
        // 2. 导入前端公钥
        PublicKey clientPub = importX25519PublicKey(decodeClientPubKeyBytes);
        // 3. 计算共享密钥
        KeyAgreement ka = KeyAgreement.getInstance("XDH");
        ka.init(serverKeyPair.getPrivate());
        ka.doPhase(clientPub, true);
        byte[] sharedSecret = ka.generateSecret();
        // 4. HKDF 派生 AES-GCM 密钥（32 字节）
        byte[] aesKey = hkdfSha256(sharedSecret, new byte[0], "demo-session".getBytes());
        SecurityVO vo = new SecurityVO();
        vo.setServerPubKey(Base64.getEncoder().encodeToString(exportX25519PublicKey(serverKeyPair.getPublic())));
        vo.setSecret(Base64.getEncoder().encodeToString(aesKey));
        return vo;
    }

    /**
     * 导入 raw 32 字节的 X25519 公钥
     */
    private static PublicKey importX25519PublicKey(byte[] raw) throws Exception {
        if (raw.length != 32) {
            throw new IllegalArgumentException("Invalid X25519 public key length: " + raw.length);
        }
        byte[] reversed = new byte[32];
        for (int i = 0; i < 32; i++) {
            reversed[i] = raw[31 - i];
        }
        KeyFactory kf = KeyFactory.getInstance("XDH");
        return kf.generatePublic(new XECPublicKeySpec(NamedParameterSpec.X25519, new java.math.BigInteger(1, reversed)));
    }

    /**
     * 导出 X25519 公钥为 raw 32 字节
     */
    private static byte[] exportX25519PublicKey(PublicKey pub) throws Exception {
        XECPublicKey xec = (XECPublicKey) pub;
        byte[] bytes = xec.getU().toByteArray();
        // BigInteger 可能带符号位，补齐到 32 字节
        byte[] result = new byte[32];
        if (bytes.length > 32) {
            System.arraycopy(bytes, bytes.length - 32, result, 0, 32);
        } else {
            System.arraycopy(bytes, 0, result, 32 - bytes.length, bytes.length);
        }
        // 核心修复：转为小端序返回给前端 WebCrypto
        byte[] reversed = new byte[32];
        for (int i = 0; i < 32; i++) {
            reversed[i] = result[31 - i];
        }
        return reversed;
    }

    /**
     * 简易 HKDF-SHA256（RFC 5869）
     */
    private static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        // Extract
        mac.init(new SecretKeySpec(salt.length == 0 ? new byte[32] : salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        // Expand
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] okm = new byte[32];
        byte[] t = new byte[0];
        int pos = 0;
        byte counter = 1;
        while (pos < 32) {
            mac.update(t);
            mac.update(info);
            mac.update(counter);
            t = mac.doFinal();
            int copy = Math.min(t.length, 32 - pos);
            System.arraycopy(t, 0, okm, pos, copy);
            pos += copy;
            counter++;
        }
        return okm;
    }


    @SneakyThrows
    public static String decrypt(@NonNull String encryptedContent, @NonNull String ivStr, @NonNull String aesKey) {
        byte[] iv = Base64.getDecoder().decode(ivStr);
        byte[] ciphertext = Base64.getDecoder().decode(encryptedContent);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(Base64.getDecoder().decode(aesKey), "AES"),
                new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public static Map<String, Object> encrypt(String aesKey, String source) {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        // 2. 初始化 Cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(Base64.getDecoder().decode(aesKey), "AES"),
                new GCMParameterSpec(128, iv) // Tag 长度 128 位
        );
        byte[] ciphertext = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));
        return Map.of("iv", Base64.getEncoder().encodeToString(iv), "data", Base64.getEncoder().encodeToString(ciphertext));
    }

}
