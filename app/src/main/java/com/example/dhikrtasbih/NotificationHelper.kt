package com.example.dhikrtasbih

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_MORNING = "morning_adhkar_channel"
    const val CHANNEL_EVENING = "evening_adhkar_channel"
    const val NOTIF_ID_MORNING = 1001
    const val NOTIF_ID_EVENING = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MORNING,
                    "تذكير أذكار الصباح",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تذكير يومي بأذكار الصباح بعد الفجر"
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_EVENING,
                    "تذكير أذكار المساء",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تذكير يومي بأذكار المساء بعد العصر"
                }
            )
        }
    }

    fun sendMorningNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING)
            .setSmallIcon(R.drawable.ic_dua)
            .setContentTitle("وقت أذكار الصباح 🌅")
            .setContentText("ابدأ يومك بذكر الله — اضغط للفتح")
            .setStyle(NotificationCompat.BigTextStyle().bigText("سبحان الله وبحمده، سبحان الله العظيم\nابدأ يومك بذكر الله وتحصَّن بأذكار الصباح."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_MORNING, notification)
    }

    fun sendEveningNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENING)
            .setSmallIcon(R.drawable.ic_dua)
            .setContentTitle("وقت أذكار المساء 🌙")
            .setContentText("أتممتَ أذكارك اليوم؟ لا تضع هذا الكنز")
            .setStyle(NotificationCompat.BigTextStyle().bigText("أذكار المساء درعٌ يحميك من الشيطان والعين.\nاستغرق الأمر دقيقتين — فلا تُفرِّط."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_EVENING, notification)
    }
}
