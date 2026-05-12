package bluepie.ad_silence

import android.content.Context
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.HashSet
import kotlin.text.split

class RootMuteManager(private val context: Context) {
    private val PROPERTY_PLAY = "PLAY_AUDIO"
    private val PROPERTY_FOCUS = "TAKE_AUDIO_FOCUS"

    private val MUTE_PLAY = "deny"
    private val MUTE_FOCUS = "ignore"
    private val UNMUTE_PLAY = "allow"
    private val UNMUTE_FOCUS = "allow"

    private val FIELD_PLAY_STATUS = "Default mode"
    private val FIELD_FOCUS_STATUS = "TAKE_AUDIO_FOCUS"

    private var mutedPackages = HashSet<String>()

    fun getPackageName(app: SupportedApps): List<String> {
        val preference = Preference(context)
        return when (app) {
            SupportedApps.ACCURADIO -> arrayListOf(context.getString(R.string.accuradio_pkg_name))
            SupportedApps.SPOTIFY -> arrayListOf(context.getString(R.string.spotify_package_name))
            SupportedApps.TIDAL -> arrayListOf(context.getString(R.string.tidal_package_name))
            SupportedApps.SPOTIFY_LITE -> arrayListOf(context.getString(R.string.spotify_lite_package_name))
            SupportedApps.PANDORA -> arrayListOf(context.getString(R.string.pandora_package_name))
            SupportedApps.LiveOne -> arrayListOf(context.getString(R.string.liveOne_package_name))
            SupportedApps.Soundcloud -> arrayListOf(context.getString(R.string.soundcloud_package_name))
            SupportedApps.JIO_SAAVN -> arrayListOf(context.getString(R.string.jio_saavn_pkg_name))
            SupportedApps.CUSTOM -> preference.getCustomApps().filter { a -> a.isEnabled }.map { a -> a.packageName }
            else -> ArrayList()
        }
    }

    private fun getConfiguredApps(context: Context): List<SupportedApps> {
        val preference = Preference(context)
        val apps = SupportedApps.values().filter { a -> preference.isAppConfigured(a) }
        return if (preference.getCustomApps().isEmpty()) {
            apps
        } else {
            apps + SupportedApps.CUSTOM
        }
    }

    private fun getConfiguredPackages(): List<String> {
        return getConfiguredApps(context).flatMap { a -> getPackageName(a) }
    }

    private fun getAppProperty(appPackage: String, property: String, field: String): String? {
        val result: BufferedReader
        try {
            val su = Runtime.getRuntime().exec("su")
            val command = DataOutputStream(su.outputStream)

            command.writeBytes("appops get $appPackage $property\n")
            command.flush()

            command.writeBytes("exit\n")
            command.flush()
            su.waitFor()

            result = BufferedReader(InputStreamReader(su.inputStream))
        } catch (_: Exception) {
            return null
        }

        var line = result.readLine()
        while (line != null) {
            for (f in line.split(';')) {
                val kv = f.split(':', limit = 2)
                if (kv.size < 2) continue

                if (kv[0].trim() == field) {
                    return kv[1].trim()
                }
            }
            line = result.readLine()
        }

        return null
    }

    fun isPackageMuted(pack: String): Boolean? {
        val statePlay = getAppProperty(pack, PROPERTY_PLAY, FIELD_PLAY_STATUS)
        val stateFocus = getAppProperty(pack, PROPERTY_FOCUS, FIELD_FOCUS_STATUS)
        if (statePlay == null || stateFocus == null) {
            return null
        }

        if ((statePlay != MUTE_PLAY && statePlay != UNMUTE_PLAY) || (stateFocus != MUTE_FOCUS && stateFocus != UNMUTE_FOCUS)) {
            return null
        }

        return statePlay != UNMUTE_PLAY || stateFocus != UNMUTE_FOCUS
    }

    fun mutePackage(pack: String, addNotificationHelper: AppNotificationHelper?, preference: Preference) {
        try {
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)

            outputStream.writeBytes("appops set $pack $PROPERTY_PLAY $MUTE_PLAY\n")
            outputStream.writeBytes("appops set $pack $PROPERTY_FOCUS $MUTE_FOCUS\n")
            outputStream.flush()

            outputStream.writeBytes("exit\n")
            outputStream.flush()
            su.waitFor()
        } catch (_: Exception) {
            return
        }

        mutedPackages.add(pack)

        if (preference.isDebugLogEnabled()) {
            LogManager.addLifecycleLog(LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = true,
                title = "Root Mute Executed for package \"$pack\"",
                text = "Audio playback disabled for package",
                subText = "Action"
            ))
        }

        this.updateNotification("AdSilence, ad-detected", preference, addNotificationHelper)
    }

    fun unmutePackage(pack: String, addNotificationHelper: AppNotificationHelper?, preference: Preference) {
        try {
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)

            outputStream.writeBytes("appops set $pack $PROPERTY_PLAY $UNMUTE_PLAY\n")
            outputStream.writeBytes("appops set $pack $PROPERTY_FOCUS $UNMUTE_FOCUS\n")
            outputStream.flush()

            outputStream.writeBytes("exit\n")
            outputStream.flush()
            su.waitFor()
        } catch (_: Exception) {
            return
        }

        mutedPackages.remove(pack)

        if (preference.isDebugLogEnabled()) {
            LogManager.addLifecycleLog(LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = true,
                title = "Root Unmute Executed for package \"$pack\"",
                text = "Audio playback enabled for package",
                subText = "Action"
            ))
        }

        this.updateNotification("AdSilence, ad-detected", preference, addNotificationHelper)
    }

    private fun updateNotification(msg: String, preference: Preference, addNotificationHelper: AppNotificationHelper?) {
        if (preference.isNotificationsEnabled()) {
            addNotificationHelper?.updateNotification(msg)
        }
    }
}