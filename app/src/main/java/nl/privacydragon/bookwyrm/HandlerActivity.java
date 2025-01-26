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
import android.os.Bundle;
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

public class HandlerActivity extends AppCompatActivity {

    WebView myWebView;
    ProgressBar LoadIndicator;
    public ValueCallback<Uri[]> omhooglader;
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
        ActivityResultLauncher<Intent> voodooLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        if (omhooglader == null)
                            return;
                        omhooglader.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), data));
                    } else {
                        omhooglader.onReceiveValue(null);
                    }
                });
        LoadIndicator = (ProgressBar) findViewById(R.id.progressBar3);
        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setVisibility(View.GONE);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setAllowFileAccess(true);
        myWebView.getSettings().setAllowContentAccess(true);
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
                                                 omhooglader = null;
                                             }
                                             omhooglader = filePathCallback;
                                             Intent intent = fileChooserParams.createIntent();
                                             try {
                                                 voodooLauncher.launch(intent);
                                             } catch (ActivityNotFoundException grrr){
                                                 omhooglader = null;
                                                 return false;
                                             }
                                             return true;
                                         }
                                     });
        //The name of the user's server is stored in the shared preferences, so first it has to be read from there.
        String defaultValue = "none";
        SharedPreferences sharedPref = HandlerActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
        String server = sharedPref.getString(getString(R.string.server), defaultValue);
        if (server.equals("none")) {
            startActivity(new Intent(HandlerActivity.this, nl.privacydragon.bookwyrm.MainActivity.class));
        }
        String pathMaybe = appLinkData.getPath();
        String toGoServer = "bla";
        //This bit of code regelt wanneer de webpagina wordt weergegeven. It is quite handig then om dan ook "book" toe te laten, zodat ook boeken in de app bekeken kunnen worden...
        if (pathMaybe.contains("user") || pathMaybe.contains("book")) {
            //If the path contains 'user', it is a user profile, unless it is followed by something like 'review'.
            if (pathMaybe.contains("review") || pathMaybe.contains("generatednote") || pathMaybe.contains("quotation") || pathMaybe.contains("comment") || pathMaybe.contains("book")) {
                toGoServer = "https://" + appLinkData.getHost() + pathMaybe;
            }
            else {
                String notAtUser = pathMaybe.substring(pathMaybe.indexOf("user") + 5); //This line gets the username.
                String atUser = notAtUser + "@" + appLinkData.getHost(); //This appends @[HOST] to the string, so we have the full username thing needed.
                toGoServer = "https://" + server + "/user/" + atUser;
            }
        } else {
            //If the toGoServer string remains "bla", dan zal de user when they teruggaan uitkomen op https://bla/, which is 'not allowed'.
            //So, maybe here, just to be sure, assign the value of the user's own server to the toGoServer variabele.
            //After that, since apparently I have decided that the URL the user tried to follow is not valid in my application, redirect them to StartActivity.
            toGoServer = "https://" + server;
            startActivity(new Intent(HandlerActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
        }

        //A webviewclient thing is needed for some stuff. To automatically log in, the credentials are put in the form by the javascript that is loaded once the page is fully loaded. Then it is automatically submitted if the current page is the login page.
        String finalToGoServer = toGoServer;
        myWebView.setWebViewClient(new HandlerActivity.MyWebViewClient() {
            public void onPageFinished(WebView view, String url) {
                LoadIndicator.setVisibility(View.GONE);
                myWebView.setVisibility(View.VISIBLE);
                view.loadUrl("javascript:(function() { if (/(review|generatednote|quotation|comment|book)/i.test(window.location.href)) {" +
                                "blocks = document.getElementsByClassName('block');" +
                                "for (let element of blocks){" +
                                        "if (element.localName == 'header') { " +
                                            "element.innerHTML = ` <a href=\"https://"+ server  +"\" class=\"button\" data-back=\"\">\n" +
                                                "<span class=\"icon icon-arrow-left\" aria-hidden=\"true\"></span>\n" +
                                                "<span><b>Back to homeserver</b></span>\n" +
                                            "</a>`;" +
                                            "break;" +
                                        "}" +
                                "}" +
                        "} ;})()");
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
                            "ISBN.innerHTML = '<button class=\"button\" type=\"button\" onclick=\"scan.performClick(this)\"><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"24\" height=\"24\" aria-hidden=\"true\"><path fill=\"none\" d=\"M0 0h24v24H0z\"/><path d=\"M4 5v14h16V5H4zM3 3h18a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm3 4h3v10H6V7zm4 0h2v10h-2V7zm3 0h1v10h-1V7zm2 0h3v10h-3V7z\"/></svg><span class=\"is-sr-only\">Search</span></button>';" +
                            "nav = document.getElementsByClassName(\"field has-addons\")[0];" +
                            "nav.appendChild(ISBN);" +
                        "}" +
                        ";})()");
            }
        });
        //Here, load the login page of the server. That actually does all that is needed.
        myWebView.loadUrl(toGoServer);
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLanceerder = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(HandlerActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(HandlerActivity.this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
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
        int grant = ContextCompat.checkSelfPermission(HandlerActivity.this, permission);
        if (grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;
            ActivityCompat.requestPermissions(HandlerActivity.this, permission_list, 1);
        }
        ScanOptions eisen = new ScanOptions();
        eisen.setDesiredBarcodeFormats(ScanOptions.EAN_13);
        eisen.setBeepEnabled(true);
        eisen.setCameraId(0);
        eisen.setPrompt("SCAN ISBN");
        eisen.setBarcodeImageEnabled(false);
        barcodeLanceerder.launch(eisen);
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