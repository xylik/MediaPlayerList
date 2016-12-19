package hr.fer.com.mplayerlist.util;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by Igor on 12/12/16.
 */
public class ItemVisibilityUtil {
    private LinearLayoutManager layoutManager;
    private RecyclerView recyclerView;
    private int scrollOrientation;
    private int oldItemInFocus = 0;
    private OnFocusUpdateListener focusUpdateListener;
    private int itemHeight = -1;
    private int recyclerViewMiddle = -1;

    public interface OnFocusUpdateListener {
        void onFocusUpdate(int itemInFocus);
    }

    public ItemVisibilityUtil(RecyclerView recyclerView, LinearLayoutManager layoutManager, int scrollOrientation) {
        this.layoutManager = layoutManager;
        this.recyclerView = recyclerView;
        this.scrollOrientation = scrollOrientation;
        recyclerView.getContext();
    }

    public void setFocusUpdateListener(OnFocusUpdateListener l) {
        this.focusUpdateListener = l;
    }

    public synchronized void onScroll() {
        if(itemHeight == -1) {
            View firstVisibleView = layoutManager.getChildAt(0); //first visible item in recycler
            if(firstVisibleView == null) return;
            itemHeight = firstVisibleView.getHeight();
            recyclerViewMiddle = recyclerView.getHeight()/2;
        }
        int firstVisibleItemPos = layoutManager.findFirstVisibleItemPosition();
        int lastVisibleItemPos = layoutManager.findLastVisibleItemPosition();
        int visibleItemCnt = lastVisibleItemPos - firstVisibleItemPos + 1;
        Log.d("IVisibility", "visibleItemCnt:" + visibleItemCnt);
        Log.d("IVisibility", "lastVisibleItemPos=" + lastVisibleItemPos);
        Log.d("IVisibility", "firstVisibleItemPos=" + firstVisibleItemPos);
        if(scrollOrientation == RecyclerView.VERTICAL) {
            calcVerticalPercentage(visibleItemCnt);
        }else {
            calcHorizontalPercentage();
        }
    }

    private void calcHorizontalPercentage() {
        throw new RuntimeException("Unexpected call!");
    }

    private View topView;
    private void calcVerticalPercentage(int visibleItemCnt) {
        if(visibleItemCnt == 1){
            Log.d("IVisibility", " case1");
            maybeUpdateItemInFocus(0);
        }else if(visibleItemCnt == 2) {
            Log.d("IVisibility", " case2");
            topView = layoutManager.getChildAt(0);
            if(itemHeight + topView.getTop() >= recyclerViewMiddle) maybeUpdateItemInFocus(layoutManager.findFirstVisibleItemPosition());
            else maybeUpdateItemInFocus(layoutManager.findLastVisibleItemPosition());
        }else if(visibleItemCnt == 3){
            Log.d("IVisibility", " case3");
            maybeUpdateItemInFocus(layoutManager.findFirstCompletelyVisibleItemPosition());
        }else {
            throw new RuntimeException("Not expected item count, num of items occured:" + visibleItemCnt);
        }
    }

    private void maybeUpdateItemInFocus(int currItemInFocus) {
        if(oldItemInFocus != currItemInFocus) {
            Log.d("IVisibility", "focusChanged from:" + oldItemInFocus + " to:" + currItemInFocus);
            oldItemInFocus = currItemInFocus;
            if(focusUpdateListener != null) focusUpdateListener.onFocusUpdate(currItemInFocus);
        }
    }
}
