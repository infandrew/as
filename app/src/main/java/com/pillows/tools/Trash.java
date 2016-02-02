package com.pillows.tools;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.encryption.Encryptor;

/**
 * Created by agudz on 29/01/16.
 */
public class Trash {

    public static int getNotEncryptedCount(Iterable<? extends AccountDetails> items) {
        int result = 0;
        for (AccountDetails item : items)
            if (!item.isEncrypted())
                result++;
        return result;
    }

    public static int getEncryptedCount(Iterable<? extends AccountDetails> items) {
        int result = 0;
        for (AccountDetails item : items)
            if (item.isEncrypted())
                result++;
        return result;
    }

    /**
     *
     * @param items
     * @return true if current key was used to encrypt previous one account
     */
    public static boolean checkPreviousEncryption(Iterable<? extends AccountDetails> items, String key) {
        for (AccountDetails item : items)
            if (item.isEncrypted()) {
                Encryptor enc = new Encryptor(key);
                return item.decryptLogin(enc) != null;
            }
        return true;
    }

}
