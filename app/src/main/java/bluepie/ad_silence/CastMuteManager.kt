package bluepie.ad_silence

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.util.Log

class CastMuteManager(private val context: Context) {
    private val TAG = "CastMuteManager"
    private var isMutedByManager = false

    private var originalVolume = -1

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
                // Save volume only if not already muted by CasteMuteManager
                // a bug where subsequence mute, saves as 0.
                if (!isMutedByManager) {
                    val current = controller.playbackInfo?.currentVolume
                    if (current != null) {
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
                if (originalVolume != -1) {
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
                     // Fallback if missed saving
                     Log.v(TAG, "No saved volume, adjusting unmute")
                     controller.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                     if (Preference(context).isDebugLogEnabled()) {
                        LogManager.addLifecycleLog(LogEntry(
                            appName = "AdSilence",
                            timestamp = System.currentTimeMillis(),
                            isAd = false,
                            title = "Casting Unmute Fallback",
                            text = "No saved volume, using adjustVolume UNMUTE",
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
