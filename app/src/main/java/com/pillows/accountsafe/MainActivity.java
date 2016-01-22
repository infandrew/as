package com.pillows.accountsafe;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.pillows.accessory.AccessoryCallback;
import com.pillows.accessory.AccessoryService;
import com.pillows.encryption.Encryptor;
import com.pillows.saver.DataSaver;
import com.pillows.tools.CollapseHelper;
import com.pillows.tools.PasswordHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.pillows.accountsafe.Settings.ACCOUNT_WATCH_TIME;
import static com.pillows.accountsafe.Settings.ACTION_CLOSE;
import static com.pillows.accountsafe.Settings.ACTION_NOTHING;
import static com.pillows.accountsafe.Settings.ACTION_OPEN;
import static com.pillows.accountsafe.Settings.CALL_GEAR_DELAY;
import static com.pillows.accountsafe.Settings.DEBUG;
import static com.pillows.accountsafe.Settings.TAG;
import static com.pillows.accountsafe.Settings.TEST_KEY;

public class MainActivity extends AppCompatActivity implements AccessoryCallback {

    private static final int ADD_ACCOUNT_RESULT = 1;

    private StableArrayAdapter adapter;
    private boolean mIsBound = false;
    private AccessoryService mConsumerService = null;
    private String currentAction = ACTION_NOTHING;

    /**
     * Object to establish connection between MainActivity and AccessoryService
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConsumerService = ((AccessoryService.LocalBinder) service).getService();
            mConsumerService.setCallbacks(MainActivity.this);

            Log.d(Settings.TAG, "ServiceConnected");
            mConsumerService.findPeers();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
            Log.d(Settings.TAG, "ServiceDisconnected");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Set<AccountDetails> pickedFiles = new HashSet<AccountDetails>();
        switch (requestCode) {
            case ADD_ACCOUNT_RESULT:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Bundle res = data.getExtras();
                        String name = res.getString("account_name");
                        String login = res.getString("account_login");
                        String password = res.getString("account_password");
                        pickedFiles.add(new AccountDetails(name, login, password));
                    }
                }
                break;
        }
        adapter.addAll(pickedFiles);
        DataSaver.serialize(adapter.getItems(), getCacheDir());
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mIsBound = bindService(new Intent(MainActivity.this, AccessoryService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "mIsBound: " + mIsBound);

        final List<AccountDetails> items = (List) DataSaver.deserialize(getCacheDir());

        final ListView listview = (ListView) findViewById(R.id.listview);
        listview.setEmptyView(findViewById(R.id.empty));
        adapter = new StableArrayAdapter(this, R.layout.list_item, items);

        listview.setAdapter(adapter);

        //bindButtonActions();
    }

    @Override
    protected void onDestroy() {
        // Clean up connections
        if (mIsBound && mConsumerService != null) {
            mConsumerService.closeConnection();
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
    }

    private void bindButtonActions() {
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, AddActivity.class),
                        ADD_ACCOUNT_RESULT
                );
            }
        });

        FloatingActionButton encfab = (FloatingActionButton) findViewById(R.id.encrypt_fab);
        encfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DEBUG)
                    new EncProgressTask(TEST_KEY).execute();
                else
                    callGear(ACTION_CLOSE);
            }
        });

        FloatingActionButton decfab = (FloatingActionButton) findViewById(R.id.decrypt_fab);
        decfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DEBUG)
                    new DecProgressTask(TEST_KEY).execute();
                else
                    callGear(ACTION_OPEN);
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_safe:
                if (DEBUG)
                    new EncProgressTask(TEST_KEY).execute();
                else
                    callGear(ACTION_CLOSE);
                break;

            case R.id.action_add:
                startActivityForResult(
                        new Intent(MainActivity.this, AddActivity.class),
                        ADD_ACCOUNT_RESULT
                );
                break;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.action_reconnect:
                mConsumerService.findPeers();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private class StableArrayAdapter extends ArrayAdapter<AccountDetails> {

        private List<AccountDetails> items;
        private Context context;

        public StableArrayAdapter(Context context, int viewId,
                                  List<AccountDetails> items) {
            super(context, viewId, items);
            this.items = items;
            this.context = context;
        }

        public List<AccountDetails> getItems() {
            return items;
        }

        public void addAll(Collection<? extends AccountDetails> newItems) {
            for (AccountDetails possibleItem : newItems) {
                add(possibleItem);
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_item, null);
            } else {
                view = convertView;
            }

            final AccountDetails account = items.get(position);
            final boolean encrypted = account.isEncrypted();
            long watchTime = System.currentTimeMillis() - account.getWatchTime();

            TextView listItemTitle = (TextView) view.findViewById(R.id.list_item_title);
            listItemTitle.setText(account.getName());

            ImageButton collapseBtn = (ImageButton) view.findViewById(R.id.list_item_collapse);
            final View collapsablePart = view.findViewById(R.id.list_item_collapsable);

            if (encrypted) {
                if (watchTime > ACCOUNT_WATCH_TIME) {
                    hideAccount(view);
                }
            } else {
                showAccount(view, account.getLogin(), account.getPassword());
            }

            view.setOnClickListener(new View.OnClickListener() {
                private CountDownTimer countDownTimer = null;

                @Override
                public void onClick(View v) {
                    if (encrypted) {
                        if (collapsablePart.isShown()) {
                            if (countDownTimer != null)
                                countDownTimer.cancel();
                            hideDecAccount(view);
                        } else {
                            if (DEBUG) {
                                countDownTimer = showDecAccount(TEST_KEY, view, account);
                            } else {
                                callGear(ACTION_OPEN);
                            }
                        }
                    } else {
                        if (collapsablePart.isShown()) {
                            CollapseHelper.collapse(collapsablePart);
                        } else {
                            CollapseHelper.expand(collapsablePart);
                        }
                    }
                }
            });

            return view;
        }

        private void hideDecAccount(View view) {
            view.findViewById(R.id.list_item_watchtimer).setVisibility(View.GONE);
            hideAccount(view);
            // TODO no animation
            CollapseHelper.collapse(view.findViewById(R.id.list_item_collapsable));
        }

        private void hideAccount(View view) {
            ImageButton collapseBtn = (ImageButton) view.findViewById(R.id.list_item_collapse);
            final View collapsablePart = view.findViewById(R.id.list_item_collapsable);
            collapseBtn.setImageResource(R.drawable.ic_lock);
            collapsablePart.setVisibility(View.GONE);

            // password login hide
            TextView listItemPassword = (TextView) view.findViewById(R.id.list_item_password);
            listItemPassword.setText("encrypted");
            TextView listItemLogin = (TextView) view.findViewById(R.id.list_item_login);
            listItemLogin.setText("encrypted");

            view.findViewById(R.id.list_item_copypassword).setOnClickListener(null);
            view.findViewById(R.id.list_item_copylogin).setOnClickListener(null);
            view.findViewById(R.id.list_item_showpassword).setOnClickListener(null);
            view.findViewById(R.id.list_item_loginrow).setVisibility(View.GONE);
            view.findViewById(R.id.list_item_passwordrow).setVisibility(View.GONE);
        }

        private CountDownTimer showDecAccount(String key, final View view, AccountDetails account) {
            Encryptor enc = new Encryptor(key);
            String login = account.decryptLogin(enc);
            String password = account.decryptPassword(enc);
            if (login == null || password == null) {
                Snackbar.make(view, R.string.snake_wrong_eky, Snackbar.LENGTH_LONG).show();
                return null;
            } else {
                account.setWatchTime(System.currentTimeMillis());

                final long watchTime = account.getWatchTime();
                final TextView watchTimer = (TextView) view.findViewById(R.id.list_item_watchtimer);
                watchTimer.setText("" + ACCOUNT_WATCH_TIME / 1000);
                watchTimer.setVisibility(View.VISIBLE);

                CountDownTimer countDownTimer = new CountDownTimer(ACCOUNT_WATCH_TIME, 1000) {
                    public void onTick(long millisUntilFinished) {
                        long timeToHide = ACCOUNT_WATCH_TIME - System.currentTimeMillis() + watchTime;
                        watchTimer.setText("" + timeToHide / 1000);
                    }

                    public void onFinish() {
                        watchTimer.setVisibility(View.GONE);
                        hideAccount(view);
                    }

                }.start();

                showAccount(view, login, password);
                CollapseHelper.expand(view.findViewById(R.id.list_item_collapsable));
                return countDownTimer;
            }
        }

        private void showAccount(final View view, final String login, final String password) {
            ImageButton collapseBtn = (ImageButton) view.findViewById(R.id.list_item_collapse);
            final View collapsablePart = view.findViewById(R.id.list_item_collapsable);

            collapseBtn.setImageResource(R.drawable.ic_unlock);
            collapsablePart.setVisibility(View.VISIBLE);

            final String passwordMask = PasswordHelper.getMask(password.length());

            // password setup
            view.findViewById(R.id.list_item_passwordrow).setVisibility(View.VISIBLE);
            final TextView listItemPassword = (TextView) view.findViewById(R.id.list_item_password);
            listItemPassword.setText(passwordMask);
            ImageButton listItemPasswordCopy = (ImageButton) view.findViewById(R.id.list_item_copypassword);
            listItemPasswordCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("text", password);
                    clipboard.setPrimaryClip(clip);
                    Snackbar.make(view, R.string.snake_copied, Snackbar.LENGTH_LONG).show();
                }
            });
            final ImageButton listItemPasswordShow = (ImageButton) view.findViewById(R.id.list_item_showpassword);
            listItemPasswordShow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listItemPassword.getText().toString().equals(passwordMask)) {
                        listItemPassword.setText(password);
                        listItemPasswordShow.setImageResource(R.drawable.ic_hidden);
                    } else {
                        listItemPassword.setText(passwordMask);
                        listItemPasswordShow.setImageResource(R.drawable.ic_visible);
                    }
                }
            });

            // login setup
            if (TextUtils.isEmpty(login))
                view.findViewById(R.id.list_item_loginrow).setVisibility(View.GONE);
            else {
                view.findViewById(R.id.list_item_loginrow).setVisibility(View.VISIBLE);
                TextView listItemLogin = (TextView) view.findViewById(R.id.list_item_login);
                listItemLogin.setText(login);
                ImageButton listItemLoginCopy = (ImageButton) view.findViewById(R.id.list_item_copylogin);
                listItemLoginCopy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("text", login);
                        clipboard.setPrimaryClip(clip);
                        Snackbar.make(view, R.string.snake_copied, Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        }

    }

    class EncProgressTask extends AsyncTask<Void, Integer, Void> {
        ProgressDialog dialog;
        String key;

        public EncProgressTask(String key) {
            this.key = key;
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
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Please wait for encryption");
            dialog.setIndeterminate(false);
            dialog.setMax(getEncryptedCount(items));
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
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }
    }

    private int getEncryptedCount(Iterable<? extends AccountDetails> items) {
        int result = 0;
        for (AccountDetails item : items)
            if (item.isEncrypted())
                result++;
        return result;
    }

    private void callGear(String gearAction) {
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Waiting for Gear...");
        dialog.setMessage("You have 60 sec to input safe code");
        //dialog.setIndeterminate(false);
        //dialog.setCancelable(false);
        //dialog.setMax(60);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                currentAction = ACTION_NOTHING;
            }
        });
        dialog.show();

        currentAction = gearAction;
        mConsumerService.sendData(gearAction);

        new CountDownTimer(CALL_GEAR_DELAY, 1000) {
            public void onTick(long millisUntilFinished) {
                if (currentAction.equals(ACTION_NOTHING))
                    dialog.dismiss();
            }

            public void onFinish() {
                currentAction = ACTION_NOTHING;
                dialog.dismiss();
            }
        }.start();
    }


    @Override
    public void gearResponse(String data) {
        switch (currentAction) {
            case ACTION_CLOSE:
                new EncProgressTask(data).execute();
                break;
            case ACTION_OPEN:
                // TODO ADD dec call
                break;
            default:
        }
        currentAction = ACTION_NOTHING;
    }

}
