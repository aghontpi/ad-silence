package bluepie.ad_silence

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaControlIntent

@SuppressLint("LongLogTag")
class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    private var audioManager: AudioManager? = null
    private var appNotificationHelper: AppNotificationHelper? = null
    private var isMuted: Boolean = false
    private var muteCount: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var unmuteRunnable: Runnable? = null
    
    // MediaRouter related variables
    private var mediaRouter: MediaRouter? = null
    private var currentRoute: MediaRouter.RouteInfo? = null
    private var originalRemoteVolume: Int = -1
    private val mediaRouterCallback = object : MediaRouter.Callback() {
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            currentRoute = route
            Log.v(TAG, "MediaRouter: Route selected: ${route.name}, type: ${route.playbackType}")
            // If route changes while muted, reset the original volume tracking
            if (originalRemoteVolume != -1) {
                originalRemoteVolume = -1
                // potentially we could try to unmute the old route if we had a reference, but simplified for now
            }
        }

        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            Log.v(TAG, "MediaRouter: Route unselected: ${route.name}")
            if (currentRoute == route) {
                currentRoute = null
            }
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            if (currentRoute == route) {
                Log.v(TAG, "MediaRouter: Route changed: ${route.name}, volume: ${route.volume}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startTime = System.currentTimeMillis()
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        appNotificationHelper = AppNotificationHelper(applicationContext)

        // Initialize MediaRouter on main thread
        handler.post {
            try {
                mediaRouter = MediaRouter.getInstance(applicationContext)
                val selector = MediaRouteSelector.Builder()
                    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    .build()
                mediaRouter?.addCallback(selector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
                currentRoute = mediaRouter?.selectedRoute
                Log.v(TAG, "MediaRouter initialized. Current route: ${currentRoute?.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MediaRouter", e)
            }
        }

        Log.v(TAG, "listener created")
        if (Preference(applicationContext).isDebugLogEnabled()) {
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val preference = Preference(applicationContext)

        if (intent?.action == "STOP_SERVICE") {
            Log.v(TAG, "Service received STOP_SERVICE. Stopping foreground.")
            if (preference.isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Stop",
                    text = "Service received STOP_SERVICE. Stopping foreground.",
                    subText = "Lifecycle Event"
                ))
            }
            stopForeground(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.v(TAG, "API >= 24: Requesting Unbind.")
                if (preference.isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "AdSilence",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Service Unbind",
                        text = "API >= 24: Requesting Unbind.",
                        subText = "Lifecycle Event"
                    ))
                }
                requestUnbind()
            }
            stopSelf() // Force the service to stop, then destroy
            return Service.START_NOT_STICKY
        } else if (intent?.action == "STOP_FOREGROUND") {
            Log.v(TAG, "Service received STOP_FOREGROUND.")
            if (preference.isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Stop Foreground",
                    text = "Service received STOP_FOREGROUND.",
                    subText = "Lifecycle Event"
                ))
            }
            stopForeground(true)
        } else if (intent?.action == "START_SERVICE") {
            Log.v(TAG, "Service received START_SERVICE. Starting foreground.")
            if (preference.isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Start",
                    text = "Service received START_SERVICE. Starting foreground.",
                    subText = "Lifecycle Event"
                ))
            }
            if (preference.isNotificationsEnabled()) {
                appNotificationHelper?.getNotificationBuilder("adSilence, service started")?.run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, this.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, this.build())
                    }
                }
            }
        } else {
            // Default behavior for system start
            Log.v(TAG, "Service restored by system (START_STICKY).")
            if (preference.isDebugLogEnabled()) {
                LogManager.addLifecycleLog(LogEntry(
                    appName = "AdSilence",
                    timestamp = System.currentTimeMillis(),
                    isAd = false,
                    title = "Service Restored",
                    text = "Service restored by system (START_STICKY).",
                    subText = "Lifecycle Event"
                ))
            }

            if (preference.isNotificationsEnabled()) {
                appNotificationHelper?.getNotificationBuilder("adSilence, service started")?.run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, this.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, this.build())
                    }
                }
            }
        }
        return Service.START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val preference = Preference(applicationContext)
        if (preference.isNotificationsEnabled()) {
            appNotificationHelper?.updateNotification("AdSilence, listening for ads")?.run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, this, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, this) // persistent notification
                }
            }
        }
        Log.v(TAG, "notification listener connected")
        if (preference.isDebugLogEnabled()) {
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
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.v(TAG, "notification listener disconnected")
        if (Preference(applicationContext).isDebugLogEnabled()) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup MediaRouter
        handler.post {
            try {
                mediaRouter?.removeCallback(mediaRouterCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing MediaRouter callback", e)
            }
        }
        
        Log.v(TAG, "listener destroyed")
        if (Preference(applicationContext).isDebugLogEnabled()) {
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
                        Log.v(TAG, "new notification posted: $currentPackage ($packageName)")
                        Utils().run {
                            val parser = NotificationParser(this@with)
                            val isAd = parser.isAd()
                            
                            val isDebugEnabled = preference.isDebugLogEnabled()
                            if (isDebugEnabled) {
                                parser.lastLogEntry?.let { LogManager.addLog(it) }
                            }
                            
                            when (isAd) {
                                true -> {
                                    // Cancel any pending unmute if a new ad is detected
                                    unmuteRunnable?.let {
                                        handler.removeCallbacks(it)
                                        unmuteRunnable = null
                                        Log.v(TAG, "New ad detected, cancelled pending unmute")
                                        if (isDebugEnabled) {
                                            LogManager.addLifecycleLog(LogEntry(
                                                appName = "AdSilence",
                                                timestamp = System.currentTimeMillis(),
                                                isAd = true,
                                                title = "Unmute Cancelled",
                                                text = "Cancelled pending unmute for $currentPackage ($packageName)",
                                                subText = "Action"
                                            ))
                                        }
                                    }

                                    val isMusicStreamMuted = this.isMusicMuted(audioManager!!)
                                    
                                    // Check for casting
                                    val route = currentRoute
                                    // DEBUG: Force isCasting to true to test logic flow
                                    val isCasting = true // route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                                    
                                    if (!isMuted || !isMusicStreamMuted) {
                                        Log.v(TAG, "'MusicStream' muted? -> $isMusicStreamMuted")
                                        Log.v(TAG, "Ad detected muting, state-> $isMuted to ${!isMuted}, currentPackage: $currentPackage")
                                        
                                        // DEBUG: Simplified check for null route to prevent crash during mock
                                        if (isCasting && (route == null || route.volumeHandling == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)) {
                                            Log.v(TAG, "Casting detected on route: ${route?.name ?: "MOCK_ROUTE"}. Muting remote volume.")
                                            handler.post {
                                                if (route != null) {
                                                    originalRemoteVolume = route.volume
                                                    route.requestSetVolume(0)
                                                } else {
                                                    Log.v(TAG, "MOCK: Would set remote volume to 0")
                                                }
                                            }
                                        } else {
                                            this.mute(audioManager, appNotificationHelper, preference)
                                        }
                                        
                                        if (isDebugEnabled) {
                                            LogManager.addLifecycleLog(LogEntry(
                                                appName = "AdSilence",
                                                timestamp = System.currentTimeMillis(),
                                                isAd = true,
                                                title = "Muted",
                                                text = if (isCasting) "Muted remote cast on ${route?.name}" else "Muted audio for $currentPackage ($packageName)",
                                                subText = "Action"
                                            ))
                                        }
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
                                        // Instead of unmuting immediately, we schedule it to fix race condition, 
                                        // https://github.com/aghontpi/ad-silence/pull/282#issue-3682929324
                                        if (unmuteRunnable == null) {
                                            unmuteRunnable = Runnable {
                                                val route = this@NotificationListener.currentRoute
                                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                                    Log.v(TAG, "Not an ad, Unmuting, < M")
                                                    // for android 5 & 5.1, unmute has to be done, count x mutedCount
                                                    // DEBUG: Force isCasting to true
                                                    val isCasting = true // route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                                                    
                                                    while (muteCount > 0) {
                                                        // DEBUG: Handle null route safely
                                                        if (isCasting && (route == null || (route.volumeHandling == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE && originalRemoteVolume != -1))) {
                                                             Log.v(TAG, "Restoring remote volume to $originalRemoteVolume")
                                                             handler.post {
                                                                 if (route != null) {
                                                                     route.requestSetVolume(originalRemoteVolume)
                                                                 } else {
                                                                     Log.v(TAG, "MOCK: Would restore remote volume")
                                                                 }
                                                                 originalRemoteVolume = -1
                                                             }
                                                        } else {
                                                            this@run.unmute(
                                                                audioManager,
                                                                appNotificationHelper,
                                                                currentPackage,
                                                                preference
                                                            )
                                                        }
                                                        muteCount--
                                                    }
                                                    isMuted = false
                                                } else {
                                                    Log.v(TAG, "Not an ad, Unmuting, > M")
                                                    // DEBUG: Force isCasting to true
                                                    val isCasting = true // route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                                                    
                                                    // DEBUG: Handle null route safely
                                                    if (isCasting && (route == null || (route.volumeHandling == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE && originalRemoteVolume != -1))) {
                                                         Log.v(TAG, "Restoring remote volume to $originalRemoteVolume")
                                                         handler.post {
                                                             if (route != null) {
                                                                 route.requestSetVolume(originalRemoteVolume)
                                                             } else {
                                                                 Log.v(TAG, "MOCK: Would restore remote volume")
                                                             }
                                                             originalRemoteVolume = -1
                                                         }
                                                    } else {
                                                        this@run.unmute(
                                                            audioManager,
                                                            appNotificationHelper,
                                                            currentPackage,
                                                            preference
                                                        )
                                                    }
                                                    isMuted = false
                                                }
                                                if (isDebugEnabled) {
                                                    LogManager.addLifecycleLog(LogEntry(
                                                        appName = "AdSilence",
                                                        timestamp = System.currentTimeMillis(),
                                                        isAd = false,
                                                        title = "Unmuted",
                                                        text = "Unmuted audio for $currentPackage ($packageName)",
                                                        subText = "Action"
                                                    ))
                                                }
                                                unmuteRunnable = null
                                            }

                                            val delay = this.getUnmuteDelay(currentPackage)
                                            if (delay > 0) {
                                                Log.v(TAG, "scheduling unmute for $currentPackage ($packageName) with delay $delay")
                                                if (isDebugEnabled) {
                                                    LogManager.addLifecycleLog(LogEntry(
                                                        appName = "AdSilence",
                                                        timestamp = System.currentTimeMillis(),
                                                        isAd = false,
                                                        title = "Unmute Scheduled",
                                                        text = "Unmute Scheduled after ${delay}ms",
                                                        subText = "Action"
                                                    ))
                                                }
                                                handler.postDelayed(unmuteRunnable!!, delay)
                                            } else {
                                                Log.v(TAG, "unmuting immediately for $currentPackage ($packageName)")
                                                unmuteRunnable!!.run()
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
    companion object {
        @Volatile var startTime: Long = 0
    }
}