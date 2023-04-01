package bluepie.ad_silence

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

@SuppressLint("LongLogTag")
class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    private var audioManager: AudioManager? = null
    private var appNotificationHelper: AppNotificationHelper? = null
    private var isMuted: Boolean = false
    private var muteCount: Int = 0

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
        val preference = Preference(applicationContext)
        sbn?.let {
            with(AppNotification(applicationContext, it.notification, sbn.packageName)) {
                preference.isAppConfigured(this.getApp()).takeIf { b -> b }
                    ?.run {
                        val currentPackage = this@with.getApp()
                        Log.v(TAG, "new notification posted: $currentPackage")
                        Utils().run {
                            when (NotificationParser(this@with).isAd() ) {
                                true -> {
                                    val isMusicStreamMuted = this.isMusicMuted(audioManager!!)
                                    if (!isMuted || !isMusicStreamMuted) {
                                        Log.v(TAG, "'MusicStream' muted? -> $isMusicStreamMuted")
                                        Log.v(TAG, "Ad detected muting, state-> $isMuted to ${!isMuted}, currentPackage: $currentPackage")
                                        this.mute(audioManager, appNotificationHelper, preference)
                                        isMuted = true
                                        if (isMusicStreamMuted) muteCount = 0 else muteCount++
                                    } else {
                                        Log.v(
                                            TAG,
                                            "Ad detected but already muted, state-> $isMuted"
                                        )
                                    }
                                }
                                false -> {
                                    isMuted.takeIf { b -> b }?.also {
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                            Log.v(TAG, "Not an ad, Unmuting, < M")
                                            // for android 5 & 5.1, unmute has to be done, count x mutedCount
                                            while (muteCount > 0) {
                                                this@run.unmute(
                                                    audioManager,
                                                    appNotificationHelper,
                                                    currentPackage,
                                                    preference
                                                )
                                                muteCount--
                                            }
                                            isMuted = false
                                        } else {
                                            Log.v(TAG, "Not an ad, Unmuting, > M")
                                            this@run.unmute(
                                                audioManager,
                                                appNotificationHelper,
                                                currentPackage,
                                                preference
                                            )
                                            isMuted = false
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}