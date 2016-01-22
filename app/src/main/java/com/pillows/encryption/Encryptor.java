package com.pillows.encryption;

import android.util.Base64;
import android.util.Log;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.Settings;

import org.apache.commons.io.FileUtils;

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
            SecretKeySpec secretKeySpec = new SecretKeySpec(generate128ByteKey(key, provider), "AES");
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

    /**
     * Encrypt list of files
     *
     * @param files paths to files
     */
    public void encrypt(Iterable<String> files) {
        for (String file : files) {
            encrypt(file);
        }
    }

    public void encryptFileDetails(Iterable<AccountDetails> files) {
        for (AccountDetails file : files) {
            if(!file.isEncrypted() && encrypt(file.getPath())) {
                file.setEncrypted(true);
            }
        }
    }

    /**
     * Encrypt list of files
     *
     * @param files paths to files
     */
    public void decrypt(Iterable<String> files) {
        for (String file : files) {
            decrypt(file);
        }
    }

    public void decryptFileDetails(Iterable<AccountDetails> files) {
        for (AccountDetails file : files) {
            if(file.isEncrypted() && decrypt(file.getPath())) {
                file.setEncrypted(false);
            }
        }
    }

    public boolean encrypt(String file) {
        return encrypt(file, file);
    }

    public boolean decrypt(String file) {
        return decrypt(file, file);
    }

    /**
     * Encrypt one file
     *
     * @param oriPath path to file
     * @param encPath path to encrypted file
     */
    public boolean encrypt(String oriPath, String encPath) {

        boolean pathNotChanged = false;
        File oriFile = new File(oriPath);
        File encFile = new File(encPath);

        if (oriPath.equals(encPath)) {
            pathNotChanged = true;
            encPath = encPath + ".temp";
            encFile = new File(encPath);
            try {
                FileUtils.copyFile(oriFile, encFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on copying of %s -> %s", oriFile, encFile));
                return false;
            }
        }

        try (FileInputStream fis = new FileInputStream(oriPath);
             FileOutputStream fos = new FileOutputStream(encPath);
             CipherOutputStream cos = new CipherOutputStream(fos, encCipher);) {

            // some kind of watermark
            int b = 1024;
            byte[] d = new byte[b];

            // write watermark
            fos.write(WATERMARK, 0, WATERMARK.length);
            // write sha1 checksum 20-byte  (+4-byte empty)
            byte[] sha1 = sha1(oriFile);
            fos.write(sha1, 0, 32);

            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }

        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on encryption of %s. %s", oriPath, e.getMessage()));
            encFile.delete();
            return false;
        }

        try {
            if (prefSecureDelete)
                secureDelete(oriFile);
            else
                oriFile.delete();
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on deleting of %s", oriPath));
            return false;
        }

        if (pathNotChanged) {
            try {
                FileUtils.moveFile(encFile, oriFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on renaming of %s -> %s", encFile, oriFile));
                return false;
            }
        }

        return true;
    }

    /**
     * Decrypt one file
     *
     * @param oriPath path to file
     * @param decPath path to encrypted file
     */
    public boolean decrypt(String oriPath, String decPath) {

        boolean pathNotChanged = false;
        File oriFile = new File(oriPath);
        File decFile = new File(decPath);

        if (oriPath.equals(decPath)) {
            pathNotChanged = true;
            decPath = decPath + ".temp";
            decFile = new File(decPath);
            try {
                FileUtils.copyFile(oriFile, decFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on copying of %s -> %s", oriFile, decFile));
                return false;
            }
        }

        byte[] sha1 = new byte[32];
        byte[] expectedSha1 = new byte[32];

        try (FileInputStream fis = new FileInputStream(oriPath);
             FileOutputStream fos = new FileOutputStream(decPath);
             CipherOutputStream cos = new CipherOutputStream(fos, decCipher);) {

            byte[] watermarkCheck = new byte[WATERMARK.length];
            fis.read(watermarkCheck);

            if (!Arrays.equals(watermarkCheck, WATERMARK))
                throw new Exception("Can't find watermark");

            fis.read(sha1);

            int b = 1024;
            byte[] d = new byte[b];
            while ((b = fis.read(d)) != -1)
                cos.write(d, 0, b);

        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on decryption of %s. %s", oriPath, e.getMessage()));
            decFile.delete();
            return false;
        }

        try {
            expectedSha1 = sha1(decFile);
            if (!Arrays.equals(sha1, expectedSha1)) {
                throw new Exception("Checksum not matched");
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on decryption of %s. %s", oriPath, e.getMessage()));
            return false;
        }

        oriFile.delete();

        if (pathNotChanged) {
            try {
                FileUtils.moveFile(decFile, oriFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on renaming of %s -> %s", decFile, oriFile));
                return false;
            }
        }

        return true;
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

    public static byte[] generate128ByteKey(String baseString) throws Exception {
        return generate128ByteKey(baseString, DEFAULT_PROVIDER);
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
     * Generates 128-byte key from string
     *
     * @param baseString base string to build key
     * @return 128-byte array that represent key
     * @throws Exception
     */
    public static byte[] generate128ByteKey(String baseString, String provider) throws Exception {
        // init random
        byte[] keyStart = baseString.getBytes("UTF-8");
        SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", provider);
        rand.setSeed(keyStart);

        // init key
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128, rand);
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
