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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.vkplayer.api.APICallHelper;
import com.android.vkplayer.entity.PlayBackStatus;
import com.android.vkplayer.entity.Track;
import com.android.vkplayer.entity.TrackStatus;
import com.android.vkplayer.service.DownloadService;
import com.android.vkplayer.service.PlayerService;
import com.android.vkplayer.utils.JSONField;
import com.android.vkplayer.utils.KeyMap;
import com.android.vkplayer.view.AutoResizeTextView;
import com.android.vkplayer.view.LoadMoreListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements APICallHelper.APIListener, PlayerService.SongStatusListener, DownloadService.DownloadListener {

    private static final String TAG = "TAG";
    private static final int TRACKS_COUNT = 100;
    private String accessToken;
    private LoadMoreListView mListview;
    private AutoResizeTextView titleTv;
    private ImageView playPauseIv;
    private ImageView prevIv;
    private ImageView nextIv;
    private ProgressBar progressBar;
    private RelativeLayout controlPanel;

    private MusicAdapter mAdapter;
    private PlayerService mPlayerService;
    private DownloadService mDownloadService;
    private PlayerReceiver mReciever;
    private String mPath;
    private String curTitle = "";
    private ArrayList<Track> mTrackList = new ArrayList<Track>();
    private ArrayList<String> downloadList = new ArrayList<String>();
    private ArrayList<String> playList = new ArrayList<String>();
    private boolean isSeaking = false;
    private int offset = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPath = getExternalCacheDir().getPath() + "/";
        mListview = (LoadMoreListView) findViewById(R.id.listView);
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                progressBar.setVisibility(View.VISIBLE);
                controlPanel.setVisibility(View.INVISIBLE);
                playBack(position);
            }
        });

        mListview.setOnLoadMoreListener(new LoadMoreListView.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                offset += 100;
                loadMusicList();
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        controlPanel = (RelativeLayout) findViewById(R.id.controlPanel);

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
                Track track = findItemByAid(aid);
                if (track == null)
                    return;
                int position = mAdapter.getItemPosition(track);
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
                Track track = findItemByAid(aid);
                if (track == null)
                    return;
                int position = mAdapter.getItemPosition(track);
                if (position != -1)
                    playBack(position + 1);
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            accessToken = bundle.getString(KeyMap.ACCESS_TOKEN);
        }

        if (savedInstanceState != null) {
            accessToken = savedInstanceState.getString(KeyMap.ACCESS_TOKEN);
            mTrackList = savedInstanceState.getParcelableArrayList(KeyMap.TRACK_LIST);
            mAdapter = new MusicAdapter(this, mTrackList);
            mListview.setAdapter(mAdapter);
            downloadList = savedInstanceState.getStringArrayList(KeyMap.DOWNLOAD_LIST);
            playList = savedInstanceState.getStringArrayList(KeyMap.PLAY_LIST);
            curTitle = savedInstanceState.getString(KeyMap.TITLE);

        } else {
            loadMusicList();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KeyMap.ACCESS_TOKEN, accessToken);
        outState.putParcelableArrayList(KeyMap.TRACK_LIST, mTrackList);
        outState.putStringArrayList(KeyMap.DOWNLOAD_LIST, downloadList);
        outState.putStringArrayList(KeyMap.PLAY_LIST, playList);
        outState.putString(KeyMap.TITLE, curTitle);
        outState.putInt(KeyMap.OFFSET, offset);
        super.onSaveInstanceState(outState);
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        titleTv.setText(curTitle);
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
        ArrayList<Track> data = new ArrayList<Track>();
        try {
            if(!json.has(JSONField.RESPONSE)) {
                mListview.onLoadMoreComplete();
                return;
            }
            JSONArray array = json.getJSONArray(JSONField.RESPONSE);
            if(array.length() == 0) {
                mListview.onLoadMoreComplete();
                return;
            }
            for (int i = 0; i < array.length(); i++) {
                Track track = new Track(array.getJSONObject(i));
                track.setTrackStatus(isFileOnDevice(track.getAid()), 0);
                data.add(track);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mTrackList.addAll(data);
        if(mAdapter == null) {
            mAdapter = new MusicAdapter(MainActivity.this, mTrackList);
            mListview.setAdapter(mAdapter);
        }
        else {
            mAdapter.addItems(data);
        }
        mListview.onLoadMoreComplete();
    }


    private void loadMusicList() {
        APICallHelper helper = APICallHelper.getInstance();
        helper.attachListener(this);
        helper.getMusicList("https://api.vk.com/method/audio.get?access_token=" + accessToken
                + "&count=" + TRACKS_COUNT + "&offset=" + offset + "&need_user=0");
    }

    private void playBack(int position) {
        Track track = mAdapter.getItem(position);
        String url = track.getUrl();
        String audioId = track.getAid();
        curTitle = track.getArtist() + " — " + track.getTitle();
        titleTv.setText(curTitle);

        boolean isOnDevice = isFileOnDevice(audioId);
        String path = isOnDevice ? mPath + audioId : url;

        if (!isOnDevice && !isFileDownloading(audioId)) {
            downloadList.add(audioId);
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
                playList.remove(currentAid);
            }
        } else {
            if (currentPath != null) {
                if (!currentPath.equals(url) && !currentPath.equals(mPath + audioId)) {
                    mPlayerService.setDataSource(path, url, audioId);
                    playList.remove(currentAid);
                }
            } else {
                mPlayerService.setDataSource(path, url, audioId);
            }
            mPlayerService.playPause();
        }
    }

    private boolean isFileDownloading(String aid) {
        for (String _aid : downloadList) {
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
        playList.add(aid);
        playPauseIv.setImageResource(R.drawable.pause);

        playPauseIv.setClickable(true);
        prevIv.setClickable(true);
        nextIv.setClickable(true);
        progressBar.setVisibility(View.INVISIBLE);
        controlPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void OnSongFinished(String url) {
        Track track = findItemByUrl(url);
        if (track != null) {
            int position = mAdapter.getItemPosition(track);
            playBack(position + 1);
        }
    }

    @Override
    public void OnPlaybackStatusChanged(String url, int time) {
        Log.d(url, "" + time);

        Track track = findItemByUrl(url);
        if (track == null)
            return;

        int duration = track.getDuration();

        String aid = track.getAid();
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

    private Track findItemByAid(String aid) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Track track = mAdapter.getItem(i);
            if (track.getAid().equals(aid)) {
                return track;
            }
        }
        return null;
    }

    private Track findItemByUrl(String url) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Track track = mAdapter.getItem(i);
            if (track.getUrl().equals(url)) {
                return track;
            }
        }
        return null;
    }

    @Override
    public void onProgressChanged(String aid, int progress) {
    }

    @Override
    public void onDownloadCompleted(String aid) {
        Track track = findItemByAid(aid);
        track.setTrackStatus(isFileOnDevice(aid), 0);
        downloadList.remove(aid);
    }

    class MusicAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private ArrayList<Track> data;

        public MusicAdapter(Context context, ArrayList<Track> data) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.data = data;
        }

        public void addItems(ArrayList<Track> additionalData){
            data.addAll(additionalData);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Track getItem(int position) {
            return data.get(position);
        }

        public int getItemPosition(Track track) {
            for (int i = 0; i < data.size(); i++) {
                Track _track = data.get(i);
                if (_track.equals(track))
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
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Track track = getItem(position);
            holder.labelTv.setText(track.getArtist() + " - " + track.getTitle());
            holder.durationTv.setText(track.getDuration() / 60 + ":" + (track.getDuration() % 60 <= 9 ? 0 + "" + track.getDuration() % 60 : track.getDuration() % 60));

            TrackStatus status = track.getTrackStatus();
            convertView.setBackgroundColor(getResources().getColor(status.isDownloaded() ? R.color.download_bg : R.color.white));

            boolean isPlaying = false;
            for (String aid : playList) {
                if (aid.equals(track.getAid()))
                    isPlaying = true;
            }

            PlayBackStatus playBackStatus = track.getPlayBackStatus();
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
