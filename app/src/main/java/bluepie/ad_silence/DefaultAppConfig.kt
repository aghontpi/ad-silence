package bluepie.ad_silence

import android.content.Context
import bluepie.ad_silence.triggers.spotifyTrigger

object DefaultAppConfig {
    fun getDefaultKeywords(app: SupportedApps, context: Context): List<String> {
        return when (app) {
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
            SupportedApps.JIO_SAAVN -> listOf(context.getString(R.string.jio_saavn_ad_string))
            else -> emptyList()
        }
    }
}
