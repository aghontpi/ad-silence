package bluepie.ad_silence

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

// todo: create a notification, so app wont be killed.
class AdSilenceActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.grant_permission).setOnClickListener {
            val msg = "Opening Notification Settings"
            Log.v(TAG, msg)
            startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
        }

        configureToggle()
    }


    private fun configureToggle(){
        val preference = Preference(getPreferences(MODE_PRIVATE))
        val statusToggle: Switch = findViewById<Switch>(R.id.status_toggle)
        val appNotificationHelper = AppNotificationHelper(applicationContext)

        statusToggle.setOnClickListener {
            val toChange: Boolean = ! preference.isEnabled()
            preference.setEnabled(toChange)
            if(toChange){
                appNotificationHelper.enable()
            } else {
                appNotificationHelper.disable()
            }
        }
        if(preference.isEnabled()){
            statusToggle.setChecked(true)
            appNotificationHelper.start()
        }

    }
}
