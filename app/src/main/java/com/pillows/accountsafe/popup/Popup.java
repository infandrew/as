package com.pillows.accountsafe.popup;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.pillows.accountsafe.R;
import com.pillows.accountsafe.Settings;


/**
 * Created by agudz on 01/02/16.
 */
public class Popup {

    protected WindowManager mWindowManager;

    protected Context mContext;
    protected PopupWindow mWindow;

    private TextView mHelpTextView;
    private ImageView mUpImageView;
    private ImageView mDownImageView;
    protected View mView;

    protected Drawable mBackgroundDrawable = null;
    protected ShowListener showListener;

    public Popup(Context context, String text, int viewResource) {
        mContext = context;
        mWindow = new PopupWindow(context);
        mWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dismiss();
                return false;
            }
        });

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(layoutInflater.inflate(viewResource, null));

        mHelpTextView = (TextView) mView.findViewById(R.id.popup_text);
        mUpImageView = (ImageView) mView.findViewById(R.id.popup_arrow_up);
        mDownImageView = (ImageView) mView.findViewById(R.id.popup_arrow_down);

        mHelpTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mHelpTextView.setSelected(true);
    }

    public Popup(Context context) {
        this(context, "", R.layout.popup);
    }

    public Popup(Context context, String text) {
        this(context);

        setText(text);
    }

    public void show(View anchor) {
        preShow();

        int[] location = new int[2];

        anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0]
                + anchor.getWidth(), location[1] + anchor.getHeight());

        /*mView.measure(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        int rootHeight = mView.getMeasuredHeight();
        int rootWidth = mView.getMeasuredWidth();*/

        View k = mView.findViewById(R.id.popup_text);
        k.measure(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        int rootHeight = k.getMeasuredHeight();
        int rootWidth = k.getMeasuredWidth();


        final int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        final int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

        int yPos = anchorRect.top - rootHeight;

        boolean onTop = true;

        if (anchorRect.top < screenHeight / 2) {
            yPos = anchorRect.bottom;
            onTop = false;
        }

        int whichArrow, requestedX;

        whichArrow = ((onTop) ? R.id.popup_arrow_down : R.id.popup_arrow_up);
        requestedX = anchorRect.centerX();

        View arrow = whichArrow == R.id.popup_arrow_up ? mUpImageView
                : mDownImageView;
        View hideArrow = whichArrow == R.id.popup_arrow_up ? mDownImageView
                : mUpImageView;

        arrow.setVisibility(View.VISIBLE);

        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) arrow
                .getLayoutParams();

        hideArrow.setVisibility(View.INVISIBLE);

        int xPos = anchorRect.left + anchor.getWidth() / 2 - rootWidth / 2;
        int rPos = anchorRect.left + anchor.getWidth() / 2 + rootWidth / 2;

        // ETXTREME RIGHT CLIKED
        if (rPos > screenWidth) {
            xPos = (screenWidth - rootWidth);
            param.leftMargin = (rPos - screenWidth) * 2;
        }
        // ETXTREME LEFT CLIKED
        else if (xPos < 0) {
            param.leftMargin = -xPos * 2;
            xPos = 0;
        }

        if (onTop) {
            mHelpTextView.setMaxHeight(anchorRect.top - anchorRect.height());

        } else {
            mHelpTextView.setMaxHeight(screenHeight - yPos);
        }

        Log.w(Settings.TAG, ""+rootWidth);

        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
        mView.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.float_anim));

    }

    protected void preShow() {
        if (mView == null)
            throw new IllegalStateException("view undefined");



        if (showListener != null) {
            showListener.onPreShow();
            showListener.onShow();
        }

        if (mBackgroundDrawable == null)
            mWindow.setBackgroundDrawable(new BitmapDrawable());
        else
            mWindow.setBackgroundDrawable(mBackgroundDrawable);

        mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setTouchable(true);
        mWindow.setFocusable(true);
        mWindow.setOutsideTouchable(true);

        mWindow.setContentView(mView);
    }

    public void setBackgroundDrawable(Drawable background) {
        mBackgroundDrawable = background;
    }

    public void setContentView(View root) {
        mView = root;
        mWindow.setContentView(root);
    }

    public void setContentView(int layoutResID) {
        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(inflator.inflate(layoutResID, null));
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mWindow.setOnDismissListener(listener);
    }

    public void dismiss() {
        mWindow.dismiss();
        if (showListener != null) {
            showListener.onDismiss();
        }
    }

    public void setText(String text) {
        mHelpTextView.setText(text);
    }

    public static interface ShowListener {
        void onPreShow();
        void onDismiss();
        void onShow();
    }

    public void setShowListener(ShowListener showListener) {
        this.showListener = showListener;
    }
}