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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.vkplayer.api.APICallHelper;
import com.android.vkplayer.entity.Song;
import com.android.vkplayer.entity.TrackStatus;
import com.android.vkplayer.service.DownloadService;
import com.android.vkplayer.service.PlayerService;
import com.android.vkplayer.utils.JSONField;
import com.android.vkplayer.utils.KeyMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements APICallHelper.APIListener, PlayerService.SongStatusListener, DownloadService.DownloadListener {

    private static final String TAG = "TAG";
    private String accessToken;
    private ListView mListview;
    private TextView titleTv;
    private ImageView playPauseIv;
    private ImageView prevIv;
    private ImageView nextIv;

    private MusicAdapter mAdapter;
    private PlayerService mPlayerService;
    private DownloadService mDownloadService;
    private PlayerReciever mReciever;
    private String mPath;
    private ArrayList<String> downloadings = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListview = (ListView) findViewById(R.id.listView);
        mListview.setOnItemClickListener(onSongClickListener);

        titleTv = (TextView) findViewById(R.id.titleTv);
        playPauseIv = (ImageView) findViewById(R.id.playPauseIv);
        prevIv = (ImageView) findViewById(R.id.prevIv);
        nextIv = (ImageView) findViewById(R.id.nextIv);

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

        mReciever = new PlayerReciever();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(KeyMap.ACTION_PLAYER);
        registerReceiver(mReciever, intentFilter);

        Intent intent = new Intent(this, PlayerService.class);
        bindService(intent, mPlayerConnection,
                BIND_AUTO_CREATE);
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

    private String currentUrl = null;
    private AdapterView.OnItemClickListener onSongClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Song song = mAdapter.getItem(position);
            String url = song.getUrl();
            String audioId = song.getAid();
            titleTv.setText(song.getArtist() + " — " + song.getTitle());

            boolean isOnDevice = isFileOnDevice(audioId);
            String path = isOnDevice ? mPath + audioId : url;

            if (!isOnDevice) {
                mDownloadService.downloadFile(url, audioId);
            }


            if (mPlayerService == null) {
                processStartService(path, audioId, isOnDevice);
            } else {
                currentUrl = mPlayerService.getUrl();
                unbindService(mPlayerConnection);
                stopService(new Intent(MainActivity.this, PlayerService.class));
                mPlayerService = null;

                // same track selected, then stop playback
                if (currentUrl != null && currentUrl.equals(path)) {
                    currentUrl = null;
                }

                // another track selected
                else {
                    processStartService(path, audioId, isOnDevice);
                }
            }
        }
    };

    public boolean isFileOnDevice(String id) {
        File file = new File(mPath + id);
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

    private void processStartService(String filePath, String audioId, boolean isFileOnDevice) {
        Intent intent = new Intent(this, PlayerService.class);
        if (isFileOnDevice) {
            intent.putExtra(KeyMap.FILE_PATH, filePath);
        } else {
            intent.putExtra(KeyMap.URL, filePath);
        }
        intent.putExtra(KeyMap.AUDIO_ID, audioId);
        startService(intent);
        bindService(intent, mPlayerConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void OnSongLoaded(String url) {
//        Toast.makeText(this, url + " is playing", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProgressChanged(String aid, int progress) {
        int position = -1;
        for(int i=0; i<mAdapter.getCount(); i++){
            if(mAdapter.getItem(i).getAid().equals(aid))
                position = i;
        }

        if (position != -1) {
            downloadings.add(aid);
            mAdapter.getItem(position).setTrackStatus(progress == 100, progress);
            mAdapter.notifyDataSetChanged();
        }

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
                holder.downloadPb = (ProgressBar) convertView.findViewById(R.id.downloadPb);
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


            if(song.getTrackStatus().getProgress() > 0) {
                holder.downloadPb.setVisibility(View.VISIBLE);
                holder.downloadPb.setProgress(status.getProgress());
            }

            boolean isDownloading = false;
            for(String aid : downloadings){
                if(aid.equals(song.getAid()))
                    isDownloading = true;
            }
            if(!isDownloading || song.getTrackStatus().getProgress() == 100)
                holder.downloadPb.setVisibility(View.INVISIBLE);

            return convertView;
        }

        class ViewHolder {
            TextView labelTv;
            TextView durationTv;
            ProgressBar downloadPb;
            CheckBox downloadStatusCb;
        }
    }

    class PlayerReciever extends BroadcastReceiver {
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
