package com.on1Aug24.webviewapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.on1Aug24.webviewapp.constants.AppConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomWebChromeClient extends WebChromeClient {

    private MainActivity mainActivity;
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    protected FrameLayout mFullscreenContainer;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private FrameLayout mContentView;


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
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if (acceptTypes != null) {
                List<String> validTypes = new ArrayList<>();
                for (String type : acceptTypes) {
                    if (type != null && !type.trim().isEmpty()) {
                        validTypes.add(type);
                    }
                }
                if (!validTypes.isEmpty()) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, validTypes.toArray(new String[0]));
                } else {
                    intent.setType("*/*");
                }
            }


            boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);

            Intent chooserIntent = Intent.createChooser(intent, "Choose File");
            ((Activity) webView.getContext()).startActivityForResult(chooserIntent, AppConfig.FILE_CHOOSER_REQUEST_CODE);

            mainActivity.setUploadMessage(filePathCallback);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(webView.getContext(), "No file picker installed!", Toast.LENGTH_LONG).show();
            return false;
        }
    }

}
