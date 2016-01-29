package com.pillows.accountsafe.main;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.pillows.accessory.AccessoryCallback;
import com.pillows.accessory.AccessoryService;
import com.pillows.accountsafe.AboutActivity;
import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.AddActivity;
import com.pillows.accountsafe.CallGearAction;
import com.pillows.accountsafe.R;
import com.pillows.accountsafe.reorder.ReorderActivity;
import com.pillows.accountsafe.Settings;
import com.pillows.accountsafe.SettingsActivity;
import com.pillows.saver.DataSaver;
import com.pillows.tools.ClipboardHelper;
import com.pillows.tools.Trash;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.pillows.accountsafe.Settings.ACTION_CLOSE;
import static com.pillows.accountsafe.Settings.CALL_GEAR_DELAY;
import static com.pillows.accountsafe.Settings.DEBUG;
import static com.pillows.accountsafe.Settings.TAG;
import static com.pillows.accountsafe.Settings.TEST_KEY;

public class MainActivity extends AppCompatActivity implements AccessoryCallback {

    private static final int ADD_ACCOUNT_RESULT = 1;
    private static final int REORDER_ACCOUNT_RESULT = 2;

    private MainArrayAdapter adapter;
    private boolean mIsBound = false;
    private AccessoryService mConsumerService = null;
    private CallGearAction currentAction = CallGearAction.NOTHING;
    private ListView listview;
    private CountDownTimer gearWaitingTimer = null;

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
        Log.d(TAG, "MainActivity onActivityResult");
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

                        adapter.addAll(pickedFiles);
                        DataSaver.serialize(adapter.getItems(), getCacheDir());
                    }
                }
                break;
            case REORDER_ACCOUNT_RESULT:
                List<AccountDetails> items = (List) DataSaver.deserialize(getCacheDir());
                adapter.setItems(items);
                adapter.notifyDataSetChanged();
                break;
        }
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

        listview = (ListView) findViewById(R.id.listview);
        listview.setEmptyView(findViewById(R.id.empty));
        adapter = new MainArrayAdapter(this, R.layout.list_item, items);

        listview.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity onDestroy");
        // Clean up connections
        if (mIsBound && mConsumerService != null) {
            mConsumerService.closeConnection();
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        ClipboardHelper.forceClearCliboard();

        super.onDestroy();
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
            //case R.id.action_search:
                // TODO add search
                // break;

            case R.id.action_safe:
                if (Trash.getNotEncryptedCount(adapter.getItems()) == 0) {
                    Snackbar.make(listview, R.string.snake_no_account_to_lock, Snackbar.LENGTH_LONG).show();
                    break;
                }
                CallGearAction cb = new CallGearAction(ACTION_CLOSE, this) {
                    @Override public void callback(String str) {
                        new EncProgressTask(this.getActivity(), str).execute();
                    }
                };
                if (DEBUG)
                    cb.callback(TEST_KEY);
                else
                    callGear(cb);
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

            case R.id.action_reorder:
                startActivityForResult(
                        new Intent(this, ReorderActivity.class),
                        REORDER_ACCOUNT_RESULT
                );
                break;

            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;

            case R.id.action_changekey:
                if (Trash.getEncryptedCount(adapter.getItems()) == 0) {
                    Snackbar.make(listview, R.string.snake_no_account_locked, Snackbar.LENGTH_LONG).show();
                    break;
                }
                CallGearAction getOld = new CallGearAction(ACTION_CLOSE, this) {
                    @Override public void callback(String str) {
                        final String oldKey = str;
                        CallGearAction getNew = new CallGearAction(ACTION_CLOSE, MainActivity.this) {
                            @Override public void callback(String str) {
                                new ReEncProgressTask(MainActivity.this, oldKey, str).execute();
                            }
                        };
                        callGear(getNew);
                    }
                };
                callGear(getOld);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void callGear(CallGearAction ca) {
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle(getString(R.string.gear_waiting));
        dialog.setMessage(getString(R.string.gear_waiting_message));
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                currentAction = CallGearAction.NOTHING;
            }
        });
        dialog.show();

        currentAction = ca;
        mConsumerService.sendData(ca.getAction());

        gearWaitingTimer = new CountDownTimer(CALL_GEAR_DELAY, 1000) {
            public void onTick(long millisUntilFinished) {
                if (currentAction == CallGearAction.NOTHING) {
                    dialog.dismiss();
                    this.cancel();
                }
            }

            public void onFinish() {
                currentAction = CallGearAction.NOTHING;
                dialog.dismiss();
            }
        }.start();
    }


    @Override
    public void snakeResponce(final int stringId, final boolean stopGearWaiting) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if (stringId != R.string.no_message)
                    Snackbar.make(listview, stringId, Snackbar.LENGTH_LONG).show();
                if (stopGearWaiting && gearWaitingTimer != null)
                    gearWaitingTimer.onFinish();
            }
        });
    }

    @Override
    public void gearResponse(final String data) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                currentAction.callback(data);
                currentAction = CallGearAction.NOTHING;
            }
        });
    }

}
