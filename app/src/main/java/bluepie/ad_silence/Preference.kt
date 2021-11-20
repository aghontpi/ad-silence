package bluepie.ad_silence

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class Preference(private var preference: SharedPreferences) {
    private val TAG = "Preference"

    @Suppress("PrivatePropertyName")
    private val APP_ENABLED = "appEnabled"
    @Suppress("PrivatePropertyName")
    private val APP_ENABLED_DEFAULT = true

    fun isEnabled(): Boolean{
        return preference.getBoolean(APP_ENABLED, APP_ENABLED_DEFAULT)
    }

    fun setEnabled(status: Boolean){
        preference.edit { putBoolean(APP_ENABLED, status).commit()  }
        Log.v(TAG, "changing  ${isEnabled()} -> $status")
    }
}

