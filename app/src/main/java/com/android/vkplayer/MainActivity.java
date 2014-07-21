package com.android.vkplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.vkplayer.api.APICallHelper;
import com.android.vkplayer.entity.PlayBackStatus;
import com.android.vkplayer.entity.Song;
import com.android.vkplayer.entity.TrackStatus;
import com.android.vkplayer.service.DownloadService;
import com.android.vkplayer.service.PlayerService;
import com.android.vkplayer.utils.JSONField;
import com.android.vkplayer.utils.KeyMap;
import com.android.vkplayer.view.AutoResizeTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements APICallHelper.APIListener, PlayerService.SongStatusListener, DownloadService.DownloadListener {

    private static final String TAG = "TAG";
    private String accessToken;
    private ListView mListview;
    private AutoResizeTextView titleTv;
    private ImageView playPauseIv;
    private ImageView prevIv;
    private ImageView nextIv;

    private MusicAdapter mAdapter;
    private PlayerService mPlayerService;
    private DownloadService mDownloadService;
    private PlayerReceiver mReciever;
    private String mPath;
    private ArrayList<String> downloadingsList = new ArrayList<String>();
    private ArrayList<String> playingList = new ArrayList<String>();
    private boolean isSeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListview = (ListView) findViewById(R.id.listView);
        mListview.setOnItemClickListener(onSongClickListener);

        titleTv = (AutoResizeTextView) findViewById(R.id.titleTv);
        playPauseIv = (ImageView) findViewById(R.id.playPauseIv);
        playPauseIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerService.playPause();
                playPauseIv.setImageResource(mPlayerService.isPlaying() ? R.drawable.pause : R.drawable.play);
            }
        });
        prevIv = (ImageView) findViewById(R.id.prevIv);
        prevIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String aid = mPlayerService.getAid();
                if (aid == null)
                    return;
                Song song = findItemByAid(aid);
                if (song == null)
                    return;
                int position = mAdapter.getItemPosition(song);
                if (position != -1)
                    playBack(position - 1);
            }
        });
        nextIv = (ImageView) findViewById(R.id.nextIv);
        nextIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String aid = mPlayerService.getAid();
                if (aid == null)
                    return;
                Song song = findItemByAid(aid);
                if (song == null)
                    return;
                int position = mAdapter.getItemPosition(song);
                if (position != -1)
                    playBack(position + 1);
            }
        });

        Bundle bundle = getIntent().getExtras();
        accessToken = bundle.getString(KeyMap.ACCESS_TOKEN);
        mPath = getExternalCacheDir().getPath() + "/";

        APICallHelper helper = APICallHelper.getInstance();
        helper.attachListener(this);
        helper.getMusicList("https://api.vk.com/method/audio.get?access_token=" + accessToken + "&count=100&offset=0&need_user=0");
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, DownloadService.class), mDownloadConnection, Context.BIND_AUTO_CREATE);

        mReciever = new PlayerReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(KeyMap.ACTION_PLAYER);
        registerReceiver(mReciever, intentFilter);

        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        bindService(intent, mPlayerConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReciever);
        unbindService(mPlayerConnection);
        unbindService(mDownloadConnection);
        super.onPause();
    }

    @Override
    public void onDataRecieved(JSONObject json) {
        ArrayList<Song> songs = new ArrayList<Song>();
        try {
            JSONArray array = json.getJSONArray(JSONField.RESPONSE);
            for (int i = 0; i < array.length(); i++) {
                Song song = new Song(array.getJSONObject(i));
                song.setTrackStatus(isFileOnDevice(song.getAid()), 0);
                songs.add(song);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mAdapter = new MusicAdapter(MainActivity.this, songs);
        mListview.setAdapter(mAdapter);
    }


    private AdapterView.OnItemClickListener onSongClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            playBack(position);
        }
    };

    private void playBack(int position) {
        Song song = mAdapter.getItem(position);
        String url = song.getUrl();
        String audioId = song.getAid();
        titleTv.setText(song.getArtist() + " — " + song.getTitle());

        boolean isOnDevice = isFileOnDevice(audioId);
        String path = isOnDevice ? mPath + audioId : url;

        if (!isOnDevice && !isFileDownloading(audioId)) {
            downloadingsList.add(audioId);
            mDownloadService.downloadFile(url, audioId);
        }

        playPauseIv.setClickable(false);
        prevIv.setClickable(false);
        nextIv.setClickable(false);

        String currentPath = mPlayerService.getPath();
        String currentAid = mPlayerService.getAid();
        if (mPlayerService.isPlaying()) {
            mPlayerService.playPause();
            if (!currentPath.equals(url) && !currentPath.equals(mPath + audioId)) {
                mPlayerService.setDataSource(path, url, audioId);
                playingList.remove(currentAid);
            }
        } else {
            if (currentPath != null) {
                if (!currentPath.equals(url) && !currentPath.equals(mPath + audioId)) {
                    mPlayerService.setDataSource(path, url, audioId);
                    playingList.remove(currentAid);
                }
            } else {
                mPlayerService.setDataSource(path, url, audioId);
            }
            mPlayerService.playPause();
        }
    }

    private boolean isFileDownloading(String aid) {
        for (String _aid : downloadingsList) {
            if (_aid.equals(aid)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFileOnDevice(String aid) {
        File file = new File(mPath + aid);
        return file.exists();

    }

    private ServiceConnection mPlayerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            mPlayerService = ((PlayerService.PlayerBinder) binder).getService();
            mPlayerService.attachListener(MainActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            mPlayerService = null;
        }
    };

    private ServiceConnection mDownloadConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mDownloadService = ((DownloadService.FileDownloadBinder) binder).getService();
            mDownloadService.attachListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDownloadService = null;
        }
    };

    @Override
    public void OnSongLoaded(String aid) {
        playingList.add(aid);
        playPauseIv.setImageResource(R.drawable.pause);

        playPauseIv.setClickable(true);
        prevIv.setClickable(true);
        nextIv.setClickable(true);
    }

    @Override
    public void OnSongFinished(String url) {
        Song song = findItemByUrl(url);
        if (song != null) {
            int position = mAdapter.getItemPosition(song);
            playBack(position + 1);
        }
    }

    @Override
    public void OnPlaybackStatusChanged(String url, int time) {
        Log.d(url, "" + time);

        Song song = findItemByUrl(url);
        if (song == null)
            return;

        int duration = song.getDuration();

        String aid = song.getAid();
        if (aid == null)
            return;

        int position = -1;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (mAdapter.getItem(i).getAid().equals(aid))
                position = i;
        }

        if (position != -1 && !isSeaking) {
            mAdapter.getItem(position).setPlayBackStatus(time, duration);
            mAdapter.notifyDataSetChanged();
        }
    }

    private Song findItemByAid(String aid) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Song song = mAdapter.getItem(i);
            if (song.getAid().equals(aid)) {
                return song;
            }
        }
        return null;
    }

    private Song findItemByUrl(String url) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Song song = mAdapter.getItem(i);
            if (song.getUrl().equals(url)) {
                return song;
            }
        }
        return null;
    }

    @Override
    public void onProgressChanged(String aid, int progress) {
    }

    @Override
    public void onDownloadCompleted(String aid) {
        Song song = findItemByAid(aid);
        song.setTrackStatus(isFileOnDevice(aid), 0);
        downloadingsList.remove(aid);
    }

    class MusicAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private ArrayList<Song> data;

        public MusicAdapter(Context context, ArrayList<Song> data) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Song getItem(int position) {
            return data.get(position);
        }

        public int getItemPosition(Song song) {
            for (int i = 0; i < data.size(); i++) {
                Song s = data.get(i);
                if (s.equals(song))
                    return i;
            }
            return -1;
        }

        @Override
        public long getItemId(int position) {
            return Long.parseLong(data.get(position).getAid());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_track, parent, false);
                holder.labelTv = (TextView) convertView.findViewById(R.id.titleTv);
                holder.durationTv = (TextView) convertView.findViewById(R.id.durationTv);
                holder.playbackSb = (SeekBar) convertView.findViewById(R.id.playbackPb);
                holder.downloadStatusCb = (CheckBox) convertView.findViewById(R.id.downloadStatusCb);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Song song = getItem(position);
            holder.labelTv.setText(song.getArtist() + " - " + song.getTitle());
            holder.durationTv.setText(song.getDuration() / 60 + ":" + (song.getDuration() % 60 <= 9 ? 0 + "" + song.getDuration() % 60 : song.getDuration() % 60));

            TrackStatus status = song.getTrackStatus();
            holder.downloadStatusCb.setChecked(status.isDownloaded());

            boolean isPlaying = false;
            for (String aid : playingList) {
                if (aid.equals(song.getAid()))
                    isPlaying = true;
            }

            PlayBackStatus playBackStatus = song.getPlayBackStatus();
            if (playBackStatus != null) {
                if (playBackStatus.getProgress() > 0) {
                    holder.playbackSb.setVisibility(View.VISIBLE);
                    holder.playbackSb.setMax(playBackStatus.getDuaration());
                    holder.playbackSb.setProgress(playBackStatus.getProgress());
                    holder.playbackSb.setOnSeekBarChangeListener(seekBarChangeListener);
                }

                if (playBackStatus.getProgress() == playBackStatus.getDuaration())
                    holder.playbackSb.setVisibility(View.INVISIBLE);
            }

            if (!isPlaying)
                holder.playbackSb.setVisibility(View.INVISIBLE);
            return convertView;
        }

        class ViewHolder {
            TextView labelTv;
            TextView durationTv;
            SeekBar playbackSb;
            CheckBox downloadStatusCb;
        }
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeaking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeaking = false;
            mPlayerService.goToPosition(seekBar.getProgress());
        }
    };

    private class PlayerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            boolean isPlaying = bundle.getBoolean(KeyMap.PLAYING, false);
            String url = bundle.getString(KeyMap.URL);

//            int position = findItemByUrl(url);
//            if (isPlaying && position != -1) {
//                mAdapter.setItemStatus(position, RadioAdapter.PlaybackStatus.PLAYING);
//                mAdapter.notifyDataSetChanged();
//            }
        }
    }
}
