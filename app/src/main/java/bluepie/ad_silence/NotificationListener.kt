package bluepie.ad_silence

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    var mConnected = false
    var isMuted = false
    private var audioManager: AudioManager? = null
    private var addNotificationHelper: AppNotificationHelper? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        Log.v(TAG, "listener created")
        addNotificationHelper = AppNotificationHelper(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationBuilder =  addNotificationHelper?.updateNotification("adSilence, service started",fromService = true)
        if (notificationBuilder != null){
            startForeground(NOTIFICATION_ID,notificationBuilder.build())
        }
        return Service.START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.v(TAG, "notification listener connected")
        val notification = addNotificationHelper?.updateNotification("AdSilence, listening for ads")
        mConnected = true

        // persistent notification
        startForeground(NOTIFICATION_ID,notification)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.v(TAG, "notification listener disconnected")
        mConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.v(TAG, "new notification posted: ${sbn.toString()}")

        if (sbn != null && sbn.packageName == getString(R.string.accuradio_pkg_name)) {
            when (NotificationParser(AppNotification(applicationContext, sbn.notification, SupportedApps.ACCURADIO)).isAd()) {
                true -> mute()
                false -> unMute()
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

    private fun mute() {
        audioManager?.adjustVolume(
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_PLAY_SOUND
        )
        isMuted = true
        addNotificationHelper?.updateNotification("AdSilence, ad-detected")
        Log.v(TAG, "Ad detected muting")
    }

    private fun unMute() {
        audioManager?.adjustVolume(
                AudioManager.ADJUST_UNMUTE,
                AudioManager.FLAG_PLAY_SOUND
        )
        addNotificationHelper?.updateNotification("AdSilence, listening for ads")
        Log.v(TAG, "Not an ad")
        isMuted = false
    }

}