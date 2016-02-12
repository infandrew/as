package com.pillows.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.CountDownTimer;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.pillows.accountsafe.R;
import com.pillows.accountsafe.Settings;

import static com.pillows.accountsafe.Settings.ACCOUNT_WATCH_TIME;

/**
 * Created by agudz on 27/01/16.
 */
public class ClipboardHelper {

    private static CountDownTimer countDownTimer = null;

    /**
     * Galaxy S5 have 20 clips in history
     *
     * @param context
     */
    public static void copyToClipBoard(final Context context, View view, String copyText) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("text", copyText);
        clipboard.setPrimaryClip(clip);

        Snackbar.make(view, R.string.snake_copied, Snackbar.LENGTH_LONG).show();

        if (countDownTimer != null)
            countDownTimer.cancel();
        countDownTimer = new CountDownTimer(ACCOUNT_WATCH_TIME, ACCOUNT_WATCH_TIME) {
            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", " ");
                clipboard.setPrimaryClip(clip);

                for (int i = Settings.CLIPBOARD_HISTORY_SIZE; i >= 1; i--) {
                    clip = ClipData.newPlainText("text", "" + i);
                    clipboard.setPrimaryClip(clip);

                }

                countDownTimer = null;
            }
        }.start();
    }

    public static void forceClearCliboard() {
        if (countDownTimer != null)
            countDownTimer.onFinish();
    }
}
