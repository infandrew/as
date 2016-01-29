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
public class EncProgressTask extends AsyncTask<Void, Integer, Void> {
    private ProgressDialog dialog;
    private String key;
    private Activity activity;
    private ListView listView;
    private MainArrayAdapter adapter;
    private int encryptedCount = 0;

    public EncProgressTask(Activity activity, String key) {
        this.activity = activity;
        this.key = key;
        listView = (ListView) activity.findViewById(R.id.listview);
        adapter = (MainArrayAdapter) listView.getAdapter();
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (isCancelled()) return null;

        publishProgress(0);

        List<AccountDetails> items = adapter.getItems();

        Encryptor enc = new Encryptor(key);

        int i = 0;

        for (AccountDetails item : items)
            if (!item.isEncrypted()) {
                item.encrypt(enc);
                if (item.isEncrypted())
                    encryptedCount++;
                publishProgress(++i);
                if (isCancelled()) break;
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
        int itemsToEncrypt = Trash.getNotEncryptedCount(items);

        dialog = new ProgressDialog(activity);

        if (itemsToEncrypt == 0) {
            Snackbar.make(listView, R.string.snake_no_account_to_lock, Snackbar.LENGTH_LONG).show();
            cancel(true);
            return;
        }

        if (!Trash.checkPreviousEncryption(items, key)) {
            Snackbar.make(listView, R.string.snake_wrong_key, Snackbar.LENGTH_LONG).show();
            cancel(true);
            return;
        }

        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage(activity.getString(R.string.message_wait_encryption));
        dialog.setIndeterminate(false);
        dialog.setMax(itemsToEncrypt);
        dialog.setCancelable(false);
        dialog.show();
        dialog.setProgress(0);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        dialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        Snackbar.make(listView,
                String.format(activity.getString(R.string.snake_successfully_enc), encryptedCount),
                Snackbar.LENGTH_LONG).show();
        adapter.notifyDataSetChanged();
        DataSaver.serialize(adapter.getItems(), activity.getCacheDir());
        dialog.dismiss();
    }

}
