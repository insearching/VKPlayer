package com.android.vkplayer.entity;

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
    private String lyrics_id;
    private String genre;

    public Song(JSONObject song){
        try {
            aid = song.getString(JSONField.AID);
            ownerId = song.getString(JSONField.OWNER_ID);
            artist = song.getString(JSONField.ARTIST);
            title = song.getString(JSONField.TITLE);
            duration = song.getInt(JSONField.DURATION);
            url = song.getString(JSONField.URL);
            lyrics_id = song.getString(JSONField.LYRICS_ID);
            genre = song.getString(JSONField.GENRE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    public String getLyrics_id() {
        return lyrics_id;
    }

    public String getGenre() {
        return genre;
    }

}
