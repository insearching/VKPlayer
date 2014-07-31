package com.android.vkplayer.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;

import com.android.vkplayer.utils.JSONField;

import org.json.JSONException;
import org.json.JSONObject;

public class Track implements Parcelable {

    private String aid;
    private String ownerId;
    private String artist;
    private String title;
    private int duration;
    private String url;
    private String lyricsId = null;
    private String genre;

    private DownloadStatus status = null;
    private PlayBackStatus playbackStatus;

    public Track(JSONObject song){
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

        status = new DownloadStatus(false, 0);
    }

    @Override
    public boolean equals(Object _track) {
        if(_track instanceof Track)
            return this.aid.equals(((Track)_track).getAid());
        return false;
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
        status = new DownloadStatus(isDownloaded, progress);
    }

    public DownloadStatus getTrackStatus(){
        return status;
    }

    public void setPlayBackStatus(int progress, int duration) {
        playbackStatus = new PlayBackStatus(progress, duration);
    }

    public PlayBackStatus getPlayBackStatus(){
        return playbackStatus;
    }

    protected Track(Parcel in) {
        aid = in.readString();
        ownerId = in.readString();
        artist = in.readString();
        title = in.readString();
        duration = in.readInt();
        url = in.readString();
        lyricsId = in.readString();
        genre = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(aid);
        dest.writeString(ownerId);
        dest.writeString(artist);
        dest.writeString(title);
        dest.writeInt(duration);
        dest.writeString(url);
        dest.writeString(lyricsId);
        dest.writeString(genre);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
}