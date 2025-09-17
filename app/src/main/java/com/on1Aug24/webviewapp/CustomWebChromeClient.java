package com.on1Aug24.webviewapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.on1Aug24.webviewapp.constants.AppConfig;

public class CustomWebChromeClient extends WebChromeClient {

    private MainActivity mainActivity;
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    protected FrameLayout mFullscreenContainer;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private FrameLayout mContentView;

    private ValueCallback<Uri[]> uploadMessage;

    public CustomWebChromeClient(MainActivity mainActivity,
                                 ActivityResultLauncher<Intent> fileChooserLauncher,
                                 FrameLayout mContentView) {
        super();
        this.mainActivity = mainActivity;
        this.fileChooserLauncher = fileChooserLauncher;
        this.mContentView = mContentView;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }

        mOriginalOrientation = mainActivity.getRequestedOrientation();
        mOriginalSystemUiVisibility = mainActivity.getWindow().getDecorView().getSystemUiVisibility();

        mFullscreenContainer = new FrameLayout(mainActivity);
        mFullscreenContainer.setBackgroundColor(Color.BLACK);
        mFullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mContentView.addView(mFullscreenContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mCustomView = view;
        mCustomViewCallback = callback;

        mainActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onHideCustomView() {
        if (mCustomView == null) {
            return;
        }

        mainActivity.getWindow().getDecorView().setSystemUiVisibility(mOriginalSystemUiVisibility);
        mainActivity.setRequestedOrientation(mOriginalOrientation);

        mContentView.removeView(mFullscreenContainer);
        mFullscreenContainer = null;
        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();
        mCustomViewCallback = null;
    }

    @Override
    public boolean onShowFileChooser(WebView webView,
                                     ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        // Reset previous callback if not handled
        if (uploadMessage != null) {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }

        uploadMessage = filePathCallback;
        mainActivity.setUploadMessage(uploadMessage); // give MainActivity reference

        Intent intent = fileChooserParams.createIntent();
        intent.setType(AppConfig.UPLOAD_FILE_SUPPORT);

        try {
            fileChooserLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            uploadMessage = null;
            Toast.makeText(mainActivity, "Cannot open file chooser", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
