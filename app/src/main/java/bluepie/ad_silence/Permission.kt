package bluepie.ad_silence

import android.content.Context
import android.provider.Settings

val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

fun checkNotificationPermission(context: Context): Boolean {
    val packageName = context.packageName
    val permissionList = Settings.Secure.getString(context.contentResolver,
        ENABLED_NOTIFICATION_LISTENERS)
    return (permissionList!= null && permissionList.contains(packageName))
}