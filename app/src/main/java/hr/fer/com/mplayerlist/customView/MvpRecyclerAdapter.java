package hr.fer.com.mplayerlist.customView;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class MvpRecyclerAdapter<M, P extends VHolderBasePresenter, VH extends MvpViewHolder> extends RecyclerView.Adapter<VH> {
    protected final Map<Integer, P> presenters;
    protected final List<M> models;
    protected int nextPresenterId = 0;

    public MvpRecyclerAdapter() {
        presenters = new HashMap<>();
        models = new ArrayList<>();
    }

    //region RecyclerView overrides
    @Override
    public void onViewRecycled(VH holder) {
        super.onViewRecycled(holder);

        holder.unbindPresenter();
    }

    @Override
    public boolean onFailedToRecycleView(VH holder) {
        // Sometimes, if animations are running on the itemView's children, the RecyclerView won't
        // be able to recycle the view. We should still unbind the presenter.
        holder.unbindPresenter();

        return super.onFailedToRecycleView(holder);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        presenters.get(position).setPosition(position);
        holder.bindPresenter(presenters.get(position));
    }

    @Override
    public int getItemCount() {
        return models.size();
    }
    //endregion

    //region Public api
    public void clearAndAddAll(Collection<M> data) {
        resetAdapter();
        for (M item : data) {
            addInternal(item);
        }
        notifyDataSetChanged();
    }

    public void addAll(Collection<M> data) {
        for (M item : data) {
            addInternal(item);
        }

        int addedSize = data.size();
        int oldSize = models.size() - addedSize;
        notifyItemRangeInserted(oldSize, addedSize);
    }

    public void addItem(M item) {
        addInternal(item);
        notifyItemInserted(models.size());
    }

    public void updateItem(M item) {
        // Swap the model
        int position = getItemPosition(item);
        if (position >= 0) {
            models.remove(position);
            models.add(position, item);
        }

        // Swap the presenter
        P existingPresenter = presenters.get(position);
        if (existingPresenter != null) {
            existingPresenter.bindModel(item);
        }

        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    public void removeItem(M item) {
        int position = getItemPosition(item);
        if (position >= 0) {
            models.remove(item);
            presenters.remove(position);
            List<P> presentersLeft = new ArrayList(presenters.values());
            presenters.clear();
            nextPresenterId = 0;
            for(P p: presentersLeft){
                p.bindModel(models.get(nextPresenterId));
                presenters.put(nextPresenterId, p);
                nextPresenterId++;
            }
            notifyItemRemoved(position);
        }
    }

    public void removeItem(M item, Iterator<M> iterator) {
        int position = getItemPosition(item);
        if (position >= 0) {
            iterator.remove();
            presenters.remove(position);
            List<P> presentersLeft = new ArrayList(presenters.values());
            presenters.clear();
            nextPresenterId = 0;
            for(P p: presentersLeft){
                p.bindModel(models.get(nextPresenterId));
                presenters.put(nextPresenterId, p);
                nextPresenterId++;
            }
            notifyItemRemoved(position);
        }
    }

    public List<M> getData() {
        return models;
    }
    //endregion

    //region Private api
    private void addInternal(M item) {
        models.add(item);
        presenters.put(nextPresenterId++, createPresenter(item));
    }

    protected int getItemPosition(M item) {
        return models.indexOf(item);
    }

    private  void resetAdapter() {
        models.clear();
        presenters.clear();
        nextPresenterId = 0;
    }
    //endregion

    @NonNull protected abstract P createPresenter(M data);
}
