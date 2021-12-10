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
        }
    }

    fun isAppConfigured(app: SupportedApps): Boolean {
        val status = when (app) {
            SupportedApps.ACCURADIO -> preference.getBoolean(ACCURADIO, ACCURAIO_DEFAULT)
            SupportedApps.SPOTIFY -> preference.getBoolean(SPOTIFY, SPOTIFY_DEFAULT)
            SupportedApps.TIDAL-> preference.getBoolean(TIDAL, TIDAL_DEFAULT)
            else -> false
        }

        if(app != SupportedApps.INVALID){
            Log.v(TAG, "getting appConfiguration: $app -> $status")
        }
        return status
    }
}



