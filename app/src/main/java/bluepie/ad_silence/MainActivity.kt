package bluepie.ad_silence

import android.content.Intent
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button


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

        // NotificationListener Start
        // notification listener doesn't need to be started
//        applicationContext.startService(
//            Intent(
//                applicationContext,
//                NotificationListener::class.java
//            )
//        )
//        var settingActivity = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
//        startActivity(settingActivity)

    }
}