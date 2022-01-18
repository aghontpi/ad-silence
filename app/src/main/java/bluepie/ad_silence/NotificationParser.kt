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
        context.getString(R.string.spotify_package_name) -> SupportedApps.SPOTIFY
        context.getString(R.string.tidal_package_name) -> SupportedApps.TIDAL
        context.getString(R.string.spotify_lite_package_name) -> SupportedApps.SPOTIFY_LITE
        context.getString(R.string.pandora_package_name) -> SupportedApps.PANDORA
        context.getString(R.string.spotify_stations_package_name) -> SupportedApps.SPOTIFY_STATIONS
        else -> SupportedApps.INVALID
    }
}

fun AppNotification.adString(): List<String> {
    return when (getApp()) {
        SupportedApps.ACCURADIO -> listOf(context.getString(R.string.accuradio_ad_text))
        SupportedApps.SPOTIFY, SupportedApps.SPOTIFY_LITE -> listOf(
            context.getString(R.string.spotify_ad_string),
            context.getString(R.string.spotify_ad2)
        )
        SupportedApps.TIDAL -> listOf(context.getString(R.string.tidal_ad_string))
        SupportedApps.PANDORA -> listOf(
            context.getString(R.string.pandora_ad_string),
            context.getString(R.string.pandora_ad_string_2)
        )
        SupportedApps.SPOTIFY_STATIONS -> listOf(context.getString(R.string.spotify_stations_ad_string))
        else -> listOf("")
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
            SupportedApps.SPOTIFY, SupportedApps.SPOTIFY_LITE -> parseSpotifyNotification()
            SupportedApps.TIDAL -> parseTidalNotification()
            SupportedApps.PANDORA -> parsePandoraNotification()
            SupportedApps.SPOTIFY_STATIONS -> parseSpotifyStationsNotification()
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
        var isAd = false
        for (adString in appNotification.adString()) {
            if (info.any { it.contains(adString) }) {
                Log.v(TAG, "detection in Accuradio: $adString")
                isAd = true
                break
            }
        }
        return isAd
    }

    private fun parseSpotifyNotification(): Boolean =
        detectInNotificationExtras("detection in Spotify")

    private fun parseTidalNotification(): Boolean = detectInNotificationExtras("detection in Tidal")

    private fun parsePandoraNotification(): Boolean =
        detectInNotificationExtras("detection in Pandora")

    private fun parseSpotifyStationsNotification(): Boolean =
        detectInNotificationExtras("detection in 'Spotify-Stations'", "text");

    private fun detectInNotificationExtras(logMessage: String, index: String = "title"): Boolean {
        var isAd = false
        this.appNotification.notification.extras?.get("android.$index").toString().run {
            for (adString in appNotification.adString()) {
                if (this == adString) {
                    Log.v(TAG, "$logMessage: $adString")
                    isAd = true
                    break
                }
            }
        }
        return isAd
    }
}