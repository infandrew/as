package com.pillows.tools;

/**
 * Created by agudz on 21/01/16.
 */
public class PasswordHelper {
    protected PasswordHelper() {}

    public static String getMask(int stringLength) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringLength; i++)
            sb.append("*");
        return sb.toString();
    }
}
