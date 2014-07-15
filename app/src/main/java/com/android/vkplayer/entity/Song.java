package com.android.vkplayer.entity;

import android.text.Html;

import com.android.vkplayer.utils.JSONField;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by insearching on 08.07.2014.
 */
public class Song {

    private String aid;
    private String ownerId;
    private String artist;
    private String title;
    private int duration;
    private String url;
    private String lyricsId = null;
    private String genre;

    private TrackStatus status = null;

    public Song(JSONObject song){
        try {
            aid = song.getString(JSONField.AID);
            ownerId = song.getString(JSONField.OWNER_ID);
            artist = Html.fromHtml(song.getString(JSONField.ARTIST)).toString();
            title = Html.fromHtml(song.getString(JSONField.TITLE)).toString();
            duration = song.getInt(JSONField.DURATION);
            url = song.getString(JSONField.URL);
            if(song.has(JSONField.LYRICS_ID))
                lyricsId = song.getString(JSONField.LYRICS_ID);
            if(song.has(JSONField.GENRE))
                genre = song.getString(JSONField.GENRE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        status = new TrackStatus(false, 0);
    }

    public String getAid() {
        return aid;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public String getUrl() {
        return url;
    }

    public String getLyricsId() {
        return lyricsId;
    }

    public String getGenre() {
        return genre;
    }

    public void setTrackStatus(boolean isDownloaded, int progress) {
        status = new TrackStatus(isDownloaded, progress);
    }

    public TrackStatus getTrackStatus(){
        return status;
    }
}
