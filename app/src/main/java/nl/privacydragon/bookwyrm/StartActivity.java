package nl.privacydragon.bookwyrm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class StartActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        //The user credentials are stored in the shared preferences, so first they have to be read from there.
        String defaultValue = "none";
        SharedPreferences sharedPref = StartActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
        String server = sharedPref.getString(getString(R.string.server), defaultValue);
        SharedPreferences sharedPrefName = StartActivity.this.getSharedPreferences(getString(R.string.name), Context.MODE_PRIVATE);
        String name = sharedPrefName.getString(getString(R.string.name), defaultValue);
        SharedPreferences sharedPrefPass = StartActivity.this.getSharedPreferences(getString(R.string.pw), Context.MODE_PRIVATE);
        String pass = sharedPrefPass.getString(getString(R.string.pw), defaultValue);
        SharedPreferences sharedPrefMagic = StartActivity.this.getSharedPreferences(getString(R.string.q), Context.MODE_PRIVATE);
        String codeMagic = sharedPrefMagic.getString(getString(R.string.q), defaultValue);
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
        myWebView.setWebViewClient(new MyWebViewClient(){
            public void onPageFinished(WebView view, String url) {

                view.loadUrl("javascript:(function() { document.getElementById('id_password_confirm').value = '" + passw + "'; ;})()");
                view.loadUrl("javascript:(function() { document.getElementById('id_localname_confirm').value = '" + name + "'; ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "/login') { document.getElementsByName(\"login-confirm\")[0].submit();} ;})()");

            }
        });
        //Here, load the login page of the server. That actually does all that is needed.
        myWebView.loadUrl("https://" + server + "/login");
    }
    //Here is code to make sure that links of the bookwyrm server are handled withing the webview client, instead of having it open in the default browser.
    //Yes, I used the web for this too.
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            SharedPreferences sharedPref = StartActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
            String defaultValue = "none";
            String server = sharedPref.getString(getString(R.string.server), defaultValue);
            if (server.equals(request.getUrl().getHost())) {
                //If the server is the same as the bookwyrm, load it in the webview.
                return false;
            }
            // Otherwise, it should go to the default browser instead.
            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
            startActivity(intent);
            return true;
        }
    }
}