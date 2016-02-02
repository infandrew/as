package com.pillows.accountsafe.main;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ListView;

import com.pillows.accessory.AccessoryCallback;
import com.pillows.accessory.AccessoryService;
import com.pillows.accountsafe.AboutActivity;
import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.AddActivity;
import com.pillows.accountsafe.CallGearAction;
import com.pillows.accountsafe.R;
import com.pillows.accountsafe.popup.Popup;
import com.pillows.accountsafe.reorder.ReorderActivity;
import com.pillows.accountsafe.Settings;
import com.pillows.accountsafe.SettingsActivity;
import com.pillows.saver.DataSaver;
import com.pillows.tools.ClipboardHelper;
import com.pillows.tools.Trash;

import java.util.List;

import static com.pillows.accountsafe.Settings.*;

public class MainActivity extends AppCompatActivity implements AccessoryCallback {

    private static final int ADD_ACCOUNT_RESULT = 1;
    private static final int REORDER_ACCOUNT_RESULT = 2;

    private MainArrayAdapter adapter;
    private boolean mIsBound = false;
    private AccessoryService mConsumerService = null;
    private CallGearAction currentAction = CallGearAction.NOTHING;
    private ListView listview;
    private CountDownTimer gearWaitingTimer = null;
    private ProgressDialog gearWaitingDialog;

    /**
     * Object to establish connection between MainActivity and AccessoryService
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConsumerService = ((AccessoryService.LocalBinder) service).getService();
            mConsumerService.setCallbacks(MainActivity.this);
            mConsumerService.findPeers();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "MainActivity onActivityResult");
        switch (requestCode) {
            case ADD_ACCOUNT_RESULT:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Bundle res = data.getExtras();
                        String name = res.getString("account_name");
                        String login = res.getString("account_login");
                        String password = res.getString("account_password");

                        adapter.addAll(new AccountDetails(name, login, password));
                        DataSaver.serialize(adapter.getItems());
                        showLockPopup();
                    }
                }
                break;
            case REORDER_ACCOUNT_RESULT:
                List<AccountDetails> items = (List) DataSaver.deserialize();
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

        gearWaitingDialog = new ProgressDialog(MainActivity.this);
        mIsBound = bindService(
                new Intent(MainActivity.this, AccessoryService.class),
                mConnection,
                Context.BIND_AUTO_CREATE
        );

        final List<AccountDetails> items = (List) DataSaver.deserialize();

        listview = (ListView) findViewById(R.id.listview);
        listview.setEmptyView(findViewById(R.id.empty));
        adapter = new MainArrayAdapter(this, R.layout.list_item, items);

        listview.setAdapter(adapter);

        showAddPopup();

    }

    private void showAddPopup() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        View actionView = findViewById(R.id.action_add);
                        if (actionView != null) {
                            List<AccountDetails> items = adapter.getItems();
                            if (items.size() == 0) {
                                Popup popup = new Popup(MainActivity.this, getString(R.string.popup_add));
                                popup.show(actionView);
                            }
                        }
                    }
                },
                1000
        );
    }

    private void showLockPopup() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        View actionView = findViewById(R.id.action_safe);
                        if (actionView != null) {
                            List<AccountDetails> items = adapter.getItems();
                            if (items.size() > 0) {
                                int encryptedCount = Trash.getEncryptedCount(items);
                                if (encryptedCount == 0) {
                                    Popup popup = new Popup(MainActivity.this, getString(R.string.popup_lock));
                                    popup.show(actionView);
                                }
                            }
                        }
                    }
                },
                2000
        );
    }

    /**/

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
                CallGearAction cb = new CallGearAction(ACTION_GET, this) {
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
                //startActivity(new Intent(this, SettingsActivity.class));
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
                CallGearAction getOld = new CallGearAction(ACTION_GET, this) {
                    @Override public void callback(String str) {
                        final String oldKey = str;
                        if (!Trash.checkPreviousEncryption(adapter.getItems(), oldKey)) {
                            Snackbar.make(listview, R.string.snake_wrong_key, Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        final CallGearAction getNew = new CallGearAction(ACTION_NEW, MainActivity.this) {
                            @Override public void callback(String str) {
                                new ReEncProgressTask(MainActivity.this, oldKey, str).execute();
                            }
                        };

                        new android.os.Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    callGear(getNew);
                                }
                            },
                            1000
                        );
                    }
                };
                callGear(getOld);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void callGear(CallGearAction ca) {
        currentAction = ca;

        gearWaitingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        gearWaitingDialog.setTitle(getString(R.string.gear_waiting));
        gearWaitingDialog.setMessage(getString(R.string.gear_waiting_message));
        switch(ca.getAction()) {
            case Settings.ACTION_NEW:
                gearWaitingDialog.setMessage(Html.fromHtml(getString(R.string.gear_waiting_message_new)));
                break;
        }
        gearWaitingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                currentAction = CallGearAction.NOTHING;
            }
        });
        gearWaitingDialog.show();

        mConsumerService.sendData(ca.getAction());

        gearWaitingTimer = new CountDownTimer(CALL_GEAR_DELAY, 1000) {
            public void onTick(long millisUntilFinished) {
                if (currentAction == CallGearAction.NOTHING) {
                    gearWaitingDialog.dismiss();
                    this.cancel();
                }
            }

            public void onFinish() {
                currentAction = CallGearAction.NOTHING;
                gearWaitingDialog.dismiss();
            }
        }.start();
    }


    @Override
    public void snakeResponce(final int stringId, final boolean stopGearWaiting) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if (stringId != R.string.no_message)
                    Snackbar.make(listview, stringId, Snackbar.LENGTH_LONG).show();
                if (stopGearWaiting) {
                    if (gearWaitingTimer != null)
                        gearWaitingTimer.cancel();
                    gearWaitingDialog.dismiss();
                    currentAction = CallGearAction.NOTHING;
                }
            }
        });
    }

    @Override
    public void gearResponse(final String data) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                CallGearAction action = currentAction;
                currentAction.callback(data);
                if (action == currentAction) {
                    currentAction = CallGearAction.NOTHING;
                    gearWaitingDialog.dismiss();
                }
            }
        });
    }

}
