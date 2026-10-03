package ir.mza.tash;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.webkit.CookieManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TashWatchService extends Service {
    public static final String CHECK_URL = "https://tashweb.ir/app-notify.php";
    private static final String WATCH_CHANNEL = "tash_watch";
    private static final String MESSAGE_CHANNEL = "tash_messages";
    private static final int WATCH_ID = 41;
    private volatile boolean running;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureChannels(this);
        startForeground(WATCH_ID, watchNotification());
        if (!running) {
            running = true;
            new Thread(this::loop, "tash-watch").start();
        }
        return START_STICKY;
    }

    private void loop() {
        while (running) {
            try {
                checkOnce();
            } catch (Exception ignored) {}
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void checkOnce() throws Exception {
        CookieManager cookies = CookieManager.getInstance();
        String cookie = cookies.getCookie("https://tashweb.ir/");
        HttpURLConnection conn = (HttpURLConnection) new URL(CHECK_URL).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        if (cookie != null) conn.setRequestProperty("Cookie", cookie);
        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) return;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);
        reader.close();
        JSONObject json = new JSONObject(body.toString());
        String id = json.optString("id", "");
        if (id.isEmpty() || "0".equals(id)) return;
        String last = getSharedPreferences("tash", MODE_PRIVATE).getString("last_notice", "");
        if (id.equals(last)) return;
        getSharedPreferences("tash", MODE_PRIVATE).edit().putString("last_notice", id).apply();
        showMessage(json.optString("title", "Tash"), json.optString("body", "پیام جدید"), json.optString("url", "https://tashweb.ir/"));
    }

    private void showMessage(String title, String body, String url) {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("open_url", url);
        PendingIntent pending = PendingIntent.getActivity(this, (int) (System.currentTimeMillis() & 0xffff), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, MESSAGE_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_tash)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) (System.currentTimeMillis() & 0x7fffffff), notification);
    }

    private Notification watchNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, WATCH_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_tash)
                .setContentTitle("تش")
                .setContentText("در حال دریافت پیام")
                .setOngoing(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(WATCH_CHANNEL) == null) {
            NotificationChannel watch = new NotificationChannel(WATCH_CHANNEL, "اتصال تش", NotificationManager.IMPORTANCE_MIN);
            watch.setSound(null, null);
            manager.createNotificationChannel(watch);
        }
        if (manager.getNotificationChannel(MESSAGE_CHANNEL) == null) {
            NotificationChannel messages = new NotificationChannel(MESSAGE_CHANNEL, "پیام‌های تش", NotificationManager.IMPORTANCE_HIGH);
            messages.enableVibration(true);
            messages.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            manager.createNotificationChannel(messages);
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
