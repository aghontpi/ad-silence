package bluepie.ad_silence

import android.content.Context
import android.provider.Settings


fun checkNotificationListenerPermission(context: Context): Boolean {
    val permissionList = Settings.Secure.getString(context.contentResolver,
        context.getString(R.string.android_constant_enabled_notification_listeners)
    )
    return (permissionList!= null && permissionList.contains(context.packageName))
}