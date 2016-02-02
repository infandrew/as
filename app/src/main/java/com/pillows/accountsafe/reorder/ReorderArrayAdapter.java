package com.pillows.accountsafe.reorder;

/**
 * Created by agudz on 25/01/16.
 */

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pillows.accountsafe.AccountDetails;
import com.pillows.accountsafe.EditActivity;
import com.pillows.accountsafe.R;
import com.pillows.dialog.DialogAction;
import com.pillows.dialog.DialogHelper;

import java.util.List;


/**
 * Created by agudz on 25/01/16.
 */
class ReorderArrayAdapter extends ArrayAdapter<AccountDetails> {

    private List<AccountDetails> items;
    private Context context;

    public ReorderArrayAdapter(Context context, int viewId,
                              List<AccountDetails> items) {
        super(context, viewId, items);
        this.items = items;
        this.context = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_reorder, null);
        } else {
            view = convertView;
        }

        final AccountDetails account = items.get(position);

        TextView listItemTitle = (TextView) view.findViewById(R.id.list_item_title);
        listItemTitle.setText(account.getName());

        ImageButton collapseBtn = (ImageButton) view.findViewById(R.id.list_item_collapse);
        ImageButton removeBtn = (ImageButton) view.findViewById(R.id.list_item_remove);
        ImageButton editBtn = (ImageButton) view.findViewById(R.id.list_item_edit);

        if (account.isDragged()) {
            view.setBackground(ContextCompat.getDrawable(context, R.drawable.round_rect_draged));
            view.setAlpha((float) 0.1);
        } else {
            view.setBackground(ContextCompat.getDrawable(context, R.drawable.round_rect));
            view.setAlpha((float) 1);
        }

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                account.setDragged(true);
                view.setBackground(ContextCompat.getDrawable(context, R.drawable.round_rect_draged));
                view.setAlpha((float) 0.1);

                ClipData data = ClipData.newPlainText("", "");
                //View.DragShadowBuilder myShadow = new MyDragShadowBuilder(imageView);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, new Object[]{view, position}, 0);
                return true;
            }
        });

        collapseBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                account.setDragged(true);
                view.setBackground(ContextCompat.getDrawable(context, R.drawable.round_rect_draged));
                view.setAlpha((float) 0.1);

                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, new Object[]{view, position}, 0);
                return true;
            }
        });

        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.createAndShowAlertDialog(context, R.string.action_remove_sure, new DialogAction() {
                    @Override
                    public void yesAction() {
                        ReorderArrayAdapter.this.remove(position);
                    }

                    @Override
                    public void cancelAction() { /* empty */ }
                });
            }
        });

        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle conData = new Bundle();
                conData.putInt("account_position", position);
                conData.putString("account_name", account.getName());
                Intent intent = new Intent(context, EditActivity.class);
                intent.putExtras(conData);
                ((Activity) context).startActivityForResult(intent, ReorderActivity.EDIT_ACCOUNT_RESULT);
            }
        });

        return view;
    }

    private void remove(int position) {
        items.remove(position);
        notifyDataSetChanged();
    }

    public List<AccountDetails> getItems() {
        return items;
    }



    public void swap(int p1, int p2) {
        AccountDetails s1 = items.get(p1);
        AccountDetails s2 = items.get(p2);
        items.set(p1, s2);
        items.set(p2, s1);
    }
}
