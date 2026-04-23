package com.example.spamdetector;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final String PREFS_NAME = "secure_sms_guard";
    private static final String KEY_ALIAS = "aes_key_b64";
    private static SecretKeySpec cachedKey = null;

    private static SharedPreferences securePrefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private static synchronized SecretKeySpec getSecretKey(Context context) throws Exception {
        if (cachedKey != null) return cachedKey;

        SharedPreferences prefs = securePrefs(context);
        String encoded = prefs.getString(KEY_ALIAS, null);
        if (encoded == null) {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            encoded = Base64.encodeToString(key, Base64.NO_WRAP);
            prefs.edit().putString(KEY_ALIAS, encoded).apply();
        }
        byte[] keyBytes = Base64.decode(encoded, Base64.NO_WRAP);
        cachedKey = new SecretKeySpec(keyBytes, "AES");
        return cachedKey;
    }

    public static String encrypt(Context context, String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(context), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            return plainText;
        }
    }

    public static String decrypt(Context context, String cipherText) {
        if (cipherText == null || cipherText.isEmpty() || !cipherText.contains(":")) {
            return cipherText == null ? "" : cipherText;
        }
        try {
            String[] parts = cipherText.split(":", 2);
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(context), new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }
}
