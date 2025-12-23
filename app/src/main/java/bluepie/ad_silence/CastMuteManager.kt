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
            try {
                // Save volume only if not already muted by CasteMuteManager
                // a bug where subsequence mute, saves as 0.
                if (!isMutedByManager) {
                    val current = controller.playbackInfo?.currentVolume
                    if (current != null) {
                        originalVolume = current
                        Log.v(TAG, "Saved cast volume: $originalVolume")
                    }
                } else {
                    Log.v(TAG, "Already muted by manager, keeping original volume: $originalVolume")
                }
                
                controller.setVolumeTo(0, 0)
                isMutedByManager = true
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error muting via MediaController", e)
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
            try {
                if (originalVolume != -1) {
                     Log.v(TAG, "Restoring cast volume to $originalVolume")
                     controller.setVolumeTo(originalVolume, 0)
                     originalVolume = -1
                } else {
                     // Fallback if missed saving
                     Log.v(TAG, "No saved volume, adjusting unmute")
                     controller.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                }
                
                isMutedByManager = false
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error unmuting via MediaController", e)
                try {
                     controller.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                     isMutedByManager = false
                     return true
                } catch (e2: Exception) {
                     Log.e(TAG, "Fallback also failed", e2)
                }
            }
        }
        
        // Even if controller is null (session might get lost), reset flag
        isMutedByManager = false
        return false
    }
    
    fun isMuted() = isMutedByManager
}
