package hr.fer.com.mplayerlist.customView;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class MvpViewHolder<P extends VHolderBasePresenter> extends RecyclerView.ViewHolder {
    protected P presenter;

    public MvpViewHolder(View itemView) {
        super(itemView);
    }

    public void bindPresenter(P presenter) {
        this.presenter = presenter;
        presenter.bindView(this);
    }

    public void unbindPresenter() {
        presenter = null;
    }
}
