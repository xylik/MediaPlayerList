package hr.fer.com.mplayerlist.mvp;

/**
 * Created by Igor on 11/05/16.
 */
public interface IBaseView {
    enum LifecycleEvent{ OnCreate, OnResume, OnActivityResult, OnPause, OnStop, OnDestroy}

    void showProgressSpinner();
    void hideProgressSpinner();
}
