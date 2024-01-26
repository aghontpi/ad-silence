package bluepie.ad_silence

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class Preference(private val context: Context) {
    private val TAG = "Preference"
    private var preference: SharedPreferences = context.getSharedPreferences(
        AdSilenceActivity::javaClass.javaClass.simpleName,
        Context.MODE_PRIVATE
    )


    @Suppress("PrivatePropertyName")
    private val APP_ENABLED = "appEnabled"

    @Suppress("PrivatePropertyName")
    private val APP_ENABLED_DEFAULT = true
    val ACCURADIO = "Accuradio"
    private val ACCURAIO_DEFAULT = true
    val SPOTIFY = "Spotify"
    private val SPOTIFY_DEFAULT = true
    val TIDAL = "Tidal"
    private val TIDAL_DEFAULT = true
    val SPOTIFY_LITE = "SpotifyLite"
    private val SPOTIFY_LITE_DEFAULT = true
    val PANDORA = "Pandora"
    private val PANDORA_DEFAULT = true
    val LIVEONE = "Liveone"
    private val LIVEONE_DEFAULT = true
    val SOUNDCLOUD = "Soundcloud"
    private val SOUNDCLOUD_DEFAULT = true


    private val Android13NotificationPermissionGranted = "Android13NotificationPermissionGranted"
    private val Android13NotificationPermissionGrantedDefault = false

    private val HAS_REQUESTD_NOTIFICATON_POSTING_PERMISSION = "HasRequestedNotificationPostingPermission"
    private val HAS_REQUESTED_NOTIFICATION_POSTING_PERMISSION_DEFAULT = false

    private val EnableNotifications = "EnableNotifications"
    private val EnableNotificationsDefault = false

    private val HAS_DISABLED_HIBERNATION = "HasDisabledHibernation"
    private val HAS_DISABLED_HIBERNATION_DEFAULT = false


    fun isEnabled(): Boolean {
        return preference.getBoolean(APP_ENABLED, APP_ENABLED_DEFAULT)
    }

    fun setEnabled(status: Boolean) {
        preference.edit { putBoolean(APP_ENABLED, status).commit() }
        Log.v(TAG, "changing appStatus:  ${isEnabled()} -> $status")
    }

    fun setAppConfigured(app: SupportedApps, status: Boolean) {
        Log.v(TAG, "setting appConfiguration: $app -> $status")
        when (app) {
            SupportedApps.ACCURADIO -> preference.edit { putBoolean(ACCURADIO, status).commit() }
            SupportedApps.SPOTIFY -> preference.edit { putBoolean(SPOTIFY, status).commit() }
            SupportedApps.TIDAL-> preference.edit { putBoolean(TIDAL, status).commit() }
            SupportedApps.SPOTIFY_LITE-> preference.edit { putBoolean(SPOTIFY_LITE, status).commit() }
            SupportedApps.PANDORA-> preference.edit { putBoolean(PANDORA, status).commit() }
            SupportedApps.LiveOne-> preference.edit { putBoolean(LIVEONE, status).commit() }
            SupportedApps.Soundcloud-> preference.edit { putBoolean(SOUNDCLOUD, status).commit() }
            else -> {}
        }
    }

    fun isAppConfigured(app: SupportedApps): Boolean {
        val status = when (app) {
            SupportedApps.ACCURADIO -> preference.getBoolean(ACCURADIO, ACCURAIO_DEFAULT)
            SupportedApps.SPOTIFY -> preference.getBoolean(SPOTIFY, SPOTIFY_DEFAULT)
            SupportedApps.TIDAL-> preference.getBoolean(TIDAL, TIDAL_DEFAULT)
            SupportedApps.SPOTIFY_LITE-> preference.getBoolean(SPOTIFY_LITE, SPOTIFY_LITE_DEFAULT)
            SupportedApps.PANDORA-> preference.getBoolean(PANDORA, PANDORA_DEFAULT)
            SupportedApps.LiveOne-> preference.getBoolean(LIVEONE, LIVEONE_DEFAULT)
            SupportedApps.Soundcloud-> preference.getBoolean(SOUNDCLOUD, SOUNDCLOUD_DEFAULT)
            else -> false
        }

        if(app != SupportedApps.INVALID){
            Log.v(TAG, "getting appConfiguration: $app -> $status")
        }
        return status
    }

    fun isNotificationPostingPermissionGranted(): Boolean {
        return preference.getBoolean(Android13NotificationPermissionGranted, Android13NotificationPermissionGrantedDefault)
    }

    fun setNotificationPostingPermission(status: Boolean) {
        preference.edit {
            putBoolean(Android13NotificationPermissionGranted, status).commit()
        }
        Log.v(TAG, "[notificationPostingPermission]:  ${isNotificationPostingPermissionGranted()} -> $status")
    }

    fun isNotificationPermissionRequested(): Boolean {
        return preference.getBoolean(HAS_REQUESTD_NOTIFICATON_POSTING_PERMISSION, HAS_REQUESTED_NOTIFICATION_POSTING_PERMISSION_DEFAULT)
    }

    fun setNotificationPermissionRequested(status: Boolean) {
        preference.edit {
            putBoolean(HAS_REQUESTD_NOTIFICATON_POSTING_PERMISSION, status).commit()
        }
        Log.v(TAG, "[hasRequestedNotificationPermission] ${isNotificationPermissionRequested()} -> $status")
    }


    // current implementation is only enabled for android 13 and above
    fun isNotificationsEnabled(): Boolean {
        return preference.getBoolean(EnableNotifications, EnableNotificationsDefault)
    }

    // current implementation is only enabled for android 13 and above
    fun setNotificationEnabled(status: Boolean) {
        Log.v(TAG, "[configNotificationChange] ${isNotificationsEnabled()} -> $status")
        preference.edit {
            putBoolean(EnableNotifications, status).commit()
        }
    }

    fun isHibernationDisabled(): Boolean {
        return preference.getBoolean(HAS_DISABLED_HIBERNATION, HAS_DISABLED_HIBERNATION_DEFAULT)
    }

    fun setHibernatonDisabledStatus(status: Boolean) {
        Log.v(TAG, "[configHibernationEnabled] ${isHibernationDisabled()} -> $status")
        preference.edit{
            putBoolean(HAS_DISABLED_HIBERNATION, status).commit()
        }
    }
}




