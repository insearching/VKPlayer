package com.android.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.myapplication.utils.Credentials;
import com.android.myapplication.utils.KeyMap;

import java.net.MalformedURLException;
import java.net.URL;

public class LoginActivity extends Activity {

    private static final String TAG = "TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WebView webView = (WebView) findViewById(R.id.webView);

        String url = "https://oauth.vk.com/authorize?client_id=" + Credentials.APP_ID + "&scope=audio&display=page&response_type=token";
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String urlStr) {
                try {
                    URL url = new URL(urlStr);

                    if (url.getProtocol().equals("https") && url.getHost().equals("oauth.vk.com")
                            && url.getRef().contains(KeyMap.ACCESS_TOKEN)) {
                        String accessToken = getAccessToken(url.getRef());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(KeyMap.ACCESS_TOKEN, accessToken);
                        startActivity(intent);
                        Log.d(TAG, accessToken);
                    } else {
                        view.loadUrl(urlStr);
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    private String getAccessToken(String ref) {
        int start = 0;
        int end = 0;
        for (int i = 0; i < ref.length(); i++) {
            if (ref.charAt(i) == '=') {
                start = i + 1;
            } else if (ref.charAt(i) == '&') {
                end = i;
                break;
            }
        }
        return ref.substring(start, end);
    }
}
