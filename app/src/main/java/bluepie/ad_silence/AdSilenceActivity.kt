package bluepie.ad_silence

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class AdSilenceActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configurePermission()
        configureToggle()
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        // when resuming after permission is granted
        configurePermission()
        configureToggle()
    }

    private fun configurePermission(){
        val grantPermissionButton = findViewById<Button>(R.id.grant_permission)
        if(!checkNotificationPermission(applicationContext)){
           grantPermissionButton.isEnabled = true
           grantPermissionButton.setOnClickListener {
                val msg = "Opening Notification Settings"
                Log.v(TAG, msg)
                startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
            }
        } else {
           grantPermissionButton.text = getString(R.string.permission_granted)
           grantPermissionButton.isEnabled = false
        }
    }


    private fun configureToggle(){
        val preference = Preference(getPreferences(MODE_PRIVATE))
        val statusToggle: Switch = findViewById<Switch>(R.id.status_toggle)
        val appNotificationHelper = AppNotificationHelper(applicationContext)
        val utils = Utils()

        if (!checkNotificationPermission(applicationContext)){
            // even if appNotification is disabled, while granting permission
            //   force it to be enabled, otherwise it wont be listed in permission window
            appNotificationHelper.enable()
            utils.disableSwitch(statusToggle)
            return
        } else {
            utils.enableSwitch(statusToggle)
        }

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
            statusToggle.isChecked = true
            appNotificationHelper.start()
        } else {
            statusToggle.isChecked = false
        }

    }
}



