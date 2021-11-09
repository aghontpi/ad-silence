package bluepie.ad_silence

import android.app.Notification
import android.content.Context

data class AppNotification(val context: Context, val notification: Notification, val appName: SupportedApps )

fun AppNotification.adString(): String {
    return when (appName) {
        SupportedApps.ACCURADIO -> context.getString(R.string.accuradio_ad_text)
    }
}




