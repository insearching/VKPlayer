package com.android.vkplayer.entity;

/**
 * Created by insearching on 15.07.2014.
 */
public class TrackStatus {
    boolean isDownloaded;
    int progress;

    public TrackStatus(boolean isDownloaded, int progress){
        this.isDownloaded = isDownloaded;
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }
}