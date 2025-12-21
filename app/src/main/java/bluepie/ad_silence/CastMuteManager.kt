package bluepie.ad_silence

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.util.Log

class CastMuteManager(private val context: Context) {
    private val TAG = "CastMuteManager"
    private var isMutedByManager = false

    fun tryMute(notificationListener: NotificationListener): Boolean {
        val controller = notificationListener.getMediaControllerForCasting()
        if (controller != null) {
            Log.v(TAG, "Found MediaController, attempting to mute...")
            try {
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
                // Some apps does not support ADJUST_UNMUTE or it might not work if volume is set to 0 directly.
                val max = controller.playbackInfo?.maxVolume ?: 10
                val targetVol = if (max > 15) max / 3 else max / 2 // Default to reasonable volume
                
                Log.v(TAG, "Unmuting: Setting volume to $targetVol (Max: $max)")
                controller.setVolumeTo(targetVol, 0)
                
                isMutedByManager = false
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error unmuting via MediaController, trying adjust fallback", e)
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
