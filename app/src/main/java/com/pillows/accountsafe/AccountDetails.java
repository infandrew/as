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

    private boolean encrypted = false;
    private String name;
    private String password;
    private String login;
    private long watchTime = 0;
    private byte[] lard = new byte[LARD_SIZE];
    private boolean collapsed = false;
    private boolean dragged = false;

    /**
     * Constructor
     */
    public AccountDetails(String name, String login, String password) {
        this.login = login;
        this.name = name;
        this.password = password;
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
            collapsed = true;
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

    public boolean isCollapsed() {
        return collapsed;
    }

    public boolean isDragged() {
        return dragged;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public void setDragged(boolean dragged) {
        this.dragged = dragged;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setName(String name) {
        this.name = name;
    }
}
