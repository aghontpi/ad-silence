package bluepie.ad_silence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val NOTIFICATION_CHANNEL_ID = "AD-SILENCE-CHANNEL"
const val NOTIFICATION_CHANNEL_DESCRIPTION = "Ad Silence Notification channel"

// since gona use the same notification all purposes, use same id
const val NOTIFICATION_ID = 69


class AppNotificationHelper(val context: Context)

fun AppNotificationHelper.updateNotification(status: String ): Notification{
    val notifiBuilder = createNotification(status)
    createChannel()
    val notification = notifiBuilder.build()
    with(NotificationManagerCompat.from(context)){
        notify(NOTIFICATION_ID,notification)
    }
    return notification
}

fun AppNotificationHelper.getNotificationBuilder(status: String ): NotificationCompat.Builder {
    val notifiBuilder = createNotification(status)
    createChannel()
    return notifiBuilder
}

private fun AppNotificationHelper.createNotification(status: String): NotificationCompat.Builder {
    val adSilenceIntent = Intent(context, AdSilenceActivity::class.java)

    val pendingIntent = Build.VERSION.SDK_INT.let {
        when {
            it >= Build.VERSION_CODES.M -> PendingIntent.getActivity(
                context,
                0,
                adSilenceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            else -> PendingIntent.getActivity(
                context,
                0,
                adSilenceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
}

// android version > 26
private fun AppNotificationHelper.createChannel()  {
    val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = NOTIFICATION_CHANNEL_DESCRIPTION }
    val notificationManager : NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.createNotificationChannel(channel)
}


// manually stop and start the service
fun AppNotificationHelper.start(){
    val packageManager = context.packageManager
    val componentName = ComponentName(context, NotificationListener::class.java)
    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
}

fun AppNotificationHelper.disable(){
    val packageManager = context.packageManager
    val componentName = ComponentName(context, NotificationListener::class.java)
    if (Build.VERSION.SDK_INT >= 30){
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.SYNCHRONOUS)
    } else {
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,0)
    }
}

fun AppNotificationHelper.enable(){
    val packageManager = context.packageManager
    val componentName = ComponentName(context, NotificationListener::class.java)
    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
}




