package com.pillows.encryption;

/**
 * Created by agudz on 20/01/16.
 */
public class WrongKeyException extends Exception {

    private static String message = "Wrong key used to decrypt password";

    public WrongKeyException() {
        super(message);
    }
}
