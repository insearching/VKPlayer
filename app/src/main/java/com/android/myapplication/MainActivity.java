package com.android.myapplication;

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
import android.widget.ListView;
import android.widget.TextView;

import com.android.myapplication.api.APICallHelper;
import com.android.myapplication.entity.Song;
import com.android.myapplication.service.DownloadService;
import com.android.myapplication.service.PlayerService;
import com.android.myapplication.utils.JSONField;
import com.android.myapplication.utils.KeyMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements APICallHelper.APIListener {

    private static final String TAG = "TAG";
    private String accessToken;
    private ListView mListview;
    private MusicAdapter mAdapter;
    private PlayerService mService;
    private DownloadService mDownloadService;
    private PlayerReciever mReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListview = (ListView) findViewById(R.id.listView);
        mListview.setOnItemClickListener(onSongClickListener);

        Bundle bundle = getIntent().getExtras();
        accessToken = bundle.getString(KeyMap.ACCESS_TOKEN);

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
                songs.add(new Song(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mAdapter = new MusicAdapter(MainActivity.this, songs);
        mListview.setAdapter(mAdapter);

    }

    private String currentUrl = null;
    private AdapterView.OnItemClickListener onSongClickListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String url = mAdapter.getItem(position).getUrl();

            if(!isFileOnDevice(url)){
                mDownloadService.downloadFile(url);


                if (mService == null || mService.getUrl() == null) {
                    processStartService(url);
                }
                // player is playing track
                else {
                    currentUrl = mService.getUrl();
                    unbindService(mPlayerConnection);
                    stopService(new Intent(MainActivity.this, PlayerService.class));
                    mService = null;

                    // same station being selected, then stop playback
                    if (currentUrl != null && currentUrl.equals(url)) {
                        currentUrl = null;
                    }

                    // another track selected
                    else {
                        processStartService(url);
                    }
                }
            }
            else{

            }
        }
    };

    public boolean isFileOnDevice(String url) {
        File file = new File(getExternalCacheDir().getPath() + "/" + url);
        return file.exists() ? true : false;

    }

    private ServiceConnection mPlayerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            mService = ((PlayerService.PlayerBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private ServiceConnection mDownloadConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mDownloadService = ((DownloadService.FileDownloadBinder) binder).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDownloadService = null;
        }
    };

    private void processStartService(String url) {
        Intent intent = new Intent(this, PlayerService.class);
        intent.putExtra(KeyMap.URL, url);
        intent.addCategory(url);
        startService(intent);

        bindService(intent, mPlayerConnection, BIND_AUTO_CREATE);
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
                convertView = inflater.inflate(R.layout.row_song, parent, false);
                holder.labelTv = (TextView) convertView.findViewById(R.id.songTv);
                holder.durationTv = (TextView) convertView.findViewById(R.id.durationTv);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Song song = data.get(position);
            holder.labelTv.setText(song.getArtist() + " - " + song.getTitle());
            holder.durationTv.setText(song.getDuration() / 60 + ":" + (song.getDuration() % 60 <= 9 ? 0 + "" + song.getDuration() % 60 : song.getDuration() % 60));
            return convertView;
        }

        class ViewHolder {
            TextView labelTv;
            TextView durationTv;
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
