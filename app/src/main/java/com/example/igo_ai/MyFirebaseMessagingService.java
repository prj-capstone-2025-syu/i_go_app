package com.example.igo_ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "igo_notifications";
    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM 메시지 수신: " + remoteMessage.getFrom());

        // 알림 표시
        if (remoteMessage.getNotification() != null) {
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "새로운 FCM 토큰: " + token);

        // 토큰을 서버로 전송
        sendTokenToServer(token);
    }

    private void sendNotification(String title, String body, java.util.Map<String, String> data) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 데이터가 있다면 Intent에 추가
        if (data != null) {
            for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info) // 기본 안드로이드 아이콘 사용
                        .setContentTitle(title != null ? title : "IGO 알림")
                        .setContentText(body != null ? body : "새로운 알림이 있습니다")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body)); // 긴 텍스트 지원

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "IGO 알림";
            String description = "IGO 앱의 일정, 루틴, 준비물 알림";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendTokenToServer(String token) {
        Log.d(TAG, "서버로 토큰 전송 시도: " + token);

        // MainActivity의 WebView를 통해 JavaScript로 토큰 전달
        // MainActivity가 활성화되어 있을 때만 실행
        try {
            // 브로드캐스트로 MainActivity에 토큰 업데이트 알림
            Intent intent = new Intent("com.igo.app.FCM_TOKEN_UPDATED");
            intent.putExtra("fcm_token", token);
            sendBroadcast(intent);

            Log.d(TAG, "FCM 토큰 브로드캐스트 전송 완료");
        } catch (Exception e) {
            Log.e(TAG, "토큰 전송 중 오류 발생", e);
        }
    }
}
