package bluepie.ad_silence

import android.app.Notification
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import bluepie.ad_silence.triggers.spotifyTrigger
import java.util.*

data class AppNotification(
    val context: Context,
    val notification: Notification,
    val packageName: String,
)

fun AppNotification.getPreloadedAppType(): SupportedApps {
    return when (packageName) {
        context.getString(R.string.accuradio_pkg_name) -> SupportedApps.ACCURADIO
        context.getString(R.string.spotify_package_name) -> SupportedApps.SPOTIFY
        context.getString(R.string.tidal_package_name) -> SupportedApps.TIDAL
        context.getString(R.string.spotify_lite_package_name) -> SupportedApps.SPOTIFY_LITE
        context.getString(R.string.pandora_package_name) -> SupportedApps.PANDORA
        context.getString(R.string.liveOne_package_name) -> SupportedApps.LiveOne
        context.getString(R.string.soundcloud_package_name) -> SupportedApps.Soundcloud
        else -> SupportedApps.INVALID
    }
}


fun AppNotification.getApp(): SupportedApps {
    val preference = Preference(context)
    val customApps = preference.getCustomApps()
    if (customApps.any { it.packageName == packageName && it.isEnabled }) {
        return SupportedApps.CUSTOM
    }

    return getPreloadedAppType()
}

fun AppNotification.adString(): List<String> {
    if (getApp() == SupportedApps.CUSTOM) {
        val preference = Preference(context)
        return preference.getCustomApps().find { it.packageName == packageName }?.keywords ?: emptyList()
    }

    return when (getApp()) {
        SupportedApps.ACCURADIO -> listOf(context.getString(R.string.accuradio_ad_text))
        SupportedApps.SPOTIFY, SupportedApps.SPOTIFY_LITE -> listOf(
            context.getString(R.string.spotify_ad_string),
            context.getString(R.string.spotify_ad2),
            *spotifyTrigger
        )

        SupportedApps.TIDAL -> listOf(context.getString(R.string.tidal_ad_string))
        SupportedApps.PANDORA -> listOf(
            context.getString(R.string.pandora_ad_string),
            context.getString(R.string.pandora_ad_string_2)
        )

        SupportedApps.LiveOne -> listOf(
            context.getString(R.string.liveOne_ad_string),
            context.getString(R.string.liveOne_ad_string_2)
        )

        SupportedApps.Soundcloud -> listOf(context.getString(R.string.soundcloud_ad_string))

        else -> listOf("")
    }
}

interface NotificationParserInterface {
    var appNotification: AppNotification
    fun isAd(): Boolean
}

class NotificationParser(override var appNotification: AppNotification) :
    NotificationParserInterface {
    private val TAG = "NotificationParser"

    var lastLogEntry: LogEntry? = null

    private var matchedText: String = "";

    private fun checkPreloadedAppAd(appType: SupportedApps): Boolean {
        return when (appType) {
            SupportedApps.ACCURADIO -> parseAccuradioNotification()
            SupportedApps.SPOTIFY, SupportedApps.SPOTIFY_LITE -> parseSpotifyNotification()
            SupportedApps.TIDAL -> parseTidalNotification()
            SupportedApps.PANDORA -> parsePandoraNotification()
            SupportedApps.LiveOne -> parseLiveOneNotification()
            SupportedApps.Soundcloud -> parseSoundCloudNotification()
            else -> false
        }
    }

    override fun isAd(): Boolean {
        val appType = appNotification.getApp()
        var isAd = if (appType == SupportedApps.CUSTOM) {
            val customResult = parseCustomAppNotification()
            if (!customResult) {
                // if user keywords are not matched, check preloaded app keywords
                val preloadedAppType = appNotification.getPreloadedAppType()
                val preference = Preference(appNotification.context)
                if (preference.isAppConfigured(preloadedAppType)) {
                    checkPreloadedAppAd(preloadedAppType)
                } else {
                    false
                }
            } else {
                true
            }
        } else {
            checkPreloadedAppAd(appType)
        }
        
        val title = appNotification.notification.extras?.get("android.title")?.toString() ?: ""
        val text = appNotification.notification.extras?.get("android.text")?.toString() ?: ""
        val subText = appNotification.notification.extras?.get("android.subText")?.toString() ?: ""
        
        lastLogEntry = LogEntry(
            appName = appNotification.packageName,
            timestamp = System.currentTimeMillis(),
            isAd = isAd,
            title = title,
            text = text,
            subText = subText,
            matchedText = matchedText
        )
        
        return isAd
    }


    private fun extractTextFromRemoteViews(remoteViews: RemoteViews): String {
        return try {
            // Inflate the RemoteViews into a temporary container.
            val parent = FrameLayout(appNotification.context)
            val view = remoteViews.apply(appNotification.context, parent)
            // Traverse the inflated view tree to extract all text.
            extractTextFromView(view)
        } catch (e: Exception) {
            Log.e(TAG, "RemoteViews inflation failed", e)
            ""
        }
    }

    private fun extractTextFromView(view: View): String {
        val sb = StringBuilder()
        if (view is TextView) {
            sb.append(view.text)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                sb.append(" ").append(extractTextFromView(view.getChildAt(i)))
            }
        }
        return sb.toString().trim()
    }

    private fun extractTextViaReflection(remoteViews: RemoteViews): String {
        var fields: ArrayList<*>? = null
        remoteViews.javaClass.declaredFields.forEach { field ->
            if (field.name == "mActions") {
                field.isAccessible = true
                fields = field.get(remoteViews) as? ArrayList<*>
            }
        }
        val info = LinkedList<String>()
        fields?.forEach { action ->
            action?.javaClass?.declaredFields
                ?.firstOrNull { it.name == "value" }
                ?.apply { isAccessible = true }
                ?.let { field ->
                    when (val v = field.get(action)) {
                        is String -> info.push(v)
                    }
                }
        }
        return info.joinToString(" ")
    }


    private fun parseAccuradioNotification(): Boolean {
        Log.v(
            TAG,
            "detected ${appNotification.context.getString(R.string.accuradio)} -> " +
                    appNotification.context.getString(R.string.accuradio_pkg_name)
        )
        val notification = appNotification.notification
        try {
            var notificationText = ""
            // remote views
            val remoteViews = notification.contentView
                ?: notification.bigContentView
                ?: notification.headsUpContentView

            // Trying to inflate the RemoteViews
            if (remoteViews != null) {
                try {
                    notificationText = extractTextFromRemoteViews(remoteViews)
                } catch (e: Exception) {
                    Log.e(TAG, "RemoteViews inflation failed", e)
                    notificationText = ""
                }
            }

            // fallback to reflection.
            if (notificationText.isBlank() && remoteViews != null) {
                notificationText = extractTextViaReflection(remoteViews)
            }

            var notificationInfo: LinkedList<String> = LinkedList()

            notificationInfo.push(notificationText);

            listOf("android.title", "android.text", "android.subText").forEach { key ->
                appNotification.notification.extras?.get(key)?.toString()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { notificationInfo.push(it) }
            }

            var isAd = false
            for (adString in appNotification.adString()) {
                Log.v(TAG, "trying match against \"$notificationInfo\" with $adString")
                if (notificationInfo.any { it.contains(adString, ignoreCase = true) }) {
                    Log.v(TAG, "detection in Accuradio: $adString")
                    matchedText = adString;
                    isAd = true
                    break
                }
            }

            return isAd
        } catch (e: Exception) {
            Log.v(TAG, "Ad-silence exception in parsing accuradio", e)
            return false
        }
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
            Log.v(
                TAG,
                "trying match against \"$this\" with ${
                    appNotification.adString().take(10)
                }...refer app/src/main/java/bluepie/ad_silence/triggers/Spotify.kt in source (ie) github"
            )
            for (adString in appNotification.adString()) {
                if (this == adString) {
                    matchedText = adString;
                    Log.v(TAG, "detection in Spotify: $adString")
                    isAd = true
                    break
                }
            }
        }

        Log.v(
            TAG,
            "[spotify notification][new detection] " + this.appNotification.notification.extras.toString()
        )
        Log.v(
            TAG,
            "[spotify notification][new detection] ticker: ${this.appNotification.notification.tickerText}"
        )

        // miui & stock seems to have sub & txt reversed, https://github.com/aghontpi/ad-silence/pull/64
        val spotifyAdText = appNotification.context.getString(R.string.spotify_ad2);

        if (!isAd) {
            // new add detection method https://github.com/aghontpi/ad-silence/issues/63
            this.appNotification.notification.extras?.get("android.subText").toString().run {
                if (this.contains(spotifyAdText) || this == spotifyAdText) {
                    Log.v(TAG, "[new detection][spotify] detected subTxt: '${this}'")
                    matchedText = this;
                    isAd = true
                }
            }
        }

        if (!isAd) {
            // new add detection method https://github.com/aghontpi/ad-silence/issues/63
            this.appNotification.notification.extras?.get("android.text").toString().run {
                if (this.contains(spotifyAdText) || this == spotifyAdText) {
                    Log.v(TAG, "[spotify][new detection] detected txt: '${this}'")
                    matchedText = this;
                    isAd = true
                }
            }
        }


        if (!isAd) {
            //https://github.com/aghontpi/ad-silence/pull/64#issuecomment-1179105786
            //also refer miui & stock comment above
            listOf(
                this.appNotification.notification.extras?.get("android.subText"),
                this.appNotification.notification.extras?.get("android.title"),
                this.appNotification.notification.extras?.get("android.text")
            ).forEach { diffTypeString ->
                val songString = diffTypeString.toString();
                if (songString != "null" && songString is String && !isAd) {
                    val isMatchFound = appNotification.adString()
                        .filter { songString.contains(it, ignoreCase = true) }
                    if (isMatchFound.isNotEmpty() && !isAd) {
                        Log.v(
                            TAG,
                            "[spotify][new detection][regex match] found \"${
                                isMatchFound.joinToString(separator = ",")
                            }\" against \"$songString\""
                        )
                        matchedText = songString;
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

    private fun parseSoundCloudNotification(): Boolean {
        var isAd = false
        try {
            val text = this.appNotification.notification.extras?.get("android.text").toString()
            val title = this.appNotification.notification.extras?.get("android.title").toString()
            Log.v(
                TAG,
                "[trying] match against \"$text\", \"$title\" with ${appNotification.adString()}"
            )
            for (adString in appNotification.adString()) {
                if (title == adString || text == adString) {
                    Log.v(TAG, "[detection][soundcloud]: $adString")
                    isAd = true
                    break
                }
            }
        } catch (e: Exception) {
            Log.v(TAG, "[parseSoundcloud][ex] " + e.message)
        }
        return isAd
    }

    private fun parseCustomAppNotification(): Boolean {
        var isAd = false
        val adStrings = appNotification.adString()
        
        val title = appNotification.notification.extras?.get("android.title")?.toString() ?: ""
        val text = appNotification.notification.extras?.get("android.text")?.toString() ?: ""
        val subText = appNotification.notification.extras?.get("android.subText")?.toString() ?: ""

        Log.v(TAG, "trying match against \"$title\", \"$text\", \"$subText\" with $adStrings")
        
        for (adString in adStrings) {
             if (title.contains(adString, ignoreCase = true) || 
                 text.contains(adString, ignoreCase = true) || 
                 subText.contains(adString, ignoreCase = true)) {
                 Log.v(TAG, "detection in Custom App (${appNotification.packageName}): $adString")
                 matchedText = adString
                 isAd = true
                 break
             }
        }
        return isAd
    }
}