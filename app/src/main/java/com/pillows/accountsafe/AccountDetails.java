package com.pillows.accountsafe;

import android.util.Log;

import com.pillows.encryption.Encryptor;

import java.io.Serializable;
import java.security.SecureRandom;

import static com.pillows.accountsafe.Settings.*;

/**
 * Created by agudz on 11/01/16.
 */
public class AccountDetails implements Serializable {

    private boolean encrypted;
    private String name;
    private String password;
    private String login;
    private long watchTime;
    private byte[] lard = new byte[LARD_SIZE];

    /**
     * Constructor
     */
    public AccountDetails(String name, String login, String password) {
        this.encrypted = false;
        this.login = login;
        this.name = name;
        this.password = password;
        this.watchTime = 0;
        new SecureRandom().nextBytes(lard);
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getPath() {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getLogin() {
        return login;
    }

    public boolean encrypt(Encryptor enc) {
        if (encrypted == false) {
            login = enc.encryptPass(login, lard);
            password = enc.encryptPass(password, lard);
            encrypted = true;
            return true;
        }
        return false;
    }

    public String decryptLogin(Encryptor enc) {
        if (encrypted == true) {
            return enc.decryptPass(login);
        }
        return null;
    }

    public String decryptPassword(Encryptor enc) {
        if (encrypted == true) {
            return enc.decryptPass(password);
        }
        return null;
    }

    public long getWatchTime() {
        return watchTime;
    }

    public void setWatchTime(long watchTime) {
        this.watchTime = watchTime;
    }
}
