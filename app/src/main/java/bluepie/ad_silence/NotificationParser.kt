package bluepie.ad_silence

import android.app.Notification
import android.content.Context
import android.util.Log
import java.util.*

data class AppNotification(
    val context: Context,
    val notification: Notification,
    val packageName: String
)

fun AppNotification.getApp(): SupportedApps {
    return when (packageName) {
        context.getString(R.string.accuradio_pkg_name) -> SupportedApps.ACCURADIO
        else -> SupportedApps.INVALID
    }
}

fun AppNotification.adString(): String {
    return when (getApp()) {
        SupportedApps.ACCURADIO -> context.getString(R.string.accuradio_ad_text)
        else -> ""
    }
}

interface NotificationParserInterface {
    var appNotification: AppNotification
    fun isAd(): Boolean
    fun info(): LinkedList<String>
}

class NotificationParser(override var appNotification: AppNotification) :
    NotificationParserInterface {
    private val TAG = "NotificationParser"
    private var notificationInfo: LinkedList<String> = LinkedList()

    override fun isAd(): Boolean {
        return when (appNotification.getApp()) {
            SupportedApps.ACCURADIO -> parseAccuradioNotification()
            else -> false
        }
    }

    override fun info(): LinkedList<String> {
        return notificationInfo
    }

    private fun parseAccuradioNotification(): Boolean {
        Log.v(
            TAG,
            "detected ${appNotification.context.getString(R.string.accuradio)} -> ${
                appNotification.context.getString(R.string.accuradio_pkg_name)
            }"
        )
        val notification = appNotification.notification
        var fields: ArrayList<*>? = null
        for (field in notification.contentView.javaClass.declaredFields) {
            if (field.name == "mActions") {
                field.isAccessible = true
                fields = field.get(notification.contentView) as ArrayList<*>
            }

        }
        val info = LinkedList<String>()
        fields?.toArray()?.forEach { it ->
            if (it != null) {
                val fieldFilter = it.javaClass.declaredFields.filter { it.name == "value" }
                if (fieldFilter.size == 1) {
                    val field = fieldFilter.get(0)
                    field.isAccessible = true

                    // necessary fields
                    when (val v = field.get(it)) {
                        is String -> info.push(v)
                    }
                }
            }
        }

        notificationInfo = info

        return info.any { it.contains(appNotification.adString()) }
    }

}