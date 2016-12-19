package hr.fer.com.mplayerlist.customView;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import hr.fer.com.mplayerlist.model.MomentData;
import hr.fer.com.mplayerlist.player.VideoPlayManager;
import hr.fer.com.mplayerlist.util.ItemVisibilityUtil;

/**
 * Created by Igor on 16/05/16.
 */
public class MomentsView extends RecyclerView {
    private MomentsAdapter momentsAdapter;
    private int lastVisibleMomentIndx = 0;
    private boolean isInFocus = false;
    private LinearLayoutManager layoutManager;
    private int scrollOrientation = VERTICAL;

    private ItemVisibilityUtil visibilityUtil;

    //region Constructors
    public MomentsView(Context context) {
        super(context);
        initView();
    }

    public MomentsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MomentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }
    //endregion

    private void initView() {
        setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        setHasFixedSize(true);
        RecyclerView.LayoutParams momentListLytParams = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
        setLayoutParams(momentListLytParams);
        momentsAdapter = new MomentsAdapter(getContext(), new ArrayList<>());
        setAdapter(momentsAdapter);
    }

    //region Activity lifecycle
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        momentsAdapter.notifyActivityCreated();
    }

    public void onActivityResumed(Activity activity) {
        momentsAdapter.notifyActivityResumed();
    }

    public void onActivityPaused(Activity activity) {
        momentsAdapter.notifyActivityPaused();
    }

    public void onActivityDestroyed(Activity activity) {
        momentsAdapter.notifyActivityDestroyed();
    }
    //endregion

    public void setLayoutManagerOrientation(int orientation) {
        scrollOrientation = orientation;
        layoutManager = new LinearLayoutManager(getContext(), scrollOrientation, false);
        setLayoutManager(layoutManager);
        if(scrollOrientation == VERTICAL) {
            visibilityUtil = new ItemVisibilityUtil(this, layoutManager, scrollOrientation);
            visibilityUtil.setFocusUpdateListener(itemFocusListener);
        }
        setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(scrollOrientation == VERTICAL) {
                    if (momentsAdapter == null || momentsAdapter.getData().isEmpty() || dy == 0) return;
                    visibilityUtil.onScroll();
                }else throw new RuntimeException("Unexpected orientation!");
            }
        });
    }

    private final ItemVisibilityUtil.OnFocusUpdateListener itemFocusListener = new ItemVisibilityUtil.OnFocusUpdateListener() {
        @Override
        public void onFocusUpdate(int position) {
            if(isInFocus && position != lastVisibleMomentIndx) {
                Log.d("igorLog", "itemFocusListener focus changed from:" + lastVisibleMomentIndx + " to:" + position);
                VideoPlayManager.ignoreAnyPlayInProgress();
                updateChildrenFocus(position);
            }
        }
    };

    public void bindData(List<MomentData> moments) {
        List<MomentData> detachedCollection = new ArrayList<>(moments);
        isInFocus = false;
        lastVisibleMomentIndx = 0;

        momentsAdapter.clearAndAddAll(detachedCollection);
        //TODO check this if maybe needs to be removed
        setNestedScrollingEnabled(false);
    }

    public void setIsViewInFocus(boolean isViewInFocus) {
        if(this.isInFocus == isViewInFocus) return;

        this.isInFocus = isViewInFocus;
        momentsAdapter.notifyChildFocusChanged(lastVisibleMomentIndx, isViewInFocus);
    }

    private void updateChildrenFocus(int newChildInFocusPosition) {
        if (lastVisibleMomentIndx != newChildInFocusPosition) {
            momentsAdapter.notifyChildFocusChanged(lastVisibleMomentIndx, false);
            momentsAdapter.notifyChildFocusChanged(lastVisibleMomentIndx = newChildInFocusPosition, true);
        }
    }
}
