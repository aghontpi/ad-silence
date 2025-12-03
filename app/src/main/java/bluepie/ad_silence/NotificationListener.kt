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
        LogManager.addLifecycleLog(
            LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "Service Created",
                text = "Notification Listener Service Created",
                subText = "Lifecycle Event"
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            Log.v(TAG, "Service received STOP_SERVICE. Stopping foreground.")
            LogManager.addLifecycleLog(LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "Service Stop",
                text = "Service received STOP_SERVICE. Stopping foreground.",
                subText = "Lifecycle Event"
            ))
            stopForeground(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.v(TAG, "API >= 24: Requesting Unbind.")
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Unbind",
                    text = "API >= 24: Requesting Unbind.",
                    subText = "Lifecycle Event"
                ))
                requestUnbind()
            }
            stopSelf() // Force the service to stop, then destroy
            return Service.START_NOT_STICKY
        } else if (intent?.action == "START_SERVICE") {
            Log.v(TAG, "Service received START_SERVICE. Starting foreground.")
            LogManager.addLifecycleLog(LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "Service Start",
                text = "Service received START_SERVICE. Starting foreground.",
                subText = "Lifecycle Event"
            ))
            appNotificationHelper?.getNotificationBuilder("adSilence, service started")?.run {
                startForeground(NOTIFICATION_ID, this.build())
            }
        } else {
            // Default behavior for system start
            appNotificationHelper?.getNotificationBuilder("adSilence, service started")?.run {
                startForeground(NOTIFICATION_ID, this.build())
            }
        }
        return Service.START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        appNotificationHelper?.updateNotification("AdSilence, listening for ads")?.run {
            startForeground(NOTIFICATION_ID, this) // persistent notification
        }
        Log.v(TAG, "notification listener connected")
        LogManager.addLifecycleLog(
            LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "Listener Connected",
                text = "Notification Listener Connected",
                subText = "Lifecycle Event"
            )
        )
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.v(TAG, "notification listener disconnected")
        LogManager.addLifecycleLog(
            LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "Listener Disconnected",
                text = "Notification Listener Disconnected",
                subText = "Lifecycle Event"
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "listener destroyed")
        LogManager.addLifecycleLog(
            LogEntry(
                appName = "AdSilence",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = "Service Destroyed",
                text = "Notification Listener Service Destroyed",
                subText = "Lifecycle Event"
            )
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val preference = Preference(applicationContext)
        
        if (!preference.isEnabled()) {
            return
        }

        sbn?.let {
            with(AppNotification(applicationContext, it.notification, sbn.packageName)) {
                preference.isAppConfigured(this.getApp(), this.packageName).takeIf { b -> b }
                    ?.run {
                        val currentPackage = this@with.getApp()
                        Log.v(TAG, "new notification posted: $currentPackage")
                        Utils().run {
                            val parser = NotificationParser(this@with)
                            val isAd = parser.isAd()
                            if (preference.isDebugLogEnabled()) {
                                parser.lastLogEntry?.let { LogManager.addLog(it) }
                            }
                            
                            when (isAd) {
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