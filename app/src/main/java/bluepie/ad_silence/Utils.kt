package bluepie.ad_silence

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Switch
import kotlin.RuntimeException

class Utils {
    private val TAG = "Utils"

    fun disableSwitch(toggle: Switch) {
        toggle.isChecked = false
        toggle.isEnabled = false
    }

    fun enableSwitch(toggle: Switch) {
        toggle.isChecked = true
        toggle.isEnabled = true
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.v(TAG, "exception: $e")
            false
        }
    }

    fun isAccuradioInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.accuradio_pkg_name))

    fun isSpotifyInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.spotify_package_name))

    fun isSpotifyLiteInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.spotify_lite_package_name))

    fun isTidalInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.tidal_package_name))

    fun isPandoraInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.pandora_package_name))

    fun isLiveOneInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.liveOne_package_name))

    fun isSoundcloudInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.soundcloud_package_name))

    fun isMusicMuted(audoManager: AudioManager): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            return audoManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            val volume = try {
                audoManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            } catch (e: RuntimeException) {
                Log.v(
                    TAG,
                    "Could not retrieve stream volume for stream type " + AudioManager.STREAM_MUSIC
                )
                  if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Volume Check Error",
                        text = "Could not retrieve stream volume (Runtime Exception)",
                        subText = "Error"
                    ))
                }
                audoManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            }
            return volume == 0
        }
    }

    fun mute(audioManager: AudioManager?, addNotificationHelper: AppNotificationHelper?, preference: Preference) {
        if (preference.isMuteEntireDeviceEnabled() && Build.VERSION.SDK_INT >= 23) {
             audioManager?.adjustVolume(AudioManager.ADJUST_MUTE, 0)
        } else {
             audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        }

        if (preference.isDebugLogEnabled()) {
            LogManager.addLifecycleLog(LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = true,
                title = "System Mute Executed",
                text = "Mute command sent to AudioManager",
                subText = "Action"
            ))
        }

        this.updateNotification("AdSilence, ad-detected", preference, addNotificationHelper)
    }

    fun getUnmuteDelay(app: SupportedApps, packageName: String? = null, preference: Preference? = null): Long {
        if (app == SupportedApps.CUSTOM && packageName != null && preference != null) {
            return preference.getCustomApps().find { it.packageName == packageName }?.unmuteDelay ?: 0
        }
        return when (app) {
            SupportedApps.SPOTIFY_LITE -> 540
            SupportedApps.SPOTIFY -> 480
            else -> 0
        }
    }

    fun unmute(
        audioManager: AudioManager?,
        addNotificationHelper: AppNotificationHelper?,
        app: SupportedApps,
        preference: Preference
    ) {
        if (preference.isMuteEntireDeviceEnabled() && Build.VERSION.SDK_INT >= 23) {
             audioManager?.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
        } else {
             audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
        }

        if (preference.isDebugLogEnabled()) {
            LogManager.addLifecycleLog(LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "System Unmute Executed",
                text = "Unmute command sent to AudioManager",
                subText = "Action"
            ))
        }

        this.updateNotification("AdSilence, listening for ads", preference, addNotificationHelper)
    }

    private fun updateNotification(msg: String, preference: Preference, addNotificationHelper: AppNotificationHelper?) {
        if (preference.isNotificationsEnabled()) {
            addNotificationHelper?.updateNotification(msg)
        }
    }
}

