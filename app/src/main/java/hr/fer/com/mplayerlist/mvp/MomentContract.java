package hr.fer.com.mplayerlist.mvp;

import android.net.Uri;

import hr.fer.com.mplayerlist.mvp.IBasePresenter;

/**
 * Created by Igor on 19/05/16.
 */
public class MomentContract {
    public interface View{
        enum Message{
            PlayerError
        }

        void setState(boolean isEnabled);
        void setIsUserInputEnabled(boolean isEnabled);
        void setVideoThumbnail(Uri thumbnailPath);
        void setVideoThumbnailVisibility(boolean isVisible);
        void setMomentTitle(String title);
        void setCounterTimeText(String timeLeft);

        void setProgressBarVisiblity(boolean isVisible);
        void showMessage(Message message);

        void playVideoFromUri(String videoUrl, int videoId, long startMs, long endMs, boolean shouldPlayInLoop, boolean playWhenReady);
        void playVideo();
        void pauseVideo();
        void seekVideo(int videoId, long positionMS);
        void stopVideo();

        void releaseResources();
    }

    public interface Presenter extends IBasePresenter {
        //region Player state constants
        /**
         * The player is neither prepared or being prepared.
         */
        int STATE_IDLE = 1;
        /**
         * The player is being prepared.
         */
        int STATE_PREPARING = 2;
        /**
         * The player is prepared but not able to immediately play from the current position. The cause
         * is {TrackRenderer} specific, but this state typically occurs when more data needs
         * to be buffered for playback to start.
         */
        int STATE_BUFFERING = 3;
        /**
         * The player is prepared and able to immediately play from the current position. The player will
         * be playing if {getPlayWhenReady()} returns true, and paused otherwise.
         */
        int STATE_READY = 4;
        /**
         * The player has finished playing the media.
         */
        int STATE_ENDED = 5;
        //endregion

        void onDestroy();

        void playerTouched();
        void momentFocusChanged(boolean isInFocus);
        void playerStateChanged(boolean playWhenReady, int playerState);
        void playerSurfaceStateChanged(boolean isSurfaceDestroyed);
        void playerStartedDrawing();
        void playerErrorOccured(Exception error);
        void videoProgressUpdated(long currentPosition);
    }
}
