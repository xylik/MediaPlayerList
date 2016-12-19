package hr.fer.com.mplayerlist.model;

import android.net.Uri;

/**
 * Created by andrijanstankovic on 02/05/16.
 */
public class MomentData {
    private int id = -1;
    private long startTimeMs = -1;
    private long endTimeMs = -1;
    private String title = "";
    private Uri thumbnail;
    private Uri videoPath;

    public MomentData(int id, long startTimeMs, long endTimeMs, String title, Uri thumbnail, Uri videoPath) {
        this.id = id;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.title = title;
        this.thumbnail = thumbnail;
        this.videoPath = videoPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public void setEndTimeMs(long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Uri getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Uri thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Uri getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(Uri videoPath) {
        this.videoPath = videoPath;
    }
}
