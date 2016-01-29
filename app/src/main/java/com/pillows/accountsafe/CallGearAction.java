package com.pillows.accountsafe;

import android.app.Activity;

import static com.pillows.accountsafe.Settings.*;

/**
 * Created by agudz on 28/01/16.
 */
public abstract class CallGearAction {
    public static final CallGearAction NOTHING = new CallGearAction(ACTION_NOTHING, null) {
        @Override public void callback(String str) {}
    };

    private String action;
    private Activity activity;

    public CallGearAction(String action, Activity activity) {
        this.action = action;
        this.activity = activity;
    }

    public String getAction() {
        return action;
    }

    protected Activity getActivity() {
        return activity;
    }

    public abstract void callback(String str);
}
