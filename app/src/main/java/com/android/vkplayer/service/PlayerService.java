package com.android.vkplayer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.android.vkplayer.utils.KeyMap;

import java.io.IOException;

public class PlayerService extends Service {
    private MediaPlayer player;
    private boolean isRunning;
    private boolean isOnDevice;
    private PlayerTask task;
    private String filePath;
    private String audioId;
    private SongStatusListener callback;
    private final IBinder mBinder = new PlayerBinder();


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getUrl() {
        return filePath;
    }
    public String getAudioId() {
        return audioId;
    }

    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        String path = null;
        String url = null;
        if(extras.containsKey(KeyMap.FILE_PATH)) {
            filePath = path = extras.getString(KeyMap.FILE_PATH);
            audioId = extras.getString(KeyMap.AUDIO_ID);
            isOnDevice = true;
        }

        if(extras.containsKey(KeyMap.URL)){
            filePath = url = extras.getString(KeyMap.URL);
            isOnDevice = false;
        }

        if (!isRunning) {
            isRunning = true;
            task = new PlayerTask();
            if(isOnDevice)
                task.execute(path);
            else
                task.execute(url);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (task != null)
            task.cancel(true);
        if (player != null) {
            player.pause();
            player.release();
        }
    }

    class PlayerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                player = new MediaPlayer();
                player.setDataSource(params[0]);
                player.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            callback.OnSongLoaded(result);

            player.start();
            Intent intent = new Intent();
            intent.setAction(KeyMap.ACTION_PLAYER);

            intent.putExtra(KeyMap.PLAYING, true);
            intent.putExtra(KeyMap.URL, result);
            sendBroadcast(intent);
        }
    }

    public void attachListener(Context context){
        callback = (SongStatusListener) context;
    }

    public interface SongStatusListener {
        public void OnSongLoaded(String url);
    }
}
