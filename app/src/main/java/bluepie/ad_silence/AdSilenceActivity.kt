package bluepie.ad_silence

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch

class AdSilenceActivity : Activity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configurePermission()
        checkAppInstalled()
        configureToggle()
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        // when resuming after permission is granted
        configurePermission()
        checkAppInstalled()
        configureToggle()
    }

    private fun checkAppInstalled(){
        val utils  = Utils()
        val statusToggle = findViewById<Switch>(R.id.status_toggle)
        Log.v(TAG, "Accuradio installed ?: ${utils.isAccuradioInstalled(applicationContext)}")
        when(utils.isAccuradioInstalled(applicationContext)){
            true -> {
                val msg = kotlin.run {
                   if (checkNotificationPermission(applicationContext)) getString(R.string.app_status)
                   else getString(R.string.app_status_permission_not_granted)
                }
                statusToggle.text = msg
                utils.enableSwitch(statusToggle)
            }
            false -> {
                statusToggle.text = getString(R.string.app_status_accuradio_not_installed)
                utils.disableSwitch(statusToggle)
            }
        }
    }

    private fun configurePermission(){
        val grantPermissionButton = findViewById<Button>(R.id.grant_permission)
        val statusToggle = findViewById<Switch>(R.id.status_toggle)

        when (checkNotificationPermission(applicationContext)){
            true -> {
                grantPermissionButton.text = getString(R.string.permission_granted)
                grantPermissionButton.isEnabled = false
            }
            false -> {
                statusToggle.text = getString(R.string.app_status_permission_not_granted)
                grantPermissionButton.isEnabled = true
                grantPermissionButton.setOnClickListener {
                    val msg = "Opening Notification Settings"
                    Log.v(TAG, msg)
                    startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
                }
            }
        }
    }


    private fun configureToggle(){
        val preference = Preference(getPreferences(MODE_PRIVATE))
        val statusToggle = findViewById<Switch>(R.id.status_toggle)
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



