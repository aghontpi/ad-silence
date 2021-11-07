package bluepie.ad_silence

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val audioManager: AudioManager =
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager

        (findViewById<Button>(R.id.vol_inc_tmp)).setOnClickListener {
            val msg = "Volume Inc"
            Log.v(TAG, msg)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
        }

        (findViewById<Button>(R.id.vol_dec_tmp)).setOnClickListener {
            val msg = "Volume Dec"
            Log.v(TAG, msg)
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
        }

        findViewById<Button>(R.id.volume_mute_tmp).setOnClickListener {
            val msg = "Volume Mute"
            Log.v(TAG, msg)
            audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND)
        }

        findViewById<Button>(R.id.volume_unmute_tmp).setOnClickListener {
            val msg = "Volume UnMute"
            Log.v(TAG, msg)
            audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND)
        }

        findViewById<Button>(R.id.notification_access_settings).setOnClickListener {
            val msg = "Opening Notification Settings"
            Log.v(TAG, msg)
            startActivity(Intent(getString(R.string.notification_listener_settings_intent)))
        }

    }
}