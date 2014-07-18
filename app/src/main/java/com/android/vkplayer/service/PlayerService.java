package com.android.vkplayer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.vkplayer.utils.KeyMap;

import java.io.IOException;

public class PlayerService extends Service {
    private MediaPlayer player;
    private boolean isRunning;
    private boolean isOnDevice;
    private String path;
    private PlayerTask task;
    private SongStatusListener callback;
    private final IBinder mBinder = new PlayerBinder();
    private boolean isBinded;

    @Override
    public IBinder onBind(Intent intent) {
        isBinded = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isBinded = false;
        return true;
    }

    public String getPath() {
        return path;
    }

    public boolean isBinded() {
        return isBinded;
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

    public void setDataSource(String path, boolean isOnDevice) {
        this.path = path;
        if (task != null)
            task.cancel(true);
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        task = new PlayerTask();
        task.execute(path);
        this.isOnDevice = isOnDevice;
    }

    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = false;
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
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                int mCurrentPosition = player.getCurrentPosition() / 1000;
                Log.d("TAG", "" + mCurrentPosition);
            }
            mHandler.postDelayed(this, 1000);
        }
    };;

    class PlayerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                player = new MediaPlayer();
                player.setDataSource(params[0]);
                player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        Log.d("TAG", "" + percent);
                    }
                });


                mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (player != null) {
                            int mCurrentPosition = player.getCurrentPosition() / 1000;
                            Log.d("TAG", "" + mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                };

                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d("TAG", "Song finished");
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

            callback.OnSongLoaded(result);

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
        public void OnSongLoaded(String url);

        public void OnSongFinished(String url);

        public void OnPlaybackStatusChanged(String url, int time);
    }
}
