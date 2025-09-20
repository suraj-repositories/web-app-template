package com.on1Aug24.webviewapp;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.webkit.WebSettings;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.on1Aug24.webviewapp.constants.AppConfig;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ValueCallback<Uri[]> uploadMessage;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private FrameLayout mContentView;
    public static String currentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContentView = findViewById(R.id.activity_main);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        currentUrl = AppConfig.WEB_URL;

        // Pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (CustomWebViewClient.isErrorPageLoaded) {
                webView.loadUrl(currentUrl);
            } else {
                webView.reload();
            }
        });

        // Permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    for (String key : result.keySet()) {
                        if (Boolean.FALSE.equals(result.get(key))) {
                            Toast.makeText(this, "Permission denied: " + key, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    setupWebView();
                }
        );

        if (SDK_INT >= Build.VERSION_CODES.M) {
            Log.i("MainActivity", "Requesting permissions, SDK: " + SDK_INT);
            requestPermissions();
        } else {
            setupWebView();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        };

        requestPermissionLauncher.launch(permissions);
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setSupportZoom(true);
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setSaveFormData(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webView.setClickable(true);
        webView.setWebViewClient(new CustomWebViewClient(swipeRefreshLayout));
        webView.setWebChromeClient(new CustomWebChromeClient(this, fileChooserLauncher, mContentView));

        webView.setDownloadListener(new CustomDownloadListener(this, getApplicationContext()));
        webView.loadUrl(AppConfig.WEB_URL);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.this.finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void setUploadMessage(ValueCallback<Uri[]> callback) {
        this.uploadMessage = callback;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Handle landscape if needed
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Handle portrait if needed
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppConfig.FILE_CHOOSER_REQUEST_CODE) {
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    }
                }
            }

            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(results);
                uploadMessage = null;
            }
        }
    }

}
