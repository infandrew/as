package com.pillows.encryption;

import android.util.Base64;
import android.util.Log;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.Settings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static com.pillows.accountsafe.Settings.*;

/**
 * Created by agudz on 31/12/15.
 */
public class Encryptor {

    private static final String DEFAULT_PROVIDER = "Crypto";
    private static final String HASH_ALGO = "SHA-256";
    private static final int HASH_ALGO_LEN = 32;

    private Cipher encCipher = null;
    private Cipher decCipher = null;
    private boolean prefSecureDelete = true;

    /**
     * Constructor
     *
     * @param key string key
     */
    public Encryptor(String key) {
        this(key, DEFAULT_PROVIDER, true);
    }

    /**
     * Constructor
     *
     * @param key string key
     */
    public Encryptor(String key, boolean prefSecureDelete) {
        this(key, DEFAULT_PROVIDER, prefSecureDelete);

    }

    /**
     * Constructor
     *
     * @param key string key
     */
    public Encryptor(String key, String provider) {
        this(key, provider, true);

    }

    /**
     * Constructor
     *
     * @param key
     */
    public Encryptor(String key, String provider, boolean prefSecureDelete) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(generate256ByteKey(key, provider), "AES");
            encCipher = Cipher.getInstance("AES");
            encCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            decCipher = Cipher.getInstance("AES");
            decCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, String.format("Failed to init Encryptor"));
        }

        this.prefSecureDelete = prefSecureDelete;
    }

    public byte[] encrypt(byte[] bytes)
    {
        try {
            return encCipher.doFinal(bytes);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to encrypt byte array. %s", e.getMessage()));
        }
        return new byte[256];
    }

    public byte[] decrypt(byte[] bytes)
    {
        try {
            return decCipher.doFinal(bytes);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to decrypt byte array. %s", e.getMessage()));
        }
        return new byte[256];
    }

    public String encryptPass(String pw, byte[] lard)
    {
        byte[] hash = sha256(pw);
        byte[] fullhash = concatArrays(hash, lard);
        fullhash = concatArrays(fullhash, pw.getBytes());
        byte[] encpw = encrypt(fullhash);
        String encBase64 = Base64.encodeToString(encpw, Base64.DEFAULT);
        return encBase64;
    }

    public String decryptPass(String encBase64) {
        byte[] fullhash = decrypt(Base64.decode(encBase64, Base64.DEFAULT));
        byte[] hash = new byte[HASH_ALGO_LEN];
        System.arraycopy(fullhash, 0, hash, 0, HASH_ALGO_LEN);

        int hash2Length = fullhash.length - HASH_ALGO_LEN - LARD_SIZE;
        byte[] hash2 = new byte[hash2Length];
        System.arraycopy(fullhash, HASH_ALGO_LEN + LARD_SIZE, hash2, 0, hash2Length);

        String result = new String(hash2);
        byte[] expectedHash = sha256(result);
        if (!Arrays.equals(hash, expectedHash))
            return null;
        return result;
    }

    public static byte[] generate256ByteKey(String baseString) throws Exception {
        return generate256ByteKey(baseString, DEFAULT_PROVIDER);
    }

    /**
     * Xor two byte arrays
     * result = first xor second
     *
     * @param first byte array
     * @param second byte array
     * @return result byte array
     */
    public static byte[] xorArrays(byte[] first, byte[] second) {
        byte[] result;
        if (first.length >= second.length) {
            result = first.clone();
            for (int i = 0; i < second.length; i++) {
                result[i] = (byte) (((int) first[i]) ^ ((int) second[i]));
            }
        } else {
            result = second.clone();
            for (int i = 0; i < first.length; i++) {
                result[i] = (byte) (((int) first[i]) ^ ((int) second[i]));
            }
        }
        return result;
    }

    public static byte[] concatArrays(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /**
     * Generates 256-byte key from string
     *
     * @param baseString base string to build key
     * @return 256-byte array that represent key
     * @throws Exception
     */
    public static byte[] generate256ByteKey(String baseString, String provider) throws Exception {
        // init random
        byte[] keyStart = baseString.getBytes("UTF-8");
        SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", provider);
        rand.setSeed(keyStart);

        // init key
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(256, rand);
        SecretKey skey = kgen.generateKey();

        return skey.getEncoded();
    }

    public static void secureDelete(File file) throws IOException {
        if (file.exists()) {
            long length = file.length();
            SecureRandom random = new SecureRandom();
            RandomAccessFile raf = new RandomAccessFile(file, "rws");
            raf.seek(0);
            raf.getFilePointer();
            byte[] data = new byte[4096];
            int pos = 0;
            while (pos < length) {
                random.nextBytes(data);
                raf.write(data);
                pos += data.length;
            }
            raf.close();
            file.delete();
        }
    }

    public static boolean checkWatermark(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);) {

                byte[] d = new byte[8];
                fis.read(d);

                if (Arrays.equals(d, Settings.WATERMARK))
                    return true;
                else
                    return false;

            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to check Watermark of %s.", filePath, e.getMessage()));
            }
        }
        return false;
    }

    public static byte[] sha1(final File file) throws Exception {
        MessageDigest messageDigest;

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            messageDigest = MessageDigest.getInstance("SHA-256");
            final byte[] buffer = new byte[1024];
            for (int read = 0; (read = is.read(buffer)) != -1;) {
                messageDigest.update(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new Exception("Failed to calculate checksum");
        }

        //byte[] result = new byte[32];
        //System.arraycopy(messageDigest.digest(),0,result,0,32);
        return messageDigest.digest();
    }

    public static byte[] sha256(String inputString) {
        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance(HASH_ALGO);
            messageDigest.update(inputString.getBytes(), 0, inputString.length());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, String.format("No such algorithm: %s. %s", HASH_ALGO, e.getMessage()));
        }
        return new byte[HASH_ALGO_LEN];
    }
}
