package bluepie.ad_silence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

val NOTIFICATION_CHANNEL_ID = "AD-SILENCE-CHANNEL"
val NOTIFICATION_CHANNEL_DESCRIPTION = "Ad Silence Notification channel"

// since gona use the same notification all purposes, use same id
val NOTIFICATION_ID = 69

data class AppNotification(val context: Context, val notification: Notification, val appName: SupportedApps)

fun AppNotification.adString(): String {
    return when (appName) {
        SupportedApps.ACCURADIO -> context.getString(R.string.accuradio_ad_text)
    }
}

class AppNotificationHelper(val context: Context)

fun AppNotificationHelper.updateNotification(status: String ){
    var notifiBuilder = createNotification(status)
    createChannel()
    with(NotificationManagerCompat.from(context)){
        notify(NOTIFICATION_ID,notifiBuilder.build())
    }
}

fun AppNotificationHelper.updateNotification(status: String, fromService: Boolean): NotificationCompat.Builder{
    var notifiBuilder = createNotification(status)
    createChannel()
    return notifiBuilder
}

private fun AppNotificationHelper.createNotification(status: String): NotificationCompat.Builder{
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
}

// android version > 26
private fun AppNotificationHelper.createChannel()  {
    var channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = NOTIFICATION_CHANNEL_DESCRIPTION }
    val notificationManager : NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.createNotificationChannel(channel)
}




