package com.pillows.accountsafe.reorder;

import android.content.Context;
import android.view.DragEvent;
import android.view.View;
import android.widget.ListView;

/**
 * Created by agudz on 25/01/16.
 */
public class MyDragListener implements View.OnDragListener {

    private Context context;

    private View viewToDrag;
    private int currentPosition;

    MyDragListener(Context context) {
        this.context = context;
    }

    @Override
    public boolean onDrag(View container, DragEvent event) {
        int action = event.getAction();
        ListView listview = (ListView) container;
        int position;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                Object[] localData = (Object[]) event.getLocalState();
                viewToDrag = (View) localData[0];
                currentPosition = (int) localData[1];
                break;

            case DragEvent.ACTION_DRAG_LOCATION:
                position = listview.pointToPosition((int)event.getX(), (int)event.getY());
                if (position != -1) {
                    if (currentPosition != position) {
                        ReorderArrayAdapter adapter = (ReorderArrayAdapter) listview.getAdapter();
                        adapter.swap(currentPosition, position);
                        currentPosition = position;

                        adapter.notifyDataSetChanged();

                        if (position >= listview.getLastVisiblePosition()-1) {
                            listview.smoothScrollToPosition(position+1);
                            //listview.smoothScrollBy(128, 1000);
                        }
                        else if (position <= listview.getFirstVisiblePosition()+1) {
                            listview.smoothScrollToPosition(position-1);
                            //listview.smoothScrollBy(-128, 1000);
                        }
                    }
                }

                break;
            case DragEvent.ACTION_DROP:
                ReorderArrayAdapter adapter = (ReorderArrayAdapter) listview.getAdapter();
                adapter.getItems().get(currentPosition).setDragged(false);
                adapter.notifyDataSetChanged();
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
            default:
                break;
        }
        return true;
    }
}
