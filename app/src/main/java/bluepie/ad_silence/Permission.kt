package bluepie.ad_silence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat


private val TAG = "Permission"

fun checkNotificationListenerPermission(context: Context): Boolean {
    val permissionList = Settings.Secure.getString(context.contentResolver,
        context.getString(R.string.android_constant_enabled_notification_listeners)
    )
    Log.v(TAG, "nl already present: " + permissionList);
    return (permissionList!= null && permissionList.contains(context.packageName))
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun checkNotificationPostingPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}