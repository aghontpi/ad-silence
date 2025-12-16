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

    private val DEBUG_LOG_ENABLED = "DebugLogEnabled"
    private val DEBUG_LOG_ENABLED_DEFAULT = false


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
            SupportedApps.CUSTOM -> {} // Custom apps are handled via setCustomAppEnabled
            else -> {}
        }
    }

    fun isAppConfigured(app: SupportedApps, packageName: String? = null): Boolean {
        val status = when (app) {
            SupportedApps.ACCURADIO -> preference.getBoolean(ACCURADIO, ACCURAIO_DEFAULT)
            SupportedApps.SPOTIFY -> preference.getBoolean(SPOTIFY, SPOTIFY_DEFAULT)
            SupportedApps.TIDAL-> preference.getBoolean(TIDAL, TIDAL_DEFAULT)
            SupportedApps.SPOTIFY_LITE-> preference.getBoolean(SPOTIFY_LITE, SPOTIFY_LITE_DEFAULT)
            SupportedApps.PANDORA-> preference.getBoolean(PANDORA, PANDORA_DEFAULT)
            SupportedApps.LiveOne-> preference.getBoolean(LIVEONE, LIVEONE_DEFAULT)
            SupportedApps.Soundcloud-> preference.getBoolean(SOUNDCLOUD, SOUNDCLOUD_DEFAULT)
            SupportedApps.CUSTOM -> {
                if (packageName != null) {
                    getCustomApps().find { it.packageName == packageName }?.isEnabled ?: false
                } else {
                    false
                }
            }
            else -> false
        }

        if(app != SupportedApps.INVALID){
            val extraInfo = if (app == SupportedApps.CUSTOM && packageName != null) {
                val name = getCustomApps().find { it.packageName == packageName }?.name
                if (name != null) "($name : $packageName)" else "($packageName)"
            } else if (packageName != null) {
                 "($packageName)"
            } else {
                ""
            }
            Log.v(TAG, "getting appConfiguration: $app $extraInfo -> $status")
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


    fun isNotificationsEnabled(): Boolean {
        return preference.getBoolean(EnableNotifications, EnableNotificationsDefault)
    }

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

    fun isDebugLogEnabled(): Boolean {
        return preference.getBoolean(DEBUG_LOG_ENABLED, DEBUG_LOG_ENABLED_DEFAULT)
    }

    fun setDebugLogEnabled(status: Boolean) {
        Log.v(TAG, "[configDebugLogEnabled] ${isDebugLogEnabled()} -> $status")
        preference.edit {
            putBoolean(DEBUG_LOG_ENABLED, status).commit()
        }
    }

    /* Mute Behavior Preferences */
    private val MUTE_ENTIRE_DEVICE = "MuteEntireDevice"
    private val MUTE_ENTIRE_DEVICE_DEFAULT = false

    fun isMuteEntireDeviceEnabled(): Boolean {
        return preference.getBoolean(MUTE_ENTIRE_DEVICE, MUTE_ENTIRE_DEVICE_DEFAULT)
    }

    fun setMuteEntireDeviceEnabled(status: Boolean) {
        Log.v(TAG, "[configMuteEntireDevice] ${isMuteEntireDeviceEnabled()} -> $status")
        preference.edit {
            putBoolean(MUTE_ENTIRE_DEVICE, status).commit()
        }
    }

    private val CUSTOM_APPS = "CustomApps"

    companion object {
        private var cachedCustomApps: List<CustomApp>? = null
    }

    fun getCustomApps(): List<CustomApp> {
        if (cachedCustomApps != null) {
            return cachedCustomApps!!
        }

        val jsonString = preference.getString(CUSTOM_APPS, "[]") ?: "[]"
        val customApps = mutableListOf<CustomApp>()
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val name = jsonObject.getString("name")
                val packageName = jsonObject.getString("packageName")
                val keywordsJsonArray = jsonObject.getJSONArray("keywords")
                val keywords = mutableListOf<String>()
                for (j in 0 until keywordsJsonArray.length()) {
                    keywords.add(keywordsJsonArray.getString(j))
                }
                val isEnabled = jsonObject.optBoolean("isEnabled", true)
                val unmuteDelay = jsonObject.optLong("unmuteDelay", 0)
                customApps.add(CustomApp(name, packageName, keywords, isEnabled, unmuteDelay))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing custom apps", e)
        }
        cachedCustomApps = customApps
        return customApps
    }

    fun addCustomApp(app: CustomApp) {
        Log.v(TAG, "Adding custom app: ${app.name} (${app.packageName})")
        val currentApps = getCustomApps().toMutableList()
        // Remove if exists to update
        currentApps.removeAll { it.packageName == app.packageName }
        currentApps.add(app)
        saveCustomApps(currentApps)
    }

    fun removeCustomApp(packageName: String) {
        Log.v(TAG, "Removing custom app: $packageName")
        val currentApps = getCustomApps().toMutableList()
        currentApps.removeAll { it.packageName == packageName }
        saveCustomApps(currentApps)
    }

    fun setCustomAppEnabled(packageName: String, isEnabled: Boolean) {
        Log.v(TAG, "Setting custom app enabled: $packageName -> $isEnabled")
        val currentApps = getCustomApps().toMutableList()
        currentApps.find { it.packageName == packageName }?.let {
            it.isEnabled = isEnabled
            saveCustomApps(currentApps)
        }
    }

    private fun saveCustomApps(apps: List<CustomApp>) {
        cachedCustomApps = apps
        val jsonArray = org.json.JSONArray()
        apps.forEach { app ->
            val jsonObject = org.json.JSONObject()
            jsonObject.put("name", app.name)
            jsonObject.put("packageName", app.packageName)
            val keywordsArray = org.json.JSONArray()
            app.keywords.forEach { keywordsArray.put(it) }
            jsonObject.put("keywords", keywordsArray)
            jsonObject.put("isEnabled", app.isEnabled)
            jsonObject.put("unmuteDelay", app.unmuteDelay)
            jsonArray.put(jsonObject)
        }
        preference.edit { putString(CUSTOM_APPS, jsonArray.toString()).commit() }
    }
}




