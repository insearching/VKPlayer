package com.android.vkplayer.entity;

/**
 * Created by insearching on 15.07.2014.
 */
public class PlayBackStatus {
    private int progress;



    private int duaration;

    public PlayBackStatus(int progress, int duaration){
        this.progress = progress;
        this.duaration = duaration;
    }

    public int getProgress() {
        return progress;
    }

    public int getDuaration() {
        return duaration;
    }
}