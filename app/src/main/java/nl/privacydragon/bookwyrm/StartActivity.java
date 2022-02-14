package nl.privacydragon.bookwyrm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class StartActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        //Context context = StartActivity.this;
        String defaultValue = "none";
        SharedPreferences sharedPref = StartActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
        String server = sharedPref.getString(getString(R.string.server), defaultValue);
        SharedPreferences sharedPrefName = StartActivity.this.getSharedPreferences(getString(R.string.name), Context.MODE_PRIVATE);
        String name = sharedPrefName.getString(getString(R.string.name), defaultValue);
        SharedPreferences sharedPrefPass = StartActivity.this.getSharedPreferences(getString(R.string.pw), Context.MODE_PRIVATE);
        String pass= sharedPrefPass.getString(getString(R.string.pw), defaultValue);
        myWebView.setWebViewClient(new MyWebViewClient(){
            public void onPageFinished(WebView view, String url) {

                view.loadUrl("javascript:(function() { document.getElementById('id_password_confirm').value = '" + pass + "'; ;})()");
                view.loadUrl("javascript:(function() { document.getElementById('id_localname_confirm').value = '" + name + "'; ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "/login') { document.getElementsByName(\"login-confirm\")[0].submit();} ;})()");

            }
        });
        //String data = "localname="+name+"&password="+pass;
        //String data = "";
        //myWebView.loadDataWithBaseURL("https://httpbin.org");
        //myWebView.loadDataWithBaseURL("https://httpbin.org/", data, "", "", "");
        //myWebView.postUrl("https://"+ server + "/login", EncodingUtils.getBytes(data, "base64"));
        myWebView.loadUrl("https://" + server + "/login");
        //myWebView.loadUrl("javascript:document.getElementsById('id_password_confirm').value = \""+pass+"\"");
    }
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            SharedPreferences sharedPref = StartActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
            String defaultValue = "none";
            String server = sharedPref.getString(getString(R.string.server), defaultValue);
            if (server.equals(request.getUrl().getHost())) {
                // This is my website, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
            startActivity(intent);
            return true;
        }
    }
}