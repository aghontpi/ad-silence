package bluepie.ad_silence

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

// todo: create a notification, so app wont be killed.
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val audioManager: AudioManager =
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager

        findViewById<Button>(R.id.grant_permission).setOnClickListener {
            val msg = "Opening Notification Settings"
            Log.v(TAG, msg)
            startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
        }

    }
}