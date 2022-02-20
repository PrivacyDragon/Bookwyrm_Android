package nl.privacydragon.bookwyrm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

public class HandlerActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        // End of auto-generated stuff
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        //The user credentials are stored in the shared preferences, so first they have to be read from there.
        String defaultValue = "none";
        SharedPreferences sharedPref = HandlerActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
        String server = sharedPref.getString(getString(R.string.server), defaultValue);
        SharedPreferences sharedPrefName = HandlerActivity.this.getSharedPreferences(getString(R.string.name), Context.MODE_PRIVATE);
        String name = sharedPrefName.getString(getString(R.string.name), defaultValue);
        SharedPreferences sharedPrefPass = HandlerActivity.this.getSharedPreferences(getString(R.string.pw), Context.MODE_PRIVATE);
        String pass = sharedPrefPass.getString(getString(R.string.pw), defaultValue);
        SharedPreferences sharedPrefMagic = HandlerActivity.this.getSharedPreferences(getString(R.string.q), Context.MODE_PRIVATE);
        String codeMagic = sharedPrefMagic.getString(getString(R.string.q), defaultValue);
        //If there is nothing configured yet, the user should be redirected to the main screen for logging in.
        if (server == "none") {
            startActivity(new Intent(HandlerActivity.this, nl.privacydragon.bookwyrm.MainActivity.class));
        }
        String pathMaybe = appLinkData.getPath();
        String toGoServer = "bla";
        if (pathMaybe.contains("user")) {
            //If the path contains 'user', it is a user profile, unless it is followed by something like 'review'.
            if (pathMaybe.contains("review") || pathMaybe.contains("generatednote") || pathMaybe.contains("quotation") || pathMaybe.contains("comment") ) {
                toGoServer = "https://" + appLinkData.getHost() + pathMaybe;
            }
            else {
                String notAtUser = pathMaybe.substring(pathMaybe.indexOf("user") + 5); //This line gets the username.
                String atUser = notAtUser + "@" + appLinkData.getHost(); //This appends @[HOST] to the string, so we have the full username thing needed.
                toGoServer = "https://" + server + "/user/" + atUser;
            }
        } else {
            startActivity(new Intent(HandlerActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
        }
        //Then all the decryption stuff has to happen. There are a lot of try-catch stuff, because apparently that seems to be needed.
        //First get the keystore thing.
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        //Then, load it. or something. To make sure that it can be used.
        try {
            keyStore.load(null);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //Next, retrieve the key to be used for the decryption.
        Key DragonLikeKey = null;
        try {
            DragonLikeKey = keyStore.getKey("BookWyrm", null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        //Do something with getting the/a cipher or something.
        Cipher c = null;
        try {
            c = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        //And then initiating the cipher, so it can be used.
        try {
            c.init(Cipher.DECRYPT_MODE, DragonLikeKey, new GCMParameterSpec(128, codeMagic.getBytes()));
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        //Decrypt the password!
        byte[] truePass = null;
        try {
            truePass = c.doFinal(Base64.decode(pass, Base64.DEFAULT));
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        //Convert the decrypted password back to a string.
        String passw = new String(truePass, StandardCharsets.UTF_8);

        //A webviewclient thing is needed for some stuff. To automatically log in, the credentials are put in the form by the javascript that is loaded once the page is fully loaded. Then it is automatically submitted if the current page is the login page.
        String finalToGoServer = toGoServer;
        myWebView.setWebViewClient(new HandlerActivity.MyWebViewClient() {
            public void onPageFinished(WebView view, String url) {

                view.loadUrl("javascript:(function() { document.getElementById('id_password').value = '" + passw + "'; ;})()");
                view.loadUrl("javascript:(function() { document.getElementById('id_localname').value = '" + name + "'; ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == '" + finalToGoServer + "' && !/(review|generatednote|quotation|comment)/i.test(window.location.href)) { document.getElementsByName(\"login\")[0].submit();} ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "') { document.getElementsByName(\"login\")[0].submit();} ;})()");
                view.loadUrl("javascript:(function() { if (/(review|generatednote|quotation|comment)/i.test(window.location.href)) { document.getElementsByClassName(\"block\")[0].innerHTML = ` <a href=\"https://"+ server  +"\" class=\"button\" data-back=\"\">\n" +
                        "        <span class=\"icon icon-arrow-left\" aria-hidden=\"true\"></span>\n" +
                        "        <span><b>Back to homeserver</b></span>\n" +
                        "    </a>`;} ;})()");

            }
        });
        //Here, load the login page of the server. That actually does all that is needed.
        myWebView.loadUrl(toGoServer);
    }
    //Here is code to make sure that links of the bookwyrm server are handled withing the webview client, instead of having it open in the default browser.
    //Yes, I used the web for this too.
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // ATTENTION: This was auto-generated to handle app links.
            Intent appLinkIntent = getIntent();
            String appLinkAction = appLinkIntent.getAction();
            Uri appLinkData = appLinkIntent.getData();
            // End of auto-generated stuff
            String strangeHost = appLinkData.getHost();
            SharedPreferences sharedPref = HandlerActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
            String defaultValue = "none";
            String server = sharedPref.getString(getString(R.string.server), defaultValue);
            if (server.equals(request.getUrl().getHost())) {
                //If the server is the same as the bookwyrm, load it in the webview.
                return false;
            } else if (strangeHost.equals(request.getUrl().getHost())) {
                return false;
            }
            // Otherwise, it should go to the default browser instead.
            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
            startActivity(intent);
            return true;
        }
    }
}