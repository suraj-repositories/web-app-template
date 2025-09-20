package com.on1Aug24.webviewapp;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import com.on1Aug24.webviewapp.constants.AppConfig;

import java.io.File;

public class CustomDownloadListener implements DownloadListener {

    private final MainActivity mainActivity;
    private final Context context;
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long POLLING_INTERVAL = 300;

    public CustomDownloadListener(MainActivity mainActivity, Context context) {
        this.mainActivity = mainActivity;
        this.context = context;
        createNotificationChannel();
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, "Invalid download URL", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimetype);

        String cookies = CookieManager.getInstance().getCookie(url);
        if (cookies != null) request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);

        request.setTitle(fileName);
        request.setDescription("Downloading...");
        request.allowScanningByMediaScanner();
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        if (AppConfig.USE_CUSTOM_NOTIFICATION) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        } else {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        DownloadManager downloadManager = (DownloadManager) mainActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            long downloadId = downloadManager.enqueue(request);
            Toast.makeText(context, "Downloading file: " + fileName, Toast.LENGTH_SHORT).show();

            if (AppConfig.USE_CUSTOM_NOTIFICATION) {
                trackDownloadProgress(downloadManager, downloadId, fileName);
            }
        } else {
            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
    private void trackDownloadProgress(DownloadManager downloadManager, long downloadId, String fileName) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Downloading")
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setColor(ContextCompat.getColor(context, R.color.white_day))
                .setOngoing(true)
                .setProgress(100, 0, false);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);

                try (Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        String uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));

                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            builder.setOngoing(false)
                                    .setProgress(0, 0, false)
                                    .setContentTitle(status == DownloadManager.STATUS_SUCCESSFUL ? "Download Complete" : "Download Failed")
                                    .setContentText(status == DownloadManager.STATUS_SUCCESSFUL ? "Click to Open" : "Download Failed")
                                    .setSmallIcon(status == DownloadManager.STATUS_SUCCESSFUL ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_notify_error);

                           
                            if (status == DownloadManager.STATUS_SUCCESSFUL && uriString != null) {

                                File file = new File(Uri.parse(uriString).getPath());
                                Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        context.getPackageName() + ".fileprovider",
                                        file
                                );

                                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                                openIntent.setDataAndType(fileUri, cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)));
                                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);


                                builder.setContentIntent(android.app.PendingIntent.getActivity(
                                        context,
                                        0,
                                        openIntent,
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                                ? android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                                                : android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                ));
                            }

                            if (notificationManager != null) notificationManager.notify(NOTIFICATION_ID, builder.build());
                        } else {
                            int progress = total == 0 ? 0 : (int) ((downloaded * 100L) / total);
                            builder.setProgress(100, progress, false);
                            if (notificationManager != null) notificationManager.notify(NOTIFICATION_ID, builder.build());
                            handler.postDelayed(this, POLLING_INTERVAL);
                        }
                    } else {
                        handler.postDelayed(this, POLLING_INTERVAL);
                    }
                }
            }
        };

        handler.post(runnable);
    }

}
