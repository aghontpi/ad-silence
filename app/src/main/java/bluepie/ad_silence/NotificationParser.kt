package bluepie.ad_silence

import android.app.Notification
import android.content.Context
import android.util.Log
import bluepie.ad_silence.triggers.spotifyTrigger
import java.util.*

data class AppNotification(
    val context: Context,
    val notification: Notification,
    val packageName: String,
)

fun AppNotification.getApp(): SupportedApps {
    return when (packageName) {
        context.getString(R.string.accuradio_pkg_name) -> SupportedApps.ACCURADIO
        context.getString(R.string.spotify_package_name) -> SupportedApps.SPOTIFY
        context.getString(R.string.tidal_package_name) -> SupportedApps.TIDAL
        context.getString(R.string.spotify_lite_package_name) -> SupportedApps.SPOTIFY_LITE
        context.getString(R.string.pandora_package_name) -> SupportedApps.PANDORA
        context.getString(R.string.liveOne_package_name) -> SupportedApps.LiveOne
        else -> SupportedApps.INVALID
    }
}

fun AppNotification.adString(): List<String> {
    return when (getApp()) {
        SupportedApps.ACCURADIO -> listOf(context.getString(R.string.accuradio_ad_text))
        SupportedApps.SPOTIFY, SupportedApps.SPOTIFY_LITE -> listOf(
            context.getString(R.string.spotify_ad_string),
            context.getString(R.string.spotify_ad2),
            *spotifyTrigger
        )
        SupportedApps.TIDAL -> listOf(context.getString(R.string.tidal_ad_string))
        SupportedApps.PANDORA -> listOf(context.getString(R.string.pandora_ad_string),
            context.getString(R.string.pandora_ad_string_2))
        SupportedApps.LiveOne -> listOf(context.getString(R.string.liveOne_ad_string),
            context.getString(R.string.liveOne_ad_string_2))
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
            SupportedApps.LiveOne -> parseLiveOneNotification()
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

    private fun parseSpotifyNotification(): Boolean {
        Log.v(
            TAG,
            "detected ${appNotification.context.getString(R.string.spotify)} -> ${
                appNotification.getApp()
            }"
        )

        var isAd = false
        this.appNotification.notification.extras?.get("android.title").toString().run {
            Log.v(TAG, "trying match against \"$this\" with ${appNotification.adString().take(40)}")
            for (adString in appNotification.adString()) {
                if (this == adString) {
                    Log.v(TAG, "detection in Spotify: $adString")
                    isAd = true
                    break
                }
            }
        }

        Log.v(TAG, "[spotify notification][new detection] "+ this.appNotification.notification.extras.toString())
        Log.v(TAG, "[spotify notification][new detection] ticker: ${this.appNotification.notification.tickerText}" )

        // miui & stock seems to have sub & txt reversed, https://github.com/aghontpi/ad-silence/pull/64
        val spotifyAdText = appNotification.context.getString(R.string.spotify_ad2);

        if (!isAd) {
            // new add detection method https://github.com/aghontpi/ad-silence/issues/63
            this.appNotification.notification.extras?.get("android.subText") .toString().run {
                if (this.contains(spotifyAdText) || this == spotifyAdText) {
                    Log.v(TAG,"[new detection][spotify] detected subTxt: '${this}'")
                    isAd = true
                }
            }
        }

        if (!isAd) {
            // new add detection method https://github.com/aghontpi/ad-silence/issues/63
            this.appNotification.notification.extras?.get("android.text") .toString().run {
                if (this.contains(spotifyAdText) || this == spotifyAdText) {
                    Log.v(TAG,"[spotify][new detection] detected txt: '${this}'")
                    isAd = true
                }
            }
        }


        if (!isAd) {
            //https://github.com/aghontpi/ad-silence/pull/64#issuecomment-1179105786
            //also refer miui & stock comment above
            listOf(this.appNotification.notification.extras?.get("android.subText"),
                this.appNotification.notification.extras?.get("android.title"),
                this.appNotification.notification.extras?.get("android.text")).forEach { diffTypeString ->
                val songString = diffTypeString.toString();
                if (songString != "null" && songString is String && !isAd) {
                    val isMatchFound = appNotification.adString().filter { songString.contains(it, ignoreCase = true) }
                    if (isMatchFound.isNotEmpty() && !isAd) {
                        Log.v(TAG, "[spotify][new detection][regex match] found \"${isMatchFound.joinToString(separator=",")}\" against \"$songString\"")
                        isAd = true
                    }
                }
            }


        }

        return isAd
    }

    private fun parseTidalNotification(): Boolean {
        var isAd = false
        this.appNotification.notification.extras?.get("android.title").toString().run {
            Log.v(TAG, "trying match against \"$this\" with ${appNotification.adString()}")
            for (adString in appNotification.adString()) {
                if (this == adString) {
                    Log.v(TAG, "detection in Tidal: $adString")
                    isAd = true
                    break
                }
            }
        }
        return isAd
    }

    private fun parsePandoraNotification(): Boolean {
        var isAd = false
        this.appNotification.notification.extras?.get("android.title").toString().run {
            Log.v(TAG, "trying match against \"$this\" with ${appNotification.adString()}")
            for (adString in appNotification.adString()) {
                if (this == adString) {
                    Log.v(TAG, "detection in Pandora: $adString")
                    isAd = true
                    break
                }
            }
        }
        return isAd
    }

    private fun parseLiveOneNotification(): Boolean {
        var isAd = false
        val text = this.appNotification.notification.extras?.get("android.text").toString()
        val title = this.appNotification.notification.extras?.get("android.title").toString()
        Log.v(TAG, "trying match against \"$text\", \"$title\" with ${appNotification.adString()}")
        for (adString in appNotification.adString()) {
            if (text == "null" || title == adString || text == adString) {
                Log.v(TAG, "detection in LiveOne: $adString")
                isAd = true
                break
            }
        }
        return isAd
    }
}