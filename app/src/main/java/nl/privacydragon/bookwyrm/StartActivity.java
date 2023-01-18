package nl.privacydragon.bookwyrm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.support.v7.app.AppCompatActivity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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
    WebView myWebView;
    ProgressBar LoadIndicator;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        LoadIndicator = (ProgressBar) findViewById(R.id.progressBar3);
        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setVisibility(View.GONE);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new Object()
        {
            @JavascriptInterface           // For API 17+
            public void performClick(String what)
            {
                if (!what.contains("[object Window]")) { //For some reason the function has to be called when the event listener is attached to the button. So, by adding in 'this', it is possible to make sure to only act when the thing that called the function is NOT the window, but the button.
                    ScanBarCode();
                }

            }
        }, "scan");
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
                LoadIndicator.setVisibility(View.GONE);
                myWebView.setVisibility(View.VISIBLE);

                view.loadUrl("javascript:(function() { document.getElementById('id_password_confirm').value = '" + passw + "'; ;})()");
                view.loadUrl("javascript:(function() { document.getElementById('id_localname_confirm').value = '" + name + "'; ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "/login') { document.getElementsByName(\"login-confirm\")[0].submit();} ;})()");
                view.loadUrl("javascript:(function() { " +
                        "if (document.querySelectorAll(\"[data-modal-open]\")[0]) {" +
                            "let ISBN_Button = document.querySelectorAll(\"[data-modal-open]\")[0];" +
                            "ISBN_Button.replaceWith(ISBN_Button.cloneNode(true));" +
                            "document.querySelectorAll(\"[data-modal-open]\")[0].addEventListener('click', () => {" +
                                "scan.performClick(this);" +
                            "});" +
                        "} else {" +
                            "let ISBN = document.createElement(\"div\");" +
                            "ISBN.class = 'control';" +
                            //"ISBN.class = 'button';" +
                            //"ISBN.type = 'button';" +
                            "ISBN.innerHTML = '<button class=\"button\" type=\"button\" onclick=\"scan.performClick(this)\"><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"24\" height=\"24\" aria-hidden=\"true\"><path fill=\"none\" d=\"M0 0h24v24H0z\"/><path d=\"M4 5v14h16V5H4zM3 3h18a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm3 4h3v10H6V7zm4 0h2v10h-2V7zm3 0h1v10h-1V7zm2 0h3v10h-3V7z\"/></svg><span class=\"is-sr-only\">Search</span></button>';" +
                            "nav = document.getElementsByClassName(\"field has-addons\")[0];" +
                            "nav.appendChild(ISBN);" +
                            "}" +
                        ";})()"); //This lines replace the ISBN-scan button event listener with one that points to the on-device scanning implementation, if it is available on the instance. If not, the button is added.

            }
        });
        //Here, load the login page of the server. That actually does all that is needed.
        myWebView.loadUrl("https://" + server + "/login");
    }
    public void ScanBarCode() {
        String permission = Manifest.permission.CAMERA;
        int grant = ContextCompat.checkSelfPermission(StartActivity.this, permission);
        if (grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;
            ActivityCompat.requestPermissions(StartActivity.this, permission_list, 1);
        }

        IntentIntegrator intentIntegrator = new IntentIntegrator(StartActivity.this);
        intentIntegrator.setDesiredBarcodeFormats(intentIntegrator.EAN_13);
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.setCameraId(0);
        intentIntegrator.setPrompt("SCAN ISBN");
        intentIntegrator.setBarcodeImageEnabled(false);
        intentIntegrator.initiateScan();

        //return "blup";
        //return "bla";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult Result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (Result != null) {
            if (Result.getContents() == null) {
                Toast.makeText(this, "cancelled", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("MainActivity", "Scanned");
                myWebView.loadUrl("Javascript:(function() {document.getElementById('tour-search').value = " + Result.getContents() + ";" + "document.getElementById('search_input').value = " + Result.getContents() + ";" +
                        "document.getElementsByTagName('form')[0].submit(); ;})()");
                LoadIndicator.setVisibility(View.VISIBLE);

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
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

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            LoadIndicator.setVisibility(View.VISIBLE);
        }
    }
}