package bluepie.ad_silence

import android.content.Context
import android.media.AudioManager
import android.util.Log

import android.media.AudioDeviceInfo
import android.os.Build

class CarHelper(private val context: Context, private val audioManager: AudioManager?) {
    private val TAG = "CarHelper"

    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    // AudioFocusChangeListener is needed for the request, even if we don't do anything on change
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio Focus changed: $focusChange")
    }

    fun isCarConnected(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR) {
            return true
        }

        // Fallback: Check for Android Auto Projection (Remote Submix) or Automotive (Bus/IP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices?.forEach { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BUS,
                    AudioDeviceInfo.TYPE_IP,
                    AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> {
                        Log.v(TAG, "Car detected via Audio Device: ${device.type}")
                        return true
                    }
                }
            }
        }
        return false
    }

    private var audioTrack: android.media.AudioTrack? = null

    fun attemptMute(addNotificationHelper: AppNotificationHelper?, preference: Preference): Boolean {
        if (isCarConnected()) {
            try {
                Log.v(TAG, "Car connected, playing silence and requesting Focus (MAY_DUCK).")
                
                // 1. Play Silent Audio to enforce active media state
                try {
                    val sampleRate = 44100
                    val buffSize = android.media.AudioTrack.getMinBufferSize(sampleRate, android.media.AudioFormat.CHANNEL_OUT_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioTrack = android.media.AudioTrack.Builder()
                            .setAudioAttributes(android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                            .setAudioFormat(android.media.AudioFormat.Builder()
                                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                            .setBufferSizeInBytes(buffSize)
                            .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        audioTrack = android.media.AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            android.media.AudioFormat.CHANNEL_OUT_MONO,
                            android.media.AudioFormat.ENCODING_PCM_16BIT,
                            buffSize,
                            android.media.AudioTrack.MODE_STATIC
                        )
                    }
                    
                    val silence = ByteArray(buffSize) // Default initialized to 0
                    audioTrack?.write(silence, 0, buffSize)
                    audioTrack?.setLoopPoints(0, buffSize / 2, -1) // Loop infinitely
                    audioTrack?.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play silence", e)
                }

                // 2. Request Audio Focus
                val result: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build()
                    result = audioManager?.requestAudioFocus(audioFocusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                } else {
                    @Suppress("DEPRECATION")
                    result = audioManager?.requestAudioFocus(
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    ) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                }

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.v(TAG, "Audio Focus GRANTED (MAY_DUCK)")
                    if (preference.isNotificationsEnabled()) {
                        addNotificationHelper?.updateNotification("AdSilence, ad-detected (Duck)")
                    }
                    return true
                } else {
                    Log.w(TAG, "Audio Focus FAILED")
                    // If focus failed, ensure we stop the track
                    try {
                        audioTrack?.stop()
                        audioTrack?.release()
                        audioTrack = null
                    } catch (e: Exception) { /* ignore */ }
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in car mute (Focus)", e)
            }
        }
        return false
    }

    fun attemptUnmute(addNotificationHelper: AppNotificationHelper?, preference: Preference): Boolean {
        if (isCarConnected()) {
            try {
                Log.v(TAG, "Car connected, stopping silence and abandoning Audio Focus.")
                
                // 1. Stop Silence
                try {
                    if (audioTrack != null) {
                        if (audioTrack!!.playState == android.media.AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.stop()
                        }
                        audioTrack?.release()
                        audioTrack = null
                    }
                } catch (e: Exception) {
                     Log.e(TAG, "Error stopping silence", e)
                }

                // 2. Abandon Focus
                val result: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (audioFocusRequest != null) {
                        result = audioManager?.abandonAudioFocusRequest(audioFocusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                    } else {
                        // If null, maybe we didn't mock it or it was cleared?
                        result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED 
                    }
                } else {
                    @Suppress("DEPRECATION")
                    result = audioManager?.abandonAudioFocus(audioFocusChangeListener) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                }
                
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                     audioFocusRequest = null
                     if (preference.isNotificationsEnabled()) {
                        addNotificationHelper?.updateNotification("AdSilence, listening for ads")
                     }
                     return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in car unmute (Focus)", e)
            }
        }
        return false
    }
}
