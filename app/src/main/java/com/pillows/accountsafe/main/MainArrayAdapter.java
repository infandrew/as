package com.pillows.accountsafe.main;

import android.content.Context;
import android.os.CountDownTimer;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.CallGearAction;
import com.pillows.accountsafe.R;
import com.pillows.encryption.Encryptor;
import com.pillows.tools.ClipboardHelper;
import com.pillows.tools.CollapseHelper;
import com.pillows.tools.PasswordHelper;

import java.util.Collection;
import java.util.List;

import static com.pillows.accountsafe.Settings.*;

/**
 * Created by agudz on 25/01/16.
 */
class MainArrayAdapter extends ArrayAdapter<AccountDetails> {

    private List<AccountDetails> items;
    private MainActivity context;

    public MainArrayAdapter(MainActivity context, int viewId,
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

        final View collapsablePart = view.findViewById(R.id.list_item_collapsable);

        if (encrypted) {
            if (watchTime > ACCOUNT_WATCH_TIME) {
                hideAccount(view);
            }
        } else {
            showAccount(view, account.getLogin(), account.getPassword(), account);
        }

        if (account.isDragged()) {
            view.setBackground(ContextCompat.getDrawable(context, R.drawable.round_rect_draged));
            view.setAlpha((float) 0.1);
        } else {
            view.setBackground(ContextCompat.getDrawable(context, R.drawable.round_rect));
            view.setAlpha((float) 1);
        }

        final View collapseImage = view.findViewById(R.id.list_item_collapse);

        View.OnClickListener listner = new View.OnClickListener() {
            private CountDownTimer countDownTimer = null;

            @Override
            public void onClick(View v) {
                if (encrypted) {
                    if (!account.isCollapsed()) {
                        if (countDownTimer != null)
                            countDownTimer.cancel();
                        hideDecAccount(view);
                        account.setCollapsed(true);
                    } else {
                        CallGearAction cb = new CallGearAction(ACTION_OPEN, context) {
                            @Override public void callback(String key) {
                                countDownTimer = showDecAccount(key, view, account);
                            }
                        };
                        if (DEBUG) {
                            cb.callback(TEST_KEY);
                        } else {
                            context.callGear(cb);
                        }
                    }
                } else {
                    if (!account.isCollapsed()) {
                        CollapseHelper.collapse(collapsablePart);
                        account.setCollapsed(true);
                    } else {
                        CollapseHelper.expand(collapsablePart);
                        account.setCollapsed(false);
                    }
                }
            }
        };

        view.setOnClickListener(listner);
        collapseImage.setOnClickListener(listner);

        return view;
    }



    private void hideDecAccount(View view) {
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
        view.findViewById(R.id.list_item_watchtimer).setVisibility(View.GONE);
    }

    private CountDownTimer showDecAccount(String key, final View view, final AccountDetails account) {
        Encryptor enc = new Encryptor(key);
        String login = account.decryptLogin(enc);
        String password = account.decryptPassword(enc);
        if (login == null || password == null) {
            Snackbar.make(view, R.string.snake_wrong_key, Snackbar.LENGTH_LONG).show();
            return null;
        } else {
            account.setWatchTime(System.currentTimeMillis());
            account.setCollapsed(false);

            final TextView watchTimer = (TextView) view.findViewById(R.id.list_item_watchtimer);
            watchTimer.setText("" + ACCOUNT_WATCH_TIME / 1000);
            watchTimer.setVisibility(View.VISIBLE);

            CountDownTimer countDownTimer = new CountDownTimer(ACCOUNT_WATCH_TIME, 1000) {
                public void onTick(long millisUntilFinished) {
                    long timeToHide = ACCOUNT_WATCH_TIME - System.currentTimeMillis() + account.getWatchTime();
                    watchTimer.setText("" + timeToHide / 1000);
                }

                public void onFinish() {
                    hideAccount(view);
                    account.setCollapsed(true);
                }

            }.start();

            showAccount(view, login, password, account);
            CollapseHelper.expand(view.findViewById(R.id.list_item_collapsable));
            return countDownTimer;
        }
    }

    private void showAccount(final View view, final String login, final String password, AccountDetails account) {
        ImageButton collapseBtn = (ImageButton) view.findViewById(R.id.list_item_collapse);
        final View collapsablePart = view.findViewById(R.id.list_item_collapsable);

        collapseBtn.setImageResource(R.drawable.ic_unlock);
        if (!account.isCollapsed()) {
            collapsablePart.setVisibility(View.VISIBLE);
        } else {
            collapsablePart.setVisibility(View.GONE);
        }

        final String passwordMask = PasswordHelper.getMask(password.length());

        // password setup
        view.findViewById(R.id.list_item_passwordrow).setVisibility(View.VISIBLE);
        final TextView listItemPassword = (TextView) view.findViewById(R.id.list_item_password);
        listItemPassword.setText(passwordMask);
        ImageButton listItemPasswordCopy = (ImageButton) view.findViewById(R.id.list_item_copypassword);
        listItemPasswordCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardHelper.copyToClipBoard(context, view, password);
            }
        });
        final ImageButton listItemPasswordShow = (ImageButton) view.findViewById(R.id.list_item_showpassword);
        listItemPasswordShow.setImageResource(R.drawable.ic_visible);
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
                    ClipboardHelper.copyToClipBoard(context, view, login);
                }
            });
        }
    }

    public void swap(int p1, int p2) {
        AccountDetails s1 = items.get(p1);
        AccountDetails s2 = items.get(p2);
        items.set(p1, s2);
        items.set(p2, s1);
    }

    public void setItems(List<AccountDetails> items) {
        this.items.clear();
        this.items.addAll(items);
    }
}
