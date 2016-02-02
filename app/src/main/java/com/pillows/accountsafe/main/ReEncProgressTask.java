package com.pillows.accountsafe.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.widget.ListView;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.R;
import com.pillows.encryption.Encryptor;
import com.pillows.saver.DataSaver;
import com.pillows.tools.Trash;

import java.util.List;

/**
 * Created by agudz on 28/01/16.
 */
public class ReEncProgressTask extends AsyncTask<Void, Integer, Void> {
    private ProgressDialog dialog;
    private String oldKey;
    private String newKey;
    private Activity activity;
    private ListView listView;
    private MainArrayAdapter adapter;
    private int encryptedCount = 0;
    private boolean failed = false;

    public ReEncProgressTask(Activity activity, String oldKey, String newKey) {
        this.activity = activity;
        this.oldKey = oldKey;
        this.newKey = newKey;
        listView = (ListView) activity.findViewById(R.id.listview);
        adapter = (MainArrayAdapter) listView.getAdapter();
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (isCancelled()) return null;

        publishProgress(0);

        List<AccountDetails> items = adapter.getItems();

        Encryptor enc = new Encryptor(oldKey);

        int i = 0;

        for (AccountDetails item : items)
            if (item.isEncrypted()) {
                String login = item.decryptLogin(enc);
                String password = item.decryptPassword(enc);
                if (login != null && password != null) {
                    item.setEncrypted(false);
                    item.setLogin(login);
                    item.setPassword(password);
                } else {
                    failed = true;
                    return null;
                }
                if (isCancelled()) return null;
            }

        enc = new Encryptor(newKey);

        for (AccountDetails item : items)
            if (!item.isEncrypted()) {
                item.encrypt(enc);
                if (item.isEncrypted()) {
                    encryptedCount++;
                } else {
                    failed = true;
                    return null;
                }
                if (isCancelled()) return null;
            }

        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        adapter.notifyDataSetChanged();
        dialog.dismiss();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        List<AccountDetails> items = adapter.getItems();
        int itemsEncrypted = Trash.getEncryptedCount(items);

        dialog = new ProgressDialog(activity);

        if (itemsEncrypted == 0) {
            Snackbar.make(listView, R.string.snake_no_account_locked, Snackbar.LENGTH_LONG).show();
            cancel(true);
            return;
        }

        if (!Trash.checkPreviousEncryption(items, oldKey)) {
            Snackbar.make(listView, R.string.snake_wrong_key, Snackbar.LENGTH_LONG).show();
            cancel(true);
            return;
        }

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(activity.getString(R.string.message_wait_encryption));
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (failed)
            Snackbar.make(listView,
                String.format(activity.getString(R.string.snake_failed_reenc), encryptedCount),
                Snackbar.LENGTH_LONG).show();
        else
            Snackbar.make(listView,
                String.format(activity.getString(R.string.snake_successfully_enc), encryptedCount),
                Snackbar.LENGTH_LONG).show();
        adapter.notifyDataSetChanged();
        DataSaver.serialize(adapter.getItems());
        dialog.dismiss();
    }
}
