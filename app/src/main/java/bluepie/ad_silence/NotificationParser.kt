package bluepie.ad_silence

import android.app.Notification
import android.content.res.Resources
import java.util.*

interface NotificationParserInterface {
    fun isAd(): Boolean
    fun info(): LinkedList<String>
}

class NotificationParser(var notification: Notification) : NotificationParserInterface {
    private var notificationInfo : LinkedList<String> = LinkedList()

    override fun isAd(): Boolean {
        var fields: ArrayList<*>? = null
        for (field in notification.contentView.javaClass.declaredFields) {
            if (field.name == "mActions") {
                field.isAccessible = true
                fields = field.get(notification.contentView) as ArrayList<*>
            }

        }
        val info = LinkedList<String>()
        fields?.toArray()?.forEach {
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

        return info.any {
            it.contains(
                    Resources.getSystem().getString(R.string.accuradio_ad_text)
            )
        }
    }

    override fun info(): LinkedList<String> {
        return notificationInfo
    }

}