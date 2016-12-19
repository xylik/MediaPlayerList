package hr.fer.com.mplayerlist.customView;

/**
 * Created by Igor on 18/05/16.
 */
public abstract class VHolderBasePresenter<M, V>{
    protected V view;
    protected M model;
    protected int position = -1;

    public void bindModel(M model) {
        this.model = model;
        if(isBindFinished()) initView();
    }

    public void bindView(V view){
        this.view = view;
        if(isBindFinished()) initView();
    }

    public void unbindView(){
        this.view = null;
    }

    protected boolean isBindFinished() {
        return view != null && model != null;
    }

    public void setPosition(int position){
        this.position = position;
    }

    protected abstract void initView();
}
