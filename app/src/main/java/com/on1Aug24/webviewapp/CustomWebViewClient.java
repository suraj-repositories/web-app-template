package com.on1Aug24.webviewapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.on1Aug24.webviewapp.constants.AppConfig;
import com.on1Aug24.webviewapp.enums.ProgressBarStyle;

public class CustomWebViewClient extends WebViewClient {
    protected static boolean isErrorPageLoaded = false;
    private final SwipeRefreshLayout swipeRefreshLayout;
    private final FrameLayout loadingScreen;

    public CustomWebViewClient(MainActivity mainActivity, SwipeRefreshLayout swipeRefreshLayout){
        this.swipeRefreshLayout = swipeRefreshLayout;
        loadingScreen = mainActivity.findViewById(R.id.loadingOverlay);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        isErrorPageLoaded = false;

        if(AppConfig.LOADER_STYLE == ProgressBarStyle.SPINNER){
            loadingScreen.setVisibility(View.VISIBLE);
        }

    }

//    @Override
//    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//        view.loadUrl("file:///android_asset/error.html");
//        isErrorPageLoaded = true;
//        swipeRefreshLayout.setRefreshing(false);
//        // loadingScreen.setVisibility(View.GONE);
//    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        if (request.isForMainFrame()) {
            view.loadUrl("file:///android_asset/error.html");
            isErrorPageLoaded = true;
            swipeRefreshLayout.setRefreshing(false);
            if(AppConfig.LOADER_STYLE == ProgressBarStyle.SPINNER) {
                loadingScreen.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
            view.loadUrl("file:///android_asset/error.html");
            isErrorPageLoaded = true;
            swipeRefreshLayout.setRefreshing(false);
            if(AppConfig.LOADER_STYLE == ProgressBarStyle.SPINNER) {
                loadingScreen.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        swipeRefreshLayout.setRefreshing(false);
        MainActivity.currentUrl = url;
        if(AppConfig.LOADER_STYLE == ProgressBarStyle.SPINNER) {
            loadingScreen.setVisibility(View.GONE);
        }
    }



}
