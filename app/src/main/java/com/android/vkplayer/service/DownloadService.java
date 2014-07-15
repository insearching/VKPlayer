package com.android.vkplayer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class DownloadService extends Service {
    private static final String TAG = "DOWANLOAD SERVICE";
    private DownloadListener callback;
    private Integer mProgress = 0;
    private IBinder mBinder = new FileDownloadBinder();
    private int startId;

    public class FileDownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startId = startId;
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public void downloadFile(String url, String aid){
        new DownloadFileTask(startId, aid).executeOnExecutor(Executors.newFixedThreadPool(2), url);
    }
    /**
     * Downloads file from service
     * param[0] - request URL
     * param[1] - accessToken
     */
    class DownloadFileTask extends AsyncTask<String, Integer, String> {
        int startId;
        String aid;

        public DownloadFileTask(int startId, String aid) {
            this.startId = startId;
            this.aid = aid;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = 0;
        }

        @Override
        protected String doInBackground(String... param) {

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(param[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(createFile(getExternalCacheDir().getPath(), aid));

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return param[0];
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            if ((progress[0] % 5) == 0 && mProgress != progress[0]) {
                mProgress = progress[0];
                callback.onProgressChanged(aid, mProgress);
//                BoxHelper.updateDownloadNotification(mContext, fileName, getString(R.string.downloading), mProgress, android.R.drawable.stat_sys_download, false);
//                if (callback != null) {
//                    callback.onProgressChanged(mProgress, fileName, getString(R.string.downloading));
//                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("TAG", "Downloaded " + result);

//            if (callback != null)
//                callback.onDownloadCompleted(Integer.parseInt(position), fileName, result);
//            if (result != null && result == HttpURLConnection.HTTP_OK) {
//                BoxHelper.showNotification(mContext, fileName, getString(R.string.download_completed), path, android.R.drawable.stat_sys_download_done);
//
//            } else {
//                BoxHelper.showNotification(mContext, fileName, getString(R.string.download_failed), path, android.R.drawable.stat_notify_error);
//                Toast.makeText(mContext, getString(R.string.download_failed) + " " + result, Toast.LENGTH_LONG).show();
//            }
            stopSelf(startId);
        }
    }

    private File createFile(String path, String fileName) {
        String sFolder = path + "/";
        File file = new File(sFolder);
        if (!file.exists())
            file.mkdirs();

        try {
            file = new File(sFolder + fileName);

            if (!file.createNewFile()) {
                file.delete();
                if (!file.createNewFile()) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    public void attachListener (Context context) {
        callback = (DownloadListener) context;
    }

    public interface DownloadListener {

        public void onProgressChanged(String aid, int progress);

    }

}
