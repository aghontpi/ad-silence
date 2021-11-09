package bluepie.ad_silence

import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    var mConnected = false
    var isMuted = false
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        Log.v(TAG, "listener created")

    }

    override fun onListenerConnected() {
        Log.v(TAG, "notification listener connected")
        mConnected = true
    }

    override fun onListenerDisconnected() {
        Log.v(TAG, "notification listener disconnected")
        mConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.v(TAG, "new notification posted: ${sbn.toString()}")

        if (sbn != null && sbn.packageName == getString(R.string.accuradio_pkg_name)) {

            when (NotificationParser(sbn.notification).isAd()) {
                true -> Mute()
                false -> UnMute()
            }

        }

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        //todo: check for accuradio notification present or not.
        Log.v(TAG, "notification removed: ${sbn.toString()}")
    }

    override fun onLowMemory() {
        Log.v(TAG, "low memory trigger")
    }

    private fun Mute() {
        audioManager?.adjustVolume(
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_PLAY_SOUND
        )
        isMuted = true
        Log.v(TAG, "Ad detected muting")
    }

    private fun UnMute() {
        audioManager?.adjustVolume(
                AudioManager.ADJUST_UNMUTE,
                AudioManager.FLAG_PLAY_SOUND
        )
        isMuted = false
        Log.v(TAG, "Not an ad unmuting")
    }

}