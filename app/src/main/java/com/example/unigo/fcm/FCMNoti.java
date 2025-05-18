package com.example.unigo.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.unigo.Home;
import com.example.unigo.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMNoti extends FirebaseMessagingService{
    private static final String TAG = "ServicioFirebase";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        if (message.getNotification() != null) {
            showNotification(
                    message.getNotification().getTitle(),
                    message.getNotification().getBody(),
                    message.getData()
            );
        }
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "default_channel_id";

        // Crear canal (solo necesario para API 26+)
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "External Notificación",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificación externa a la aplicación para comunicar cualquier inidencia de la aplicación.");
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.icon_app) // Icono IMPORTANTE
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(this, R.color.green_light)); // Color del icono

        // Manejar click en la notificación
        Intent intent = new Intent(this, Home.class); // Cambia a tu actividad principal
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder.setContentIntent(pendingIntent);

        notificationManager.notify(0, notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        super.onNewToken(token);
        // Aquí puedes enviar el token a tu servidor si es necesario
    }
}
