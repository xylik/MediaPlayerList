package hr.fer.com.mplayerlist.player;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import net.protyposis.android.mediaplayer.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import hr.fer.com.mplayerlist.mvp.MomentContract;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Igor on 05/10/16.
 */
public class MediaPlayerImp implements IPlayer {
    public static final String TAG = MediaPlayerImp.class.getSimpleName();

    private OnVideoSizeChangeListener mVideoSizeChangeListener;
    private OnPlayerStartedDrawingListener mPlayerStartedDrawingListener;
    private OnPlayerStateChangeListener mPlayerStateChangeListener;
    private OnPlayerErrorListener mOnErrorListener;
    private OnVideoProgressListener mVideoProgressListener;

    private Uri mUri;
    private Map<String, String> mHeaders;
    private static long PROGRESS_UPDATE_INTERVAL_MS = 20;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private Context mContext = null;
    private TextureView mDrawingView;
    private SurfaceTexture mDrawingSurface = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mCurrentBufferPercentage;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;
    private int mSeekWhenPrepared = 0;  // recording the seek position while preparing
    private boolean playWhenReady = false;
    private Subscription timerSubscription;
    private boolean isPlayerReleased = true;
    private VideoPlayParameters videoPlayParameters;
    private boolean isFirstFrameRendered = false;

    private static IPlayer instance;

    public static class MediaPlayerImpException extends RuntimeException {
        public MediaPlayerImpException() {}
        public MediaPlayerImpException(String detailMessage) {
            super(detailMessage);
        }
        public MediaPlayerImpException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
        public MediaPlayerImpException(Throwable throwable) {
            super(throwable);
        }
    }

    public static synchronized IPlayer instance(Context ctx, TextureView tv) {
        return new MediaPlayerImp(ctx, tv);
    }

    private MediaPlayerImp(Context ctx, TextureView tv) {
        this.mContext = ctx;
        mDrawingView = tv;
        initVideoDimensions();
    }

    @Override
    public void playFromUri(Uri videoUri, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady) {
        throw new RuntimeException();
    }

    int videoId = -1;
    /**
     *  Before calling this method, setContext() and setDisplay() must be called!
     */
    @Override
    public synchronized void playFromUri(Uri videoUri, int videoId, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady) {
        this.videoId = videoId;
        videoPlayParameters = new VideoPlayParameters(videoUri, videoId, startPositionMs, endPositionMs, playInLoop, playWhenReady);
        mHeaders = new HashMap<>();
        this.playWhenReady = playWhenReady;
        mUri = videoUri;
        Log.d("ILruIndex", "mplayer playFromUri() -> momentId:" + videoId + " compensationTime:" + startPositionMs);
        Log.d("igorLog", "playFromUri() startTimeMs:" + startPositionMs + " endPositionMs:" + endPositionMs);

        isPlayerReleased = false;
        if(mSeekWhenPrepared == 0) seekTo(startPositionMs); //seek will be executed later when player is ready, here only mSeekWhenPrepared will be set
        openVideo();
        if(playWhenReady) play();
    }

    private void initVideoDimensions() {
        mDrawingView.setSurfaceTextureListener(mSurfaceTextureListener);
        if(mDrawingView.isAvailable()) {
            mDrawingSurface = mDrawingView.getSurfaceTexture();
            mSurfaceWidth = mDrawingView.getWidth();
            mSurfaceHeight = mDrawingView.getHeight();
        }

        if(mDrawingSurface != null) {
            mVideoWidth = mSurfaceWidth;
            mVideoHeight = mSurfaceHeight;
        }else {
            mVideoWidth = 0;
            mVideoHeight = 0;
        }

        //#VideoView implementation
        //mDrawingView.setFocusable(true);
        //mDrawingView.setFocusableInTouchMode(true);
        //mDrawingView.requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    private void openVideo() {
        if (isPlayerReleased || mUri == null || mDrawingSurface == null) return; // not ready for playback just yet, will try again later

        //#VideoView, we shouldn't clear the target state, because somebody might have called play() previously
        // release();
        if(mMediaPlayer != null) Log.d("igorLog", "openVideo() -> mp != null, currentState=" + mCurrentState);

        try {
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setSeekMode(MediaPlayer.SeekMode.PRECISE);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            mMediaPlayer.setSurface(new Surface(mDrawingSurface));
            //#Media player specific api
            //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            release();
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            release();
            return;
        }
    }

    private void startTimer() {
        Log.d("igorNext", "MP startTimer()");
        timerSubscription = Observable.interval(PROGRESS_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onBackpressureDrop(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        Log.d("igorNext", "time dropped:" + aLong);
                    }
                })
                .subscribe(
                        tick -> {
                            if(isPlayerReleased) return;
                            long currPosition = getCurrentPosition();
                            Log.d("igor3", "mediaPlayer tick() currPosition:" + currPosition);
                            if(currPosition < 0) mErrorListener.onError(mMediaPlayer, -1, -1); //When player breaks silently thats the way to detect its broken
                            if(videoPlayParameters.playInLoop && currPosition >= videoPlayParameters.endPositionMs) {
                                Log.d("igorLog", "revindVideo() getCurrentPosition()=" + currPosition + "endPosition=" + videoPlayParameters.endPositionMs + " videoDuration:" + getMediaDuration());
                                revindVideo();
                                return;
                            }
                            if(mVideoProgressListener != null) mVideoProgressListener.onProgressUpdate(currPosition);
                        },
                        error ->  {
                            Log.e(TAG, Log.getStackTraceString(error));
                        }
                );
    }

    private void revindVideo() {
        if(isPlayerReleased) return;
        pause();
        seekTo(videoPlayParameters.startPositionMs);
        play();
    }

    @Override
    public void setDisplay(TextureView tv) {
        mDrawingView = tv;
    }

    @Override
    public void setContext(Context ctx) {
        mContext = ctx;
    }

    @Override
    public void play() {
        if(isPlayerReleased) return;

        if (isInPlaybackState()) {
            if(mCurrentState == STATE_PAUSED || mTargetState == STATE_PAUSED){
                isFirstFrameRendered = false;
                startTimer();
            }
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if(isPlayerReleased) return;

        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        if(timerSubscription != null && !timerSubscription.isUnsubscribed()) timerSubscription.unsubscribe();
        mTargetState = STATE_PAUSED;
    }

    @Override
    public void stop() {
        if(isPlayerReleased) return;

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
        if(timerSubscription != null && !timerSubscription.isUnsubscribed()) timerSubscription.unsubscribe();
        mDrawingView.setSurfaceTextureListener(null);
        mDrawingView = null;
        mDrawingSurface = null;
        isPlayerReleased = true;
    }

    @Override
    public void seekTo(long msec) {
        if(isPlayerReleased) return;

        if (isInPlaybackState()) {
            mMediaPlayer.seekTo((int)msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = (int)msec;
        }
    }

    @Override
    public boolean isPlaying() {
        if(isPlayerReleased) return false;

        if(isInPlaybackState()) return mMediaPlayer.isPlaying();
        else return false;
    }

    @Override
    public long getCurrentPosition() {
        if(isPlayerReleased) return -1;

        if (isInPlaybackState()) return mMediaPlayer.getCurrentPosition();
        else return -1;
    }

    @Override
    public long getMediaDuration() {
        if(isPlayerReleased) return -1;

        if (isInPlaybackState()) return mMediaPlayer.getDuration();
        else return -1;
    }

    @Override
    public synchronized void release() {
        if(isPlayerReleased) return;

        isPlayerReleased = true;
        Log.d("igorLog", "release() -> mediaPlayer: momentId:" + videoId);

        mVideoSizeChangeListener = null;
        mPlayerStartedDrawingListener = null;
        mPlayerStateChangeListener = null;
        mOnErrorListener = null;
        mVideoProgressListener = null;

        if(timerSubscription != null && !timerSubscription.isUnsubscribed()){
            timerSubscription.unsubscribe();
            timerSubscription = null;
        }

        if(mDrawingView != null) {
            Log.d("igorLog", "drawingView -> null, momentId:" + videoId);
            mDrawingView.setSurfaceTextureListener(null);
            mDrawingView = null;
            mDrawingSurface = null;
        }

        if (mMediaPlayer != null) {
            Log.d("igorLog", "mediaPlayer -> null, momentId:" + videoId);
            mMediaPlayer.setOnPreparedListener(null);
            mMediaPlayer.setOnVideoSizeChangedListener(null);
            mMediaPlayer.setOnCompletionListener(null);
            mMediaPlayer.setOnErrorListener(null);
            mMediaPlayer.setOnBufferingUpdateListener(null);
            //#VideoView implementation
            //mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }

        videoId = -1;
        mUri = null;
        mHeaders = null;
        videoPlayParameters = null;
        mVideoHeight = mVideoWidth = 0;
        mSurfaceHeight = mSurfaceWidth = 0;
        mSeekWhenPrepared = 0;
        mCurrentBufferPercentage = 0;
        playWhenReady = false;
        mContext = null;
        isFirstFrameRendered = false;

        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    //region Player state listener setters
    public void setPlayerStateChangeListener(OnPlayerStateChangeListener l) {
        this.mPlayerStateChangeListener = l;
    }

    @Override
    public void setPlayerErrorListener(OnPlayerErrorListener l) {
        this.mOnErrorListener = l;
    }

    @Override
    public void setVideoSizeChangeListener(OnVideoSizeChangeListener l) {
        this.mVideoSizeChangeListener = l;
    }

    @Override
    public void setPlayerStartedDrawingListener(OnPlayerStartedDrawingListener l) {
        this.mPlayerStartedDrawingListener = l;
    }

    @Override
    public void setProgressListener(OnVideoProgressListener l) {
        this.mVideoProgressListener = l;
    }
    //endregion

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new MediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    if(isPlayerReleased) return;
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    if(mVideoSizeChangeListener != null) mVideoSizeChangeListener.onVideoSizeChanged(mVideoWidth, mVideoHeight, -1, -1);
                }
            };

    MediaPlayer.OnPreparedListener mPreparedListener =
        new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    if(isPlayerReleased) return;
                    mCurrentState = STATE_PREPARED;
                    Log.d("igorLog", "MediaPlayer videoDuration:" + mMediaPlayer.getDuration());
                    if(timerSubscription == null) startTimer();
                    if (mPlayerStateChangeListener != null) mPlayerStateChangeListener.onPlayerStateChanged(playWhenReady, MomentContract.Presenter.STATE_BUFFERING);

                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();

                    //mSeekWhenPrepared may be changed after seekTo() call
                    if (mSeekWhenPrepared != 0) seekTo(mSeekWhenPrepared);
                    if (mTargetState == STATE_PLAYING) play();
                }
        };

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if(isPlayerReleased) return;
                mCurrentState = STATE_PLAYBACK_COMPLETED;
                mTargetState = STATE_PLAYBACK_COMPLETED;
                int currPos = mp.getCurrentPosition();
                Log.d("igorNext", "MPlayer completeListener -> mId:" + videoId + " currentMs:" + currPos);
                Log.d("igorNext", "MPlayer completeListener -> mId:" + videoId + " playParams.endPositionMs:" + videoPlayParameters.endPositionMs);
                Log.d("igorNext", "MPlayer completeListener -> mId:" + videoId + " earlyStoppedFor:" + (videoPlayParameters.endPositionMs - currPos) + "ms");
                if(videoPlayParameters.playInLoop) {
                    revindVideo();
                } else {
                    if(mVideoProgressListener != null) {
                        if(timerSubscription != null && !timerSubscription.isUnsubscribed()) timerSubscription.unsubscribe();
                        mVideoProgressListener.onProgressUpdate(videoPlayParameters.endPositionMs);
                    }
                }
            }
        };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                Log.d(TAG, "Error: " + framework_err + "," + impl_err);
                mCurrentState = STATE_ERROR;
                mTargetState = STATE_ERROR;

                /* If an error handler has been supplied, use it and finish. */
                if (mOnErrorListener != null) mOnErrorListener.onPlayerError(new MediaPlayerImpException("Error: " + framework_err + "," + impl_err));
                release();
                return true;
            }
        };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if(isPlayerReleased) return;
                mCurrentBufferPercentage = percent;
            }
        };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int w, int h) {
            if(isPlayerReleased) return;

            mDrawingSurface = surface;
            openVideo();

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            if (mMediaPlayer != null && mTargetState == STATE_PLAYING) {
                if (mSeekWhenPrepared != 0) seekTo(mSeekWhenPrepared);
                play();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {
            if(isPlayerReleased) return;

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            if(mMediaPlayer != null && mTargetState == STATE_PLAYING) {
                if (mSeekWhenPrepared != 0) seekTo(mSeekWhenPrepared);
                play();
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d("igorLog", "surfaceDestroyed() -> momentId:" + videoId);
            if(isPlayerReleased) return true;
            release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if(!isFirstFrameRendered && mPlayerStateChangeListener != null && mTargetState == STATE_PLAYING) {
                Log.d("igorLog", " firstFrameRendered()");
                isFirstFrameRendered = true;
                mPlayerStateChangeListener.onPlayerStateChanged(playWhenReady, MomentContract.Presenter.STATE_READY);
            }
        }
    };

    private class VideoPlayParameters {
        public Uri videoUri;
        public int videoId;
        public long startPositionMs;
        public long endPositionMs;
        public boolean playInLoop;
        public boolean playWhenReady;

        public VideoPlayParameters(Uri videoUri, int videoId, long startPositionMs, long endPositionMs, boolean playInLoop, boolean playWhenReady) {
            this.videoUri = videoUri;
            this.videoId = videoId;
            this.startPositionMs = startPositionMs;
            this.endPositionMs = endPositionMs;
            this.playInLoop = playInLoop;
            this.playWhenReady = playWhenReady;
        }
    }
}
