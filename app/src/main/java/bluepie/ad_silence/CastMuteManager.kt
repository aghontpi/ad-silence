package bluepie.ad_silence

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.util.Log

class CastMuteManager(private val context: Context) {
    private val TAG = "CastMuteManager"
    private var isMutedByManager = false
    private var originalVolume = -1

    fun resetState() {
        Log.v(TAG, "Resetting state")
        isMutedByManager = false
        originalVolume = -1
    }

    fun tryMute(notificationListener: NotificationListener): Boolean {
        val controller = notificationListener.getMediaControllerForCasting()
        if (controller != null) {
            Log.v(TAG, "Found MediaController, attempting to mute...")
            if (Preference(context).isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = true,
                    title = "Casting Mute Attempt",
                    text = "Found MediaController, attempting to mute...",
                    subText = "Cast Manager"
                ))
            }
            try {
                val current = controller.playbackInfo?.currentVolume
                
                // Consistency Check: If manager thinks it's muted, but volume is > 0, 
                // it means user/app unmuted externally. Treat as fresh mute.
                if (isMutedByManager && current != null && current > 0) {
                    Log.w(TAG, "Manager was marked muted, but current volume is $current. External unmute detected. Resaving.")
                    if (Preference(context).isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = true,
                            title = "External Unmute Detected",
                            text = "Manager thought muted/0, but found vol=$current. Resaving.",
                            subText = "Cast Manager"
                        ))
                    }
                    isMutedByManager = false // Reset to force save
                }

                // Save volume only if not already muted by CasteMuteManager
                // a bug where subsequence mute, saves as 0.
                if (!isMutedByManager) {
                    // Only save if current volume is > 0 to prevent "sticking" to 0 if restarted while muted
                    if (current != null && current > 0) {
                        originalVolume = current
                        Log.v(TAG, "Saved cast volume: $originalVolume")
                        if (Preference(context).isDebugLogEnabled()) {
                            LogManager.addLifecycleLog(LogEntry(
                                appName = "AdSilence",
                                timestamp = System.currentTimeMillis(),
                                isAd = true,
                                title = "Saved Cast Volume",
                                text = "Saved cast volume: $originalVolume",
                                subText = "Cast Manager"
                            ))
                        }
                    } else {
                        Log.v(TAG, "Skipping save of cast volume: $current (Too low or null)")
                    }
                } else {
                    Log.v(TAG, "Already muted by manager, keeping original volume: $originalVolume")
                }
                
                controller.setVolumeTo(0, 0)
                isMutedByManager = true
                if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = true,
                        title = "Casting Muted",
                        text = "Successfully muted via MediaController",
                        subText = "Cast Manager"
                    ))
                }
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error muting via MediaController", e)
                if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = true,
                        title = "Casting Mute Error",
                        text = "Error muting via MediaController: ${e.message}",
                        subText = "Cast Manager"
                    ))
                }
            }
        } else {
             if (Preference(context).isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = true,
                    title = "Casting Mute Failed",
                    text = "No MediaController found for casting",
                    subText = "Cast Manager"
                ))
            }
        }
        return false
    }

    fun tryUnmute(notificationListener: NotificationListener): Boolean {
        if (!isMutedByManager) {
            return false
        }

        val controller = notificationListener.getMediaControllerForCasting()
        if (controller != null) {
            Log.v(TAG, "Found MediaController, attempting to unmute...")
             if (Preference(context).isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Casting Unmute Attempt",
                    text = "Found MediaController, attempting to unmute...",
                    subText = "Cast Manager"
                ))
            }
            try {
                if (originalVolume > 0) {
                     Log.v(TAG, "Restoring cast volume to $originalVolume")
                     controller.setVolumeTo(originalVolume, 0)
                     if (Preference(context).isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Restoring Cast Volume",
                            text = "Restoring cast volume to $originalVolume",
                            subText = "Cast Manager"
                        ))
                    }
                     originalVolume = -1
                } else {
                     // Fallback: If no valid original volume, set to safe default (e.g. 1/3 max)
                     // because ADJUST_UNMUTE often doesn't work well for Cast if volume is 0
                     val max = controller.playbackInfo?.maxVolume ?: 50 // Default 50 if null
                     val safeVol = (max / 3).coerceAtLeast(1)
                     
                     Log.v(TAG, "No saved volume, setting to safe default: $safeVol (Max: $max)")
                     // need to revert to // controller.adjustVolume(AudioManager.ADJUST_UNMUTE, 0) if below is causing issues
                     controller.setVolumeTo(safeVol, 0)
                     
                     if (Preference(context).isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Casting Unmute Fallback",
                            text = "No saved volume, setting to safe default: $safeVol (Max: $max)",
                            subText = "Cast Manager"
                        ))
                    }
                }
                
                isMutedByManager = false
                if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Casting Unmuted",
                        text = "Successfully unmuted via MediaController",
                        subText = "Cast Manager"
                    ))
                }
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error unmuting via MediaController", e)
                 if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Casting Unmute Error",
                        text = "Error unmuting via MediaController: ${e.message}",
                        subText = "Cast Manager"
                    ))
                }
                try {
                     controller.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                     isMutedByManager = false
                      if (Preference(context).isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Casting Unmute Fallback Retry",
                            text = "Attempted fallback adjustVolume UNMUTE after error",
                            subText = "Cast Manager"
                        ))
                    }
                     return true
                } catch (e2: Exception) {
                     Log.e(TAG, "Fallback also failed", e2)
                      if (Preference(context).isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Casting Unmute Fatal Error",
                            text = "Fallback also failed: ${e2.message}",
                            subText = "Cast Manager"
                        ))
                    }
                }
            }
        }
        
        // Even if controller is null (session might get lost), reset flag
        isMutedByManager = false
        return false
    }
    
    fun isMuted() = isMutedByManager
}
