package com.datainfo.remoteshell.common.util;

import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Properties;

public class CipherUtil {

    private static String readKeyFromConfigFile() {
        String fileName = "src/main/resources/cipherKey";   // TODO: 배포시 경로 맞게 설정
        Properties prop = new Properties();
        try(InputStream input = Files.newInputStream(Paths.get(fileName))) {
            prop.load(input);
            return prop.getProperty("key");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * AES128 get Key
     * */
    public static Key getAESKey() {
        Key keySpec;

        String key = readKeyFromConfigFile();
        byte[] keyBytes = new byte[16];
        byte[] b = key.getBytes(StandardCharsets.UTF_8);

        int len = b.length;
        if (len > keyBytes.length) {
            len = keyBytes.length;
        }

        System.arraycopy(b, 0, keyBytes, 0, len);
        keySpec = new SecretKeySpec(keyBytes, "AES");

        return keySpec;
    }

    /**
     * AES128 암호화
     * */
    public static String encAES(String str) throws Exception {
        Key keySpec = getAESKey();
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = c.doFinal(str.getBytes(StandardCharsets.UTF_8));

        return new String(Base64.encodeBase64(encrypted));
    }

    /**
     * AES128 복호화
     * */
    public static String decAES(String enStr) throws Exception {
        Key keySpec = getAESKey();
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] byteStr = Base64.decodeBase64(enStr.getBytes(StandardCharsets.UTF_8));

        return new String(c.doFinal(byteStr), StandardCharsets.UTF_8);
    }

    public static String decAES(String enStr, String defaultStr) {
        try {
            return decAES(enStr);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultStr;
        }
    }

    /**
     * SHA256 암호화
     * */
    public static String encrypt(String planText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(planText.getBytes());
            byte[] byteData = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte byteDatum : byteData) {

                String hex = Integer.toHexString(0xff & byteDatum);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

}
