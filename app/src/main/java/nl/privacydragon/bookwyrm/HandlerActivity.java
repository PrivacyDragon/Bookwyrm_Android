package nl.privacydragon.bookwyrm;

import static androidx.core.content.ContextCompat.startActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
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

public class HandlerActivity extends AppCompatActivity {

    WebView myWebView;
    ProgressBar LoadIndicator;
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
        LoadIndicator = (ProgressBar) findViewById(R.id.progressBar3);
        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
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
        //myWebView.addJavascriptInterface(new HandlerActivity.WebAppInterface(this), "Android");
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
                LoadIndicator.setVisibility(View.GONE);

                view.loadUrl("javascript:(function() { document.getElementById('id_password').value = '" + passw + "'; ;})()");
                view.loadUrl("javascript:(function() { document.getElementById('id_localname').value = '" + name + "'; ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == '" + finalToGoServer + "' && !/(review|generatednote|quotation|comment)/i.test(window.location.href)) { document.getElementsByName(\"login\")[0].submit();} ;})()");
                view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "') { document.getElementsByName(\"login\")[0].submit();} ;})()");
                view.loadUrl("javascript:(function() { if (/(review|generatednote|quotation|comment)/i.test(window.location.href)) { document.getElementsByClassName(\"block\")[0].innerHTML = ` <a href=\"https://"+ server  +"\" class=\"button\" data-back=\"\">\n" +
                        "        <span class=\"icon icon-arrow-left\" aria-hidden=\"true\"></span>\n" +
                        "        <span><b>Back to homeserver</b></span>\n" +
                        "    </a>`;} ;})()");
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
                        ";})()");
            }
        });
        /*myWebView.setWebChromeClient(new WebChromeClient(){
            // Need to accept permissions to use the camera
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String permission = Manifest.permission.CAMERA;
                int grant = ContextCompat.checkSelfPermission(HandlerActivity.this, permission);
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    String[] permission_list = new String[1];
                    permission_list[0] = permission;
                    ActivityCompat.requestPermissions(HandlerActivity.this, permission_list, 1);
                }
                request.grant(request.getResources());
                final String[] requestedResources = request.getResources();
                for (String r : requestedResources) {
                   if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                      request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                      break;
                   }
                }
            }
        });*/
        //Here, load the login page of the server. That actually does all that is needed.
        //myWebView.loadUrl("https://serratus.github.io/quaggaJS/examples/live_w_locator.html");
        myWebView.loadUrl(toGoServer);
    }

    public void ScanBarCode() {
        String permission = Manifest.permission.CAMERA;
        int grant = ContextCompat.checkSelfPermission(HandlerActivity.this, permission);
        if (grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;
            ActivityCompat.requestPermissions(HandlerActivity.this, permission_list, 1);
        }

        IntentIntegrator intentIntegrator = new IntentIntegrator(HandlerActivity.this);
        intentIntegrator.setDesiredBarcodeFormats(intentIntegrator.EAN_13);
        intentIntegrator.setBeepEnabled(true);
        intentIntegrator.setCameraId(0);
        intentIntegrator.setPrompt("SCAN");
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
                myWebView.loadUrl("Javascript:(function() {document.getElementById('search_input').value = " + Result.getContents() + ";" +
                        "document.getElementsByTagName('form')[0].submit(); ;})()");
                LoadIndicator.setVisibility(View.VISIBLE);

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*public class WebAppInterface {
        Context mContext;

        //Instantiate the interface and set the context
        WebAppInterface(Context c) {
            mContext = c;
        }

        // Show a toast from the web page
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }*/

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

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            LoadIndicator.setVisibility(View.VISIBLE);
        }

    }
}