package org.intelehealth.ezazi.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.intelehealth.ezazi.R;


/**
 * Created by Vaghela Mithun R. on 02-08-2023 - 16:21.
 * Email : mithun@intelehealth.org
 * Mob   : +919727206702
 **/
public class AppNotification {
    private AppNotification() {

    }

    private PendingIntent pendingIntent;
    private String title;
    private String message;

    private int notificationId = 100100;

    /**
     * Renders the launcher icon into a bitmap for the notification's large icon.
     *
     * BitmapFactory.decodeResource cannot be used here. Since minSdk 26 the launcher icon resolves
     * to mipmap-anydpi-v26/ic_launcher.xml, an adaptive-icon, and decodeResource returns null for
     * anything that is not a bitmap stream -- so the large icon was silently absent. Drawing the
     * drawable onto a canvas works for both adaptive icons and plain bitmaps.
     *
     * Returns null if the icon cannot be loaded, which setLargeIcon accepts as "no large icon".
     */
    private static Bitmap largeIcon(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
        if (drawable == null) return null;
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            width = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
            height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        }
        if (width <= 0 || height <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    public void sendNotification(Context context) {
        String channelId = "CHANNEL_ID";

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon(context))
                //.setContentTitle("Firebase Push Notification")
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        /*NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);*/

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "Default Channel description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }


//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        notificationManager.notify(notificationId, notificationBuilder.build());
    }


    public static class Builder {
        private Context context;
        private AppNotification appNotification;

        public Builder(Context context) {
            this.context = context;
            appNotification = new AppNotification();
        }

        public Builder title(String title) {
            appNotification.title = title;
            return this;
        }

        public Builder body(String body) {
            appNotification.message = body;
            return this;
        }

        public Builder pendingIntent(PendingIntent pendingIntent) {
            appNotification.pendingIntent = pendingIntent;
            return this;
        }

        public void send() {
            appNotification.sendNotification(context);
        }
    }
}
