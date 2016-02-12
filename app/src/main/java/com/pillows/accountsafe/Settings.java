package com.pillows.accountsafe;

/**
 * Created by agudz on 11/01/16.
 */
final public class Settings {

    public static final boolean DEBUG = false;

    public static final String TAG = "AccountSafe";
    public static final String DB_DIR = "AccountSafe";
    public static final String DB_FILE_NAME = "AccountSafe.db";
    public static final String TEST_KEY = "Awertwertwergsdfgsdfgz";

    public static final String ACTION_GET = "get";
    public static final String ACTION_NEW = "new";
    public static final String ACTION_NOTHING = "nothing";

    public static final int CLIPBOARD_HISTORY_SIZE = 25;
    public static final int LARD_SIZE = 32;
    public static final int COLLAPSE_TIME = 228;
    public static final int ACTION_CHANNEL = 228;
    public static final int CALL_GEAR_DELAY = 120000;
    public static final int ACCOUNT_WATCH_TIME = 120000;

    public static final byte[] WATERMARK = new byte[] {0x73,0x61,0x66,0x65,0x30,0x30,0x30,0x31};
}
