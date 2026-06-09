package com.genzopia.addiction.Launcher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.genzopia.addiction.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper for creating the FCM notification channel and posting local notifications.
 * Requirements: 2.4, 2.5, 4.3
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String CHANNEL_ID = "fcm_default_channel";
    private static final String CHANNEL_NAME = "App Notifications";

    /**
     * Creates the notification channel required on Android 8.0+ (API 26+).
     * Safe to call multiple times — the OS ignores duplicate creation.
     * Requirement 2.4
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            } else {
                Log.w(TAG, "NotificationManager is null; channel not created.");
            }
        }
    }

    /**
     * Builds and posts a notification with an optional big picture image.
     * Downloads the image from imageUrl on the calling thread — call from a background thread.
     *
     * @param context        Application context
     * @param title          Notification title
     * @param body           Notification body
     * @param imageUrl       Optional HTTPS URL of image to show as BigPictureStyle (may be null)
     * @param silent         true = no sound/vibration, false = default sound
     * @param contentIntent  PendingIntent launched on tap (may be null)
     */
    public static void show(Context context, String title, String body,
                            String imageUrl, boolean silent, PendingIntent contentIntent) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            Log.w(TAG, "NotificationManager is null; notification not posted.");
            return;
        }

        // Use ic_launcher as small icon — shows the actual app icon in the status bar
        // and notification shade on Android 5+
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(silent
                        ? NotificationCompat.PRIORITY_LOW
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Sound / vibration control
        if (silent) {
            builder.setSilent(true);
        }

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }

        // Download and attach image if URL provided
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Bitmap bitmap = downloadBitmap(context, imageUrl);
            if (bitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon((Bitmap) null)); // hide large icon when expanded
                builder.setLargeIcon(bitmap);
            }
        }

        manager.notify(generateNotificationId(), builder.build());
    }

    /**
     * Convenience overload — no image, no silent flag (backwards compat).
     */
    public static void show(Context context, String title, String body, PendingIntent contentIntent) {
        show(context, title, body, null, false, contentIntent);
    }

    /**
     * Convenience overload — image + no silent flag.
     */
    public static void show(Context context, String title, String body,
                            String imageUrl, PendingIntent contentIntent) {
        show(context, title, body, imageUrl, false, contentIntent);
    }

    /** 
     * Downloads a bitmap from a URL, scaling it to fit the screen width while
     * preserving the original aspect ratio. Returns null on any error.
     */
    private static Bitmap downloadBitmap(Context context, String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            // First pass — decode bounds only (no pixel allocation)
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            InputStream boundsStream = connection.getInputStream();
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
            boundsStream.close();
            connection.disconnect();

            int srcWidth  = boundsOptions.outWidth;
            int srcHeight = boundsOptions.outHeight;

            // Target width = screen width; height auto-calculated to preserve aspect ratio
            android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int targetWidth = dm.widthPixels;

            // Calculate inSampleSize: largest power-of-2 that keeps width >= targetWidth
            int sampleSize = 1;
            if (srcWidth > targetWidth) {
                int halfWidth = srcWidth / 2;
                while (halfWidth / sampleSize >= targetWidth) {
                    sampleSize *= 2;
                }
            }

            // Second pass — decode actual pixels at reduced sample size
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize    = sampleSize;
            decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565; // lower memory

            HttpURLConnection connection2 = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection2.setDoInput(true);
            connection2.setConnectTimeout(5000);
            connection2.setReadTimeout(5000);
            connection2.connect();
            InputStream decodeStream = connection2.getInputStream();
            Bitmap sampled = BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
            decodeStream.close();
            connection2.disconnect();

            if (sampled == null) return null;

            // Scale precisely to targetWidth x auto-height preserving aspect ratio
            int scaledHeight = (int) ((float) sampled.getHeight() / sampled.getWidth() * targetWidth);
            return Bitmap.createScaledBitmap(sampled, targetWidth, scaledHeight, true);

        } catch (Exception e) {
            Log.w(TAG, "Failed to download notification image: " + e.getMessage());
            return null;
        }
    }

    /** Generates a unique notification ID based on current time. */
    private static int generateNotificationId() {
        return (int) (System.currentTimeMillis() & 0xfffffff);
    }

    /**
     * Applies fallback defaults when title or body are missing from the FCM data map.
     * Package-private to allow unit testing.
     * Requirement 2.6
     */
    public static String applyTitleDefault(String title) {
        return (title == null || title.trim().isEmpty()) ? "Notification" : title;
    }

    public static String applyBodyDefault(String body) {
        return (body == null || body.trim().isEmpty()) ? "You have a new message" : body;
    }
}
