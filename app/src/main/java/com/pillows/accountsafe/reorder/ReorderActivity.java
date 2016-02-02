package com.pillows.accountsafe.reorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.R;
import com.pillows.saver.DataSaver;

import java.util.Comparator;
import java.util.List;

import static com.pillows.accountsafe.Settings.TAG;

/**
 * Created by agudz on 19/01/16.
 */
public class ReorderActivity extends AppCompatActivity {
    public static final int EDIT_ACCOUNT_RESULT = 3;

    private ReorderArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorder);
        // Set back button in top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final List<AccountDetails> items = (List) DataSaver.deserialize();

        final ListView listview = (ListView) findViewById(R.id.listview);
        listview.setEmptyView(findViewById(R.id.empty));
        adapter = new ReorderArrayAdapter(this, R.layout.list_item_reorder, items);

        listview.setAdapter(adapter);
        listview.setOnDragListener(new MyDragListener(this));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            DataSaver.serialize(adapter.getItems());
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reorder_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_ascending:
                adapter.sort(new Comparator<AccountDetails>() {
                    @Override
                    public int compare(AccountDetails acc1, AccountDetails acc2) {
                        String name1 = acc1.getName();
                        String name2 = acc2.getName();
                        return name1.compareToIgnoreCase(name2);
                    }
                });
                break;
            case R.id.action_sort_descending:
                adapter.sort(new Comparator<AccountDetails>() {
                    @Override
                    public int compare(AccountDetails acc1, AccountDetails acc2) {
                        String name1 = acc1.getName();
                        String name2 = acc2.getName();
                        return name2.compareToIgnoreCase(name1);
                    }
                });
                break;
            case android.R.id.home:
                DataSaver.serialize(adapter.getItems());
                finish();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "ReorderActivity onActivityResult");
        switch (requestCode) {
            case EDIT_ACCOUNT_RESULT:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Bundle res = data.getExtras();
                        int position = res.getInt("account_position");
                        String name = res.getString("account_name");
                        String login = res.getString("account_login");
                        String password = res.getString("account_password");
                        AccountDetails acc = adapter.getItem(position);
                        acc.setEncrypted(false);
                        acc.setLogin(login);
                        acc.setPassword(password);
                        acc.setName(name);

                        DataSaver.serialize(adapter.getItems());
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        Log.d("AccountSafe", "ReorderActivity onDestroy");
        super.onDestroy();
    }
}