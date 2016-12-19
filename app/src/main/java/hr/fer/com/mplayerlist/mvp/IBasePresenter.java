package hr.fer.com.mplayerlist.mvp;

/**
 * Created by Igor on 11/05/16.
 */
public interface IBasePresenter {
    boolean enabled = true;
    boolean disabled = false;
    boolean active = true;
    boolean inActive = false;
    boolean visible = true;
    boolean gone = false;
    boolean destroyScreen = true;
    boolean keepScreen = false;

    void onCreate();
    void onResume();
    void onPause();
}