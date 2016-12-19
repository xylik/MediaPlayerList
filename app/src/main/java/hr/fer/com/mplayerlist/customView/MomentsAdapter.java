package hr.fer.com.mplayerlist.customView;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import hr.fer.com.mplayerlist.model.MomentData;
import hr.fer.com.mplayerlist.R;
import hr.fer.com.mplayerlist.mvp.MomentPresenterImpl;
import hr.fer.com.mplayerlist.mvp.MomentViewHolder;

public class MomentsAdapter extends MvpRecyclerAdapter<MomentData, MomentPresenterImpl, MomentViewHolder> {
    private Context ctx;
    private Resources res;
    private LayoutInflater layoutInflater;

    public MomentsAdapter(Context ctx, List<MomentData> data) {
        this.ctx = ctx;
        res = ctx.getResources();
        layoutInflater = LayoutInflater.from(ctx);
        clearAndAddAll(data);
    }

    @NonNull
    @Override
    protected MomentPresenterImpl createPresenter(MomentData data) {
        MomentPresenterImpl presenter = new MomentPresenterImpl();
        presenter.bindModel(data);
        return presenter;
    }

    @Override
    public MomentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = layoutInflater.inflate(R.layout.item_moment, parent, false);
        return new MomentViewHolder(v);
    }

    public boolean removeItem(int momentId) {
        MomentData momentToDelete = null;
        for(MomentData m: models){
            if(m.getId() == momentId){
                momentToDelete = m;
                break;
            }
        }
        if(momentToDelete == null) return false;
        else {
            removeItem(momentToDelete);
            return true;
        }
    }

    public void notifyChildFocusChanged(int childPosition, boolean isInFocus) {
        if(presenters.get(childPosition) != null) presenters.get(childPosition).momentFocusChanged(isInFocus);
    }

    public void notifyActivityCreated() {
        for(MomentPresenterImpl presenter : presenters.values()) presenter.onCreate();
    }

    public void notifyActivityResumed() {
        for(MomentPresenterImpl presenter : presenters.values()) presenter.onResume();
    }

    public void notifyActivityPaused() {
        for(MomentPresenterImpl presenter : presenters.values()) presenter.onPause();
    }

    public void notifyActivityDestroyed() {
        for(MomentPresenterImpl presenter : presenters.values()) presenter.onDestroy();
    }
}