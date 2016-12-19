package hr.fer.com.mplayerlist.mvp;

import android.content.Context;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hr.fer.com.mplayerlist.App;
import hr.fer.com.mplayerlist.customView.MvpViewHolder;
import hr.fer.com.mplayerlist.R;
import hr.fer.com.mplayerlist.player.VideoPlayManager;
import hr.fer.com.mplayerlist.player.IPlayer;

public class MomentViewHolder extends MvpViewHolder<MomentPresenterImpl> implements MomentContract.View {
    //region Fields and Constructors
    public static final String TAG = MomentViewHolder.class.getSimpleName();

    @Bind (R.id.momentRootLyt)
    LinearLayout rootLyt;
    @Bind (R.id.playerSurface)
    TextureView playerView;
    @Bind (R.id.videoThubnailImg)
    ImageView videoThubnailImg;
    @Bind (R.id.spinnerProgressBar)
    ProgressBar progressBar;
    @Bind (R.id.momentCounterViewRL)
    RelativeLayout momentCounterView;
    @Bind (R.id.momentCounterTimeTV)
    TextView momentCounterTime;
    @Bind (R.id.videoDescTV)
    TextView videoDescTV;

    private VideoPlayManager playerManager;
    private Context ctx;

    public MomentViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);

        playerManager = VideoPlayManager.instance();
        ctx = view.getContext();
    }
    //endregion

    //region Click listeners
    @OnClick (R.id.playerSurface)
    void onPlayerSurfaceClick() {
        presenter.playerTouched();
    }
    //endregion

    //region View state setters
    @Override
    public void setState(boolean isEnabled) {
        rootLyt.animate().alpha(isEnabled ? 1 : 0.3f).setDuration(400).start();
        setIsUserInputEnabled(isEnabled);
    }

    @Override
    public void setIsUserInputEnabled(boolean isEnabled) {
        //empty
    }

    @Override
    public void setVideoThumbnail(Uri thumbnail) {
        if (thumbnail != null) Glide.with(ctx).load(thumbnail).into(videoThubnailImg);
        else videoThubnailImg.setImageDrawable(null);
    }

    @Override
    public void setVideoThumbnailVisibility(boolean isVisible) {
        videoThubnailImg.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setMomentTitle(String title) {
        videoDescTV.setText(title);
    }

    @Override
    public void setCounterTimeText(String timeLeft) {
        momentCounterTime.setText(timeLeft);
    }

    @Override
    public void setProgressBarVisiblity(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
    //endregion

    //region Dialogs
    @Override
    public void showMessage(Message message) {
        switch (message) {
            case PlayerError:
                showSnackMessage("Player error");
                break;
        }
    }

    private void showSnackMessage(String msg) {
        Snackbar.make(App.getCurrentActivity().findViewById(android.R.id.content).getRootView(), msg, Snackbar.LENGTH_SHORT).show();
    }
    //endregion

    //region Player controls

    @Override
    public void playVideoFromUri(String videoUrl, int videoId, long startMs, long endMs, boolean shouldPlayInLoop, boolean shouldPlayWhenReady) {
        playerManager.setDisplay(playerView);
        playerManager.setContext(ctx);
        playerManager.setPlayerStateChangeListener(playerStateChangeListener);
        playerManager.setVideoSizeChangeListener(videoSizeChangeListener);
        playerManager.setPlayerStartedDrawingListener(playerStartedDrawingListener);
        playerManager.setPlayerErrorListener(playerErrorListener);
        playerManager.setProgressListener(currentPosition -> {
            if (presenter != null) presenter.videoProgressUpdated(currentPosition);
        });

        playerManager.playFromUri(Uri.parse(videoUrl), videoId, startMs, endMs, shouldPlayInLoop, shouldPlayWhenReady);
    }

    @Override
    public void playVideo() {
        playerManager.play();
    }

    @Override
    public void pauseVideo() {
        playerManager.pause();
    }

    @Override
    public void seekVideo(int videoId, long positionMS) {
        playerManager.pickPlayerBasedOnVideoId(videoId);
        playerManager.seekTo(positionMS);
    }

    @Override
    public void stopVideo() {
        playerManager.releasePlayer();
    }
    //endregion

    //region PlayerListeners
    private final IPlayer.OnVideoSizeChangeListener videoSizeChangeListener = new IPlayer.OnVideoSizeChangeListener() {
        @Override
        public void onVideoSizeChanged(int videoWidth, int videoHeight, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            Log.d("igorLog", "videoSizeChanged() videoWidth=" + videoWidth + " videoHeight=" + videoHeight);
            ViewGroup.LayoutParams params = playerView.getLayoutParams();
            int newHeight = videoThubnailImg.getHeight();
            int w = params.height * videoWidth / videoHeight;
            Log.d("igorLog", "myMeasures: w="+ w + " h=" + newHeight);
        }
    };

    private final IPlayer.OnPlayerStartedDrawingListener playerStartedDrawingListener = new IPlayer.OnPlayerStartedDrawingListener() {
        @Override
        public void onPlayerStartedDrawing() {
            presenter.playerStartedDrawing();
        }
    };

    private final IPlayer.OnPlayerStateChangeListener playerStateChangeListener = new IPlayer.OnPlayerStateChangeListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            //TODO check why presenter is null, maybe when ViewHolder is unBinded; presenter is set to null and focus change happens earlier than bindViewHolder
            if (presenter != null) presenter.playerStateChanged(playWhenReady, playbackState);
        }
    };

    private final IPlayer.OnPlayerErrorListener playerErrorListener = new IPlayer.OnPlayerErrorListener() {
        @Override
        public void onPlayerError(Exception error) {
            if (presenter != null) presenter.playerErrorOccured(error);
        }
    };

    private final IPlayer.OnVideoProgressListener onVideoProgressListener = new IPlayer.OnVideoProgressListener() {
        @Override
        public void onProgressUpdate(long currentPosition) {
            presenter.videoProgressUpdated(currentPosition);
        }
    };
    //endregion

    //region Other
    @Override
    public void releaseResources() {
        playerManager = null;
        presenter = null;
        ctx = null;
    }
    //endregion
}
