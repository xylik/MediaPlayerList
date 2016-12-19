package hr.fer.com.mplayerlist.mvp;

import hr.fer.com.mplayerlist.customView.VHolderBasePresenter;
import hr.fer.com.mplayerlist.model.MomentData;
import hr.fer.com.mplayerlist.util.TimeUtil;

/**
 * Created by Igor on 19/05/16.
 */
public class MomentPresenterImpl extends VHolderBasePresenter<MomentData, MomentContract.View> implements MomentContract.Presenter {
    public static final String TAG = MomentPresenterImpl.class.getSimpleName();

    private boolean isPlayerTouched = false;
    private boolean isInFocus = false;
    private boolean isVideoPlaying = false;
    private boolean isPlayerInitialized = false;
    private int lastShownSecond = -1;
    private long lastPlayedPositionMs;
    private boolean shouldPlayInLoop = false;
    private boolean isScreenVisible = false;

    public MomentPresenterImpl() {
    }

    //region Lifecycle events
    @Override
    public void onCreate() {
        //empty
    }

    @Override
    public void onResume() {
        if(isInFocus)playOrPauseVideoIfMomentIsInFocus();
        isScreenVisible = true;
    }

    @Override
    public void onPause() {
        if (isInFocus) {
            view.stopVideo();
        }
        isPlayerTouched = false;
        isScreenVisible = false;
        isPlayerInitialized = false;
    }

    @Override
    public void onDestroy() {
        if(view == null)return; //this presenter was not binded with viewholder because user didnt scroll to this item, or was unbinded on viewholder recycling
        view.releaseResources();
        view = null;
    }
    //endregion

    //region UI events
    @Override
    protected void initView() {
        view.setVideoThumbnail(model.getThumbnail());

        if (isInFocus) {
            playOrPauseVideoIfMomentIsInFocus();
        }

        view.setMomentTitle(model.getTitle());
        view.setCounterTimeText("" + Math.round((model.getEndTimeMs() - model.getStartTimeMs())/ TimeUtil.MS_IN_SECOND));

        view.setState(isInFocus);
    }

    @Override
    public void playerTouched() {
        if (!isInFocus) return;
        isPlayerTouched = !isPlayerTouched;

        view.setProgressBarVisiblity(isPlayerTouched ? gone : visible);
        if(isPlayerTouched) view.pauseVideo();
        else view.playVideo();
    }
    //endregion

    //region Player
    @Override
    public void momentFocusChanged(boolean isInFocus) {
        if(this.isInFocus == isInFocus) return;

        if(this.isInFocus = isInFocus) isPlayerTouched = false;

        //check if view!=null is necessary because binding ViewHolders in RecyclerView is async task and takes some time,
        // momentFocusChanged can be called before bind was finished and in that case view will be null
        if(view != null) {
            view.setState(isInFocus);
            playOrPauseVideoIfMomentIsInFocus();
        }
    }

    private void playOrPauseVideoIfMomentIsInFocus() {
        if (isInFocus) {
            view.setProgressBarVisiblity(visible);
            final long momentStartMs = model.getStartTimeMs();
            final long momentEndMs = model.getEndTimeMs();
            view.playVideoFromUri(model.getVideoPath().toString(), model.getId(), momentStartMs, momentEndMs, true, true);
        } else releasePlayer();
    }

    @Override
    public void playerStateChanged(boolean playWhenReady, int playerState) {
        isVideoPlaying = (playWhenReady && playerState == STATE_READY);
        if(isVideoPlaying && !isPlayerInitialized) isPlayerInitialized = true;
        if (isVideoPlaying) {
            view.setVideoThumbnailVisibility(gone);
            view.setProgressBarVisiblity(gone);
        }
    }

    @Override
    public void playerSurfaceStateChanged(boolean isSurfaceReadyForRendering) {
        //empty
    }

    @Override
    public void playerStartedDrawing() {
        view.setVideoThumbnailVisibility(gone);
        view.setProgressBarVisiblity(gone);
    }

    @Override
    public void playerErrorOccured(Exception error) {
        view.showMessage(MomentContract.View.Message.PlayerError);
        releasePlayer();
    }

    @Override
    public void videoProgressUpdated(long currentPosition) {
        lastPlayedPositionMs = currentPosition;
        float currentSecond = currentPosition / 1000f;

        int roundedCurrentSecond = Math.round((float)model.getEndTimeMs()/TimeUtil.MS_IN_SECOND - currentSecond);
        if (lastShownSecond != roundedCurrentSecond) {
            lastShownSecond = roundedCurrentSecond;
            view.setCounterTimeText(Integer.toString(lastShownSecond));
        }
    }

    private void releasePlayer() {
        view.stopVideo();
        view.setProgressBarVisiblity(gone);
        view.setVideoThumbnailVisibility(visible);
        isInFocus = false;
        isVideoPlaying = false;
        isPlayerTouched = false;
        lastShownSecond = -1;
        lastPlayedPositionMs = -1;
        isPlayerInitialized = false;
    }
    //endregion
}
