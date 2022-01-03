package bluepie.ad_silence

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Switch

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

    fun isSpotifyLiteInstalled(context: Context) =  isPackageInstalled(context, context.getString(R.string.spotify_lite_package_name))

    fun isTidalInstalled(context: Context) =
        isPackageInstalled(context, context.getString(R.string.tidal_package_name))


    fun mute(audioManager: AudioManager?, addNotificationHelper: AppNotificationHelper?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager?.adjustVolume(
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_PLAY_SOUND
            )
        } else {
            audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
        }
        addNotificationHelper?.updateNotification("AdSilence, ad-detected")
    }

    fun unmute(
        audioManager: AudioManager?,
        addNotificationHelper: AppNotificationHelper?,
        app: SupportedApps,
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
            addNotificationHelper?.updateNotification("AdSilence, listening for ads")
        }

        if (app == SupportedApps.SPOTIFY || app == SupportedApps.SPOTIFY_LITE) {
            Log.v(TAG, "introducing delay for spotify")
            Handler(Looper.getMainLooper()).postDelayed({
                Log.v(
                    TAG,
                    "running unmute..after delay"
                ); process()
            }, 480)
        } else process()
    }
}

