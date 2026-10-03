package ir.mza.tash;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class TashMessageService extends FirebaseMessagingService {
    public static final String CHANNEL_ID = "tash_messages";

    @Override
    public void onNewToken(String token) {
        getSharedPreferences("tash", MODE_PRIVATE).edit().putString("push_token", token).apply();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = "Tash";
        String body = "پیام جدید";
        String url = "https://tashweb.ir/";
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null) body = message.getNotification().getBody();
        }
        if (message.getData().get("title") != null) title = message.getData().get("title");
        if (message.getData().get("body") != null) body = message.getData().get("body");
        if (message.getData().get("url") != null) url = message.getData().get("url");
        show(this, title, body, url);
    }

    public static void show(Context context, String title, String body, String url) {
        ensureChannel(context);
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("open_url", url);
        PendingIntent pending = PendingIntent.getActivity(
                context, (int) (System.currentTimeMillis() & 0xffff), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_tash)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        NotificationManagerCompat.from(context).notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "پیام‌های تش", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("صدای پیام جدید حتی وقتی برنامه بسته است");
        channel.enableVibration(true);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        channel.setSound(sound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        manager.createNotificationChannel(channel);
    }
}
