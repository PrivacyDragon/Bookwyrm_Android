package nl.privacydragon.bookwyrm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

public class StartActivity extends AppCompatActivity {
    WebView myWebView;
    ProgressBar LoadIndicator;
    public ValueCallback<Uri[]> omhooglader;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        LoadIndicator = (ProgressBar) findViewById(R.id.progressBar3);
        ActivityResultLauncher<Intent> voodooLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        if (omhooglader == null)
                            return;
                        omhooglader.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), data));
                    }
                    else {
                        omhooglader.onReceiveValue(null);
                    }
                });
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
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (omhooglader != null) {
                    //omhooglader.onReceiveValue(null);
                    omhooglader = null;
                }
                omhooglader = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
//                    String toestemming = Manifest.permission.READ_EXTERNAL_STORAGE;
//                    int grant = ContextCompat.checkSelfPermission(StartActivity.this, toestemming);
//                    if (grant != PackageManager.PERMISSION_GRANTED) {
//                        String[] permission_list = new String[1];
//                        permission_list[0] = toestemming;
//                        ActivityCompat.requestPermissions(StartActivity.this, permission_list, 1);
//                    }
                    voodooLauncher.launch(intent);
                } catch (ActivityNotFoundException grrr){
                    omhooglader = null;
                    return false;
                }
                return true;
            }
        });
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
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //Next, retrieve the key to be used for the decryption.
        Key DragonLikeKey = null;
        try {
            DragonLikeKey = keyStore.getKey("BookWyrm", null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        //Do something with getting the/a cipher or something.
        Cipher c = null;
        try {
            c = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        //And then initiating the cipher, so it can be used.
        try {
            assert c != null;
            c.init(Cipher.DECRYPT_MODE, DragonLikeKey, new GCMParameterSpec(128, codeMagic.getBytes()));
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
        }
        //Decrypt the password!
        byte[] truePass = null;
        try {
            truePass = c.doFinal(Base64.decode(pass, Base64.DEFAULT));
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        //Convert the decrypted password back to a string.
        String passw = new String(truePass, StandardCharsets.UTF_8);
        //String wacht = passw.replaceAll("'", "\\\\'");

        //A webviewclient thing is needed for some stuff. To automatically log in, the credentials are put in the form by the javascript that is loaded once the page is fully loaded. Then it is automatically submitted if the current page is the login page.
        myWebView.setWebViewClient(new MyWebViewClient(){
            public void onPageFinished(WebView view, String url) {
                LoadIndicator.setVisibility(View.GONE);
                myWebView.setVisibility(View.VISIBLE);

                //view.loadUrl("javascript:(function() { document.getElementById('id_password_confirm').value = '" + wacht + "'; ;})()");
                //view.loadUrl("javascript:(function() { document.getElementById('id_localname_confirm').value = '" + name + "'; ;})()");
                //view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "/login') { document.getElementsByName(\"login-confirm\")[0].submit();} ;})()");
                //view.loadUrl("javascript:(function() { if (window.location.href == 'https://" + server + "/login' && document.title != '403 Forbidden') { this.document.location.href = 'source://' + encodeURI(document.documentElement.outerHTML);} ;})()");
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
//        try {
//            getMiddleWareToken(server, name, passw);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        String geheimeToken = null;
//        try {
//            geheimeToken = getMiddleWareToken(server);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        String gegevens = null;
//        try {
//            gegevens = "csrfmiddlewaretoken=" + URLEncoder.encode(geheimeToken, "UTF-8") + "&localname=" + URLEncoder.encode(name, "UTF-8") + "&password=" + URLEncoder.encode(passw, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
//        myWebView.postUrl("https://" + server + "/login", gegevens.getBytes());
          //myWebView.loadUrl("https://" + server + "/login");
//          myWebView.setVisibility(View.GONE);
//          LoadIndicator.setVisibility(View.VISIBLE);
//          android.webkit.CookieManager oven = android.webkit.CookieManager.getInstance();
          //myWebView.loadUrl("javascript:this.document.location.href = 'source://' + encodeURI(document.documentElement.outerHTML);");
        try {
            getMiddleWareTokenAndLogIn(server, name, passw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
//    public void logIn(String lichaam) {
//        //First, verkrijg the user credentials.
//        //The user credentials are stored in the shared preferences, so first they have to be read from there.
//        String defaultValue = "none";
//        SharedPreferences sharedPref = StartActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
//        String server = sharedPref.getString(getString(R.string.server), defaultValue);
//        SharedPreferences sharedPrefName = StartActivity.this.getSharedPreferences(getString(R.string.name), Context.MODE_PRIVATE);
//        String name = sharedPrefName.getString(getString(R.string.name), defaultValue);
//        SharedPreferences sharedPrefPass = StartActivity.this.getSharedPreferences(getString(R.string.pw), Context.MODE_PRIVATE);
//        String pass = sharedPrefPass.getString(getString(R.string.pw), defaultValue);
//        SharedPreferences sharedPrefMagic = StartActivity.this.getSharedPreferences(getString(R.string.q), Context.MODE_PRIVATE);
//        String codeMagic = sharedPrefMagic.getString(getString(R.string.q), defaultValue);
//        //Then all the decryption stuff has to happen. There are a lot of try-catch stuff, because apparently that seems to be needed.
//        //First get the keystore thing.
//        KeyStore keyStore = null;
//        try {
//            keyStore = KeyStore.getInstance("AndroidKeyStore");
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        }
//        //Then, load it. or something. To make sure that it can be used.
//        try {
//            keyStore.load(null);
//        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        //Next, retrieve the key to be used for the decryption.
//        Key DragonLikeKey = null;
//        try {
//            DragonLikeKey = keyStore.getKey("BookWyrm", null);
//        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
//            e.printStackTrace();
//        }
//        //Do something with getting the/a cipher or something.
//        Cipher c = null;
//        try {
//            c = Cipher.getInstance("AES/GCM/NoPadding");
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//            e.printStackTrace();
//        }
//        //And then initiating the cipher, so it can be used.
//        try {
//            assert c != null;
//            c.init(Cipher.DECRYPT_MODE, DragonLikeKey, new GCMParameterSpec(128, codeMagic.getBytes()));
//        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
//            e.printStackTrace();
//        }
//        //Decrypt the password!
//        byte[] truePass = null;
//        try {
//            truePass = c.doFinal(Base64.decode(pass, Base64.DEFAULT));
//        } catch (BadPaddingException | IllegalBlockSizeException e) {
//            e.printStackTrace();
//        }
//        //Convert the decrypted password back to a string.
//        String passw = new String(truePass, StandardCharsets.UTF_8);
//        Log.d("body", lichaam);
//        String[] opgebroken = lichaam.split("name=\"csrfmiddlewaretoken\" value=\"");
//        String[] breukjes = opgebroken[1].split("\">");
//        String middelToken = breukjes[0];
//        String[] splitsing = lichaam.split("var csrf_token = '");
//        String[] dilemma = splitsing[1].split("';");
//        String csrf = dilemma[0];
//        Log.d("tokens", "middel= " + middelToken);
//        Log.d("tokens", "csrf= " + csrf);
//        String gegevens = null;
//        try {
//            gegevens = "csrfmiddlewaretoken=" + URLEncoder.encode(middelToken, "UTF-8") + "&localname=" + URLEncoder.encode(name, "UTF-8") + "&password=" + URLEncoder.encode(passw, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
////        android.webkit.CookieManager oven = android.webkit.CookieManager.getInstance();
////        oven.setCookie("https://" + server, "csrftoken=" + csrf);
//        myWebView.postUrl("https://" + server + "/login", gegevens.getBytes());
//    }
    public void getMiddleWareTokenAndLogIn(String server, String name, String passw) throws IOException {
        //Het idee is dat deze functie de loginpagina van de server laadt en dan de 'csrfmiddlewaretoken' uit het inlogformulier haalt,
        //Zodat dat dan gebruikt kan worden bij het inloggen.
        Thread draadje = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://" + server + "/login");
                    CookieManager koekManager = new CookieManager();
                    CookieHandler.setDefault(koekManager);
                    CookieStore bakker = koekManager.getCookieStore();
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream ina = new BufferedInputStream(urlConnection.getInputStream());
                        byte[] pagina = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pagina = ina.readAllBytes();
                        } else {
                            ina.read(pagina, 0, ina.available());
                        }
                        try {
                            ina.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        String zooi = new String(pagina);
                        String[] opgebroken = zooi.split("name=\"csrfmiddlewaretoken\" value=\"");
                        String[] breukjes = opgebroken[1].split("\">");
                        String token = breukjes[0];
                        String gegevens = null;

                        String speculaas = "", THT = "";
                        List<HttpCookie> koektrommel = bakker.get(URI.create("https://" + server));
                        //Log.d("koek", koektrommel.toString());
                        for (int i = 0; i < koektrommel.size(); ++i) {
                            HttpCookie koekje = koektrommel.get(i);
                            if (Objects.equals(koekje.getName(), "csrftoken")) {
                                speculaas = koekje.toString();
                                THT = String.valueOf(koekje.getMaxAge());
                                //Log.d("domein", koekje.getDomain());
                            }
                        }
                        try {
                            gegevens = "csrfmiddlewaretoken=" + URLEncoder.encode(token, "UTF-8") + "&localname=" + URLEncoder.encode(name, "UTF-8") + "&password=" + URLEncoder.encode(passw, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        String finalGegevens = gegevens;
                        //Log.d("token", speculaas);
                        String finalSpeculaas = speculaas;
                        String finalTHT = THT;
                        runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {

                                              android.webkit.CookieManager oven = android.webkit.CookieManager.getInstance();
                                              oven.setCookie("https://" + server, finalSpeculaas + "; Max-Age=" + finalTHT + "; Path=/; SameSite=Lax; Secure");
                                              myWebView.postUrl("https://" + server + "/login?next=/", finalGegevens.getBytes());
                                          }
                                      });

                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        draadje.start();
        //return token;
    }
    private final ActivityResultLauncher<ScanOptions> barcodeLanceerder = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(StartActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(StartActivity.this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                    myWebView.loadUrl("Javascript:(function() {" +
                            "try {" +
                            "document.getElementById('tour-search').value = " + result.getContents() + ";" +
                            "} catch {" +
                            "document.getElementById('search_input').value = " + result.getContents() + ";" +
                            "}" +
                            "document.getElementsByTagName('form')[0].submit();" +
                            ";})()");
                    LoadIndicator.setVisibility(View.VISIBLE);
                }
            });

    public void ScanBarCode() {
        String permission = Manifest.permission.CAMERA;
        int grant = ContextCompat.checkSelfPermission(StartActivity.this, permission);
        if (grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;
            ActivityCompat.requestPermissions(StartActivity.this, permission_list, 1);
        }
        ScanOptions eisen = new ScanOptions();
        eisen.setDesiredBarcodeFormats(ScanOptions.EAN_13);
        eisen.setBeepEnabled(true);
        eisen.setCameraId(0);
        eisen.setPrompt("SCAN ISBN");
        eisen.setBarcodeImageEnabled(false);
        barcodeLanceerder.launch(eisen);
        //return "blup";
        //return "bla";
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyUp(keyCode, event);
    }
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
    //Here is code to make sure that links of the bookwyrm server are handled within the webview client, instead of having it open in the default browser.
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