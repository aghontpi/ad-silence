package bluepie.ad_silence

import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.*


class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    var mConnected = false
    var isMuted = false
    var audioManager: AudioManager? = null

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

        if (sbn != null && sbn.packageName == "com.slipstream.accuradio") {

            // parse the notificaion with refection
            val notification = sbn.notification


            var fields: ArrayList<Any>? = null
            for (field in notification.contentView.javaClass.declaredFields) {
                if (field.name == "mActions") {
                    field.isAccessible = true
                    fields = field.get(notification.contentView) as ArrayList<Any>
                }

            }

            val info = java.util.LinkedList<String>()
            fields?.toArray()?.forEach {
                if (it != null) {
                    var fieldFilter = it.javaClass.declaredFields.filter { it.name == "value" }
                    if (fieldFilter.size == 1) {
                        var field = fieldFilter.get(0)
                        field.isAccessible = true

                        // necessary fields
                        when (val _v = field.get(it)) {
                            is String -> info.push(_v)
                            else -> "Not applicable"
                        }
                    }
                }
            }


            if (info.size > 0 && audioManager is AudioManager) {
                if (info.any { it.contains(getString(R.string.accuradio_ad_text)) }) {
                    // mute
                    audioManager?.adjustVolume(
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    isMuted = true
                    Log.v(TAG, "Add detected muting")
                } else if (isMuted) {
                    // unmute
                    audioManager?.adjustVolume(
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    isMuted = false
                    Log.v(TAG, "Not an add: ${info.toString()}")
                }
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
}