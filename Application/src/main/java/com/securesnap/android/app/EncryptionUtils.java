package com.securesnap.android.app;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;

/**
 * Created by gabi on 10/1/17.
 */

public class EncryptionUtils {
    private static final String TAG = "SecureSnapApp";

    private static final byte[] STATIC_SALT_PART = new byte[] { 0x45, -0x23, 0x3f, 0x5a, -0x12, 0x7a, 0x6f, -0x79, 0x5b, -0x6c};
    private static final String PBKD_ALGORITHM = "PBKDF2withHmacSHA1";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private static final int PBKD_ITERATIONS = 32123;

    public static String hash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return Base64.encodeToString(hash,Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String randSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[6];
        random.nextBytes(bytes);
        return Base64.encodeToString(bytes,Base64.NO_WRAP);
    }

    private static byte[] buildSalt(String b64RandSalt) {
        byte[] randSaltBytes = Base64.decode(b64RandSalt, Base64.NO_WRAP);
        byte[] saltBytes = new byte[16];
        System.arraycopy(randSaltBytes, 0, saltBytes, 0, randSaltBytes.length);
        System.arraycopy(STATIC_SALT_PART, 0, saltBytes, randSaltBytes.length, STATIC_SALT_PART.length);
        return saltBytes;
    }

    public static SecretKeySpec buildKey(String password, String b64RandSalt) {
        byte[] key = buildKeyRaw(password,b64RandSalt);
        SecretKeySpec secret = new SecretKeySpec(key, 0, key.length, "AES");
        return secret;
    }

    public static SecretKeySpec buildKey(byte[] key) {
        SecretKeySpec secret = new SecretKeySpec(key, 0, key.length, "AES");
        return secret;
    }

    public static byte[] buildKeyRaw(String password, String b64RandSalt) {
        byte[] saltBytes = buildSalt(b64RandSalt);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKD_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, PBKD_ITERATIONS, 256);
            SecretKey tmp = factory.generateSecret(spec);
            return tmp.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }



    public static void encrypt(OutputStream out, byte[] data, SecretKeySpec key) {
        SecureRandom random = new SecureRandom();
        byte ivBytes[] = new byte[16];
        random.nextBytes(ivBytes);

        try {
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            out.write(ivBytes);
            CipherOutputStream cos = new CipherOutputStream(out, cipher);
            cos.write(data);
            cos.flush();
            cos.close();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static byte[] decrypt(InputStream in, SecretKeySpec key) {
        try {
            byte ivBytes[] = new byte[16];
            int count = in.read(ivBytes, 0, 16);
            if (count != 16)
                return null;

            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            CipherInputStream cis = new CipherInputStream(in,cipher);
            byte[] data = IOUtils.toByteArray(cis);
            cis.close();
            return data;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }  catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
            return null;
        }
    }


}
