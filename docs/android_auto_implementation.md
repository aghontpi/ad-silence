# Android Auto Integration Research

This document summarizes findings for implementing AdSilence support on Android Auto.

## 1. Car Mode Detection
**Problem**: `UiModeManager.currentModeType` often returns `UI_MODE_TYPE_NORMAL` even when connected to Android Auto, specifically during "Projection" (phone screen projected to car head unit).

**Solution**: Use a fallback check for specific Audio Output Devices.
- **Primary**: `UiModeManager` (standard check).
- **Fallback**: Check `AudioManager.getDevices(GET_DEVICES_OUTPUTS)` for:
    - `TYPE_REMOTE_SUBMIX` (Standard for AA Projection)
    - `TYPE_BUS` (Automotive OS)
    - `TYPE_IP`

### Code Snippet
```kotlin
fun isCarConnected(context: Context, audioManager: AudioManager): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR) return true

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { 
            it.type == AudioDeviceInfo.TYPE_REMOTE_SUBMIX || 
            it.type == AudioDeviceInfo.TYPE_BUS || 
            it.type == AudioDeviceInfo.TYPE_IP 
        }
    }
    return false
}
```

## 2. Volume Control Strategy
**Problem**: 
1. `audioManager.setStreamVolume(STREAM_MUSIC, 0, 0)` is often ignored by Head Units (volume is controlled externally/read-only).
2. `AUDIOFOCUS_GAIN_TRANSIENT` causes the main music app (e.g., Spotify) to **PAUSE**, which stops the ad playback (if the ad needs to play to finish).
3. `ADJUST_MUTE` commands are often ignored.

**Solution**: **Active Ducking**
Use `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` *while* playing a silent `AudioTrack`.
- The `MAY_DUCK` request tells the music app to lower volume.
- Valid active audio (even silence) ensures the system respects the focus request.

### Code Snippet (Mute/Duck)
```kotlin
// 1. Start playing silence to tell system "I am an active media app"
val buffSize = AudioTrack.getMinBufferSize(44100, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT)
val audioTrack = AudioTrack.Builder()...build()
audioTrack.play() // Loop silent buffer

// 2. Request Focus with Ducking
val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    .setAudioAttributes(...)
    .build()
audioManager.requestAudioFocus(focusRequest)
```

**Result**: Background music volume drops (~70%), ad continues playing but is much quieter.

### Code Snippet (Unmute/Restore)
```kotlin
// 1. Stop Silence
audioTrack?.stop()
audioTrack?.release()

// 2. Abandon Focus
audioManager.abandonAudioFocusRequest(focusRequest)
```

## 3. Full Implementation
Below is the reusable code for `CarHelper.kt`, which encapsulates all the logic.

### `CarHelper.kt`
```kotlin
package bluepie.ad_silence

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.media.AudioDeviceInfo
import android.os.Build

class CarHelper(private val context: Context, private val audioManager: AudioManager?) {
    private val TAG = "CarHelper"
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private var audioTrack: android.media.AudioTrack? = null
    
    // AudioFocusChangeListener is needed for the request
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio Focus changed: $focusChange")
    }

    fun isCarConnected(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR) return true

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

    fun attemptMute(addNotificationHelper: AppNotificationHelper?, preference: Preference): Boolean {
        if (!isCarConnected()) return false
        
        try {
            Log.v(TAG, "Car connected, playing silence and requesting Focus (MAY_DUCK).")
            
            // 1. Play Silent Audio (Forces system to treat us as active media)
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
                        AudioManager.STREAM_MUSIC, sampleRate,
                        android.media.AudioFormat.CHANNEL_OUT_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT,
                        buffSize, android.media.AudioTrack.MODE_STATIC
                    )
                }
                
                val silence = ByteArray(buffSize)
                audioTrack?.write(silence, 0, buffSize)
                audioTrack?.setLoopPoints(0, buffSize / 2, -1) // Loop infinitely
                audioTrack?.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play silence", e)
            }

            // 2. Request Audio Focus (Duck)
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
                if (preference.isNotificationsEnabled()) {
                    addNotificationHelper?.updateNotification("AdSilence, ad-detected (Duck)")
                }
                return true
            } else {
                // Cleanup on failure
                try { audioTrack?.stop(); audioTrack?.release(); audioTrack = null } catch (e: Exception) {}
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in car mute", e)
        }
        return false
    }

    fun attemptUnmute(addNotificationHelper: AppNotificationHelper?, preference: Preference): Boolean {
        if (!isCarConnected()) return false
        
        try {
            Log.v(TAG, "Car connected, stopping silence and abandoning Audio Focus.")
            
            // 1. Stop Silence
            try {
                if (audioTrack != null) {
                    if (audioTrack!!.playState == android.media.AudioTrack.PLAYSTATE_PLAYING) audioTrack?.stop()
                    audioTrack?.release()
                    audioTrack = null
                }
            } catch (e: Exception) { Log.e(TAG, "Error stopping silence", e) }

            // 2. Abandon Focus
            val result: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) {
                    result = audioManager?.abandonAudioFocusRequest(audioFocusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                } else { result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED }
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
            Log.e(TAG, "Error in car unmute", e)
        }
        return false
    }
}
```

### Integration Snippet (`NotificationListener.kt`)
```kotlin
// In onCreate()
val carHelper = CarHelper(applicationContext, audioManager)

// In muteLogic()
if (carHelper?.attemptMute(appNotificationHelper, preference) == true) {
    // Car mute handled, skip standard logic
    isMuted = true
} else {
    // Fallback to standard phone mute
    utils.mute(audioManager, ...)
}
```
