package com.yearlyt.freepdf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 41;
    private WebView webView;
    private volatile File currentFile;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://app.local/document.pdf") && currentFile != null && currentFile.exists()) {
                    try { return new WebResourceResponse("application/pdf", null, new FileInputStream(currentFile)); }
                    catch (Exception ignored) {}
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AppBridge(), "Android");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/viewer.html");
        Uri incoming = getIntent() != null ? getIntent().getData() : null;
        if (incoming != null) webView.postDelayed(() -> openPdf(incoming), 700);
    }

    public class AppBridge {
        @JavascriptInterface public void choosePdf() { runOnUiThread(() -> pickPdf()); }
    }

    private void pickPdf() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        startActivityForResult(i, PICK_PDF);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_PDF && result == RESULT_OK && data != null && data.getData() != null) openPdf(data.getData());
    }

    private void openPdf(Uri uri) {
        try {
            File out = new File(getCacheDir(), "current.pdf");
            try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream fos = new FileOutputStream(out)) {
                if (in == null) throw new Exception("Cannot read PDF");
                byte[] buffer = new byte[65536]; int n;
                while ((n = in.read(buffer)) > 0) fos.write(buffer, 0, n);
            }
            currentFile = out;
            String name = fileName(uri).replace("\\", "\\\\").replace("'", "\\'");
            String servedUrl = "https://app.local/document.pdf?v=" + System.currentTimeMillis();
            webView.evaluateJavascript("loadPdf('" + servedUrl + "','" + name + "')", null);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open this PDF", Toast.LENGTH_LONG).show();
        }
    }

    private String fileName(Uri uri) {
        String name = "PDF document"; Cursor c = null;
        try { c = getContentResolver().query(uri, null, null, null, null); if (c != null && c.moveToFirst()) { int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) name = c.getString(i); } }
        catch (Exception ignored) {} finally { if (c != null) c.close(); }
        return name;
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() { webView.destroy(); super.onDestroy(); }
}
