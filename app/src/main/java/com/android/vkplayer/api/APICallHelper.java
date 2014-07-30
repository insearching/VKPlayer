package com.android.vkplayer.api;

import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by insearching on 07.07.2014.
 */
public class APICallHelper {
    private String TAG = "TAG";

    private APIListener callback;
    private static APICallHelper instance;

    public static APICallHelper getInstance() {
        if (instance == null) {
            instance = new APICallHelper();
        }
        return instance;
    }

    public void attachListener(Context context) {
        callback = (APIListener) context;
    }

    private APICallHelper() {
    }

    public void getMusicList(String url){
        new GetMusicTask().execute(url);
    }
    class GetMusicTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet get = new HttpGet(params[0]);
                HttpResponse response = httpclient.execute(get);
                return  EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result == null)
                return;
            try {
                callback.onDataRecieved(new JSONObject(result));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public interface APIListener {
        public void onDataRecieved(JSONObject json);
    }
}
