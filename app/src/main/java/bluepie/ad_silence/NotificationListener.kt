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
import android.media.MediaRouter

@SuppressLint("LongLogTag")
class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    private var audioManager: AudioManager? = null
    private var appNotificationHelper: AppNotificationHelper? = null
    private var isMuted: Boolean = false
    private var muteCount: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var unmuteRunnable: Runnable? = null
    private lateinit var castMuteManager: CastMuteManager
    
    // MediaRouter related variables
    private var mediaRouter: MediaRouter? = null
    private var currentRoute: MediaRouter.RouteInfo? = null
    private var originalRemoteVolume: Int = -1
    private val mediaRouterCallback = object : MediaRouter.SimpleCallback() {
        override fun onRouteSelected(router: MediaRouter, type: Int, route: MediaRouter.RouteInfo) {
            currentRoute = route
            Log.v(TAG, "MediaRouter: Route selected: ${route.name}, type: ${route.playbackType}")
            // If route changes while muted, reset the original volume tracking
            if (originalRemoteVolume != -1) {
                originalRemoteVolume = -1
            }
        }

        override fun onRouteUnselected(router: MediaRouter, type: Int, route: MediaRouter.RouteInfo) {
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
        instance = this
        startTime = System.currentTimeMillis()
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        appNotificationHelper = AppNotificationHelper(applicationContext)
        castMuteManager = CastMuteManager(applicationContext)

        // Initialize MediaRouter on main thread
        handler.post {
            try {
                mediaRouter = applicationContext.getSystemService(android.content.Context.MEDIA_ROUTER_SERVICE) as MediaRouter
                mediaRouter?.addCallback(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
                currentRoute = mediaRouter?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
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
        instance = null
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

                                    val isForceMuteNoCheckEnabled = preference.isForceMuteNoCheckEnabled()
                                    var isMusicStreamMuted = this.isMusicMuted(applicationContext, audioManager!!)
                                    
                                    if (isForceMuteNoCheckEnabled) {
                                        Log.v(TAG, "Force mute enabled, ignoring existing mute state")
                                        isMusicStreamMuted = false
                                    }

                                    
                                    // Check for casting
                                    val route = currentRoute
                                    var isCasting = false
                                    if (preference.isCastingMuteEnabled()) {
                                        isCasting = route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                                        // Fallback: If MediaRouter says local, check notifications for a casting token
                                        if (!isCasting) {
                                            val castController = getMediaControllerForCasting()
                                            if (castController != null) {
                                                isCasting = true
                                                Log.v(TAG, "Casting detected via Notification Fallback")
                                                if (isDebugEnabled) {
                                                    LogManager.addLifecycleLog(LogEntry(
                                                        appName = "AdSilence",
                                                        timestamp = System.currentTimeMillis(),
                                                        isAd = true,
                                                        title = "Casting Detected",
                                                        text = "Casting session detected via Notification Fallback",
                                                        subText = "Detection"
                                                    ))
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (!isMuted || !isMusicStreamMuted) {
                                        Log.v(TAG, "'MusicStream' muted? -> $isMusicStreamMuted")
                                        Log.v(TAG, "Ad detected muting, state-> $isMuted to ${!isMuted}, currentPackage: $currentPackage")
                                        
                                        if (isCasting) {
                                            if (route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE) {
                                                Log.v(TAG, "Casting detected on route: ${route.name}. Muting remote volume.")
                                                if (isDebugEnabled) {
                                                    LogManager.addLifecycleLog(LogEntry(
                                                        appName = "AdSilence",
                                                        timestamp = System.currentTimeMillis(),
                                                        isAd = true,
                                                        title = "Casting Mute (Remote)",
                                                        text = "Muting remote volume on route: ${route.name}",
                                                        subText = "Action"
                                                    ))
                                                }
                                                handler.post {
                                                    originalRemoteVolume = route.volume
                                                    route.requestSetVolume(0)
                                                }
                                            } else {
                                                if (castMuteManager.tryMute(this@NotificationListener)) {
                                                    Log.v(TAG, "Muted via CastMuteManager (Notification Fallback)")
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
                                                    // Check for casting (Route or Fallback)
                                                    val currentRoute = this@NotificationListener.currentRoute
                                                    val isRemoteRoute = currentRoute != null && currentRoute.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                                                    val fallbackController = if (!isRemoteRoute) getMediaControllerForCasting() else null
                                                    val isCasting = isRemoteRoute || fallbackController != null
                                                    
                                                    while (muteCount > 0) {
                                                        if (isCasting) {
                                                            if (isRemoteRoute && currentRoute?.volumeHandling == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE && originalRemoteVolume != -1) {
                                                                 Log.v(TAG, "Restoring remote volume to $originalRemoteVolume")
                                                                 handler.post {
                                                                     currentRoute.requestSetVolume(originalRemoteVolume)
                                                                     originalRemoteVolume = -1
                                                                 }
                                                            } else {
                                                                if (castMuteManager.tryUnmute(this@NotificationListener)) {
                                                                    Log.v(TAG, "Unmuted via CastMuteManager (< M)")
                                                                }
                                                            }
                                                        } else {
                                                            castMuteManager.resetState()
                                                            unmute(
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
                                                     // Check for casting (Route or Fallback) 
                                                    
                                                    val currentRoute = this@NotificationListener.currentRoute
                                                    val isRemoteRoute = currentRoute != null && currentRoute.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                                                    val fallbackController = if (!isRemoteRoute) getMediaControllerForCasting() else null
                                                    val isCasting = isRemoteRoute || fallbackController != null

                                                    if (isCasting) {
                                                         if (isRemoteRoute && currentRoute?.volumeHandling == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE && originalRemoteVolume != -1) {
                                                             Log.v(TAG, "Restoring remote volume to $originalRemoteVolume")
                                                             handler.post {
                                                                 currentRoute.requestSetVolume(originalRemoteVolume)
                                                                 originalRemoteVolume = -1
                                                             }
                                                         } else {
                                                             if (castMuteManager.tryUnmute(this@NotificationListener)) {
                                                                 Log.v(TAG, "Unmuted via CastMuteManager")
                                                             }
                                                         }
                                                    } else {
                                                            castMuteManager.resetState()
                                                            unmute(
                                                                audioManager,
                                                                appNotificationHelper,
                                                                currentPackage,
                                                                preference
                                                            )
                                                        }
                                                    }
                                                    isMuted = false

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

                                            val delay = this.getUnmuteDelay(currentPackage, packageName, preference)
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
        @Volatile var instance: NotificationListener? = null
    }

    fun checkForCastingNotification(): String? {
        try {
            val notifications = activeNotifications
            for (sbn in notifications) {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
                val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
                
                // "Casting to..." (GMS & others)
                if (title?.contains("Casting to", ignoreCase = true) == true || 
                    text.contains("Casting to", ignoreCase = true) == true) {
                    return "$title"
                }
                
                // "Listening on..." (Spotify)
                if (subText.contains("Listening on", ignoreCase = true) == true) {
                    return "Casting: $subText" // e.g. "Casting: Listening on Kitchen Speaker"
                }

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for casting notification", e)
        }
        return null
    }
    fun getMediaControllerForCasting(): android.media.session.MediaController? {
        var fallbackController: android.media.session.MediaController? = null
        
        try {
            val notifications = activeNotifications
            if (notifications.isEmpty()) {
                Log.v(TAG, "getMediaControllerForCasting: No active notifications found.")
            }
            
            for (sbn in notifications) {
                val extras = sbn.notification.extras
                val token = extras.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION)
                val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: "No Title"
                val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
                
                Log.v(TAG, "Checking notification: Pkg=${sbn.packageName}, Title='$title', SubText='$subText', Token=${if(token!=null) "Yes" else "No"}")

                if (token != null) {
                    val controller = android.media.session.MediaController(applicationContext, token)
                    
                    // 1. High Priority: Explicit "Casting" text
                    val isCastingText = subText.contains("Listening on", ignoreCase = true) || 
                                      subText.contains("Casting to", ignoreCase = true) ||
                                      title.contains("Casting to", ignoreCase = true)

                    if (isCastingText) {
                         Log.v(TAG, "Found MediaSession for casting (Explicit): ${sbn.packageName}")
                         return controller
                    }
                    
                    // 2. Fallback: Check if it's playing
                    // We prefer a controller that is actually playing over a paused one
                    val playbackState = controller.playbackState
                    val isPlaying = playbackState != null && 
                                   (playbackState.state == android.media.session.PlaybackState.STATE_PLAYING ||
                                    playbackState.state == android.media.session.PlaybackState.STATE_BUFFERING)
                    
                    if (isPlaying) {
                        Log.v(TAG, "Found candidate MediaSession (Playing): ${sbn.packageName}")
                        fallbackController = controller
                    } else if (fallbackController == null) {
                        // Store as last resort if we haven't found a playing one yet
                        Log.v(TAG, "Found candidate MediaSession (Token Only): ${sbn.packageName}")
                        fallbackController = controller
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MediaController", e)
        }
        
        if (fallbackController != null) {
            Log.v(TAG, "Using fallback MediaController")
            return fallbackController
        }
        
        return null
    }
}