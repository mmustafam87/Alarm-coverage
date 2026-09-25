package com.ewis.coverage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * EWIS Coverage for Android tablets.
 * Hosts the coverage tool (assets/www/index.html) in a WebView and provides
 * Save design / Open design / PDF report / Copy through Android's file picker.
 */
public class MainActivity extends Activity {

    private static final int REQ_SAVE = 1;
    private static final int REQ_OPEN = 2;

    private WebView web;
    private byte[] pendingBytes;
    private String pendingMime;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if (scheme.equals("http") || scheme.equals("https") || scheme.equals("mailto")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, u));
                    } catch (ActivityNotFoundException e) {
                        toast("No app can open that link.");
                    }
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/www/index.html");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Close EWIS Coverage?")
                .setMessage("Anything you haven't saved with Save design will be lost.")
                .setPositiveButton("Close", (d, w) -> finish())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ---- File saving through Android's "Save to" picker ----
    private void startSave(byte[] bytes, String mime, String name) {
        pendingBytes = bytes;
        pendingMime = mime;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, name);
        try {
            startActivityForResult(i, REQ_SAVE);
        } catch (ActivityNotFoundException e) {
            toast("This device has no file picker for saving.");
        }
    }

    private void startOpen() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
        try {
            startActivityForResult(i, REQ_OPEN);
        } catch (ActivityNotFoundException e) {
            toast("This device has no file picker for opening.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_SAVE && pendingBytes != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                out.write(pendingBytes);
                toast("Saved.");
                if ("application/pdf".equals(pendingMime)) {
                    Intent view = new Intent(Intent.ACTION_VIEW);
                    view.setDataAndType(uri, "application/pdf");
                    view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(view);
                    } catch (ActivityNotFoundException ignored) {
                        // No PDF viewer installed: the file is still saved.
                    }
                }
            } catch (Exception e) {
                toast("Couldn't save the file: " + e.getMessage());
            } finally {
                pendingBytes = null;
            }
        } else if (requestCode == REQ_OPEN) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[16384];
                int n;
                while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
                String text = new String(buf.toByteArray(), StandardCharsets.UTF_8);
                String js = "(function(){try{window.loadDesign&&window.loadDesign(JSON.parse(" + JSONObject.quote(text) + "));}"
                        + "catch(e){var m=document.getElementById('designMsg');if(m)m.textContent=\"That file isn't an EWIS coverage design.\";}})()";
                web.evaluateJavascript(js, null);
            } catch (Exception e) {
                toast("Couldn't open the file: " + e.getMessage());
            }
        }
    }

    /** Receives window.webkit.messageHandlers.native.postMessage(...) calls from the page. */
    private class Bridge {
        @JavascriptInterface
        public void postMessage(String json) {
            runOnUiThread(() -> handle(json));
        }
    }

    private void handle(String json) {
        try {
            JSONObject m = new JSONObject(json);
            String type = m.optString("type");
            switch (type) {
                case "copy": {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("EWIS speaker schedule", m.optString("text")));
                    toast("Copied.");
                    break;
                }
                case "save":
                    startSave(m.optString("json", "{}").getBytes(StandardCharsets.UTF_8),
                            "application/json", "EWIS coverage design.json");
                    break;
                case "savePdf":
                    startSave(Base64.decode(m.optString("base64"), Base64.DEFAULT),
                            "application/pdf", m.optString("filename", "EWIS coverage report.pdf"));
                    break;
                case "open":
                    startOpen();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            toast("Something went wrong: " + e.getMessage());
        }
    }
}
