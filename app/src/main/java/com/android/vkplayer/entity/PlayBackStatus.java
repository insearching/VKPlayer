package com.android.vkplayer.entity;

/**
 * Created by insearching on 15.07.2014.
 */
public class PlayBackStatus {
    private int progress;
    private int duration;
    private boolean isStoped;

    public PlayBackStatus(int progress, int duration){
        this.progress = progress;
        this.duration = duration;
    }

    public void setStoped(boolean flag){
        isStoped = flag;
    }

    public boolean isStoped(){
        return isStoped;
    }

    public int getProgress() {
        return progress;
    }

    public int getDuration() {
        return duration;
    }
}