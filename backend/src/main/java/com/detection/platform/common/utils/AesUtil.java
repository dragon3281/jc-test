package com.detection.platform.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于敏感数据加密(如服务器密码、代理密码等)
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${aes.secret-key:detection-platform-aes-key-2024-32}")
    private String secretKey;

    /**
     * 加密
     *
     * @param content 明文内容
     * @return Base64编码的密文
     */
    public String encrypt(String content) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * 解密
     *
     * @param encryptedContent Base64编码的密文
     * @return 明文内容
     */
    public String decrypt(String encryptedContent) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedContent);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 获取密钥字节数组(确保32字节用于AES-256)
     */
    private byte[] getKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32]; // AES-256需要32字节
        System.arraycopy(keyBytes, 0, result, 0, Math.min(keyBytes.length, 32));
        return result;
    }

    /**
     * 批量加密
     *
     * @param contents 明文数组
     * @return 密文数组
     */
    public String[] encryptBatch(String[] contents) {
        if (contents == null) {
            return new String[0];
        }
        String[] results = new String[contents.length];
        for (int i = 0; i < contents.length; i++) {
            results[i] = encrypt(contents[i]);
        }
        return results;
    }

    /**
     * 批量解密
     *
     * @param encryptedContents 密文数组
     * @return 明文数组
     */
    public String[] decryptBatch(String[] encryptedContents) {
        if (encryptedContents == null) {
            return new String[0];
        }
        String[] results = new String[encryptedContents.length];
        for (int i = 0; i < encryptedContents.length; i++) {
            results[i] = decrypt(encryptedContents[i]);
        }
        return results;
    }
}
