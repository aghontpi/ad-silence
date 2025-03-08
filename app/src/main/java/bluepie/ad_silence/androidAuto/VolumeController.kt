package bluepie.ad_silence.androidAuto

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

private var duckFocusRequest: AudioFocusRequest? = null


// Global listener for pre-O devices
private val duckFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
}

fun requestDuckAudioFocus(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        duckFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener { /* handle changes if needed */ }
            .build()
        audioManager.requestAudioFocus(duckFocusRequest!!)
    } else {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
            duckFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
    }
}

fun abandonDuckAudioFocus(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        duckFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            duckFocusRequest = null
        }
    } else {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(duckFocusChangeListener)
    }
}

private fun muteMediaStream(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            0
        )
    } else {
        @Suppress("DEPRECATION")
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
    }
}

private fun unmuteMediaStream(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE,
            0
        )
    } else {
        @Suppress("DEPRECATION")
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
    }
}
