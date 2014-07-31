package com.android.vkplayer.entity;

/**
 * Created by insearching on 15.07.2014.
 */
public class DownloadStatus {
    boolean isDownloaded;
    int progress;

    public DownloadStatus(boolean isDownloaded, int progress){
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