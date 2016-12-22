package hr.fer.com.mplayerlist.player;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.TextureView;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by andrijanstankovic on 02/05/16.
 */
public class VideoPlayManager {
    //region Contructor
    public static final String TAG = VideoPlayManager.class.getSimpleName();

    private static VideoPlayManager instance;
    private static boolean isPlaying = false;
    private static long lastPlayerReleasedMs = System.currentTimeMillis();

    private IPlayer player;
    int momentId;
    private PlayRequest lastPlayRequest;

    private Context ctx;
    private TextureView drawingView;

    private IPlayer.OnVideoSizeChangeListener mVideoSizeChangeListener;
    private IPlayer.OnPlayerStartedDrawingListener mPlayerStartedDrawingListener;
    private IPlayer.OnPlayerStateChangeListener mPlayerStateChangeListener;
    private IPlayer.OnPlayerErrorListener mOnErrorListener;
    private IPlayer.OnVideoProgressListener mVideoProgressListener;

    public static synchronized VideoPlayManager instance() {
        if(instance == null) instance = new VideoPlayManager();
        return instance;
    }

    private VideoPlayManager() {
        //empty
    }
    //endregion

    //region Player Api
    public synchronized void playFromUri(Uri httpVideoUri, int videoId, long startMs, long endMs, boolean playInLoop, boolean playWhenReady) {
        Log.d("igorLog", "playFromUri() -> playerMng: momentId:" + videoId);
        lastPlayRequest = new PlayRequest(videoId, startMs, endMs, httpVideoUri, playInLoop, playWhenReady);
//        if(isPlayerPlaying() || !setIsPlaying(true)) return;
        if(isPlayerPlaying()) return;
        else {
            isPlaying = true;
            playFromUriInternal(httpVideoUri, videoId, startMs, endMs, playInLoop, playWhenReady);
        }
    }

    private void playFromUriInternal(Uri videoPath, int videoId, long startMs, long endMs, boolean playInLoop, boolean playWhenReady) {
        Log.d("igorLog", "playFromUriInternal() -> playerMng: momentId:" + videoId + " url:" + videoPath);
        try {
            pickPlayerBasedOnVideoId(videoId);

            if(player instanceof MediaPlayerImp) {
                Log.d("igorLog", "playWithMediaPlayer() -> momentId:" + videoId + "\t\tfile:" + videoPath + "http:" + videoPath);
                player.playFromUri(videoPath, videoId, startMs, endMs, playInLoop, playWhenReady);
            } else {
                throw new RuntimeException("Not expected case!");
            }
        }catch(Exception ex){
            Log.e("igorLog", "NativeException");
            ex.printStackTrace();
            releasePlayer();
            if(mOnErrorListener != null) mOnErrorListener.onPlayerError(ex);
        }
    }

    public void pickPlayerBasedOnVideoId(int videoId) {
        this.momentId = videoId;
        player = MediaPlayerImp.instance(ctx, drawingView);
        player.setPlayerStateChangeListener(mPlayerStateChangeListener);
        player.setPlayerErrorListener(mOnErrorListener);
        player.setVideoSizeChangeListener(mVideoSizeChangeListener);
        player.setPlayerStartedDrawingListener(mPlayerStartedDrawingListener);
        player.setProgressListener(mVideoProgressListener);
    }

    public void setDisplay(TextureView drawingView) {
        this.drawingView = drawingView;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    public void play() {
        Log.d("igorLog", "play() momentId:" + momentId);
        if(player == null) return;
        player.play();
    }

    public void pause() {
        Log.d("igorLog", "pause() momentId:" + momentId);
        if(player == null) return;
        player.pause();
    }

    public void stop() {
        Log.d("igorLog", "stop() momentId:" + momentId);
        if(player == null) return;
        player.stop();
    }

    public void seekTo(long seekPositionMS) {
        Log.d("igorLog", "seek() momentId:" + momentId);
        if(player == null) return;
        player.seekTo(seekPositionMS);
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getVideoDuration() {
        return player.getMediaDuration();
    }

    public synchronized void releasePlayer() {
        Log.d("igorLog", "release() -> playerMng : momentId:" + momentId);
        if(player == null) return;
        player.release();
        if(momentPlaySubscription != null && !momentPlaySubscription.isUnsubscribed()){
            momentPlaySubscription.unsubscribe();
            momentPlaySubscription = null;
        }
        momentId = -1;
        setIsPlaying(false);
    }

    private static boolean shouldIgnorePlay;
    public static synchronized void ignoreAnyPlayInProgress() {
        VideoPlayManager.shouldIgnorePlay = true;
    }

    public static final long SWIPE_IGNORE_TRASHOLD = 250;
    private Subscription momentPlaySubscription;
    private synchronized boolean setIsPlaying(boolean isPlaying) {
        long currentMs = System.currentTimeMillis();
        if(isPlaying){
            VideoPlayManager.shouldIgnorePlay = false;
            long elapsedTime = currentMs - lastPlayerReleasedMs;
            if(elapsedTime > SWIPE_IGNORE_TRASHOLD) {
                Log.d("igorLog", "elapsedTime=" + elapsedTime + " ms");
                if(momentPlaySubscription != null && !momentPlaySubscription.isUnsubscribed()) momentPlaySubscription.unsubscribe();
                return VideoPlayManager.isPlaying = true;
            }else{
                Log.d("igorLog", "<=" + SWIPE_IGNORE_TRASHOLD +" elapsedTime=" + elapsedTime + " ms");
                momentPlaySubscription = Observable.timer(SWIPE_IGNORE_TRASHOLD - elapsedTime, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(tick -> {
                            if(VideoPlayManager.shouldIgnorePlay){
                                VideoPlayManager.shouldIgnorePlay = false;
                                return;
                            }

                            VideoPlayManager.isPlaying = true;
                            playFromUriInternal(
                                    lastPlayRequest.videoUri,
                                    lastPlayRequest.videoId,
                                    lastPlayRequest.startMs,
                                    lastPlayRequest.endMs,
                                    lastPlayRequest.playInLoop,
                                    lastPlayRequest.playWhenReady);
                        });
                return false;
            }
        }else {
            lastPlayerReleasedMs = currentMs;
            VideoPlayManager.isPlaying = false;
            return true;
        }
    }

    private synchronized boolean isPlayerPlaying() {
        return isPlaying;
    }
    //endregion

    //region Player state listener setters
    public void setPlayerStateChangeListener(IPlayer.OnPlayerStateChangeListener listener) {
        mPlayerStateChangeListener = listener;
        if(player != null)player.setPlayerStateChangeListener(listener);
    }

    public void setPlayerErrorListener(IPlayer.OnPlayerErrorListener listener) {
        mOnErrorListener = listener;
        if(player != null) player.setPlayerErrorListener(listener);
    }

    public void setVideoSizeChangeListener(IPlayer.OnVideoSizeChangeListener listener) {
        mVideoSizeChangeListener = listener;
        if(player != null) player.setVideoSizeChangeListener(listener);
    }

    public void setPlayerStartedDrawingListener(IPlayer.OnPlayerStartedDrawingListener listener) {
        mPlayerStartedDrawingListener = listener;
        if(player != null) player.setPlayerStartedDrawingListener(listener);
    }

    public void setProgressListener(IPlayer.OnVideoProgressListener listener) {
        mVideoProgressListener = listener;
        if(player != null) player.setProgressListener(listener);
    }
    //endregion

    private static class PlayRequest {
        public PlayRequest(int videoId, long startMs, long endMs, Uri videoUri, boolean playInLoop, boolean playWhenReady) {
            this.videoId = videoId;
            this.startMs = startMs;
            this.endMs = endMs;
            this.videoUri = videoUri;
            this.playInLoop = playInLoop;
            this.playWhenReady = playWhenReady;
        }

        int videoId;
        long startMs;
        long endMs;
        Uri videoUri;
        boolean playInLoop;
        boolean playWhenReady;
    }
}

