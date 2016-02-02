package com.pillows.saver;

import android.os.Environment;
import android.util.Log;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.Settings;
import com.pillows.encryption.Encryptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.pillows.accountsafe.Settings.DB_DIR;
import static com.pillows.accountsafe.Settings.DB_FILE_NAME;

/**
 * Created by agudz on 06/01/16.
 */
public class DataSaver {

    public static synchronized boolean serialize(List<AccountDetails> items) {
        return serialize(items, Environment.getExternalStorageDirectory());
    }

    /**
     * Serialize large tests map to file.
     *
     * @param items     large tests map
     * @param cacheFile path to serialization file
     * @return true if serialization was successful
     */
    public static synchronized boolean serialize(List<AccountDetails> items, File cacheFile) {
        File dbDir = new File(cacheFile.getPath() + "/" + DB_DIR);
        dbDir.mkdirs();
        File dbFile = new File(cacheFile.getPath() + "/" + DB_DIR + "/" + DB_FILE_NAME);

        try {
            Encryptor.secureDelete(dbFile);
        } catch (IOException e) {
            Log.w(Settings.TAG, "Exception on deleting. " + e.getMessage());
        }

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dbFile))) {
            out.writeObject(items);
        } catch (IOException e) {
            Log.e(Settings.TAG, "Exception on serializing. " + e.getMessage());
            return false;
        }
        return true;
    }

    public static List deserialize() {
        return deserialize(Environment.getExternalStorageDirectory());
    }

    /**
     * Deserialize large tests map from file.
     *
     * @param cacheFile path to cache file
     * @return large tests map
     */
    public static List deserialize(File cacheFile) {

        File dbFile = new File(cacheFile.getPath() + "/" + DB_DIR + "/" + DB_FILE_NAME);

        List<AccountDetails> holder = new ArrayList<AccountDetails>();
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(dbFile))) {
            holder = (ArrayList) stream.readObject();
        } catch (Exception e) {
            Log.w(Settings.TAG, "Exception on deserializing. " + e.getMessage());
        }
        return holder;
    }
}
