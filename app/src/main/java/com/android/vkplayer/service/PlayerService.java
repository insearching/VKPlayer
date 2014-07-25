package com.android.vkplayer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.android.vkplayer.utils.KeyMap;

import java.io.IOException;

public class PlayerService extends Service {
    private MediaPlayer player;
    private String path;
    private PlayerTask task;
    private SongStatusListener callback;
    private final IBinder mBinder = new PlayerBinder();
    private String url;
    private String aid;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public String getPath() {
        return path;
    }

    public String getAid() {
        return aid;
    }

    public void goToPosition(int seconds) {
        player.seekTo(seconds * 1000);
    }

    public boolean isPlaying() {
        if (player != null)
            return player.isPlaying();
        return false;
    }

    public void playPause() {
        if (player == null)
            return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
    }

    public void setDataSource(String path, String url, String aid) {
        this.path = path;
        this.url = url;
        this.aid = aid;

        if (task != null)
            task.cancel(true);
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        task = new PlayerTask();
        task.execute(path);
    }


    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    class PlayerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(final String... params) {
            try {
                player = new MediaPlayer();
                player.setDataSource(params[0]);

                mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (player != null) {
                            int mCurrentPosition = player.getCurrentPosition() / 1000;
                            if (mCurrentPosition > 0)
                                callback.OnPlaybackStatusChanged(url, mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 1100);
                    }
                };


                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        callback.OnSongFinished(url);
                    }
                });
                player.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            callback.OnSongLoaded(aid);

            player.start();
            mRunnable.run();

            Intent intent = new Intent();
            intent.setAction(KeyMap.ACTION_PLAYER);
            intent.putExtra(KeyMap.PLAYING, true);
            intent.putExtra(KeyMap.URL, result);
            sendBroadcast(intent);
        }
    }

    public void attachListener(Context context) {
        callback = (SongStatusListener) context;
    }

    public interface SongStatusListener {
        public void OnSongLoaded(String aid);

        public void OnSongFinished(String url);

        public void OnPlaybackStatusChanged(String url, int time);
    }
}
