package bluepie.ad_silence

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

@SuppressLint("LongLogTag")
class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    private var audioManager: AudioManager? = null
    private var appNotificationHelper: AppNotificationHelper? = null
    private var isMuted: Boolean = false

    override fun onCreate() {
        super.onCreate()
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        appNotificationHelper = AppNotificationHelper(applicationContext)
        Log.v(TAG, "listener created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appNotificationHelper?.getNotificationBuilder("adSilence, service started")?.run {
            startForeground(NOTIFICATION_ID, this.build())
        }
        return Service.START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        appNotificationHelper?.updateNotification("AdSilence, listening for ads")?.run {
            startForeground(NOTIFICATION_ID, this) // persistent notification
        }
        Log.v(TAG, "notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            with(AppNotification(applicationContext, it.notification, sbn.packageName)) {
                Preference(applicationContext).isAppConfigured(this.getApp()).takeIf { b -> b }
                    ?.run {
                        Log.v(TAG, "new notification posted: ${this@with.getApp()}")
                        Utils().run {
                            when (NotificationParser(this@with).isAd()) {
                                true -> {
                                    Log.v(TAG, "Ad detected muting")
                                    this.mute(audioManager, appNotificationHelper)
                                    isMuted = true
                                }
                                false -> {
                                    Log.v(TAG, "Not an ad")
                                    isMuted.takeIf { b -> b }?.also {
                                        isMuted = false
                                        this@run.unmute(
                                            audioManager,
                                            appNotificationHelper,
                                            this@with.getApp()
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}