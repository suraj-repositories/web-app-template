package com.on1Aug24.webviewapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CustomWebViewClient extends WebViewClient {
    protected static boolean isErrorPageLoaded = false;
    private SwipeRefreshLayout swipeRefreshLayout;

    public CustomWebViewClient(SwipeRefreshLayout swipeRefreshLayout){
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        isErrorPageLoaded = false;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        view.loadUrl("file:///android_asset/error.html");
        isErrorPageLoaded = true;
        swipeRefreshLayout.setRefreshing(false); // Stop the spinner on error
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        if (request.isForMainFrame()) {
            view.loadUrl("file:///android_asset/error.html");
            isErrorPageLoaded = true;
            swipeRefreshLayout.setRefreshing(false); // Stop the spinner on error
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
            view.loadUrl("file:///android_asset/error.html");
            isErrorPageLoaded = true;
            swipeRefreshLayout.setRefreshing(false); // Stop the spinner on error
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        swipeRefreshLayout.setRefreshing(false); // Stop the spinner when the page is fully loaded
        MainActivity.currentUrl = url;
    }



}
