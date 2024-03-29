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
                audoManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            }
            return volume == 0
        }
    }

    fun mute(audioManager: AudioManager?, addNotificationHelper: AppNotificationHelper?, preference: Preference) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager?.adjustVolume(
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_PLAY_SOUND
            )
        } else {
            audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
        }

        this.updateNotification("AdSilence, ad-detected", preference, addNotificationHelper)
    }

    fun unmute(
        audioManager: AudioManager?,
        addNotificationHelper: AppNotificationHelper?,
        app: SupportedApps,
        preference: Preference
    ) {
        val process = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager?.adjustVolume(
                    AudioManager.ADJUST_UNMUTE,
                    AudioManager.FLAG_PLAY_SOUND
                )
            } else {
                audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false)
            }

            this.updateNotification("AdSilence, listening for ads", preference, addNotificationHelper)
        }

        if (app == SupportedApps.SPOTIFY || app == SupportedApps.SPOTIFY_LITE) {
            Log.v(TAG, "introducing delay for spotify")
            val delay: Long = app.run {
                when (this) {
                    SupportedApps.SPOTIFY_LITE -> 540
                    SupportedApps.SPOTIFY -> 480
                    else -> 0
                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                Log.v(
                    TAG,
                    "running unmute..after delay $delay"
                ); process()
            }, delay)
        } else process()
    }

    private fun updateNotification(msg: String, preference: Preference, addNotificationHelper: AppNotificationHelper?) {

        val showNotificaiton : Boolean = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            preference.isNotificationsEnabled()
        } else {
            true
        }
        if (showNotificaiton) {
            addNotificationHelper?.updateNotification(msg)
        }
    }
}

