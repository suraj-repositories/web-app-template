package com.on1Aug24.webviewapp;

public interface PermissionCallback {
    void onPermissionGranted(String permission);
    void onPermissionDenied(String permission, boolean neverAskAgain);
}
