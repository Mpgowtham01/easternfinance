package com.nse.utils;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryptionUtilV2 {

    private static String encrypt(String plainText, String passphrase) throws Exception {

        // Generate random salt and IV
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Derive the key using PBKDF2 (CryptoJS default: HmacSHA1, 1000 iterations, 128-bit key)
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 1000, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        // Encrypt using AES/CBC/PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secret, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Compose like CryptoJS: iv::salt::ciphertext (all Base64-encoded)
        String encodedIv = bytesToHex(iv);
        String encodedSalt = bytesToHex(salt);
        String encodedCipherText = Base64.getEncoder().encodeToString(encrypted);

        String aesPassword = encodedIv + "::" + encodedSalt + "::" + encodedCipherText;
        return Base64.getEncoder().encodeToString(aesPassword.getBytes("UTF-8"));
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
    public static String base64EncodedAuth(String nse_secret_key, String nse_license_key, String nse_userid) throws Exception {

        int randomNumber = (int)(Math.random() * 10000000000L);
        String plainText = nse_secret_key + "|" + randomNumber;
        String encryptedPassword = encrypt(plainText, nse_license_key);
        System.out.println("encryptedPassword: " + encryptedPassword);

        String combinedloginUserIdEncryptedPassword = nse_userid + ":" + encryptedPassword;

        String auth= Base64.getEncoder().encodeToString(combinedloginUserIdEncryptedPassword.getBytes());

        return auth;
    }
}