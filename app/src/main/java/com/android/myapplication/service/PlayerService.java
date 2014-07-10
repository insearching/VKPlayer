package com.android.myapplication.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import com.android.myapplication.utils.KeyMap;

import java.io.IOException;

public class PlayerService extends Service {
    private MediaPlayer player;
    private String mUrl = null;
    private boolean isRunning;
    private PlayerTask task;


    private final IBinder mBinder = new PlayerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getUrl() {
        return mUrl;
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
        mUrl = intent.getExtras().getString(KeyMap.URL);

        if (!isRunning) {
            isRunning = true;
            task = new PlayerTask();
            task.execute();
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

    class PlayerTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                player = new MediaPlayer();
                player.setDataSource(mUrl);
                player.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            player.start();
            Intent intent = new Intent();
            intent.setAction(KeyMap.ACTION_PLAYER);

            intent.putExtra(KeyMap.PLAYING, true);
            intent.putExtra(KeyMap.URL, mUrl);
            sendBroadcast(intent);
        }
    }
}
